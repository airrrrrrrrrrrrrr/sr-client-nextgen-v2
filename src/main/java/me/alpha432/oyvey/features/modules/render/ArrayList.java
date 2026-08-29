package me.alpha432.oyvey.features.modules.render;

import java.awt.Color;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.Feature;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ArrayList extends Module {
   public final Setting<ArrayList.Mode> mode = this.register(new Setting<>("Mode", ArrayList.Mode.LIQUIDBOUNCE));
   public final Setting<Boolean> rainbow = this.register(new Setting<>("Rainbow", false));
   public final Setting<Integer> r = this.register(new Setting<>("Red", 255, 0, 255, v -> !this.rainbow.getValue()));
   public final Setting<Integer> g = this.register(new Setting<>("Green", 0, 0, 255, v -> !this.rainbow.getValue()));
   public final Setting<Integer> b = this.register(new Setting<>("Blue", 200, 0, 255, v -> !this.rainbow.getValue()));
   public final Setting<Boolean> bloom = this.register(new Setting<>("Bloom", true));
   public final Setting<Integer> bloomSize = this.register(new Setting<>("BloomSize", 3, 1, 6, v -> this.bloom.getValue()));
   private final Map<String, Long> lbAnimMap = new HashMap<>();

   public ArrayList() {
      super("ArrayList", "Displays enabled modules", Module.Category.RENDER);
   }

   private int accentColor(int y, int alpha) {
      if (this.rainbow.getValue()) {
         float hue = (float)((System.currentTimeMillis() / 10L + y * 8L) % 3600L) / 3600.0F;
         Color c = Color.getHSBColor(hue, 1.0F, 1.0F);
         return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha).getRGB();
      } else {
         return new Color(this.r.getValue(), this.g.getValue(), this.b.getValue(), alpha).getRGB();
      }
   }

   @Override
   public void onRender2D(Render2DEvent event) {
      if (mc.player != null && mc.level != null) {
         List<Module> mods = OyVey.moduleManager
            .getModules()
            .stream()
            .filter(Module::isEnabled)
            .filter(Module::isDrawn)
            .sorted(Comparator.comparingInt(m -> mc.font.width(m.getName() + (m.getDisplayInfo() != null ? " " + m.getDisplayInfo() : ""))))
            .collect(Collectors.toList());
         Collections.reverse(mods);
         GuiGraphicsExtractor gfx = event.getContext();
         int sw = mc.getWindow().getGuiScaledWidth();
         int lh = 10;
         int yOff = 2;
         long now = System.currentTimeMillis();
         if (this.mode.getValue() == ArrayList.Mode.LIQUIDBOUNCE) {
            Set<String> currentNames = mods.stream().map(Feature::getName).collect(Collectors.toSet());
            this.lbAnimMap.keySet().retainAll(currentNames);

            for (Module mod : mods) {
               this.lbAnimMap.putIfAbsent(mod.getName(), now);
            }
         }

         for (Module mod : mods) {
            String plain = mod.getName() + (mod.getDisplayInfo() != null ? " " + mod.getDisplayInfo() : "");
            String tag = mod.getName() + (mod.getDisplayInfo() != null ? " \u00a77" + mod.getDisplayInfo() : "");
            int pw = mc.font.width(plain);
            int x = sw - pw - 2;
            int acc = this.accentColor(yOff, 255);
            switch ((ArrayList.Mode)this.mode.getValue()) {
               case LIQUIDBOUNCE:
                  long spawnTime = this.lbAnimMap.getOrDefault(mod.getName(), now);
                  float slideT = Math.min(1.0F, (float)(now - spawnTime) / 220.0F);
                  float ease = 1.0F - (1.0F - slideT) * (1.0F - slideT);
                  int slideOff = (int)((1.0F - ease) * (pw + 20));
                  int ACCENT_W = 2;
                  int PAD_R = 4;
                  int PAD_L = 5;
                  int textX = sw - 2 - 4 - pw + slideOff;
                  int bgX = textX - 5;
                  int bgEnd = sw - 2 + slideOff;
                  int barX = sw - 2 + slideOff;
                  RenderUtil.roundedRect(gfx, bgX, yOff - 1, bgEnd - bgX, lh + 1, 3, -872152050);
                  gfx.fill(barX, yOff - 1, barX + 2, yOff + lh, acc);
                  if (this.bloom.getValue()) {
                     this.drawBloom(gfx, barX, yOff - 1, 2, lh + 1, acc);
                  }

                  gfx.text(mc.font, mod.getName(), textX, yOff, -1, true);
                  if (mod.getDisplayInfo() != null) {
                     gfx.text(mc.font, " " + mod.getDisplayInfo(), textX + mc.font.width(mod.getName()), yOff, -7829317, false);
                  }
                  break;
               case NORMAL:
                  if (this.bloom.getValue()) {
                     this.drawBloom(gfx, x - 2, yOff - 1, pw + 4, lh, acc);
                  }

                  gfx.text(mc.font, tag, x, yOff, acc, true);
                  break;
               case FLAT:
                  RenderUtil.roundedRect(gfx, x - 2, yOff - 1, sw - (x - 2), lh + 1, 3, new Color(0, 0, 0, 140).getRGB());
                  gfx.fill(x - 2, yOff - 1, x - 1, yOff + lh, acc);
                  if (this.bloom.getValue()) {
                     this.drawBloom(gfx, x - 2, yOff - 1, pw + 4, lh, acc);
                  }

                  gfx.text(mc.font, tag, x, yOff, -1, false);
                  break;
               case BRACKET:
                  int bx = sw - mc.font.width("[" + plain + "]") - 2;
                  if (this.bloom.getValue()) {
                     this.drawBloom(gfx, bx - 1, yOff - 1, mc.font.width("[" + plain + "]") + 2, lh, acc);
                  }

                  gfx.text(mc.font, "\u00a7r[\u00a7r", bx, yOff, acc, false);
                  gfx.text(mc.font, tag, bx + mc.font.width("["), yOff, -1, true);
                  gfx.text(mc.font, "\u00a7r]", bx + mc.font.width("[" + plain), yOff, acc, false);
                  break;
               case GRADIENT:
                  float hue2 = (float)((System.currentTimeMillis() / 10L + yOff * 8L + 900L) % 3600L) / 3600.0F;
                  Color c2 = this.rainbow.getValue() ? Color.getHSBColor(hue2, 1.0F, 1.0F) : new Color(this.r.getValue(), this.g.getValue(), this.b.getValue());
                  int acc2 = new Color(c2.getRed(), c2.getGreen(), c2.getBlue(), 255).getRGB();
                  if (this.bloom.getValue()) {
                     this.drawBloom(gfx, x - 2, yOff - 1, pw + 4, lh, acc);
                  }

                  RenderUtil.gradient(
                     gfx,
                     x - 2,
                     yOff - 1,
                     sw,
                     yOff + lh,
                     new Color(0, 0, 0, 0).getRGB(),
                     new Color(0, 0, 0, 0).getRGB(),
                     new Color(0, 0, 0, 80).getRGB(),
                     new Color(0, 0, 0, 80).getRGB()
                  );
                  this.drawGradientText(gfx, plain, x, yOff, acc, acc2);
                  break;
               case OUTLINED:
                  int shadow = new Color(0, 0, 0, 180).getRGB();
                  gfx.text(mc.font, tag, x - 1, yOff, shadow, false);
                  gfx.text(mc.font, tag, x + 1, yOff, shadow, false);
                  gfx.text(mc.font, tag, x, yOff - 1, shadow, false);
                  gfx.text(mc.font, tag, x, yOff + 1, shadow, false);
                  if (this.bloom.getValue()) {
                     this.drawBloom(gfx, x - 2, yOff - 1, pw + 4, lh, acc);
                  }

                  gfx.text(mc.font, tag, x, yOff, acc, false);
                  break;
               case MODERN:
                  RenderUtil.roundedRect(gfx, sw - pw - 8, yOff - 1, pw + 8, lh + 1, 3, new Color(0, 0, 0, 90).getRGB());
                  gfx.fill(sw - 2, yOff - 1, sw, yOff + lh, acc);
                  if (this.bloom.getValue()) {
                     this.drawBloom(gfx, sw - pw - 8, yOff - 1, pw + 8, lh, acc);
                  }

                  gfx.text(mc.font, tag, x, yOff, -1, false);
            }

            yOff += lh;
         }
      }
   }

   private void drawBloom(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int color) {
      int size = this.bloomSize.getValue();
      Color c = new Color(color, true);

      for (int i = size; i >= 1; i--) {
         int alpha = (int)(c.getAlpha() * 0.15F * (size - i + 1) / size);
         gfx.fill(x - i, y - i, x + w + i, y + h + i, new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha).getRGB());
      }
   }

   private void drawGradientText(GuiGraphicsExtractor gfx, String text, int x, int y, int col1, int col2) {
      int len = text.length();
      if (len != 0) {
         Color c1 = new Color(col1, true);
         Color c2 = new Color(col2, true);
         int cx = x;

         for (int i = 0; i < len; i++) {
            float t = len == 1 ? 0.0F : (float)i / (len - 1);
            int red = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * t);
            int green = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
            int blue = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t);
            String ch = String.valueOf(text.charAt(i));
            gfx.text(mc.font, ch, cx, y, new Color(red, green, blue, 255).getRGB(), true);
            cx += mc.font.width(ch);
         }
      }
   }

   public enum Mode {
      LIQUIDBOUNCE,
      NORMAL,
      FLAT,
      BRACKET,
      GRADIENT,
      OUTLINED,
      MODERN;
   }
}
