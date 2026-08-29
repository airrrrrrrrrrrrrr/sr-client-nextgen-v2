package me.alpha432.oyvey.features.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import me.alpha432.oyvey.util.ColorUtil;
import me.alpha432.oyvey.util.Lang;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class CustomMainMenuScreen extends Screen {
   private static final int STARS = 110;
   private final float[] sx = new float[110];
   private final float[] sy = new float[110];
   private final float[] svx = new float[110];
   private final float[] svy = new float[110];
   private final float[] sa = new float[110];
   private final List<CustomMainMenuScreen.Btn> buttons = new ArrayList<>();
   private static final int BW = 180;
   private static final int BH = 22;
   private static final int BGAP = 6;
   private long openTime;

   public CustomMainMenuScreen() {
      super(Component.literal("sr client"));
   }

   public void init() {
      this.openTime = System.currentTimeMillis();
      this.buttons.clear();
      Random rng = new Random();

      for (int i = 0; i < 110; i++) {
         this.sx[i] = rng.nextFloat() * this.width;
         this.sy[i] = rng.nextFloat() * this.height;
         float angle = rng.nextFloat() * 6.2831855F;
         float spd = 0.04F + rng.nextFloat() * 0.14F;
         this.svx[i] = (float)Math.cos(angle) * spd;
         this.svy[i] = (float)Math.sin(angle) * spd;
         this.sa[i] = 0.25F + rng.nextFloat() * 0.55F;
      }

      int bw = 175;
      int bh = 22;
      int gapX = 12;
      int rowStep = 28;
      int rows = 4;
      int cx = this.width / 2;
      int leftX = cx - gapX / 2 - bw;
      int rightX = cx + gapX / 2;
      int by = Math.min((int)(this.height * 0.46F), this.height - 30 - ((rows - 1) * rowStep + bh));
      this.buttons
         .add(
            new CustomMainMenuScreen.Btn(
               Lang.get("menu.singleplayer"), Lang.get("menu.hint.explore"), leftX, by, bw, bh, () -> this.minecraft.setScreen(new SelectWorldScreen(this))
            )
         );
      this.buttons
         .add(
            new CustomMainMenuScreen.Btn(
               Lang.get("menu.multiplayer"), Lang.get("menu.hint.bypass"), rightX, by, bw, bh, () -> this.minecraft.setScreen(new CustomServerListScreen())
            )
         );
      this.buttons
         .add(
            new CustomMainMenuScreen.Btn(
               Lang.get("menu.accounts"), Lang.get("menu.hint.alts"), leftX, by + rowStep, bw, bh, () -> this.minecraft.setScreen(new AccountManagerScreen())
            )
         );
      this.buttons
         .add(
            new CustomMainMenuScreen.Btn(
               Lang.get("menu.capes"), Lang.get("menu.hint.custom"), rightX, by + rowStep, bw, bh, () -> this.minecraft.setScreen(new CapeScreen())
            )
         );
      this.buttons
         .add(
            new CustomMainMenuScreen.Btn(
               Lang.get("menu.options"),
               "",
               leftX,
               by + 2 * rowStep,
               bw,
               bh,
               () -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options, false))
            )
         );
      this.buttons
         .add(
            new CustomMainMenuScreen.Btn(
               Lang.get("menu.proxies"), Lang.get("menu.hint.route"), rightX, by + 2 * rowStep, bw, bh, () -> this.minecraft.setScreen(new ProxyScreen())
            )
         );
      this.buttons
         .add(
            new CustomMainMenuScreen.Btn(
               Lang.get("menu.quit"), Lang.get("menu.hint.cya"), leftX, by + 3 * rowStep, bw * 2 + gapX, bh, () -> this.minecraft.stop()
            )
         );
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      float age = (float)(System.currentTimeMillis() - this.openTime) / 1000.0F;
      float fadeIn = Mth.clamp(age / 0.7F, 0.0F, 1.0F);
      int accent = OyVey.colorManager.getColorAsInt();
      int aR = accent >> 16 & 0xFF;
      int aG = accent >> 8 & 0xFF;
      int aB = accent & 0xFF;
      g.fill(0, 0, this.width, this.height, -16316657);
      int stops = 6;
      float bandH = this.height * 0.5F;

      for (int s = 0; s < stops; s++) {
         Color c0 = ColorUtil.astolfo(s, stops, 8000, 0.55F, 0.9F);
         Color c1 = ColorUtil.astolfo(s + 1, stops, 8000, 0.55F, 0.9F);
         float gx0 = this.width * ((float)s / stops);
         float gx1 = this.width * ((float)(s + 1) / stops);
         int ba = (int)(24.0F * fadeIn);
         RenderUtil.horizontalGradient(
            g, gx0, 0.0F, gx1, bandH, new Color(c0.getRed(), c0.getGreen(), c0.getBlue(), ba), new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), ba)
         );
      }

      float pulse = (float)(Math.sin(age * 0.7) * 0.5 + 0.5);
      RenderUtil.verticalGradient(g, 0.0F, 0.0F, this.width, this.height * 0.45F, new Color(aR, aG, aB, (int)(14.0F + pulse * 18.0F)), new Color(0, 0, 0, 0));
      RenderUtil.verticalGradient(g, 0.0F, this.height * 0.6F, this.width, this.height, new Color(0, 0, 0, 0), new Color(0, 0, 0, 195));

      for (int i = 0; i < 110; i++) {
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

         int a = (int)(this.sa[i] * fadeIn * 155.0F);
         g.fill((int)this.sx[i], (int)this.sy[i], (int)this.sx[i] + 1, (int)this.sy[i] + 1, applyAlpha(aR << 16 | aG << 8 | aB, a));
      }

      this.drawTitle(g, age, fadeIn, aR, aG, aB);

      for (CustomMainMenuScreen.Btn btn : this.buttons) {
         this.drawBtn(g, btn, mx, my, fadeIn, aR, aG, aB);
      }

      boolean sol = OyVey.preferences != null && OyVey.preferences.isShowOnLaunch();
      String setupLabel = "setup on launch: " + (sol ? "on" : "off");
      int slx = 4;
      int sly = this.height - 20;
      boolean setupHov = mx >= slx - 2 && mx < slx + this.font.width(setupLabel) + 2 && my >= sly - 1 && my < sly + 9;
      g.text(this.font, setupLabel, slx, sly, applyAlpha(setupHov ? 8952234 : (sol ? 4875912 : 3030346), (int)(fadeIn * (setupHov ? 200 : 160))), false);
      String prefLabel = Lang.get("menu.preferences");
      int pfx = this.width - this.font.width(prefLabel) - 5;
      int pfy = this.height - 20;
      boolean prefHov = mx >= pfx - 2 && mx < pfx + this.font.width(prefLabel) + 2 && my >= pfy - 1 && my < pfy + 9;
      g.text(this.font, prefLabel, pfx, pfy, applyAlpha(prefHov ? 8952234 : 4018014, (int)(fadeIn * (prefHov ? 200 : 140))), false);
      String ver = "v2.0";
      g.text(this.font, ver, 4, this.height - 10, applyAlpha(4875912, (int)(fadeIn * 220.0F)), false);
      String tag = "sr client 2.0 // nextgen";
      g.text(this.font, tag, this.width - this.font.width(tag) - 4, this.height - 10, applyAlpha(3359829, (int)(fadeIn * 210.0F)), false);
      String toggleLabel = Lang.get("menu.vanilla");
      int tx = this.width / 2 - this.font.width(toggleLabel) / 2;
      int ty = this.height - 10;
      boolean togHov = mx >= tx - 2 && mx < tx + this.font.width(toggleLabel) + 2 && my >= ty - 1 && my < ty + 9;
      g.text(this.font, toggleLabel, tx, ty, applyAlpha(togHov ? 8952234 : 4018014, (int)(fadeIn * (togHov ? 200 : 140))), false);
   }

   private void drawTitle(GuiGraphicsExtractor g, float age, float fadeIn, int aR, int aG, int aB) {
      String title = "SR CLIENT";
      float scale = 3.0F;
      int titleY = (int)(this.height * 0.33F);
      g.pose().pushMatrix();
      g.pose().scale(scale, scale);
      int tx = (int)((this.width / 2.0F - this.font.width(title) * scale / 2.0F) / scale);
      int ty = (int)((titleY - 8.0F) / scale);

      for (int d = 4; d >= 1; d--) {
         int ga = (int)(fadeIn * 40.0F / d);
         int gc = applyAlpha(aR << 16 | aG << 8 | aB, ga);
         g.text(this.font, title, tx - d, ty, gc, false);
         g.text(this.font, title, tx + d, ty, gc, false);
         g.text(this.font, title, tx, ty - d, gc, false);
         g.text(this.font, title, tx, ty + d, gc, false);
      }

      g.text(this.font, title, tx + 1, ty + 1, applyAlpha(0, (int)(fadeIn * 140.0F)), false);
      float sh = age % 4.0F / 4.0F;
      float shA = Mth.clamp(1.0F - Math.abs(sh - 0.1F) * 12.0F, 0.0F, 1.0F);
      if (shA > 0.01F) {
         g.text(this.font, title, tx, ty, applyAlpha(16777215, (int)(fadeIn * shA * 210.0F)), false);
      }

      g.text(this.font, title, tx, ty, applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * 255.0F)), false);
      g.pose().popMatrix();
      g.centeredText(this.font, "nextgen", this.width / 2, titleY + 8, applyAlpha(7838122, (int)(fadeIn * 185.0F)));
      int lineW = (int)Mth.lerp(Mth.clamp(age / 0.55F, 0.0F, 1.0F), 0.0F, 130.0F);
      if (lineW > 0) {
         int lineY = titleY + 21;
         int lc = (int)(fadeIn * 145.0F);
         RenderUtil.horizontalGradient(g, this.width / 2.0F - lineW, lineY, this.width / 2.0F, lineY + 1, new Color(0, 0, 0, 0), new Color(aR, aG, aB, lc));
         RenderUtil.horizontalGradient(g, this.width / 2.0F, lineY, this.width / 2.0F + lineW, lineY + 1, new Color(aR, aG, aB, lc), new Color(0, 0, 0, 0));
      }
   }

   private void drawBtn(GuiGraphicsExtractor g, CustomMainMenuScreen.Btn btn, int mx, int my, float fadeIn, int aR, int aG, int aB) {
      boolean hov = mx >= btn.x() && mx < btn.x() + btn.w() && my >= btn.y() && my < btn.y() + btn.h();
      int bg = hov ? applyAlpha(1842224, (int)(fadeIn * 225.0F)) : applyAlpha(723740, (int)(fadeIn * 175.0F));
      if (hov) {
         RenderUtil.dropShadow(g, btn.x(), btn.y(), btn.w(), btn.h(), 5.0F, 8.0F, (int)(fadeIn * 105.0F));
      }
      RenderUtil.roundedRectGradient(g, btn.x(), btn.y(), btn.w(), btn.h(), 5.0F, bg, applyAlpha(460558, (int)(fadeIn * 195.0F)), true);
      RenderUtil.roundedOutline(
         g, btn.x(), btn.y(), btn.w(), btn.h(), 5.0F, 0.8F, applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * (hov ? 190 : 70)))
      );
      RenderUtil.roundedRect(g, btn.x() + 4, btn.y() + 5, 2, btn.h() - 10, 1, applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * (hov ? 255 : 185))));
      int lc = hov ? applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * 255.0F)) : applyAlpha(12307677, (int)(fadeIn * 235.0F));
      g.text(this.font, btn.label(), btn.x() + 10, btn.y() + (btn.h() - 8) / 2, lc, false);
      if (!btn.hint().isEmpty() && !hov) {
         g.text(
            this.font,
            btn.hint(),
            btn.x() + btn.w() - this.font.width(btn.hint()) - 7,
            btn.y() + (btn.h() - 8) / 2,
            applyAlpha(4085111, (int)(fadeIn * 210.0F)),
            false
         );
      }

      if (hov) {
         g.text(this.font, ">", btn.x() + btn.w() - 11, btn.y() + (btn.h() - 8) / 2, applyAlpha(aR << 16 | aG << 8 | aB, (int)(fadeIn * 215.0F)), false);
      }
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      if (click.button() == 0) {
         double mx = click.x();
         double my = click.y();

         for (CustomMainMenuScreen.Btn b : this.buttons) {
            if (mx >= b.x() && mx < b.x() + b.w() && my >= b.y() && my < b.y() + b.h()) {
               b.action().run();
               return true;
            }
         }

         String toggleLabel = Lang.get("menu.vanilla");
         int tx = this.width / 2 - this.font.width(toggleLabel) / 2;
         int ty = this.height - 10;
         if (mx >= tx - 2 && mx < tx + this.font.width(toggleLabel) + 2 && my >= ty - 1 && my < ty + 9) {
            ClickGuiModule cfg = ClickGuiModule.getInstance();
            if (cfg != null) {
               cfg.customMenu.setValue(false);
               OyVey.configManager.save();
            }

            this.minecraft.setScreen(new TitleScreen());
            return true;
         }

         boolean sol = OyVey.preferences != null && OyVey.preferences.isShowOnLaunch();
         String setupLabel = "setup on launch: " + (sol ? "on" : "off");
         int slx = 4;
         int sly = this.height - 20;
         if (mx >= slx - 2 && mx < slx + this.font.width(setupLabel) + 2 && my >= sly - 1 && my < sly + 9) {
            if (OyVey.preferences != null) {
               OyVey.preferences.setShowOnLaunch(!sol);
               OyVey.configManager.save();
            }

            return true;
         }

         String prefLabel = Lang.get("menu.preferences");
         int pfx = this.width - this.font.width(prefLabel) - 5;
         int pfy = this.height - 20;
         if (mx >= pfx - 2 && mx < pfx + this.font.width(prefLabel) + 2 && my >= pfy - 1 && my < pfy + 9) {
            this.minecraft.setScreen(new WelcomeScreen(false));
            return true;
         }
      }

      return super.mouseClicked(click, doubled);
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static int applyAlpha(int rgb, int alpha) {
      return Mth.clamp(alpha, 0, 255) << 24 | rgb & 16777215;
   }

   private record Btn(String label, String hint, int x, int y, int w, int h, Runnable action) {
   }
}
