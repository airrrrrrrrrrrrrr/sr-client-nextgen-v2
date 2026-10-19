package me.alpha432.oyvey.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.alpha432.oyvey.features.cape.SrCapeTexture;
import me.alpha432.oyvey.features.modules.render.Chams;
import me.alpha432.oyvey.util.render.Layers;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeCollection.class)
public class MixinSubmitNodeCollection {
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
      if (!oyvey$replacing) {
         // Entity submission also includes armor, held items and resource-pack
         // feature models. Restrict the depth override to the actual player body.
         boolean isChams = Chams.renderingChamsEntity && model instanceof PlayerModel && state instanceof AvatarRenderState;
         Identifier capeTexId = getCapeTexture();
         if (isChams || capeTexId != null) {
            oyvey$replacing = true;

            try {
               RenderType newType = renderType;
               if (capeTexId != null) {
                  newType = Layers.capeCustomTexture(renderType, capeTexId);
               }

               if (isChams) {
                  newType = Layers.chamsDepthBias(newType);
               }

               ((SubmitNodeCollection)(Object)this)
                  .submitModel(model, state, poseStack, newType, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
            } finally {
               oyvey$replacing = false;
            }

            ci.cancel();
         }
      }
   }

   private static Identifier getCapeTexture() {
      if (!Chams.renderingOwnCape) {
         return null;
      } else {
         return SrCapeTexture.get();
      }
   }
}
