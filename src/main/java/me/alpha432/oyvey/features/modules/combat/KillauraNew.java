package me.alpha432.oyvey.features.modules.combat;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.MathUtil;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class KillauraNew extends Module {
   public final Setting<Double> reach = this.register(new Setting<>("Reach", 3.0, 1.0, 6.0));
   public final Setting<Boolean> mobs = this.register(new Setting<>("Mobs", false));
   public final Setting<Boolean> respectFriends = this.register(new Setting<>("RespectFriends", true));
   public final Setting<Boolean> wallCheck = this.register(new Setting<>("WallCheck", true));
   public final Setting<Integer> aps = this.register(new Setting<>("APS", 12, 1, 20));
   public final Setting<Boolean> cooldown = this.register(new Setting<>("1.9Cooldown", true));
   public final Setting<Float> turnSpeed = this.register(new Setting<>("TurnSpeed", 25.0F, 1.0F, 180.0F));
   public final Setting<Boolean> gcdFix = this.register(new Setting<>("GCDFix", true));
   public final Setting<KillauraNew.MoveFix> moveFix = this.register(new Setting<>("MoveFix", KillauraNew.MoveFix.NORMAL));
   public final Setting<Boolean> armSwing = this.register(new Setting<>("ArmSwing", true));
   public static volatile boolean isRotating = false;
   public static volatile float visualYaw = 0.0F;
   public static volatile float visualPitch = 0.0F;
   private float smoothYaw;
   private float smoothPitch;
   private float sentYaw;
   private float sentPitch;
   private float savedYaw;
   private float savedPitch;
   private boolean hasSaved = false;
   private float[] targetRots = null;
   private Entity currentTarget = null;
   private long lastAttackTime = 0L;

   public KillauraNew() {
      super("KillauraNew", "LiquidBounce-style KillAura with a correct yaw-delta move fix.", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      this.currentTarget = null;
      this.targetRots = null;
      this.hasSaved = false;
      this.lastAttackTime = 0L;
      if (mc.player != null) {
         this.smoothYaw = mc.player.getYRot();
         this.smoothPitch = mc.player.getXRot();
         this.sentYaw = this.smoothYaw;
         this.sentPitch = this.smoothPitch;
      }
   }

   @Override
   public void onDisable() {
      isRotating = false;
      if (this.hasSaved && mc.player != null) {
         OyVey.rotationManager.setPlayerRotations(this.savedYaw, this.savedPitch);
      }

      this.hasSaved = false;
      this.targetRots = null;
      this.currentTarget = null;
      OyVey.rotationManager.clearServerRotations();
   }

   @Subscribe
   public void onUpdateWalkingPlayer(UpdateWalkingPlayerEvent event) {
      if (mc.level != null && mc.player != null) {
         if (event.getStage() == Stage.MOTION_PRE) {
            this.updateTargeting();
            if (this.targetRots != null) {
               visualYaw = mc.player.getYRot();
               visualPitch = mc.player.getXRot();
               isRotating = true;
               this.savedYaw = visualYaw;
               this.savedPitch = visualPitch;
               this.hasSaved = true;
               OyVey.rotationManager.setPlayerRotations(this.targetRots[0], this.targetRots[1]);
               this.applyMoveFix(this.targetRots[0], this.savedYaw);
            } else {
               isRotating = false;
               this.hasSaved = false;
            }
         } else if (event.getStage() == Stage.PRE) {
            if (this.targetRots != null && this.currentTarget != null && this.shouldAttack()) {
               if (mc.player.isSprinting()) {
                  mc.player.setSprinting(false);
                  mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SPRINTING));
               }

               mc.gameMode.attack(mc.player, this.currentTarget);
               if (this.armSwing.getValue()) {
                  mc.player.swing(InteractionHand.MAIN_HAND);
               }

               this.lastAttackTime = System.currentTimeMillis();
            }
         } else if (event.getStage() == Stage.POST) {
            isRotating = false;
            if (this.hasSaved) {
               OyVey.rotationManager.setPlayerRotations(this.savedYaw, this.savedPitch);
               if (this.targetRots != null) {
                  this.sentYaw = this.targetRots[0];
                  this.sentPitch = this.targetRots[1];
               }
            }

            this.hasSaved = false;
         }
      }
   }

   private void updateTargeting() {
      List<Entity> targets = this.getTargets();
      if (targets.isEmpty()) {
         this.currentTarget = null;
         this.targetRots = null;
         OyVey.rotationManager.clearServerRotations();
      } else {
         this.currentTarget = targets.get(0);
         this.targetRots = this.buildRotations(this.currentTarget);
         OyVey.rotationManager.setServerRotations(this.targetRots[0], this.targetRots[1]);
      }
   }

   private List<Entity> getTargets() {
      double r = this.reach.getValue();
      return StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), false)
         .filter(e -> e != mc.player && e.isAlive())
         .filter(e -> e instanceof Player || this.mobs.getValue() && e instanceof Mob)
         .filter(e -> !(e instanceof Player p && this.respectFriends.getValue() && OyVey.friendManager.isFriend(p)))
         .filter(e -> mc.player.distanceTo(e) <= r)
         .filter(e -> !this.wallCheck.getValue() || this.hasLineOfSight(e))
         .sorted(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
         .collect(Collectors.toList());
   }

   private float[] buildRotations(Entity target) {
      float[] raw = MathUtil.calcAngle(mc.player.getEyePosition(), target.getEyePosition());
      float speed = this.turnSpeed.getValue();
      float dYaw = Mth.wrapDegrees(raw[0] - this.smoothYaw);
      float dPitch = raw[1] - this.smoothPitch;
      this.smoothYaw = this.smoothYaw + Mth.clamp(dYaw, -speed, speed);
      this.smoothPitch = Mth.clamp(this.smoothPitch + Mth.clamp(dPitch, -speed * 0.6F, speed * 0.6F), -90.0F, 90.0F);
      return this.gcdFix.getValue() ? this.applyGcd(this.smoothYaw, this.smoothPitch) : new float[]{this.smoothYaw, this.smoothPitch};
   }

   private float[] applyGcd(float tYaw, float tPitch) {
      float gcd = this.computeGcd();
      float deltaYaw = Math.round(Mth.wrapDegrees(tYaw - this.sentYaw) / gcd) * gcd;
      float deltaPitch = Math.round((tPitch - this.sentPitch) / gcd) * gcd;
      return new float[]{this.sentYaw + deltaYaw, Mth.clamp(this.sentPitch + deltaPitch, -90.0F, 90.0F)};
   }

   private float computeGcd() {
      double s = (Double)mc.options.sensitivity().get();
      double f = s * 0.6 + 0.2;
      return (float)(f * f * f * 8.0 * 0.15);
   }

   private void applyMoveFix(float serverYaw, float realYaw) {
      float strafe = mc.player.input.getMoveVector().x;
      float forward = mc.player.input.getMoveVector().y;
      if (this.moveFix.getValue() != KillauraNew.MoveFix.OFF && (strafe != 0.0F || forward != 0.0F)) {
         float rad = (float)Math.toRadians(realYaw - serverYaw);
         float sin = Mth.sin(rad);
         float cos = Mth.cos(rad);
         float nx = strafe * cos - forward * sin;
         float nz = strafe * sin + forward * cos;
         if (this.moveFix.getValue() == KillauraNew.MoveFix.GRIM) {
            nx = Math.abs(nx) > 0.3F ? Math.signum(nx) : 0.0F;
            nz = Math.abs(nz) > 0.3F ? Math.signum(nz) : 0.0F;
         }

         mc.player.xxa = nx;
         mc.player.zza = nz;
      } else {
         mc.player.xxa = strafe;
         mc.player.zza = forward;
      }
   }

   private boolean shouldAttack() {
      if (this.currentTarget == null || this.targetRots == null || !this.rotationHitsTarget(this.currentTarget, this.targetRots[0], this.targetRots[1])) {
         return false;
      } else {
         return this.cooldown.getValue()
            ? mc.player.getAttackStrengthScale(0.0F) >= 0.95F
            : System.currentTimeMillis() - this.lastAttackTime >= 1000L / Math.max(1, this.aps.getValue());
      }
   }

   private boolean rotationHitsTarget(Entity target, float yaw, float pitch) {
      Vec3 eyes = mc.player.getEyePosition();
      Vec3 end = eyes.add(Vec3.directionFromRotation(pitch, yaw).scale(this.reach.getValue()));
      Optional<Vec3> hit = target.getBoundingBox().inflate(0.1).clip(eyes, end);
      return hit.isPresent() && this.rayMiss(eyes, hit.get());
   }

   private boolean hasLineOfSight(Entity target) {
      Vec3 eyes = mc.player.getEyePosition();
      AABB box = target.getBoundingBox();
      if (this.rayMiss(eyes, box.getCenter())) {
         return true;
      }

      for (double x : new double[]{box.minX, box.maxX}) {
         for (double y : new double[]{box.minY + 0.1, box.maxY - 0.1}) {
            for (double z : new double[]{box.minZ, box.maxZ}) {
               if (this.rayMiss(eyes, new Vec3(x, y, z))) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean rayMiss(Vec3 from, Vec3 to) {
      return mc.level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, mc.player)).getType() == Type.MISS;
   }

   public Entity getTarget() {
      return this.currentTarget;
   }

   public enum MoveFix {
      OFF,
      NORMAL,
      GRIM;
   }
}
