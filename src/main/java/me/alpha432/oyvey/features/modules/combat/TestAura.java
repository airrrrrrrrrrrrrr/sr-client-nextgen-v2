package me.alpha432.oyvey.features.modules.combat;

import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.StreamSupport;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class TestAura extends Module {
   public final Setting<Double> reach = this.num("Reach", 3.5, 1.0, 6.0);
   public final Setting<Boolean> mobs = this.bool("Mobs", false);
   public final Setting<Boolean> onlyCrits = this.bool("OnlyCrits", false);
   public final Setting<Boolean> cooldown = this.bool("1.9Cooldown", true);
   public final Setting<Integer> cpsMin = this.num("CPSMin", 9, 1, 20);
   public final Setting<Integer> cpsMax = this.num("CPSMax", 14, 1, 20);
   public final Setting<Float> rotSpeed = this.num("RotSpeed", 4.0F, 0.5F, 15.0F);
   public final Setting<Float> fov = this.num("FOV", 180.0F, 10.0F, 360.0F);
   public final Setting<TestAura.RotMode> rotMode = this.mode("RotMode", TestAura.RotMode.VISIBLE);
   public final Setting<Boolean> gcdFix = this.bool("GCDFix", true);
   private float smoothYaw = 0.0F;
   private float smoothPitch = 0.0F;
   private float sentYaw = 0.0F;
   private float sentPitch = 0.0F;
   private boolean hasSentRot = false;
   private Entity target = null;
   private long lastAttack = 0L;
   private long attackDelay = 0L;
   private float savedYaw = 0.0F;
   private float savedPitch = 0.0F;
   private boolean hasSaved = false;

   public TestAura() {
      super("TestAura", "Visible-rotation kill aura for testing.", Module.Category.OLD);
   }

   @Override
   public void onEnable() {
      this.target = null;
      this.hasSaved = false;
      this.hasSentRot = false;
      this.attackDelay = this.rollDelay();
      if (mc.player != null) {
         this.smoothYaw = mc.player.getYRot();
         this.smoothPitch = mc.player.getXRot();
         this.sentYaw = this.smoothYaw;
         this.sentPitch = this.smoothPitch;
      }
   }

   @Override
   public void onDisable() {
      if (this.hasSaved && mc.player != null) {
         OyVey.rotationManager.setPlayerRotations(this.savedYaw, this.savedPitch);
      }

      this.hasSaved = false;
      this.hasSentRot = false;
      this.target = null;
   }

   @Subscribe
   public void onWalk(UpdateWalkingPlayerEvent event) {
      if (mc.player != null && mc.level != null) {
         if (event.getStage() == Stage.MOTION_PRE) {
            this.target = this.pickTarget();
            if (this.target == null) {
               this.hasSaved = false;
            } else {
               float[] rot = this.buildSmoothedRot(this.target);
               TestAura.RotMode rm = this.rotMode.getValue();
               if (rm != TestAura.RotMode.VISIBLE && rm != TestAura.RotMode.HYBRID) {
                  this.savedYaw = mc.player.getYRot();
                  this.savedPitch = mc.player.getXRot();
                  this.hasSaved = true;
                  OyVey.rotationManager.setPlayerRotations(rot[0], rot[1]);
                  this.applyMovementCorrection(rot[0], this.savedYaw);
               } else {
                  OyVey.rotationManager.setPlayerRotations(rot[0], rot[1]);
                  this.hasSaved = false;
               }
            }
         } else if (event.getStage() != Stage.POST) {
            if (event.getStage() == Stage.PRE && this.target != null && this.shouldAttack()) {
               WTap.onAttack();
               mc.gameMode.attack(mc.player, this.target);
               mc.player.swing(InteractionHand.MAIN_HAND);
               this.lastAttack = System.currentTimeMillis();
               this.attackDelay = this.rollDelay();
            }
         } else {
            if (this.hasSaved && mc.player != null) {
               TestAura.RotMode rm = this.rotMode.getValue();
               if (rm == TestAura.RotMode.SILENT) {
                  OyVey.rotationManager.setPlayerRotations(this.savedYaw, this.savedPitch);
               }

               if (this.target != null) {
                  float[] rot = this.buildRaw(this.target);
                  this.sentYaw = rot[0];
                  this.sentPitch = rot[1];
                  this.hasSentRot = true;
               }
            } else {
               this.hasSentRot = false;
            }

            this.hasSaved = false;
         }
      }
   }

   private Entity pickTarget() {
      double r = this.reach.getValue();
      return StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), false)
         .filter(e -> e != mc.player && e.isAlive())
         .filter(e -> e instanceof Player || this.mobs.getValue() && e instanceof Mob)
         .filter(e -> mc.player.distanceTo(e) <= r)
         .filter(this::inFov)
         .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
         .orElse(null);
   }

   private boolean inFov(Entity e) {
      float half = this.fov.getValue() / 2.0F;
      if (half >= 180.0F) {
         return true;
      }

      float[] raw = this.rawRot(e.getX(), e.getY() + e.getEyeHeight() * 0.6, e.getZ());
      float dYaw = Math.abs(Mth.wrapDegrees(raw[0] - mc.player.getYRot()));
      return dYaw <= half;
   }

   private float[] buildSmoothedRot(Entity t) {
      float[] raw = this.rawRot(t.getX(), t.getY() + t.getEyeHeight() * 0.6, t.getZ());
      float spd = this.rotSpeed.getValue();
      float dYaw = Mth.wrapDegrees(raw[0] - this.smoothYaw);
      float dPitch = raw[1] - this.smoothPitch;
      this.smoothYaw = this.smoothYaw + Mth.clamp(dYaw, -spd * 4.0F, spd * 4.0F);
      this.smoothPitch = Mth.clamp(this.smoothPitch + Mth.clamp(dPitch, -spd * 2.0F, spd * 2.0F), -90.0F, 90.0F);
      return this.gcdFix.getValue() ? this.applyGcd(this.smoothYaw, this.smoothPitch) : new float[]{this.smoothYaw, this.smoothPitch};
   }

   private float[] buildRaw(Entity t) {
      return this.rawRot(t.getX(), t.getY() + t.getEyeHeight() * 0.6, t.getZ());
   }

   private float[] rawRot(double tx, double ty, double tz) {
      double dX = tx - mc.player.getX();
      double dY = ty - (mc.player.getY() + mc.player.getEyeHeight());
      double dZ = tz - mc.player.getZ();
      double dist = Math.sqrt(dX * dX + dZ * dZ);
      float yaw = (float)(Math.atan2(dZ, dX) * 180.0 / 3.141592653589793) - 90.0F;
      float pitch = (float)(-(Math.atan2(dY, dist) * 180.0 / 3.141592653589793));
      return new float[]{yaw, Mth.clamp(pitch, -90.0F, 90.0F)};
   }

   private float[] applyGcd(float yaw, float pitch) {
      float gcd = this.computeGcd();
      float deltaYaw = Mth.wrapDegrees(yaw - this.sentYaw);
      float deltaPitch = pitch - this.sentPitch;
      deltaYaw = Math.round(deltaYaw / gcd) * gcd;
      deltaPitch = Math.round(deltaPitch / gcd) * gcd;
      return new float[]{this.sentYaw + deltaYaw, Mth.clamp(this.sentPitch + deltaPitch, -90.0F, 90.0F)};
   }

   private float computeGcd() {
      double s = (Double)mc.options.sensitivity().get();
      double f = s * 0.6 + 0.2;
      return (float)(f * f * f * 8.0 * 0.15);
   }

   private void applyMovementCorrection(float serverYaw, float realYaw) {
      mc.player.zza = mc.player.input.getMoveVector().y;
      mc.player.xxa = mc.player.input.getMoveVector().x;
   }

   private boolean shouldAttack() {
      return this.cooldown.getValue() ? mc.player.getAttackStrengthScale(0.0F) >= 0.9F : System.currentTimeMillis() - this.lastAttack >= this.attackDelay;
   }

   private long rollDelay() {
      int lo = Math.min(this.cpsMin.getValue(), this.cpsMax.getValue());
      int hi = Math.max(this.cpsMin.getValue(), this.cpsMax.getValue());
      float cps = lo + ThreadLocalRandom.current().nextFloat() * (hi - lo);
      return (long)(1000.0F / Math.max(1.0F, cps));
   }

   public enum RotMode {
      VISIBLE,
      SILENT,
      HYBRID;
   }
}
