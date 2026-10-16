package me.alpha432.oyvey.util.player;

import net.minecraft.world.phys.Vec3;

/** Fixed movement sequence from the supplied Vulcan2 preset. */
public final class Vulcan2Movement {
   private int airTicks;

   public void reset() {
      this.airTicks = 0;
   }

   public Vec3 step(Vec3 velocity, boolean grounded, boolean knockedBack, float yaw, float strafe, float forward) {
      this.airTicks = grounded ? 0 : this.airTicks + 1;
      double inputLength = Math.hypot(strafe, forward);
      if (inputLength < 1.0E-6) {
         return velocity;
      }
      double angle = Math.toRadians(yaw);
      double dx = (strafe * Math.cos(angle) - forward * Math.sin(angle)) / inputLength;
      double dz = (forward * Math.cos(angle) + strafe * Math.sin(angle)) / inputLength;
      if (grounded) {
         return new Vec3(dx * 0.45, 0.42, dz * 0.45);
      }
      if (knockedBack) {
         return velocity;
      }
      double speed = Math.hypot(velocity.x, velocity.z);
      return switch (this.airTicks) {
         case 8 -> new Vec3(velocity.x * 0.6 + dx * speed * 0.4, velocity.y,
            velocity.z * 0.6 + dz * speed * 0.4);
         case 9 -> new Vec3(dx * 0.235, -0.24, dz * 0.235);
         case 10 -> new Vec3(dx * speed, velocity.y, dz * speed);
         default -> velocity;
      };
   }
}
