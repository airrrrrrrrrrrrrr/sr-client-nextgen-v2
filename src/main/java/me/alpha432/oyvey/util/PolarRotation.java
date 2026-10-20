package me.alpha432.oyvey.util;

import java.util.random.RandomGenerator;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Varied, accelerating tracking with a drifting aim point inside the head. */
public final class PolarRotation {
   private final RandomGenerator random;
   private int target = -1, lastTick = Integer.MIN_VALUE, changeIn;
   private float yawVelocity, pitchVelocity, speed = .5F, speedGoal, response;
   private double x, y, z, goalX, goalY, goalZ;

   public PolarRotation() { this(ThreadLocalRandom.current()); }
   public PolarRotation(RandomGenerator random) { this.random = random; }

   public void reset() {
      target = -1; lastTick = Integer.MIN_VALUE; changeIn = 0;
      yawVelocity = pitchVelocity = 0; speed = .5F;
      x = y = z = goalX = goalY = goalZ = 0;
   }

   private void update(int targetId, int tick) {
      if (target != targetId || (long)tick - lastTick > 2 || tick < lastTick) {
         reset(); target = targetId;
      }
      if (tick == lastTick) return;
      lastTick = tick;
      if (changeIn-- <= 0) {
         changeIn = 3 + random.nextInt(6);
         speedGoal = random.nextFloat();
         response = .22F + random.nextFloat() * .18F;
         goalX = (random.nextDouble() * 2 - 1) * .28;
         goalZ = (random.nextDouble() * 2 - 1) * .28;
         goalY = (random.nextDouble() * 2 - 1) * .04;
      }
      speed += (speedGoal - speed) * .3F;
      x += (goalX - x) * .28;
      y += (goalY - y) * .28;
      z += (goalZ - z) * .28;
   }

   public Vec3 aimPoint(AABB box, double eyeY, int targetId, int tick) {
      update(targetId,tick);
      Vec3 head = CombatAim.headCenter(box,eyeY + y);
      return new Vec3(head.x + box.getXsize() * x,head.y,head.z + box.getZsize() * z);
   }

   public SilentRotationMath.Rotation step(float sentYaw, float sentPitch, float targetYaw, float targetPitch,
      float minSpeed, float maxSpeed, int targetId, int tick) {
      return step(sentYaw,sentPitch,targetYaw,targetPitch,minSpeed,maxSpeed,.85F,.6F,targetId,tick);
   }

   /** Acceleration and deceleration are independent response fractions per native tick. */
   public SilentRotationMath.Rotation step(float sentYaw, float sentPitch, float targetYaw, float targetPitch,
      float minSpeed, float maxSpeed, float acceleration, float deceleration, int targetId, int tick) {
      update(targetId,tick);
      float dy = Mth.wrapDegrees(targetYaw - sentYaw), dp = Mth.clamp(targetPitch,-90,90) - sentPitch;
      if (acceleration >= 10.0F) {
         yawVelocity = pitchVelocity = 0;
         return new SilentRotationMath.Rotation(sentYaw+dy,Mth.clamp(targetPitch,-90,90));
      }
      // Preserve the original 1-100 tuning, then increase acquisition speed
      // above 100. The GUI's 1000 endpoint is explicitly instantaneous.
      float boost = Math.max(1.0F,acceleration);
      float baseLo = Math.max(.1F,Math.min(minSpeed,maxSpeed));
      float lo = baseLo * boost, hi = Math.max(baseLo,Math.max(minSpeed,maxSpeed)) * boost;
      float limit = lo + (hi - lo) * speed;
      float tracking = Math.min(1.0F,response * boost);
      float desiredYaw = Mth.clamp(dy * tracking,-limit * 4,limit * 4);
      float desiredPitch = Mth.clamp(dp * tracking,-limit * 2,limit * 2);
      yawVelocity = approachVelocity(yawVelocity,desiredYaw,acceleration,deceleration);
      pitchVelocity = approachVelocity(pitchVelocity,desiredPitch,acceleration,deceleration);
      float yawStep = Math.copySign(Math.min(Math.abs(dy),Math.max(0,Math.copySign(1,dy) * yawVelocity)),dy);
      float pitchStep = Math.copySign(Math.min(Math.abs(dp),Math.max(0,Math.copySign(1,dp) * pitchVelocity)),dp);
      return new SilentRotationMath.Rotation(sentYaw + Mth.clamp(yawStep,-hi * 4,hi * 4),
         Mth.clamp(sentPitch + Mth.clamp(pitchStep,-hi * 2,hi * 2),-90,90));
   }

   private static float approachVelocity(float current, float desired, float acceleration, float deceleration) {
      boolean braking = current != 0 && (Math.signum(current) != Math.signum(desired) || Math.abs(desired) < Math.abs(current));
      float response = Mth.clamp(braking ? deceleration : acceleration,.01F,1.0F);
      return current + (desired - current) * response;
   }
}
