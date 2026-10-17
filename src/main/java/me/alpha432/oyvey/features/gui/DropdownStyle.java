package me.alpha432.oyvey.features.gui;

import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

/** Shared presentation and inverse input transform for the dropdown screen. */
final class DropdownStyle {
   static final int PANEL_WIDTH = 132;
   static final int COLUMNS = 5;
   static final int GAP = 12;
   static final int MAX_HEIGHT = 480;

   private DropdownStyle() {}

   static int totalWidth() {
      return COLUMNS * (PANEL_WIDTH + GAP) - GAP;
   }

   static float fitScale(int width, int height, float requested) {
      float fit = Math.min((width - 32.0F) / totalWidth(), (height - 32.0F) / 180.0F);
      return Math.max(0.1F, Math.min(requested, fit));
   }

   static int availableHeight(int height, float scale) {
      return Math.max(180, Math.min(MAX_HEIGHT, (int)((height - 32) / scale)));
   }

   static Matrix3x2f transform(float centerX, float centerY, float scale) {
      return new Matrix3x2f().translate(centerX, centerY).scale(scale).translate(-centerX, -centerY);
   }

   static Vector2f mouse(double x, double y, float centerX, float centerY, float scale) {
      return transform(centerX, centerY, scale).invert().transformPosition(new Vector2f((float)x, (float)y));
   }

   static void background(GuiGraphicsExtractor ctx, int width, int height, float opacity) {
      int alpha = Math.round(opacity * 210);
      RenderUtil.gradient(ctx, 0, 0, width, height,
         tint(0x08090C, alpha), tint(0x08090C, alpha), tint(0x08090C, alpha), tint(0x08090C, alpha));
   }

   private static int tint(int rgb, int alpha) {
      return alpha << 24 | rgb;
   }

   static void line(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int color) {
      int dx = Math.abs(x2 - x1), sx = x1 < x2 ? 1 : -1;
      int dy = -Math.abs(y2 - y1), sy = y1 < y2 ? 1 : -1;
      int error = dx + dy;
      while (true) {
         ctx.fill(x1, y1, x1 + 1, y1 + 1, color);
         if (x1 == x2 && y1 == y2) break;
         int twice = error * 2;
         if (twice >= dy) { error += dy; x1 += sx; }
         if (twice <= dx) { error += dx; y1 += sy; }
      }
   }

   static void icon(GuiGraphicsExtractor ctx, int x, int y, int kind) {
      int c = 0xFFD2D3D8;
      switch (kind) {
         case 0 -> { // Shield.
            line(ctx, x + 2, y + 1, x + 8, y + 1, c);
            line(ctx, x + 1, y + 2, x + 2, y + 7, c);
            line(ctx, x + 9, y + 2, x + 8, y + 7, c);
            line(ctx, x + 2, y + 7, x + 5, y + 10, c);
            line(ctx, x + 5, y + 10, x + 8, y + 7, c);
         }
         case 1 -> { // Runner.
            ctx.fill(x + 6, y, x + 8, y + 2, c);
            line(ctx, x + 5, y + 3, x + 7, y + 6, c);
            line(ctx, x + 5, y + 3, x + 2, y + 5, c);
            line(ctx, x + 6, y + 5, x + 3, y + 8, c);
            line(ctx, x + 3, y + 8, x, y + 8, c);
            line(ctx, x + 6, y + 6, x + 8, y + 7, c);
            line(ctx, x + 8, y + 7, x + 7, y + 10, c);
         }
         case 2 -> { // Eye.
            line(ctx, x, y + 5, x + 4, y + 2, c);
            line(ctx, x + 4, y + 2, x + 7, y + 2, c);
            line(ctx, x + 7, y + 2, x + 11, y + 5, c);
            line(ctx, x, y + 5, x + 4, y + 8, c);
            line(ctx, x + 4, y + 8, x + 7, y + 8, c);
            line(ctx, x + 7, y + 8, x + 11, y + 5, c);
            ctx.fill(x + 5, y + 4, x + 7, y + 7, c);
         }
         case 3 -> { // Player.
            RenderUtil.roundedOutline(ctx, x + 3, y, 5, 5, 2, 1, c);
            line(ctx, x + 1, y + 10, x + 2, y + 7, c);
            line(ctx, x + 2, y + 7, x + 9, y + 7, c);
            line(ctx, x + 9, y + 7, x + 10, y + 10, c);
         }
         case 4 -> { // Tools.
            line(ctx, x + 1, y + 1, x + 10, y + 10, c);
            line(ctx, x + 1, y + 10, x + 10, y + 1, c);
            line(ctx, x + 8, y, x + 11, y + 3, c);
            line(ctx, x, y + 8, x + 3, y + 11, c);
         }
         case 5 -> { // Ghost.
            RenderUtil.roundedOutline(ctx, x + 1, y + 1, 9, 9, 4, 1, c);
            ctx.fill(x + 3, y + 4, x + 4, y + 6, c);
            ctx.fill(x + 7, y + 4, x + 8, y + 6, c);
         }
         default -> {
            for (int i = 0; i < 3; i++) line(ctx, x + 1, y + 2 + i * 3, x + 10, y + 2 + i * 3, c);
         }
      }
   }
}
