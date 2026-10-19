package me.alpha432.oyvey.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.alpha432.oyvey.features.cape.CapeStateModifier;
import me.alpha432.oyvey.features.cape.SrCapeTexture;
import me.alpha432.oyvey.features.modules.render.Chams;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public class MixinCapeLayer {
   @Inject(
      method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
      at = @At("HEAD")
   )
   private void onSubmitHead(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yaw, float pitch, CallbackInfo ci) {
      Chams.renderingClassicCape = true;
      Chams.renderingOwnCape = false;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && state.id == mc.player.getId()) {
         Identifier tex = SrCapeTexture.get();
         if (tex != null) {
            CapeStateModifier.tryApply(state, tex);
            Chams.renderingOwnCape = true;
         }
      }
   }

   @Inject(
      method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
      at = @At("RETURN")
   )
   private void onSubmitReturn(CallbackInfo ci) {
      Chams.renderingClassicCape = false;
      Chams.renderingOwnCape = false;
   }
}
