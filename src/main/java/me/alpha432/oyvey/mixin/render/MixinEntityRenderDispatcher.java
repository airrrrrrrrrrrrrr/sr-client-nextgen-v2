package me.alpha432.oyvey.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.render.Chams;
import me.alpha432.oyvey.features.modules.movement.Scaffold;
import me.alpha432.oyvey.features.modules.movement.scaffold.SilentHeadPose;
import me.alpha432.oyvey.manager.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
   @Unique
   private float oyvey$savedBodyRot;
   @Unique
   private float oyvey$savedBodyRotO;
   @Unique
   private float oyvey$savedHeadRot;
   @Unique
   private float oyvey$savedHeadRotO;
   @Unique
   private float oyvey$savedXRot;
   @Unique
   private float oyvey$savedXRotO;
   @Unique
   private boolean oyvey$didSwapRot = false;

   @Inject(method = "extractEntity", at = @At("HEAD"))
   private void oyvey$preExtractEntity(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
      this.oyvey$didSwapRot = false;
      if (OyVey.rotationManager != null) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && entity == mc.player && entity instanceof AbstractClientPlayer player) {
            RotationManager rm = OyVey.rotationManager;
            Scaffold scaffold = OyVey.moduleManager == null ? null : OyVey.moduleManager.getModuleByClass(Scaffold.class);
            SilentHeadPose head = scaffold != null && scaffold.hasSilentHeadPose() ? scaffold.getSilentHeadPose() : null;
            if (head != null || rm.isRenderOverrideActive()) {
               float renderYaw = head == null ? rm.getRenderYaw() : head.yaw();
               float prevYaw = head == null ? rm.getPrevRenderYaw() : head.previousYaw();
               float renderPitch = head == null ? rm.getRenderPitch() : head.pitch();
               float prevPitch = head == null ? rm.getPrevRenderPitch() : head.previousPitch();
               this.oyvey$savedBodyRot = player.yBodyRot;
               this.oyvey$savedBodyRotO = player.yBodyRotO;
               this.oyvey$savedHeadRot = player.yHeadRot;
               this.oyvey$savedHeadRotO = player.yHeadRotO;
               this.oyvey$savedXRot = player.getXRot();
               this.oyvey$savedXRotO = player.xRotO;
               // Rotate the entire model for Scaffold. Extraction is render-only;
               // the camera and physics fields are restored immediately below.
               player.yBodyRot = renderYaw;
               player.yBodyRotO = prevYaw;
               player.yHeadRot = renderYaw;
               player.yHeadRotO = prevYaw;
               player.setXRot(renderPitch);
               player.xRotO = prevPitch;
               RotationManager.isExtractingLocal = true;
               this.oyvey$didSwapRot = true;
            }
         }
      }
   }

   @Inject(method = "extractEntity", at = @At("RETURN"))
   private void oyvey$onExtractEntity(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
      EntityRenderState state = (EntityRenderState)cir.getReturnValue();
      if (state != null) {
         Chams.CHAMS_STATES.remove(state);
         if (entity instanceof AbstractClientPlayer && entity != Minecraft.getInstance().player) {
            Chams chams = OyVey.moduleManager.getModuleByClass(Chams.class);
            if (chams != null && chams.isEnabled()) {
               Chams.CHAMS_STATES.add(state);
            }
         }
      }

      if (this.oyvey$didSwapRot) {
         this.oyvey$didSwapRot = false;
         RotationManager.isExtractingLocal = false;
         if (entity instanceof AbstractClientPlayer player) {
            player.yBodyRot = this.oyvey$savedBodyRot;
            player.yBodyRotO = this.oyvey$savedBodyRotO;
            player.yHeadRot = this.oyvey$savedHeadRot;
            player.yHeadRotO = this.oyvey$savedHeadRotO;
            player.setXRot(this.oyvey$savedXRot);
            player.xRotO = this.oyvey$savedXRotO;
         }
      }
   }

   @Inject(method = "submit", at = @At("HEAD"))
   private void onSubmitHead(
      EntityRenderState state,
      CameraRenderState cameraRenderState,
      double x,
      double y,
      double z,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      CallbackInfo ci
   ) {
      Chams.renderingChamsEntity = Chams.CHAMS_STATES.contains(state);
   }

   @Inject(method = "submit", at = @At("RETURN"))
   private void onSubmitReturn(
      EntityRenderState state,
      CameraRenderState cameraRenderState,
      double x,
      double y,
      double z,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      CallbackInfo ci
   ) {
      Chams.renderingChamsEntity = false;
   }
}
