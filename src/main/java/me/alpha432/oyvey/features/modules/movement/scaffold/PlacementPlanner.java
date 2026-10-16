package me.alpha432.oyvey.features.modules.movement.scaffold;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Pure placement selection, with world queries supplied by the caller. */
public final class PlacementPlanner {
   private static final Direction[] SUPPORTS = {
      Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP
   };

   private PlacementPlanner() { }

   public interface World {
      boolean replaceable(BlockPos target);
      boolean attachable(BlockPos support, Direction face);
      BlockHitResult raycast(Vec3 from, Vec3 to);
   }

   public record Placement(BlockPos target, BlockHitResult hit) { }

   public static int blockLayer(double feetY) {
      return Mth.floor(feetY) - 1;
   }

   public static List<BlockPos> targets(double x, double z, int layer, double velocityX, double velocityZ) {
      LinkedHashSet<BlockPos> targets = new LinkedHashSet<>();
      targets.add(BlockPos.containing(x, layer, z));
      // Predict only the next edge, then cover the four corners of the feet.
      double aheadX = x + Mth.clamp(velocityX * 2, -0.4, 0.4);
      double aheadZ = z + Mth.clamp(velocityZ * 2, -0.4, 0.4);
      targets.add(BlockPos.containing(aheadX, layer, aheadZ));
      for (double offsetX : new double[]{-0.28, 0.28}) {
         for (double offsetZ : new double[]{-0.28, 0.28}) {
            targets.add(BlockPos.containing(aheadX + offsetX, layer, aheadZ + offsetZ));
         }
      }
      return new ArrayList<>(targets);
   }

   public static Placement find(List<BlockPos> targets, Vec3 eyes, double reach, World world) {
      for (BlockPos target : targets) {
         if (!world.replaceable(target)) continue;
         Placement nearest = null;
         double nearestDistance = Double.POSITIVE_INFINITY;
         for (Direction offset : SUPPORTS) {
            BlockPos support = target.relative(offset);
            Direction face = offset.getOpposite();
            if (!world.attachable(support, face)) continue;
            Vec3 point = Vec3.atCenterOf(support).add(face.getStepX() * 0.5,
               face.getStepY() * 0.5, face.getStepZ() * 0.5);
            double distance = eyes.distanceToSqr(point);
            if (distance > reach * reach || distance >= nearestDistance) continue;
            // Trace just inside the support so an exact face boundary cannot return MISS.
            Vec3 inside = point.add(-face.getStepX() * 0.001, -face.getStepY() * 0.001, -face.getStepZ() * 0.001);
            BlockHitResult hit = world.raycast(eyes, inside);
            if (hit == null || hit.getType() != HitResult.Type.BLOCK
               || !hit.getBlockPos().equals(support) || hit.getDirection() != face) continue;
            nearest = new Placement(target.immutable(), hit);
            nearestDistance = distance;
         }
         if (nearest != null) return nearest;
      }
      return null;
   }
}
