package me.alpha432.oyvey.features.modules.render;

import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.HudModule;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class MinimapModule extends HudModule {
   public final Setting<Integer> size = this.register(new Setting<>("Size", 90, 50, 200));
   public final Setting<Double> zoom = this.register(new Setting<>("Zoom", 48.0, 8.0, 256.0));
   public final Setting<Boolean> rotate = this.register(new Setting<>("Rotate", false));
   public final Setting<Boolean> showSelf = this.register(new Setting<>("ShowSelf", true));

   public MinimapModule() {
      super("Minimap", "Radar showing nearby players.", 90.0F, 90.0F, Module.Category.RENDER);
   }

   @Override
   protected void render(Render2DEvent e) {
      super.render(e);
      if (!nullCheck() && mc.level != null) {
         GuiGraphicsExtractor gfx = e.getContext();
         int sz = this.size.getValue();
         int half = sz / 2;
         int cx = (int)this.getX() + half;
         int cy = (int)this.getY() + half;
         int clipR = half - 3;
         RenderUtil.roundedRect(gfx, (int)this.getX(), (int)this.getY(), sz, sz, half - 2, 855638016);
         double scale = sz / (this.zoom.getValue() * 2.0);
         Vec3 selfPosition = RenderUtil.interpolatedPosition(mc.player, e.getDelta());

         for (AbstractClientPlayer player : mc.level.players()) {
            boolean isSelf = player == mc.player;
            if (!isSelf || this.showSelf.getValue()) {
               Vec3 playerPosition = RenderUtil.interpolatedPosition(player, e.getDelta());
               double dx = playerPosition.x - selfPosition.x;
               double dz = playerPosition.z - selfPosition.z;
               if (this.rotate.getValue()) {
                  double yawRad = Math.toRadians(Mth.rotLerp(e.getDelta(), mc.player.yRotO, mc.player.getYRot()));
                  double sin = Math.sin(yawRad);
                  double cos = Math.cos(yawRad);
                  double rx = dx * cos + dz * sin;
                  double rz = -dx * sin + dz * cos;
                  dx = rx;
                  dz = rz;
               }

               int sx = (int)(cx + dx * scale);
               int sy = (int)(cy + dz * scale);
               int ox = sx - cx;
               int oy = sy - cy;
               if (ox * ox + oy * oy <= clipR * clipR) {
                  if (isSelf) {
                     gfx.fill(sx - 1, sy - 1, sx + 2, sy + 2, -1);
                  } else {
                     gfx.fill(sx, sy, sx + 2, sy + 2, -5579265);
                  }
               }
            }
         }

         this.setWidth(sz);
         this.setHeight(sz);
      }
   }
}
