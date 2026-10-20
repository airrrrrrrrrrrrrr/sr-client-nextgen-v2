package me.alpha432.oyvey.features.modules.movement.scaffold;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import me.alpha432.oyvey.util.SilentRotationMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Extends a floor and a low ceiling for native sprint/head-hitter jumps. */
public final class AndromidaBridge {
   public enum Phase { FLOOR, ROOF, STARTUP, TRAVEL }
   public record Step(Phase phase, PlacementPlanner.Placement placement,
      SilentRotationMath.Rotation look, boolean ready) { }
   private Integer layer;
   private BlockPos seed;
   private PlacementPlanner.Placement prepared;
   private int lastJump = Integer.MIN_VALUE;
   private boolean ceilingStarted;

   public void reset() { layer = null; seed = null; prepared = null; lastJump = Integer.MIN_VALUE; ceilingStarted = false; }
   public int layer(int footLayer) { if (layer == null) layer = footLayer; return layer; }

   public Step plan(Vec3 feet, Vec3 eyes, Vec3 motion, float heading, double reach,
      float sentYaw, float sentPitch, double sensitivity, boolean hasSent, PlacementPlanner.World world) {
      int floorY = this.layer(PlacementPlanner.blockLayer(feet.y));
      Vec3 forward = Vec3.directionFromRotation(0, heading);
      this.ceilingStarted |= world.supportsLanding(BlockPos.containing(feet).atY(floorY + 3));
      if (seed == null) seed = BlockPos.containing(feet.subtract(forward.scale(1.1))).atY(floorY);
      BlockPos floor = BlockPos.containing(feet.add(forward.scale(0.9))).atY(floorY);
      var floorTargets = new LinkedHashSet<BlockPos>();
      floorTargets.add(floor);
      floorTargets.addAll(PlacementPlanner.catchTargets(feet, motion.add(forward.scale(0.05)), floorY,
         true, false, 0.08));
      if (!world.supportsLanding(floor)) {
         Step catchFloor = this.aim(Phase.FLOOR, new ArrayList<>(floorTargets), eyes, motion, reach,
            sentYaw, sentPitch, sensitivity, hasSent, world);
         if (catchFloor != null) return catchFloor;
      }
      // Three blocks above the floor leaves two blocks of clear player space.
      // Retain both current and upcoming roof cells, including diagonal corners.
      var roofTargets = new LinkedHashSet<BlockPos>();
      BlockPos roofHere = BlockPos.containing(feet).atY(floorY + 3);
      roofTargets.add(roofHere);
      // A diagonal startup pillar needs an actual face-connected elbow.
      // A corner touch alone never supplies a legal placement support.
      if (world.supportsLanding(seed.above(3)) && !world.supportsLanding(roofHere)) {
         BlockPos connector = seed.above(3);
         for (int count = 0; count < 4 && connector.getX() != roofHere.getX(); count++) {
            connector = connector.offset(Integer.signum(roofHere.getX() - connector.getX()), 0, 0);
            roofTargets.add(connector);
         }
         for (int count = 0; count < 4 && connector.getZ() != roofHere.getZ(); count++) {
            connector = connector.offset(0, 0, Integer.signum(roofHere.getZ() - connector.getZ()));
            roofTargets.add(connector);
         }
      }
      roofTargets.add(floor.above(3));
      for (BlockPos pos : floorTargets) if (world.supportsLanding(pos)) roofTargets.add(pos.above(3));
      Step roof = this.aim(Phase.ROOF, new ArrayList<>(roofTargets), eyes, motion, reach,
         sentYaw, sentPitch, sensitivity, hasSent, world);
      if (roof != null) return roof;

      // Bootstrap a ceiling from a real pillar behind the starting position.
      // Every piece still needs a visible, reachable support and player clearance.
      if (!world.supportsLanding(BlockPos.containing(feet).atY(floorY + 3))) {
         Step startup = this.aim(Phase.STARTUP, List.of(seed, seed.above(), seed.above(2), seed.above(3)),
            eyes, motion, reach, sentYaw, sentPitch, sensitivity, hasSent, world);
         if (startup != null) return startup;
      }
      this.prepared = null;
      return new Step(Phase.TRAVEL, null,
         TellyBridge.turn(sentYaw, sentPitch, heading, 15, sensitivity), false);
   }

   private Step aim(Phase phase, List<BlockPos> targets, Vec3 eyes, Vec3 motion, double reach,
      float yaw, float pitch, double sensitivity, boolean hasSent, PlacementPlanner.World world) {
      var step = PlacementPlanner.clutchStep(targets, eyes, eyes.add(motion), reach,
         yaw, pitch, sensitivity, hasSent, world, this.prepared);
      if (step == null) return null;
      this.prepared = step.placement();
      return new Step(phase, step.placement(), step.rotation(), step.ready());
   }

   public void placed() { this.prepared = null; }

   public boolean jump(int tick, boolean grounded, boolean moving, boolean aligned, boolean clearHeadroom,
      Vec3 feet, float heading, PlacementPlanner.World world) {
      if (!grounded || !moving || !aligned || (long)tick - lastJump <= 1 || layer == null) return false;
      Vec3 forward = Vec3.directionFromRotation(0, heading);
      BlockPos current = BlockPos.containing(feet).atY(layer);
      BlockPos next = BlockPos.containing(feet.add(forward.scale(0.9))).atY(layer);
      if (!world.supportsLanding(current) || !world.supportsLanding(next)) return false;
      boolean ceiling = world.supportsLanding(current.above(3))
         || world.supportsLanding(BlockPos.containing(feet.subtract(forward.scale(0.29))).atY(layer + 3));
      this.ceilingStarted |= ceiling;
      boolean startup = !this.ceilingStarted && clearHeadroom && seed != null && world.supportsLanding(seed.above(2)) && !ceiling;
      if (!ceiling && !startup) return false;
      this.lastJump = tick;
      return true;
   }
}
