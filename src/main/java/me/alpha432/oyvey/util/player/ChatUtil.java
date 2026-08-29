package me.alpha432.oyvey.util.player;

import java.nio.charset.StandardCharsets;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.ducks.render.IChatComponent;
import me.alpha432.oyvey.features.commands.Command;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Style;

public class ChatUtil {
   public static void sendMessage(Component message, String identifier) {
      sendClientSideMessage(
         Component.empty()
            .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))
            .append("<")
            .append(getClientNameComponent())
            .append(">")
            .append(" ")
            .append(message),
         identifier
      );
   }

   public static void sendClientSideMessage(Component message, String identifier) {
      if (!Command.nullCheck()) {
         IChatComponent chat = (IChatComponent)Util.mc.gui.getChat();
         MessageSignature signature = new MessageSignature(get256Bytes(identifier));
         chat.oyvey$removeMessage(signature);
         chat.oyvey$addMessage(new GuiMessage(Util.mc.gui.getGuiTicks(), message, signature, GuiMessageSource.SYSTEM_CLIENT, getMessageTag()));
      }
   }

   public static Component getClientNameComponent() {
      return Component.empty().withColor(OyVey.colorManager.getColorAsInt()).append("sr client");
   }

   private static GuiMessageTag getMessageTag() {
      return new GuiMessageTag(OyVey.colorManager.getColorAsInt(), null, null, null);
   }

   private static byte[] get256Bytes(String identifier) {
      byte[] bytes = new byte[256];
      byte[] identifierBytes = identifier.getBytes(StandardCharsets.UTF_8);
      System.arraycopy(identifierBytes, 0, bytes, 0, Math.min(bytes.length, identifierBytes.length));
      return bytes;
   }
}
