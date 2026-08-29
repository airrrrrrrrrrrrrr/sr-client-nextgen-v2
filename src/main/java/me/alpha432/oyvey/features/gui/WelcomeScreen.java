package me.alpha432.oyvey.features.gui;

import com.google.gson.JsonParser;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.manager.PreferencesManager;
import me.alpha432.oyvey.util.Lang;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class WelcomeScreen extends Screen {
   private static final int STARS = 90;
   private final float[] sx = new float[90];
   private final float[] sy = new float[90];
   private final float[] svx = new float[90];
   private final float[] svy = new float[90];
   private final float[] sa = new float[90];
   public static boolean shownThisSession = false;
   private int step;
   private PreferencesManager.Language selectedLang;
   private PreferencesManager.ThemeColor selectedColor;
   private PreferencesManager.Server selectedServer;
   private long stepStartMs;
   private final boolean fromStartup;
   private static final int SW = 40;
   private static final int SH = 48;
   private static final int SGAP = 6;
   private static final int ROW_GAP = 7;
   private static final int COLS = 6;
   private static final int CARD_W = 108;
   private static final int CARD_H = 56;
   private static final int CARD_GAP = 14;

   public WelcomeScreen(boolean fromStartup) {
      super(Component.literal("setup"));
      this.fromStartup = fromStartup;
      this.step = 0;
      this.selectedLang = OyVey.preferences != null ? OyVey.preferences.getLanguage() : PreferencesManager.Language.ENGLISH;
      this.selectedColor = OyVey.preferences != null ? OyVey.preferences.getThemeColor() : PreferencesManager.ThemeColor.BLUE;
      this.selectedServer = OyVey.preferences != null ? OyVey.preferences.getServer() : PreferencesManager.Server.NONE;
   }

   public void init() {
      this.stepStartMs = System.currentTimeMillis();
      Random rng = new Random();

      for (int i = 0; i < 90; i++) {
         this.sx[i] = rng.nextFloat() * this.width;
         this.sy[i] = rng.nextFloat() * this.height;
         float a = rng.nextFloat() * 6.2831855F;
         float s = 0.03F + rng.nextFloat() * 0.1F;
         this.svx[i] = (float)Math.cos(a) * s;
         this.svy[i] = (float)Math.sin(a) * s;
         this.sa[i] = 0.18F + rng.nextFloat() * 0.5F;
      }
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      float age = (float)(System.currentTimeMillis() - this.stepStartMs) / 1000.0F;
      float fadeIn = Mth.clamp(age / 0.55F, 0.0F, 1.0F);
      PreferencesManager.ThemeColor tc = this.step == 1 ? this.selectedColor : (this.step == 2 ? this.selectedColor : PreferencesManager.ThemeColor.BLUE);
      int aR = tc.r();
      int aG = tc.g();
      int aB = tc.b();
      g.fill(0, 0, this.width, this.height, -16316657);
      float pulse = (float)(Math.sin(age * 0.65) * 0.5 + 0.5);
      RenderUtil.verticalGradient(g, 0.0F, 0.0F, this.width, this.height * 0.5F, new Color(aR, aG, aB, (int)(10.0F + pulse * 15.0F)), new Color(0, 0, 0, 0));
      RenderUtil.verticalGradient(g, 0.0F, this.height * 0.65F, this.width, this.height, new Color(0, 0, 0, 0), new Color(0, 0, 0, 180));

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

         g.fill(
            (int)this.sx[i],
            (int)this.sy[i],
            (int)this.sx[i] + 1,
            (int)this.sy[i] + 1,
            applyAlpha(aR << 16 | aG << 8 | aB, (int)(this.sa[i] * fadeIn * 120.0F))
         );
      }

      this.drawClientTitle(g, age, fadeIn, aR, aG, aB);
      int cx = this.width / 2;
      this.drawStepDots(g, cx, (int)(this.height * 0.28F), fadeIn, aR, aG, aB);
      switch (this.step) {
         case 0:
            this.renderLanguageStep(g, mx, my, cx, fadeIn, aR, aG, aB);
            break;
         case 1:
            this.renderColorStep(g, mx, my, cx, fadeIn, aR, aG, aB);
            break;
         case 2:
            this.renderServerStep(g, mx, my, cx, fadeIn, aR, aG, aB);
      }
   }

   private void drawClientTitle(GuiGraphicsExtractor g, float age, float fadeIn, int aR, int aG, int aB) {
      String title = "SR CLIENT";
      float scale = 2.0F;
      int titleY = (int)(this.height * 0.1F);
      g.pose().pushMatrix();
      g.pose().scale(scale, scale);
      int tx = (int)((this.width / 2.0F - this.font.width(title) * scale / 2.0F) / scale);
      int ty = (int)(titleY / scale);

      for (int d = 3; d >= 1; d--) {
         int gc = applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * 32.0F / d));
         g.text(this.font, title, tx - d, ty, gc, false);
         g.text(this.font, title, tx + d, ty, gc, false);
         g.text(this.font, title, tx, ty - d, gc, false);
         g.text(this.font, title, tx, ty + d, gc, false);
      }

      g.text(this.font, title, tx, ty, applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * 255.0F)), false);
      g.pose().popMatrix();
      float sh = age % 4.0F / 4.0F;
      float shA = Mth.clamp(1.0F - Math.abs(sh - 0.08F) * 14.0F, 0.0F, 1.0F);
      if (shA > 0.01F) {
         g.text(
            this.font,
            title,
            (int)(this.width / 2.0F - this.font.width(title) * scale / 2.0F),
            titleY,
            applyAlpha(16777215, (int)(fadeIn * shA * 200.0F)),
            false
         );
      }
   }

   private void drawStepDots(GuiGraphicsExtractor g, int cx, int y, float fadeIn, int aR, int aG, int aB) {
      int gap = 16;
      int total = 3;
      int startX = cx - gap * (total - 1) / 2;

      for (int i = 0; i < total; i++) {
         int dx = startX + i * gap;
         boolean active = i == this.step;
         int col = active ? applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * 230.0F)) : applyAlpha(3359829, (int)(fadeIn * 110.0F));
         g.fill(dx - 4, y - 2, dx + 4, y + 2, col);
      }
   }

   private void renderLanguageStep(GuiGraphicsExtractor g, int mx, int my, int cx, float fadeIn, int aR, int aG, int aB) {
      int baseY = (int)(this.height * 0.33F);
      int cardsY = baseY + 28;
      int cardW = 120;
      int cardH = 52;
      int gap = 22;
      g.centeredText(this.font, Lang.get("welcome.step1"), cx, baseY, applyAlpha(5596791, (int)(fadeIn * 195.0F)));
      g.centeredText(this.font, Lang.get("welcome.lang.title"), cx, baseY + 13, applyAlpha(11189196, (int)(fadeIn * 230.0F)));
      PreferencesManager.Language[] langs = new PreferencesManager.Language[]{
         PreferencesManager.Language.ENGLISH, PreferencesManager.Language.SPANISH, PreferencesManager.Language.GERMAN
      };
      String[] labels = new String[]{"English", "Espa\u00f1ol", "Deutsch"};
      String[] subs = new String[]{"en", "es", "de"};
      int totalW = 3 * cardW + 2 * gap;
      int startX = cx - totalW / 2;
      int[] xs = new int[]{startX, startX + cardW + gap, startX + 2 * (cardW + gap)};

      for (int i = 0; i < 3; i++) {
         int x = xs[i];
         boolean sel = this.selectedLang == langs[i];
         boolean hov = mx >= x && mx < x + cardW && my >= cardsY && my < cardsY + cardH;
         boolean lit = sel || hov;
         if (lit) {
            RenderUtil.dropShadow(g, x, cardsY, cardW, cardH, 7.0F, 9.0F, (int)(fadeIn * 90.0F));
         }
         RenderUtil.roundedRect(g, x, cardsY, cardW, cardH, 4, lit ? applyAlpha(1975605, (int)(fadeIn * 245.0F)) : applyAlpha(723994, (int)(fadeIn * 200.0F)));
         RenderUtil.roundedOutline(g, x, cardsY, cardW, cardH, 5.0F, 0.8F, applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * (lit ? 165 : 42))));
         if (sel) {
            RenderUtil.roundedRect(g, x + 8, cardsY + 3, cardW - 16, 2, 1, applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * 215.0F)));
         }

         g.fill(x, cardsY + 3, x + 2, cardsY + cardH - 3, applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * (lit ? 255 : 65))));
         g.centeredText(
            this.font,
            labels[i],
            x + cardW / 2,
            cardsY + 10,
            lit ? applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * 255.0F)) : applyAlpha(8952234, (int)(fadeIn * 210.0F))
         );
         g.centeredText(this.font, subs[i], x + cardW / 2, cardsY + 24, applyAlpha(4085111, (int)(fadeIn * 170.0F)));
         if (sel) {
            g.centeredText(this.font, "\u2713", x + cardW / 2, cardsY + 37, applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * 220.0F)));
         }
      }

      this.drawBtn(g, mx, my, cx, this.btnY(), fadeIn, aR, aG, aB, Lang.get("welcome.next"));
   }

   private void renderColorStep(GuiGraphicsExtractor g, int mx, int my, int cx, float fadeIn, int aR, int aG, int aB) {
      int baseY = (int)(this.height * 0.26F);
      int row1Y = baseY + 26;
      int row2Y = row1Y + 48 + 7;
      int rowW = 270;
      int startX = cx - rowW / 2;
      g.centeredText(this.font, Lang.get("welcome.step2"), cx, baseY, applyAlpha(5596791, (int)(fadeIn * 195.0F)));
      g.centeredText(this.font, Lang.get("welcome.color.title"), cx, baseY + 13, applyAlpha(11189196, (int)(fadeIn * 230.0F)));
      PreferencesManager.ThemeColor[] colors = PreferencesManager.ThemeColor.values();

      for (int i = 0; i < colors.length; i++) {
         PreferencesManager.ThemeColor tc = colors[i];
         int col = i % 6;
         int row = i / 6;
         int x = startX + col * 46;
         int sy2 = row == 0 ? row1Y : row2Y;
         boolean sel = this.selectedColor == tc;
         boolean hov = mx >= x && mx < x + 40 && my >= sy2 && my < sy2 + 48;
         boolean lit = sel || hov;
         int cr = tc.r();
         int cg = tc.g();
         int cb = tc.b();
         RenderUtil.roundedRect(g, x, sy2, 40, 48, 3, lit ? applyAlpha(1975605, (int)(fadeIn * 245.0F)) : applyAlpha(723994, (int)(fadeIn * 200.0F)));
         RenderUtil.roundedOutline(g, x, sy2, 40, 48, 4.0F, 0.75F, applyAlpha(cr << 16 | cg << 8 | cb, (int)(fadeIn * (lit ? 175 : 45))));
         if (sel) {
            RenderUtil.roundedRect(g, x + 6, sy2 + 3, 28, 2, 1, applyAlpha(cr << 16 | cg << 8 | cb, (int)(fadeIn * 210.0F)));
         }

         int dotS = 14;
         int dotX = x + 20 - dotS / 2;
         int dotY = sy2 + 5;
         if (lit) {
            for (int d = 4; d >= 1; d--) {
               g.fill(dotX - d, dotY - d, dotX + dotS + d, dotY + dotS + d, applyAlpha(cr << 16 | cg << 8 | cb, (int)(fadeIn * 20.0F / d)));
            }
         }

         RenderUtil.roundedRect(g, dotX, dotY, dotS, dotS, 3, applyAlpha(cr << 16 | cg << 8 | cb, (int)(fadeIn * (lit ? 255 : 185))));
         g.centeredText(
            this.font, Lang.get("color." + tc.name()), x + 20, sy2 + 22, applyAlpha(lit ? cr << 16 | cg << 8 | cb : 8952234, (int)(fadeIn * 205.0F))
         );
         if (sel) {
            g.centeredText(this.font, "\u2713", x + 20, sy2 + 35, applyAlpha(cr << 16 | cg << 8 | cb, (int)(fadeIn * 225.0F)));
         }
      }

      this.drawBtn(g, mx, my, cx, this.btnY(), fadeIn, aR, aG, aB, Lang.get("welcome.next"));
   }

   private void renderServerStep(GuiGraphicsExtractor g, int mx, int my, int cx, float fadeIn, int aR, int aG, int aB) {
      int baseY = (int)(this.height * 0.33F);
      int cardsY = baseY + 28;
      int totalW = 352;
      int startX = cx - totalW / 2;
      g.centeredText(this.font, Lang.get("welcome.step3"), cx, baseY, applyAlpha(5596791, (int)(fadeIn * 195.0F)));
      g.centeredText(this.font, Lang.get("welcome.server.title"), cx, baseY + 13, applyAlpha(11189196, (int)(fadeIn * 230.0F)));
      PreferencesManager.Server[] servers = new PreferencesManager.Server[]{
         PreferencesManager.Server.DEATHZONE, PreferencesManager.Server.HYPIXEL, PreferencesManager.Server.OTHER
      };
      String[] subs = new String[]{"dz.wtf", "mc.hypixel.net", "custom"};

      for (int i = 0; i < servers.length; i++) {
         PreferencesManager.Server sv = servers[i];
         int x = startX + i * 122;
         boolean sel = this.selectedServer == sv;
         boolean hov = mx >= x && mx < x + 108 && my >= cardsY && my < cardsY + 56;
         boolean lit = sel || hov;
         if (lit) {
            RenderUtil.dropShadow(g, x, cardsY, 108, 56, 7.0F, 9.0F, (int)(fadeIn * 90.0F));
         }
         RenderUtil.roundedRect(g, x, cardsY, 108, 56, 4, lit ? applyAlpha(1975605, (int)(fadeIn * 245.0F)) : applyAlpha(723994, (int)(fadeIn * 200.0F)));
         RenderUtil.roundedOutline(g, x, cardsY, 108, 56, 5.0F, 0.8F, applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * (lit ? 165 : 42))));
         if (sel) {
            RenderUtil.roundedRect(g, x + 8, cardsY + 3, 92, 2, 1, applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * 215.0F)));
         }

         g.fill(x, cardsY + 3, x + 2, cardsY + 56 - 3, applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * (lit ? 255 : 65))));
         String label = Lang.get("server." + sv.name());
         g.centeredText(
            this.font,
            label,
            x + 54,
            cardsY + 10,
            lit ? applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * 255.0F)) : applyAlpha(8952234, (int)(fadeIn * 210.0F))
         );
         g.centeredText(this.font, subs[i], x + 54, cardsY + 24, applyAlpha(4085111, (int)(fadeIn * 165.0F)));
         if (sel) {
            g.centeredText(this.font, "\u2713", x + 54, cardsY + 39, applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * 220.0F)));
         }
      }

      this.drawBtn(g, mx, my, cx, this.btnY(), fadeIn, aR, aG, aB, Lang.get("welcome.done"));
   }

   private int btnY() {
      return (int)(this.height * 0.82F);
   }

   private void drawBtn(GuiGraphicsExtractor g, int mx, int my, int cx, int y, float fadeIn, int aR, int aG, int aB, String label) {
      int bw = 140;
      int bh = 22;
      int bx = cx - bw / 2;
      boolean hov = mx >= bx && mx < bx + bw && my >= y && my < y + bh;
      if (hov) {
         RenderUtil.dropShadow(g, bx, y, bw, bh, 5.0F, 8.0F, (int)(fadeIn * 100.0F));
      }
      RenderUtil.roundedRectGradient(
         g,
         bx,
         y,
         bw,
         bh,
         5.0F,
         hov ? applyAlpha(1842224, (int)(fadeIn * 235.0F)) : applyAlpha(723740, (int)(fadeIn * 190.0F)),
         applyAlpha(460558, (int)(fadeIn * 195.0F)),
         true
      );
      RenderUtil.roundedOutline(g, bx, y, bw, bh, 5.0F, 0.8F, applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * (hov ? 190 : 75))));
      g.centeredText(
         this.font,
         label,
         cx,
         y + (bh - 8) / 2,
         hov ? applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * 255.0F)) : applyAlpha(12307677, (int)(fadeIn * 230.0F))
      );
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      if (click.button() != 0) {
         return super.mouseClicked(click, doubled);
      }

      double mx = click.x();
      double my = click.y();
      int cx = this.width / 2;
      int by = this.btnY();
      int bw = 140;
      int bh = 22;
      switch (this.step) {
         case 0:
            int baseY = (int)(this.height * 0.33F);
            int cardsY = baseY + 28;
            int cardW = 120;
            int cardH = 52;
            int gap = 22;
            int totalW = 3 * cardW + 2 * gap;
            int startX = cx - totalW / 2;
            int[] xs = new int[]{startX, startX + cardW + gap, startX + 2 * (cardW + gap)};
            PreferencesManager.Language[] langs = new PreferencesManager.Language[]{
               PreferencesManager.Language.ENGLISH, PreferencesManager.Language.SPANISH, PreferencesManager.Language.GERMAN
            };

            for (int i = 0; i < 3; i++) {
               if (mx >= xs[i] && mx < xs[i] + cardW && my >= cardsY && my < cardsY + cardH) {
                  this.selectedLang = langs[i];
                  if (OyVey.preferences != null) {
                     OyVey.preferences.setLanguage(this.selectedLang);
                  }

                  return true;
               }
            }

            if (mx >= cx - bw / 2 && mx < cx + bw / 2 && my >= by && my < by + bh) {
               this.advance();
               return true;
            }
            break;
         case 1:
            int themeBaseY = (int)(this.height * 0.26F);
            int row1Y = themeBaseY + 26;
            int row2Y = row1Y + 48 + 7;
            int rowW = 270;
            int themeStartX = cx - rowW / 2;
            PreferencesManager.ThemeColor[] colors = PreferencesManager.ThemeColor.values();

            for (int ix = 0; ix < colors.length; ix++) {
               int col = ix % 6;
               int row = ix / 6;
               int x = themeStartX + col * 46;
               int sy2 = row == 0 ? row1Y : row2Y;
               if (mx >= x && mx < x + 40 && my >= sy2 && my < sy2 + 48) {
                  this.selectedColor = colors[ix];
                  if (OyVey.preferences != null) {
                     OyVey.preferences.setThemeColor(this.selectedColor);
                  }

                  return true;
               }
            }

            if (mx >= cx - bw / 2 && mx < cx + bw / 2 && my >= by && my < by + bh) {
               this.advance();
               return true;
            }
            break;
         case 2:
            int serverBaseY = (int)(this.height * 0.33F);
            int serverCardsY = serverBaseY + 28;
            int serverTotalW = 352;
            int serverStartX = cx - serverTotalW / 2;
            PreferencesManager.Server[] servers = new PreferencesManager.Server[]{
               PreferencesManager.Server.DEATHZONE, PreferencesManager.Server.HYPIXEL, PreferencesManager.Server.OTHER
            };

            for (int ixx = 0; ixx < servers.length; ixx++) {
               int x = serverStartX + ixx * 122;
               if (mx >= x && mx < x + 108 && my >= serverCardsY && my < serverCardsY + 56) {
                  this.selectedServer = servers[ixx];
                  if (OyVey.preferences != null) {
                     OyVey.preferences.setServer(this.selectedServer);
                  }

                  return true;
               }
            }

            if (mx >= cx - bw / 2 && mx < cx + bw / 2 && my >= by && my < by + bh) {
               this.finish();
               return true;
            }
      }

      return super.mouseClicked(click, doubled);
   }

   private void advance() {
      this.step++;
      this.stepStartMs = System.currentTimeMillis();
   }

   private void finish() {
      if (OyVey.preferences != null) {
         OyVey.preferences.setLanguage(this.selectedLang);
         OyVey.preferences.setThemeColor(this.selectedColor);
         OyVey.preferences.setServer(this.selectedServer);
         if (this.fromStartup && this.selectedServer == PreferencesManager.Server.DEATHZONE) {
            this.applyDeathzonePreset();
         }

         OyVey.preferences.applyTheme();
         OyVey.configManager.save();
      }

      if (this.fromStartup) {
         this.minecraft.setScreen(new TitleScreen());
      } else {
         this.minecraft.setScreen(new CustomMainMenuScreen());
      }
   }

   private void applyDeathzonePreset() {
      try {
         OyVey.moduleManager
            .fromJson(
               JsonParser.parseString(
                  "{\n  \"SrMusic\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"SrMusic\",\n    \"YtdlpPath\": \"yt-dlp\",\n    \"FfmpegPath\": \"C:/Users/JD Richardson/Desktop/everything/oyvey-ported-master/run/sr_music/bin/ffmpeg.exe\",\n    \"Volume\": \"0.7\",\n    \"Shuffle\": \"false\",\n    \"Repeat\": \"false\"\n  },\n  \"Watermark\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Watermark\",\n    \"Position\": \"0.0,0.0\",\n    \"Rainbow\": \"true\",\n    \"Red\": \"100\",\n    \"Green\": \"180\",\n    \"Blue\": \"255\"\n  },\n  \"Coordinates\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Coordinates\",\n    \"Position\": \"0.0020833334,0.024557957\",\n    \"Nether\": \"false\"\n  },\n  \"HudEditor\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"HudEditor\"\n  },\n  \"ClickGui\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": 344,\n    \"DisplayName\": \"ClickGui\",\n    \"CustomMenu\": \"true\",\n    \"Prefix\": \".\",\n    \"Color\": \"0,0,255,180\",\n    \"TopColor\": \"0,0,150,240\",\n    \"Rainbow\": \"true\",\n    \"Delay\": \"240\",\n    \"Brightness\": \"255.0\",\n    \"Saturation\": \"150.0\",\n    \"Mode\": \"DROPDOWN\",\n    \"GuiScale\": \"1.0163934\"\n  },\n  \"Notifications\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Notifications\",\n    \"Module Toggle\": \"true\"\n  },\n  \"Criticals\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Criticals\",\n    \"Mode\": \"DEATHZONE2\",\n    \"MinDelay\": \"500\",\n    \"MaxDelay\": \"500\"\n  },\n  \"KeyPearl\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": 86,\n    \"DisplayName\": \"KeyPearl\",\n    \"Inventory\": \"true\"\n  },\n  \"KillAura\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"KillAura\",\n    \"Reach\": \"4.4375\",\n    \"Mobs\": \"true\",\n    \"CPS\": \"7\",\n    \"CpsRand\": \"true\",\n    \"CpsMin\": \"8\",\n    \"CpsMax\": \"14\",\n    \"1.9Cooldown\": \"false\",\n    \"OnlyCrits\": \"false\",\n    \"MultiAura\": \"false\",\n    \"SwitchAura\": \"true\",\n    \"SwitchDelay\": \"50\",\n    \"AutoBlock\": \"SMART\",\n    \"CorrectMove\": \"true\",\n    \"IntaveMode\": \"false\",\n    \"IntaveJitter\": \"0.003\",\n    \"AntiBot\": \"false\",\n    \"BotSpinDeg\": \"60.0\"\n  },\n  \"KillauraRecode\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": 67,\n    \"DisplayName\": \"KillauraRecode\",\n    \"Reach\": \"3.696078300476074\",\n    \"Mobs\": \"false\",\n    \"RequireArmor\": \"false\",\n    \"RespectFriends\": \"false\",\n    \"WallCheck\": \"false\",\n    \"TargetMode\": \"CLOSEST\",\n    \"1.9Cooldown\": \"false\",\n    \"OnlyCrits\": \"false\",\n    \"RandAPS\": \"true\",\n    \"APSMin\": \"9\",\n    \"APSMax\": \"11\",\n    \"APS\": \"7\",\n    \"RotationMode\": \"ADVANCED\",\n    \"RotSpeed\": \"10.080357\",\n    \"PredictTicks\": \"1.5\",\n    \"MinRotSpeed\": \"13.009804\",\n    \"MaxRotSpeed\": \"15.0\",\n    \"Jitter\": \"55.88235\",\n    \"Noise\": \"28.431374\",\n    \"GCDFix\": \"true\",\n    \"CorrectMove\": \"false\",\n    \"AutoBlock\": \"FULL\",\n    \"ArmSwing\": \"true\",\n    \"MultiAura\": \"false\",\n    \"SwitchAura\": \"false\",\n    \"SwitchDelay\": \"326\",\n    \"AntiBot\": \"false\",\n    \"BotSpinDeg\": \"60.0\"\n  },\n  \"AutoGapple\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"AutoGapple\",\n    \"Health\": \"13.040437\"\n  },\n  \"Reach\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Reach\",\n    \"MaxReach\": \"4.442623138427734\",\n    \"MinReach\": \"6.0\",\n    \"Chance\": \"37\"\n  },\n  \"Hitbox\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Hitbox\",\n    \"Horizontal\": \"0.6339285969734192\",\n    \"Vertical\": \"0.59375\"\n  },\n  \"Backtrack\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Backtrack\",\n    \"DelayMS\": \"172\",\n    \"MaxRange\": \"5.549019813537598\",\n    \"SmartMode\": \"true\",\n    \"GhostESP\": \"true\"\n  },\n  \"WTap\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"WTap\",\n    \"Duration\": \"2\",\n    \"Chance\": \"70\"\n  },\n  \"GrimAura\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"GrimAura\",\n    \"Reach\": \"3.0\",\n    \"CPS\": \"11\",\n    \"1.9Cooldown\": \"false\",\n    \"Mobs\": \"false\",\n    \"WallCheck\": \"false\",\n    \"OnlyCrits\": \"false\",\n    \"RequireArmor\": \"false\",\n    \"AntiBot\": \"OFF\",\n    \"BotSpinDeg\": \"146.89655\"\n  },\n  \"TeleportAura\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"TeleportAura\",\n    \"MaxRange\": \"30.0\"\n  },\n  \"BedNuker\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"BedNuker\",\n    \"Range\": \"5\",\n    \"BreakDelay\": \"0\",\n    \"WallCheck\": \"false\",\n    \"AutoRotate\": \"true\",\n    \"HeadOnly\": \"false\"\n  },\n  \"Fly\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": 71,\n    \"DisplayName\": \"Fly\",\n    \"Mode\": \"VANILLA\",\n    \"Speed\": \"0.93950975\",\n    \"Boost\": \"0.42\",\n    \"DZ Speed\": \"0.69375\",\n    \"DZ Timer\": \"0.15\",\n    \"Test Speed\": \"0.35\",\n    \"Mospixel Speed\": \"0.287\",\n    \"StopMotion\": \"false\",\n    \"MP3Mode\": \"FAST\",\n    \"MP3Speed\": \"0.3\",\n    \"MP3NoCollide\": \"true\",\n    \"SpeedCap\": \"false\",\n    \"MaxSpeed\": \"3.3666666\",\n    \"AntiKick\": \"true\",\n    \"GlideSpeed\": \"0.14287256\"\n  },\n  \"FastFall\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": 88,\n    \"DisplayName\": \"FastFall\",\n    \"Speed\": \"2.2873564\"\n  },\n  \"Step\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Step\",\n    \"Height\": \"2.9\"\n  },\n  \"Scaffold\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": 82,\n    \"DisplayName\": \"Scaffold\",\n    \"Technique\": \"NORMAL\",\n    \"GBLedge\": \"JUMP\",\n    \"Tower\": \"JUMP\",\n    \"TowerDelay\": \"0\",\n    \"Rotate\": \"true\",\n    \"Lookback\": \"true\",\n    \"RotSpeed\": \"0.1\",\n    \"PlaceDelay\": \"0\",\n    \"SafeWalk\": \"false\",\n    \"Eagle\": \"false\",\n    \"Expand\": \"false\",\n    \"AutoBlock\": \"true\",\n    \"Sprint\": \"false\",\n    \"VulcanBypass\": \"false\",\n    \"Debug\": \"false\"\n  },\n  \"Speed\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": 88,\n    \"DisplayName\": \"Speed\",\n    \"Mode\": \"VANILLA\",\n    \"Speed\": \"0.6482758522033691\",\n    \"AutoJump\": \"true\",\n    \"LowHop\": \"true\",\n    \"HopHeight\": \"1.25\",\n    \"DamageBoost\": \"true\",\n    \"BoostFactor\": \"1.6071429252624512\",\n    \"PotionSync\": \"true\"\n  },\n  \"NoSlow\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"NoSlow\",\n    \"Mode\": \"VANILLA\"\n  },\n  \"Sprint\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Sprint\",\n    \"KeepSprint\": \"true\",\n    \"OmniSprint\": \"true\",\n    \"SprintResetTicks\": \"40\"\n  },\n  \"Velocity\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Velocity\",\n    \"Horizontal\": \"0\",\n    \"Vertical\": \"100\",\n    \"Explosions\": \"false\"\n  },\n  \"KillInsults\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"KillInsults\",\n    \"Message\": \"cooked by sr client ({name})\"\n  },\n  \"Disabler\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Disabler\",\n    \"CancelKeepAlive\": \"false\",\n    \"CancelSwing\": \"false\",\n    \"CancelEntityAction\": \"false\",\n    \"CancelPlayerAction\": \"false\",\n    \"CancelAbilities\": \"false\",\n    \"CancelMove\": \"false\",\n    \"OnlyWhenStill\": \"false\",\n    \"NoRotation\": \"false\",\n    \"RotationMode\": \"ZERO\",\n    \"RotOffset\": \"6.0\",\n    \"WatchdogMotion\": \"false\",\n    \"Vulcan\": \"false\",\n    \"VulcanSprint\": \"false\",\n    \"VulcanMovement\": \"false\",\n    \"DeathZoneTest\": \"true\",\n    \"DeathzoneReach\": \"false\",\n    \"StartSprint\": \"false\"\n  },\n  \"AimAssist\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"AimAssist\",\n    \"Speed\": \"3.414731979370117\",\n    \"Range\": \"3.25\",\n    \"OnlyTarget\": \"true\",\n    \"VertMultiplier\": \"0.75\",\n    \"HitboxJitter\": \"0.04\"\n  },\n  \"FPS Counter\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"FPS Counter\",\n    \"Position\": \"0.0020833334,0.035792783\",\n    \"ColoredFPS\": \"true\",\n    \"Label\": \"true\"\n  },\n  \"Minimap\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Minimap\",\n    \"Position\": \"0.0,0.108133085\",\n    \"Size\": \"90\",\n    \"Zoom\": \"48.0\",\n    \"Rotate\": \"false\",\n    \"ShowSelf\": \"true\"\n  },\n  \"MusicPlayer\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"MusicPlayer\",\n    \"Position\": \"0.0,0.08040665\",\n    \"RefreshSecs\": \"3\",\n    \"ShowSource\": \"true\",\n    \"Rainbow\": \"true\",\n    \"Red\": \"100\",\n    \"Green\": \"180\",\n    \"Blue\": \"255\"\n  },\n  \"TargetRingESP\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"TargetRingESP\",\n    \"Ring\": \"true\",\n    \"RingColor\": \"0,180,255,200\",\n    \"RingSpeed\": \"1.6529412\",\n    \"RingAmplitude\": \"0.86372554\",\n    \"RingRadius\": \"0.7117647\",\n    \"RingWidth\": \"2.4558823\",\n    \"RingSegments\": \"64\",\n    \"2D ESP\": \"true\",\n    \"2DColor\": \"0,180,255,200\",\n    \"2DWidth\": \"3.0\"\n  },\n  \"BlockHighlight\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"BlockHighlight\",\n    \"Color\": \"255,0,0,255\",\n    \"LineWidth\": \"1.0\"\n  },\n  \"ChestESP\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"ChestESP\",\n    \"ChestColor\": \"255,165,0,255\",\n    \"BarrelColor\": \"139,69,19,255\",\n    \"ShulkerColor\": \"160,32,240,255\",\n    \"EnderColor\": \"0,200,200,255\",\n    \"LineWidth\": \"1.5\",\n    \"Chests\": \"true\",\n    \"Barrels\": \"true\",\n    \"Shulkers\": \"true\",\n    \"EnderChests\": \"true\",\n    \"Filled\": \"false\",\n    \"Range\": \"64\"\n  },\n  \"ArrayList\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"ArrayList\",\n    \"Mode\": \"LIQUIDBOUNCE\",\n    \"Rainbow\": \"false\",\n    \"Red\": \"97\",\n    \"Green\": \"6\",\n    \"Blue\": \"255\",\n    \"Bloom\": \"true\",\n    \"BloomSize\": \"6\"\n  },\n  \"TargetHUD\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"TargetHUD\",\n    \"Position\": \"0.5,0.5\",\n    \"ModernMode\": \"true\"\n  },\n  \"Chams\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Chams\"\n  },\n  \"PlayerESP\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"PlayerESP\",\n    \"Box\": \"true\",\n    \"Corner\": \"true\",\n    \"2D\": \"true\",\n    \"Filled\": \"true\",\n    \"Healthbar\": \"true\",\n    \"Nametag\": \"true\",\n    \"Players\": \"true\",\n    \"Mobs\": \"true\",\n    \"Animals\": \"true\",\n    \"Invisibles\": \"true\",\n    \"Friends\": \"false\",\n    \"HideSelf\": \"true\",\n    \"Color\": \"0,255,0,255\",\n    \"FriendColor\": \"0,255,100,200\",\n    \"LineWidth\": \"2.0\",\n    \"FilledAlpha\": \"30\",\n    \"Rainbow\": \"true\",\n    \"DistScale\": \"true\",\n    \"Range\": \"4\"\n  },\n  \"Animations\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Animations\",\n    \"FakeAutoBlock\": \"true\",\n    \"OldAnimations\": \"true\",\n    \"OldBlockHit\": \"true\",\n    \"1.7Shield\": \"true\"\n  },\n  \"Ambience\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Ambience\",\n    \"CustomTime\": \"false\",\n    \"Time\": \"6000\",\n    \"RainStrength\": \"0.0\",\n    \"ThunderStrength\": \"0.0\",\n    \"Gamma\": \"2.0\",\n    \"NoFog\": \"false\"\n  },\n  \"WTap\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"WTap\",\n    \"Duration\": \"2\",\n    \"Chance\": \"70\"\n  }\n}\n"
               )
            );
         Path cfgDir = FabricLoader.getInstance().getGameDir().resolve("oyvey").resolve("configs");
         Files.createDirectories(cfgDir);
         Files.writeString(
            cfgDir.resolve("deathzone.json"),
            "{\n  \"SrMusic\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"SrMusic\",\n    \"YtdlpPath\": \"yt-dlp\",\n    \"FfmpegPath\": \"C:/Users/JD Richardson/Desktop/everything/oyvey-ported-master/run/sr_music/bin/ffmpeg.exe\",\n    \"Volume\": \"0.7\",\n    \"Shuffle\": \"false\",\n    \"Repeat\": \"false\"\n  },\n  \"Watermark\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Watermark\",\n    \"Position\": \"0.0,0.0\",\n    \"Rainbow\": \"true\",\n    \"Red\": \"100\",\n    \"Green\": \"180\",\n    \"Blue\": \"255\"\n  },\n  \"Coordinates\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Coordinates\",\n    \"Position\": \"0.0020833334,0.024557957\",\n    \"Nether\": \"false\"\n  },\n  \"HudEditor\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"HudEditor\"\n  },\n  \"ClickGui\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": 344,\n    \"DisplayName\": \"ClickGui\",\n    \"CustomMenu\": \"true\",\n    \"Prefix\": \".\",\n    \"Color\": \"0,0,255,180\",\n    \"TopColor\": \"0,0,150,240\",\n    \"Rainbow\": \"true\",\n    \"Delay\": \"240\",\n    \"Brightness\": \"255.0\",\n    \"Saturation\": \"150.0\",\n    \"Mode\": \"DROPDOWN\",\n    \"GuiScale\": \"1.0163934\"\n  },\n  \"Notifications\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Notifications\",\n    \"Module Toggle\": \"true\"\n  },\n  \"Criticals\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Criticals\",\n    \"Mode\": \"DEATHZONE2\",\n    \"MinDelay\": \"500\",\n    \"MaxDelay\": \"500\"\n  },\n  \"KeyPearl\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": 86,\n    \"DisplayName\": \"KeyPearl\",\n    \"Inventory\": \"true\"\n  },\n  \"KillAura\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"KillAura\",\n    \"Reach\": \"4.4375\",\n    \"Mobs\": \"true\",\n    \"CPS\": \"7\",\n    \"CpsRand\": \"true\",\n    \"CpsMin\": \"8\",\n    \"CpsMax\": \"14\",\n    \"1.9Cooldown\": \"false\",\n    \"OnlyCrits\": \"false\",\n    \"MultiAura\": \"false\",\n    \"SwitchAura\": \"true\",\n    \"SwitchDelay\": \"50\",\n    \"AutoBlock\": \"SMART\",\n    \"CorrectMove\": \"true\",\n    \"IntaveMode\": \"false\",\n    \"IntaveJitter\": \"0.003\",\n    \"AntiBot\": \"false\",\n    \"BotSpinDeg\": \"60.0\"\n  },\n  \"KillauraRecode\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": 67,\n    \"DisplayName\": \"KillauraRecode\",\n    \"Reach\": \"3.696078300476074\",\n    \"Mobs\": \"false\",\n    \"RequireArmor\": \"false\",\n    \"RespectFriends\": \"false\",\n    \"WallCheck\": \"false\",\n    \"TargetMode\": \"CLOSEST\",\n    \"1.9Cooldown\": \"false\",\n    \"OnlyCrits\": \"false\",\n    \"RandAPS\": \"true\",\n    \"APSMin\": \"9\",\n    \"APSMax\": \"11\",\n    \"APS\": \"7\",\n    \"RotationMode\": \"ADVANCED\",\n    \"RotSpeed\": \"10.080357\",\n    \"PredictTicks\": \"1.5\",\n    \"MinRotSpeed\": \"13.009804\",\n    \"MaxRotSpeed\": \"15.0\",\n    \"Jitter\": \"55.88235\",\n    \"Noise\": \"28.431374\",\n    \"GCDFix\": \"true\",\n    \"CorrectMove\": \"false\",\n    \"AutoBlock\": \"FULL\",\n    \"ArmSwing\": \"true\",\n    \"MultiAura\": \"false\",\n    \"SwitchAura\": \"false\",\n    \"SwitchDelay\": \"326\",\n    \"AntiBot\": \"false\",\n    \"BotSpinDeg\": \"60.0\"\n  },\n  \"AutoGapple\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"AutoGapple\",\n    \"Health\": \"13.040437\"\n  },\n  \"Reach\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Reach\",\n    \"MaxReach\": \"4.442623138427734\",\n    \"MinReach\": \"6.0\",\n    \"Chance\": \"37\"\n  },\n  \"Hitbox\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Hitbox\",\n    \"Horizontal\": \"0.6339285969734192\",\n    \"Vertical\": \"0.59375\"\n  },\n  \"Backtrack\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Backtrack\",\n    \"DelayMS\": \"172\",\n    \"MaxRange\": \"5.549019813537598\",\n    \"SmartMode\": \"true\",\n    \"GhostESP\": \"true\"\n  },\n  \"WTap\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"WTap\",\n    \"Duration\": \"2\",\n    \"Chance\": \"70\"\n  },\n  \"GrimAura\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"GrimAura\",\n    \"Reach\": \"3.0\",\n    \"CPS\": \"11\",\n    \"1.9Cooldown\": \"false\",\n    \"Mobs\": \"false\",\n    \"WallCheck\": \"false\",\n    \"OnlyCrits\": \"false\",\n    \"RequireArmor\": \"false\",\n    \"AntiBot\": \"OFF\",\n    \"BotSpinDeg\": \"146.89655\"\n  },\n  \"TeleportAura\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"TeleportAura\",\n    \"MaxRange\": \"30.0\"\n  },\n  \"BedNuker\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"BedNuker\",\n    \"Range\": \"5\",\n    \"BreakDelay\": \"0\",\n    \"WallCheck\": \"false\",\n    \"AutoRotate\": \"true\",\n    \"HeadOnly\": \"false\"\n  },\n  \"Fly\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": 71,\n    \"DisplayName\": \"Fly\",\n    \"Mode\": \"VANILLA\",\n    \"Speed\": \"0.93950975\",\n    \"Boost\": \"0.42\",\n    \"DZ Speed\": \"0.69375\",\n    \"DZ Timer\": \"0.15\",\n    \"Test Speed\": \"0.35\",\n    \"Mospixel Speed\": \"0.287\",\n    \"StopMotion\": \"false\",\n    \"MP3Mode\": \"FAST\",\n    \"MP3Speed\": \"0.3\",\n    \"MP3NoCollide\": \"true\",\n    \"SpeedCap\": \"false\",\n    \"MaxSpeed\": \"3.3666666\",\n    \"AntiKick\": \"true\",\n    \"GlideSpeed\": \"0.14287256\"\n  },\n  \"FastFall\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": 88,\n    \"DisplayName\": \"FastFall\",\n    \"Speed\": \"2.2873564\"\n  },\n  \"Step\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Step\",\n    \"Height\": \"2.9\"\n  },\n  \"Scaffold\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": 82,\n    \"DisplayName\": \"Scaffold\",\n    \"Technique\": \"NORMAL\",\n    \"GBLedge\": \"JUMP\",\n    \"Tower\": \"JUMP\",\n    \"TowerDelay\": \"0\",\n    \"Rotate\": \"true\",\n    \"Lookback\": \"true\",\n    \"RotSpeed\": \"0.1\",\n    \"PlaceDelay\": \"0\",\n    \"SafeWalk\": \"false\",\n    \"Eagle\": \"false\",\n    \"Expand\": \"false\",\n    \"AutoBlock\": \"true\",\n    \"Sprint\": \"false\",\n    \"VulcanBypass\": \"false\",\n    \"Debug\": \"false\"\n  },\n  \"Speed\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": 88,\n    \"DisplayName\": \"Speed\",\n    \"Mode\": \"VANILLA\",\n    \"Speed\": \"0.6482758522033691\",\n    \"AutoJump\": \"true\",\n    \"LowHop\": \"true\",\n    \"HopHeight\": \"1.25\",\n    \"DamageBoost\": \"true\",\n    \"BoostFactor\": \"1.6071429252624512\",\n    \"PotionSync\": \"true\"\n  },\n  \"NoSlow\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"NoSlow\",\n    \"Mode\": \"VANILLA\"\n  },\n  \"Sprint\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Sprint\",\n    \"KeepSprint\": \"true\",\n    \"OmniSprint\": \"true\",\n    \"SprintResetTicks\": \"40\"\n  },\n  \"Velocity\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Velocity\",\n    \"Horizontal\": \"0\",\n    \"Vertical\": \"100\",\n    \"Explosions\": \"false\"\n  },\n  \"KillInsults\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"KillInsults\",\n    \"Message\": \"cooked by sr client ({name})\"\n  },\n  \"Disabler\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Disabler\",\n    \"CancelKeepAlive\": \"false\",\n    \"CancelSwing\": \"false\",\n    \"CancelEntityAction\": \"false\",\n    \"CancelPlayerAction\": \"false\",\n    \"CancelAbilities\": \"false\",\n    \"CancelMove\": \"false\",\n    \"OnlyWhenStill\": \"false\",\n    \"NoRotation\": \"false\",\n    \"RotationMode\": \"ZERO\",\n    \"RotOffset\": \"6.0\",\n    \"WatchdogMotion\": \"false\",\n    \"Vulcan\": \"false\",\n    \"VulcanSprint\": \"false\",\n    \"VulcanMovement\": \"false\",\n    \"DeathZoneTest\": \"true\",\n    \"DeathzoneReach\": \"false\",\n    \"StartSprint\": \"false\"\n  },\n  \"AimAssist\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"AimAssist\",\n    \"Speed\": \"3.414731979370117\",\n    \"Range\": \"3.25\",\n    \"OnlyTarget\": \"true\",\n    \"VertMultiplier\": \"0.75\",\n    \"HitboxJitter\": \"0.04\"\n  },\n  \"FPS Counter\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"FPS Counter\",\n    \"Position\": \"0.0020833334,0.035792783\",\n    \"ColoredFPS\": \"true\",\n    \"Label\": \"true\"\n  },\n  \"Minimap\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Minimap\",\n    \"Position\": \"0.0,0.108133085\",\n    \"Size\": \"90\",\n    \"Zoom\": \"48.0\",\n    \"Rotate\": \"false\",\n    \"ShowSelf\": \"true\"\n  },\n  \"MusicPlayer\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"MusicPlayer\",\n    \"Position\": \"0.0,0.08040665\",\n    \"RefreshSecs\": \"3\",\n    \"ShowSource\": \"true\",\n    \"Rainbow\": \"true\",\n    \"Red\": \"100\",\n    \"Green\": \"180\",\n    \"Blue\": \"255\"\n  },\n  \"TargetRingESP\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"TargetRingESP\",\n    \"Ring\": \"true\",\n    \"RingColor\": \"0,180,255,200\",\n    \"RingSpeed\": \"1.6529412\",\n    \"RingAmplitude\": \"0.86372554\",\n    \"RingRadius\": \"0.7117647\",\n    \"RingWidth\": \"2.4558823\",\n    \"RingSegments\": \"64\",\n    \"2D ESP\": \"true\",\n    \"2DColor\": \"0,180,255,200\",\n    \"2DWidth\": \"3.0\"\n  },\n  \"BlockHighlight\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"BlockHighlight\",\n    \"Color\": \"255,0,0,255\",\n    \"LineWidth\": \"1.0\"\n  },\n  \"ChestESP\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"ChestESP\",\n    \"ChestColor\": \"255,165,0,255\",\n    \"BarrelColor\": \"139,69,19,255\",\n    \"ShulkerColor\": \"160,32,240,255\",\n    \"EnderColor\": \"0,200,200,255\",\n    \"LineWidth\": \"1.5\",\n    \"Chests\": \"true\",\n    \"Barrels\": \"true\",\n    \"Shulkers\": \"true\",\n    \"EnderChests\": \"true\",\n    \"Filled\": \"false\",\n    \"Range\": \"64\"\n  },\n  \"ArrayList\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"ArrayList\",\n    \"Mode\": \"LIQUIDBOUNCE\",\n    \"Rainbow\": \"false\",\n    \"Red\": \"97\",\n    \"Green\": \"6\",\n    \"Blue\": \"255\",\n    \"Bloom\": \"true\",\n    \"BloomSize\": \"6\"\n  },\n  \"TargetHUD\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"TargetHUD\",\n    \"Position\": \"0.5,0.5\",\n    \"ModernMode\": \"true\"\n  },\n  \"Chams\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Chams\"\n  },\n  \"PlayerESP\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"PlayerESP\",\n    \"Box\": \"true\",\n    \"Corner\": \"true\",\n    \"2D\": \"true\",\n    \"Filled\": \"true\",\n    \"Healthbar\": \"true\",\n    \"Nametag\": \"true\",\n    \"Players\": \"true\",\n    \"Mobs\": \"true\",\n    \"Animals\": \"true\",\n    \"Invisibles\": \"true\",\n    \"Friends\": \"false\",\n    \"HideSelf\": \"true\",\n    \"Color\": \"0,255,0,255\",\n    \"FriendColor\": \"0,255,100,200\",\n    \"LineWidth\": \"2.0\",\n    \"FilledAlpha\": \"30\",\n    \"Rainbow\": \"true\",\n    \"DistScale\": \"true\",\n    \"Range\": \"4\"\n  },\n  \"Animations\": {\n    \"Enabled\": \"false\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Animations\",\n    \"FakeAutoBlock\": \"true\",\n    \"OldAnimations\": \"true\",\n    \"OldBlockHit\": \"true\",\n    \"1.7Shield\": \"true\"\n  },\n  \"Ambience\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"Ambience\",\n    \"CustomTime\": \"false\",\n    \"Time\": \"6000\",\n    \"RainStrength\": \"0.0\",\n    \"ThunderStrength\": \"0.0\",\n    \"Gamma\": \"2.0\",\n    \"NoFog\": \"false\"\n  },\n  \"WTap\": {\n    \"Enabled\": \"true\",\n    \"Drawn\": \"true\",\n    \"Keybind\": -1,\n    \"DisplayName\": \"WTap\",\n    \"Duration\": \"2\",\n    \"Chance\": \"70\"\n  }\n}\n"
         );
      } catch (Exception var2) {
         OyVey.LOGGER.error("[WelcomeScreen] Failed to apply DeathZone preset", var2);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static int applyAlpha(int rgb, int alpha) {
      return Mth.clamp(alpha, 0, 255) << 24 | rgb & 16777215;
   }
}
