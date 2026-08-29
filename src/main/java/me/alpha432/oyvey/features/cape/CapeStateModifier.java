package me.alpha432.oyvey.features.cape;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset.Texture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

public class CapeStateModifier {
   public static void tryApply(AvatarRenderState state, final Identifier capeTextureId) {
      Texture capeAsset = new Texture() {
         public Identifier id() {
            return capeTextureId;
         }

         public Identifier texturePath() {
            return capeTextureId;
         }
      };
      PlayerSkin original = state.skin;
      Texture body = original != null ? original.body() : null;
      Texture elytra = original != null ? original.elytra() : null;
      PlayerModelType model = original != null ? original.model() : PlayerModelType.WIDE;
      state.skin = PlayerSkin.insecure(body, capeAsset, elytra, model);
      state.showCape = true;
   }
}
