package me.alpha432.oyvey.features.gui;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.cape.CapeEntry;
import me.alpha432.oyvey.features.cape.CapeTextureLoader;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class CapeScreen extends Screen {
   private static final int CARD_W = 270;
   private static final int ENTRY_H = 20;
   private static final int ENTRY_GAP = 3;
   private static final int MAX_VISIBLE = 7;
   private static final int FH = 18;
   private static final int DEL_W = 14;
   private static final int TOGGLE_W = 28;
   private static final int STARS = 90;
   private final float[] sx = new float[90];
   private final float[] sy = new float[90];
   private final float[] svx = new float[90];
   private final float[] svy = new float[90];
   private final float[] sa = new float[90];
   private final CapeScreen.SimpleField nameField = new CapeScreen.SimpleField(32, "cape name...");
   private final CapeScreen.SimpleField sourceField = new CapeScreen.SimpleField(256, "https://... or file.png");
   private boolean focusSource = false;
   private String statusMsg = "";
   private boolean statusOk = false;
   private int scrollOffset = 0;
   private long openTime;

   public CapeScreen() {
      super(Component.literal("capes"));
   }

   public void init() {
      this.openTime = System.currentTimeMillis();
      this.statusMsg = "";
      Random rng = new Random();

      for (int i = 0; i < 90; i++) {
         this.sx[i] = rng.nextFloat() * this.width;
         this.sy[i] = rng.nextFloat() * this.height;
         float angle = rng.nextFloat() * 6.2831855F;
         float spd = 0.04F + rng.nextFloat() * 0.14F;
         this.svx[i] = (float)Math.cos(angle) * spd;
         this.svy[i] = (float)Math.sin(angle) * spd;
         this.sa[i] = 0.25F + rng.nextFloat() * 0.55F;
      }
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      float age = (float)(System.currentTimeMillis() - this.openTime) / 1000.0F;
      float fadeIn = Mth.clamp(age / 0.6F, 0.0F, 1.0F);
      int accent = OyVey.colorManager.getColorAsInt();
      int aR = accent >> 16 & 0xFF;
      int aG = accent >> 8 & 0xFF;
      int aB = accent & 0xFF;
      g.fill(0, 0, this.width, this.height, -16316657);
      float pulse = (float)(Math.sin(age * 0.7) * 0.5 + 0.5);
      RenderUtil.verticalGradient(g, 0.0F, 0.0F, this.width, this.height * 0.5F, new Color(aR, aG, aB, (int)(12.0F + pulse * 16.0F)), new Color(0, 0, 0, 0));

      for (int i = 0; i < 90; i++) {
         this.sx[i] = this.sx[i] + this.svx[i];
         if (this.sx[i] < 0.0F) {
            this.sx[i] = this.sx[i] + this.width;
         }

         if (this.sx[i] > this.width) {
            this.sx[i] = this.sx[i] - this.width;
         }

         this.sy[i] = this.sy[i] + this.svy[i];
         if (this.sy[i] < 0.0F) {
            this.sy[i] = this.sy[i] + this.height;
         }

         if (this.sy[i] > this.height) {
            this.sy[i] = this.sy[i] - this.height;
         }

         g.fill((int)this.sx[i], (int)this.sy[i], (int)this.sx[i] + 1, (int)this.sy[i] + 1, argb((int)(this.sa[i] * fadeIn * 140.0F), aR, aG, aB));
      }

      String title = "CAPES";
      float titleScale = 2.2F;
      int titleY = (int)(this.height * 0.18F);
      g.pose().pushMatrix();
      g.pose().scale(titleScale, titleScale);
      int tx = (int)((this.width / 2.0F - this.font.width(title) * titleScale / 2.0F) / titleScale);
      int ty = (int)((titleY - 6.0F) / titleScale);

      for (int d = 3; d >= 1; d--) {
         int ga = (int)(fadeIn * 35.0F / d);
         g.text(this.font, title, tx - d, ty, argb(ga, aR, aG, aB), false);
         g.text(this.font, title, tx + d, ty, argb(ga, aR, aG, aB), false);
         g.text(this.font, title, tx, ty - d, argb(ga, aR, aG, aB), false);
         g.text(this.font, title, tx, ty + d, argb(ga, aR, aG, aB), false);
      }

      g.text(this.font, title, tx + 1, ty + 1, argb((int)(fadeIn * 120.0F), 0, 0, 0), false);
      g.text(this.font, title, tx, ty, argb((int)(fadeIn * 255.0F), aR, aG, aB), false);
      g.pose().popMatrix();
      List<CapeEntry> capes = OyVey.capeManager.getCapes();
      int active = OyVey.capeManager.getActiveIndex();
      int visible = Math.min(capes.size(), 7);
      int listH = visible * 23;
      int headerH = 30;
      int addH = 60;
      int addBtnH = 24;
      int CARD_H = headerH + listH + addH + addBtnH + 4;
      int cx = this.width / 2 - 135;
      int cy = (int)(this.height * 0.28F);
      RenderUtil.dropShadow(g, cx, cy, 270, CARD_H, 8.0F, 14.0F, (int)(fadeIn * 145.0F));
      RenderUtil.roundedRectGradient(
         g, cx, cy, 270, CARD_H, 8.0F, argb((int)(fadeIn * 228.0F), 14, 14, 36), argb((int)(fadeIn * 218.0F), 7, 7, 22), true
      );
      RenderUtil.roundedOutline(g, cx, cy, 270, CARD_H, 8.0F, 0.9F, argb((int)(fadeIn * 115.0F), aR, aG, aB));
      RenderUtil.roundedRect(g, cx + 4, cy + 8, 2, Math.max(4, CARD_H - 16), 1, argb((int)(fadeIn * 220.0F), aR, aG, aB));
      String headerText = active >= 0 && active < capes.size() ? "active: " + capes.get(active).name() : "no cape active";
      g.centeredText(this.font, headerText, this.width / 2, cy + 8, argb((int)(fadeIn * 180.0F), aR, aG, aB));
      g.fill(cx + 6, cy + headerH - 2, cx + 270 - 6, cy + headerH - 1, argb((int)(fadeIn * 60.0F), aR, aG, aB));
      int lx = cx + 8;
      int lw = 254;
      int ly = cy + headerH;
      int start = Mth.clamp(this.scrollOffset, 0, Math.max(0, capes.size() - 7));
      int end = Math.min(capes.size(), start + 7);

      for (int i = start; i < end; i++) {
         CapeEntry cape = capes.get(i);
         boolean isActive = i == active;
         int ey = ly + (i - start) * 23;
         boolean rowHov = mx >= lx && mx < lx + lw - 14 - 4 && my >= ey && my < ey + 20;
         boolean delHov = mx >= lx + lw - 14 && mx < lx + lw && my >= ey && my < ey + 20;
         if (mx >= lx + lw - 14 - 28 - 4 && mx < lx + lw - 14 - 4 && my >= ey && my < ey + 20) {
            boolean var64 = true;
         } else {
            boolean ver = false;
         }

         int ebg = isActive
            ? argb((int)(fadeIn * 185.0F), 24, 24, 48)
            : (rowHov ? argb((int)(fadeIn * 145.0F), 20, 20, 38) : argb((int)(fadeIn * 100.0F), 12, 12, 28));
         RenderUtil.roundedRect(g, lx, ey, lw, 20, 2, ebg);
         if (isActive) {
            g.fill(lx, ey + 2, lx + 2, ey + 20 - 2, argb((int)(fadeIn * 220.0F), aR, aG, aB));
         }

         CapeTextureLoader.LoadStatus st = CapeTextureLoader.statusOf(cape.source());

         int dotColor = switch (st) {
            case LOADED -> argb((int)(fadeIn * 220.0F), 80, 215, 100);
            case LOADING -> argb((int)(fadeIn * 220.0F), 220, 195, 50);
            case FAILED -> argb((int)(fadeIn * 220.0F), 255, 75, 75);
            default -> argb((int)(fadeIn * 100.0F), 85, 85, 85);
         };
         g.fill(lx + 4, ey + 8, lx + 8, ey + 12, dotColor);
         int nameColor = isActive
            ? argb((int)(fadeIn * 255.0F), aR, aG, aB)
            : (rowHov ? argb((int)(fadeIn * 230.0F), 221, 238, 255) : argb((int)(fadeIn * 185.0F), 153, 170, 187));
         String display = cape.name();
         int maxW = lw - 14 - 28 - 22;

         while (this.font.width(display) > maxW && display.length() > 1) {
            display = display.substring(0, display.length() - 1);
         }

         if (!display.equals(cape.name())) {
            display = display + "\u2026";
         }

         g.text(this.font, display, lx + 12, ey + 6, nameColor, false);
         int tbx = lx + lw - 14 - 28 - 4;
         int tbg = isActive ? argb((int)(fadeIn * 190.0F), 24, 24, 48) : argb((int)(fadeIn * 130.0F), 10, 10, 28);
         RenderUtil.roundedRect(g, tbx, ey + 3, 28, 14, 2, tbg);
         g.fill(tbx, ey + 3, tbx + 28, ey + 4, argb((int)(fadeIn * (isActive ? 200 : 70)), aR, aG, aB));
         String togLabel = isActive ? "ON" : "OFF";
         int togColor = isActive ? argb((int)(fadeIn * 255.0F), aR, aG, aB) : argb((int)(fadeIn * 140.0F), 102, 119, 136);
         g.centeredText(this.font, togLabel, tbx + 14, ey + 6, togColor);
         int xColor = delHov ? argb((int)(fadeIn * 255.0F), 255, 75, 75) : argb((int)(fadeIn * 130.0F), 119, 68, 68);
         g.centeredText(this.font, "\u2715", lx + lw - 7, ey + 6, xColor);
      }

      if (capes.size() > 7) {
         int barX = cx + 270 - 4;
         int barY = cy + headerH;
         float ratio = 7.0F / capes.size();
         int thumbH = Math.max(10, (int)(listH * ratio));
         int thumbY = barY + (int)((float)(listH - thumbH) * start / (capes.size() - 7));
         g.fill(barX, barY, barX + 2, barY + listH, argb((int)(fadeIn * 40.0F), 255, 255, 255));
         g.fill(barX, thumbY, barX + 2, thumbY + thumbH, argb((int)(fadeIn * 160.0F), aR, aG, aB));
      }

      int fy = cy + headerH + listH + 10;
      g.text(this.font, "Name", lx, fy, argb((int)(fadeIn * 150.0F), aR, aG, aB), false);
      this.drawField(g, this.nameField, lx, fy + 9, lw, 18, mx, my, !this.focusSource, fadeIn, aR, aG, aB);
      int fy2 = fy + 9 + 18 + 6;
      g.text(this.font, "URL or file", lx, fy2, argb((int)(fadeIn * 150.0F), aR, aG, aB), false);
      this.drawField(g, this.sourceField, lx, fy2 + 9, lw, 18, mx, my, this.focusSource, fadeIn, aR, aG, aB);
      int btnY = fy2 + 9 + 18 + 6;
      boolean btnHov = mx >= lx && mx < lx + lw && my >= btnY && my < btnY + 18 - 2;
      int abg = btnHov ? argb((int)(fadeIn * 225.0F), 28, 28, 48) : argb((int)(fadeIn * 165.0F), 11, 11, 28);
      RenderUtil.roundedRect(g, lx, btnY, lw, 16, 3, abg);
      g.fill(lx + 3, btnY, lx + lw - 3, btnY + 1, argb((int)(fadeIn * (btnHov ? 55 : 22)), 255, 255, 255));
      g.fill(lx, btnY + 2, lx + 2, btnY + 18 - 4, argb((int)(fadeIn * (btnHov ? 255 : 185)), aR, aG, aB));
      int addTextColor = btnHov ? argb((int)(fadeIn * 255.0F), aR, aG, aB) : argb((int)(fadeIn * 225.0F), 187, 204, 221);
      g.centeredText(this.font, "Add Cape", lx + lw / 2, btnY + 4, addTextColor);
      if (!this.statusMsg.isEmpty()) {
         int sc = this.statusOk ? argb((int)(fadeIn * 220.0F), 80, 215, 100) : argb((int)(fadeIn * 220.0F), 255, 80, 80);
         g.centeredText(this.font, this.statusMsg, this.width / 2, cy + CARD_H + 6, sc);
      }

      g.text(this.font, "ESC to go back", cx, cy + CARD_H + (this.statusMsg.isEmpty() ? 6 : 16), argb((int)(fadeIn * 90.0F), 68, 85, 102), false);
      String ver = "v2.0";
      g.text(this.font, ver, 4, this.height - 10, argb((int)(fadeIn * 200.0F), 74, 102, 136), false);
   }

   private void drawField(
      GuiGraphicsExtractor g, CapeScreen.SimpleField field, int x, int y, int w, int h, int mx, int my, boolean focused, float fadeIn, int aR, int aG, int aB
   ) {
      int bg = focused ? argb((int)(fadeIn * 210.0F), 20, 20, 40) : argb((int)(fadeIn * 170.0F), 12, 12, 28);
      RenderUtil.roundedRect(g, x, y, w, h, 2, bg);
      int bdrA = focused ? (int)(fadeIn * 200.0F) : (int)(fadeIn * 55.0F);
      g.fill(x, y, x + w, y + 1, argb(bdrA, aR, aG, aB));
      g.fill(x, y + h - 1, x + w, y + h, argb(bdrA / 3, aR, aG, aB));
      boolean showCursor = focused && field.cursorVisible();
      String rendered = field.text + (showCursor ? "|" : "");
      int maxW = w - 8;

      while (this.font.width(rendered) > maxW && rendered.length() > 1) {
         rendered = rendered.substring(1);
      }

      if (field.text.isEmpty() && !focused) {
         g.text(this.font, field.placeholder, x + 4, y + (h - 8) / 2, argb((int)(fadeIn * 70.0F), 170, 187, 204), false);
      } else {
         g.text(this.font, rendered, x + 4, y + (h - 8) / 2, argb((int)(fadeIn * 230.0F), 221, 238, 255), false);
      }
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      if (click.button() != 0) {
         return super.mouseClicked(click, doubled);
      }

      double mx = click.x();
      double my = click.y();
      List<CapeEntry> capes = OyVey.capeManager.getCapes();
      int active = OyVey.capeManager.getActiveIndex();
      int visible = Math.min(capes.size(), 7);
      int listH = visible * 23;
      int headerH = 30;
      int cx = this.width / 2 - 135;
      int cy = (int)(this.height * 0.28F);
      int lx = cx + 8;
      int lw = 254;
      int ly = cy + headerH;
      int start = Mth.clamp(this.scrollOffset, 0, Math.max(0, capes.size() - 7));
      int end = Math.min(capes.size(), start + 7);

      for (int i = start; i < end; i++) {
         int ey = ly + (i - start) * 23;
         if (mx >= lx + lw - 14 && mx < lx + lw && my >= ey && my < ey + 20) {
            OyVey.capeManager.removeCape(i);
            OyVey.configManager.save();
            this.statusMsg = "cape removed";
            this.statusOk = false;
            return true;
         }

         int tbx = lx + lw - 14 - 28 - 4;
         if (mx >= tbx && mx < tbx + 28 && my >= ey && my < ey + 20) {
            if (active == i) {
               OyVey.capeManager.setActiveIndex(-1);
               this.statusMsg = "cape disabled";
            } else {
               OyVey.capeManager.setActiveIndex(i);
               if (CapeTextureLoader.statusOf(capes.get(i).source()) == CapeTextureLoader.LoadStatus.FAILED) {
                  CapeTextureLoader.invalidate(capes.get(i).source());
               }

               CapeTextureLoader.get(capes.get(i).source());
               this.statusMsg = "cape enabled: " + capes.get(i).name();
            }

            this.statusOk = true;
            OyVey.configManager.save();
            return true;
         }
      }

      int addH = 60;
      int fy = cy + headerH + listH + 10;
      int fy2 = fy + 9 + 18 + 6;
      int btnY = fy2 + 9 + 18 + 6;
      if (mx >= lx && mx < lx + lw && my >= fy + 9 && my < fy + 9 + 18) {
         this.focusSource = false;
         return true;
      } else if (mx >= lx && mx < lx + lw && my >= fy2 + 9 && my < fy2 + 9 + 18) {
         this.focusSource = true;
         return true;
      } else if (mx >= lx && mx < lx + lw && my >= btnY && my < btnY + 18 - 2) {
         this.doAdd();
         return true;
      } else {
         return super.mouseClicked(click, doubled);
      }
   }

   public boolean mouseScrolled(double mx, double my, double dx, double dy) {
      int max = Math.max(0, OyVey.capeManager.getCapes().size() - 7);
      this.scrollOffset = Mth.clamp(this.scrollOffset - (int)Math.signum(dy), 0, max);
      return true;
   }

   public boolean keyPressed(KeyEvent input) {
      int key = input.input();
      if (key == 256) {
         this.minecraft.setScreen(new CustomMainMenuScreen());
         return true;
      }

      if (key == 258) {
         this.focusSource = !this.focusSource;
         return true;
      }

      if (key != 257 && key != 335) {
         if (key == 259) {
            this.activeField().backspace();
            return true;
         } else {
            return super.keyPressed(input);
         }
      } else if (!this.focusSource) {
         this.focusSource = true;
         return true;
      } else {
         this.doAdd();
         return true;
      }
   }

   public boolean charTyped(CharacterEvent input) {
      this.activeField().type(input.codepoint());
      return true;
   }

   private CapeScreen.SimpleField activeField() {
      return this.focusSource ? this.sourceField : this.nameField;
   }

   private void doAdd() {
      String name = this.nameField.text.trim();
      String source = this.sourceField.text.trim();
      if (name.isEmpty()) {
         this.statusMsg = "enter a name";
         this.statusOk = false;
      } else if (source.isEmpty()) {
         this.statusMsg = "enter a URL or file name";
         this.statusOk = false;
      } else {
         OyVey.capeManager.addCape(name, source);
         OyVey.configManager.save();
         CapeTextureLoader.get(source);
         this.statusMsg = "added " + name;
         this.statusOk = true;
         this.nameField.text = "";
         this.sourceField.text = "";
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static int argb(int a, int r, int g, int b) {
      return Mth.clamp(a, 0, 255) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
   }

   private static class SimpleField {
      String text = "";
      final String placeholder;
      final int maxLen;
      private long blinkBase = System.currentTimeMillis();

      SimpleField(int maxLen, String placeholder) {
         this.maxLen = maxLen;
         this.placeholder = placeholder;
      }

      void type(int codepoint) {
         if (this.text.length() < this.maxLen) {
            this.text = this.text + new String(Character.toChars(codepoint));
            this.blinkBase = System.currentTimeMillis();
         }
      }

      void backspace() {
         if (!this.text.isEmpty()) {
            this.text = this.text.substring(0, this.text.length() - 1);
            this.blinkBase = System.currentTimeMillis();
         }
      }

      boolean cursorVisible() {
         return (System.currentTimeMillis() - this.blinkBase) % 1000L < 530L;
      }
   }
}
