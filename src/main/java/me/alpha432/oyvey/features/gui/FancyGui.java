package me.alpha432.oyvey.features.gui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import me.alpha432.oyvey.features.modules.client.SrMusicModule;
import me.alpha432.oyvey.features.settings.Bind;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class FancyGui extends Screen {
   private static final int W = 720;
   private static final int H = 455;
   private static final int SIDEBAR_W = 128;
   private static final int HEADER_H = 48;
   private static final int CAT_H = 30;
   private static final int CARD_H = 58;
   private static final int CARD_GAP = 4;
   private static final int CONTENT_PAD = 8;
   private static final int SETTING_H = 28;
   private static final int STATUS_H = 18;
   private static final int CONTENT_W = 592;
   private static final int CARD_W = 286;
   private static final int C_BG = -267973611;
   private static final int C_SIDEBAR = -268105712;
   private static final int C_HEADER = -267776230;
   private static final int C_SEP = 419430399;
   private static final int C_HOVER = 385875967;
   private static final int C_TEXT = -2236946;
   private static final int C_SUBTEXT = -12693922;
   private Module.Category selectedCategory;
   private String searchQuery = "";
   private int scrollOffset = 0;
   private Module settingsModule = null;
   private int settingsScroll = 0;
   private Setting<?> draggingSlider = null;
   private int sliderBarX;
   private int sliderBarW;
   private Setting<?> capturingBind = null;
   private boolean musicOpen = false;
   private float musicAnim = 0.0F;
   private int musicQueueScroll = 0;
   private boolean musicAddTyping = false;
   private String musicAddInput = "";
   private boolean musicVolDragging = false;
   private int musicVolBarX;
   private int musicVolBarW;
   private float musicMarqueeOffset = 0.0F;
   private long musicMarqueeLast = 0L;
   private static final int MP_W = 240;
   private static final int MP_H = 330;
   private static final int TAB_W = 14;
   private int mpCtrlBtnY = 0;
   private int mpSfX = 0;
   private int mpRpX = 0;
   private int mpVolBarY = 0;
   private int mpQueueAreaY = 0;
   private int mpAddInputY = 0;
   private int mpAddBtnX = 0;
   private boolean configsOpen = false;
   private List<String> configList = new ArrayList<>();
   private String configSaveName = "";
   private boolean configTyping = false;
   private int configScroll = 0;
   private static final Path CFG_DIR = FabricLoader.getInstance().getGameDir().resolve("oyvey").resolve("configs");
   private int px;
   private int py;
   private long openTime = 0L;

   public FancyGui() {
      super(Component.literal("sr client"));

      for (Module.Category cat : OyVey.moduleManager.getCategories()) {
         if (cat != Module.Category.HUD) {
            this.selectedCategory = cat;
            break;
         }
      }
   }

   public void init() {
      super.init();
      if (this.openTime == 0L) {
         this.openTime = System.currentTimeMillis();
      }
   }

   public boolean isCapturingKeyboardInput() {
      return this.configTyping || this.musicAddTyping || this.capturingBind != null || this.settingsModule == null;
   }

   public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      this.px = (this.width - 720) / 2;
      this.py = (this.height - 455) / 2;
      if (this.draggingSlider != null) {
         long win = Minecraft.getInstance().getWindow().handle();
         if (GLFW.glfwGetMouseButton(win, 0) == 1) {
            this.applySliderDrag(mx);
         } else {
            this.draggingSlider = null;
         }
      }

      int accent = OyVey.colorManager.getColorAsInt();
      int aR = accent >> 16 & 0xFF;
      int aG = accent >> 8 & 0xFF;
      int aB = accent & 0xFF;
      float time = (float)(System.currentTimeMillis() - this.openTime) / 1000.0F;
      float pulse = (float)(Math.sin(time * 1.3) * 0.5 + 0.5);
      g.fill(0, 0, this.width, this.height, -872283128);
      RenderUtil.gradient(g, this.px - 80, this.py + 455 - 150, this.px + 350, this.py + 455 + 100, 0, argb(80, aR / 2, aG / 2, aB / 2), 0, 0);
      RenderUtil.gradient(g, this.px + 720 - 200, this.py - 80, this.px + 720 + 80, this.py + 220, 0, 0, 0, argb(60, aR / 3, aG / 3, aB / 3));
      RenderUtil.gradient(
         g,
         this.px + 360 - 200,
         this.py + 455 - 30,
         this.px + 360 + 200,
         this.py + 455 + 100,
         0,
         argb((int)(20.0F + pulse * 25.0F), aR / 2, aG / 2, aB / 2),
         0,
         0
      );
      g.fill(this.px, this.py, this.px + 720, this.py + 455, -267973611);
      g.fill(this.px, this.py, this.px + 128, this.py + 455, -268105712);
      g.fill(this.px, this.py, this.px + 720, this.py + 48, -267776230);
      g.fill(this.px, this.py + 48 - 1, this.px + 720, this.py + 48, argb(70, aR, aG, aB));
      g.fill(this.px + 128, this.py + 48, this.px + 128 + 1, this.py + 455, 419430399);
      g.fill(this.px, this.py, this.px + 720, this.py + 1, argb(50, aR, aG, aB));
      g.fill(this.px, this.py + 455 - 1, this.px + 720, this.py + 455, 553648127);
      g.fill(this.px, this.py, this.px + 1, this.py + 455, 553648127);
      g.fill(this.px + 720 - 1, this.py, this.px + 720, this.py + 455, 553648127);
      int bracketColor = 0xFF000000 | accent;
      g.fill(this.px, this.py, this.px + 14, this.py + 2, bracketColor);
      g.fill(this.px, this.py, this.px + 2, this.py + 14, bracketColor);
      g.fill(this.px + 720 - 14, this.py, this.px + 720, this.py + 2, bracketColor);
      g.fill(this.px + 720 - 2, this.py, this.px + 720, this.py + 14, bracketColor);
      g.fill(this.px, this.py + 455 - 2, this.px + 14, this.py + 455, bracketColor);
      g.fill(this.px, this.py + 455 - 14, this.px + 2, this.py + 455, bracketColor);
      g.fill(this.px + 720 - 14, this.py + 455 - 2, this.px + 720, this.py + 455, bracketColor);
      g.fill(this.px + 720 - 2, this.py + 455 - 14, this.px + 720, this.py + 455, bracketColor);
      g.fill(this.px, this.py + 455 - 18, this.px + 720, this.py + 455, argb(210, 5, 8, 14));
      g.fill(this.px, this.py + 455 - 18 - 1, this.px + 720, this.py + 455 - 18, argb(30, aR, aG, aB));
      this.drawHeader(g, mx, my, accent, aR, aG, aB, time, pulse);
      this.drawSidebar(g, mx, my, accent, aR, aG, aB);
      int cx = this.px + 128 + 1;
      int cTop = this.py + 48;
      int cH = 389;
      int cw = 591;
      if (this.settingsModule != null) {
         this.drawSettings(g, mx, my, cx, cw, cTop, cH, accent, aR, aG, aB);
      } else {
         this.drawGrid(g, mx, my, cx, cw, cTop, cH, aR, aG, aB, time);
      }

      Font font = Minecraft.getInstance().font;
      List<Module> modules = OyVey.moduleManager.getModules();
      long totalOn = modules.stream().filter(m -> m.isEnabled() && !m.hidden).count();
      g.text(font, "\u00a78" + totalOn + " modules active", this.px + 10, this.py + 455 - 18 + 5, -1, false);
      String tag = "sr client 2.0 // nextgen";
      g.text(font, "\u00a78" + tag, this.px + 720 - font.width(tag) - 10, this.py + 455 - 18 + 5, -1, false);
      int cbW = 54;
      int cbH = 14;
      int cbX = this.px + (720 - cbW) / 2;
      int cbY = this.py + 455 - 18 + (18 - cbH) / 2;
      boolean cbHov = mx >= cbX && mx <= cbX + cbW && my >= cbY && my <= cbY + cbH;
      g.fill(cbX, cbY, cbX + cbW, cbY + cbH, this.configsOpen ? argb(190, aR / 4, aG / 4, aB / 4) : (cbHov ? 872415231 : 587202559));
      g.fill(cbX, cbY, cbX + cbW, cbY + 1, 1090519039);
      g.fill(cbX, cbY, cbX + 1, cbY + cbH, 822083583);
      g.fill(cbX + cbW - 1, cbY, cbX + cbW, cbY + cbH, 822083583);
      String cfgLbl = ClickGuiModule.t("Configs");
      g.text(font, cfgLbl, cbX + (cbW - font.width(cfgLbl)) / 2, cbY + (cbH - 8) / 2, -2236963, false);
      if (this.configsOpen) {
         this.drawConfigsPanel(g, mx, my, aR, aG, aB);
      }

      this.drawMusicTabAndPanel(g, mx, my, accent, aR, aG, aB);
   }

   private void drawHeader(GuiGraphicsExtractor g, int mx, int my, int accent, int aR, int aG, int aB, float time, float pulse) {
      Font font = Minecraft.getInstance().font;
      g.pose().pushMatrix();
      g.pose().scale(1.5F, 1.5F);
      int tx = (int)((this.px + 14) / 1.5F);
      int ty = (int)((this.py + 15) / 1.5F);
      g.text(font, "SR CLIENT", tx + 1, ty + 1, argb(60, 0, 0, 0), false);
      g.text(font, "SR CLIENT", tx, ty, 0xFF000000 | accent, false);
      g.pose().popMatrix();
      RenderUtil.horizontalGradient(
         g, this.px + 14, this.py + 48 - 3, this.px + 90, this.py + 48 - 2, new Color(aR, aG, aB, (int)(100.0F + pulse * 80.0F)), new Color(0, 0, 0, 0)
      );
      long en = OyVey.moduleManager.getModules().stream().filter(m -> m.isEnabled() && !m.hidden).count();
      g.text(font, "\u00a78[\u00a7f" + en + "\u00a78 on]", this.px + 95, this.py + 22, -1, false);
      if (this.settingsModule == null) {
         int sw = 190;
         int sh = 22;
         int sx = this.px + 360 - sw / 2;
         int sy = this.py + (48 - sh) / 2;
         g.fill(sx, sy, sx + sw, sy + sh, 553648127);
         g.fill(sx, sy, sx + 1, sy + sh, 956301311);
         g.fill(sx + sw - 1, sy, sx + sw, sy + sh, 956301311);
         g.fill(sx, sy, sx + sw, sy + 1, 956301311);
         g.fill(sx, sy + sh - 1, sx + sw, sy + sh, 419430399);
         boolean cur = System.currentTimeMillis() % 1000L < 500L;
         String disp = this.searchQuery.isEmpty() ? "\u00a78search modules..." : this.searchQuery + (cur ? "\u00a77|" : "");
         g.text(font, disp, sx + 8, sy + 7, this.searchQuery.isEmpty() ? -13090736 : -1118482, false);
      }
   }

   private void drawSidebar(GuiGraphicsExtractor g, int mx, int my, int accent, int aR, int aG, int aB) {
      Font font = Minecraft.getInstance().font;
      int catY = this.py + 48 + 8;

      for (Module.Category cat : OyVey.moduleManager.getCategories()) {
         if (cat != Module.Category.HUD) {
            boolean sel = cat == this.selectedCategory;
            boolean hov = !sel && this.settingsModule == null && mx >= this.px + 8 && mx <= this.px + 128 - 8 && my >= catY && my <= catY + 30;
            if (sel) {
               RenderUtil.roundedRect(g, this.px + 8, catY, 112, 30, 3, argb(185, aR / 4, aG / 4, aB / 4));
               g.fill(this.px + 8, catY + 4, this.px + 10, catY + 30 - 4, 0xFF000000 | accent);
            } else if (hov) {
               RenderUtil.roundedRect(g, this.px + 8, catY, 112, 30, 3, 385875967);
            }

            String lbl = ClickGuiModule.t(cat.getName());
            int lx = this.px + 8 + (112 - font.width(lbl)) / 2;
            int ly = catY + 11;
            g.text(font, lbl, lx, ly, sel ? 0xFF000000 | accent : (hov ? -2236946 : -12693922), false);
            long enCount = OyVey.moduleManager.getModulesByCategory(cat).stream().filter(m -> m.isEnabled() && !m.hidden).count();
            if (enCount > 0L) {
               int badgeX = this.px + 128 - 22;
               int badgeY = catY + 10;
               RenderUtil.roundedRect(g, badgeX, badgeY, 14, 10, 2, argb(200, aR / 3 + 20, aG / 3 + 20, aB / 3 + 20));
               g.fill(badgeX, badgeY, badgeX + 14, badgeY + 10, argb(160, aR / 2, aG / 2, aB / 2));
               String cnt = String.valueOf(enCount);
               g.text(font, cnt, badgeX + (14 - font.width(cnt)) / 2, badgeY + 1, 0xFF000000 | accent, false);
            }

            catY += 32;
         }
      }
   }

   private void drawGrid(GuiGraphicsExtractor g, int mx, int my, int cx, int cw, int cTop, int cH, int aR, int aG, int aB, float time) {
      for (int gx = cx + 12; gx < cx + cw - 1; gx += 22) {
         for (int gy = cTop + 12; gy < cTop + cH - 1; gy += 22) {
            g.fill(gx, gy, gx + 1, gy + 1, argb(18, aR, aG, aB));
         }
      }

      g.enableScissor(cx, cTop, cx + cw, cTop + cH);
      RenderUtil.verticalGradient(g, cx, cTop, cx + cw, cTop + 10, new Color(0, 0, 0, 50), new Color(0, 0, 0, 0));
      RenderUtil.verticalGradient(g, cx, cTop + cH - 10, cx + cw, cTop + cH, new Color(0, 0, 0, 0), new Color(0, 0, 0, 50));
      List<Module> mods = this.filteredModules();
      int col = 0;
      int rowY = cTop + 8 - this.scrollOffset;

      for (Module mod : mods) {
         int cardX = cx + 8 + col * 290;
         if (rowY + 58 >= cTop && rowY <= cTop + cH) {
            this.drawCard(g, mx, my, mod, cardX, rowY, aR, aG, aB);
         }

         if (++col >= 2) {
            col = 0;
            rowY += 62;
         }
      }

      g.disableScissor();
   }

   private void drawCard(GuiGraphicsExtractor g, int mx, int my, Module mod, int cx, int cy, int aR, int aG, int aB) {
      Font font = Minecraft.getInstance().font;
      boolean en = mod.isEnabled();
      boolean hov = mx >= cx && mx < cx + 286 && my >= cy && my < cy + 58;
      RenderUtil.roundedRect(g, cx, cy, 286, 58, 4, en ? -1156703704 : -2012606952);
      if (hov) {
         RenderUtil.roundedRect(g, cx, cy, 286, 58, 4, 419430399);
      }

      RenderUtil.verticalGradient(g, cx + 1, cy + 1, cx + 286 - 1, cy + 29, new Color(255, 255, 255, en ? 14 : 7), new Color(0, 0, 0, 0));
      if (en) {
         g.fill(cx, cy, cx + 286, cy + 1, OyVey.colorManager.getColorWithAlpha(cy, 200));
         g.fill(cx, cy + 1, cx + 3, cy + 58 - 1, OyVey.colorManager.getColorWithAlpha(cy, 255));
         g.fill(cx + 286 - 1, cy, cx + 286, cy + 58, argb(40, aR, aG, aB));
         g.fill(cx, cy + 58 - 1, cx + 286, cy + 58, argb(40, aR, aG, aB));
      } else {
         g.fill(cx, cy, cx + 286, cy + 1, 587202559);
         g.fill(cx, cy, cx + 1, cy + 58, 369098751);
      }

      g.text(font, mod.getName(), cx + 12, cy + 10, en ? -1 : -2236946, en);
      String desc = mod.getDescription();
      if (desc != null && !desc.isEmpty()) {
         String d = desc;
         int maxW = 242;

         while (font.width(d) > maxW && d.length() > 3) {
            d = d.substring(0, d.length() - 4) + "...";
         }

         g.text(font, d, cx + 12, cy + 23, -12693922, false);
      }

      String info = mod.getDisplayInfo();
      if (info != null) {
         g.text(font, info, cx + 12, cy + 35, 0xFF000000 | OyVey.colorManager.getColorAsInt(), false);
      }

      if (hov) {
         g.text(font, "\u00a78right-click: settings", cx + 12, cy + 58 - 13, 0, false);
      }

      int ptx = cx + 286 - 30;
      int pty = cy + 7;
      if (en) {
         int acc = OyVey.colorManager.getColorWithAlpha(cy, 200);
         g.fill(ptx, pty, ptx + 26, pty + 13, acc);
         g.fill(ptx + 14, pty + 1, ptx + 25, pty + 12, -1);
      } else {
         g.fill(ptx, pty, ptx + 26, pty + 13, 872415231);
         g.fill(ptx + 1, pty + 1, ptx + 11, pty + 12, -10854288);
      }
   }

   private void drawSettings(GuiGraphicsExtractor g, int mx, int my, int cx, int cw, int cTop, int cH, int accent, int aR, int aG, int aB) {
      Font font = Minecraft.getInstance().font;
      int hdrH = 38;
      g.fill(cx, cTop, cx + cw, cTop + cH, -267973869);
      g.fill(cx, cTop, cx + cw, cTop + hdrH, -268105456);
      g.fill(cx, cTop + hdrH - 1, cx + cw, cTop + hdrH, argb(50, aR, aG, aB));
      g.fill(cx, cTop, cx + cw, cTop + 1, 0xFF000000 | accent);
      boolean backHov = mx >= cx + 8 && mx <= cx + 30 && my >= cTop + 8 && my <= cTop + hdrH - 8;
      RenderUtil.roundedRect(g, cx + 8, cTop + 8, 22, hdrH - 16, 3, backHov ? 1157627903 : 587202559);
      g.text(font, "<", cx + 14, cTop + 15, -4473925, false);
      g.text(font, this.settingsModule.getName(), cx + 38, cTop + 11, -1, true);
      String desc = this.settingsModule.getDescription();
      if (desc != null && !desc.isEmpty()) {
         g.text(font, desc, cx + 38, cTop + 23, -12693922, false);
      }

      boolean en = this.settingsModule.isEnabled();
      int tx = cx + cw - 44;
      int ty = cTop + hdrH / 2 - 5;
      if (en) {
         g.fill(tx, ty, tx + 22, ty + 11, OyVey.colorManager.getColorWithAlpha(ty, 200));
         g.fill(tx + 12, ty + 1, tx + 21, ty + 10, -1);
      } else {
         g.fill(tx, ty, tx + 22, ty + 11, 872415231);
         g.fill(tx + 1, ty + 1, tx + 10, ty + 10, -10854288);
      }

      g.text(font, en ? "\u00a7aON" : "\u00a7cOFF", tx - font.width(en ? "ON" : "OFF") - 6, cTop + 15, en ? -11149995 : -2271915, false);
      List<Setting<?>> settings = this.visibleSettings();
      g.enableScissor(cx, cTop + hdrH, cx + cw, cTop + cH);
      int sy = cTop + hdrH + 6 - this.settingsScroll;

      for (Setting<?> s : settings) {
         if (sy + 28 >= cTop + hdrH && sy <= cTop + cH) {
            this.drawSettingRow(g, mx, my, s, cx, sy, cw);
         }

         sy += 29;
      }

      if (settings.isEmpty()) {
         String msg = "No configurable settings";
         g.text(font, msg, cx + (cw - font.width(msg)) / 2, cTop + hdrH + 20, -12693922, false);
      }

      g.disableScissor();
   }

   private void drawSettingRow(GuiGraphicsExtractor g, int mx, int my, Setting<?> s, int cx, int sy, int cw) {
      Font font = Minecraft.getInstance().font;
      boolean hov = mx >= cx && mx <= cx + cw && my >= sy && my <= sy + 28;
      g.fill(cx, sy, cx + cw, sy + 28, hov ? 419430399 : 0);
      g.fill(cx + 8, sy + 28, cx + cw - 8, sy + 28 + 1, 318767103);
      g.text(font, s.getName(), cx + 14, sy + 10, -3355444, false);
      if (s.getValue() instanceof Boolean bool) {
         int tx = cx + cw - 38;
         int ty = sy + 14 - 5;
         if (bool) {
            g.fill(tx, ty, tx + 22, ty + 11, OyVey.colorManager.getColorWithAlpha(sy, 200));
            g.fill(tx + 12, ty + 1, tx + 21, ty + 10, -1);
         } else {
            g.fill(tx, ty, tx + 22, ty + 11, 872415231);
            g.fill(tx + 1, ty + 1, tx + 10, ty + 10, -10854288);
         }
      } else if (s.isNumberSetting() && s.hasRestriction()) {
         float min = ((Number)s.getMin()).floatValue();
         float max = ((Number)s.getMax()).floatValue();
         float val = ((Number)s.getValue()).floatValue();
         float pct = Math.max(0.0F, Math.min(1.0F, (val - min) / (max - min)));
         int barX = cx + (int)(cw * 0.42F);
         int barW = (int)(cw * 0.38F);
         int barY = sy + 14 - 3;
         g.fill(barX, barY, barX + barW, barY + 6, 872415231);
         g.fill(barX, barY, barX + (int)(barW * pct), barY + 6, OyVey.colorManager.getColorWithAlpha(sy, 200));
         int kx = barX + (int)(barW * pct) - 3;
         g.fill(kx, barY - 2, kx + 6, barY + 8, -1);
         String vs = s.getValue() instanceof Integer ? String.valueOf((int)val) : String.format("%.2f", val);
         g.text(font, vs, cx + cw - 8 - font.width(vs), sy + 10, -7829368, false);
      } else if (s.isEnumSetting()) {
         String ev = "< " + s.currentEnumName() + " >";
         g.text(font, ev, cx + cw - 10 - font.width(ev), sy + 10, -10053172, false);
      } else if (s.getValue() instanceof Bind bind) {
         boolean cap = s == this.capturingBind;
         String bs = cap ? "Press a key..." : "[" + bind.toString() + "]";
         g.text(font, bs, cx + cw - 10 - font.width(bs), sy + 10, cap ? OyVey.colorManager.getColorWithAlpha(sy, 255) : -8943463, false);
      } else if (s.isColorSetting()) {
         Color c = (Color)s.getValue();
         int swX = cx + cw - 42;
         int swY = sy + 5;
         int swW = 30;
         int swH = 18;
         g.fill(swX, swY, swX + swW / 2, swY + swH / 2, -5592406);
         g.fill(swX + swW / 2, swY, swX + swW, swY + swH / 2, -10066330);
         g.fill(swX, swY + swH / 2, swX + swW / 2, swY + swH, -10066330);
         g.fill(swX + swW / 2, swY + swH / 2, swX + swW, swY + swH, -5592406);
         g.fill(swX, swY, swX + swW, swY + swH, c.getRGB());
         g.fill(swX - 1, swY - 1, swX + swW + 1, swY, 1627389951);
         g.fill(swX - 1, swY + swH, swX + swW + 1, swY + swH + 1, 1627389951);
         g.fill(swX - 1, swY, swX, swY + swH, 1627389951);
         g.fill(swX + swW, swY, swX + swW + 1, swY + swH, 1627389951);
      }
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      double mx = click.x();
      double my = click.y();
      int btn = click.button();
      int cx = this.px + 128 + 1;
      int cTop = this.py + 48;
      int cH = 389;
      int cw = 591;
      if (this.settingsModule != null) {
         int hdrH = 38;
         if (mx >= cx + 8 && mx <= cx + 30 && my >= cTop + 8 && my <= cTop + hdrH - 8) {
            this.closeSettings();
            return true;
         }

         int tx = cx + cw - 44;
         int ty = cTop + hdrH / 2 - 5;
         if (mx >= tx && mx <= tx + 22 && my >= ty && my <= ty + 11) {
            this.settingsModule.toggle();
            return true;
         }

         List<Setting<?>> settings = this.visibleSettings();
         int sy = cTop + hdrH + 6 - this.settingsScroll;

         for (Setting<?> s : settings) {
            if (my >= sy && my <= sy + 28 && mx >= cx && mx <= cx + cw) {
               this.handleSettingClick(s, mx, sy, cx, cw, btn);
               return true;
            }

            sy += 29;
         }

         if (mx < this.px || mx > this.px + 720 || my < this.py || my > this.py + 455) {
            this.closeSettings();
         }

         return true;
      } else {
         int catY = this.py + 48 + 8;

         for (Module.Category cat : OyVey.moduleManager.getCategories()) {
            if (cat != Module.Category.HUD) {
               if (mx >= this.px + 8 && mx <= this.px + 128 - 8 && my >= catY && my <= catY + 30) {
                  this.selectedCategory = cat;
                  this.scrollOffset = 0;
                  return true;
               }

               catY += 32;
            }
         }

         if (mx >= cx && mx <= cx + cw && my >= cTop && my <= cTop + cH) {
            List<Module> mods = this.filteredModules();
            int col = 0;
            int rowY = cTop + 8 - this.scrollOffset;

            for (Module mod : mods) {
               int cardX = cx + 8 + col * 290;
               if (mx >= cardX && mx < cardX + 286 && my >= rowY && my < rowY + 58) {
                  if (btn == 1) {
                     this.openSettings(mod);
                  } else {
                     mod.toggle();
                  }

                  return true;
               }

               if (++col >= 2) {
                  col = 0;
                  rowY += 62;
               }
            }
         }

         int cbW = 54;
         int cbH = 14;
         int cbX = this.px + (720 - cbW) / 2;
         int cbY = this.py + 455 - 18 + (18 - cbH) / 2;
         if (mx >= cbX && mx <= cbX + cbW && my >= cbY && my <= cbY + cbH) {
            this.configsOpen = !this.configsOpen;
            if (this.configsOpen) {
               this.refreshConfigList();
               this.configScroll = 0;
            }

            return true;
         } else if (this.configsOpen) {
            int margin = 20;
            int headerH = 24;
            int entryH = 18;
            int saveBarH = 36;
            int panW = this.width - margin * 2;
            int panH = this.height - margin * 2;
            int panX = margin;
            int closeW = 20;
            int closeH = 14;
            int closeX = margin + panW - closeW - 4;
            int closeY = margin + (headerH - closeH) / 2;
            if (mx >= closeX && mx <= closeX + closeW && my >= closeY && my <= closeY + closeH) {
               this.configsOpen = false;
               this.configTyping = false;
               return true;
            }

            int listAreaH = panH - headerH - 4 - saveBarH;
            int maxVisible = listAreaH / entryH;
            int ey = margin + headerH + 4;
            int end = Math.min(this.configList.size(), this.configScroll + maxVisible);

            for (int i = this.configScroll; i < end; i++) {
               String cfg = this.configList.get(i);
               int lbX = panX + panW - 48;
               if (mx >= lbX && mx <= lbX + 44 && my >= ey + 2 && my <= ey + entryH - 2) {
                  this.loadConfig(cfg);
                  this.configsOpen = false;
                  return true;
               }

               ey += entryH;
            }

            int saveY = margin + panH - saveBarH;
            int inputW = panW - 100;
            if (mx >= panX + 6 && mx <= panX + 6 + inputW && my >= saveY + 6 && my <= saveY + saveBarH - 6) {
               this.configTyping = true;
               return true;
            }

            int saveBtnX = panX + 6 + inputW + 6;
            int saveBtnW = panW - 6 - inputW - 18;
            if (mx >= saveBtnX && mx <= saveBtnX + saveBtnW && my >= saveY + 6 && my <= saveY + saveBarH - 6) {
               if (!this.configSaveName.isBlank()) {
                  this.saveConfig(this.configSaveName.trim());
                  this.configSaveName = "";
                  this.configTyping = false;
                  this.refreshConfigList();
               }

               return true;
            } else {
               if (mx < panX || mx > panX + panW || my < margin || my > margin + panH) {
                  this.configsOpen = false;
                  this.configTyping = false;
               }

               return true;
            }
         } else {
            SrMusicModule music = SrMusicModule.INSTANCE;
            if (music != null) {
               int tabH = 80;
               int tabX = this.px - 14;
               int tabY2 = this.py + (455 - tabH) / 2;
               if (mx >= tabX && mx <= this.px && my >= tabY2 && my <= tabY2 + tabH) {
                  this.musicOpen = !this.musicOpen;
                  if (!this.musicOpen) {
                     this.musicAddTyping = false;
                     this.musicAddInput = "";
                  }

                  return true;
               }

               if (this.musicAnim > 0.01F) {
                  int panX2 = this.musicPanelX();
                  int panY2 = this.musicPanelY();
                  if (mx >= panX2 && mx <= panX2 + 240 && my >= panY2 && my <= panY2 + 330) {
                     int togX = panX2 + 240 - 32;
                     int togY3 = panY2 + 8;
                     if (mx >= togX && mx <= togX + 22 && my >= togY3 && my <= togY3 + 11) {
                        if (music.isEnabled()) {
                           music.disable();
                        } else {
                           music.enable();
                        }

                        return true;
                     }

                     if (!music.isEnabled()) {
                        return true;
                     }

                     int testBtnX2 = panX2 + 240 - 30;
                     int testBtnY2 = panY2 + 26 + 1 + 4;
                     if (mx >= testBtnX2 && mx <= testBtnX2 + 26 && my >= testBtnY2 && my <= testBtnY2 + 12) {
                        music.runCheck();
                        return true;
                     }

                     int ctrlSpacing = 26;
                     int btnSize = 20;
                     int ctrlStartX = panX2 + (240 - ctrlSpacing * 3) / 2;

                     for (int i = 0; i < 3; i++) {
                        int bx = ctrlStartX + i * ctrlSpacing;
                        if (mx >= bx && mx <= bx + btnSize && my >= this.mpCtrlBtnY && my <= this.mpCtrlBtnY + btnSize) {
                           if (i == 0) {
                              music.prev();
                           } else if (i == 1) {
                              music.togglePlayPause();
                           } else {
                              music.next();
                           }

                           return true;
                        }
                     }

                     if (mx >= this.mpSfX && mx <= this.mpSfX + 18 && my >= this.mpCtrlBtnY + 2 && my <= this.mpCtrlBtnY + 18) {
                        music.shuffleSetting.setValue(!music.isShuffle());
                        return true;
                     }

                     if (mx >= this.mpRpX && mx <= this.mpRpX + 18 && my >= this.mpCtrlBtnY + 2 && my <= this.mpCtrlBtnY + 18) {
                        music.repeatSetting.setValue(!music.isRepeat());
                        return true;
                     }

                     if (mx >= this.musicVolBarX - 4 && mx <= this.musicVolBarX + this.musicVolBarW + 4 && my >= this.mpVolBarY - 4 && my <= this.mpVolBarY + 9
                        )
                      {
                        this.musicVolDragging = true;
                        float pct = Mth.clamp((float)(mx - this.musicVolBarX) / this.musicVolBarW, 0.0F, 1.0F);
                        music.setVolume(pct);
                        return true;
                     }

                     int entH = 16;
                     int eyQ = this.mpQueueAreaY - this.musicQueueScroll;

                     for (int ix = 0; ix < music.getQueueSize(); ix++) {
                        if (my >= eyQ && my <= eyQ + entH) {
                           int rmX2 = panX2 + 240 - 20;
                           if (mx >= rmX2 && mx <= rmX2 + 16) {
                              music.removeAt(ix);
                           } else {
                              music.playAt(ix);
                           }

                           return true;
                        }

                        eyQ += entH;
                     }

                     if (my >= this.mpAddInputY && my <= this.mpAddInputY + 18) {
                        if (mx >= this.mpAddBtnX && mx <= this.mpAddBtnX + 18) {
                           this.commitMusicInput(music);
                        } else {
                           this.musicAddTyping = true;
                        }

                        return true;
                     }

                     return true;
                  }
               }
            }

            return super.mouseClicked(click, doubled);
         }
      }
   }

   public boolean mouseReleased(MouseButtonEvent click) {
      if (click.button() == 0) {
         this.draggingSlider = null;
         this.musicVolDragging = false;
      }

      return super.mouseReleased(click);
   }

   public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
      if (this.musicAnim > 0.01F) {
         int panX2 = this.musicPanelX();
         int panY2 = this.musicPanelY();
         if (mx >= panX2 && mx <= panX2 + 240 && my >= panY2 && my <= panY2 + 330) {
            SrMusicModule music = SrMusicModule.INSTANCE;
            int maxScroll = Math.max(0, (music != null ? music.getQueueSize() : 0) * 16 - 80);
            this.musicQueueScroll = (int)Math.max(0.0, Math.min(maxScroll, this.musicQueueScroll - vAmt * 14.0));
            return true;
         }
      }

      if (this.configsOpen) {
         int margin = 20;
         int headerH = 24;
         int entryH = 18;
         int saveBarH = 36;
         int panH = this.height - margin * 2;
         int listAreaH = panH - headerH - 4 - saveBarH;
         int maxScroll = Math.max(0, this.configList.size() - listAreaH / entryH);
         this.configScroll = Math.max(0, Math.min(maxScroll, this.configScroll - (int)Math.signum(vAmt)));
         return true;
      }

      int cx = this.px + 128 + 1;
      if (mx >= cx && mx <= this.px + 720) {
         if (this.settingsModule != null) {
            this.settingsScroll = Math.max(0, this.settingsScroll - (int)(vAmt * 12.0));
         } else if (my >= this.py + 48 && my <= this.py + 455 - 18) {
            this.scrollOffset = Math.max(0, this.scrollOffset - (int)(vAmt * 14.0));
         }
      }

      return super.mouseScrolled(mx, my, hAmt, vAmt);
   }

   public boolean keyPressed(KeyEvent input) {
      int key = input.input();
      if (this.configTyping) {
         if (key == 256) {
            this.configTyping = false;
            return true;
         } else if (key == 259 && !this.configSaveName.isEmpty()) {
            this.configSaveName = this.configSaveName.substring(0, this.configSaveName.length() - 1);
            return true;
         } else if ((key == 257 || key == 335) && !this.configSaveName.isBlank()) {
            this.saveConfig(this.configSaveName.trim());
            this.configSaveName = "";
            this.configTyping = false;
            this.refreshConfigList();
            return true;
         } else {
            return true;
         }
      } else if (this.capturingBind == null) {
         if (this.musicAddTyping) {
            if (key == 256) {
               this.musicAddTyping = false;
               return true;
            }

            if (key == 259 && !this.musicAddInput.isEmpty()) {
               this.musicAddInput = this.musicAddInput.substring(0, this.musicAddInput.length() - 1);
               return true;
            }

            if (key != 257 && key != 335) {
               return true;
            }

            SrMusicModule music = SrMusicModule.INSTANCE;
            if (music != null) {
               this.commitMusicInput(music);
            }

            return true;
         } else if (this.settingsModule == null && key == 259 && !this.searchQuery.isEmpty()) {
            this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
            return true;
         } else if (key == 256 && this.settingsModule != null) {
            this.closeSettings();
            return true;
         } else {
            return super.keyPressed(input);
         }
      } else {
         Bind bind = key != 256 && key != 261 && key != 259 ? new Bind(key) : new Bind(-1);
         ((Setting<Bind>)this.capturingBind).setValue(bind);
         this.capturingBind = null;
         return true;
      }
   }

   public boolean charTyped(CharacterEvent input) {
      if (this.configTyping) {
         String c = input.codepointAsString();
         if (c.matches("[a-zA-Z0-9_\\-]") && this.configSaveName.length() < 24) {
            this.configSaveName = this.configSaveName + c;
         }

         return true;
      } else if (this.musicAddTyping) {
         String c = input.codepointAsString();
         if (this.musicAddInput.length() < 200) {
            this.musicAddInput = this.musicAddInput + c;
         }

         return true;
      } else {
         if (this.capturingBind != null) {
            return true;
         }

         if (this.settingsModule == null) {
            this.searchQuery = this.searchQuery + input.codepointAsString();
            this.scrollOffset = 0;
         }

         return true;
      }
   }

   private List<Module> filteredModules() {
      List<Module> out = new ArrayList<>();
      if (this.searchQuery.isEmpty()) {
         if (this.selectedCategory != null) {
            OyVey.moduleManager.getModulesByCategory(this.selectedCategory).stream().filter(m -> !m.hidden).forEach(out::add);
         }
      } else {
         String q = this.searchQuery.toLowerCase();
         OyVey.moduleManager.getModules().stream().filter(m -> !m.hidden && m.getName().toLowerCase().contains(q)).forEach(out::add);
      }

      return out;
   }

   private List<Setting<?>> visibleSettings() {
      return this.settingsModule == null
         ? new ArrayList<>()
         : this.settingsModule
            .getSettings()
            .stream()
            .filter(Setting::isVisible)
            .filter(s -> !s.getName().equalsIgnoreCase("Enabled") && !s.getName().equalsIgnoreCase("Drawn") && !s.getName().equalsIgnoreCase("DisplayName"))
            .collect(Collectors.toList());
   }

   private void applySliderDrag(int mx) {
      if (this.draggingSlider != null) {
         float pct = Math.max(0.0F, Math.min(1.0F, (float)(mx - this.sliderBarX) / this.sliderBarW));
         float min = ((Number)this.draggingSlider.getMin()).floatValue();
         float max = ((Number)this.draggingSlider.getMax()).floatValue();
         float nv = min + pct * (max - min);
         Object v = this.draggingSlider.getValue();
         if (v instanceof Integer) {
            ((Setting<Integer>)this.draggingSlider).setValueNoEvent((int)nv);
         } else if (v instanceof Float) {
            ((Setting<Float>)this.draggingSlider).setValueNoEvent(nv);
         } else if (v instanceof Double) {
            ((Setting<Double>)this.draggingSlider).setValueNoEvent((double)nv);
         }
      }
   }

   private void handleSettingClick(Setting<?> s, double mx, int sy, int cx, int cw, int btn) {
      if (s.getValue() instanceof Boolean) {
         ((Setting<Boolean>)s).setValue(!(Boolean)s.getValue());
      } else if (s.isNumberSetting() && s.hasRestriction()) {
         int barX = cx + (int)(cw * 0.42F);
         int barW = (int)(cw * 0.38F);
         if (mx >= barX - 4 && mx <= barX + barW + 4) {
            this.draggingSlider = s;
            this.sliderBarX = barX;
            this.sliderBarW = barW;
            this.applySliderDrag((int)mx);
         }
      } else if (s.isEnumSetting()) {
         s.increaseEnum();
      } else if (s.getValue() instanceof Bind) {
         if (btn == 1) {
            ((Setting<Bind>)s).setValue(new Bind(-1));
            this.capturingBind = null;
         } else {
            this.capturingBind = this.capturingBind == s ? null : s;
         }
      }
   }

   private void commitMusicInput(SrMusicModule music) {
      String input = this.musicAddInput.trim();
      if (!input.isEmpty()) {
         if (!input.contains("list=") && !input.contains("/playlist")) {
            music.addToQueue(input);
         } else {
            music.addPlaylist(input);
         }

         this.musicAddInput = "";
         this.musicAddTyping = false;
      }
   }

   private void openSettings(Module mod) {
      this.settingsModule = mod;
      this.settingsScroll = 0;
      this.capturingBind = null;
      this.draggingSlider = null;
      this.searchQuery = "";
   }

   private void closeSettings() {
      this.settingsModule = null;
      this.settingsScroll = 0;
      this.capturingBind = null;
      this.draggingSlider = null;
   }

   private static int argb(int a, int r, int g, int b) {
      return a << 24 | r << 16 | g << 8 | b;
   }

   private void drawConfigsPanel(GuiGraphicsExtractor g, int mx, int my, int aR, int aG, int aB) {
      int margin = 20;
      int headerH = 24;
      int entryH = 18;
      int saveBarH = 36;
      int panW = this.width - margin * 2;
      int panH = this.height - margin * 2;
      int panX = margin;
      Font font = Minecraft.getInstance().font;
      g.fill(0, 0, this.width, this.height, -1157627904);
      g.fill(margin + 2, margin + 2, margin + panW + 2, margin + panH + 2, 1140850688);
      g.fill(margin, margin, margin + panW, margin + panH, -267316975);
      g.fill(margin - 1, margin - 1, margin + panW + 1, margin, -14013910);
      g.fill(margin - 1, margin + panH, margin + panW + 1, margin + panH + 1, -14013910);
      g.fill(margin - 1, margin, margin, margin + panH, -14013910);
      g.fill(margin + panW, margin, margin + panW + 1, margin + panH, -14013910);
      g.fill(margin, margin, margin + panW, margin + headerH, -266461666);
      g.fill(margin, margin + headerH - 1, margin + panW, margin + headerH, argb(255, aR, aG, aB));
      g.text(font, ClickGuiModule.t("CONFIGS"), margin + 8, margin + (headerH - 8) / 2, -1, false);
      String closeStr = "X";
      int closeW = 20;
      int closeH = 14;
      int closeX = margin + panW - closeW - 4;
      int closeY = margin + (headerH - closeH) / 2;
      boolean closeHov = mx >= closeX && mx <= closeX + closeW && my >= closeY && my <= closeY + closeH;
      g.fill(closeX, closeY, closeX + closeW, closeY + closeH, closeHov ? 1157579844 : 587202559);
      g.text(font, closeStr, closeX + (closeW - font.width(closeStr)) / 2, closeY + (closeH - 8) / 2, -5592406, false);
      int acc = OyVey.colorManager.getColorAsInt();
      int listAreaH = panH - headerH - 4 - saveBarH;
      int maxVisible = listAreaH / entryH;
      int maxScroll = Math.max(0, this.configList.size() - maxVisible);
      if (this.configScroll > maxScroll) {
         this.configScroll = maxScroll;
      }

      int ey = margin + headerH + 4;
      if (this.configList.isEmpty()) {
         g.text(font, ClickGuiModule.t("No configs saved yet"), margin + 8, ey + 4, -11184811, false);
      } else {
         int end = Math.min(this.configList.size(), this.configScroll + maxVisible);

         for (int i = this.configScroll; i < end; i++) {
            String cfg = this.configList.get(i);
            boolean hov = mx >= panX && mx < panX + panW - 52 && my >= ey && my < ey + entryH;
            if (hov) {
               g.fill(panX, ey, panX + panW - 52, ey + entryH, 419430399);
            }

            g.fill(panX, ey + entryH - 1, panX + panW, ey + entryH, 369098751);
            g.text(font, cfg, panX + 8, ey + (entryH - 8) / 2, -3355444, false);
            int lbX = panX + panW - 48;
            int lbW = 44;
            boolean lhov = mx >= lbX && mx <= lbX + lbW && my >= ey + 2 && my <= ey + entryH - 2;
            g.fill(lbX, ey + 2, lbX + lbW, ey + entryH - 2, lhov ? 0xFF000000 | acc : 872415231);
            g.text(font, ClickGuiModule.t("Load"), lbX + (lbW - font.width(ClickGuiModule.t("Load"))) / 2, ey + (entryH - 8) / 2, -1, false);
            ey += entryH;
         }
      }

      if (this.configList.size() > maxVisible) {
         int sbX = panX + panW - 5;
         int sbW = 4;
         int sbAreaY = margin + headerH + 4;
         g.fill(sbX, sbAreaY, sbX + sbW, sbAreaY + listAreaH, 587202559);
         int thumbH = Math.max(16, (int)((float)listAreaH * maxVisible / this.configList.size()));
         int thumbY = sbAreaY + (maxScroll > 0 ? (int)((float)(listAreaH - thumbH) * this.configScroll / maxScroll) : 0);
         g.fill(sbX, thumbY, sbX + sbW, thumbY + thumbH, -1996488705);
      }

      int saveY = margin + panH - saveBarH;
      g.fill(panX, saveY, panX + panW, saveY + saveBarH, -267645940);
      g.fill(panX, saveY, panX + panW, saveY + 1, -14013910);
      int inputW = panW - 100;
      boolean inputHov = mx >= panX + 6 && mx <= panX + 6 + inputW && my >= saveY + 6 && my <= saveY + saveBarH - 6;
      g.fill(panX + 6, saveY + 6, panX + 6 + inputW, saveY + saveBarH - 6, this.configTyping ? 1157627903 : (inputHov ? 587202559 : 419430399));
      boolean blink = this.configTyping && System.currentTimeMillis() % 900L < 450L;
      String dispName = this.configSaveName.isEmpty() ? (this.configTyping ? "" : ClickGuiModule.t("name...")) : this.configSaveName;
      g.text(font, dispName + (blink ? "|" : ""), panX + 9, saveY + (saveBarH - 8) / 2, this.configSaveName.isEmpty() ? -12303292 : -1118482, false);
      int saveBtnX = panX + 6 + inputW + 6;
      int saveBtnW = panW - 6 - inputW - 18;
      boolean shov = mx >= saveBtnX && mx <= saveBtnX + saveBtnW && my >= saveY + 6 && my <= saveY + saveBarH - 6;
      g.fill(saveBtnX, saveY + 6, saveBtnX + saveBtnW, saveY + saveBarH - 6, shov ? 0xFF000000 | acc : 872415231);
      g.text(font, ClickGuiModule.t("Save"), saveBtnX + (saveBtnW - font.width(ClickGuiModule.t("Save"))) / 2, saveY + (saveBarH - 8) / 2, -1, false);
   }

   private void drawMusicTabAndPanel(GuiGraphicsExtractor g, int mx, int my, int accent, int aR, int aG, int aB) {
      Font font = Minecraft.getInstance().font;
      SrMusicModule music = SrMusicModule.INSTANCE;
      float target = this.musicOpen ? 1.0F : 0.0F;
      this.musicAnim = this.musicAnim + (target - this.musicAnim) * 0.22F;
      if (Math.abs(this.musicAnim - target) < 0.004F) {
         this.musicAnim = target;
      }

      int tabH = 80;
      int tabX = this.px - 14;
      int tabY = this.py + (455 - tabH) / 2;
      boolean tabHov = mx >= tabX && mx <= this.px && my >= tabY && my <= tabY + tabH;
      RenderUtil.roundedRect(g, tabX, tabY, 14, tabH, 3, this.musicOpen ? argb(220, aR / 4, aG / 4, aB / 4) : (tabHov ? 1728053247 : 1157627903));
      if (this.musicAnim > 0.01F) {
         g.fill(tabX, tabY + 3, tabX + 2, tabY + tabH - 3, argb((int)(this.musicAnim * 200.0F), aR, aG, aB));
      }

      g.text(font, "\u266a", tabX + (14 - font.width("\u266a")) / 2, tabY + 10, this.musicOpen ? 0xFF000000 | accent : -5592389, false);

      for (int li = 0; li < 5; li++) {
         g.text(font, "MUSIC".substring(li, li + 1), tabX + 4, tabY + 24 + li * 9, -11180425, false);
      }

      if (!(this.musicAnim < 0.01F)) {
         int panX = this.musicPanelX();
         int panY = this.musicPanelY();
         g.fill(panX + 3, panY + 3, panX + 240 + 3, panY + 330 + 3, 1426063360);
         g.fill(panX, panY, panX + 240, panY + 330, -234484972);
         g.fill(panX, panY, panX + 240, panY + 1, 0xFF000000 | accent);
         g.fill(panX, panY, panX + 1, panY + 330, 637534207);
         g.fill(panX, panY + 330 - 1, panX + 240, panY + 330, 419430399);
         g.fill(panX, panY, panX + 240, panY + 26, -267776230);
         g.text(font, "\u266a  SR Music", panX + 8, panY + 9, 0xFF000000 | accent, false);
         boolean modEn = music != null && music.isEnabled();
         int togX = panX + 240 - 32;
         int togY = panY + 8;
         if (modEn) {
            g.fill(togX, togY, togX + 22, togY + 11, OyVey.colorManager.getColorWithAlpha(togY, 200));
            g.fill(togX + 12, togY + 1, togX + 21, togY + 10, -1);
         } else {
            g.fill(togX, togY, togX + 22, togY + 11, 872415231);
            g.fill(togX + 1, togY + 1, togX + 10, togY + 10, -10854288);
         }

         int cur = panY + 26;
         g.fill(panX, cur, panX + 240, cur + 1, 419430399);
         cur++;
         if (music != null && modEn) {
            SrMusicModule.CheckStatus cs = music.getCheckStatus();

            int statusColor = switch (cs) {
               case OK -> -12277146;
               case MISSING_YTDLP, MISSING_FFMPEG -> -3381692;
               case ERROR -> -3390396;
               case CHECKING -> -5588156;
               default -> -12298906;
            };

            String statusIcon = switch (cs) {
               case OK -> "\u2714";
               case MISSING_YTDLP, MISSING_FFMPEG, ERROR -> "\u2717";
               case CHECKING -> "\u2026";
               default -> "?";
            };
            g.fill(panX, cur, panX + 240, cur + 20, -267907816);
            g.text(font, statusIcon + " " + music.getCheckDetail(), panX + 6, cur + 6, statusColor, false);
            int testBtnX = panX + 240 - 30;
            int testBtnW = 26;
            int testBtnY = cur + 4;
            boolean testHov = mx >= testBtnX && mx <= testBtnX + testBtnW && my >= testBtnY && my <= testBtnY + 12;
            g.fill(testBtnX, testBtnY, testBtnX + testBtnW, testBtnY + 12, testHov ? 0xFF000000 | accent : 872415231);
            g.text(font, "Test", testBtnX + (testBtnW - font.width("Test")) / 2, testBtnY + 2, -3355444, false);
            cur += 20;
            g.fill(panX, cur, panX + 240, cur + 1, 419430399);
            cur++;
            SrMusicModule.State mstate = music.getState();

            String stateStr = switch (mstate) {
               case LOADING -> "Loading\u2026";
               case PLAYING -> "Playing";
               case PAUSED -> "Paused";
               case ERROR -> "Error";
               default -> "Idle";
            };

            int stateColor = switch (mstate) {
               case LOADING -> -10053172;
               case PLAYING -> -12277163;
               case PAUSED -> -3364301;
               case ERROR -> -3390396;
               default -> -12298906;
            };
            g.text(font, stateStr, panX + 8, cur + 4, stateColor, false);
            cur += 12;
            String title = music.getNowTitle();
            int maxTW = 224;
            int titleW = font.width(title);
            g.enableScissor(panX + 6, cur, panX + 240 - 6, cur + 10);
            if (titleW > maxTW && mstate == SrMusicModule.State.PLAYING) {
               long now = System.currentTimeMillis();
               if (now - this.musicMarqueeLast > 28L) {
                  this.musicMarqueeOffset += 0.55F;
                  if (this.musicMarqueeOffset > titleW + 24) {
                     this.musicMarqueeOffset = 0.0F;
                  }

                  this.musicMarqueeLast = now;
               }

               int ox = -((int)this.musicMarqueeOffset);
               g.text(font, title, panX + 6 + ox, cur, -1118482, false);
               if (this.musicMarqueeOffset > titleW - maxTW + 10) {
                  g.text(font, title, panX + 6 + ox + titleW + 24, cur, -1118482, false);
               }
            } else {
               if (mstate != SrMusicModule.State.PLAYING) {
                  this.musicMarqueeOffset = 0.0F;
               }

               String t = title;

               while (font.width(t) > maxTW && t.length() > 3) {
                  t = t.substring(0, t.length() - 4) + "\u2026";
               }

               g.text(font, t, panX + 6, cur, -1118482, false);
            }

            g.disableScissor();
            cur += 11;
            g.text(font, music.getNowProgress(), panX + 6, cur, -12298906, false);
            cur += 9;
            int barX = panX + 6;
            int barW = 228;
            g.fill(barX, cur, barX + barW, cur + 4, 872415231);
            int filled = (int)(barW * music.getProgressPct());
            if (filled > 0) {
               g.fill(barX, cur, barX + filled, cur + 4, OyVey.colorManager.getColorWithAlpha(cur, 220));
               g.fill(barX + filled - 3, cur - 2, barX + filled + 3, cur + 6, -1);
            }

            cur += 9;
            g.fill(panX, cur, panX + 240, cur + 1, 318767103);
            cur += 4;
            int btnSize = 20;
            int ctrlSpacing = 26;
            int ctrlStartX = panX + (240 - ctrlSpacing * 3) / 2;
            this.mpCtrlBtnY = cur;
            String[] btns = new String[]{"|<", mstate == SrMusicModule.State.PLAYING ? "\u23f8" : "\u25b6", ">|"};

            for (int i = 0; i < 3; i++) {
               int bx = ctrlStartX + i * ctrlSpacing;
               boolean bHov = mx >= bx && mx <= bx + btnSize && my >= cur && my <= cur + btnSize;
               RenderUtil.roundedRect(g, bx, cur, btnSize, btnSize, 4, bHov ? 1442840575 : 587202559);
               g.text(font, btns[i], bx + (btnSize - font.width(btns[i])) / 2, cur + (btnSize - 8) / 2, bHov ? 0xFF000000 | accent : -3355444, false);
            }

            this.mpSfX = panX + 6;
            int sfY = cur + 2;
            boolean sfOn = music.isShuffle();
            RenderUtil.roundedRect(g, this.mpSfX, sfY, 18, 16, 3, sfOn ? argb(180, aR / 3, aG / 3, aB / 3) : 587202559);
            g.text(font, "\u21c4", this.mpSfX + (18 - font.width("\u21c4")) / 2, sfY + 4, sfOn ? 0xFF000000 | accent : -11180425, false);
            this.mpRpX = panX + 240 - 26;
            int rpY = cur + 2;
            boolean rpOn = music.isRepeat();
            RenderUtil.roundedRect(g, this.mpRpX, rpY, 18, 16, 3, rpOn ? argb(180, aR / 3, aG / 3, aB / 3) : 587202559);
            g.text(font, "\u21ba", this.mpRpX + (18 - font.width("\u21ba")) / 2, rpY + 4, rpOn ? 0xFF000000 | accent : -11180425, false);
            cur += btnSize + 6;
            g.fill(panX, cur, panX + 240, cur + 1, 318767103);
            cur += 4;
            g.text(font, "vol", panX + 6, cur + 2, -12298906, false);
            int vbX = panX + 26;
            int vbW = 206;
            this.mpVolBarY = cur + 3;
            this.musicVolBarX = vbX;
            this.musicVolBarW = vbW;
            g.fill(vbX, this.mpVolBarY, vbX + vbW, this.mpVolBarY + 5, 872415231);
            int vFill = (int)(vbW * music.getVolume());
            if (vFill > 0) {
               g.fill(vbX, this.mpVolBarY, vbX + vFill, this.mpVolBarY + 5, OyVey.colorManager.getColorWithAlpha(this.mpVolBarY, 200));
            }

            g.fill(vbX + vFill - 3, this.mpVolBarY - 2, vbX + vFill + 3, this.mpVolBarY + 7, -1);
            if (this.musicVolDragging) {
               long win = Minecraft.getInstance().getWindow().handle();
               if (GLFW.glfwGetMouseButton(win, 0) == 1) {
                  float pct = Mth.clamp((float)(mx - vbX) / vbW, 0.0F, 1.0F);
                  music.setVolume(pct);
               } else {
                  this.musicVolDragging = false;
               }
            }

            cur += 14;
            g.fill(panX, cur, panX + 240, cur + 1, 318767103);
            cur += 3;
            g.text(font, "Queue", panX + 6, cur + 2, -13417387, false);
            cur += 12;
            int qAreaH = panY + 330 - 30 - cur;
            this.mpQueueAreaY = cur;
            int entH = 16;
            int qi = music.getQueueIdx();
            g.enableScissor(panX, cur, panX + 240, cur + qAreaH);
            int ey = cur - this.musicQueueScroll;
            int qSz = music.getQueueSize();

            for (int i = 0; i < qSz; i++) {
               boolean active = i == qi;
               boolean eHov = mx >= panX && mx <= panX + 240 - 20 && my >= ey && my <= ey + entH;
               if (active) {
                  g.fill(panX, ey, panX + 240 - 20, ey + entH, argb(55, aR / 3, aG / 3, aB / 3));
               } else if (eHov) {
                  g.fill(panX, ey, panX + 240 - 20, ey + entH, 385875967);
               }

               String lbl = music.getQueueTitle(i);

               while (font.width(lbl) > 198 && lbl.length() > 3) {
                  lbl = lbl.substring(0, lbl.length() - 4) + "\u2026";
               }

               g.text(font, (active ? "\u25b6 " : "  ") + lbl, panX + 4, ey + 4, active ? 0xFF000000 | accent : -10850688, false);
               int rmX2 = panX + 240 - 20;
               boolean rmH = mx >= rmX2 && mx <= rmX2 + 16 && my >= ey + 2 && my <= ey + entH - 2;
               if (rmH) {
                  g.fill(rmX2, ey + 2, rmX2 + 16, ey + entH - 2, 1157579844);
               }

               g.text(font, "\u00d7", rmX2 + (16 - font.width("\u00d7")) / 2, ey + 4, -8956570, false);
               ey += entH;
            }

            if (qSz == 0) {
               g.text(font, "Empty \u2014 add a URL or search below", panX + 6, cur + 4, -13417387, false);
            }

            g.disableScissor();
            cur = panY + 330 - 28;
            g.fill(panX, cur, panX + 240, cur + 1, 419430399);
            cur += 3;
            this.mpAddInputY = cur;
            int inpW = 210;
            int inpH = 18;
            boolean inpHov = mx >= panX + 4 && mx <= panX + 4 + inpW && my >= cur && my <= cur + inpH;
            g.fill(panX + 4, cur, panX + 4 + inpW, cur + inpH, this.musicAddTyping ? 1157627903 : (inpHov ? 587202559 : 369098751));
            g.fill(panX + 4, cur, panX + 4 + inpW, cur + 1, 687865855);
            boolean blink2 = this.musicAddTyping && System.currentTimeMillis() % 900L < 450L;
            String disp2 = this.musicAddInput.isEmpty() ? (this.musicAddTyping ? "" : "URL or search\u2026") : this.musicAddInput;
            String dispColor = this.musicAddInput.isEmpty() ? "\u00a78" : "";
            g.text(font, dispColor + disp2 + (blink2 ? "\u00a7f|" : ""), panX + 7, cur + 5, -1118482, false);
            this.mpAddBtnX = panX + 4 + inpW + 4;
            int addBW = 18;
            boolean addHov2 = mx >= this.mpAddBtnX && mx <= this.mpAddBtnX + addBW && my >= cur && my <= cur + inpH;
            g.fill(this.mpAddBtnX, cur, this.mpAddBtnX + addBW, cur + inpH, addHov2 ? 0xFF000000 | accent : 872415231);
            g.text(font, "+", this.mpAddBtnX + (addBW - font.width("+")) / 2, cur + 5, -1, false);
         } else {
            g.text(font, "Enable SrMusic to use the player", panX + 8, cur + 12, -12298906, false);
         }
      }
   }

   private int musicPanelX() {
      return (this.width - 240) / 2;
   }

   private int musicPanelY() {
      return (this.height - 330) / 2 + (int)((1.0F - this.musicAnim) * 24.0F);
   }

   private void refreshConfigList() {
      this.configList.clear();

      try {
         if (!Files.exists(CFG_DIR)) {
            return;
         }

         try (Stream<Path> stream = Files.list(CFG_DIR)) {
            this.configList = stream.filter(p -> p.toString().endsWith(".json"))
               .map(p -> p.getFileName().toString().replace(".json", ""))
               .sorted()
               .collect(Collectors.toList());
         }
      } catch (IOException var6) {
      }
   }

   private void saveConfig(String name) {
      if (name.matches("[a-zA-Z0-9_\\-]+")) {
         try {
            Files.createDirectories(CFG_DIR);
            Files.writeString(CFG_DIR.resolve(name + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(OyVey.moduleManager.toJson()));
         } catch (IOException var3) {
         }
      }
   }

   private void loadConfig(String name) {
      try {
         Path f = CFG_DIR.resolve(name + ".json");
         if (!Files.exists(f)) {
            return;
         }

         OyVey.moduleManager.fromJson(JsonParser.parseString(Files.readString(f)));
      } catch (IOException var3) {
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}
