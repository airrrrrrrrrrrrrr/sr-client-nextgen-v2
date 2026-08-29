package me.alpha432.oyvey.features.modules.player;

import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.StreamSupport;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.combat.KillAura;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.RotationUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class AimAssistModule extends Module {
   public final Setting<Boolean> enabled = this.register(new Setting<>("Enabled", true));
   public final Setting<Double> speed = this.register(new Setting<>("Speed", 2.0, 0.1, 10.0));
   public final Setting<Double> range = this.register(new Setting<>("Range", 4.5, 1.0, 8.0));
   public final Setting<Boolean> onlyTarget = this.register(new Setting<>("OnlyTarget", true));
   public final Setting<Double> vertMultiplier = this.register(new Setting<>("VertMultiplier", 0.75, 0.1, 1.5));
   public final Setting<Double> hitboxJitter = this.register(new Setting<>("HitboxJitter", 0.04, 0.0, 0.2));
   private final Random rng = new Random(new SecureRandom().nextLong());
   private long lastNs = 0L;
   public static volatile boolean simulateClick = false;

   public AimAssistModule() {
      super("AimAssist", "Assist aiming", Module.Category.GHOST);
   }

   @Override
   public void onTick() {
      if (mc.player != null && this.enabled.getValue()) {
         AutoClickerModule autoClicker = OyVey.moduleManager.getModuleByClass(AutoClickerModule.class);
         boolean clickerOn = autoClicker != null && autoClicker.isEnabled();
         if (!this.isClicking() && !clickerOn) {
            this.lastNs = 0L;
            simulateClick = false;
         } else {
            Entity target = this.findTarget();
            if (target == null) {
               this.lastNs = 0L;
               simulateClick = false;
            } else {
               long nowNs = System.nanoTime();
               if (this.lastNs == 0L) {
                  this.lastNs = nowNs;
               }

               double dt = Math.min((nowNs - this.lastNs) / 1.0E9, 0.1);
               this.lastNs = nowNs;
               Vec3 aimPoint = this.nearestHitboxPoint(target);
               double dx = aimPoint.x - mc.player.getX();
               double dy = aimPoint.y - mc.player.getEyeY();
               double dz = aimPoint.z - mc.player.getZ();
               double hDist = Math.sqrt(dx * dx + dz * dz);
               float targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, hDist)));
               float targetYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
               float yawDiff = wrapDegrees(targetYaw - mc.player.getYRot());
               float pitchDiff = wrapDegrees(targetPitch - mc.player.getXRot());
               if (Math.abs(yawDiff) >= 1.0E-4F || Math.abs(pitchDiff) >= 1.0E-4F) {
                  float lerpFactor = (float)(1.0 - Math.exp(-this.speed.getValue() * dt));
                  float newYaw = mc.player.getYRot() + yawDiff * lerpFactor;
                  float newPitch = Mth.clamp(mc.player.getXRot() + pitchDiff * lerpFactor * this.vertMultiplier.getValue().floatValue(), -90.0F, 90.0F);
                  float[] sent = RotationUtils.normalizeGcd(new float[]{newYaw, newPitch}, mc.player.getYRot(), mc.player.getXRot());
                  mc.player.setYRot(sent[0]);
                  mc.player.yHeadRot = sent[0];
                  mc.player.setXRot(sent[1]);
               }

               simulateClick = clickerOn;
            }
         }
      } else {
         simulateClick = false;
      }
   }

   private Vec3 nearestHitboxPoint(Entity target) {
      AABB box = target.getBoundingBox();
      Vec3 eye = mc.player.getEyePosition();
      double nx = clamp(eye.x, box.minX, box.maxX);
      double ny = clamp(eye.y, box.minY, box.maxY);
      double nz = clamp(eye.z, box.minZ, box.maxZ);
      double j = this.hitboxJitter.getValue();
      nx = clamp(nx + this.rng.nextGaussian() * j, box.minX, box.maxX);
      ny = clamp(ny + this.rng.nextGaussian() * j, box.minY, box.maxY);
      nz = clamp(nz + this.rng.nextGaussian() * j, box.minZ, box.maxZ);
      return new Vec3(nx, ny, nz);
   }

   private static float wrapDegrees(float d) {
      while (d > 180.0F) {
         d -= 360.0F;
      }

      while (d < -180.0F) {
         d += 360.0F;
      }

      return d;
   }

   private static double clamp(double v, double lo, double hi) {
      return v < lo ? lo : Math.min(v, hi);
   }

   private boolean isClicking() {
      return GLFW.glfwGetMouseButton(mc.getWindow().handle(), 0) == 1;
   }

   private Entity findTarget() {
      if (this.onlyTarget.getValue()) {
         try {
            KillAura ka = OyVey.moduleManager.getModuleByClass(KillAura.class);
            if (ka != null && ka.isEnabled()) {
               Entity t = ka.getTarget();
               if (t != null && mc.player.distanceTo(t) <= this.range.getValue()) {
                  return t;
               }
            }
         } catch (Exception var5) {
         }
      }

      double maxRange = this.range.getValue();
      double[] best = new double[]{maxRange};
      Entity[] found = new Entity[]{null};
      StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), false).forEach(e -> {
         if (e != mc.player && e instanceof LivingEntity && !e.isRemoved()) {
            double d = mc.player.distanceTo(e);
            if (d < best[0]) {
               best[0] = d;
               found[0] = e;
            }
         }
      });
      return found[0];
   }
}
