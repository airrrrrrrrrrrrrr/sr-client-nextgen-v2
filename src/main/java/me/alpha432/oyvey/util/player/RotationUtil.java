package me.alpha432.oyvey.util.player;

import me.alpha432.oyvey.util.RotationUtils;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class RotationUtil implements Util {
   public static float forwardYaw() {
      return mc.player == null ? 0.0F : mc.player.getYRot();
   }

   public static float[] getDirectionToBlock(BlockPos pos, Direction facing) {
      if (mc.player == null || pos == null || facing == null) {
         return new float[]{forwardYaw(), mc.player == null ? 0.0F : mc.player.getXRot()};
      }

      Vec3 dir = new Vec3(
         pos.getX() + 0.5 + (double) facing.getStepX() * 0.5,
         pos.getY() + 0.5 + (double) facing.getStepY() * 0.5,
         pos.getZ() + 0.5 + (double) facing.getStepZ() * 0.5
      );
      return RotationUtils.fromRotationVec(dir.subtract(mc.player.getEyePosition()));
   }
}
