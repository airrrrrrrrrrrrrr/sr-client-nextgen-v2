package me.alpha432.oyvey.features.modules.movement.scaffold;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import me.alpha432.oyvey.util.SilentRotationMath;
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
      default boolean rotationValid(Placement placement) { return true; }
      default boolean supportsLanding(BlockPos pos) { return attachable(pos, Direction.UP); }
   }

   public record Placement(BlockPos target, BlockHitResult hit) { }

   public static float backwardPitch(List<BlockPos> targets, Vec3 eyes, double reach,
      float yaw, float sentPitch, double sensitivity, World world) {
      // Keep the usual steep look whenever possible. At grid corners the
      // support may require a shallower pitch; yaw always remains backward.
      for (int offset = 0; offset <= 148; offset++) {
         for (int sign : new int[]{1, -1}) {
            float pitch = 82 + offset * sign * 0.25F;
            if (pitch < 45 || pitch > 89.5F) continue;
            pitch = SilentRotationMath.quantizeRotation(yaw, sentPitch, yaw, pitch, sensitivity).pitch();
            if (findAlongLook(targets, eyes, reach, yaw, pitch, world) != null) return pitch;
         }
      }
      return 82;
   }

   /** Select the block actually under a held look, without generating new aim. */
   public static Placement findAlongLook(List<BlockPos> targets, Vec3 eyes, double reach,
      float yaw, float pitch, World world) {
      BlockHitResult hit = world.raycast(eyes, eyes.add(Vec3.directionFromRotation(pitch, yaw).scale(reach)));
      if (hit == null || hit.getType() != HitResult.Type.BLOCK || hit.isInside()) return null;
      BlockPos target = hit.getBlockPos().relative(hit.getDirection());
      if (!targets.contains(target) || !world.replaceable(target)
         || !world.attachable(hit.getBlockPos(), hit.getDirection())) return null;
      Placement placement = new Placement(target, hit);
      return world.rotationValid(placement) ? placement : null;
   }

   public static boolean matchesSupport(Placement placement, BlockHitResult hit) {
      return placement != null && hit != null && hit.getType() == HitResult.Type.BLOCK
         && !hit.isInside() && hit.getBlockPos().equals(placement.hit().getBlockPos())
         && hit.getDirection() == placement.hit().getDirection()
         && hit.getBlockPos().relative(hit.getDirection()).equals(placement.target());
   }

   public static boolean lookMatchesSupport(Placement placement, Vec3 eyes, double reach,
      float yaw, float pitch, World world) {
      BlockHitResult hit = world.raycast(eyes, eyes.add(Vec3.directionFromRotation(pitch, yaw).scale(reach)));
      // The preceding camera look can reach a different face of this block.
      // The actual click still goes through matchesSupport's exact-face check.
      return placement != null && hit != null && hit.getType() == HitResult.Type.BLOCK && !hit.isInside()
         && hit.getBlockPos().equals(placement.hit().getBlockPos());
   }

   public static BlockHitResult findSupportAim(List<BlockPos> supports, Vec3 eyes, double reach, World world) {
      BlockHitResult nearest = null;
      for (BlockPos support : supports) {
         if (!world.attachable(support, Direction.UP)) continue;
         Vec3 point = Vec3.atCenterOf(support).add(0, 0.499, 0);
         if (eyes.distanceToSqr(point) > reach * reach) continue;
         BlockHitResult hit = world.raycast(eyes, point);
         if (hit != null && hit.getType() == HitResult.Type.BLOCK && !hit.isInside()
            && hit.getBlockPos().equals(support)
            && (nearest == null || eyes.distanceToSqr(hit.getLocation()) < eyes.distanceToSqr(nearest.getLocation()))) {
            nearest = hit;
         }
      }
      return nearest;
   }

   public static int blockLayer(double feetY) {
      return Mth.floor(feetY) - 1;
   }

   public static int bridgeLayer(double feetY, int lockedLayer, boolean keepY) {
      return keepY ? lockedLayer : blockLayer(feetY);
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

   /**
    * Keeps a landing surface beneath an airborne player instead of moving it up
    * through the jump arc. Projection selects candidates only; native physics,
    * reach and visible support checks still decide whether anything can place.
    */
   public static List<BlockPos> catchTargets(Vec3 feet, Vec3 motion, int bridgeLayer,
      boolean airborne, boolean allowLowerLayers, double gravity) {
      LinkedHashSet<BlockPos> targets = new LinkedHashSet<>();
      int layer = Math.min(bridgeLayer, blockLayer(feet.y));
      int ticks = airborne ? landingTicks(feet.y, motion.y, layer + 1.0, gravity) : 2;
      double speed = Math.hypot(motion.x, motion.z);
      double maxTicks = speed > 1.0E-6 ? Math.min(ticks, 3.0 / speed) : ticks;
      int layers = airborne && allowLowerLayers ? 3 : 1;
      for (int depth = 0; depth < layers; depth++) {
         int candidateLayer = layer - depth;
         // Save the current footprint first, then the landing footprint and the
         // path between them. Intermediate blocks can extend confirmed support
         // toward a landing that cannot yet be reached in a single placement.
         addFootprint(targets, feet.x, feet.z, candidateLayer);
         addFootprint(targets, feet.x + motion.x * maxTicks, feet.z + motion.z * maxTicks, candidateLayer);
         for (int tick = 1; tick <= ticks; tick++) {
            double time = Math.min(tick, maxTicks);
            addFootprint(targets, feet.x + motion.x * time, feet.z + motion.z * time, candidateLayer);
         }
      }
      // Both grounded and airborne diagonal paths can leave the next footprint
      // disconnected from confirmed support. Try connectors after direct targets;
      // this also lets a player waiting at an edge resume after the support ack.
      var direct = new LinkedHashSet<>(targets);
      for (BlockPos target : direct) {
         targets.add(target.north());
         targets.add(target.south());
         targets.add(target.west());
         targets.add(target.east());
      }
      if (airborne) {
         // Extend toward the landing before filling incidental corners behind
         // the player. Those side branches used to consume the tick needed for
         // a diagonal connector while the nearer prediction awaited its ack.
         double landingX = feet.x + motion.x * maxTicks;
         double landingZ = feet.z + motion.z * maxTicks;
         ArrayList<BlockPos> ordered = new ArrayList<>(targets);
         ordered.sort(Comparator.comparingInt((BlockPos pos) -> direct.contains(pos) ? 0 : 1)
            .thenComparingInt(pos -> layer - pos.getY())
            .thenComparingDouble(pos -> Mth.square(pos.getX() + 0.5 - landingX)
               + Mth.square(pos.getZ() + 0.5 - landingZ)));
         return ordered;
      }
      return new ArrayList<>(targets);
   }

   private static int landingTicks(double y, double velocityY, double top, double gravity) {
      double acceleration = Double.isFinite(gravity) && gravity > 0.0 ? gravity : 0.08;
      for (int tick = 1; tick <= 12; tick++) {
         y += velocityY;
         if (velocityY <= 0.0 && y <= top) return tick;
         velocityY = (velocityY - acceleration) * 0.98;
      }
      return 12;
   }

   private static void addFootprint(LinkedHashSet<BlockPos> targets, double x, double z, int layer) {
      targets.add(BlockPos.containing(x, layer, z));
      for (double offsetX : new double[]{-0.28, 0.28}) {
         for (double offsetZ : new double[]{-0.28, 0.28}) {
            targets.add(BlockPos.containing(x + offsetX, layer, z + offsetZ));
         }
      }
   }

   public static Placement findStepOrCatch(Vec3 feet, Vec3 motion, int bridgeLayer,
      boolean airborne, boolean keepY, double gravity, Vec3 eyes, double reach, World world) {
      if (airborne && !keepY && feet.y >= bridgeLayer + 2.001) {
         Vec3 landing = landingPosition(feet, motion, bridgeLayer + 2.0, gravity);
         // Placing beneath the current position creates a hump the player can
         // overshoot. Only raise a block that intersects the descending landing.
         if (landing != null && coveredLanding(landing, bridgeLayer + 1, world)) return null;
         // Leave recovery time for a rejected prediction instead of spending
         // the last falling tick on a raised block.
         Placement step = landing == null || landingTicks(feet.y, motion.y, bridgeLayer + 2.0, gravity) < 3 ? null
            : find(landingTargets(landing, bridgeLayer + 1), eyes, reach, world);
         if (step != null) return step;
      }
      if (airborne) {
         int layer = Math.min(bridgeLayer, blockLayer(feet.y));
         Vec3 landing = landingPosition(feet, motion, layer + 1.0, gravity);
         if (landing != null && coveredLanding(landing, layer, world)) return null;
      }
      // No real/reachable/aimable step: preserve the successful flat catch path.
      List<BlockPos> candidates = catchTargets(feet, motion, bridgeLayer, airborne, !keepY, gravity);
      if (airborne) {
         int layer = Math.min(bridgeLayer, blockLayer(feet.y));
         Vec3 landing = landingPosition(feet, motion, layer + 1.0, gravity);
         if (landing != null) {
            var frontier = new HashMap<Integer, Double>();
            for (BlockPos pos : candidates) {
               if (world.supportsLanding(pos)) {
                  frontier.merge(pos.getY(), landingDistance(pos, landing), Math::min);
               }
            }
            candidates.removeIf(pos -> landingDistance(pos, landing)
               >= frontier.getOrDefault(pos.getY(), Double.POSITIVE_INFINITY) - 1.0E-6);
         }
      }
      return find(candidates, eyes, reach, world);
   }

   public static Placement findAimOrPlace(Vec3 feet, Vec3 motion, int bridgeLayer,
      boolean airborne, boolean keepY, double gravity, Vec3 eyes, double reach, World world,
      float sentYaw, float sentPitch) {
      // Prefer a catch we can already click over spending its recovery tick
      // aiming at a new raised step. Otherwise prepare the next visible support.
      World readyWorld = new World() {
         public boolean replaceable(BlockPos pos) { return world.replaceable(pos); }
         public boolean attachable(BlockPos pos, Direction face) { return world.attachable(pos, face); }
         public BlockHitResult raycast(Vec3 from, Vec3 to) { return world.raycast(from, to); }
         public boolean supportsLanding(BlockPos pos) { return world.supportsLanding(pos); }
         public boolean rotationValid(Placement placement) {
            return world.rotationValid(placement)
               && lookMatchesSupport(placement, eyes, reach, sentYaw, sentPitch, world);
         }
      };
      Placement ready = findStepOrCatch(feet, motion, bridgeLayer, airborne, keepY, gravity, eyes, reach, readyWorld);
      return ready != null ? ready
         : findStepOrCatch(feet, motion, bridgeLayer, airborne, keepY, gravity, eyes, reach, world);
   }

   private static Vec3 landingPosition(Vec3 feet, Vec3 motion, double top, double gravity) {
      if (feet.y < top) return null;
      double acceleration = Double.isFinite(gravity) && gravity > 0.0 ? gravity : 0.08;
      double y = feet.y, velocityY = motion.y;
      for (int tick = 1; tick <= 12; tick++) {
         y += velocityY;
         if (velocityY <= 0.0 && y <= top) {
            return feet.add(motion.x * tick, 0, motion.z * tick);
         }
         velocityY = (velocityY - acceleration) * 0.98;
      }
      return null;
   }

   private static List<BlockPos> landingTargets(Vec3 landing, int layer) {
      LinkedHashSet<BlockPos> targets = new LinkedHashSet<>();
      targets.add(BlockPos.containing(landing.x, layer, landing.z));
      // Leave a margin inside the 0.6-wide player footprint rather than aiming
      // at a barely touching corner that a small change in motion can miss.
      for (double x : new double[]{-0.20, 0.20}) {
         for (double z : new double[]{-0.20, 0.20}) {
            targets.add(BlockPos.containing(landing.x + x, layer, landing.z + z));
         }
      }
      return new ArrayList<>(targets);
   }

   private static boolean coveredLanding(Vec3 landing, int layer, World world) {
      return landingTargets(landing, layer).stream().anyMatch(world::supportsLanding);
   }

   private static double landingDistance(BlockPos pos, Vec3 landing) {
      return Mth.square(pos.getX() + 0.5 - landing.x) + Mth.square(pos.getZ() + 0.5 - landing.z);
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
            Vec3 center = Vec3.atCenterOf(support).add(face.getStepX() * 0.5,
               face.getStepY() * 0.5, face.getStepZ() * 0.5);
            // A face-center-only aim falls below the support during descending
            // movement before the next tick can use it. Upper and lateral
            // samples keep a real, visible click available across that motion.
            int samples = face.getAxis() == Direction.Axis.Y ? 1 : 6;
            for (int sample = 0; sample < samples; sample++) {
            double across = sample / 2 == 0 ? 0 : sample / 2 == 1 ? -0.3 : 0.3;
            Vec3 point = center.add(face.getAxis() == Direction.Axis.Z ? across : 0,
               sample % 2 * 0.47, face.getAxis() == Direction.Axis.X ? across : 0);
            double distance = eyes.distanceToSqr(point);
            if (distance > reach * reach || distance >= nearestDistance) continue;
            // Trace just inside the support so an exact face boundary cannot return MISS.
            Vec3 inside = point.add(-face.getStepX() * 0.001, -face.getStepY() * 0.001, -face.getStepZ() * 0.001);
            BlockHitResult hit = world.raycast(eyes, inside);
            if (hit == null || hit.getType() != HitResult.Type.BLOCK
               || hit.isInside() || !hit.getBlockPos().equals(support) || hit.getDirection() != face) continue;
            Placement placement = new Placement(target.immutable(), hit);
            if (!world.rotationValid(placement)) continue;
            nearest = placement;
            nearestDistance = distance;
            }
         }
         if (nearest != null) return nearest;
      }
      return null;
   }
}
