package me.alpha432.oyvey.mixin.render.gui;

import me.alpha432.oyvey.OyVey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class MixinChatScreen {
   @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
   private void onHandleChatInput(String message, boolean addToRecentChat, CallbackInfo ci) {
      String prefix = OyVey.commandManager.getCommandPrefix();
      if (message.startsWith(prefix)) {
         OyVey.commandManager.onChatSent(message);
         Minecraft.getInstance().setScreen(null);
         ci.cancel();
      }
   }
}
