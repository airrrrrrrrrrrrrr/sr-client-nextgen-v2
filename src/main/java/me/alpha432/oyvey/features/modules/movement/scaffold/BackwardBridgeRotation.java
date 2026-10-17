package me.alpha432.oyvey.features.modules.movement.scaffold;

import me.alpha432.oyvey.util.SilentRotationMath;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;

/** A continuous bridge look, independent of individual placement targets. */
public final class BackwardBridgeRotation {
   private boolean initialized;
   private float heading;

   public void reset() {
      this.initialized = false;
   }

   public float heading(Input keys, float cameraYaw) {
      int forward = (keys.forward() ? 1 : 0) - (keys.backward() ? 1 : 0);
      int sideways = (keys.left() ? 1 : 0) - (keys.right() ? 1 : 0);
      if (forward != 0 || sideways != 0) {
         // A fixed half-degree offset avoids rays running exactly along voxel
         // corners on diagonal bridges. It never changes per placement.
         float movementYaw = cameraYaw - (float)Math.toDegrees(Math.atan2(sideways, forward));
         this.heading = Math.round(movementYaw / 45.0F) * 45.0F + 180.5F;
         this.initialized = true;
      } else if (!this.initialized) {
         this.heading = Math.round(cameraYaw / 45.0F) * 45.0F + 180.5F;
         this.initialized = true;
      }
      return this.heading;
   }

   public SilentRotationMath.Rotation next(Input keys, float cameraYaw, float sentYaw, float sentPitch,
      float desiredPitch, double sensitivity) {
      // Ease toward the heading instead of repeating a fixed angular step.
      float remaining = Mth.wrapDegrees(this.heading(keys, cameraYaw) - sentYaw);
      float yaw = sentYaw + remaining * (Math.abs(remaining) <= 2 ? 1 : 0.35F);
      float pitch = sentPitch + Mth.clamp(desiredPitch - sentPitch, -10, 10);
      return SilentRotationMath.quantizeRotation(sentYaw, sentPitch, yaw, pitch, sensitivity);
   }
}
