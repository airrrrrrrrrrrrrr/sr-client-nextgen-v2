package me.alpha432.oyvey.features.modules.combat;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.movement.SprintModule;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class GrimAura extends Module {
   public final Setting<Double> reach = this.register(new Setting<>("Reach", 3.0, 1.0, 3.0));
   public final Setting<Integer> cps = this.register(new Setting<>("CPS", 10, 1, 20));
   public final Setting<Boolean> cooldown = this.register(new Setting<>("1.9Cooldown", true));
   public final Setting<Boolean> mobs = this.register(new Setting<>("Mobs", false));
   public final Setting<Boolean> wallCheck = this.register(new Setting<>("WallCheck", true));
   public final Setting<Boolean> onlyCrits = this.register(new Setting<>("OnlyCrits", false));
   public final Setting<Boolean> requireArmor = this.register(new Setting<>("RequireArmor", false));
   public final Setting<GrimAura.AntiBotMode> antiBot = this.register(new Setting<>("AntiBot", GrimAura.AntiBotMode.SPIN));
   public final Setting<Float> botSpinDeg = this.register(
      new Setting<>("BotSpinDeg", 60.0F, 20.0F, 180.0F, v -> this.antiBot.getValue() == GrimAura.AntiBotMode.SPIN)
   );
   private float sentYaw = 0.0F;
   private float sentPitch = 0.0F;
   private float aimYaw = 0.0F;
   private float aimPitch = 0.0F;
   private float savedYaw = 0.0F;
   private float savedPitch = 0.0F;
   private boolean rotInjected = false;
   private Entity currentTarget = null;
   private long lastAttackTime = 0L;
   private final Map<Integer, float[]> botTracker = new HashMap<>();

   public GrimAura() {
      super("GrimAura", "Server-side only rotations to bypass Grim \u2014 camera never moves.", Module.Category.OLD);
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.sentYaw = mc.player.getYRot();
         this.sentPitch = mc.player.getXRot();
      }

      this.currentTarget = null;
      this.rotInjected = false;
      this.botTracker.clear();
   }

   @Override
   public void onDisable() {
      if (this.rotInjected && mc.player != null) {
         this.restoreCamera();
      }

      this.currentTarget = null;
      OyVey.rotationManager.clearServerRotations();
   }

   @Subscribe
   public void onWalk(UpdateWalkingPlayerEvent event) {
      if (mc.player != null && mc.level != null) {
         if (event.getStage() == Stage.MOTION_PRE) {
            if (this.currentTarget != null && this.shouldAttack()) {
               SprintModule sm0 = OyVey.moduleManager.getModuleByClass(SprintModule.class);
               boolean ks = sm0 != null && sm0.keepSprint.getValue();
               if (!ks && mc.player.isSprinting()) {
                  mc.player.setSprinting(false);
                  mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SPRINTING));
               }
            }
         } else if (event.getStage() == Stage.POST) {
            if (this.rotInjected) {
               this.restoreCamera();
               this.sentYaw = this.aimYaw;
               this.sentPitch = this.aimPitch;
            }
         } else if (event.getStage() == Stage.PRE) {
            SprintModule sm = OyVey.moduleManager.getModuleByClass(SprintModule.class);
            boolean ks = sm != null && sm.keepSprint.getValue();
            if (!ks && mc.player.isSprinting()) {
               long pauseMs = sm != null ? sm.sprintResetTicks.getValue().intValue() * 50L : 500L;
               if (System.currentTimeMillis() - SprintModule.lastAttackMs < pauseMs) {
                  mc.player.setSprinting(false);
               }
            }

            this.currentTarget = this.pickTarget();
            if (this.currentTarget == null) {
               this.rotInjected = false;
               OyVey.rotationManager.clearServerRotations();
            } else {
               float[] aim = this.snapAim(this.currentTarget);
               this.aimYaw = aim[0];
               this.aimPitch = aim[1];
               this.savedYaw = mc.player.getYRot();
               this.savedPitch = mc.player.getXRot();
               mc.player.setYRot(this.aimYaw);
               mc.player.yHeadRot = this.aimYaw;
               mc.player.setXRot(this.aimPitch);
               this.rotInjected = true;
               OyVey.rotationManager.setServerRotations(this.aimYaw, this.aimPitch);
               if (this.shouldAttack()) {
                  WTap.onAttack();
                  SprintModule smKs = OyVey.moduleManager.getModuleByClass(SprintModule.class);
                  boolean ksx = smKs != null && smKs.keepSprint.getValue();
                  if (!ksx && mc.player.isSprinting()) {
                     mc.player.setSprinting(false);
                     mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SPRINTING));
                  }

                  SprintModule.lastAttackMs = System.currentTimeMillis();
                  BacktrackModule bt = OyVey.moduleManager.getModuleByClass(BacktrackModule.class);
                  if (bt != null && bt.isEnabled()) {
                     bt.applyBacktrack(this.currentTarget, () -> {
                        mc.gameMode.attack(mc.player, this.currentTarget);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                     });
                  } else {
                     mc.gameMode.attack(mc.player, this.currentTarget);
                     mc.player.swing(InteractionHand.MAIN_HAND);
                  }

                  this.lastAttackTime = System.currentTimeMillis();
               }
            }
         }
      }
   }

   private void restoreCamera() {
      mc.player.setYRot(this.savedYaw);
      mc.player.yHeadRot = this.savedYaw;
      mc.player.setXRot(this.savedPitch);
      this.rotInjected = false;
   }

   private Entity pickTarget() {
      double r = this.reach.getValue();
      BacktrackModule bt = OyVey.moduleManager.getModuleByClass(BacktrackModule.class);
      if (bt != null && bt.isEnabled()) {
         r = Math.max(r, bt.maxRange.getValue());
      }

      double effectiveReach = r;
      this.botTracker.keySet().removeIf(id -> {
         Entity e = mc.level.getEntity(id);
         return e == null || !e.isAlive() || mc.player.distanceTo(e) > effectiveReach + 4.0;
      });
      List<Entity> list = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), false)
         .filter(e -> e != mc.player && e.isAlive())
         .filter(e -> e instanceof Player || this.mobs.getValue() && e instanceof Mob)
         .filter(e -> mc.player.distanceTo(e) <= effectiveReach)
         .filter(e -> !this.requireArmor.getValue() || this.hasArmor(e))
         .filter(e -> !this.isBot(e))
         .filter(e -> !this.wallCheck.getValue() || this.hasLos(e))
         .sorted(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
         .collect(Collectors.toList());
      return list.isEmpty() ? null : list.get(0);
   }

   private boolean hasArmor(Entity e) {
      return e instanceof LivingEntity le
         ? !le.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
            || !le.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
            || !le.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
            || !le.getItemBySlot(EquipmentSlot.FEET).isEmpty()
         : true;
   }

   private boolean isBot(Entity e) {
      switch ((GrimAura.AntiBotMode)this.antiBot.getValue()) {
         case SPIN:
            float[] d = this.botTracker.computeIfAbsent(e.getId(), id -> new float[]{e.getYRot(), 0.0F, 0.0F});
            float delta = Math.abs(Mth.wrapDegrees(e.getYRot() - d[0]));
            d[0] = e.getYRot();
            d[1] += delta;
            d[2]++;
            if (d[2] < 10.0F) {
               return false;
            }

            return d[1] / d[2] > this.botSpinDeg.getValue();
         case CUBECRAFT:
            return e.isInvisible();
         default:
            return false;
      }
   }

   private boolean hasLos(Entity target) {
      Vec3 eyes = mc.player.getEyePosition();
      AABB box = target.getBoundingBox();
      if (this.rayMiss(eyes, box.getCenter())) {
         return true;
      }

      double[] xs = new double[]{box.minX, box.maxX};
      double[] ys = new double[]{box.minY + 0.1, box.maxY - 0.1};
      double[] zs = new double[]{box.minZ, box.maxZ};

      for (double x : xs) {
         for (double y : ys) {
            for (double z : zs) {
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

   private Vec3 getAimPoint(Entity target) {
      BacktrackModule bt = OyVey.moduleManager.getModuleByClass(BacktrackModule.class);
      if (bt != null && bt.isEnabled()) {
         AABB box = bt.getBacktrackedBox(target);
         if (box != null) {
            Vec3 c = box.getCenter();
            return new Vec3(c.x, box.minY + (box.maxY - box.minY) * 0.6, c.z);
         }
      }

      return new Vec3(target.getX(), target.getY() + target.getEyeHeight() * 0.6, target.getZ());
   }

   private float[] snapAim(Entity target) {
      Vec3 ap = this.getAimPoint(target);
      double dX = ap.x - mc.player.getX();
      double dY = ap.y - (mc.player.getY() + mc.player.getEyeHeight());
      double dZ = ap.z - mc.player.getZ();
      double dist = Math.sqrt(dX * dX + dZ * dZ);
      float exactYaw = (float)(Math.atan2(dZ, dX) * 180.0 / 3.141592653589793) - 90.0F;
      float exactPitch = Mth.clamp((float)(-(Math.atan2(dY, dist) * 180.0 / 3.141592653589793)), -90.0F, 90.0F);
      float gcd = this.computeGcd();
      float dYaw = Math.round(Mth.wrapDegrees(exactYaw - this.sentYaw) / gcd) * gcd;
      float dPitch = Math.round((exactPitch - this.sentPitch) / gcd) * gcd;
      return new float[]{this.sentYaw + dYaw, Mth.clamp(this.sentPitch + dPitch, -90.0F, 90.0F)};
   }

   private float computeGcd() {
      double s = (Double)mc.options.sensitivity().get();
      double f = s * 0.6 + 0.2;
      return (float)(f * f * f * 8.0 * 0.15);
   }

   private boolean shouldAttack() {
      long delay = 1000L / Math.max(1, this.cps.getValue());
      if (System.currentTimeMillis() - this.lastAttackTime < delay) {
         return false;
      } else {
         boolean falling = !mc.player.onGround()
            && mc.player.getDeltaMovement().y < 0.0
            && !mc.player.isInWater()
            && !mc.player.isInLava()
            && !mc.player.isFallFlying();
         if (!this.cooldown.getValue()) {
            return this.onlyCrits.getValue() ? falling : true;
         } else {
            return this.onlyCrits.getValue() ? mc.player.getAttackStrengthScale(0.0F) >= 1.0F && falling : mc.player.getAttackStrengthScale(0.0F) >= 1.0F;
         }
      }
   }

   public Entity getTarget() {
      return this.currentTarget;
   }

   public enum AntiBotMode {
      OFF,
      SPIN,
      CUBECRAFT;
   }
}
