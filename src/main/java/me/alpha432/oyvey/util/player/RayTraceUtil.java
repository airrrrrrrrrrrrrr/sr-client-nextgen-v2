package me.alpha432.oyvey.util.player;

import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class RayTraceUtil implements Util {
   public static BlockHitResult rayTrace(float distance, float yaw, float pitch) {
      if (mc.player == null || mc.level == null) {
         return null;
      }

      Vec3 eye = mc.player.getEyePosition();
      Vec3 look = Vec3.directionFromRotation(pitch, yaw);
      Vec3 end = eye.add(look.scale(distance));
      return mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
   }

   public static boolean getOver(Direction direction, BlockPos pos, boolean highDistance, float maxDistance, float yaw, float pitch) {
      BlockHitResult hit = rayTrace(maxDistance, yaw, pitch);
      if (hit == null || hit.getType() != HitResult.Type.BLOCK || direction == null || pos == null) {
         return false;
      }

      return hit.getBlockPos().equals(pos) && hit.getDirection() == direction;
   }

   public static boolean isLookingAtBlock(Direction direction, BlockPos pos, boolean highDistance, float maxDistance, float yaw, float pitch) {
      BlockHitResult hit = rayTrace(maxDistance, yaw, pitch);
      if (hit == null || hit.getType() != HitResult.Type.BLOCK || direction == null || pos == null) {
         return false;
      }

      BlockPos hitPos = hit.getBlockPos();
      if (hitPos.equals(pos)) {
         return true;
      }

      for (Direction dir : Direction.values()) {
         if (hitPos.equals(pos.relative(dir)) && hit.getDirection() == dir.getOpposite()) {
            return true;
         }
      }

      return highDistance && hitPos.distSqr(pos) <= 1.0;
   }
}
