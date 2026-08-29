package me.alpha432.oyvey.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.alpha432.oyvey.features.modules.render.Chams;
import me.alpha432.oyvey.util.render.Layers;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeStorage.class)
public class MixinSubmitNodeStorage {
   private static boolean oyvey$replacing;

   @Inject(
      method = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void onSubmitModel(
      Model model,
      Object state,
      PoseStack poseStack,
      RenderType renderType,
      int lightCoords,
      int overlayCoords,
      int tintedColor,
      TextureAtlasSprite sprite,
      int outlineColor,
      CrumblingOverlay crumblingOverlay,
      CallbackInfo ci
   ) {
      if (!oyvey$replacing && Chams.renderingChamsEntity) {
         oyvey$replacing = true;

         try {
            ((SubmitNodeStorage)(Object)this)
               .submitModel(
                  model, state, poseStack, Layers.chamsDepthBias(renderType), lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay
               );
         } finally {
            oyvey$replacing = false;
         }

         ci.cancel();
      }
   }

   @Inject(method = "submitModelPart", at = @At("HEAD"), cancellable = true)
   private void onSubmitModelPart(
      ModelPart modelPart,
      PoseStack poseStack,
      RenderType renderType,
      int lightCoords,
      int overlayCoords,
      TextureAtlasSprite sprite,
      boolean sheeted,
      boolean hasFoil,
      int tintedColor,
      CrumblingOverlay crumblingOverlay,
      int outlineColor,
      CallbackInfo ci
   ) {
      if (!oyvey$replacing && Chams.renderingChamsEntity) {
         oyvey$replacing = true;

         try {
            ((SubmitNodeStorage)(Object)this)
               .submitModelPart(
                  modelPart,
                  poseStack,
                  Layers.chamsDepthBias(renderType),
                  lightCoords,
                  overlayCoords,
                  sprite,
                  sheeted,
                  hasFoil,
                  tintedColor,
                  crumblingOverlay,
                  outlineColor
               );
         } finally {
            oyvey$replacing = false;
         }

         ci.cancel();
      }
   }
}
