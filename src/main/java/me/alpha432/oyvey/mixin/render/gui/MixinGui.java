package me.alpha432.oyvey.mixin.render.gui;

import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGui {
   @Inject(method = "extractRenderState", at = @At("RETURN"))
   public void extractRenderState(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
      if (!Minecraft.getInstance().gui.getDebugOverlay().showDebugScreen()) {
         Render2DEvent event = new Render2DEvent(context, tickCounter.getGameTimeDeltaPartialTick(true));
         Util.EVENT_BUS.post(event);
      }
   }
}
