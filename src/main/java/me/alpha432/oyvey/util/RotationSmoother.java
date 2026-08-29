package me.alpha432.oyvey.util;

import net.minecraft.util.Mth;

public final class RotationSmoother {
   private RotationSmoother() {
   }

   public static float shortestDiff(float from, float to) {
      return Mth.wrapDegrees(to - from);
   }

   public static float wrap(float angle) {
      return Mth.wrapDegrees(angle);
   }

   public static float clampPitch(float pitch) {
      return Mth.clamp(pitch, -90.0F, 90.0F);
   }

   public static float lerpAngle(float from, float to, float factor) {
      return from + shortestDiff(from, to) * Mth.clamp(factor, 0.0F, 1.0F);
   }

   public static float stepAngle(float from, float to, float maxDeg) {
      float diff = shortestDiff(from, to);
      return from + Mth.clamp(diff, -Math.abs(maxDeg), Math.abs(maxDeg));
   }

   public static float easeOutAngle(float from, float to, float factor, float power) {
      float clamped = Mth.clamp(factor, 0.0F, 1.0F);
      float eased = 1.0F - (float)Math.pow(1.0 - clamped, Math.max(1.0F, power));
      return lerpAngle(from, to, eased);
   }

   public static float interpolateAngle(float prev, float current, float partialTick) {
      return lerpAngle(prev, current, Mth.clamp(partialTick, 0.0F, 1.0F));
   }
}
