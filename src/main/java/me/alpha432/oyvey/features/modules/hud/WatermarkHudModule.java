package me.alpha432.oyvey.features.modules.hud;

import java.awt.Color;
import com.google.gson.JsonElement;
import java.util.Locale;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.client.HudModule;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.HudFont;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class WatermarkHudModule extends HudModule {
   public final Setting<HudFont> font = this.register(new Setting<>("Font", HudFont.ARIAL));
   public final Setting<Boolean> customColors = this.register(new Setting<>("CustomColors", false));
   public final Setting<Boolean> rainbow = this.register(new Setting<>("Rainbow", false));
   public final Setting<Integer> r = this.register(new Setting<>("Red", 211, 0, 255, v -> !this.rainbow.getValue()));
   public final Setting<Integer> g = this.register(new Setting<>("Green", 95, 0, 255, v -> !this.rainbow.getValue()));
   public final Setting<Integer> b = this.register(new Setting<>("Blue", 196, 0, 255, v -> !this.rainbow.getValue()));

   public WatermarkHudModule() {
      super("Watermark", "SR 3.0 badge with live FPS and selectable fonts.", 120.0F, 20.0F);
   }

   @Override
   public void fromJson(JsonElement element) {
      super.fromJson(element);
      String savedName = this.getDisplayName().toLowerCase(Locale.ROOT);
      if (savedName.contains("deathzone") || savedName.contains("nextgen") || savedName.contains("nextegen")) {
         this.displayName.setValueNoEvent("Watermark");
      }
   }

   @Override
   protected void render(Render2DEvent event) {
      if (nullCheck()) {
         return;
      }
      HudFont selected = this.font.getValue();
      var label = selected.text("SR").withStyle(style -> style.withColor(this.accentColor()));
      label.append(selected.text(" | ").withStyle(style -> style.withColor(0x969696)));
      label.append(selected.text("3.0").withStyle(style -> style.withColor(0xEAEAEA)));
      label.append(selected.text(" | ").withStyle(style -> style.withColor(0x969696)));
      label.append(selected.text(mc.getFps() + " FPS").withStyle(style -> style.withColor(0xEAEAEA)));
      this.setWidth(mc.font.width(label) + 16);
      this.setHeight(20.0F);
      super.render(event);
      int x = (int)this.getX();
      int y = (int)this.getY();
      var gfx = event.getContext();
      drawBadge(gfx, x, y, this.getWidth(), 20);
      gfx.text(mc.font, label, x + 8, y + 6, -1, true);
   }

   private int accentColor() {
      return this.getAccentColor(System.currentTimeMillis());
   }

   public static void drawBadge(GuiGraphicsExtractor graphics, float x, float y, float width, float height) {
      RenderUtil.roundedRect(graphics, x - 1, y - 1, width + 2, height + 2, 7, 0x20505050);
      RenderUtil.roundedRect(graphics, x, y, width, height, 6, 0xDC494949);
      RenderUtil.roundedOutline(graphics, x, y, width, height, 6, 1, 0x55858585);
   }

   public int getAccentColor(long now) {
      if (this.rainbow.getValue()) {
         return Color.HSBtoRGB((now % 6000L) / 6000.0F, 0.82F, 1.0F) & 0xFFFFFF;
      }
      if (this.customColors.getValue()) {
         return new Color(this.r.getValue(), this.g.getValue(), this.b.getValue()).getRGB() & 0xFFFFFF;
      }
      return 0x69CDFF;
   }

}
