package me.alpha432.oyvey.mixin.render.gui;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.gui.CustomServerListScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.DisconnectionDetails;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public class MixinDisconnectedScreen {
   @Shadow
   @Final
   @Mutable
   private Screen parent;

   @Shadow
   @Final
   private DisconnectionDetails details;

   @Inject(method = "<init>", at = @At("TAIL"))
   private void srclient$rerouteParent(CallbackInfo ci) {
      OyVey.LOGGER.warn("[connection] Disconnected: {}", this.details.reason().getString());
      if (this.parent == null || this.parent instanceof TitleScreen || this.parent instanceof JoinMultiplayerScreen) {
         this.parent = new CustomServerListScreen();
      }
   }
}
