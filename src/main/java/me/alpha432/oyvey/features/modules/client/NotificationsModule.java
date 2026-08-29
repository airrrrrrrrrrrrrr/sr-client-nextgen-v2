package me.alpha432.oyvey.features.modules.client;

import java.util.ArrayList;
import java.util.List;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.ClientEvent;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class NotificationsModule extends Module {
   public Setting<Boolean> moduleToggle = this.bool("Module Toggle", true);
   private static final List<NotificationsModule.Toast> toasts = new ArrayList<>();
   private static final Object LOCK = new Object();
   private static final int W = 140;
   private static final int PAD_X = 8;
   private static final int PAD_Y = 5;
   private static final int GAP = 4;
   private static final int MARGIN_R = 10;
   private static final int MARGIN_B = 10;
   private static final int R = 5;
   private static final long SLIDE_MS = 200L;
   private static final long FADE_MS = 350L;

   public NotificationsModule() {
      super("Notifications", "Displays notifications for various client events", Module.Category.CLIENT);
   }

   public static void post(String title, String message, int accentColor, long durationMs) {
      synchronized (LOCK) {
         toasts.add(new NotificationsModule.Toast(title, message, accentColor, durationMs));
      }
   }

   @Subscribe
   public void onClient(ClientEvent event) {
      if (this.moduleToggle.getValue() && event.getType() == ClientEvent.Type.TOGGLE_MODULE && !(event.getFeature() instanceof ClickGuiModule)) {
         boolean on = event.getFeature().isEnabled();
         int color = on ? OyVey.colorManager.getColorAsIntFullAlpha() : -3390396;
         post(event.getFeature().getName(), on ? "Enabled" : "Disabled", color, 2500L);
      } else if (event.getType() == ClientEvent.Type.SETTING_UPDATE) {
         Setting<?> s = event.getSetting();
         if (s != null
            && "Test".equals(s.getName())
            && Boolean.TRUE.equals(s.getValue())
            && event.getFeature() != null
            && "KillauraRecode".equals(event.getFeature().getName())) {
            post("Test mode", "this is not tested yet dont use", -3390396, 5000L);
         }
      }
   }

   @Subscribe
   public void onRender(Render2DEvent event) {
      Minecraft mc = Minecraft.getInstance();
      GuiGraphicsExtractor ctx = event.getContext();
      int sw = mc.getWindow().getGuiScaledWidth();
      int sh = mc.getWindow().getGuiScaledHeight();
      long now = System.currentTimeMillis();
      List<NotificationsModule.Toast> snapshot;
      synchronized (LOCK) {
         snapshot = new ArrayList<>(toasts);
         toasts.removeIf(tx -> now - tx.spawnTime >= tx.duration + 350L);
      }

      int lineH = 9;
      int nh = 5 + lineH + 2 + lineH + 5;
      int yOff = 10;

      for (int i = snapshot.size() - 1; i >= 0; i--) {
         NotificationsModule.Toast t = snapshot.get(i);
         long elapsed = now - t.spawnTime;
         float alpha;
         if (elapsed < 200L) {
            alpha = (float)elapsed / 200.0F;
         } else if (elapsed < t.duration) {
            alpha = 1.0F;
         } else {
            alpha = 1.0F - Math.min(1.0F, (float)(elapsed - t.duration) / 350.0F);
         }

         if (!(alpha <= 0.0F)) {
            float slideT = Math.min(1.0F, (float)elapsed / 200.0F);
            float ease = 1.0F - (1.0F - slideT) * (1.0F - slideT);
            int slideOff = (int)((1.0F - ease) * 150.0F);
            int nx = sw - 10 - 140 + slideOff;
            int ny = sh - yOff - nh;
            renderToast(ctx, t, nx, ny, nh, alpha);
            yOff += nh + 4;
         }
      }
   }

   private static void renderToast(GuiGraphicsExtractor ctx, NotificationsModule.Toast t, int nx, int ny, int nh, float alpha) {
      int a = (int)(alpha * 255.0F);
      int nw = 140;
      int cR = t.color >> 16 & 0xFF;
      int cG = t.color >> 8 & 0xFF;
      int cB = t.color & 0xFF;
      int[] glowPct = new int[]{14, 8, 4, 2};

      for (int g = 1; g <= 4; g++) {
         int ga = a * glowPct[g - 1] / 100;
         int col = ga << 24 | cR << 16 | cG << 8 | cB;
         RenderUtil.roundedRect(ctx, nx - g, ny - g, nw + g * 2, nh + g * 2, 5 + g, col);
      }

      int borderA = a * 80 / 100;
      RenderUtil.roundedRect(ctx, nx - 1, ny - 1, nw + 2, nh + 2, 6, borderA << 24 | 1842224);
      int bgA = a * 94 / 100;
      RenderUtil.roundedRect(ctx, nx, ny, nw, nh, 5, bgA << 24 | 855320);
      ctx.fill(nx, ny + 5, nx + 3, ny + nh - 5, a << 24 | t.color & 16777215);
      ctx.fill(nx + 5, ny + 1, nx + nw - 5, ny + 2, a * 12 / 100 << 24 | 16777215);
      int titleA = a * 95 / 100;
      ctx.text(Minecraft.getInstance().font, t.title, nx + 3 + 8, ny + 5, titleA << 24 | 14540287, true);
      int msgA = a * 80 / 100;
      int msgRGB;
      if ("Enabled".equals(t.message)) {
         msgRGB = 5631607;
      } else if ("Disabled".equals(t.message)) {
         msgRGB = 16733525;
      } else {
         msgRGB = 11184844;
      }

      ctx.text(Minecraft.getInstance().font, t.message, nx + 3 + 8, ny + 5 + 9 + 2, msgA << 24 | msgRGB, false);
   }

   private static final class Toast {
      final String title;
      final String message;
      final int color;
      final long duration;
      final long spawnTime = System.currentTimeMillis();

      Toast(String title, String message, int color, long duration) {
         this.title = title;
         this.message = message;
         this.color = color;
         this.duration = duration;
      }
   }
}
