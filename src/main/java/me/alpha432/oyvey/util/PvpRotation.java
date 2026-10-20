package me.alpha432.oyvey.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Acceleration-limited turning and a short, bounded tracking lead inside the head hitbox. */
public final class PvpRotation {
   private float yawSpeed, pitchSpeed;

   public void reset() { yawSpeed = pitchSpeed = 0; }

   public static SilentRotationMath.Rotation instant(float yaw,float pitch,float targetYaw,float targetPitch,
      float jitterYaw,float jitterPitch,double sensitivity) {
      float desiredYaw=yaw+Mth.wrapDegrees(targetYaw+Math.clamp(jitterYaw,-1.25F,1.25F)-yaw);
      float desiredPitch=Mth.clamp(targetPitch+Math.clamp(jitterPitch,-.65F,.65F),-90,90);
      return SilentRotationMath.quantizeRotation(yaw,pitch,desiredYaw,desiredPitch,sensitivity);
   }

   public SilentRotationMath.Rotation step(float yaw, float pitch, float targetYaw, float targetPitch, float limit, double sensitivity) {
      float max = Math.clamp(limit, 1, 180);
      float yawError = Mth.wrapDegrees(targetYaw - yaw);
      float pitchError = Math.clamp(targetPitch, -90, 90) - pitch;
      yawSpeed = velocity(yawSpeed, yawError, max);
      pitchSpeed = velocity(pitchSpeed, pitchError, Math.min(max, 18));
      return SilentRotationMath.quantizeRotation(yaw, pitch, yaw + yawSpeed, pitch + pitchSpeed, sensitivity);
   }

   private static float velocity(float current, float error, float max) {
      float acceleration = Math.min(4, max * 0.2F);
      float braking = (float)Math.sqrt(2 * acceleration * Math.abs(error)) * 0.7F;
      float desired = Math.copySign(Math.min(max, Math.min(Math.abs(error) * 0.42F, braking)), error);
      float next = current + Math.clamp(desired - current, -acceleration, acceleration);
      // Stop at the point instead of carrying angular momentum through it.
      if (Math.signum(next) != Math.signum(error)) next = 0;
      return Math.copySign(Math.min(Math.abs(next), Math.abs(error)), error);
   }

   public static Vec3 trackingPoint(AABB box, double eyeY, Vec3 targetMotion, Vec3 playerMotion) {
      Vec3 head = CombatAim.headCenter(box, Math.min(eyeY, box.minY + box.getYsize() * 0.86));
      Vec3 relative = targetMotion.subtract(playerMotion).scale(0.5);
      double xMargin = Math.min(0.12, box.getXsize() * 0.2), zMargin = Math.min(0.12, box.getZsize() * 0.2);
      return new Vec3(head.x + Math.clamp(relative.x, -xMargin, xMargin),
         Math.clamp(head.y + Math.clamp(relative.y, -0.07, 0.07), box.minY + box.getYsize() * 0.5, box.maxY - box.getYsize() * 0.02),
         head.z + Math.clamp(relative.z, -zMargin, zMargin));
   }
}
