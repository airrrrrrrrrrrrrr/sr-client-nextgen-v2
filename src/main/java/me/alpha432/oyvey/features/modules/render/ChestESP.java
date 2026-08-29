package me.alpha432.oyvey.features.modules.render;

import java.awt.Color;
import java.util.Map.Entry;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

public class ChestESP extends Module {
   public final Setting<Color> chestColor = this.color("ChestColor", 255, 165, 0, 255);
   public final Setting<Color> barrelColor = this.color("BarrelColor", 139, 69, 19, 255);
   public final Setting<Color> shulkerColor = this.color("ShulkerColor", 160, 32, 240, 255);
   public final Setting<Color> enderColor = this.color("EnderColor", 0, 200, 200, 255);
   public final Setting<Float> lineWidth = this.num("LineWidth", 1.5F, 0.5F, 5.0F);
   public final Setting<Boolean> chests = this.register(new Setting<>("Chests", true));
   public final Setting<Boolean> barrels = this.register(new Setting<>("Barrels", true));
   public final Setting<Boolean> shulkers = this.register(new Setting<>("Shulkers", true));
   public final Setting<Boolean> enderChests = this.register(new Setting<>("EnderChests", true));
   public final Setting<Boolean> filled = this.register(new Setting<>("Filled", false));
   public final Setting<Integer> range = this.register(new Setting<>("Range", 64, 16, 256));

   public ChestESP() {
      super("ChestESP", "Highlights nearby storage containers.", Module.Category.RENDER);
   }

   @Subscribe
   @Override
   public void onRender3D(Render3DEvent event) {
      if (mc.player != null && mc.level != null) {
         int r = this.range.getValue() >> 4;
         int cx = mc.player.blockPosition().getX() >> 4;
         int cz = mc.player.blockPosition().getZ() >> 4;

         for (int chunkX = cx - r; chunkX <= cx + r; chunkX++) {
            for (int chunkZ = cz - r; chunkZ <= cz + r; chunkZ++) {
               LevelChunk chunk = mc.level.getChunkSource().getChunkNow(chunkX, chunkZ);
               if (chunk != null) {
                  for (Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                     BlockEntity be = entry.getValue();
                     BlockPos pos = entry.getKey();
                     if (!(
                        mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                           > (double)this.range.getValue().intValue() * this.range.getValue().intValue()
                     )) {
                        Color col = this.getColor(be);
                        if (col != null) {
                           AABB box = new AABB(pos);
                           if (this.filled.getValue()) {
                              RenderUtil.drawBoxFilled(event.getMatrix(), box, withAlpha(col, 40));
                           }

                           RenderUtil.drawBox(event.getMatrix(), box, col, this.lineWidth.getValue());
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private Color getColor(BlockEntity be) {
      if (!(be instanceof ChestBlockEntity) && !(be instanceof TrappedChestBlockEntity)) {
         if (be instanceof BarrelBlockEntity) {
            return this.barrels.getValue() ? this.barrelColor.getValue() : null;
         } else if (be instanceof ShulkerBoxBlockEntity) {
            return this.shulkers.getValue() ? this.shulkerColor.getValue() : null;
         } else if (be instanceof EnderChestBlockEntity) {
            return this.enderChests.getValue() ? this.enderColor.getValue() : null;
         } else {
            return null;
         }
      } else {
         return this.chests.getValue() ? this.chestColor.getValue() : null;
      }
   }

   private static Color withAlpha(Color c, int alpha) {
      return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
   }
}
