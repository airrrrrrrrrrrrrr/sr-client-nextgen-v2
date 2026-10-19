package me.alpha432.oyvey.features.modules.hud;

import java.awt.Color;
import com.google.gson.JsonElement;
import java.util.Locale;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.client.HudModule;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.HudFont;
import me.alpha432.oyvey.util.render.RenderUtil;
import me.alpha432.oyvey.util.render.SkijaGlyphProvider;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.MutableComponent;

public class WatermarkHudModule extends HudModule {
   public enum Mode { DEFAULT, DYNAMIC_ISLAND }

   public final Setting<Mode> mode = this.register(new Setting<>("Mode", Mode.DEFAULT));
   public final Setting<HudFont> font = this.register(new Setting<>("Font", HudFont.ARIAL));
   public final Setting<Boolean> customColors = this.register(new Setting<>("CustomColors", false));
   public final Setting<Boolean> rainbow = this.register(new Setting<>("Rainbow", false));
   public final Setting<Integer> r = this.register(new Setting<>("Red", 211, 0, 255, v -> !this.rainbow.getValue()));
   public final Setting<Integer> g = this.register(new Setting<>("Green", 95, 0, 255, v -> !this.rainbow.getValue()));
   public final Setting<Integer> b = this.register(new Setting<>("Blue", 196, 0, 255, v -> !this.rainbow.getValue()));

   public WatermarkHudModule() {
      super("Watermark", "Default or Dynamic Island watermark with Minecraft username, FPS, ping and rainbow colors.", 120.0F, 20.0F);
   }

   @Override
   public float getX() {
      return this.mode.getValue() == Mode.DYNAMIC_ISLAND
         ? (mc.getWindow().getGuiScaledWidth() - this.getWidth()) / 2.0F : super.getX();
   }

   @Override
   public float getY() {
      return this.mode.getValue() == Mode.DYNAMIC_ISLAND ? 8.0F : super.getY();
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
      boolean island = this.mode.getValue() == Mode.DYNAMIC_ISLAND;
      int fps = mc.getFps();
      var info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(mc.player.getUUID());
      String ping = info == null ? "—ms" : Math.max(0, info.getLatency()) + "ms";
      String[] fields = island ? new String[]{"SR", mc.getUser().getName(), fps + "fps", ping}
         : new String[]{"SR", "3.0", fps + " FPS"};
      String plain = String.join(" | ", fields);
      var label = this.label(selected, fields, island, System.currentTimeMillis());
      // Layout quads include transparent texture padding. Use actual raster
      // pixels, and center the letters optically without the taller separators.
      var bounds = mc.font.prepareText(label.getVisualOrderText(), 0, 0, -1, true, false, 0).bounds();
      var provider = selected == HudFont.MINECRAFT ? null : SkijaGlyphProvider.find("oyvey:hud/" + selected.name().toLowerCase(Locale.ROOT) + ".ttf", 11);
      var ink = provider == null ? null : provider.inkBounds(plain);
      var caps = provider == null ? null : provider.inkBounds(plain.replace("|", ""));
      float textWidth = ink == null ? bounds.width() : ink.getWidth();
      float textLeft = ink == null ? bounds.left() : ink.getLeft();
      var fallbackCaps = mc.font.prepareText(selected.text(plain.replace("|", "")).getVisualOrderText(), 0, 0, -1, false, false, 0).bounds();
      float textHeight = caps == null ? fallbackCaps.height() : caps.getHeight();
      float textTop = caps == null ? fallbackCaps.top() : caps.getTop();
      this.setWidth((float)Math.ceil(textWidth) + (island ? 24 : 16));
      this.setHeight(20.0F);
      // The island stays anchored, without changing Default's saved drag position.
      if (!island) super.render(event);
      float x = this.getX();
      float y = this.getY();
      var gfx = event.getContext();
      if (island) drawIsland(gfx, x, y, this.getWidth(), this.getHeight());
      else drawBadge(gfx, x, y, this.getWidth(), this.getHeight());
      float textX = x + (this.getWidth() - textWidth) / 2.0F - textLeft;
      float textY = y + (this.getHeight() - textHeight) / 2.0F - textTop;
      gfx.pose().pushMatrix();
      gfx.pose().translate(textX, textY);
      gfx.text(mc.font, label, 0, 0, -1, true);
      gfx.pose().popMatrix();
   }

   private MutableComponent label(HudFont selected, String[] fields, boolean island, long now) {
      MutableComponent label = selected.text("");
      int character = 0;
      for (int field = 0; field < fields.length; field++) {
         if (field > 0) {
            label.append(selected.text(" | ").withStyle(style -> style.withColor(island ? 0x686B78 : 0x969696)));
            character += 3;
         }
         if (this.rainbow.getValue()) {
            // A moving color wave colors both modes using the same toggle.
            for (int cp : fields[field].codePoints().toArray()) {
               int color = rainbowColor(now, character++);
               label.append(selected.text(new String(Character.toChars(cp))).withStyle(style -> style.withColor(color)));
            }
         } else {
            int color = field == 0 ? this.getAccentColor(now) : island ? 0xD7D8E2 : 0xEAEAEA;
            label.append(selected.text(fields[field]).withStyle(style -> style.withColor(color)));
            character += fields[field].codePointCount(0, fields[field].length());
         }
      }
      return label;
   }

   private static int rainbowColor(long now, int character) {
      return Color.HSBtoRGB((now % 6000L) / 6000.0F + character * 0.025F, 0.82F, 1.0F) & 0xFFFFFF;
   }

   private static void drawIsland(GuiGraphicsExtractor graphics, float x, float y, float width, float height) {
      float radius = height / 2;
      RenderUtil.dropShadow(graphics, x, y, width, height, radius, 6, 75);
      RenderUtil.roundedRect(graphics, x, y, width, height, radius, 0xE017181E);
      RenderUtil.roundedOutline(graphics, x, y, width, height, radius, 0.75F, 0x304B4D58);
   }

   public static void drawBadge(GuiGraphicsExtractor graphics, float x, float y, float width, float height) {
      RenderUtil.roundedRect(graphics, x - 1, y - 1, width + 2, height + 2, 7, 0x20505050);
      RenderUtil.roundedRect(graphics, x, y, width, height, 6, 0xDC494949);
      RenderUtil.roundedOutline(graphics, x, y, width, height, 6, 1, 0x55858585);
   }

   public int getAccentColor(long now) {
      if (this.rainbow.getValue()) {
         return rainbowColor(now, 0);
      }
      if (this.customColors.getValue()) {
         return new Color(this.r.getValue(), this.g.getValue(), this.b.getValue()).getRGB() & 0xFFFFFF;
      }
      return 0x69CDFF;
   }

}
