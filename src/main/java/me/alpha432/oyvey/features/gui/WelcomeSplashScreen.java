package me.alpha432.oyvey.features.gui;

import java.awt.Color;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.manager.PreferencesManager;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class WelcomeSplashScreen extends Screen {
   public static boolean shownThisSession = false;
   private static final long DURATION_MS = 3000L;
   private long startMs;

   public WelcomeSplashScreen() {
      super(Component.literal("welcome_splash"));
   }

   public void init() {
      this.startMs = System.currentTimeMillis();
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      long elapsed = System.currentTimeMillis() - this.startMs;
      if (elapsed >= 3000L) {
         this.minecraft.setScreen(new CustomMainMenuScreen());
      } else {
         float t = (float)elapsed / 3000.0F;
         float alpha = Math.min(Mth.clamp(t / 0.2F, 0.0F, 1.0F), Mth.clamp((1.0F - t) / 0.2F, 0.0F, 1.0F));
         int accent = OyVey.colorManager.getColorAsInt();
         int aR = accent >> 16 & 0xFF;
         int aG = accent >> 8 & 0xFF;
         int aB = accent & 0xFF;
         g.fill(0, 0, this.width, this.height, -16382450);
         float pulse = (float)(Math.sin(t * 3.141592653589793 * 1.5) * 0.25 + 0.75);
         RenderUtil.verticalGradient(g, 0.0F, 0.0F, this.width, this.height, new Color(aR, aG, aB, (int)(alpha * pulse * 28.0F)), new Color(0, 0, 0, 0));
         RenderUtil.verticalGradient(
            g, 0.0F, this.height / 2.0F, this.width, this.height, new Color(0, 0, 0, 0), new Color(aR, aG, aB, (int)(alpha * pulse * 18.0F))
         );
         String text = "Welcome";
         if (OyVey.preferences != null) {
            PreferencesManager.Language lang = OyVey.preferences.getLanguage();
            if (lang == PreferencesManager.Language.SPANISH) {
               text = "Bienvenido";
            } else if (lang == PreferencesManager.Language.GERMAN) {
               text = "Willkommen";
            }
         }

         float scale = 3.2F;
         int titleY = this.height / 2 - 12;
         g.pose().pushMatrix();
         g.pose().scale(scale, scale);
         int tx = (int)((this.width / 2.0F - this.font.width(text) * scale / 2.0F) / scale);
         int ty = (int)(titleY / scale);

         for (int d = 5; d >= 1; d--) {
            int gc = applyAlpha(aR << 16 | aG << 8 | aB, (int)(alpha * 38.0F / d));
            g.text(this.font, text, tx - d, ty, gc, false);
            g.text(this.font, text, tx + d, ty, gc, false);
            g.text(this.font, text, tx, ty - d, gc, false);
            g.text(this.font, text, tx, ty + d, gc, false);
         }

         g.text(this.font, text, tx + 1, ty + 1, applyAlpha(0, (int)(alpha * 130.0F)), false);
         g.text(this.font, text, tx, ty, applyAlpha(aR << 16 | aG << 8 | aB, (int)(alpha * 255.0F)), false);
         g.pose().popMatrix();
         g.centeredText(this.font, "sr client", this.width / 2, titleY + 18, applyAlpha(5601194, (int)(alpha * 190.0F)));
         int lineW = (int)(alpha * 100.0F);
         if (lineW > 0) {
            int ly = titleY + 30;
            RenderUtil.horizontalGradient(
               g, this.width / 2.0F - lineW, ly, this.width / 2.0F, ly + 1, new Color(0, 0, 0, 0), new Color(aR, aG, aB, (int)(alpha * 160.0F))
            );
            RenderUtil.horizontalGradient(
               g, this.width / 2.0F, ly, this.width / 2.0F + lineW, ly + 1, new Color(aR, aG, aB, (int)(alpha * 160.0F)), new Color(0, 0, 0, 0)
            );
         }
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static int applyAlpha(int rgb, int alpha) {
      return Mth.clamp(alpha, 0, 255) << 24 | rgb & 16777215;
   }
}
