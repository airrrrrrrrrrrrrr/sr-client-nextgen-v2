package me.alpha432.oyvey.util.render;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/** Font resources shared by layout and drawing; custom TTF providers use Skija. */
public enum HudFont {
   MINECRAFT, ARIAL, VERDANA, TAHOMA, CONSOLAS, SEGOE, COMIC, GEORGIA, TREBUCHET, COURIER;

   public MutableComponent text(String text) {
      MutableComponent component = Component.literal(text);
      if (this != MINECRAFT) {
         component.withStyle(style -> style.withFont(new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("oyvey", "hud/" + this.name().toLowerCase(Locale.ROOT)))));
      }
      return component;
   }
}
