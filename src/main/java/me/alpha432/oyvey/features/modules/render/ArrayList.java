package me.alpha432.oyvey.features.modules.render;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.hud.WatermarkHudModule;
import me.alpha432.oyvey.features.modules.client.SettingsModule;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.HudFont;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;

/** Right aligned module list matching the compact Adjust HUD style. */
public class ArrayList extends Module {
   public final Setting<ArrayList.Mode> mode = this.register(new Setting<>("Mode", ArrayList.Mode.ADJUST));
   public final Setting<HudFont> font = this.register(new Setting<>("Font", HudFont.ARIAL));
   public final Setting<Boolean> customColors = this.register(new Setting<>("CustomColors", false));
   public final Setting<Color> secondaryColor = this.register(new Setting<>("SecondaryColor", new Color(230, 244, 255)));
   public final Setting<Boolean> rainbow = this.register(new Setting<>("Rainbow", false));
   public final Setting<Integer> r = this.register(new Setting<>("Red", 211, 0, 255, v -> !this.rainbow.getValue()));
   public final Setting<Integer> g = this.register(new Setting<>("Green", 95, 0, 255, v -> !this.rainbow.getValue()));
   public final Setting<Integer> b = this.register(new Setting<>("Blue", 196, 0, 255, v -> !this.rainbow.getValue()));
   public final Setting<Integer> rightPadding = this.register(new Setting<>("RightPadding", 4, 0, 40));
   public final Setting<Integer> lineHeight = this.register(new Setting<>("LineHeight", 10, 8, 20));
   public final Setting<Boolean> showInfo = this.register(new Setting<>("ShowInfo", true));
   public final Setting<Boolean> shadow = this.register(new Setting<>("Shadow", true));

   public ArrayList() {
      super("ArrayList", "Displays enabled modules in the compact Adjust layout", Module.Category.RENDER);
   }

   @Override
   public void onRender2D(Render2DEvent event) {
      if (mc.player == null || mc.level == null) {
         return;
      }

      GuiGraphicsExtractor gfx = event.getContext();
      int screenWidth = mc.getWindow().getGuiScaledWidth();
      int y = 4;
      int spacing = Math.max(10, this.lineHeight.getValue());
      int right = screenWidth - this.rightPadding.getValue();
      int availableWidth = Math.max(1, right - 4);
      List<Row> rows = OyVey.moduleManager.getModules().stream()
         .filter(Module::isEnabled)
         .filter(Module::isDrawn)
         .filter(SettingsModule::isArrayListVisible)
         .filter(module -> !module.hidden && module != this && !(module instanceof WatermarkHudModule))
         .map(module -> this.buildRow(module, availableWidth))
         .sorted(Comparator.comparingInt(Row::width).reversed().thenComparing(row -> row.label().getString()))
         .collect(Collectors.toList());

      for (Row row : rows) {
         if (y + spacing > mc.getWindow().getGuiScaledHeight() - 4) {
            break;
         }
         gfx.text(mc.font, row.label(), right - row.width(), y, this.accentColor(y), this.shadow.getValue());
         y += spacing;
      }
   }

   private Row buildRow(Module module, int availableWidth) {
      HudFont selected = this.font.getValue();
      String name = ChatFormatting.stripFormatting(module.getDisplayName()).replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
      String info = this.showInfo.getValue() ? module.getDisplayInfo() : null;
      MutableComponent label = selected.text(name);
      if (info != null && !info.isBlank()) {
         label.append(selected.text(" " + ChatFormatting.stripFormatting(info)).withStyle(style -> style.withColor(0xBBBBBB)));
      }
      // Measure exactly the same styled component that will be submitted.
      int width = mc.font.width(label);
      if (width > availableWidth) {
         String plain = label.getString();
         do {
            plain = plain.substring(0, plain.offsetByCodePoints(plain.length(), -1));
            label = selected.text(plain + "...");
            width = mc.font.width(label);
         } while (!plain.isEmpty() && width > availableWidth);
      }
      return new Row(label, width);
   }

   private record Row(MutableComponent label, int width) { }

   private int accentColor(int y) {
      if (this.rainbow.getValue()) {
         float hue = (float)((System.currentTimeMillis() / 12L + y * 9L) % 3600L) / 3600.0F;
         Color color = Color.getHSBColor(hue, 0.82F, 1.0F);
         return color.getRGB() | 0xFF000000;
      }

      Color base = this.customColors.getValue()
         ? new Color(this.r.getValue(), this.g.getValue(), this.b.getValue())
         : this.mode.getValue() == Mode.OPAL ? new Color(74, 185, 232) : new Color(190, 91, 181);
      if (this.mode.getValue() == Mode.OPAL) {
         Color end = this.customColors.getValue() ? this.secondaryColor.getValue() : new Color(230, 244, 255);
         float row = (y - 4.0F) / Math.max(10, this.lineHeight.getValue());
         float blend = (float)(0.5 + 0.5 * Math.cos(row * Math.PI / 2.5));
         return new Color((int)(base.getRed() + (end.getRed() - base.getRed()) * blend),
            (int)(base.getGreen() + (end.getGreen() - base.getGreen()) * blend),
            (int)(base.getBlue() + (end.getBlue() - base.getBlue()) * blend)).getRGB();
      }
      return base.getRGB();
   }

   public enum Mode {
      ADJUST,
      OPAL;
   }

}
