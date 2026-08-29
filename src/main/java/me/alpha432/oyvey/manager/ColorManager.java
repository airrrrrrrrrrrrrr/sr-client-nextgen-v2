package me.alpha432.oyvey.manager;

import java.awt.Color;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import me.alpha432.oyvey.util.ColorUtil;

public class ColorManager {
   private Color color = new Color(0, 0, 255, 180);

   public void init() {
      ClickGuiModule ui = ClickGuiModule.getInstance();
      this.setColor(ui.color.getValue());
   }

   public Color getColor() {
      return this.color;
   }

   public void setColor(Color color) {
      this.color = color;
   }

   public int getColorAsInt() {
      return this.color.getRGB();
   }

   public int getColorAsIntFullAlpha() {
      return new Color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), 255).getRGB();
   }

   public int getColorWithAlpha(float offset, int alpha) {
      return ClickGuiModule.getInstance().rainbow.getValue()
         ? ColorUtil.rainbow((int)(offset / 10.0F * ClickGuiModule.getInstance().rainbowHue.getValue().intValue())).getRGB()
         : new Color(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), alpha).getRGB();
   }
}
