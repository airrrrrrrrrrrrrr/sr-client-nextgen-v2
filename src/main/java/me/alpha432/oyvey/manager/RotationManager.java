package me.alpha432.oyvey.manager;

import me.alpha432.oyvey.util.MathUtil;
import me.alpha432.oyvey.util.RotationSmoother;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RotationManager implements Util {
   private float yaw;
   private float pitch;
   private volatile float serverYaw;
   private volatile float serverPitch;
   private volatile boolean hasServerRot = false;
   public static volatile boolean isExtractingLocal = false;
   private float renderYaw;
   private float renderPitch;
   private float prevRenderYaw;
   private float prevRenderPitch;
   private boolean renderInitialized = false;
   private boolean renderOverrideActive = false;
   private float smoothFactor = 0.3F;
   private float maxBodyDesync = 60.0F;
   private boolean useEasing = true;
   private float easingPower = 2.0F;

   public void setServerRotations(float yaw, float pitch) {
      this.serverYaw = yaw;
      this.serverPitch = Mth.clamp(pitch, -90.0F, 90.0F);
      this.hasServerRot = true;
      this.renderOverrideActive = true;
   }

   public void clearServerRotations() {
      this.hasServerRot = false;
   }

   public boolean tickRenderRotation() {
      if (mc.player == null) {
         return false;
      }

      float targetYaw = this.hasServerRot ? this.serverYaw : mc.player.getYRot();
      float targetPitch = this.hasServerRot ? this.serverPitch : mc.player.getXRot();
      if (!this.renderInitialized) {
         this.renderYaw = mc.player.getYRot();
         this.renderPitch = mc.player.getXRot();
         this.prevRenderYaw = this.renderYaw;
         this.prevRenderPitch = this.renderPitch;
         this.renderInitialized = true;
      }

      if (!this.hasServerRot && !this.renderOverrideActive) {
         this.renderYaw = targetYaw;
         this.renderPitch = targetPitch;
         this.prevRenderYaw = targetYaw;
         this.prevRenderPitch = targetPitch;
         return false;
      }

      this.prevRenderYaw = this.renderYaw;
      this.prevRenderPitch = this.renderPitch;
      float f = Mth.clamp(this.smoothFactor, 0.01F, 1.0F);
      if (this.useEasing) {
         this.renderYaw = RotationSmoother.easeOutAngle(this.renderYaw, targetYaw, f, this.easingPower);
         this.renderPitch = RotationSmoother.easeOutAngle(this.renderPitch, targetPitch, f, this.easingPower);
      } else {
         this.renderYaw = RotationSmoother.lerpAngle(this.renderYaw, targetYaw, f);
         this.renderPitch = RotationSmoother.lerpAngle(this.renderPitch, targetPitch, f);
      }

      this.renderPitch = Mth.clamp(this.renderPitch, -90.0F, 90.0F);
      if (!this.hasServerRot
         && Math.abs(Mth.wrapDegrees(targetYaw - this.renderYaw)) < 0.1F
         && Math.abs(targetPitch - this.renderPitch) < 0.1F) {
         this.renderYaw = targetYaw;
         this.renderPitch = targetPitch;
         this.renderOverrideActive = false;
      }

      return this.renderOverrideActive;
   }

   public float getRenderYaw() {
      return this.renderYaw;
   }

   public float getRenderPitch() {
      return this.renderPitch;
   }

   public float getPrevRenderYaw() {
      return this.prevRenderYaw;
   }

   public float getPrevRenderPitch() {
      return this.prevRenderPitch;
   }

   public float getInterpolatedRenderYaw(float partialTick) {
      return RotationSmoother.interpolateAngle(this.prevRenderYaw, this.renderYaw, partialTick);
   }

   public float getInterpolatedRenderPitch(float partialTick) {
      return Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F), this.prevRenderPitch, this.renderPitch);
   }

   public boolean hasServerRotation() {
      return this.hasServerRot;
   }

   public boolean isRenderOverrideActive() {
      return this.renderOverrideActive;
   }

   public float getServerYaw() {
      return this.serverYaw;
   }

   public float getServerPitch() {
      return this.serverPitch;
   }

   public void setSmoothFactor(float f) {
      this.smoothFactor = Mth.clamp(f, 0.01F, 1.0F);
   }

   public void setMaxBodyDesync(float deg) {
      this.maxBodyDesync = Math.max(0.0F, deg);
   }

   public void setUseEasing(boolean b) {
      this.useEasing = b;
   }

   public void setEasingPower(float p) {
      this.easingPower = Math.max(1.0F, p);
   }

   public float getSmoothFactor() {
      return this.smoothFactor;
   }

   public float getMaxBodyDesync() {
      return this.maxBodyDesync;
   }

   public boolean isUseEasing() {
      return this.useEasing;
   }

   public float getEasingPower() {
      return this.easingPower;
   }

   public void updateRotations() {
      this.yaw = mc.player.getYRot();
      this.pitch = mc.player.getXRot();
   }

   public void restoreRotations() {
      mc.player.setYRot(this.yaw);
      mc.player.yHeadRot = this.yaw;
      mc.player.setXRot(this.pitch);
   }

   public void setPlayerRotations(float yaw, float pitch) {
      mc.player.setYRot(yaw);
      mc.player.yHeadRot = yaw;
      mc.player.setXRot(pitch);
   }

   public void setPlayerYaw(float yaw) {
      mc.player.setYRot(yaw);
      mc.player.yHeadRot = yaw;
   }

   public void setPlayerPitch(float pitch) {
      mc.player.setXRot(pitch);
   }

   public void lookAtPos(BlockPos pos) {
      float[] angle = MathUtil.calcAngle(mc.player.getEyePosition(), new Vec3(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F));
      this.setPlayerRotations(angle[0], angle[1]);
   }

   public void lookAtVec3d(Vec3 vec3d) {
      float[] angle = MathUtil.calcAngle(mc.player.getEyePosition(), vec3d);
      this.setPlayerRotations(angle[0], angle[1]);
   }

   public void lookAtVec3d(double x, double y, double z) {
      this.lookAtVec3d(new Vec3(x, y, z));
   }

   public void lookAtEntity(Entity entity) {
      float[] angle = MathUtil.calcAngle(mc.player.getEyePosition(), entity.getEyePosition());
      this.setPlayerRotations(angle[0], angle[1]);
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }
}
