package me.alpha432.oyvey.features.modules.movement.scaffold;

import net.minecraft.util.Mth;

/** Render-only pose: never writes the camera, input or network rotation. */
public final class SilentHeadPose {
   private boolean active, initialized;
   private float yaw, pitch, previousYaw, previousPitch;

   public void beginTick() {
      if (!this.active) this.initialized = false;
      this.previousYaw = this.yaw;
      this.previousPitch = this.pitch;
      this.active = false;
   }

   public void aim(float yaw, float pitch, float cameraYaw, float cameraPitch) {
      if (!this.initialized) {
         this.previousYaw = cameraYaw;
         this.previousPitch = cameraPitch;
      }
      this.yaw = this.previousYaw + Mth.wrapDegrees(yaw - this.previousYaw);
      this.pitch = Mth.clamp(pitch, -90.0F, 90.0F);
      this.active = true;
      this.initialized = true;
   }

   public void clear() {
      this.active = false;
      this.initialized = false;
   }

   public boolean isActive() { return this.active; }
   public float yaw() { return this.yaw; }
   public float pitch() { return this.pitch; }
   public float previousYaw() { return this.previousYaw; }
   public float previousPitch() { return this.previousPitch; }

   public float interpolatedYaw(float partialTick) {
      return this.previousYaw + Mth.wrapDegrees(this.yaw - this.previousYaw) * Mth.clamp(partialTick, 0.0F, 1.0F);
   }

   public float interpolatedPitch(float partialTick) {
      return Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F), this.previousPitch, this.pitch);
   }
}
