package me.alpha432.oyvey.features.gui;

import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class RenderQualityScreen extends Screen {
   public static boolean shownThisSession = false;

   public RenderQualityScreen() {
      super(Component.literal("Render Quality"));
   }

   public boolean isPauseScreen() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   private int[] buttonRect(boolean high) {
      int cx = this.width / 2;
      int cy = this.height / 2;
      int bw = 160;
      int bh = 44;
      int gap = 18;
      int bx = high ? cx - bw - gap / 2 : cx + gap / 2;
      int by = cy - bh / 2 + 8;
      return new int[]{bx, by, bw, bh};
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      g.fill(0, 0, this.width, this.height, -15723752);
      int cx = this.width / 2;
      int cy = this.height / 2;
      g.centeredText(this.font, "Select Render Quality", cx, cy - 56, -1);
      g.centeredText(this.font, "Pick the option that matches your PC.", cx, cy - 42, -6710887);
      this.drawChoice(g, mx, my, true, "High-End", "Smooth, anti-aliased edges");
      this.drawChoice(g, mx, my, false, "Low-End", "Lightweight, sharp edges");
   }

   private void drawChoice(GuiGraphicsExtractor g, int mx, int my, boolean isHigh, String title, String sub) {
      int accent = isHigh ? -13728001 : -7695716;
      int[] b = this.buttonRect(isHigh);
      boolean hov = mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3];
      RenderUtil.roundedRect(g, b[0], b[1], b[2], b[3], 6, hov ? -14013650 : -15066330);
      RenderUtil.roundedRect(g, b[0], b[1], 3, b[3], 1, accent);
      g.centeredText(this.font, title, b[0] + b[2] / 2, b[1] + 10, hov ? accent : -3355444);
      g.centeredText(this.font, sub, b[0] + b[2] / 2, b[1] + 25, -8947849);
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      if (click.button() == 0) {
         double mx = click.x();
         double my = click.y();
         int[] hi = this.buttonRect(true);
         int[] lo = this.buttonRect(false);
         if (mx >= hi[0] && mx < hi[0] + hi[2] && my >= hi[1] && my < hi[1] + hi[3]) {
            this.choose(true);
            return true;
         }

         if (mx >= lo[0] && mx < lo[0] + lo[2] && my >= lo[1] && my < lo[1] + lo[3]) {
            this.choose(false);
            return true;
         }
      }

      return super.mouseClicked(click, doubled);
   }

   private void choose(boolean high) {
      RenderUtil.highQuality = high;
      Minecraft.getInstance().setScreen(new TitleScreen());
   }
}
