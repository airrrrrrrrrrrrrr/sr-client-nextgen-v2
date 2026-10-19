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
import net.minecraft.world.phys.AABB;

/** Pure placement selection, with world queries supplied by the caller. */
public final class PlacementPlanner {
   private static final Direction[] SUPPORTS = {
      Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP
   };

   private PlacementPlanner() { }

   public interface World {
      boolean replaceable(BlockPos target);
      default boolean replaceableAt(BlockPos target, AABB playerBox) { return replaceable(target); }
      boolean attachable(BlockPos support, Direction face);
      BlockHitResult raycast(Vec3 from, Vec3 to);
      default boolean rotationValid(Placement placement) { return true; }
      default boolean supportsLanding(BlockPos pos) { return attachable(pos, Direction.UP); }
   }

   /** Plan a future click against its future player collision box. */
   public static World withPlayerBox(World world, AABB playerBox) {
      return new World() {
         public boolean replaceable(BlockPos pos) { return world.replaceableAt(pos, playerBox); }
         public boolean attachable(BlockPos pos, Direction face) { return world.attachable(pos, face); }
         public BlockHitResult raycast(Vec3 from, Vec3 to) { return world.raycast(from, to); }
         public boolean rotationValid(Placement placement) { return world.rotationValid(placement); }
         public boolean supportsLanding(BlockPos pos) { return world.supportsLanding(pos); }
      };
   }

   public record Placement(BlockPos target, BlockHitResult hit) { }
   public record Aim(Placement placement, SilentRotationMath.Rotation rotation) { }
   public record ClutchStep(Placement placement, SilentRotationMath.Rotation rotation, boolean ready) { }

   /** Consume a previously transmitted catch before preparing a different look. */
   public static ClutchStep clutchStep(List<BlockPos> targets, Vec3 eyes, Vec3 nextEyes, double reach,
      float sentYaw, float sentPitch, double sensitivity, boolean hasSentRotation, World world, Placement preferred) {
      // Only consume the face that was prepared for recovery. An arbitrary
      // ray after a successful placement often hits that new block's TOP and
      // would build a tower behind the player instead of extending the catch.
      if (hasSentRotation && preferred != null && targets.contains(preferred.target())) {
         Placement ready = findAlongLook(List.of(preferred.target()), eyes, reach, sentYaw, sentPitch, world);
         if (ready != null) return new ClutchStep(ready, new SilentRotationMath.Rotation(sentYaw, sentPitch), true);
      }
      World future = new World() {
         public boolean replaceable(BlockPos pos) { return world.replaceable(pos); }
         public boolean attachable(BlockPos pos, Direction face) { return world.attachable(pos, face); }
         public BlockHitResult raycast(Vec3 from, Vec3 to) { return world.raycast(from, to); }
         public boolean rotationValid(Placement placement) {
            return world.rotationValid(placement)
               && futureClutchLook(placement, nextEyes, reach, sentYaw, sentPitch, sensitivity, world) != null;
         }
      };
      // Select in landing-path order before imposing a current-position ray.
      // No click occurs while preparing a look for the next movement packet.
      var nearestPath = new ArrayList<>(targets);
      nearestPath.sort(Comparator.comparingDouble((BlockPos pos) -> Mth.square(pos.getX() + .5 - nextEyes.x)
         + Mth.square(pos.getZ() + .5 - nextEyes.z)).thenComparingInt(pos -> -pos.getY()));
      Placement placement = find(nearestPath, nextEyes, reach, future);
      Aim aim = placement == null ? null : futureClutchLook(placement, nextEyes, reach, sentYaw, sentPitch, sensitivity, world);
      if (aim == null) aim = clutchAim(targets, eyes, nextEyes, reach, sentYaw, sentPitch, sensitivity, world, preferred);
      if (aim == null) return null;
      if (hasSentRotation) {
         Placement ready = findAlongLook(List.of(aim.placement().target()), eyes, reach, sentYaw, sentPitch, world);
         if (ready != null) return new ClutchStep(ready, new SilentRotationMath.Rotation(sentYaw, sentPitch), true);
      }
      return new ClutchStep(aim.placement(), aim.rotation(), false);
   }
   private static Aim futureClutchLook(Placement placement, Vec3 eyes, double reach, float sentYaw,
      float sentPitch, double sensitivity, World world) {
      Aim aim = clutchLook(placement, eyes, eyes, reach, sentYaw, sentPitch, sensitivity, world);
      if (aim == null) return null;
      Vec3 ray = Vec3.directionFromRotation(aim.rotation().pitch(), aim.rotation().yaw()).scale(reach);
      // Leave room for the next native air acceleration and minor vertical
      // differences; a ray grazing the upper lip is not a dependable catch.
      for (Vec3 offset : List.of(new Vec3(.04, 0, 0), new Vec3(-.04, 0, 0),
         new Vec3(0, 0, .04), new Vec3(0, 0, -.04), new Vec3(0, .02, 0), new Vec3(0, -.02, 0))) {
         Vec3 from = eyes.add(offset);
         if (!matchesSupport(placement, world.raycast(from, from.add(ray)))) return null;
      }
      return aim;
   }

   /** Emergency look toward any reachable support, stable across the next physics step. */
   public static Aim clutchAim(List<BlockPos> targets, Vec3 eyes, Vec3 nextEyes, double reach,
      float sentYaw, float sentPitch, double sensitivity, World world, Placement preferred) {
      if (preferred != null && targets.contains(preferred.target()) && world.replaceable(preferred.target())
         && world.attachable(preferred.hit().getBlockPos(), preferred.hit().getDirection())) {
         Aim held = clutchLook(preferred, eyes, nextEyes, reach, sentYaw, sentPitch, sensitivity, world);
         if (held != null) return held;
      }
      World stable = new World() {
         public boolean replaceable(BlockPos pos) { return world.replaceable(pos); }
         public boolean attachable(BlockPos pos, Direction face) { return world.attachable(pos, face); }
         public BlockHitResult raycast(Vec3 from, Vec3 to) { return world.raycast(from, to); }
         public boolean rotationValid(Placement placement) {
            return world.rotationValid(placement)
               && clutchLook(placement, eyes, nextEyes, reach, sentYaw, sentPitch, sensitivity, world) != null;
         }
      };
      Placement placement = find(targets, nextEyes, reach, stable);
      return placement == null ? null : clutchLook(placement, eyes, nextEyes, reach, sentYaw, sentPitch, sensitivity, world);
   }

   private static Aim clutchLook(Placement placement, Vec3 eyes, Vec3 nextEyes, double reach,
      float sentYaw, float sentPitch, double sensitivity, World world) {
      Vec3 delta = placement.hit().getLocation().subtract(nextEyes);
      float yaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
      float pitch = (float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
      var look = SilentRotationMath.quantizeRotation(sentYaw, sentPitch, yaw, pitch, sensitivity);
      Vec3 direction = Vec3.directionFromRotation(look.pitch(), look.yaw()).scale(reach);
      BlockHitResult current = world.raycast(eyes, eyes.add(direction));
      BlockHitResult next = world.raycast(nextEyes, nextEyes.add(direction));
      if (!matchesSupport(placement, current) || !matchesSupport(placement, next)) return null;
      return new Aim(new Placement(placement.target(), next), look);
   }

   /** Aim at the center of the support's placement face, then verify the real ray. */
   public static Aim centerAim(List<BlockPos> targets, Vec3 eyes, double reach,
      float sentYaw, float sentPitch, double sensitivity, World world) {
      return centerAim(targets, eyes, reach, sentYaw, sentPitch, sensitivity, world, null, false);
   }

   public static Aim centerAim(List<BlockPos> targets, Vec3 eyes, double reach,
      float sentYaw, float sentPitch, double sensitivity, World world, Placement preferred, boolean holdYaw) {
      // Retain the selected face while it is still a valid placement. Reordering
      // nearby supports every tick made Center alternate between adjacent blocks.
      if (preferred != null && targets.contains(preferred.target()) && world.replaceable(preferred.target())) {
         Aim held = centerFaceAim(preferred.target(), preferred.hit().getBlockPos(), preferred.hit().getDirection(),
            eyes, reach, sentYaw, sentPitch, sensitivity, world, holdYaw);
         if (held != null) return held;
      }
      // After movement, only refine the face selected before physics. Looking
      // for a different target along the held yaw creates incidental side blocks.
      if (holdYaw) return null;
      for (BlockPos target : targets) {
         if (!world.replaceable(target)) continue;
         Aim best = null;
         double nearest = Double.POSITIVE_INFINITY;
         for (Direction offset : SUPPORTS) {
            BlockPos support = target.relative(offset);
            Direction face = offset.getOpposite();
            Aim aim = centerFaceAim(target, support, face, eyes, reach, sentYaw, sentPitch, sensitivity, world, holdYaw);
            if (aim == null) continue;
            double distance = eyes.distanceToSqr(aim.placement().hit().getLocation());
            if (distance >= nearest) continue;
            best = aim;
            nearest = distance;
         }
         if (best != null) return best;
      }
      return null;
   }

   /** One face must remain aimable at the click and after the following move. */
   public static Aim centerPulseAim(List<BlockPos> targets, Vec3 clickEyes, Vec3 postEyes, double reach,
      float sentYaw, float sentPitch, double sensitivity, World world, Placement preferred) {
      var ordered = new ArrayList<>(targets);
      ordered.sort(Comparator.comparingDouble((BlockPos pos)->Mth.square(pos.getX()+.5-postEyes.x)
         +Mth.square(pos.getZ()+.5-postEyes.z)).thenComparingInt(pos->-pos.getY()));
      if (preferred != null && ordered.remove(preferred.target())) ordered.addFirst(preferred.target());
      for (BlockPos target : ordered) {
         if (!world.replaceable(target)) continue;
         for (Direction offset : SUPPORTS) {
            BlockPos support = target.relative(offset);
            Direction face = offset.getOpposite();
            if (!world.attachable(support,face)) continue;
            Vec3 center=Vec3.atCenterOf(support).add(face.getStepX()*.5,face.getStepY()*.5,face.getStepZ()*.5);
            for (double fraction : new double[]{.5,1,0,.25,.75}) {
               Vec3 delta=center.subtract(clickEyes.lerp(postEyes,fraction));
               float yaw=(float)Math.toDegrees(Math.atan2(delta.z,delta.x))-90;
               yaw=SilentRotationMath.quantizeRotation(sentYaw,sentPitch,yaw,sentPitch,sensitivity).yaw();
               Aim aim=centerFaceAim(target,support,face,clickEyes,reach,yaw,sentPitch,sensitivity,world,true);
               if (aim==null) continue;
               Vec3 ray=Vec3.directionFromRotation(aim.rotation().pitch(),yaw).scale(reach);
               boolean stable=true;
               for (Vec3 margin : List.of(new Vec3(.04,0,0),new Vec3(-.04,0,0),new Vec3(0,0,.04),new Vec3(0,0,-.04))) {
                  Vec3 from=clickEyes.add(margin);
                  if (!matchesSupport(aim.placement(),world.raycast(from,from.add(ray)))) { stable=false; break; }
               }
               if (!stable) continue;
               float postPitch=postPlacementPitch(aim.placement(),postEyes,reach,yaw,aim.rotation().pitch(),sensitivity);
               if (lookIntersectsClickedSupport(aim.placement(),postEyes,reach,yaw,postPitch)) return aim;
            }
         }
      }
      return null;
   }

   private static Aim centerFaceAim(BlockPos target, BlockPos support, Direction face, Vec3 eyes, double reach,
      float sentYaw, float sentPitch, double sensitivity, World world, boolean holdYaw) {
      if (!world.attachable(support, face)) return null;
      Vec3 center = Vec3.atCenterOf(support).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
      Vec3 point = center;
      if (holdYaw) {
         // Physics has already used this yaw. Intersect its horizontal ray with
         // the chosen face, then aim at the face's center height. Borrowing the
         // pitch of a different yaw/target caused missed diagonal placements.
         Vec3 direction = Vec3.directionFromRotation(0, sentYaw);
         double distance;
         if (face.getAxis() == Direction.Axis.X) {
            if (Math.abs(direction.x) < 1.0E-7) return null;
            distance = (center.x - eyes.x) / direction.x;
         } else if (face.getAxis() == Direction.Axis.Z) {
            if (Math.abs(direction.z) < 1.0E-7) return null;
            distance = (center.z - eyes.z) / direction.z;
         } else {
            distance = (center.x - eyes.x) * direction.x + (center.z - eyes.z) * direction.z;
         }
         if (distance <= 0) return null;
         point = new Vec3(eyes.x + direction.x * distance, center.y, eyes.z + direction.z * distance);
      }
      if (eyes.distanceToSqr(point) > reach * reach) return null;
      Vec3 delta = point.subtract(eyes);
      float yaw = holdYaw ? sentYaw : (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
      float pitch = (float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
      var rotation = SilentRotationMath.quantizeRotation(sentYaw, sentPitch, yaw, pitch, sensitivity);
      BlockHitResult hit = world.raycast(eyes, eyes.add(Vec3.directionFromRotation(rotation.pitch(), rotation.yaw()).scale(reach)));
      Placement placement = new Placement(target.immutable(), new BlockHitResult(center, face, support, false));
      if (!matchesSupport(placement, hit)) return null;
      placement = new Placement(target.immutable(), hit);
      return world.rotationValid(placement) ? new Aim(placement, rotation) : null;
   }

   /** A Center click belongs to its chosen target and face, never a neighboring face. */
   public static Placement findAlongCenterLook(Placement selected, List<BlockPos> targets, Vec3 eyes, double reach,
      float yaw, float pitch, World world) {
      if (selected == null || !targets.contains(selected.target()) || !world.replaceable(selected.target())
         || !world.attachable(selected.hit().getBlockPos(), selected.hit().getDirection())) return null;
      BlockHitResult hit = world.raycast(eyes, eyes.add(Vec3.directionFromRotation(pitch, yaw).scale(reach)));
      if (!matchesSupport(selected, hit)) return null;
      Placement placement = new Placement(selected.target(), hit);
      return world.rotationValid(placement) ? placement : null;
   }

   public static float backwardPitch(List<BlockPos> targets, Vec3 eyes, double reach,
      float yaw, float sentPitch, double sensitivity, World world) {
      return backwardPitch(targets, eyes, reach, yaw, sentPitch, sensitivity, world, false);
   }

   public static float backwardPitch(List<BlockPos> targets, Vec3 eyes, double reach,
      float yaw, float sentPitch, double sensitivity, World world, boolean cornerCatch) {
      // Keep the usual steep look whenever possible. At grid corners the
      // support may require a shallower pitch; yaw always remains backward.
      int highestLayer = targets.stream().mapToInt(BlockPos::getY).max().orElse(Integer.MIN_VALUE);
      int bestLayer = Integer.MIN_VALUE;
      float bestPitch = 82;
      boolean bestContinuation = false;
      for (int offset = 0; offset <= 148; offset++) {
         for (int sign : new int[]{1, -1}) {
            float pitch = 82 + offset * sign * 0.25F;
            if (pitch < 45 || pitch > 89.95F) continue;
            pitch = SilentRotationMath.quantizeRotation(yaw, sentPitch, yaw, pitch, sensitivity).pitch();
            Placement placement = findAlongLook(targets, eyes, reach, yaw, pitch, world);
            boolean continuation = placement != null && cornerCatch && cornerContinuation(placement, targets, eyes, reach, yaw, pitch, world);
            if (placement != null && (placement.target().getY() > bestLayer
               || placement.target().getY() == bestLayer && continuation && !bestContinuation)) {
               bestLayer = placement.target().getY();
               bestPitch = pitch;
               bestContinuation = continuation;
               if (bestLayer == highestLayer && (!cornerCatch || continuation)) return pitch;
            }
         }
      }
      return bestPitch;
   }

   private static boolean cornerContinuation(Placement first, List<BlockPos> targets, Vec3 eyes, double reach,
      float yaw, float pitch, World world) {
      World predicted = new World() {
         public boolean replaceable(BlockPos pos) { return !pos.equals(first.target()) && world.replaceable(pos); }
         public boolean attachable(BlockPos pos, Direction face) { return pos.equals(first.target()) || world.attachable(pos, face); }
         public boolean rotationValid(Placement placement) { return world.rotationValid(placement); }
         public BlockHitResult raycast(Vec3 from, Vec3 to) {
            BlockHitResult existing = world.raycast(from, to);
            BlockHitResult added = net.minecraft.world.phys.shapes.Shapes.block().clip(from, to, first.target());
            return added != null && (existing == null || existing.getType() != HitResult.Type.BLOCK
               || from.distanceToSqr(added.getLocation()) < from.distanceToSqr(existing.getLocation())) ? added : existing;
         }
      };
      Placement next = findAlongLook(targets, eyes, reach, yaw, pitch, predicted);
      if (next == null || next.target().getY() != first.target().getY()) return false;
      Vec3 progress = Vec3.atCenterOf(next.target()).subtract(Vec3.atCenterOf(first.target()));
      return progress.dot(Vec3.directionFromRotation(0, yaw + 180)) > 0.1;
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

   /** Validate an already clicked support without letting its new block occlude it. */
   public static boolean lookIntersectsClickedSupport(Placement placement, Vec3 eyes, double reach, float yaw, float pitch) {
      if (placement == null) return false;
      AABB support = new AABB(placement.hit().getBlockPos());
      return support.contains(eyes) || support.clip(eyes,
         eyes.add(Vec3.directionFromRotation(pitch, yaw).scale(reach))).isPresent();
   }

   /** Preserve a valid previous-yaw/current-pitch ray for the queued placement. */
   public static float postPlacementPitch(Placement clicked, Vec3 postEyes, double reach,
      float previousYaw, float previousPitch, double sensitivity) {
      if (clicked == null) return previousPitch;
      Vec3 center = Vec3.atCenterOf(clicked.hit().getBlockPos());
      Vec3 direction = Vec3.directionFromRotation(0, previousYaw);
      double along = (center.x-postEyes.x)*direction.x + (center.z-postEyes.z)*direction.z;
      if (along <= .001) return previousPitch;
      for (double offset : new double[]{0,.25,-.25,.45,-.45}) {
         float pitch = (float)-Math.toDegrees(Math.atan2(center.y+offset-postEyes.y, along));
         pitch = SilentRotationMath.quantizeRotation(previousYaw, previousPitch, previousYaw, pitch, sensitivity).pitch();
         if (lookIntersectsClickedSupport(clicked, postEyes, reach, previousYaw, pitch)) return pitch;
      }
      return previousPitch;
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

   /** Center should extend the travel path, rather than fill corners behind it. */
   public static List<BlockPos> centerTargets(Vec3 feet, Vec3 motion, int bridgeLayer,
      boolean airborne, boolean allowLowerLayers, double gravity, World world) {
      List<BlockPos> targets = catchTargets(feet, motion, bridgeLayer, airborne, allowLowerLayers, gravity);
      if (motion.horizontalDistanceSqr() < 1.0E-6) return targets;
      var landings = new HashMap<Integer, Vec3>();
      var frontier = new HashMap<Integer, Double>();
      for (BlockPos pos : targets) {
         Vec3 landing = landings.computeIfAbsent(pos.getY(), layer -> airborne
            ? landingPosition(feet, motion, layer + 1.0, gravity) : feet.add(motion.scale(2)));
         if (landing != null && world.attachable(pos, Direction.UP)) {
            frontier.merge(pos.getY(), landingDistance(pos, landing), Math::min);
         }
      }
      targets.removeIf(pos -> landings.get(pos.getY()) != null && landingDistance(pos, landings.get(pos.getY()))
         >= frontier.getOrDefault(pos.getY(), Double.POSITIVE_INFINITY) - 1.0E-6);
      return targets;
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

   public static boolean hasLanding(Vec3 feet, Vec3 motion, int layer, double gravity, World world) {
      Vec3 landing = landingPosition(feet, motion, layer + 1.0, gravity);
      return landing != null && coveredLanding(landing, layer, world);
   }

   /** Forecast the forward phase, including its next native input acceleration. */
   public static boolean hasForwardLanding(Vec3 feet, Vec3 motion, int layer, double gravity,
      float travelYaw, boolean moving, World world) {
      double y = feet.y, velocityY = motion.y;
      double x = feet.x, z = feet.z, vx = motion.x, vz = motion.z;
      Vec3 acceleration = moving ? Vec3.directionFromRotation(0, travelYaw).scale(0.026) : Vec3.ZERO;
      for (int tick = 1; tick <= 12; tick++) {
         vx += acceleration.x;
         vz += acceleration.z;
         x += vx;
         z += vz;
         y += velocityY;
         if (velocityY <= 0 && y <= layer + 1.0) {
            return coveredLanding(new Vec3(x, y, z), layer, world);
         }
         velocityY = (velocityY - gravity) * 0.98;
         vx *= 0.91;
         vz *= 0.91;
      }
      return false;
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
            int samples = face.getAxis() == Direction.Axis.Y ? 1 : 15;
            for (int sample = 0; sample < samples; sample++) {
            double across = sample / 5 == 0 ? 0 : sample / 5 == 1 ? -0.3 : 0.3;
            double height = face.getAxis() == Direction.Axis.Y ? 0 : switch (sample % 5) {
               case 1 -> 0.47;
               case 2 -> -0.25;
               case 3 -> 0.25;
               case 4 -> -0.47;
               default -> 0;
            };
            Vec3 point = center.add(face.getAxis() == Direction.Axis.Z ? across : 0,
               height, face.getAxis() == Direction.Axis.X ? across : 0);
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
