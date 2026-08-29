package me.alpha432.oyvey.mixin.render.gui;

import me.alpha432.oyvey.features.gui.CustomServerListScreen;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class MixinJoinMultiplayerScreen {
   @Inject(method = "init", at = @At("TAIL"))
   private void srclient$redirectToCustom(CallbackInfo ci) {
      ClickGuiModule cfg = ClickGuiModule.getInstance();
      if (cfg == null || cfg.customMenu.getValue()) {
         Minecraft mc = Minecraft.getInstance();
         mc.execute(() -> mc.setScreen(new CustomServerListScreen()));
      }
   }
}
