package me.alpha432.oyvey.features.modules.hud;

import java.awt.Color;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.client.HudModule;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class WatermarkHudModule extends HudModule {
   public final Setting<Boolean> rainbow = this.register(new Setting<>("Rainbow", true));
   public final Setting<Integer> r = this.register(new Setting<>("Red", 100, 0, 255, v -> !this.rainbow.getValue()));
   public final Setting<Integer> g = this.register(new Setting<>("Green", 180, 0, 255, v -> !this.rainbow.getValue()));
   public final Setting<Integer> b = this.register(new Setting<>("Blue", 255, 0, 255, v -> !this.rainbow.getValue()));

   public WatermarkHudModule() {
      super("Watermark", "Draggable horizontal watermark.", 120.0F, 14.0F);
   }

   @Override
   protected void render(Render2DEvent e) {
      super.render(e);
      if (!nullCheck()) {
         GuiGraphicsExtractor gfx = e.getContext();
         long now = System.currentTimeMillis();
         String clientText = "SR Client";
         String sep = "  \u00b7  ";
         String userText = mc.getUser().getName();
         int cw = mc.font.width(clientText);
         int sw = mc.font.width(sep);
         int uw = mc.font.width(userText);
         int accentBarW = 2;
         int padX = 5;
         int padY = 3;
         int totalW = accentBarW + padX + cw + sw + uw + padX;
         int totalH = padY + 9 + padY;
         int px = (int)this.getX();
         int py = (int)this.getY();
         int accent = this.accentColor(now);
         RenderUtil.roundedRect(gfx, px, py, totalW, totalH, 3, 1426458128);
         gfx.fill(px, py + 3, px + accentBarW, py + totalH - 3, accent);
         int tx = px + accentBarW + padX;
         int ty = py + padY;
         gfx.text(mc.font, clientText, tx, ty, accent, true);
         gfx.text(mc.font, sep, tx + cw, ty, -1996488705, false);
         gfx.text(mc.font, userText, tx + cw + sw, ty, -2236963, true);
         this.setWidth(totalW);
         this.setHeight(totalH);
      }
   }

   private int accentColor(long now) {
      if (this.rainbow.getValue()) {
         float hue = (float)(now % 3600L) / 3600.0F;
         return Color.getHSBColor(hue, 1.0F, 1.0F).getRGB() | 0xFF000000;
      } else {
         return new Color(this.r.getValue(), this.g.getValue(), this.b.getValue()).getRGB() | 0xFF000000;
      }
   }
}
