package me.alpha432.oyvey.ducks.render;

import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.MessageSignature;

public interface IChatComponent {
   void oyvey$addMessage(GuiMessage var1);

   void oyvey$removeMessage(MessageSignature var1);
}
