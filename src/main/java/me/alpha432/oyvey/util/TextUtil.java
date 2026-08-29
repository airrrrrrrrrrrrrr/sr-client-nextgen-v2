package me.alpha432.oyvey.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class TextUtil {
   private static boolean initialized;

   private TextUtil() {
      throw new AssertionError("Can't create an instance of utility class");
   }

   public static void init() {
      if (initialized) {
         throw new IllegalStateException("Formatter is already initialized");
      }

      initialized = true;
   }

   public static MutableComponent text(String content, Object... obj) {
      return obj != null && obj.length != 0 ? Component.literal(applyColorTokens(String.format(content, obj))) : Component.literal(applyColorTokens(content));
   }

   private static String applyColorTokens(String text) {
      return text.replace("{black}", "\u00a70")
         .replace("{dark_blue}", "\u00a71")
         .replace("{dark_green}", "\u00a72")
         .replace("{dark_aqua}", "\u00a73")
         .replace("{dark_red}", "\u00a74")
         .replace("{dark_purple}", "\u00a75")
         .replace("{gold}", "\u00a76")
         .replace("{gray}", "\u00a77")
         .replace("{dark_gray}", "\u00a78")
         .replace("{blue}", "\u00a79")
         .replace("{green}", "\u00a7a")
         .replace("{aqua}", "\u00a7b")
         .replace("{red}", "\u00a7c")
         .replace("{light_purple}", "\u00a7d")
         .replace("{yellow}", "\u00a7e")
         .replace("{white}", "\u00a7f")
         .replace("{reset}", "\u00a7r")
         .replace("{global}", "\u00a7f");
   }
}
