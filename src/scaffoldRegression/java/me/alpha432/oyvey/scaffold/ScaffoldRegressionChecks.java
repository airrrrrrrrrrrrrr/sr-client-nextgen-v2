package me.alpha432.oyvey.scaffold;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import me.alpha432.oyvey.features.modules.movement.scaffold.PlacementConfirmations;
import me.alpha432.oyvey.features.modules.movement.scaffold.PlacementPlanner;
import me.alpha432.oyvey.features.modules.movement.scaffold.SilentHeadPose;
import me.alpha432.oyvey.util.SilentRotationMath;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.EventBus;
import me.alpha432.oyvey.event.system.Subscribe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class ScaffoldRegressionChecks {
   private static int checks;

   public static void main(String[] args) {
      placementGeometry();
      jumpingCatchPlanning();
      stepUpOrKeepLayer();
      landingDirectedStepUp();
      pendingLandingDoesNotScatterBlocks();
      forwardJumpLandings();
      predictionAcknowledgements();
      rotationContinuity();
      silentHeadRendering();
      cameraRestorationOrder();
      movementAndSprint();
      System.out.println("Scaffold regression checks passed: " + checks);
   }

   private static void placementGeometry() {
      FakeWorld world = new FakeWorld();
      BlockPos support = new BlockPos(0, 0, 0);
      BlockPos target = support.east();
      Vec3 eyes = new Vec3(1.6, 2.62, 0.5);
      world.solid.add(support);
      var placement = PlacementPlanner.find(List.of(target), eyes, 4.5, world);
      require(placement != null, "a visible, solid support can be used");
      require(placement.hit().getDirection() == Direction.EAST, "click the visible east face");
      require(PlacementPlanner.matchesSupport(placement, placement.hit()), "native ray matches planned target");
      require(!PlacementPlanner.matchesSupport(placement,
         BlockHitResult.miss(placement.hit().getLocation(), Direction.EAST, support)), "MISS cannot place");
      require(!PlacementPlanner.matchesSupport(placement,
         new BlockHitResult(placement.hit().getLocation(), Direction.UP, support, false)), "wrong face cannot place");
      require(!PlacementPlanner.matchesSupport(placement,
         new BlockHitResult(placement.hit().getLocation(), Direction.EAST, support, true)), "inside hits cannot place");
      require(PlacementPlanner.find(List.of(target), eyes, 0.5, world) == null, "respect native reach");
      world.solid.add(new BlockPos(1, 1, 0));
      require(PlacementPlanner.find(List.of(target), eyes, 4.5, world) == null, "occlusion blocks a fabricated click");
      world.solid.clear();
      require(PlacementPlanner.find(List.of(target), eyes, 4.5, world) == null, "air cannot be a support");
      require(PlacementPlanner.blockLayer(64.0) == 63, "feet layer at integer height");
      require(PlacementPlanner.blockLayer(-0.01) == -2, "feet layer at negative height");
   }

   private static void predictionAcknowledgements() {
      FakeWorld world = new FakeWorld();
      BlockPos support = new BlockPos(0, 0, 0);
      BlockPos target = support.east();
      Vec3 eyes = new Vec3(1.6, 2.62, 0.5);
      world.solid.add(support); // Visible client prediction is deliberately solid.
      world.confirmations.predict(support, 12);
      require(PlacementPlanner.find(List.of(target), eyes, 4.5, world) == null,
         "never chain from an unacknowledged client prediction");
      world.confirmations.acknowledge(11);
      require(world.confirmations.isPending(support), "older ack cannot release a future prediction");
      world.confirmations.predict(support, 14);
      world.confirmations.acknowledge(12);
      require(world.confirmations.isPending(support), "replacement prediction waits for its own sequence");
      world.confirmations.acknowledge(14);
      require(PlacementPlanner.find(List.of(target), eyes, 4.5, world) != null,
         "accepted native state becomes a support after ack");
      world.confirmations.predict(support, 15);
      world.solid.remove(support); // Model vanilla's rollback before our ack callback.
      world.confirmations.acknowledge(15);
      require(PlacementPlanner.find(List.of(target), eyes, 4.5, world) == null,
         "a rejected/rolled-back prediction stays unusable after ack");
      world.confirmations.predict(support, 20);
      world.confirmations.clear();
      require(!world.confirmations.isPending(support), "changing worlds clears stale sequences");
   }

   private static void jumpingCatchPlanning() {
      FakeWorld world = new FakeWorld();
      world.solid.add(new BlockPos(0, 0, 0));
      Vec3 feet = new Vec3(1.65, 2.2, 0.5);
      Vec3 motion = new Vec3(0.24, -0.08, 0);
      Vec3 eyes = feet.add(0, 1.62, 0);
      var oldTargets = PlacementPlanner.targets(feet.x, feet.z, PlacementPlanner.blockLayer(feet.y), motion.x, motion.z);
      require(PlacementPlanner.find(oldTargets, eyes, 4.5, world) == null,
         "reproduce the raised-layer gap in the previous jump planner");
      var targets = PlacementPlanner.catchTargets(feet, motion, 0, true, true, 0.08);
      var catchBlock = PlacementPlanner.find(targets, eyes, 4.5, world);
      require(catchBlock != null && catchBlock.target().equals(new BlockPos(1, 0, 0)),
         "a forward jump extends the bridge at its grounded layer");
      require(targets.contains(new BlockPos(3, 0, 0)), "search along the landing path beyond the old 0.4-block lookahead");
      require(targets.stream().noneMatch(pos -> pos.getY() > 0), "jumping cannot raise the bridge into an unsupported layer");
      require(PlacementPlanner.find(targets, eyes, 0.5, world) == null,
         "catch prediction cannot bypass interaction reach");

      // W with a diagonal camera heading needs a legal connector first.
      feet = new Vec3(1.65, 2.2, 1.65);
      motion = new Vec3(0.2, -0.08, 0.2);
      eyes = feet.add(0, 1.62, 0);
      targets = PlacementPlanner.catchTargets(feet, motion, 0, true, true, 0.08);
      var connector = PlacementPlanner.find(targets, eyes, 4.5, world);
      require(connector != null && connector.target().getY() == 0,
         "diagonal jumps can extend an adjacent confirmed support");
      require(!connector.target().equals(new BlockPos(1, 0, 1)),
         "a diagonal gap cannot fabricate a nonadjacent support");
      world.solid.add(connector.target());
      world.confirmations.predict(connector.target(), 42);
      var unconfirmed = PlacementPlanner.find(List.of(new BlockPos(1, 0, 1)), eyes, 4.5, world);
      require(unconfirmed == null, "catch planning still waits for connector acknowledgement");
      world.confirmations.acknowledge(42);
      var landing = PlacementPlanner.find(targets, eyes, 4.5, world);
      require(landing != null && landing.target().equals(new BlockPos(1, 0, 1)),
         "after acknowledgement the connector supports the diagonal landing");

      world.solid.clear();
      world.solid.add(new BlockPos(0, -1, 0));
      feet = new Vec3(1.65, 1.4, 0.5);
      targets = PlacementPlanner.catchTargets(feet, new Vec3(0.1, -0.4, 0), 0, true, true, 0.08);
      var lower = PlacementPlanner.find(targets, feet.add(0, 1.62, 0), 4.5, world);
      require(lower != null && lower.target().equals(new BlockPos(1, -1, 0)),
         "a descending player can catch a visible lower ledge before building connectors");
      var locked = PlacementPlanner.catchTargets(feet, motion, 0, true, false, 0.08);
      require(locked.stream().allMatch(pos -> pos.getY() == 0), "KeepY does not search lower layers");
      world.solid.clear();
      require(PlacementPlanner.find(targets, feet.add(0, 1.62, 0), 4.5, world) == null,
         "falling cannot place against air when no real support exists");
   }

   private static void rotationContinuity() {
      for (float base : new float[]{-1080, -361, -181, -1, 179, 359, 719, 1080}) {
         for (float target : new float[]{-179, -1, 0, 1, 179}) {
            var rotation = SilentRotationMath.quantizeRotation(base, 0, target, 60, 0.5);
            require(Math.abs(rotation.yaw() - base) <= 180.15,
               "crossing 360 degrees does not cause a raw full-turn snap");
            double error = Math.toDegrees(Math.atan2(Math.sin(Math.toRadians(rotation.yaw() - target)),
               Math.cos(Math.toRadians(rotation.yaw() - target))));
            require(Math.abs(error) < 0.151, "quantization stays within one mouse step of the planned face");
         }
      }
   }

   private static void stepUpOrKeepLayer() {
      FakeWorld world = new FakeWorld();
      world.solid.add(new BlockPos(0, 0, 0));
      world.solid.add(new BlockPos(1, 0, 0));
      world.solid.add(new BlockPos(2, 0, 0));
      Vec3 feet = new Vec3(1.6, 2.2, 0.5);
      Vec3 motion = new Vec3(0.24, -0.02, 0);
      var step = PlacementPlanner.findStepOrCatch(feet, motion, 0, true, false, 0.08,
         feet.add(0, 1.62, 0), 4.5, world);
      require(step != null && step.target().equals(new BlockPos(2, 1, 0)),
         "prefer a supported one-block step at the descending landing");

      world.solid.remove(new BlockPos(2, 0, 0));
      feet = new Vec3(2.05, 2.2, 0.5);
      var flat = PlacementPlanner.findStepOrCatch(feet, motion, 0, true, false, 0.08,
         feet.add(0, 1.62, 0), 4.5, world);
      require(flat != null && flat.target().equals(new BlockPos(2, 0, 0)),
         "without a supported step, catch on the original bridge layer");

      world.solid.clear();
      world.solid.add(new BlockPos(1, 0, 0));
      world.solid.add(new BlockPos(3, 0, 0));
      feet = new Vec3(2.8, 2.2, 0.5);
      world.maxRotationLayer = 0;
      flat = PlacementPlanner.findStepOrCatch(feet, motion, 0, true, false, 0.08,
         feet.add(0, 1.62, 0), 4.5, world);
      require(flat == null || flat.target().getY() == 0,
         "an invalid silent step rotation cannot send a raised use packet");
      feet = new Vec3(4.05, 1.4, 0.5);
      flat = PlacementPlanner.findStepOrCatch(feet, motion, 0, true, false, 0.08,
         feet.add(0, 1.62, 0), 4.5, world);
      require(flat != null && flat.target().equals(new BlockPos(4, 0, 0)),
         "flat catching extends the leading support instead of filling a gap behind it");
      world.maxRotationLayer = Integer.MAX_VALUE;
      var locked = PlacementPlanner.findStepOrCatch(feet, motion, 0, true, true, 0.08,
         feet.add(0, 1.62, 0), 4.5, world);
      require(locked != null && locked.target().getY() == 0, "KeepY still locks the bridge height");
      feet = new Vec3(2.6, 1.95, 0.5);
      var tooLow = PlacementPlanner.findStepOrCatch(feet, motion, 0, true, false, 0.08,
         feet.add(0, 1.62, 0), 4.5, world);
      require(tooLow == null || tooLow.target().getY() == 0,
         "do not insert a step before the player's feet clear its top");
   }

   private static void landingDirectedStepUp() {
      FakeWorld world = new FakeWorld();
      world.solid.add(new BlockPos(0, 0, 0));
      world.solid.add(new BlockPos(1, 0, 0));
      world.solid.add(new BlockPos(2, 0, 0));
      Vec3 feet = new Vec3(1.6, 2.2, 0.5);
      Vec3 motion = new Vec3(0.24, 0.08, 0);
      var step = PlacementPlanner.findStepOrCatch(feet, motion, 0, true, false, 0.08,
         feet.add(0, 1.62, 0), 4.5, world);
      require(step != null && step.target().equals(new BlockPos(2, 1, 0)),
         "a step must catch the projected landing, not create a hump behind it");
      world.solid.add(new BlockPos(2, 1, 0));
      require(PlacementPlanner.findStepOrCatch(feet, motion, 0, true, false, 0.08,
         feet.add(0, 1.62, 0), 4.5, world) == null,
         "an existing raised landing must stop extra raised and flat placements");
      world.solid.remove(new BlockPos(2, 1, 0));
      world.solid.remove(new BlockPos(2, 0, 0));
      var fallback = PlacementPlanner.findStepOrCatch(feet, motion, 0, true, false, 0.08,
         feet.add(0, 1.62, 0), 4.5, world);
      require(fallback == null || fallback.target().getY() == 0,
         "a supported hump behind the landing cannot preempt the flat catch");
      feet = new Vec3(2.05, 2.2, 0.5);
      fallback = PlacementPlanner.findStepOrCatch(feet, motion, 0, true, false, 0.08,
         feet.add(0, 1.62, 0), 4.5, world);
      require(fallback != null && fallback.target().equals(new BlockPos(2, 0, 0)),
         "the flat catch resumes as soon as its real support face is visible");
   }

   private static void pendingLandingDoesNotScatterBlocks() {
      FakeWorld world = new FakeWorld();
      world.solid.add(new BlockPos(0, 0, 0));
      world.solid.add(new BlockPos(1, 0, 0));
      BlockPos leading = new BlockPos(1, 0, -1);
      world.solid.add(leading);
      world.confirmations.predict(leading, 10);
      Vec3 feet = new Vec3(1.799, 2.249, -0.25);
      Vec3 motion = new Vec3(0.2598, 0.003, -0.15);
      var placement = PlacementPlanner.findStepOrCatch(feet, motion, 0, true, false, 0.08,
         feet.add(0, 1.62, 0), 4.5, world);
      require(placement == null, "wait for the leading prediction instead of filling the corner behind it");
      world.confirmations.acknowledge(10);
      feet = new Vec3(2.0588, 2.252, -0.4);
      motion = new Vec3(0.2598, -0.075, -0.15);
      placement = PlacementPlanner.findStepOrCatch(feet, motion, 0, true, false, 0.08,
         feet.add(0, 1.62, 0), 4.5, world);
      require(placement != null && placement.target().equals(new BlockPos(2, 0, -1)),
         "an acknowledged leading block resumes the diagonal catch path");
      world.solid.add(placement.target());
      feet = new Vec3(2.578, 2.024, -0.7);
      motion = new Vec3(0.2598, -0.228, -0.15);
      placement = PlacementPlanner.findStepOrCatch(feet, motion, 0, true, false, 0.08,
         feet.add(0, 1.62, 0), 4.5, world);
      require(placement == null || placement.target().getY() <= 0,
         "a last-tick step cannot displace the flat fallback");
   }

   private static void forwardJumpLandings() {
      // Fixed-velocity geometry replays cover timing and rejection. They do
      // not substitute for native input/physics or a live server session.
      int stepLandings = 0;
      int rejectedSteps = 0;
      for (double speed : new double[]{0.12, 0.24, 0.30}) {
      for (int ackDelay : new int[]{1, 2, 3}) {
      for (boolean rejectStep : new boolean[]{false, true}) {
      for (int heading = 0; heading < 360; heading += 30) {
         boolean flatLanded = false;
         for (boolean catchOnly : new boolean[]{true, false}) {
         FakeWorld world = new FakeWorld();
         world.solid.add(new BlockPos(0, 0, 0));
         Vec3 feet = new Vec3(0.5, 1.0, 0.5);
         double radians = Math.toRadians(heading);
         double velocityX = -Math.sin(radians) * speed;
         double velocityZ = Math.cos(radians) * speed;
         double velocityY = 0.42;
         float sentYaw = heading, sentPitch = 0;
         boolean landed = false;
         BlockPos rejected = null;
         int rejectedAt = -1;
         for (int tick = 0; tick < 16; tick++) {
            if (rejected != null && tick >= rejectedAt + ackDelay) {
               world.solid.remove(rejected);
            }
            world.confirmations.acknowledge(tick - ackDelay);
            Vec3 motion = new Vec3(velocityX, velocityY, velocityZ);
            Vec3 eyes = feet.add(0, 1.62, 0);
            var placement = catchOnly
               ? PlacementPlanner.find(PlacementPlanner.catchTargets(feet, motion, 0, true, true, 0.08), eyes, 4.5, world)
               : PlacementPlanner.findStepOrCatch(feet, motion, 0, true, false, 0.08, eyes, 4.5, world);
            if (placement != null) {
               Vec3 delta = placement.hit().getLocation().subtract(eyes);
               float yaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
               float pitch = (float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
               var rotation = SilentRotationMath.quantizeRotation(sentYaw, sentPitch, yaw, pitch, 0.5);
               sentYaw = rotation.yaw();
               sentPitch = rotation.pitch();
               var hit = world.raycast(eyes, eyes.add(Vec3.directionFromRotation(sentPitch, sentYaw).scale(4.5)));
               if (PlacementPlanner.matchesSupport(placement, hit)) {
                  require(placement.target().getY() <= PlacementPlanner.blockLayer(feet.y),
                     "catch blocks cannot intersect the moving player's feet");
                  world.solid.add(placement.target());
                  world.confirmations.predict(placement.target(), tick);
                  if (placement.target().getY() > 0) {
                     world.maxRotationLayer = 0;
                     if (rejectStep) {
                        rejected = placement.target();
                        rejectedAt = tick;
                        rejectedSteps++;
                     }
                  }
               }
            }
            Vec3 next = feet.add(velocityX, velocityY, velocityZ);
            if (velocityY < 0) {
               for (BlockPos pos : world.solid) {
                  if (pos.equals(rejected)) continue;
                  double top = pos.getY() + 1.0;
                  if (feet.y >= top && next.y <= top
                     && next.x + 0.3 > pos.getX() && next.x - 0.3 < pos.getX() + 1
                     && next.z + 0.3 > pos.getZ() && next.z - 0.3 < pos.getZ() + 1) {
                     landed = true;
                     if (pos.getY() > 0) stepLandings++;
                     break;
                  }
               }
               if (landed) break;
            }
            feet = next;
            velocityY = (velocityY - 0.08) * 0.98;
         }
         if (catchOnly) {
            flatLanded = landed;
         } else {
            require(landed || speed > 0.24 && !flatLanded,
               "step selection cannot lose a feasible flat catch: heading=" + heading + ", speed=" + speed
                  + ", ackDelay=" + ackDelay + ", rejectStep=" + rejectStep);
         }
      }
      }
      }
      }
      }
      require(stepLandings > 0, "the combined planner can actually land on raised steps");
      require(rejectedSteps > 0, "the jump replay exercises rejected raised placements");
   }

   private static void movementAndSprint() {
      // Server reconstructs acceleration from these booleans. Exercise every
      // keyboard direction while camera/server yaw differ, including sprint.
      for (int forward = -1; forward <= 1; forward++) {
         for (int sideways = -1; sideways <= 1; sideways++) {
            for (int camera = -180; camera < 180; camera += 15) {
               for (int server = -180; server < 180; server += 15) {
                  Input keys = new Input(forward > 0, forward < 0, sideways > 0, sideways < 0, false, false, true);
                  var movement = SilentRotationMath.correctMovement(keys, camera, server);
                  double length = Math.hypot(movement.sideways(), movement.forward());
                  double expectedSide = length == 0 ? 0 : movement.sideways() / length;
                  double expectedForward = length == 0 ? 0 : movement.forward() / length;
                  require(Math.abs(movement.normalizedSideways() - expectedSide) < 0.000001
                     && Math.abs(movement.normalizedForward() - expectedForward) < 0.000001,
                     "local movement must exactly match the encoded legal keyboard tuple");
                  require(forward == 0 && sideways == 0 ? length == 0 : length > 0,
                     "correction preserves whether movement was requested");
               }
            }
         }
      }
      Input sprintForward = new Input(true, false, false, false, false, false, true);
      var backwards = SilentRotationMath.correctMovement(sprintForward, 0, 180);
      require(backwards.forward() < 0, "a backwards-facing scaffold needs backwards input, which cannot sprint");
      var forwards = SilentRotationMath.correctMovement(sprintForward, 0, 0);
      require(forwards.forward() > 0, "sprint remains possible when corrected input goes forward");
   }

   private static void silentHeadRendering() {
      SilentHeadPose head = new SilentHeadPose();
      require(!head.isActive(), "no head override before a placement");
      head.beginTick();
      head.aim(90, 75, 10, 5);
      require(head.isActive(), "head pose remains available for frames after movement");
      require(head.previousYaw() == 10 && head.previousPitch() == 5,
         "the initial model turn starts at the user's heading");
      require(head.yaw() == 90 && head.pitch() == 75, "the model uses the placement direction");
      require(head.interpolatedYaw(0.5F) == 50 && head.interpolatedPitch(0.5F) == 40,
         "model interpolation is independent of camera orientation");
      for (int frame = 0; frame <= 10; frame++) {
         head.interpolatedYaw(frame / 10.0F);
         head.interpolatedPitch(frame / 10.0F);
      }
      require(head.isActive() && head.yaw() == 90 && head.pitch() == 75,
         "rendering frames cannot consume or change the network-facing head pose");
      head.beginTick();
      head.aim(95, 80, -20, -10);
      require(head.previousYaw() == 90 && head.previousPitch() == 75,
         "changing the camera does not reset consecutive model turns");
      head.clear();
      require(!head.isActive(), "disabling scaffold clears its model override");
      head.beginTick();
      head.aim(-179, 30, 179, 0);
      require(head.yaw() == 181 && head.interpolatedYaw(0.5F) == 180,
         "the head takes the short path across the angle boundary");
      head.beginTick(); // A tick without a placement must release the render pose.
      require(!head.isActive(), "no stale model head rotation without a placement");
      head.beginTick();
      head.aim(0, -120, 20, 10);
      require(head.previousYaw() == 20 && head.pitch() == -90,
         "reacquisition starts at the camera and keeps pitch within native bounds");
   }

   private static void cameraRestorationOrder() {
      EventBus bus = new EventBus();
      CameraTick state = new CameraTick();
      // Real startup order: PositionManager subscribes first; EventManager
      // subscribes next; enabled modules are registered afterwards.
      bus.register(new TickObserver(state));
      bus.register(new SharedRotationSnapshot(state, "shared"));
      bus.register(new SilentPlacementTick(state));
      bus.register(new SharedRotationSnapshot(state, "late"));
      for (int tick = 0; tick < 3; tick++) {
         state.order.clear();
         state.yaw = 17 + tick;
         state.pitch = -12 + tick;
         float cameraYaw = state.yaw;
         float cameraPitch = state.pitch;
         bus.post(new UpdateWalkingPlayerEvent(Stage.MOTION_PRE));
         require(state.order.equals(List.of("plan", "observer", "shared", "late", "cleanup")),
            "priority is descending and stable even when a listener registers late");
         require(state.yaw == 135 && state.pitch == 80, "physics uses the placement rotation");
         bus.post(new UpdateWalkingPlayerEvent(Stage.PRE));
         require(state.yaw == 135 && state.pitch == 80, "movement serialization keeps its physics yaw");
         bus.post(new UpdateWalkingPlayerEvent(Stage.POST));
         require(state.yaw == cameraYaw && state.pitch == cameraPitch,
            "the camera survives all shared and late POST restorations");
         require(state.sentYaw == 135 && state.sentPitch == 80,
            "restoring the camera cannot rewrite the rotation already sent");
      }
   }

   public static final class CameraTick {
      final List<String> order = new ArrayList<>();
      float yaw, pitch, sentYaw, sentPitch;
   }

   public static final class TickObserver {
      private final CameraTick state;
      TickObserver(CameraTick state) { this.state = state; }
      @Subscribe
      public void onTick(UpdateWalkingPlayerEvent event) { this.state.order.add("observer"); }
   }

   public static final class SharedRotationSnapshot {
      private final CameraTick state;
      private final String name;
      private float yaw, pitch;
      SharedRotationSnapshot(CameraTick state, String name) { this.state = state; this.name = name; }
      @Subscribe
      public void onTick(UpdateWalkingPlayerEvent event) {
         this.state.order.add(this.name);
         if (event.getStage() == Stage.PRE) {
            this.yaw = this.state.yaw;
            this.pitch = this.state.pitch;
         } else if (event.getStage() == Stage.POST) {
            this.state.yaw = this.yaw;
            this.state.pitch = this.pitch;
         }
      }
   }

   public static final class SilentPlacementTick {
      private final CameraTick state;
      private float cameraYaw, cameraPitch;
      SilentPlacementTick(CameraTick state) { this.state = state; }
      @Subscribe(priority = 100)
      public void plan(UpdateWalkingPlayerEvent event) {
         this.state.order.add("plan");
         if (event.getStage() == Stage.MOTION_PRE) {
            this.cameraYaw = this.state.yaw;
            this.cameraPitch = this.state.pitch;
            this.state.yaw = 135;
            this.state.pitch = 80;
         } else if (event.getStage() == Stage.PRE) {
            this.state.sentYaw = this.state.yaw;
            this.state.sentPitch = this.state.pitch;
         }
      }
      @Subscribe(priority = -1000)
      public void cleanup(UpdateWalkingPlayerEvent event) {
         this.state.order.add("cleanup");
         if (event.getStage() == Stage.POST) {
            this.state.yaw = this.cameraYaw;
            this.state.pitch = this.cameraPitch;
         }
      }
   }

   private static void require(boolean condition, String message) {
      checks++;
      if (!condition) throw new AssertionError(message);
   }

   private static final class FakeWorld implements PlacementPlanner.World {
      final Set<BlockPos> solid = new HashSet<>();
      final PlacementConfirmations confirmations = new PlacementConfirmations();
      int maxRotationLayer = Integer.MAX_VALUE;

      public boolean rotationValid(PlacementPlanner.Placement placement) {
         return placement.target().getY() <= this.maxRotationLayer;
      }

      public boolean replaceable(BlockPos pos) {
         return !this.solid.contains(pos);
      }

      public boolean supportsLanding(BlockPos pos) {
         return this.solid.contains(pos);
      }

      public boolean attachable(BlockPos pos, Direction face) {
         return this.solid.contains(pos) && !this.confirmations.isPending(pos);
      }

      public BlockHitResult raycast(Vec3 from, Vec3 to) {
         BlockHitResult closest = null;
         for (BlockPos pos : this.solid) {
            AABB box = new AABB(pos);
            if (box.contains(from)) return new BlockHitResult(from, Direction.UP, pos, true);
            // The native static helper translates local collision shapes by pos.
            BlockHitResult hit = AABB.clip(List.of(new AABB(0, 0, 0, 1, 1, 1)), from, to, pos);
            if (hit != null && (closest == null || from.distanceToSqr(hit.getLocation()) < from.distanceToSqr(closest.getLocation()))) {
               closest = hit;
            }
         }
         return closest;
      }
   }
}
