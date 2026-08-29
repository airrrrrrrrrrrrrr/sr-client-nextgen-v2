package me.alpha432.oyvey.mixin.render;

import me.alpha432.oyvey.features.modules.render.Ambience;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public class MixinSkyRenderer {
   @Inject(
      method = "extractRenderState(Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/state/level/SkyRenderState;)V",
      at = @At("RETURN")
   )
   private void oyvey$overrideSkyAngles(ClientLevel level, float partialTick, Camera camera, SkyRenderState state, CallbackInfo ci) {
      Ambience a = Ambience.INSTANCE;
      if (a != null && a.isEnabled() && a.customTime.getValue()) {
         float angle = (float)(a.time.getValue().intValue() / 24000.0 * 3.141592653589793 * 2.0);
         state.sunAngle = angle;
         state.moonAngle = angle + 3.1415927F;
         state.starAngle = angle;
      }
   }
}
