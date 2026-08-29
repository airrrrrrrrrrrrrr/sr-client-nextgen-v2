package me.alpha432.oyvey.manager;

import java.util.ArrayList;
import java.util.List;
import me.alpha432.oyvey.event.impl.entity.player.TickEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.Feature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

public class HoleManager extends Feature {
   private static final int RANGE = 8;
   private final List<HoleManager.Hole> holes = new ArrayList<>();
   private final MutableBlockPos pos = new MutableBlockPos();

   public HoleManager() {
      EVENT_BUS.register(this);
   }

   @Subscribe
   private void onTick(TickEvent event) {
      this.holes.clear();

      for (int x = -8; x < 8; x++) {
         for (int y = -8; y < 8; y++) {
            for (int z = -8; z < 8; z++) {
               this.pos.set(mc.player.getX() + x, mc.player.getY() + y, mc.player.getZ() + z);
               HoleManager.Hole hole = this.getHole(this.pos);
               if (hole != null) {
                  this.holes.add(hole);
               }
            }
         }
      }
   }

   @Nullable
   public HoleManager.Hole getHole(BlockPos pos) {
      if (mc.level.getBlockState(pos).getBlock() != Blocks.AIR) {
         return null;
      }

      HoleManager.HoleType type = HoleManager.HoleType.BEDROCK;

      for (Direction direction : Plane.HORIZONTAL) {
         Block block = mc.level.getBlockState(pos.relative(direction)).getBlock();
         if (block == Blocks.OBSIDIAN) {
            type = HoleManager.HoleType.UNSAFE;
         } else if (block != Blocks.BEDROCK) {
            return null;
         }
      }

      return new HoleManager.Hole(pos, type);
   }

   public List<HoleManager.Hole> getHoles() {
      return this.holes;
   }

   public boolean isHole(BlockPos pos) {
      return this.holes.stream().anyMatch(hole -> hole.pos().equals(pos));
   }

   public record Hole(BlockPos pos, HoleManager.HoleType holeType) {
   }

   private enum HoleType {
      BEDROCK,
      UNSAFE;
   }
}
