package me.alpha432.oyvey.util;

import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class RotationUtils implements Util {
   public static float[] lookingAt(Vec3 point, Vec3 from) {
      return fromRotationVec(point.subtract(from));
   }

   public static float[] fromRotationVec(Vec3 vec) {
      return fromRotationVec(vec.x, vec.y, vec.z);
   }

   public static float[] fromRotationVec(double dx, double dy, double dz) {
      float yaw = Mth.wrapDegrees((float)(Math.atan2(dz, dx) * 57.29577951308232) - 90.0F);
      float pitch = Mth.wrapDegrees((float)(-Math.atan2(dy, Math.hypot(dx, dz)) * 57.29577951308232));
      return new float[]{yaw, pitch};
   }

   public static double getGcdStep() {
      double f = (Double)mc.options.sensitivity().get() * 0.6 + 0.2;
      return f * f * f * 8.0 * 0.15;
   }

   public static float angleDifference(float a, float b) {
      return Mth.wrapDegrees(a - b);
   }

   public static float angleTo(float[] from, float[] to) {
      float dy = angleDifference(to[0], from[0]);
      float dp = angleDifference(to[1], from[1]);
      return Math.min((float)Math.hypot(dy, dp), 180.0F);
   }

   public static float[] rotationDeltaTo(float[] from, float[] to) {
      return new float[]{angleDifference(to[0], from[0]), angleDifference(to[1], from[1])};
   }

   public static float[] towardsLinear(float[] from, float[] to, float hFactor, float vFactor) {
      float[] delta = rotationDeltaTo(from, to);
      float len = (float)Math.hypot(delta[0], delta[1]);
      if (len == 0.0F) {
         return (float[])from.clone();
      }

      float straightYaw = Math.abs(delta[0] / len) * hFactor;
      float straightPitch = Math.abs(delta[1] / len) * vFactor;
      return new float[]{from[0] + Mth.clamp(delta[0], -straightYaw, straightYaw), from[1] + Mth.clamp(delta[1], -straightPitch, straightPitch)};
   }

   public static float[] normalizeGcd(float[] target, float curYaw, float curPitch) {
      double gcd = getGcdStep();
      float dy = angleDifference(target[0], curYaw);
      float dp = angleDifference(target[1], curPitch);
      float g1 = (float)(Math.round(dy / gcd) * gcd);
      float g2 = (float)(Math.round(dp / gcd) * gcd);
      return new float[]{curYaw + g1, Mth.clamp(curPitch + g2, -90.0F, 90.0F)};
   }

   public static float crosshairAngleToEntity(Entity entity) {
      if (mc.player == null) {
         return 0.0F;
      }

      Vec3 eyes = mc.player.getEyePosition();
      Vec3 entityCenter = entity.getBoundingBox().getCenter();
      float[] toEntity = fromRotationVec(entityCenter.subtract(eyes));
      float[] playerRot = new float[]{mc.player.getYRot(), mc.player.getXRot()};
      return angleTo(playerRot, toEntity);
   }

   @Deprecated
   public static float[] getRotationToVector(Vec3 vec) {
      return fromRotationVec(vec);
   }

   @Deprecated
   public static float applyGcd(float angle, double step) {
      return step <= 0.0 ? angle : (float)(angle - angle % step);
   }
}
