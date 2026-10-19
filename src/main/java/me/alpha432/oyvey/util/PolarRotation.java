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
   private float yawVelocity, pitchVelocity, speed = .5F, speedGoal, response, acceleration;
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
         acceleration = .35F + random.nextFloat() * .25F;
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
      update(targetId,tick);
      float lo = Math.max(.1F,Math.min(minSpeed,maxSpeed));
      float hi = Math.max(lo,Math.max(minSpeed,maxSpeed));
      float limit = lo + (hi - lo) * speed;
      float dy = Mth.wrapDegrees(targetYaw - sentYaw), dp = Mth.clamp(targetPitch,-90,90) - sentPitch;
      float desiredYaw = Mth.clamp(dy * response,-limit * 4,limit * 4);
      float desiredPitch = Mth.clamp(dp * response,-limit * 2,limit * 2);
      yawVelocity += (desiredYaw - yawVelocity) * acceleration;
      pitchVelocity += (desiredPitch - pitchVelocity) * acceleration;
      float yawStep = Math.copySign(Math.min(Math.abs(dy),Math.max(0,Math.copySign(1,dy) * yawVelocity)),dy);
      float pitchStep = Math.copySign(Math.min(Math.abs(dp),Math.max(0,Math.copySign(1,dp) * pitchVelocity)),dp);
      return new SilentRotationMath.Rotation(sentYaw + Mth.clamp(yawStep,-hi * 4,hi * 4),
         Mth.clamp(sentPitch + Mth.clamp(pitchStep,-hi * 2,hi * 2),-90,90));
   }
}
