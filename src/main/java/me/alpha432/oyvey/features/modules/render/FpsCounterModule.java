package me.alpha432.oyvey.features.modules.render;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.HudModule;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class FpsCounterModule extends HudModule {
   public final Setting<Boolean> coloredFps = this.register(new Setting<>("ColoredFPS", true));
   public final Setting<Boolean> showLabel = this.register(new Setting<>("Label", true));

   public FpsCounterModule() {
      super("FPS Counter", "Displays current FPS.", 50.0F, 14.0F, Module.Category.RENDER);
   }

   @Override
   protected void render(Render2DEvent e) {
      super.render(e);
      if (!nullCheck()) {
         GuiGraphicsExtractor gfx = e.getContext();
         int fps = mc.getFps();
         String label = this.showLabel.getValue() ? "FPS " : "";
         String text = label + fps;
         int textColor;
         if (this.coloredFps.getValue()) {
            if (fps >= 60) {
               textColor = -11141291;
            } else if (fps >= 30) {
               textColor = -171;
            } else {
               textColor = -43691;
            }
         } else {
            textColor = -2236963;
         }

         int accentBarW = 2;
         int padX = 5;
         int padY = 3;
         int tw = mc.font.width(text);
         int totalW = accentBarW + padX + tw + padX;
         int totalH = padY + 9 + padY;
         int px = (int)this.getX();
         int py = (int)this.getY();
         int accent = OyVey.colorManager.getColorAsIntFullAlpha();
         RenderUtil.roundedRect(gfx, px, py, totalW, totalH, 3, 1426458128);
         gfx.fill(px, py + 3, px + accentBarW, py + totalH - 3, accent);
         gfx.text(mc.font, text, px + accentBarW + padX, py + padY, textColor, true);
         this.setWidth(totalW);
         this.setHeight(totalH);
      }
   }
}
