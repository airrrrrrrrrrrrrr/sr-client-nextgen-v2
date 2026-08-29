package me.alpha432.oyvey.mixin.render.gui;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.gui.CustomMainMenuScreen;
import me.alpha432.oyvey.features.gui.LoginScreen;
import me.alpha432.oyvey.features.gui.RenderQualityScreen;
import me.alpha432.oyvey.features.gui.WelcomeScreen;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {
   @Inject(method = "init", at = @At("HEAD"), cancellable = true)
   private void redirectToCustomMenu(CallbackInfo ci) {
      if (!RenderQualityScreen.shownThisSession) {
         RenderQualityScreen.shownThisSession = true;
         Minecraft.getInstance().setScreen(new RenderQualityScreen());
         ci.cancel();
      } else if (!LoginScreen.shownThisSession) {
         LoginScreen.shownThisSession = true;
         Minecraft.getInstance().setScreen(new LoginScreen());
         ci.cancel();
      } else {
         ClickGuiModule cfg = ClickGuiModule.getInstance();
         if (cfg == null || cfg.customMenu.getValue()) {
            if (OyVey.preferences != null && OyVey.preferences.isShowOnLaunch() && !WelcomeScreen.shownThisSession) {
               WelcomeScreen.shownThisSession = true;
               Minecraft.getInstance().setScreen(new WelcomeScreen(true));
               ci.cancel();
            } else {
               if (!LoginScreen.authenticated && !LoginScreen.offlineMode) {
                  Minecraft.getInstance().setScreen(new LoginScreen());
               } else {
                  Minecraft.getInstance().setScreen(new CustomMainMenuScreen());
               }

               ci.cancel();
            }
         }
      }
   }

   @Inject(method = "extractRenderState", at = @At("RETURN"))
   private void drawToggleLink(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
      ClickGuiModule cfg = ClickGuiModule.getInstance();
      if (cfg != null && !cfg.customMenu.getValue()) {
         Minecraft mc = Minecraft.getInstance();
         int h = mc.getWindow().getGuiScaledHeight();
         String menuLabel = "custom menu";
         int mx2 = mc.getWindow().getGuiScaledWidth() / 2 - mc.font.width(menuLabel) / 2;
         int my2 = h - 10;
         boolean menuHov = mx >= mx2 - 2 && mx < mx2 + mc.font.width(menuLabel) + 2 && my >= my2 - 1 && my < my2 + 9;
         g.text(mc.font, menuLabel, mx2, my2, menuHov ? -5588020 : -12298906, false);
         boolean sol = OyVey.preferences != null && OyVey.preferences.isShowOnLaunch();
         String welcomeLabel = "welcome: " + (sol ? "on" : "off");
         int wx = 4;
         int wy = h - 10;
         boolean welHov = mx >= wx - 2 && mx < wx + mc.font.width(welcomeLabel) + 2 && my >= wy - 1 && my < wy + 9;
         g.text(mc.font, welcomeLabel, wx, wy, welHov ? -5588020 : (sol ? -12302088 : -13816750), false);
      }
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void onMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
      if (click.button() == 0) {
         ClickGuiModule cfg = ClickGuiModule.getInstance();
         if (cfg != null && !cfg.customMenu.getValue()) {
            Minecraft mc = Minecraft.getInstance();
            int h = mc.getWindow().getGuiScaledHeight();
            String menuLabel = "custom menu";
            int mx = mc.getWindow().getGuiScaledWidth() / 2 - mc.font.width(menuLabel) / 2;
            int my = h - 10;
            if (click.x() >= mx - 2 && click.x() < mx + mc.font.width(menuLabel) + 2 && click.y() >= my - 1 && click.y() < my + 9) {
               cfg.customMenu.setValue(true);
               OyVey.configManager.save();
               mc.setScreen(new CustomMainMenuScreen());
               cir.setReturnValue(true);
               return;
            }

            boolean sol = OyVey.preferences != null && OyVey.preferences.isShowOnLaunch();
            String welcomeLabel = "welcome: " + (sol ? "on" : "off");
            int wx = 4;
            int wy = h - 10;
            if (click.x() >= wx - 2 && click.x() < wx + mc.font.width(welcomeLabel) + 2 && click.y() >= wy - 1 && click.y() < wy + 9) {
               if (OyVey.preferences != null) {
                  OyVey.preferences.setShowOnLaunch(!sol);
                  OyVey.configManager.save();
               }

               cir.setReturnValue(true);
            }
         }
      }
   }
}
