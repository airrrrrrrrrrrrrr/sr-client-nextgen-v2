package me.alpha432.oyvey.features.modules.render;

import java.awt.Color;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import me.alpha432.oyvey.util.render.WorldRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

public class TwoDESP extends Module {
   public final Setting<Color> color = this.color("Color", 0, 180, 255, 200);
   public final Setting<Integer> lineWidth = this.num("LineWidth", 1, 1, 3);

   public TwoDESP() {
      super("2DESP", "Draws screen-space boxes at every other loaded player's position.", Category.RENDER);
   }

   @Override
   public void onRender2D(Render2DEvent event) {
      if (mc.player == null || mc.level == null) {
         return;
      }

      GuiGraphicsExtractor graphics = event.getContext();
      int tint = this.color.getValue().getRGB();
      for (Player player : mc.level.players()) {
         if (player == mc.player || !player.isAlive() || player.isRemoved()) {
            continue;
         }

         float[] bounds = WorldRenderer.aabbToScreen(RenderUtil.interpolatedBox(player, event.getDelta()));
         if (bounds == null) {
            continue;
         }

         int left = (int)Math.floor(bounds[0]);
         int top = (int)Math.floor(bounds[1]);
         int right = (int)Math.ceil(bounds[2]);
         int bottom = (int)Math.ceil(bounds[3]);
         if (right <= left || bottom <= top) {
            continue;
         }

         int width = Math.min(this.lineWidth.getValue(), Math.min(right - left, bottom - top));
         graphics.fill(left, top, right, top + width, tint);
         graphics.fill(left, bottom - width, right, bottom, tint);
         if (top + width < bottom - width) {
            graphics.fill(left, top + width, left + width, bottom - width, tint);
            graphics.fill(right - width, top + width, right, bottom - width, tint);
         }
      }
   }
}
