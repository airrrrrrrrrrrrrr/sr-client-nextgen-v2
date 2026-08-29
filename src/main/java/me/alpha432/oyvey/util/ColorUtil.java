package me.alpha432.oyvey.util;

import java.awt.Color;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;

public class ColorUtil {
   public static Color rainbow(int delay) {
      double rainbowState = Math.ceil((System.currentTimeMillis() + delay) / 20.0);
      return Color.getHSBColor(
         (float)(rainbowState % 360.0 / 360.0),
         ClickGuiModule.getInstance().rainbowSaturation.getValue() / 255.0F,
         ClickGuiModule.getInstance().rainbowBrightness.getValue() / 255.0F
      );
   }

   public static Color rgb(int speedMs, int offsetMs) {
      float hue = (float)((System.currentTimeMillis() + offsetMs) % speedMs) / speedMs;
      return Color.getHSBColor(hue, 1.0F, 1.0F);
   }

   public static Color astolfo(int index, int total, int speedMs, float saturation, float brightness) {
      float hue = ((float)(System.currentTimeMillis() % speedMs) / speedMs + (float)index / Math.max(1, total)) % 1.0F;
      return Color.getHSBColor(hue, saturation, brightness);
   }

   public static Color astolfo(int index, int total, int speedMs) {
      return astolfo(index, total, speedMs, 1.0F, 1.0F);
   }

   public static Color interpolate(Color a, Color b, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      return new Color(
         (int)(a.getRed() + (b.getRed() - a.getRed()) * t),
         (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
         (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
         (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
      );
   }

   public static int interpolate(int argbA, int argbB, float t) {
      return interpolate(new Color(argbA, true), new Color(argbB, true), t).getRGB();
   }

   public static Color hsb(float hue, float sat, float bri) {
      return Color.getHSBColor(hue, sat, bri);
   }

   public static Color withAlpha(Color c, int alpha) {
      return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
   }

   public static int withAlpha(int rgb, int alpha) {
      return Math.max(0, Math.min(255, alpha)) << 24 | rgb & 16777215;
   }
}
