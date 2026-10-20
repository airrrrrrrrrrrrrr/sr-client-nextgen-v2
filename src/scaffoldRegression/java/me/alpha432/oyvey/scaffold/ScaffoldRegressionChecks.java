package me.alpha432.oyvey.scaffold;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import me.alpha432.oyvey.features.modules.movement.scaffold.PlacementConfirmations;
import me.alpha432.oyvey.features.modules.movement.scaffold.PlacementPlanner;
import me.alpha432.oyvey.features.modules.movement.scaffold.SilentHeadPose;
import me.alpha432.oyvey.features.modules.movement.scaffold.ScaffoldMovement;
import me.alpha432.oyvey.features.modules.movement.scaffold.BackwardBridgeRotation;
import me.alpha432.oyvey.features.modules.movement.scaffold.TellyBridge;
import me.alpha432.oyvey.features.modules.movement.scaffold.Telly2Bridge;
import me.alpha432.oyvey.features.modules.movement.scaffold.OneTickBridgeRotation;
import me.alpha432.oyvey.features.modules.movement.scaffold.ShortRotationPulse;
import me.alpha432.oyvey.features.modules.movement.scaffold.GodBridge;
import me.alpha432.oyvey.features.modules.movement.scaffold.AndromidaBridge;
import me.alpha432.oyvey.features.modules.movement.scaffold.PlacementClickRate;
import me.alpha432.oyvey.util.BacktrackHistory;
import me.alpha432.oyvey.util.CombatLatency;
import me.alpha432.oyvey.util.RodLogic;
import me.alpha432.oyvey.util.ShieldCombat;
import me.alpha432.oyvey.util.JumpResetInput;
import me.alpha432.oyvey.util.render.AmbienceClock;
import me.alpha432.oyvey.features.modules.movement.scaffold.ScaffoldUseInput;
import me.alpha432.oyvey.util.SilentRotationMath;
import me.alpha432.oyvey.util.TransmittedRotation;
import me.alpha432.oyvey.util.OneTickAttackRotation;
import me.alpha432.oyvey.util.PolarRotation;
import me.alpha432.oyvey.util.RecordedClickPlayback;
import me.alpha432.oyvey.manager.RecordedClickManager;
import me.alpha432.oyvey.util.CriticalTiming;
import me.alpha432.oyvey.util.CombatAim;
import me.alpha432.oyvey.util.KnockbackDisplacement;
import me.alpha432.oyvey.util.NativeClickPulse;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.EventBus;
import me.alpha432.oyvey.event.system.Subscribe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Input;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;

public final class ScaffoldRegressionChecks {
   private static int checks;

   public static void main(String[] args) {
      if (args.length > 0 && args[0].equals("recorded-clicks")) {
         recordedClicks(); scaffoldClickRates(); oneTickAttackRotation(); transmittedAttackLook(); preferredCriticalTiming(); shieldCombat();
         System.out.println("Recorded click capture, JSON and playback checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("combat-rotation-modes")) {
         oneTickAttackRotation(); polarRotations(); transmittedAttackLook(); preferredCriticalTiming(); shieldCombat();
         System.out.println("One Tick and Polar combat rotation checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("godbridge")) {
         godbridgeCycle(); godbridgePlacementReplay();
         System.out.println("Godbridge alignment, seven-block jump and placement checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("scaffold-single-place")) {
         godbridgeCycle(); godbridgePlacementReplay(); oneTickFutureCollision();
         oneTickRotation(); oneTickNormalReplay(); oneTickAirReplay();
         System.out.println("Single-placement Godbridge and One Tick climbing checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("knockback-grim")) {
         knockbackSafeAim(); knockbackAimStrength(); knockbackDisplacement(); knockbackMovementFix();
         System.out.println("Knockback aim and movement checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("rotation-input")) {
         shortRotationPulse(); oneTickRotation(); oneTickNormalReplay(); oneTickAirReplay(); legitBlockClicks();
         System.out.println("Scaffold rotation and Legit input checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("scaffold-movefix-andromida")) {
         scaffoldMovementFixToggle(); andromidaBridge();
         shortRotationPulse(); oneTickRotation(); oneTickNormalReplay(); oneTickAirReplay(); scaffoldStartupMovement();
         tellyCycle(); tellyForwardRecovery(); telly2Cycle(); tellyMovementReplay();
         System.out.println("Scaffold MovementFix and Andromida checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("legit-block")) {
         legitBlockClicks();
         System.out.println("Legit native click checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("ambience")) {
         ambienceClock();
         System.out.println("Ambience clock checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("clutch-latency")) {
         allDirectionClutch(); clutchLandingReplay(); combatLatency();
         System.out.println("Clutch and combat-latency checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("shield-velocity")) {
         shieldCombat(); jumpResetInput();
         System.out.println("Shield and JumpReset checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("rod")) {
         rodCombatCycle();
         System.out.println("AutoRod checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("telly2")) {
         telly2Cycle();
         tellyPlacementReplay(false, true);
         tellyPlacementReplay(true, true);
         System.out.println("Telly2 checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("telly-forward")) {
         tellyForwardRecovery(); tellyCycle(); telly2Cycle(); tellyMovementReplay();
         tellyPlacementReplay(); tellyPlacementReplay(true);
         tellyPlacementReplay(false, true); tellyPlacementReplay(true, true);
         System.out.println("Telly forward recovery checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("one-tick-knockback")) {
         oneTickRotation(); knockbackSafeAim(); knockbackDisplacement(); knockbackMovementFix();
         System.out.println("One Tick and knockback checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("one-tick-normal")) {
         oneTickNormalReplay();
         System.out.println("Normal One Tick movement checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("one-tick-live-motion")) {
         oneTickNormalReplay(); oneTickAirReplay();
         System.out.println("One Tick native motion checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("scaffold-recovery")) {
         oneTickRotation(); oneTickNormalReplay(); tellyForwardRecovery(); tellyCycle(); telly2Cycle(); tellyMovementReplay();
         tellyPlacementReplay(); tellyPlacementReplay(true); tellyPlacementReplay(false,true); tellyPlacementReplay(true,true);
         System.out.println("Scaffold timing and Center recovery checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("backtrack")) {
         backtrackHistory(); backtrackPacketHooks();
         System.out.println("Backtrack checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("climb")) {
         tellyPlacementReplay(true);
         System.out.println("Telly uphill checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("support")) {
         manualUseSuppression();
         centerRotationGeometry();
         centerCornerSelection();
         System.out.println("Scaffold support checks passed: " + checks);
         return;
      }
      if (args.length > 0 && args[0].equals("telly")) {
         tellyCycle();
         tellyMovementReplay();
         tellyPlacementReplay();
         System.out.println("Telly checks passed: " + checks);
         return;
      }
      placementGeometry();
      allDirectionClutch();
      clutchLandingReplay();
      combatLatency();
      placementLookOrdering();
      jumpingCatchPlanning();
      stepUpOrKeepLayer();
      landingDirectedStepUp();
      pendingLandingDoesNotScatterBlocks();
      forwardJumpLandings();
      sprintingBridgeLatency();
      sprintInputEligibility();
      automaticSprintDiagonals();
      transmittedAttackLook();
      predictionAcknowledgements();
      rotationContinuity();
      silentHeadRendering();
      cameraRestorationOrder();
      movementAndSprint();
      legacyInputEncoding();
      backwardBridge();
      jumpingBridgeHeight();
      backwardTurnEasing();
      preferredCriticalTiming();
      visibleCombatAim();
      headAimAndReach();
      knockbackDisplacement();
      knockbackMovementFix();
      oneTickRotation();
      oneTickNormalReplay();
      knockbackSafeAim();
      scaffoldStartupMovement();
      scaffoldMovementFixToggle(); andromidaBridge();
      tellyCycle();
      tellyForwardRecovery();
      tellyMovementReplay();
      tellyPlacementReplay();
      tellyPlacementReplay(true);
      manualUseSuppression();
      centerRotationGeometry();
      centerCornerSelection();
      telly2Cycle();
      tellyPlacementReplay(false, true);
      tellyPlacementReplay(true, true);
      backtrackHistory();
      backtrackPacketHooks();
      rodCombatCycle();
      shieldCombat();
      jumpResetInput();
      ambienceClock();
      System.out.println("Scaffold regression checks passed: " + checks);
   }

   private static void ambienceClock() {
      var clock = net.minecraft.core.Holder.direct(new net.minecraft.world.clock.WorldClock());
      long[] server = {18000};
      int[] selected = {6000};
      boolean[] enabled = {true};
      net.minecraft.world.clock.ClockManager manager = ignored -> enabled[0] ? AmbienceClock.withTime(server[0], selected[0]) : server[0];
      var track = new net.minecraft.util.KeyframeTrack<Float>(List.of(
         new net.minecraft.util.Keyframe<>(0, 0.5F), new net.minecraft.util.Keyframe<>(6000, 1.0F),
         new net.minecraft.util.Keyframe<>(12000, 0.5F), new net.minecraft.util.Keyframe<>(18000, 0.0F)), net.minecraft.util.EasingType.LINEAR);
      var daylight = new net.minecraft.world.timeline.AttributeTrackSampler<Float, Float>(clock, java.util.Optional.of(24000),
         net.minecraft.world.attribute.modifier.FloatModifier.ADD, track, net.minecraft.world.attribute.LerpFunction.ofFloat(), manager);
      int tick = 0;
      for (long packetTime : new long[]{18000, 19000, 23999, 24000, 48000, 200000}) {
         server[0] = packetTime;
         require(Math.abs(daylight.applyTimeBased(0F, tick++) - 1) < 0.0001, "native atmosphere sampler remains at selected noon despite incoming server time updates");
         require(Math.floorDiv(manager.getTotalTicks(clock), 24000) == Math.floorDiv(packetTime, 24000), "custom time preserves the server day/moon counter");
      }
      selected[0] = 18000;
      require(Math.abs(daylight.applyTimeBased(0F, tick++)) < 0.0001, "selecting midnight updates native daylight, not just the sun's mesh angle");
      selected[0] = 24000;
      require(Math.abs(daylight.applyTimeBased(0F, tick++) - 0.5F) < 0.0001, "24000 wraps to the next cycle's dawn value");
      server[0] = 30000; enabled[0] = false;
      require(Math.abs(daylight.applyTimeBased(0F, tick++) - 1) < 0.0001 && manager.getTotalTicks(clock) == 30000,
         "disabling custom time immediately exposes the latest real clock without waiting for another packet");
      server[0] = 42000;
      require(Math.abs(daylight.applyTimeBased(0F, tick)) < 0.0001, "the real server day/night cycle continues normally after disabling");
   }

   private static void shieldCombat() {
      for (boolean raised : new boolean[]{false, true}) for (boolean keep : new boolean[]{false, true})
      for (boolean breaking : new boolean[]{false, true}) {
         List<String> actions = new ArrayList<>();
         ShieldCombat.attack(raised, keep, breaking, () -> actions.add("release"), () -> actions.add("attack"), () -> actions.add("use"));
         require(actions.equals(raised && (!keep || breaking) ? List.of("release", "attack", "use") : List.of("attack")),
            "shield attacks preserve raised use only when requested; native axe disabling releases first");
      }
      List<String> failed = new ArrayList<>();
      try {
         ShieldCombat.attack(true, false, false, () -> failed.add("release"), () -> { throw new IllegalStateException("attack failed"); }, () -> failed.add("use"));
      } catch (IllegalStateException expected) { }
      require(failed.equals(List.of("release", "use")), "a failed attack still restores owned shield use");
      for (int previous = 0; previous < 9; previous++) for (int axe = 0; axe < 9; axe++) {
         if (previous == axe) continue;
         ShieldCombat.AxeSwap swap = new ShieldCombat.AxeSwap();
         swap.begin(previous, axe, 42);
         require(swap.active() && swap.owns(axe, true, true), "only the selected native axe slot is owned");
         require(ShieldCombat.timingReady(true, true, 0, 1, false), "shield break can attack immediately after equipping an uncharged axe");
         require(!ShieldCombat.timingReady(false, true, 0.99F, 1, true)
            && ShieldCombat.timingReady(false, true, 1, 1, false), "ordinary attacks retain native charge requirements");
         swap.begin((previous + 1) % 9, (axe + 1) % 9, 99);
         require(swap.target() == 42, "another request cannot overwrite a pending axe swap's target or original slot");
         require(swap.finish(axe, true, true, true) == previous && !swap.active(), "completed axe hit restores the exact prior weapon once");
         require(swap.finish(axe, true, true, true) == -1, "a completed swap cannot restore twice");
         swap.begin(previous, axe, 42);
         require(swap.finish((axe + 1) % 9, true, true, true) == -1, "manual selection wins over shield-break restoration");
         swap.begin(previous, axe, 42);
         require(swap.finish(axe, true, false, true) == -1, "a new world/player never receives the old weapon slot");
         swap.begin(previous, axe, 42);
         require(swap.finish(axe, false, true, true) == -1, "a replaced/moved axe stack is no longer owned");
         swap.begin(previous, axe, 42);
         require(swap.finish(axe, true, true, false) == -1 && !swap.active(), "ShieldSwapBack off intentionally keeps the axe without retaining stale ownership");
      }
   }

   private static void jumpResetInput() {
      JumpResetInput jump = new JumpResetInput();
      jump.arm(10, true, true);
      require(!jump.consume(10, true, false, false), "velocity without damage does not create a jump");
      require(jump.consume(11, true, true, false), "a fresh grounded damage/velocity pair emits one native jump request");
      require(!jump.consume(11, true, true, false) && !jump.consume(12, true, true, false), "one hit cannot double-jump or repeatedly add sprint impulse");
      jump.arm(12, true, true);
      require(!jump.consume(12, true, true, false), "duplicate velocity packets during the hit do not rearm jumping");
      jump.arm(22, true, true);
      require(jump.consume(22, true, true, false), "a later independent hit can request another native jump");
      jump.reset(); jump.arm(0, false, true);
      require(!jump.consume(1, true, true, false), "airborne knockback is never delayed until a later landing");
      jump.reset(); jump.arm(0, true, false);
      require(!jump.consume(1, true, true, false), "vertical-only updates are not combat jump-reset triggers");
      jump.reset(); jump.arm(0, true, true);
      require(!jump.consume(3, true, true, false), "stale damage cannot jump several ticks after a velocity update");
      jump.reset(); jump.arm(0, true, true);
      require(!jump.consume(0, true, true, true) && !jump.consume(1, true, true, false), "a manually held jump is never duplicated or replayed after release");
      jump.reset(); jump.arm(0, true, true);
      require(!jump.consume(0, false, true, false) && !jump.consume(1, true, true, false), "lost ground, menus, liquids, and competing movement controls discard the request");
      jump.reset(); jump.arm(0, true, true); jump.correction(1);
      require(!jump.consume(1, true, true, false), "rollback discards pending jump input");
      jump.arm(2, true, true);
      require(!jump.consume(2, true, true, false), "a correction briefly suppresses rearming on delayed velocity packets");
      jump.arm(12, true, true);
      require(jump.consume(12, true, true, false), "native jumping can resume after correction grace");
      jump.arm(30, true, true); jump.reset();
      require(!jump.consume(30, true, true, false), "disable/world changes clear the previous session's queued jump");
   }

   private static void telly2Cycle() {
      Telly2Bridge bridge = new Telly2Bridge();
      var step = bridge.update(true, 0, 0, true, true);
      require(step.jump() && step.lookForward(), "Telly2 starts a native forward sprint takeoff without a backward alignment pause");
      for (int tick = 1; tick <= 2; tick++) {
         step = bridge.update(false, 0.4 - tick * 0.08, 0, true, false);
         require(!step.lookForward() && !step.jump(), "Telly2 uses the rising arc for its backward catch after the native sprint takeoff");
      }
      step = bridge.update(false, 0.16, 1, true, false);
      require(!step.lookForward() && step.layer() == 1, "Telly2 raises the catch above the takeoff layer without a physical jump-key setting");
      step = bridge.update(false, 0.1, 1, true, true);
      require(step.phase() == TellyBridge.Phase.RECOVER_FORWARD && !step.jump(), "a secured landing recovers forward before touching down");
      step = bridge.update(false, -0.1, 1, true, false);
      require(!step.lookForward(), "losing the landing after a direction change resumes the catch");
      step = bridge.update(true, 0, 1, false, true);
      require(!step.jump(), "blocked headroom never requests a jump");
      bridge.reset();
      step = bridge.update(false, -0.2, -1, true, false);
      require(!step.lookForward(), "enabling in midair begins with a catch");
   }
   private static void backtrackHistory() {
      BacktrackHistory<Integer> history = new BacktrackHistory<>();
      List<Integer> released = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
         Vec3 observed = new Vec3(i < 5 ? i : 10 - i, 0, 0);
         history.record(1000 + i * 10, observed, new AABB(observed, observed.add(0.6, 1.8, 0.6)));
         history.enqueue(1000 + i * 10, i, released::add);
      }
      require(history.atOrBefore(999) == null, "no history exists before the first observed timestamp");
      require(history.atOrBefore(1049).position().x == 4, "history chooses the last observation before the rewind time");
      require(history.atOrBefore(1080).position().x == 2, "direction changes use actual observed positions, not extrapolation");
      history.release(1099, 100, released::add);
      require(released.isEmpty(), "movement remains queued until its timestamp reaches the selected delay");
      history.release(1100, 100, released::add);
      require(released.equals(List.of(0)), "release includes the exact deadline");
      history.release(1100, 20, released::add);
      require(released.equals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8)), "lowering delay drains eligible records in arrival order");
      history.flush(released::add);
      require(released.equals(java.util.stream.IntStream.range(0, 10).boxed().toList()), "flushing neither drops nor duplicates relative moves");
      require(history.age(10000) == 0, "an empty queue reports no active delay");
      history.clear(); released.clear();
      for (int i = 0; i < 1000; i++) history.enqueue(i, i, released::add);
      require(history.size() == BacktrackHistory.CAPACITY && released.size() == 1000 - BacktrackHistory.CAPACITY,
         "packet bursts have a hard bound and overflow releases oldest moves");
      history.flush(released::add);
      require(released.equals(java.util.stream.IntStream.range(0, 1000).boxed().toList()), "overflow and flush preserve complete FIFO ordering");
      for (int i = 0; i < 1000; i++) history.record(i * 50L, Vec3.ZERO, new AABB(0, 0, 0, 1, 1, 1));
      require(history.atOrBefore(48000) == null && history.atOrBefore(49950) != null, "old history expires rather than accumulating across a session");
      history.enqueue(50000, 1, released::add); history.clear();
      require(history.size() == 0 && history.atOrBefore(Long.MAX_VALUE) == null, "world/target reset discards both queue and historical boxes");
   }

   private static void combatLatency() {
      CombatLatency latency = new CombatLatency();
      Object first = new Object(), second = new Object();
      List<Runnable> scheduled = new ArrayList<>();
      List<Integer> delays = new ArrayList<>(), sent = new ArrayList<>();
      java.util.function.BiConsumer<Runnable, Integer> scheduler = (task, delay) -> { scheduled.add(task); delays.add(delay); };
      require(!latency.delay(first, 0, 200, () -> sent.add(-1), scheduler), "idle connections never delay latency replies");
      for (int ms : new int[]{0, 1, 50, 100, 200, 400}) {
         latency.clear(); scheduled.clear(); delays.clear(); sent.clear();
         latency.attack(first, 1000);
         boolean delayed = latency.delay(first, 1001, ms, () -> sent.add(ms), scheduler);
         require(delayed == (ms > 0), "zero MS bypasses buffering and each positive setting activates combat latency");
         if (!delayed) continue;
         require(delays.equals(List.of(ms)) && sent.isEmpty(), "schedule the actual reply for exactly the requested extra milliseconds");
         scheduled.getFirst().run(); scheduled.getFirst().run();
         require(sent.equals(List.of(ms)) && latency.size() == 0, "each latency reply and its callbacks execute once");
      }
      latency.clear(); scheduled.clear(); sent.clear(); delays.clear();
      latency.attack(first, 2000);
      latency.delay(first, 2001, 400, () -> sent.add(1), scheduler);
      latency.delay(first, 2002, 50, () -> sent.add(2), scheduler);
      scheduled.get(1).run();
      require(sent.isEmpty(), "lowering the setting never reorders transaction replies");
      scheduled.getFirst().run();
      require(sent.equals(List.of(1, 2)), "earlier replies remain first after their scheduled delay");
      latency.delay(first, 2003, 100, () -> sent.add(3), scheduler);
      latency.flush();
      scheduled.getLast().run();
      require(sent.equals(List.of(1, 2, 3)) && latency.size() == 0, "disable/zero flush sends pending replies without duplicates");
      latency.attack(first, 3000);
      require(!latency.delay(second, 3001, 100, () -> sent.add(-1), scheduler), "another server's connection cannot inherit combat latency");
      latency.delay(first, 3001, 100, () -> sent.add(4), scheduler);
      latency.attack(second, 3010);
      scheduled.getLast().run();
      require(sent.equals(List.of(1, 2, 3)), "old scheduled replies are invalidated on a connection change");
      require(!latency.active(second, 4510) && !latency.delay(second, 4510, 100, () -> sent.add(-1), scheduler), "combat latency expires without continued attacks");
      latency.clear(); scheduled.clear(); sent.clear();
      latency.attack(first, 5000);
      for (int i = 0; i < 150; i++) { final int id = i; latency.delay(first, 5001, 400, () -> sent.add(id), scheduler); }
      require(latency.size() <= 128, "latency reply bursts have a hard memory bound");
      latency.flush(); scheduled.forEach(Runnable::run);
      require(sent.equals(java.util.stream.IntStream.range(0, 150).boxed().toList()), "overflow and flush preserve every reply in order");
   }

   private static void godbridgeCycle() {
      var bridge=new GodBridge();
      require(bridge.placementLayer(0,true)==0,"Godbridge starts at the current bridge height");
      Input forward=new Input(true,false,false,false,false,false,false);
      for(float yaw:new float[]{-720,-315,-90,-22,0,45,90,225,720}) {
         float heading=GodBridge.heading(forward,yaw);
         require(Math.abs(Math.IEEEremainder(heading-45,90))<1e-6,"Godbridge aligns to a 45-degree diagonal on the block grid");
      }
      for(int cycle=0;cycle<3;cycle++) {
         for(int block=0;block<7;block++) {
            BlockPos pos=new BlockPos(cycle*7+block,0,0);
            bridge.placed(pos); bridge.placed(pos);
            if(block<6) require(!bridge.jump(true,true,true,true),"fewer than seven distinct successful blocks cannot trigger a jump");
         }
         require(!bridge.jump(true,true,true,false),"a blocked ceiling postpones the native jump");
         require(!bridge.jump(true,false,true,true),"standing still does not spend the seven-block jump");
         require(!bridge.jump(true,true,false,true),"wait for the diagonal look to be transmitted before jumping");
         require(bridge.jump(true,true,true,true),"the seventh successful block queues the native jump");
         require(bridge.placementLayer(cycle,true)==cycle,"queuing a jump cannot place a block through the player's feet");
         require(!bridge.jump(false,true,true,true),"one observed takeoff consumes exactly one seven-block cycle");
         require(bridge.placementLayer(cycle,false)==cycle,"wait until the jump clears the next block's top");
         require(bridge.placementLayer(cycle+1,false)==cycle+1,"each seven-block takeoff raises the bridge exactly one block");
         require(bridge.placementLayer(cycle+2,false)==cycle+1,"a higher manual jump cannot add another automatic step");
         require(!bridge.jump(true,true,true,true),"landing cannot repeat the previous jump without another seven blocks");
      }
      bridge.reset();
      require(!bridge.jump(true,true,true,true),"disable, correction and world changes reset the cycle");
      require(bridge.placementLayer(12,true)==12,"reset rebases the staircase at the player's new height");
   }

   private static void godbridgePlacementReplay() {
      Input keys=new Input(true,false,false,false,false,false,false);
      for(float heading:new float[]{0,45,90,135,180,225,270,315}) {
         FakeWorld world=new FakeWorld(); world.nativeRaycast=world.allowPredictedSupports=true;
         for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) world.solid.add(new BlockPos(x,0,z));
         var bridge=new GodBridge();
         Vec3 feet=new Vec3(.5,1,.5), travel=Vec3.directionFromRotation(0,heading), velocity=travel.scale(.15);
         float yaw=heading,pitch=15; double vy=-.0784; boolean grounded=true;
         int placed=0,jumps=0,landedSteps=0;
         for(int tick=0;tick<200;tick++) {
            Vec3 eyes=feet.add(0,1.62,0),motion=velocity.add(0,vy,0);
            world.playerBox=new AABB(feet.x-.3,feet.y,feet.z-.3,feet.x+.3,feet.y+1.8,feet.z+.3);
            int layer=bridge.placementLayer(PlacementPlanner.blockLayer(feet.y),grounded);
            var targets=PlacementPlanner.catchTargets(feet,motion,layer,!grounded,false,.08);
            float diagonal=GodBridge.aimYaw(keys,heading);
            var turn=TellyBridge.turn(yaw,pitch,diagonal,75.5F,.5);
            float targetPitch=PlacementPlanner.backwardPitch(targets,eyes,4.5,turn.yaw(),pitch,.5,world,true);
            var look=TellyBridge.turn(yaw,pitch,diagonal,targetPitch,.5);
            var click=tick>0&&Math.abs(net.minecraft.util.Mth.wrapDegrees(diagonal-yaw))<1
               ? PlacementPlanner.findAlongLook(targets,eyes,4.5,yaw,pitch,world):null;
            if(click!=null) {
               world.solid.add(click.target()); bridge.placed(click.target()); placed++;
            }
            boolean jump=bridge.jump(grounded,true,tick>0&&Math.abs(net.minecraft.util.Mth.wrapDegrees(diagonal-yaw))<1,true);
            if(jump) { require(placed>=7*(jumps+1),"every Godbridge jump follows seven newly successful placements"); jumps++; }
            var corrected=SilentRotationMath.correctMovement(keys,heading,look.yaw());
            Vec3 input=inputWorld(corrected,look.yaw());
            Vec3 requested=velocity.add(input.scale(grounded?.1:.02)).add(0,vy,0);
            if(jump) requested=requested.with(Direction.Axis.Y,.42);
            var shapes=world.solid.stream().map(pos->net.minecraft.world.phys.shapes.Shapes.block().move(pos.getX(),pos.getY(),pos.getZ())).toList();
            Vec3 resolved=ScaffoldMovement.collisionStep(world.playerBox,requested,shapes);
            feet=feet.add(resolved);
            grounded=requested.y<0&&Math.abs(resolved.y-requested.y)>1e-7;
            if(grounded) {
               landedSteps=Math.max(landedSteps,(int)Math.round(feet.y)-1);
               require(Math.abs(feet.y-(jumps+1))<.001,"each automatic jump must land one full block higher: heading="+heading+" tick="+tick+" feet="+feet+" jumps="+jumps);
            }
            velocity=new Vec3(resolved.x,0,resolved.z).scale(grounded?.546:.91);
            vy=((Math.abs(resolved.y-requested.y)>1e-7?0:requested.y)-.08)*.98;
            world.playerBox=new AABB(feet.x-.3,feet.y,feet.z-.3,feet.x+.3,feet.y+1.8,feet.z+.3);
            layer=bridge.placementLayer(PlacementPlanner.blockLayer(feet.y),grounded);
            targets=PlacementPlanner.catchTargets(feet,velocity.add(0,vy,0),layer,!grounded,false,.08);
            float refined=PlacementPlanner.backwardPitch(targets,feet.add(0,1.62,0),4.5,look.yaw(),pitch,.5,world,true);
            float newPitch=SilentRotationMath.quantizeRotation(yaw,pitch,look.yaw(),look.pitch()+Math.clamp(refined-look.pitch(),-40,40),.5).pitch();
            if(click==null||PlacementPlanner.lookIntersectsClickedSupport(click,eyes,4.5,look.yaw(),newPitch)) look=new SilentRotationMath.Rotation(look.yaw(),newPitch);
            require(feet.y>=.999,"Godbridge must extend its real walking/jumping landing: heading="+heading+" tick="+tick+" placed="+placed+" jumps="+jumps+" feet="+feet);
            yaw=look.yaw(); pitch=look.pitch();
         }
         require(placed>=21&&jumps>=3,"Godbridge keeps placing through repeated seven-block jump cycles: heading="+heading+" placed="+placed+" jumps="+jumps);
         require(landedSteps>=3,"Godbridge climbs at least three real block steps in each travel direction");
      }
   }

   private static void shortRotationPulse() {
      ShortRotationPulse pulse = new ShortRotationPulse();
      require(!pulse.active(), "an idle packet pulse cannot own aim");
      long first = pulse.start(100_000_000L);
      require(pulse.active() && pulse.remaining(first,100_000_000L)==5_000_000L,
         "the deadline starts at packet submission, without waiting for a rendered frame");
      require(!pulse.finish(first,104_999_999L,false), "a scheduled callback cannot return early");
      require(pulse.finish(first,105_000_000L,false), "0.1 tick returns five milliseconds after the aim packet");
      require(!pulse.finish(first,106_000_000L,false), "duplicate render and queued callbacks cannot send a second return");
      long second = pulse.start(200_000_000L);
      require(!pulse.finish(first,210_000_000L,true) && pulse.active(), "a previous callback cannot release a newer placement's aim");
      pulse.clear();
      require(!pulse.finish(second,220_000_000L,true), "world changes and server corrections invalidate queued returns");
      long third = pulse.start(300_000_000L);
      require(pulse.finish(third,300_000_001L,true), "disabling can restore aim immediately before the deadline");
      long fourth = pulse.start(400_000_000L);
      require(pulse.finish(fourth,399_000_000L,false), "a clock rollback releases the old packet pulse");
      long fifth = pulse.start(500_000_000L);
      require(pulse.finish(fifth,560_000_000L,false), "a busy client returns on its first available callback without another tick of waiting");
   }

   private static void oneTickRotation() {
      for (int direction = 0; direction < 16; direction++) for (double speed : new double[]{.26, .4}) {
         float heading = direction * 22.5F;
         Vec3 travel = Vec3.directionFromRotation(0, heading);
         FakeWorld world = new FakeWorld(); world.nativeRaycast = true; world.solid.add(BlockPos.ZERO);
         Vec3 feet = new Vec3(.5 + travel.x * 1.05, 1.65, .5 + travel.z * 1.05);
         Vec3 motion = new Vec3(travel.x * speed, -.16, travel.z * speed);
         world.playerBox = new AABB(feet.x-.3, feet.y, feet.z-.3, feet.x+.3, feet.y+1.8, feet.z+.3);
         Vec3 eyes = feet.add(0, 1.62, 0);
         var targets = PlacementPlanner.catchTargets(feet, motion, 0, true, true, .08);
         var pulse = new OneTickBridgeRotation();
         var forward = new SilentRotationMath.Rotation(heading, 15);
         var aim = pulse.update(targets, eyes, eyes.add(motion), 4.5, heading, 15, .5, forward, true, world);
         require(aim.aiming() && aim.placement() == null && !aim.returnAfterAttempt(), "One Tick transmits one aim before attempting a click");
         feet = feet.add(motion); eyes = eyes.add(motion);
         world.playerBox = new AABB(feet.x-.3, feet.y, feet.z-.3, feet.x+.3, feet.y+1.8, feet.z+.3);
         var click = pulse.update(targets, eyes, eyes.add(motion), 4.5, aim.look().yaw(), aim.look().pitch(), .5, forward, false, world);
         require(click.returnAfterAttempt() && click.placement() != null, "the prepared look is consumed on the next tick even when another aim is not allowed");
         require(PlacementPlanner.lookMatchesSupport(click.placement(), eyes, 4.5, click.look().yaw(), click.look().pitch(), world),
            "One Tick click hits its real native support after movement: heading=" + heading + " speed=" + speed);
         var released = pulse.update(targets, eyes, eyes, 4.5, forward.yaw(), forward.pitch(), .5, forward, false, world);
         require(!released.aiming() && released.look().equals(forward) && released.placement() == null, "a missed or completed attempt cannot retain the old aim");
         pulse.update(targets, eyes, eyes, 4.5, heading, 15, .5, forward, true, world);
         pulse.reset();
         require(pulse.update(targets, eyes, eyes, 4.5, heading, 15, .5, forward, false, world).placement() == null,
            "mode/world/correction resets discard an armed placement");
      }
   }

   private static void oneTickNormalReplay() {
      for (float heading : new float[]{0,22.5F,45,67.5F,90,135,180,225,270,315}) for (boolean sprint : new boolean[]{false,true}) {
         FakeWorld world = new FakeWorld(); world.nativeRaycast = world.allowPredictedSupports = true;
         for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) world.solid.add(new BlockPos(x,0,z));
         var pulse = new OneTickBridgeRotation();
         Vec3 travel = Vec3.directionFromRotation(0,heading), feet = new Vec3(.5,1,.5), velocity = travel.scale(.15);
         float yaw=heading,pitch=15;
         double vy=-.0784;
         boolean grounded=true;
         int placed=0;
         StringBuilder trace = new StringBuilder();
         for (int tick=0;tick<80;tick++) {
            world.playerBox = new AABB(feet.x-.3,feet.y,feet.z-.3,feet.x+.3,feet.y+1.8,feet.z+.3);
            Vec3 eyes=feet.add(0,1.62,0), motion=velocity.add(0,vy,0);
            int layer=PlacementPlanner.blockLayer(feet.y);
            var targets=PlacementPlanner.catchTargets(feet,motion,layer,!grounded,true,.08);
            var forward=new SilentRotationMath.Rotation(heading,15);
            boolean landing=PlacementPlanner.hasForwardLanding(feet,motion,layer,.08,heading,true,world);
            var beforeShapes=world.solid.stream().map(pos->net.minecraft.world.phys.shapes.Shapes.block().move(pos.getX(),pos.getY(),pos.getZ())).toList();
            Vec3 forecast=ScaffoldMovement.collisionStep(world.playerBox,motion.add(travel.scale(grounded?.1:.02)),beforeShapes);
            targets=PlacementPlanner.centerTargets(feet,forecast,layer,!grounded,true,.08,world);
            Vec3 followup=new Vec3(forecast.x*(grounded?.546:.91)+travel.x*(grounded?(sprint?.13:.1):.026),
               grounded?0:(forecast.y-.08)*.98,forecast.z*(grounded?.546:.91)+travel.z*(grounded?(sprint?.13:.1):.026));
            var step=pulse.update(targets,eyes,eyes.add(forecast),eyes.add(forecast).add(followup),4.5,yaw,pitch,.5,forward,true,world);
            var look=step.look();
            PlacementPlanner.Placement clicked=null;
            if(step.placement()!=null && world.replaceable(step.placement().target())
               && PlacementPlanner.lookMatchesSupport(step.placement(),eyes,4.5,yaw,pitch,world)) {
               world.solid.add(step.placement().target()); placed++; clicked=step.placement();
               var keys=new Input(true,false,false,false,false,false,sprint);
               {
                  int placedLayer=clicked.target().getY();
                  var corners=PlacementPlanner.centerTargets(feet,motion,layer,!grounded,true,.08,world)
                     .stream().filter(pos->pos.getY()==placedLayer).toList();
                  var connector=PlacementPlanner.findAlongLook(corners,eyes,4.5,yaw,pitch,world);
                  if(connector!=null && PlacementPlanner.lookMatchesSupport(connector,eyes,4.5,yaw,pitch,world)) {
                     world.solid.add(connector.target()); placed++; clicked=connector;
                  }
               }
            }
            if(step.returnAfterAttempt()) look=forward;
            var keys=new Input(true,false,false,false,false,false,sprint);
            var corrected=SilentRotationMath.correctMovement(keys,heading,look.yaw());
            Vec3 input=inputWorld(corrected,look.yaw());
            boolean running=sprint && corrected.forward()>0;
            double acceleration=grounded?(running?.13:.1):(running?.026:.02);
            Vec3 requested=velocity.add(input.scale(acceleration)).add(0,vy,0), resolved=Vec3.ZERO;
            var shapes=world.solid.stream().map(pos->net.minecraft.world.phys.shapes.Shapes.block().move(pos.getX(),pos.getY(),pos.getZ())).toList();
            for(var axis:Direction.axisStepOrder(requested)) resolved=resolved.with(axis,net.minecraft.world.phys.shapes.Shapes.collide(axis,world.playerBox.move(resolved),shapes,requested.get(axis)));
            feet=feet.add(resolved);
            look=pulse.refine(feet.add(0,1.62,0),4.5,look.yaw(),look.pitch(),.5,world);
            if(clicked!=null) {
               float postPitch=PlacementPlanner.postPlacementPitch(clicked,feet.add(0,1.62,0),4.5,yaw,pitch,.5);
               look=new SilentRotationMath.Rotation(look.yaw(),postPitch);
               require(PlacementPlanner.lookIntersectsClickedSupport(clicked,feet.add(0,1.62,0),4.5,yaw,postPitch),
                  "returning forward retains a previous-yaw/current-pitch support ray after native physics: heading="+heading+" sprint="+sprint+" tick="+tick+" clicked="+clicked+" feet="+feet+" oldLook="+yaw+","+pitch+" pitch="+postPitch);
            }
            grounded=requested.y<0 && Math.abs(resolved.y-requested.y)>1e-7;
            velocity=new Vec3(resolved.x,0,resolved.z).scale(grounded?.546:.91);
            vy=((Math.abs(resolved.y-requested.y)>1e-7?0:requested.y)-.08)*.98;
            trace.append("\nt=").append(tick).append(" feet=").append(feet).append(" look=").append(look).append(" step=").append(step).append(" landing=").append(landing);
            require(feet.y>=.999,"Normal One Tick must keep the real walking/sprinting footprint on blocks: heading="+heading+" sprint="+sprint+" placed="+placed+trace);
            yaw=look.yaw(); pitch=look.pitch();
         }
         require(placed>5,"Normal One Tick extends the bridge continuously rather than only passing one airborne click");
      }
   }

   private static void oneTickFutureCollision() {
      FakeWorld world=new FakeWorld(); world.nativeRaycast=world.allowPredictedSupports=true;
      world.solid.add(BlockPos.ZERO);
      Vec3 feet=new Vec3(.5,1.7532,.5), nextFeet=new Vec3(.5,2.001336,.5);
      Vec3 eyes=feet.add(0,1.62,0), nextEyes=nextFeet.add(0,1.62,0);
      world.playerBox=new AABB(.2,feet.y,.2,.8,feet.y+1.8,.8);
      BlockPos upper=new BlockPos(0,1,0);
      var future=PlacementPlanner.withPlayerBox(world,world.playerBox.move(nextFeet.subtract(feet)));
      require(!world.replaceable(upper)&&future.replaceable(upper),"planning may prepare the step only when the predicted feet clear it");
      for(boolean predictionHappens:new boolean[]{false,true}) {
         var pulse=new OneTickBridgeRotation();
         var step=pulse.update(List.of(upper),eyes,nextEyes,nextEyes.add(0,.16,0),4.5,180,82,.5,
            new SilentRotationMath.Rotation(0,15),true,world,180,future,false);
         require(step.aiming()&&step.placement()==null,"future collision planning can prepare a look but never click early");
         AABB oldBox=world.playerBox;
         if(predictionHappens) world.playerBox=oldBox.move(nextFeet.subtract(feet));
         var actualEyes=predictionHappens?nextEyes:eyes;
         var look=pulse.refine(actualEyes,4.5,step.look().yaw(),step.look().pitch(),.5,world);
         var click=pulse.update(List.of(upper),actualEyes,actualEyes,actualEyes,4.5,look.yaw(),look.pitch(),.5,
            new SilentRotationMath.Rotation(0,15),false,world);
         require((click.placement()!=null)==predictionHappens,"a predicted takeoff cannot authorize a placement if the real player still intersects it");
         world.playerBox=oldBox;
      }
   }

   private static void oneTickAirReplay() {
      for (int style=0;style<4;style++) for(float heading:new float[]{0,45,90,135,180,225,270,315}) {
         FakeWorld world=new FakeWorld(); world.nativeRaycast=world.allowPredictedSupports=true;
         for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) world.solid.add(new BlockPos(x,0,z));
         var pulse=new OneTickBridgeRotation(); var telly=new TellyBridge(); var telly2=new Telly2Bridge();
         Vec3 feet=new Vec3(.5,1,.5), travel=Vec3.directionFromRotation(0,heading), velocity=travel.scale(.15);
         float yaw=heading,pitch=15; double vy=-.0784; boolean grounded=true;
         int placed=0, jumps=0, landings=0;
         StringBuilder trace=new StringBuilder();
         for(int tick=0;tick<125;tick++) {
            world.playerBox=new AABB(feet.x-.3,feet.y,feet.z-.3,feet.x+.3,feet.y+1.8,feet.z+.3);
            Vec3 eyes=feet.add(0,1.62,0), motion=velocity.add(0,vy,0);
            int layer=PlacementPlanner.blockLayer(feet.y);
            boolean climb=style>=2;
            int landingLayer=style==1?telly.placementLayer(layer,false):layer;
            boolean landing=PlacementPlanner.hasForwardLanding(feet,motion,landingLayer,.08,heading,true,world);
            var bridge=style==3?telly2.update(grounded,vy,layer,true,landing)
               :telly.update(grounded,vy,layer,true,Math.abs(net.minecraft.util.Mth.wrapDegrees(heading-yaw))<1,true,climb,landing);
            int targetLayer=style==0?layer:bridge.layer();
            boolean jump=grounded&&(style==0||bridge.jump());
            var shapes=world.solid.stream().map(pos->net.minecraft.world.phys.shapes.Shapes.block().move(pos.getX(),pos.getY(),pos.getZ())).toList();
            Vec3 preview=motion.add(travel.scale(grounded?.1:.02));
            if(jump) preview=preview.with(Direction.Axis.Y,.42);
            Vec3 forecast=ScaffoldMovement.collisionStep(world.playerBox,preview,shapes);
            Vec3 follow=new Vec3(forecast.x*(grounded&&!jump?.546:.91)+travel.x*(grounded&&!jump?.13:.026),
               grounded&&!jump?0:(forecast.y-.08)*.98,forecast.z*(grounded&&!jump?.546:.91)+travel.z*(grounded&&!jump?.13:.026));
            Vec3 aimFeet=climb?feet.add(forecast):feet;
            var aimWorld=climb?PlacementPlanner.withPlayerBox(world,world.playerBox.move(forecast)):world;
            int aimLayer=climb?PlacementPlanner.blockLayer(aimFeet.y):targetLayer;
            var targets=PlacementPlanner.centerTargets(aimFeet,climb?follow:forecast,aimLayer,!grounded,true,.08,aimWorld);
            if(climb) {
               var connectors=new java.util.LinkedHashSet<>(targets);
               connectors.addAll(PlacementPlanner.centerTargets(feet,forecast,targetLayer,!grounded,true,.08,aimWorld));
               targets=new java.util.ArrayList<>(connectors);
               targets.sort(java.util.Comparator.<BlockPos>comparingInt(BlockPos::getY).reversed());
            }
            var forward=new SilentRotationMath.Rotation(heading,15);
            var step=pulse.update(targets,eyes,eyes.add(forecast),eyes.add(forecast).add(follow),4.5,yaw,pitch,.5,forward,
               style==0||!jump&&!landing,world,style==0?180:TellyBridge.BACKWARD_OFFSET,aimWorld,!climb);
            var look=step.look(); PlacementPlanner.Placement clicked=null;
            if(step.placement()!=null&&PlacementPlanner.lookMatchesSupport(step.placement(),eyes,4.5,yaw,pitch,world)) {
               clicked=step.placement(); world.solid.add(clicked.target()); placed++;
               var connectors=PlacementPlanner.centerTargets(feet,motion,targetLayer,!grounded,true,.08,world)
                  .stream().filter(pos->pos.getY()==step.placement().target().getY()).toList();
               var corner=PlacementPlanner.findAlongLook(connectors,eyes,4.5,yaw,pitch,world);
               if(corner!=null&&!climb) { world.solid.add(corner.target()); clicked=corner; placed++; }
            }
            boolean actualAim=step.aiming()&&!step.returnAfterAttempt();
            if(step.returnAfterAttempt()) {
               look=forward;
               if(clicked!=null&&(style==0||!jump)&&!PlacementPlanner.hasForwardLanding(feet,motion,targetLayer,.08,heading,true,world)) {
                  targets=PlacementPlanner.centerTargets(aimFeet,climb?follow:forecast,aimLayer,!grounded,true,.08,aimWorld);
                  if(climb) {
                     var connectors=new java.util.LinkedHashSet<>(targets);
                     connectors.addAll(PlacementPlanner.centerTargets(feet,forecast,targetLayer,!grounded,true,.08,aimWorld));
                     targets=new java.util.ArrayList<>(connectors);
                     targets.sort(java.util.Comparator.<BlockPos>comparingInt(BlockPos::getY).reversed());
                  }
                  var next=pulse.update(targets,eyes,eyes.add(forecast),eyes.add(forecast).add(follow),4.5,yaw,pitch,.5,forward,
                     true,world,style==0?180:TellyBridge.BACKWARD_OFFSET,aimWorld,!climb);
                  look=next.look(); actualAim=next.aiming();
               }
            }
            var corrected=SilentRotationMath.correctMovement(new Input(true,false,false,false,jump,false,true),heading,look.yaw());
            Vec3 input=inputWorld(corrected,look.yaw());
            boolean running=corrected.forward()>0&&(style==0||!actualAim);
            Vec3 requested=velocity.add(input.scale(grounded?(running?.13:.1):(running?.026:.02))).add(0,vy,0);
            if(jump) {
               requested=requested.with(Direction.Axis.Y,.42);
               if(running) requested=requested.add(Vec3.directionFromRotation(0,look.yaw()).scale(.2));
               jumps++;
            }
            shapes=world.solid.stream().map(pos->net.minecraft.world.phys.shapes.Shapes.block().move(pos.getX(),pos.getY(),pos.getZ())).toList();
            Vec3 resolved=ScaffoldMovement.collisionStep(world.playerBox,requested,shapes);
            feet=feet.add(resolved);
            boolean wasGrounded=grounded;
            grounded=requested.y<0&&Math.abs(resolved.y-requested.y)>1e-7;
            if(grounded&&!wasGrounded) landings++;
            velocity=new Vec3(resolved.x,0,resolved.z).scale(grounded?.546:.91);
            vy=((Math.abs(resolved.y-requested.y)>1e-7?0:requested.y)-.08)*.98;
            world.playerBox=new AABB(feet.x-.3,feet.y,feet.z-.3,feet.x+.3,feet.y+1.8,feet.z+.3);
            look=pulse.refine(feet.add(0,1.62,0),4.5,look.yaw(),look.pitch(),.5,world);
            // Native 1.9+ retains the complete preceding yaw/pitch pair.
            // Its next prepared pitch must not be overwritten by the 1.8 path.
            trace.append("\nt=").append(tick).append(" feet=").append(feet).append(" look=").append(look).append(" step=").append(step).append(" landing=").append(landing).append(" jump=").append(jump);
            require(feet.y>=.99,"One Tick native sprint jumps stay supported: style="+style+" heading="+heading+" placed="+placed+trace);
            yaw=look.yaw(); pitch=look.pitch();
         }
         require(jumps>=5&&landings>=4&&placed>=10,"One Tick continuously places through repeated native jumps: style="+style+" heading="+heading+" jumps="+jumps+" landings="+landings+" placed="+placed);
         if(style>=2) require(feet.y>=4,"held Space/Telly2 must keep climbing with a single placement per tick");
      }
   }

   private static void legitBlockClicks() {
      for(int cps=5;cps<=20;cps++) {
         var clicks=new NativeClickPulse(); int total=0;
         for(int tick=0;tick<200;tick++) {
            long now=1_000_000_000L+tick*50_000_000L;
            if(clicks.consume(true,cps,now)) total++;
            require(!clicks.consume(true,cps,now),"a consumeClick loop cannot turn one scheduled click into a burst");
         }
         require(total==cps*10,"native input polling preserves the chosen 5-20 CPS average: cps="+cps+" actual="+total);
         require(!clicks.consume(false,cps,20_000_000_000L),"no target, a screen, Scaffold, or disabling stops simulated input");
         require(clicks.consume(true,cps,21_000_000_000L),"a new fight starts without a stale click backlog");
         require(clicks.consume(true,cps,90_000_000_000L)&&!clicks.consume(true,cps,90_000_000_000L),"a long client stall emits at most one click");
      }
   }

   private static void knockbackSafeAim() {
      for (float yaw : new float[]{-540, -180, -45, 0, 45, 90, 180, 540})
      for (double distance : new double[]{1, 3, 6}) for (double width : new double[]{.3, .6, 1.4})
      for (var direction : KnockbackDisplacement.Direction.values()) {
         Vec3 axis = Vec3.directionFromRotation(0, yaw);
         Vec3 center = new Vec3(axis.x * distance, 1.62, axis.z * distance);
         Vec3 eyes = new Vec3(0, 1.62, 0);
         AABB box = new AABB(center.x-width/2,0,center.z-width/2,center.x+width/2,1.8,center.z+width/2);
         Vec3 aim = KnockbackDisplacement.safeAimPoint(box, eyes, center, direction);
         require(box.deflate(.01).contains(aim) && aim.y == center.y, "Grim displacement keeps head aim strictly inside the real target");
         double cross = axis.x * (aim.z-center.z) - axis.z * (aim.x-center.x);
         require(direction == KnockbackDisplacement.Direction.LEFT ? cross < 0 : cross > 0, "the conservative aim retains the selected left/right bias");
         for (double sensitivity : new double[]{0, .5, 1}) {
            Vec3 delta = aim.subtract(eyes);
            var look = SilentRotationMath.quantizeRotation(yaw, 0, (float)Math.toDegrees(Math.atan2(delta.z, delta.x))-90,
               (float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x,delta.z))), sensitivity);
            require(box.clip(eyes, eyes.add(Vec3.directionFromRotation(look.pitch(),look.yaw()).scale(distance+2))).isPresent(),
               "native quantized displacement ray stays on-target instead of making a 90-degree miss");
         }
      }
   }

   private static void knockbackAimStrength() {
      for (double distance:new double[]{1,3}) {
         AABB box=new AABB(-.3,0,distance-.3,.3,1.8,distance+.3);
         Vec3 eyes=new Vec3(0,1.62,0), head=new Vec3(0,1.62,distance);
         Vec3 right=KnockbackDisplacement.safeAimPoint(box,eyes,head,KnockbackDisplacement.Direction.RIGHT);
         Vec3 left=KnockbackDisplacement.safeAimPoint(box,eyes,head,KnockbackDisplacement.Direction.LEFT);
         double side=Math.abs(right.x)/Math.hypot(right.x,right.z);
         double previousSide=.21/Math.hypot(.21,distance);
         require(side>previousSide*1.3,"edge aim gives a measurably stronger sideways component than the old center offset");
         require(Math.abs(left.x+right.x)<1e-8&&Math.abs(left.z-right.z)<1e-8,"left and right have equal physical strength");
         require(side<1,"on-target aiming does not pretend to provide a full 90-degree sideways hit at normal reach");
         for(Vec3 point:KnockbackDisplacement.safeAimPoints(box,eyes,head,KnockbackDisplacement.Direction.RIGHT))
            require(point.x<=0,"a blocked-side fallback cannot silently reverse the selected knockback direction");
      }
   }

   private static void allDirectionClutch() {
      for (int direction = 0; direction < 16; direction++) {
         float angle = direction * 22.5F;
         // Use explicit integer 45-degree directions plus halfway headings.
         Vec3 travel = Vec3.directionFromRotation(0, angle);
         for (float camera : new float[]{0, 90, 180, 270}) for (double fall : new double[]{-.08, -.24})
         for (double sensitivity : new double[]{0, .5, 1}) {
            FakeWorld world = new FakeWorld(); world.nativeRaycast = true;
            world.solid.add(BlockPos.ZERO);
            Vec3 feet = new Vec3(.5 + travel.x * 1.1, 1.2, .5 + travel.z * 1.1);
            Vec3 motion = new Vec3(travel.x * .08, fall, travel.z * .08);
            world.playerBox = new AABB(feet.x - .3, feet.y, feet.z - .3, feet.x + .3, feet.y + 1.8, feet.z + .3);
            Vec3 eyes = feet.add(0, 1.62, 0), nextEyes = eyes.add(motion);
            var targets = PlacementPlanner.catchTargets(feet, motion, 0, true, true, .08);
            var aim = PlacementPlanner.clutchAim(targets, eyes, nextEyes, 4.5, camera, 15, sensitivity, world, null);
            require(aim != null, "reachable clutch from heading=" + angle + " camera=" + camera + " fall=" + fall + " sensitivity=" + sensitivity);
            Vec3 ray = Vec3.directionFromRotation(aim.rotation().pitch(), aim.rotation().yaw()).scale(4.5);
            require(PlacementPlanner.matchesSupport(aim.placement(), world.raycast(eyes, eyes.add(ray)))
               && PlacementPlanner.matchesSupport(aim.placement(), world.raycast(nextEyes, nextEyes.add(ray))),
               "all-direction clutches keep the same real support face before and after physics");
            require(world.replaceable(aim.placement().target()) && targets.contains(aim.placement().target()),
               "clutch stays outside the player and inside the predicted landing path");
         }
      }
      FakeWorld empty = new FakeWorld();
      require(PlacementPlanner.clutchAim(List.of(BlockPos.ZERO), new Vec3(.5, 3, .5), new Vec3(.5, 2.9, .5),
         4.5, 0, 80, .5, empty, null) == null, "no support means no fabricated air placement");
      // Replay the real prepare-look -> native movement -> click boundary at
      // sprint-like speeds, rather than validating only an isolated aim ray.
      for (int direction = 0; direction < 16; direction++) for (double speed : new double[]{.26, .4}) {
         Vec3 travel = Vec3.directionFromRotation(0, direction * 22.5F);
         FakeWorld world = new FakeWorld(); world.nativeRaycast = true; world.solid.add(BlockPos.ZERO);
         Vec3 feet = new Vec3(.5 + travel.x * 1.05, 1.65, .5 + travel.z * 1.05);
         Vec3 motion = new Vec3(travel.x * speed, -.16, travel.z * speed);
         world.playerBox = new AABB(feet.x - .3, feet.y, feet.z - .3, feet.x + .3, feet.y + 1.8, feet.z + .3);
         Vec3 eyes = feet.add(0, 1.62, 0);
         var targets = PlacementPlanner.catchTargets(feet, motion, 0, true, true, .08);
         var step = PlacementPlanner.clutchStep(targets, eyes, eyes.add(motion), 4.5, 0, -70, .5, true, world, null);
         require(step != null, "moving recovery prepares a reachable look: heading=" + direction + " speed=" + speed);
         feet = feet.add(motion); eyes = eyes.add(motion);
         world.playerBox = new AABB(feet.x - .3, feet.y, feet.z - .3, feet.x + .3, feet.y + 1.8, feet.z + .3);
         motion = new Vec3(motion.x * .91, (motion.y - .08) * .98, motion.z * .91);
         targets = PlacementPlanner.catchTargets(feet, motion, 0, true, true, .08);
         var click = PlacementPlanner.clutchStep(targets, eyes, eyes.add(motion), 4.5,
            step.rotation().yaw(), step.rotation().pitch(), .5, true, world, step.placement());
         require(click != null && click.ready(), "the transmitted catch is consumed before turning back to the mode's normal heading");
         require(click.rotation().equals(step.rotation()), "the recovery click keeps its actual transmitted yaw/pitch");
         world.solid.add(click.placement().target());
         require(world.supportsLanding(click.placement().target()), "the recovery click creates a real collision block");
      }
   }

   private static void clutchLandingReplay() {
      for (int heading = 0; heading < 16; heading++) for (double speed : new double[]{.26, .4}) {
         Vec3 travel = Vec3.directionFromRotation(0, heading * 22.5F);
         FakeWorld world = new FakeWorld(); world.nativeRaycast = true;
         world.solid.add(BlockPos.ZERO);
         Vec3 feet = new Vec3(.5 + travel.x * 1.05, 2.4, .5 + travel.z * 1.05);
         Vec3 motion = new Vec3(travel.x * speed, -.08, travel.z * speed);
         float yaw = 0, pitch = -70;
         PlacementPlanner.Placement preferred = null;
         boolean landed = false;
         int placed = 0;
         StringBuilder trace = new StringBuilder();
         for (int tick = 0; tick < 12 && !landed; tick++) {
            world.playerBox = new AABB(feet.x - .3, feet.y, feet.z - .3, feet.x + .3, feet.y + 1.8, feet.z + .3);
            int layer = PlacementPlanner.blockLayer(feet.y);
            var targets = PlacementPlanner.catchTargets(feet, motion, layer, true, true, .08);
            Vec3 eyes = feet.add(0, 1.62, 0);
            var step = PlacementPlanner.clutchStep(targets, eyes, eyes.add(motion), 4.5, yaw, pitch, .5, true, world, preferred);
            if (step != null) {
               yaw = step.rotation().yaw(); pitch = step.rotation().pitch(); preferred = step.placement();
               if (step.ready()) {
                  world.solid.add(step.placement().target()); placed++;
                  // Match the module's optional second connector click with
                  // the same transmitted ray and updated native prediction.
                  int catchLayer = step.placement().target().getY();
                  var extra = PlacementPlanner.findAlongLook(targets.stream().filter(pos -> pos.getY() == catchLayer).toList(), eyes, 4.5, yaw, pitch, world);
                  if (extra != null) { world.solid.add(extra.target()); placed++; }
                  var next = PlacementPlanner.clutchStep(targets, eyes, eyes.add(motion), 4.5, yaw, pitch, .5, false, world, null);
                  if (next != null) { yaw = next.rotation().yaw(); pitch = next.rotation().pitch(); preferred = next.placement(); }
               }
            }
            trace.append("\ntick=").append(tick).append(" feet=").append(feet).append(" step=").append(step).append(" blocks=").append(world.solid);
            var shapes = world.solid.stream().map(block -> net.minecraft.world.phys.shapes.Shapes.block()
               .move(block.getX(), block.getY(), block.getZ())).toList();
            Vec3 resolved = Vec3.ZERO;
            for (Direction.Axis axis : Direction.axisStepOrder(motion)) {
               if (motion.get(axis) != 0) resolved = resolved.with(axis, net.minecraft.world.phys.shapes.Shapes.collide(
                  axis, world.playerBox.move(resolved), shapes, motion.get(axis)));
            }
            landed = motion.y < 0 && Math.abs(resolved.y - motion.y) > 1e-7;
            feet = feet.add(resolved);
            motion = new Vec3(motion.x * .91, (motion.y - .08) * .98, motion.z * .91);
         }
         require(placed > 0 && landed, "moving catch lands on native block collisions: heading=" + heading + " speed=" + speed + " feet=" + feet + trace);
      }
   }
   private static void backtrackPacketHooks() {
      // Required mixins can prevent startup if Minecraft changes a handler.
      // Verify every selected thread-handoff instruction against the shipped
      // Minecraft bytecode without starting or closing the user's game.
      try (var stream = ScaffoldRegressionChecks.class.getResourceAsStream("/net/minecraft/client/multiplayer/ClientPacketListener.class")) {
         var model = java.lang.classfile.ClassFile.of().parse(stream.readAllBytes());
         for (String name : List.of("handleMoveEntity", "handleTeleportEntity", "handleEntityPositionSync", "handleMovePlayer",
            "handleRespawn", "handleExplosion", "handleSetEntityMotion", "handleRemoveEntities")) {
            var methods = model.methods().stream().filter(method -> method.methodName().equalsString(name)).toList();
            require(methods.size() == 1, "Backtrack hook resolves exactly one Minecraft handler: " + name);
            long calls = methods.getFirst().code().orElseThrow().elementStream()
               .filter(element -> element instanceof java.lang.classfile.instruction.InvokeInstruction invoke
                  && invoke.owner().asInternalName().equals("net/minecraft/network/protocol/PacketUtils")
                  && invoke.name().equalsString("ensureRunningOnSameThread")
                  && invoke.type().stringValue().equals("(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V")).count();
            require(calls == 1, "Backtrack handler has the exact thread handoff required by its mixin: " + name);
         }
      } catch (java.io.IOException failure) { throw new AssertionError(failure); }
   }

   private static void rodCombatCycle() {
      RodLogic.Cycle cycle = new RodLogic.Cycle();
      require(cycle.ready(0) && !cycle.active(), "idle rod leaves melee alone");
      cycle.begin(42, 10);
      require(cycle.active() && !cycle.blocksMelee(), "lining up a rod never owns the weapon or blocks sword combat");
      cycle.begin(99, 11);
      require(cycle.target() == 42, "aim belongs to one selected target");
      cycle.update(11, false, false, true, 4);
      require(!cycle.active(), "a target entering melee cancels the pending rod aim immediately");
      cycle.reset(); cycle.begin(42, 10); cycle.cast(11, 5, 2);
      require(cycle.blocksMelee(), "accepted cast keeps the rod alive during its short flight");
      cycle.update(12, true, false, false, 4);
      require(cycle.phase() == RodLogic.Phase.FLYING, "do not cancel a hook before it can reach the target");
      cycle.update(13, true, false, true, 4);
      require(cycle.phase() == RodLogic.Phase.RETRIEVING, "close range preempts flight so sword restoration happens before combat");
      cycle.finish(13, 7);
      require(!cycle.blocksMelee() && !cycle.ready(19) && cycle.ready(20), "restoration releases combat in the same tick and delays only recasting");
      cycle.reset(); cycle.begin(1, 0); cycle.cast(1, 5, 2);
      cycle.update(2, true, true, false, 4);
      require(cycle.phase() == RodLogic.Phase.RETRIEVING, "hook contact returns the sword immediately");
      cycle.reset(); cycle.begin(1, 0); cycle.cast(1, 5, 2);
      cycle.update(2, true, false, false, 4); cycle.update(3, false, false, false, 4);
      require(cycle.phase() == RodLogic.Phase.RETRIEVING, "a hook that disappears does not consume its entire timeout");
      cycle.reset(); cycle.begin(1, 0); cycle.cast(1, 5, 2);
      cycle.update(4, false, false, false, 4);
      require(cycle.phase() == RodLogic.Phase.FLYING, "give a server hook time to spawn");
      cycle.update(5, false, false, false, 4);
      require(cycle.phase() == RodLogic.Phase.RETRIEVING, "rejected or missing cast releases the sword promptly");
      cycle.reset(); cycle.begin(1, 0); cycle.cast(1, 5, 2);
      cycle.update(7, true, false, false, 4);
      require(cycle.phase() == RodLogic.Phase.FLYING, "flight deadline includes bounded network grace");
      cycle.update(8, true, false, false, 4);
      require(cycle.phase() == RodLogic.Phase.RETRIEVING, "a missed hook expires at its predicted flight deadline");
      cycle.reset(); cycle.begin(1, 0); cycle.requestReturn(1);
      require(!cycle.active(), "losing the target during aim cannot reserve melee");
      cycle.reset(); cycle.begin(1, 0); cycle.cast(1, 5, 2); cycle.requestReturn(2);
      require(cycle.phase() == RodLogic.Phase.RETRIEVING, "target switch retrieves the old hook rather than silently retargeting it");
      cycle.reset(); cycle.begin(1, 0); cycle.update(6, false, false, false, 4);
      require(cycle.active() && !cycle.blocksMelee(), "slow rotations have time to align without reserving the weapon");
      cycle.update(39, false, false, false, 4);
      require(cycle.active(), "acquisition remains available while a slow turn finishes");
      cycle.update(40, false, false, false, 4);
      require(!cycle.active(), "an impossible cast eventually retries without switching weapons");
      for (int original = 0; original < 9; original++) for (int rod = 0; rod < 9; rod++) {
         require(RodLogic.restoreSlot(rod, rod, original, true, true) == original, "restore the exact previous slot");
         require(RodLogic.restoreSlot((rod + 1) % 9, rod, original, true, true) == -1, "manual hotbar input wins");
         require(RodLogic.restoreSlot(rod, rod, original, false, true) == -1, "a replaced rod stack is not owned");
         require(RodLogic.restoreSlot(rod, rod, original, true, false) == -1, "old-world ownership cannot restore another player's slot");
      }
      Vec3 eyes = new Vec3(0, 1.62, 0);
      // Independent first/second-tick fixtures from native spawn and flight order.
      AABB distant = new AABB(100, 100, 100, 101, 101, 101);
      java.util.List<Vec3> starts = new java.util.ArrayList<>(), ends = new java.util.ArrayList<>();
      RodLogic.intercept(eyes, 0, 0, distant, Vec3.ZERO, RodLogic.Flight.LEGACY,
         (a, b) -> { starts.add(a); ends.add(b); return true; });
      require(ends.get(0).distanceTo(new Vec3(-0.16, 1.52, 0)) < 1.0E-7, "1.8 spawns beside the player and below the eyes");
      require(ends.get(1).distanceTo(new Vec3(-0.16, 1.52, 1.5)) < 1.0E-7, "1.8 first collision segment uses 1.5 speed without gravity yet");
      require(ends.get(2).distanceTo(new Vec3(-0.16, 1.4832, 2.88)) < 1.0E-7, "1.8 applies 0.04 gravity and 0.92 drag after movement");
      starts.clear(); ends.clear();
      RodLogic.intercept(eyes, 0, 0, distant, Vec3.ZERO, RodLogic.Flight.MODERN,
         (a, b) -> { starts.add(a); ends.add(b); return true; });
      require(ends.get(0).distanceTo(new Vec3(0, 1.62, 0.3)) < 1.0E-7, "native hook spawns ahead of the eyes");
      require(ends.get(1).distanceTo(new Vec3(0, 1.62, 1.4)) < 1.0E-7, "native collision uses velocity before gravity");
      require(starts.get(2).distanceTo(new Vec3(0, 1.59, 1.4)) < 1.0E-7, "native movement applies gravity before the next collision segment");
      require(ends.get(2).distanceTo(new Vec3(0, 1.5624, 2.412)) < 1.0E-7, "native second collision includes drag on previous gravity");
      for (RodLogic.Flight flight : RodLogic.Flight.values())
      for (double distance : flight == RodLogic.Flight.LEGACY ? new double[]{3.5, 4, 5, 6, 7, 8, 10}
         : new double[]{3.5, 4, 5, 6, 7}) for (int yaw = 0; yaw < 360; yaw += 45)
      for (double strafe : new double[]{-0.2, 0, 0.2}) for (double away : new double[]{0, 0.15})
      for (double rise : new double[]{-0.08, 0, 0.08}) {
         double angle = Math.toRadians(yaw);
         Vec3 forward = new Vec3(-Math.sin(angle), 0, Math.cos(angle));
         Vec3 side = new Vec3(Math.cos(angle), 0, Math.sin(angle));
         Vec3 position = forward.scale(distance);
         AABB target = new AABB(position.x - 0.3, 0, position.z - 0.3, position.x + 0.3, 1.8, position.z + 0.3);
         Vec3 motion = side.scale(strafe).add(forward.scale(away)).add(0, rise, 0);
         RodLogic.Shot shot = RodLogic.solve(eyes, target, motion, true, flight);
         int hit = RodLogic.intercept(eyes, shot.yaw(), shot.pitch(), target, motion, flight, (a, b) -> true);
         require(hit > 0 && hit <= 18, "predicted hook intersects moving hitbox in native flight replay: " + flight + "/" + distance + "/" + yaw + "/" + motion);
         require(RodLogic.intercept(eyes, shot.yaw(), shot.pitch(), target, motion, flight, (a, b) -> false) == -1,
            "obstructed hook path never casts even if the final look is aligned");
         require(RodLogic.intercept(eyes, shot.yaw() + 90, shot.pitch(), target, motion, flight, (a, b) -> true) == -1,
            "an unrelated transmitted rotation cannot trigger a cast");
      }
      AABB target = new AABB(-0.3, 0, 5.7, 0.3, 1.8, 6.3);
      AABB escaping = target.move(0, 0, 4);
      Vec3 escapeMotion = new Vec3(0.45, 0.3, 0.45);
      RodLogic.Shot unreachable = RodLogic.solve(eyes, escaping, escapeMotion, true);
      require(RodLogic.intercept(eyes, unreachable.yaw(), unreachable.pitch(), escaping, escapeMotion,
         (a, b) -> true) == -1, "a distant target outrunning native hook flight cannot trigger a doomed cast");
      RodLogic.Shot stationary = RodLogic.solve(eyes, target, Vec3.ZERO, true);
      RodLogic.Shot disabled = RodLogic.solve(eyes, target, new Vec3(0.4, 0.2, 0), false);
      require(stationary.equals(disabled), "prediction toggle removes leading but retains native gravity compensation");
      require(Double.isFinite(RodLogic.solve(eyes, target, new Vec3(Double.NaN, 0, 0), true).point().lengthSqr()), "invalid motion never poisons the shared aim");
   }
   private static void scaffoldMovementFixToggle() {
      for (int mask = 0; mask < 16; mask++) {
         Input raw = new Input((mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0, (mask & 8) != 0, false, false, false);
         for (int controls = 0; controls < 8; controls++) {
            Input changed = new Input(!raw.forward(), !raw.backward(), !raw.left(), !raw.right(),
               (controls & 1) != 0, (controls & 2) != 0, (controls & 4) != 0);
            Input restored = ScaffoldMovement.rawDirections(raw, changed);
            require(restored.forward() == raw.forward() && restored.backward() == raw.backward()
               && restored.left() == raw.left() && restored.right() == raw.right(), "OFF retains all physical WASD combinations after another writer");
            require(restored.jump() == changed.jump() && restored.shift() == changed.shift() && restored.sprint() == changed.sprint(),
               "raw direction restoration preserves unrelated native control bits");
         }
         for (float camera : new float[]{-720, -179, -45, 0, 90, 359, 720}) {
            Input off = ScaffoldMovement.rotationKeys(raw, false);
            require(TellyBridge.forwardYaw(off, camera) == camera, "OFF cannot steer the forward look to compensate for a held strafe key");
            require(ScaffoldMovement.rotationKeys(raw, true) == raw, "ON retains directional bridge heading selection");
            var bridge = new BackwardBridgeRotation();
            float expected = Math.round(camera / 45.0F) * 45.0F + 180.5F;
            require(bridge.heading(off, camera) == expected, "OFF keeps one backward heading for W, A, S, D and diagonals");
         }
      }
      Input w = new Input(true, false, false, false, false, false, false);
      var rawForward = SilentRotationMath.correctMovement(w, 0, 0);
      Vec3 uncorrectedWorld = inputWorld(rawForward, 180);
      Vec3 correctedWorld = inputWorld(SilentRotationMath.correctMovement(w, 0, 180), 180);
      require(uncorrectedWorld.z < -0.99 && correctedWorld.z > 0.99,
         "W while facing backward accelerates backward with OFF; ON alone preserves camera-forward travel");
   }

   private static void andromidaBridge() {
      for (float heading : new float[]{0, 45, 90, 135, 180, 225, 270, 315}) {
         Vec3 forward = Vec3.directionFromRotation(0, heading);
         FakeWorld world = new FakeWorld(); world.nativeRaycast = true;
         for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) world.solid.add(new BlockPos(x, 0, z));
         AndromidaBridge bridge = new AndromidaBridge();
         Vec3 feet = new Vec3(0.5, 1, 0.5);
         float yaw = heading, pitch = 15;
         boolean roofBuilt = false, startupPlaced = false, jumped = false;
         int takeoff = -100;
         for (int tick = 0; tick < 36 && !roofBuilt; tick++) {
            // A native takeoff raises eyes above the startup pillar's top.
            Vec3 pose = tick - takeoff <= 3 ? feet.add(0, 0.42, 0) : feet;
            world.playerBox = new AABB(pose.x-.3, pose.y, pose.z-.3, pose.x+.3, pose.y+1.8, pose.z+.3);
            var step = bridge.plan(pose, pose.add(0, 1.62, 0), Vec3.ZERO, heading, 4.5, yaw, pitch, 0.5, true, world);
            yaw = step.look().yaw(); pitch = step.look().pitch();
            if (step.ready()) {
               require(step.placement() != null && PlacementPlanner.lookMatchesSupport(step.placement(), pose.add(0, 1.62, 0),
                  4.5, yaw, pitch, world), "Andromida places only against the actual transmitted voxel face");
               require(!world.playerBox.intersects(new AABB(step.placement().target())), "startup/roof blocks never intersect the player");
               require(world.solid.add(step.placement().target()), "one accepted placement never duplicates an existing block");
               startupPlaced |= step.phase() == AndromidaBridge.Phase.STARTUP;
               bridge.placed(); yaw = heading; pitch = 15;
            }
            boolean jumpNow = bridge.jump(tick, !jumped, true, Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw-heading))<1,
               true, pose, heading, world);
            if (jumpNow) { jumped = true; takeoff = tick; }
            roofBuilt = world.solid.contains(new BlockPos(0, 3, 0));
         }
         require(startupPlaced && jumped && roofBuilt, "Andromida bootstraps a real ceiling using a pillar and native takeoff: " + heading);

         // Extend the roof after moving beyond its exposed edge, while floor
         // extension is out of view until the next edge is reached.
         feet = new Vec3(.5, 1, .5).add(forward.scale(1.05));
         BlockPos roof = BlockPos.containing(feet).atY(3);
         world.playerBox = new AABB(feet.x-.3, feet.y, feet.z-.3, feet.x+.3, feet.y+1.8, feet.z+.3);
         boolean extended = false;
         for (int tick = 40; tick < 50 && !extended; tick++) {
            var step = bridge.plan(feet, feet.add(0,1.62,0), Vec3.ZERO, heading, 4.5, yaw, pitch, .5, true, world);
            yaw = step.look().yaw(); pitch = step.look().pitch();
            if (step.ready()) { world.solid.add(step.placement().target()); bridge.placed(); }
            extended = world.solid.contains(roof);
         }
         require(extended, "Andromida extends the overhead row in every movement direction: " + heading);
         require(!bridge.jump(60, false, true, true, false, feet, heading, world), "airborne Andromida cannot inject another jump");
         require(!bridge.jump(61, true, false, true, false, feet, heading, world), "stationary Andromida does not jump");
         require(!bridge.jump(62, true, true, false, false, feet, heading, world), "finish turning forward before a sprint takeoff");
         require(bridge.jump(63, true, true, true, false, feet, heading, world), "a supported floor and low ceiling allow the native head-hitter jump");
         require(!bridge.jump(64, true, true, true, false, feet, heading, world), "release jump between short ceiling landings");
         world.solid.remove(BlockPos.containing(feet.add(forward.scale(.9))).atY(0));
         require(!bridge.jump(66, true, true, true, false, feet, heading, world), "missing forward support prevents an automatic leap");
         world.solid.add(BlockPos.containing(feet.add(forward.scale(.9))).atY(0));
         world.solid.remove(BlockPos.containing(feet).atY(3));
         world.solid.remove(BlockPos.containing(feet.subtract(forward.scale(.29))).atY(3));
         require(!bridge.jump(67, true, true, true, true, feet, heading, world), "a lost ceiling cannot re-arm startup jumps after bridging begins");

         // At the platform lip, a real side face becomes visible below the
         // overhanging feet. Floor recovery must preempt ceiling construction.
         feet = new Vec3(.5, 1, .5).add(forward.scale(2.55 / Math.max(Math.abs(forward.x), Math.abs(forward.z))));
         world.playerBox = new AABB(feet.x-.3, feet.y, feet.z-.3, feet.x+.3, feet.y+1.8, feet.z+.3);
         boolean floorExtended = false;
         for (int tick = 70; tick < 80 && !floorExtended; tick++) {
            var step = bridge.plan(feet, feet.add(0,1.62,0), Vec3.ZERO, heading, 4.5, yaw, pitch, .5, true, world);
            yaw = step.look().yaw(); pitch = step.look().pitch();
            if (step.ready()) {
               require(step.phase() == AndromidaBridge.Phase.FLOOR && step.placement().target().getY() == 0,
                  "an exposed floor gap takes priority over roof work");
               require(PlacementPlanner.lookMatchesSupport(step.placement(), feet.add(0,1.62,0), 4.5, yaw, pitch, world),
                  "edge extension uses a visible native support face");
               floorExtended = world.solid.add(step.placement().target()); bridge.placed();
            }
         }
         require(floorExtended, "Andromida extends its floor in every movement direction: " + heading);
         bridge.reset();
         require(bridge.layer(10) == 10, "world/mode reset clears the old ceiling height");
      }
   }

   private static void scaffoldStartupMovement() {
      Input forward = new Input(true, false, false, false, false, false, false);
      for (double sensitivity : new double[]{0, 0.5, 1}) {
         var bridge = new BackwardBridgeRotation();
         float yaw = 0, pitch = 0;
         for (int tick = 0; tick < 25; tick++) {
            var look = bridge.next(forward, 0, yaw, pitch, 82, sensitivity);
            var movement = SilentRotationMath.correctMovement(forward, 0, look.yaw());
            require(movement.forward() != 0 || movement.sideways() != 0,
               "movement remains nonzero throughout the initial backward turn");
            Vec3 direction = inputWorld(movement, look.yaw());
            require(direction.z > 0.92, "startup turn preserves the requested world direction");
            yaw = look.yaw(); pitch = look.pitch();
         }
      }
   }

   private static void manualUseSuppression() {
      int[] queued = {3};
      java.util.function.BooleanSupplier consume = () -> queued[0] > 0 && queued[0]-- > 0;
      require(!ScaffoldUseInput.consumeClick(true, consume) && queued[0] == 0,
         "Scaffold consumes all queued manual use clicks without performing an interaction");
      require(!ScaffoldUseInput.consumeClick(false, consume), "turning Scaffold off cannot replay suppressed clicks");
      queued[0] = 2;
      require(ScaffoldUseInput.consumeClick(false, consume) && queued[0] == 1,
         "ordinary use clicks retain native one-click consumption when Scaffold is disabled");
      require(ScaffoldUseInput.consumeClick(false, consume) && queued[0] == 0,
         "fresh right clicks work after Scaffold is disabled");
   }

   private static void centerRotationGeometry() {
      for (Direction face : Direction.values()) {
      for (double sensitivity : new double[]{0, 0.5, 1}) {
         FakeWorld world = new FakeWorld();
         world.nativeRaycast = true;
         world.allowPredictedSupports = true;
         world.solid.add(BlockPos.ZERO);
         Vec3 outward = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
         Vec3 eyes = Vec3.atCenterOf(BlockPos.ZERO).add(outward.scale(3.2));
         BlockPos target = BlockPos.ZERO.relative(face);
         var aim = PlacementPlanner.centerAim(List.of(target), eyes, 4.5, 723.25F, 11.5F, sensitivity, world);
         require(aim != null, "Center can aim at each exposed placement face: " + face);
         require(aim.placement().target().equals(target)
            && aim.placement().hit().getBlockPos().equals(BlockPos.ZERO)
            && aim.placement().hit().getDirection() == face,
            "Center's actual ray selects the intended support, face and adjacent target");
         Vec3 center = Vec3.atCenterOf(BlockPos.ZERO).add(outward.scale(0.5));
         Vec3 delta = center.subtract(eyes);
         float yaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
         float pitch = (float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
         var expected = SilentRotationMath.quantizeRotation(723.25F, 11.5F, yaw, pitch, sensitivity);
         require(aim.rotation().equals(expected), "Center uses the face center and the transmitted mouse-step baseline");
         require(PlacementPlanner.matchesSupport(aim.placement(), world.raycast(eyes,
            eyes.add(Vec3.directionFromRotation(aim.rotation().pitch(), aim.rotation().yaw()).scale(4.5)))),
            "Center validates the quantized click against native voxel geometry");
         world.confirmations.predict(BlockPos.ZERO, 3);
         require(PlacementPlanner.centerAim(List.of(target), eyes, 4.5, 0, 0, sensitivity, world) != null,
            "Center can extend native local predictions without a round-trip placement delay");
         world.confirmations.confirmBlock(BlockPos.ZERO);
         require(PlacementPlanner.centerAim(List.of(target), eyes, 1, 0, 0, sensitivity, world) == null,
            "Center does not aim beyond interaction reach");
         world.solid.add(BlockPos.ZERO.relative(face, 2));
         require(PlacementPlanner.centerAim(List.of(target), eyes, 4.5, 0, 0, sensitivity, world) == null,
            "Center does not click through an obstruction");
      }
      }
      // The side-face center remains usable just beyond the bridge edge.
      FakeWorld edge = new FakeWorld();
      edge.nativeRaycast = true;
      edge.solid.add(BlockPos.ZERO);
      Vec3 eyes = new Vec3(0.5, 2.62, 1.24);
      var aim = PlacementPlanner.centerAim(List.of(new BlockPos(0, 0, 1)), eyes, 4.5, 180, 82, 0.5, edge);
      require(aim != null && aim.placement().hit().getDirection() == Direction.SOUTH,
         "Center reaches the side-face center during ordinary flat bridging");
      for (float heading : new float[]{0, 45, 90, 135, 180, 225, 270, 315}) {
      for (double speed : new double[]{0.15, 0.30}) {
      for (double sensitivity : new double[]{0, 0.5, 1}) {
         FakeWorld bridge = new FakeWorld();
         bridge.nativeRaycast = true;
         bridge.allowPredictedSupports = true;
         bridge.solid.add(BlockPos.ZERO);
         Vec3 feet = new Vec3(0.5, 1, 0.5), motion = Vec3.directionFromRotation(0, heading).scale(speed);
         float yaw = heading + 180, pitch = 82;
         PlacementPlanner.Placement selected = null;
         int placed = 0;

         for (int tick = 0; tick < 64; tick++) {
            bridge.playerBox = new AABB(feet.x - 0.3, 1, feet.z - 0.3, feet.x + 0.3, 2.8, feet.z + 0.3);
            Vec3 from = feet.add(0, 1.62, 0);
            var targets = PlacementPlanner.centerTargets(feet, motion, 0, false, false, 0.08, bridge);
            var pending = PlacementPlanner.findAlongCenterLook(selected, targets, from, 4.5, yaw, pitch, bridge);
            var nextAim = PlacementPlanner.centerAim(targets, from.add(motion), 4.5, yaw, pitch, sensitivity, bridge, selected, false);
            var look = pending != null || nextAim == null ? new SilentRotationMath.Rotation(yaw, pitch) : nextAim.rotation();
            selected = pending != null ? pending : nextAim == null ? null : nextAim.placement();
            if (pending != null && PlacementPlanner.lookMatchesSupport(pending, from, 4.5, look.yaw(), look.pitch(), bridge)) {
               bridge.solid.add(pending.target()); bridge.confirmations.predict(pending.target(), tick); placed++;
               var next = PlacementPlanner.centerAim(PlacementPlanner.centerTargets(feet, motion, 0, false, false, 0.08, bridge),
                  from.add(motion), 4.5, yaw, pitch, sensitivity, bridge, selected, false);
               if (next != null) { look = next.rotation(); selected = next.placement(); }
            }
            feet = feet.add(motion);
            Vec3 nextEyes = feet.add(0, 1.62, 0);
            var nextTargets = PlacementPlanner.centerTargets(feet, motion, 0, false, false, 0.08, bridge);
            var refined = PlacementPlanner.centerAim(nextTargets, nextEyes, 4.5, look.yaw(), look.pitch(), sensitivity,
               bridge, selected, true);
            if (refined != null) {
               look = new SilentRotationMath.Rotation(look.yaw(), refined.rotation().pitch());
               selected = refined.placement();
            }
            yaw = look.yaw(); pitch = look.pitch();
         }
         require(placed >= 7 && bridge.solid.stream().anyMatch(pos -> Math.hypot(pos.getX(), pos.getZ()) >= 7),
            "Center keeps placing while moving with its selected face: heading=" + heading + " speed=" + speed + " placed=" + placed);
      }
      }
      }
   }

   private static void centerCornerSelection() {
      FakeWorld world = new FakeWorld();
      world.nativeRaycast = true;
      world.allowPredictedSupports = true;
      world.solid.add(BlockPos.ZERO);
      var targets = List.of(BlockPos.ZERO.east(), BlockPos.ZERO.south());
      Vec3 eyes = new Vec3(1.6, 2.62, 1.2);
      var selected = PlacementPlanner.centerAim(targets, eyes, 4.5, 0, 0, 0.5, world);
      require(selected != null && selected.placement().target().equals(BlockPos.ZERO.east()), "select the east face's center");
      boolean reproduced = false;
      for (double x : new double[]{1.2, 1.3, 1.4, 1.5}) {
      for (double z : new double[]{1.3, 1.4, 1.5, 1.6}) {
         Vec3 movedEyes = new Vec3(x, 2.62, z);
         var generic = PlacementPlanner.findAlongLook(targets, movedEyes, 4.5,
            selected.rotation().yaw(), selected.rotation().pitch(), world);
         if (generic == null || generic.target().equals(selected.placement().target())) continue;
         reproduced = true;
         require(PlacementPlanner.findAlongCenterLook(selected.placement(), targets, movedEyes, 4.5,
            selected.rotation().yaw(), selected.rotation().pitch(), world) == null,
            "moving around a corner cannot turn the selected center into a stray neighboring placement");
         var reaim = PlacementPlanner.centerAim(targets, movedEyes, 4.5, selected.rotation().yaw(), selected.rotation().pitch(),
            0.5, world, selected.placement(), false);
         require(reaim != null && reaim.placement().target().equals(selected.placement().target()),
            "re-aim the chosen face instead of switching to the neighboring face");
         require(PlacementPlanner.findAlongCenterLook(reaim.placement(), targets, movedEyes, 4.5,
            reaim.rotation().yaw(), reaim.rotation().pitch(), world) != null, "the corrected center ray can actually place");
      }
      }
      require(reproduced, "reproduce Center's old neighboring-face double placement");
   }
   private static Vec3 inputWorld(SilentRotationMath.Movement movement, float yaw) {
      double radians = Math.toRadians(yaw);
      return new Vec3(movement.normalizedSideways() * Math.cos(radians) - movement.normalizedForward() * Math.sin(radians),
         0, movement.normalizedForward() * Math.cos(radians) + movement.normalizedSideways() * Math.sin(radians));
   }

   private static void knockbackMovementFix() {
      for (float base : new float[]{-540, -90, 0, 90, 540}) {
      for (int forward = -1; forward <= 1; forward++) {
      for (int sideways = -1; sideways <= 1; sideways++) {
      for (var direction : KnockbackDisplacement.Direction.values()) {
         Input keys = new Input(forward > 0, forward < 0, sideways > 0, sideways < 0, true, false, true);
         var expected = inputWorld(SilentRotationMath.correctMovement(keys, base, base), base);
         var original = new KnockbackDisplacement.Look(base, 30);
         KnockbackDisplacement.attack(original, direction, look -> {
            var corrected = look.correctMovement(keys, base);
            require(expected.distanceTo(inputWorld(corrected, look.yaw())) < 0.00001,
               "left/right attack look preserves movement from the already corrected input basis");
            for (boolean normalized : new boolean[]{false, true}) {
               var vector = corrected.inputVector(normalized);
               require(Math.abs(vector.x) <= 1 && Math.abs(vector.y) <= 1,
                  "displacement correction retains legal legacy and modern movement input");
            }
         }, () -> { });
      }
      }
      }
      }
   }

   private static void tellyForwardRecovery() {
      var bridge = new TellyBridge();
      bridge.update(true, 0, 0, true, false, true, false, false);
      for (int tick = 0; tick < 3; tick++) bridge.update(false, .2, 0, true, false, true, false, false);
      var recovered = bridge.recoverForward(0, false);
      require(recovered != null && recovered.lookForward() && !recovered.jump(), "a completed flat catch returns forward before physics without another jump");
      var step = bridge.update(false, .1, 1, false, true, true, false, true);
      require(step.lookForward() && step.phase() == TellyBridge.Phase.RECOVER_FORWARD, "normal Telly keeps sprinting forward once its landing is covered");
      step = bridge.update(false, -.1, 1, false, true, true, false, false);
      require(!step.lookForward(), "a direction change that loses the landing reopens the catch");
      bridge.reset(); bridge.update(true, 0, 0, true, false, true, true, false);
      require(bridge.recoverForward(0, true) == null, "the lower platform cannot prematurely finish an upward Telly catch");
      require(bridge.recoverForward(1, true).lookForward(), "a raised Telly landing returns forward as soon as it is secured");
      var second = new Telly2Bridge();
      second.update(true, 0, 0, true, true);
      require(second.recoverForward(0) == null, "Telly2 also ignores the old lower platform while climbing");
      step = second.update(false, .24, 1, true, true);
      require(step.lookForward() && step.phase() == TellyBridge.Phase.RECOVER_FORWARD,
         "Telly2 does not wait for a fixed air-tick or vertical-speed threshold after securing its raised landing");
      step = second.update(false, .16, 1, true, false);
      require(!step.lookForward(), "Telly2 resumes catching immediately when the secured landing is lost");
   }

   private static void tellyCycle() {
      var bridge = new TellyBridge();
      var step = bridge.update(true, 0, 0, false, false, true, false);
      require(!step.lookForward() && !step.jump(), "Telly first aligns backward without jumping blind");
      step = bridge.update(true, 0, 0, true, false, false, false);
      require(!step.jump(), "a low ceiling prevents the automatic jump");
      step = bridge.update(true, 0, 0, true, false, true, false);
      require(step.lookForward() && step.jump(), "Telly jumps and turns forward from the transmitted backward look");
      for (int tick = 1; tick <= 12; tick++) {
         step = bridge.update(false, tick < 6 ? 0.2 : -0.2, tick < 10 ? 1 : 0, false, false, true, false);
         require(!step.jump() && step.layer() == 0, "airborne Telly neither repeats jumps nor raises the catch layer");
         require(step.lookForward() == (tick <= TellyBridge.FORWARD_AIR_TICKS), "Telly preserves forward sprint ticks, then turns backward in time to place");
      }
      step = bridge.update(true, 0, 2, true, false, true, false);
      require(step.lookForward() && step.jump() && step.layer() == 2,
         "landing refreshes the takeoff layer and immediately begins the next jump");
      bridge.reset();
      step = bridge.update(false, -0.3, -1, false, false, true, false);
      require(!step.lookForward() && !step.jump() && step.layer() == -1,
         "enabling in the air immediately selects a downward catch");
      bridge.reset();
      bridge.update(true, 0, 0, true, false, true, true);
      step = bridge.update(false, 0.16, 1, false, false, true, true);
      require(step.layer() == 1, "holding Jump permits raised blocks once the feet clear their top");
      step = bridge.update(false, -0.15, 1, false, false, true, false);
      require(step.layer() == 0, "releasing Jump restores the takeoff layer instead of forcing stairs");
      step = bridge.update(false, -0.2, 0, true, false, true, false);
      require(!step.lookForward(), "Telly keeps its placement turn through the descending catch");
      step = bridge.update(false, -0.25, 0, false, true, true, false);
      require(!step.lookForward(), "Telly remains in the catch until landing");
   }

   private static void tellyMovementReplay() {
      // This checks uninterrupted native movement, rather than the removed
      // behavior that stopped the player to await a server-confirmed landing.
      for (float heading : new float[]{0, 45, 90, 135, 180, 225, 270, 315}) {
      for (double sensitivity : new double[]{0, 0.5, 1}) {
         var bridge = new TellyBridge();
         Input keys = new Input(true, false, false, false, false, false, false);
         float yaw = heading, pitch = 0;
         double y = 1, vy = 0, speed = 0.3;
         boolean grounded = true;
         int jumps = 0, landings = 0;
         Vec3 travel = Vec3.directionFromRotation(0, heading);
         for (int tick = 0; tick < 96; tick++) {
            boolean backwardReady = Math.abs(net.minecraft.util.Mth.wrapDegrees(heading + TellyBridge.BACKWARD_OFFSET - yaw)) < 1
               && pitch >= 45;
            boolean forwardReady = Math.abs(net.minecraft.util.Mth.wrapDegrees(heading - yaw)) < 1;
            var step = bridge.update(grounded, vy, 0, backwardReady, forwardReady, true, false);
            var look = TellyBridge.turn(yaw, pitch, step.lookForward() ? heading : heading + TellyBridge.BACKWARD_OFFSET,
               step.lookForward() ? 15 : 82, sensitivity);
            var corrected = SilentRotationMath.correctMovement(keys, heading, look.yaw());
            require(corrected.forward() != 0 || corrected.sideways() != 0,
               "Telly keeps held movement through takeoff, forward travel, backward catch and landing");
            double along = inputWorld(corrected, look.yaw()).dot(travel);
            require(along > 0.92, "Telly never countersteers or releases movement in midair");
            if (step.lookForward()) {
               require(!keys.sprint() && ScaffoldMovement.forwardSprint(new Input(corrected.forward() > 0, corrected.forward() < 0,
                  corrected.sideways() > 0, corrected.sideways() < 0, step.jump(), false, false)),
                  "Telly's complete forward phase allows auto sprint with no physical sprint key");
            }
            if (step.jump()) {
               require(grounded && corrected.forward() > 0,
                  "automatic Telly jumps retain forward input for native sprint");
               vy = 0.42; grounded = false; speed = 0.3; jumps++;
            } else if (!grounded) {
               speed += along * (step.lookForward() ? 0.026 : 0.02);
            }
            y += vy;
            if (!grounded && vy <= 0 && y <= 1) {
               y = 1; vy = 0; grounded = true; landings++;
            }
            if (!grounded) { vy = (vy - 0.08) * 0.98; speed *= 0.91; }
            require(speed > 0.18, "native air drag does not become an injected midair stop");
            yaw = look.yaw(); pitch = look.pitch();
         }
         require(jumps >= 5 && landings >= 5, "Telly keeps cycling jumps without a support-wait pause");
      }
      }
   }
   private static void tellyPlacementReplay() {
      tellyPlacementReplay(false);
   }

   private static void tellyPlacementReplay(boolean climb) {
      tellyPlacementReplay(climb, false);
   }

   private static void tellyPlacementReplay(boolean climb, boolean second) {
      for (boolean legacy : new boolean[]{false,true}) {
      for (float heading : new float[]{0, 45, 90, 135, 180, 225, 270, 315}) {
      for (double sensitivity : new double[]{0, 0.5, 1}) {
      for (int ackDelay : new int[]{1, 8}) {
      for (boolean centerRequested : new boolean[]{false, true}) {
      for (double jumpSpeed : climb ? new double[]{0.3, 0.48} : new double[]{0.3}) {
         FakeWorld world = new FakeWorld();
         world.nativeRaycast = true;
         // Match the live support policy: a locally placed solid block may
         // support the next ordered native useItemOn before its ack arrives.
         world.allowPredictedSupports = true;
         for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) world.solid.add(new BlockPos(x, 0, z));
         var bridge = new TellyBridge();
         var bridge2 = new Telly2Bridge();
         Vec3 feet = new Vec3(0.5, 1, 0.5);
         Vec3 travel = Vec3.directionFromRotation(0, heading);
         boolean diagonalKeys = centerRequested && heading == 45;
         Input keys = new Input(true, false, diagonalKeys, false, false, false, false);
         float inputCameraYaw = diagonalKeys ? heading + 45 : heading;
         boolean centerMode = centerRequested && !ScaffoldMovement.diagonalBridge(keys, inputCameraYaw);
         float yaw = heading, pitch = 0;
         PlacementPlanner.Placement centerTarget = null;
         double vy = 0;
         Vec3 velocity = travel.scale(0.28);
         boolean grounded = true;
         int placements = 0, landings = 0;
         for (int tick = 0; tick < 110; tick++) {
            float precedingYaw = yaw, precedingPitch = pitch;
            world.confirmations.acknowledge(tick - ackDelay);
            Vec3 motion = velocity.add(0, vy, 0);
            Vec3 eyes = feet.add(0, 1.62, 0);
            world.playerBox = new AABB(feet.x - 0.3, feet.y, feet.z - 0.3, feet.x + 0.3, feet.y + 1.8, feet.z + 0.3);
            boolean ready = centerMode ? tick > 0
               : Math.abs(net.minecraft.util.Mth.wrapDegrees(heading + TellyBridge.BACKWARD_OFFSET - yaw)) < 1 && pitch >= 45;
            int landingLayer = climb || second ? PlacementPlanner.blockLayer(feet.y)
               : bridge.placementLayer(PlacementPlanner.blockLayer(feet.y), false);
            boolean landingReady = PlacementPlanner.hasForwardLanding(feet, motion, landingLayer, 0.08, heading, true, world);
            boolean forwardReady = Math.abs(net.minecraft.util.Mth.wrapDegrees(heading - yaw)) < 1;
            var step = second ? bridge2.update(grounded, vy, PlacementPlanner.blockLayer(feet.y), true, landingReady)
               : bridge.update(grounded, vy, PlacementPlanner.blockLayer(feet.y), ready,
               forwardReady, true, climb, landingReady);
            int targetLayer = step.layer();
            var targets = centerMode ? PlacementPlanner.centerTargets(feet, motion, targetLayer, !grounded, true, 0.08, world)
               : PlacementPlanner.catchTargets(feet, motion, targetLayer, !grounded, true, 0.08);
            float desiredYaw = step.lookForward() ? heading : heading + TellyBridge.BACKWARD_OFFSET;
            var initial = TellyBridge.turn(yaw, pitch, desiredYaw, step.lookForward() ? 15 : 82, sensitivity);
            float desiredPitch = step.lookForward() ? 15 : PlacementPlanner.backwardPitch(targets, eyes, 4.5,
               initial.yaw(), pitch, sensitivity, world, (climb || second) && ScaffoldMovement.diagonalBridge(keys, heading));
            var look = TellyBridge.turn(yaw, pitch, desiredYaw, desiredPitch, sensitivity);
            var placement = step.lookForward() ? null : centerMode
               ? PlacementPlanner.findAlongCenterLook(centerTarget, targets, eyes, 4.5, yaw, pitch, world)
               : PlacementPlanner.findAlongLook(targets, eyes, 4.5, yaw, pitch, world);
            if (centerMode) {
               var forecastShapes = world.solid.stream().map(pos->net.minecraft.world.phys.shapes.Shapes.block().move(pos.getX(),pos.getY(),pos.getZ())).toList();
               Vec3 forecast = ScaffoldMovement.collisionStep(world.playerBox, motion.add(travel.scale(grounded ? .1 : .02)), forecastShapes);
               Vec3 following = new Vec3(forecast.x*(grounded?.546:.91)+travel.x*(grounded?.13:.026),
                  grounded?0:(forecast.y-.08)*.98,forecast.z*(grounded?.546:.91)+travel.z*(grounded?.13:.026));
               var aim = step.lookForward() ? null : legacy
                  ? PlacementPlanner.centerPulseAim(targets,eyes.add(forecast),eyes.add(forecast).add(following),4.5,yaw,pitch,sensitivity,world,centerTarget)
                  : PlacementPlanner.centerAim(targets,eyes.add(forecast),4.5,yaw,pitch,sensitivity,world,centerTarget,false);
               if (!step.lookForward()) look = aim == null ? new SilentRotationMath.Rotation(yaw, pitch) : aim.rotation();
               centerTarget = aim == null ? null : aim.placement();
            }
            if (placement != null && ready) {
               look = new SilentRotationMath.Rotation(yaw, pitch);
               if (centerMode) centerTarget = placement;
            }
            boolean clicked = placement != null && PlacementPlanner.lookMatchesSupport(placement, eyes, 4.5,
               look.yaw(), look.pitch(), world);
            if (clicked) {
               world.solid.add(placement.target()); world.confirmations.predict(placement.target(), tick); placements++;
               if ((climb || second) && !centerMode && ScaffoldMovement.diagonalBridge(keys, heading)) {
                  var corner = PlacementPlanner.findAlongLook(PlacementPlanner.centerTargets(feet, motion, targetLayer, !grounded, true, 0.08, world),
                     eyes, 4.5, yaw, pitch, world);
                  if (corner != null && PlacementPlanner.lookMatchesSupport(corner, eyes, 4.5, look.yaw(), look.pitch(), world)) {
                     world.solid.add(corner.target()); world.confirmations.predict(corner.target(), tick); placements++;
                     placement = corner;
                  }
               }
               if (centerMode) {
                  var forecastShapes=world.solid.stream().map(pos->net.minecraft.world.phys.shapes.Shapes.block().move(pos.getX(),pos.getY(),pos.getZ())).toList();
                  Vec3 forecast=ScaffoldMovement.collisionStep(world.playerBox,motion.add(travel.scale(grounded?.1:.02)),forecastShapes);
                  Vec3 following=new Vec3(forecast.x*(grounded?.546:.91)+travel.x*(grounded?.13:.026),
                     grounded?0:(forecast.y-.08)*.98,forecast.z*(grounded?.546:.91)+travel.z*(grounded?.13:.026));
                  var nextTargets=PlacementPlanner.centerTargets(feet,motion,targetLayer,!grounded,true,.08,world);
                  var next = legacy
                     ? PlacementPlanner.centerPulseAim(nextTargets,eyes.add(forecast),eyes.add(forecast).add(following),4.5,yaw,pitch,sensitivity,world,centerTarget)
                     : PlacementPlanner.centerAim(nextTargets,eyes.add(forecast),4.5,yaw,pitch,sensitivity,world,centerTarget,false);
                  if (next != null && (!legacy || PlacementPlanner.lookIntersectsClickedSupport(placement,eyes.add(forecast),4.5,yaw,next.rotation().pitch()))) {
                     look = next.rotation(); centerTarget = next.placement();
                  }
               }
            }
            if (clicked && !grounded && PlacementPlanner.hasForwardLanding(feet, motion, targetLayer, .08, heading, true, world)) {
               var recovered = second ? bridge2.recoverForward(PlacementPlanner.blockLayer(feet.y))
                  : bridge.recoverForward(PlacementPlanner.blockLayer(feet.y), climb);
               if (recovered != null) {
                  step = recovered;
                  look = TellyBridge.turn(yaw, pitch, heading, 15, sensitivity);
                  centerTarget = null;
               }
            }
            var corrected = SilentRotationMath.correctMovement(keys, inputCameraYaw, look.yaw());
            Vec3 input = inputWorld(corrected, look.yaw());
            require(input.dot(travel) > 0.92, "both Telly modes preserve held movement throughout uphill catches");
            // Apply the corrected native input direction to momentum. A
            // scalar speed forced along the camera direction erased the
            // backward look's actual lateral acceleration from the replay.
            // Retain the existing straight-heading flat fixture; the uphill
            // cases exercise the corrected WASD direction and full momentum.
            Vec3 accelerationDirection = climb || second ? input : travel;
            if (step.jump()) { vy = 0.42; grounded = false; velocity = accelerationDirection.scale(jumpSpeed); }
            else if (!grounded) velocity = velocity.add(accelerationDirection.scale(step.lookForward() ? 0.026 : 0.02));
            Vec3 previous = feet;
            boolean wasGrounded = grounded;
            double requestedY = grounded && vy == 0 ? -0.0784 : vy;
            var shapes = world.solid.stream().map(block -> net.minecraft.world.phys.shapes.Shapes.block()
               .move(block.getX(), block.getY(), block.getZ())).toList();
            Vec3 requested = new Vec3(velocity.x, requestedY, velocity.z), resolved = Vec3.ZERO;
            // Match Entity.collideWithShapes using this version's native axis
            // order, rather than checking only the destination footprint.
            for (Direction.Axis axis : Direction.axisStepOrder(requested)) {
               double distance = requested.get(axis);
               if (distance != 0) resolved = resolved.with(axis, net.minecraft.world.phys.shapes.Shapes.collide(
                  axis, world.playerBox.move(resolved), shapes, distance));
            }
            double movedX = resolved.x, movedY = resolved.y, movedZ = resolved.z;
            feet = previous.add(movedX, movedY, movedZ);
            grounded = requestedY < 0 && Math.abs(movedY - requestedY) > 1.0E-7;
            if (grounded && !wasGrounded) landings++;
            if (Math.abs(movedY - requestedY) > 1.0E-7) vy = 0;
            velocity = new Vec3(Math.abs(movedX - velocity.x) > 1.0E-7 ? 0 : velocity.x, 0,
               Math.abs(movedZ - velocity.z) > 1.0E-7 ? 0 : velocity.z);
            if (!grounded) { vy = (vy - 0.08) * 0.98; velocity = velocity.scale(0.91); }
            require(feet.y >= 0.99, "Telly must build its actual landing: center=" + centerMode + " heading=" + heading + " sensitivity=" + sensitivity
               + " delay=" + ackDelay + " speed=" + jumpSpeed + " tick=" + tick + " placed=" + placements + " feet=" + feet);
            yaw = look.yaw(); pitch = look.pitch();
            world.playerBox = new AABB(feet.x - 0.3, feet.y, feet.z - 0.3, feet.x + 0.3, feet.y + 1.8, feet.z + 0.3);
            if (!step.lookForward()) {
               int postLayer = climb || second ? PlacementPlanner.blockLayer(feet.y) : Math.min(PlacementPlanner.blockLayer(feet.y), step.layer());
               targets = centerMode ? PlacementPlanner.centerTargets(feet, velocity.add(0, vy, 0), postLayer, !grounded, true, 0.08, world)
                  : PlacementPlanner.catchTargets(feet, velocity.add(0, vy, 0), postLayer, !grounded, true, 0.08);
               if (centerMode) {
                  var aim = PlacementPlanner.centerAim(targets, feet.add(0, 1.62, 0), 4.5,
                     yaw, pitch, sensitivity, world, centerTarget, true);
                  if (aim != null) {
                     pitch = aim.rotation().pitch(); centerTarget = aim.placement();
                  }
               } else {
                  float nextPitch = PlacementPlanner.backwardPitch(targets, feet.add(0, 1.62, 0), 4.5, yaw, pitch, sensitivity, world,
                     (climb || second) && ScaffoldMovement.diagonalBridge(keys, heading));
                  float refined = SilentRotationMath.quantizeRotation(yaw, pitch, yaw,
                     pitch + net.minecraft.util.Mth.clamp(nextPitch - pitch, -40, 40), sensitivity).pitch();
                  if (climb || second || !clicked || PlacementPlanner.lookIntersectsClickedSupport(placement, eyes, 4.5, yaw, refined)) pitch = refined;
               }
            }
            if (clicked && centerMode && legacy) {
               Vec3 postEyes=feet.add(0,1.62,0);
               if (!PlacementPlanner.lookIntersectsClickedSupport(placement,postEyes,4.5,precedingYaw,pitch))
                  pitch=PlacementPlanner.postPlacementPitch(placement,postEyes,4.5,precedingYaw,precedingPitch,sensitivity);
               require(PlacementPlanner.lookIntersectsClickedSupport(placement,postEyes,4.5,precedingYaw,pitch),
                  "Telly Center retains the clicked support through its return/next-center movement packet: heading="+heading+" tick="+tick);
            }
         }
         require(placements >= 15 && landings >= 5, "Telly continuously extends the bridge across repeated jumps");
         if (climb || second) require(feet.y >= 4, "upward Telly must gain height, with Telly2 requiring no held Space: center=" + centerMode
            + " heading=" + heading + " sensitivity=" + sensitivity + " delay=" + ackDelay + " feet=" + feet);
      }
      }
      }
      }
      }
      }
   }

   private static void knockbackDisplacement() {
      for (int chance : new int[]{0, 1, 25, 50, 99, 100}) {
         int selected = 0;
         for (int roll = 0; roll < 100; roll++) {
            if (KnockbackDisplacement.shouldDisplace(chance, roll, true, false)) selected++;
            require(!KnockbackDisplacement.shouldDisplace(chance, roll, false, false),
               "ordinary unsprinted, unenchanted attacks do not rotate");
            require(KnockbackDisplacement.shouldDisplace(chance, roll, false, true)
               == KnockbackDisplacement.shouldDisplace(chance, roll, true, false),
               "enchanted attacks can displace with sprint released for PreferCrit");
         }
         require(selected == chance, "chance is an exact percentage over the 100 possible rolls");
      }
      for (float yaw : new float[]{-540, -180, -90, 0, 90, 180, 540}) {
         for (var direction : KnockbackDisplacement.Direction.values()) {
            var original = new KnockbackDisplacement.Look(yaw, 23.5F);
            var sent = new TransmittedRotation();
            sent.reset(yaw, original.pitch());
            List<String> order = new ArrayList<>();
            KnockbackDisplacement.attack(original, direction, look -> {
               order.add("look");
               sent.record(new ServerboundMovePlayerPacket.Rot(look.yaw(), look.pitch(), true, false),
                  false, 42, original.yaw(), original.pitch());
            }, () -> {
               order.add("attack");
               Vec3 forward = Vec3.directionFromRotation(0, original.yaw());
               Vec3 pushed = Vec3.directionFromRotation(0, sent.yaw());
               require(sent.yaw() == original.yaw()
                  + (direction == KnockbackDisplacement.Direction.LEFT ? -90.0F : 90.0F),
                  "attack rotation changes by exactly 90 degrees without wrapping the baseline");
               // Mth sin/cos quantize to 65,536 entries; two directions can
               // differ from mathematical orthogonality by a few table steps.
               require(Math.abs(forward.dot(pushed)) < 0.0003, "displaced knockback is sideways at native trig precision");
               double cross = forward.x * pushed.z - forward.z * pushed.x;
               require(direction == KnockbackDisplacement.Direction.LEFT ? cross < -0.999 : cross > 0.999,
                  "Left and Right use the attack aim's orientation, independent of world heading");
               require(sent.pitch() == original.pitch(), "displacement preserves attack pitch");
               require(sent.targetId() == -1, "sideways look is never falsely tagged as Recode's on-target aim");
            });
            require(order.equals(List.of("look", "attack", "look")), "send displaced look before attack and restored aim afterward");
            require(sent.yaw() == original.yaw() && sent.pitch() == original.pitch() && sent.targetId() == 42,
               "restore the precise Recode look and target tag, including unwrapped yaw");
         }
      }
      var original = new KnockbackDisplacement.Look(723.25F, -17.0F);
      List<KnockbackDisplacement.Look> sent = new ArrayList<>();
      RuntimeException failure = new RuntimeException("attack aborted");
      try {
         KnockbackDisplacement.attack(original, KnockbackDisplacement.Direction.RIGHT, sent::add, () -> { throw failure; });
         throw new AssertionError("attack failure must propagate");
      } catch (RuntimeException actual) {
         require(actual == failure && sent.size() == 2 && sent.getLast().equals(original),
            "a failed attack still restores the original server look");
      }
   }

   private static void preferredCriticalTiming() {
      var ground = new CriticalTiming.State(true, false, 0, 0, true, false, true, true);
      var takeoff = new CriticalTiming.State(true, true, 0, 0, true, false, true, true);
      var rising = new CriticalTiming.State(false, false, 0.2, 0, true, false, true, true);
      var apex = new CriticalTiming.State(false, false, 0, 0, true, false, false, true);
      var firstFall = new CriticalTiming.State(false, false, -0.075, 0, true, false, false, true);
      var falling = new CriticalTiming.State(false, false, -0.15, 0.075, true, false, false, true);
      var sprinting = new CriticalTiming.State(false, false, -0.15, 0.075, true, false, true, true);
      var blind = new CriticalTiming.State(false, false, -0.15, 0.075, true, true, false, true);
      var swimming = new CriticalTiming.State(false, true, -0.15, 0.075, false, false, false, true);
      var landed = new CriticalTiming.State(true, false, -0.075, 0.5, true, false, false, true);
      require(CriticalTiming.allowsAttack(true, false, ground), "PreferCrit retains ordinary ground attacks");
      require(!CriticalTiming.allowsAttack(true, false, takeoff), "jump input reserves the attack before takeoff");
      require(!CriticalTiming.allowsAttack(true, false, rising), "do not attack on the rising half of a jump");
      require(!CriticalTiming.allowsAttack(true, false, apex), "zero vertical velocity is not a falling crit");
      require(!CriticalTiming.allowsAttack(true, false, firstFall), "wait until native fall distance has accumulated");
      require(CriticalTiming.allowsAttack(true, false, falling), "released jump input still permits a real descending crit");
      require(!CriticalTiming.allowsAttack(true, false, sprinting), "sprinting cannot be counted as a critical hit");
      require(CriticalTiming.suppressSprint(true, false, sprinting), "release sprint before the falling attack");
      require(CriticalTiming.suppressSprint(true, false, falling), "keep sprint released through the crit window");
      require(!CriticalTiming.suppressSprint(true, false, ground), "ground sprinting remains available");
      require(!CriticalTiming.suppressSprint(true, false, takeoff), "a sprint jump retains its normal takeoff");
      require(!CriticalTiming.allowsAttack(true, false, blind), "blindness prevents a jump from being treated as a crit");
      require(CriticalTiming.allowsAttack(true, false, swimming), "swimming is not mistaken for jumping");
      require(!CriticalTiming.allowsAttack(false, true, swimming), "OnlyCrits remains strict in noncritical environments");
      require(CriticalTiming.allowsAttack(true, false, landed), "normal attacks resume immediately on landing");
      require(!CriticalTiming.allowsAttack(true, true, landed), "OnlyCrits takes precedence over PreferCrit");
      require(!CriticalTiming.allowsAttack(true, false,
         new CriticalTiming.State(false, false, 0.1, 0.2, true, false, false, true)), "stale fall distance cannot authorize an ascending hit");
      for (var state : List.of(ground, takeoff, rising, apex, firstFall, falling, sprinting, blind, swimming, landed)) {
         require(CriticalTiming.allowsAttack(false, false, state), "disabled crit options preserve ordinary attack timing");
         require(!CriticalTiming.suppressSprint(false, false, state), "disabled crit options never suppress sprint");
      }
   }

   private static void visibleCombatAim() {
      AABB standingTarget = new AABB(-0.3, 0, 1.5, 0.3, 1.8, 2.1);
      Vec3 visibleHead = CombatAim.visiblePoint(standingTarget, point -> point.y > 1.6).orElseThrow();
      Vec3 beforeJump = new Vec3(0, 1.62, 0);
      Vec3 afterJump = beforeJump.add(0, 0.42, 0);
      var oldLook = aimAt(visibleHead, beforeJump, 0, 0, 0.5);
      require(standingTarget.clip(afterJump, afterJump.add(Vec3.directionFromRotation(oldLook.pitch(), oldLook.yaw()).scale(4.5))).isEmpty(),
         "pre-movement head aim reproduces an upward jump miss");
      var refreshed = aimAt(visibleHead, afterJump, oldLook.yaw(), oldLook.pitch(), 0.5);
      require(refreshed.yaw() == oldLook.yaw()
         && standingTarget.clip(afterJump, afterJump.add(Vec3.directionFromRotation(refreshed.pitch(), oldLook.yaw()).scale(4.5))).isPresent(),
         "refreshing pitch after a jump restores the hit without changing physics yaw");
      for (double height : new double[]{0.9, 1.5, 1.8}) {
         AABB target = new AABB(-0.3, 0, 1.5, 0.3, height, 2.1);
         Vec3 center = CombatAim.visiblePoint(target, point -> true).orElseThrow();
         require(center.equals(target.getCenter()), "clear targets use their hitbox center for a stable aim margin");
         Vec3 head = CombatAim.visiblePoint(target, point -> point.y > height * 0.9).orElseThrow();
         require(target.contains(head) && head.y > height * 0.9, "aim at the visible upper hitbox when the body is covered");
         Vec3 side = CombatAim.visiblePoint(target, point -> point.x > 0.27).orElseThrow();
         require(target.contains(side) && side.x > 0.27, "a visible side can be selected without aiming outside the hitbox");
         Vec3 low = CombatAim.visiblePoint(target, point -> point.y < height * 0.3).orElseThrow();
         require(target.contains(low), "lower visible aim points stay inside crouching and small hitboxes");
         require(CombatAim.visiblePoint(target, point -> false).isEmpty(), "fully covered targets cannot invent a visible aim point");
         for (Vec3 point : List.of(center, head, side, low)) {
            for (double eyeY : new double[]{1.62, 2.25, 2.8}) {
               Vec3 eyes = new Vec3(0, eyeY, 0);
               for (double sensitivity : new double[]{0, 0.5, 1}) {
                  var rotation = aimAt(point, eyes, 0, 0, sensitivity);
                  Vec3 end = eyes.add(Vec3.directionFromRotation(rotation.pitch(), rotation.yaw()).scale(4.5));
                  require(target.clip(eyes, end).isPresent(), "quantized aim still intersects the real hitbox at jump heights");
               }
            }
         }
      }
   }

   private static void headAimAndReach() {
      for (double height : new double[]{0.3, 0.9, 1.5, 1.8}) {
         AABB box = new AABB(-0.3, 0, 2, 0.3, height, 2.6);
         double eyeY = height * 0.9;
         Vec3 head = CombatAim.visibleHeadPoint(box, eyeY, point -> true).orElseThrow();
         require(head.y == eyeY && box.contains(head), "clear targets use their actual head/eye height inside the hitbox");
         Vec3 side = CombatAim.visibleHeadPoint(box, eyeY, point -> point.x > 0.18).orElseThrow();
         require(side.y >= eyeY - 0.12 && side.x > 0.18 && box.contains(side), "partial visibility selects a head point, not a torso point");
         require(CombatAim.visibleHeadPoint(box, eyeY, point -> point.y < height * 0.5).isEmpty(),
            "a hidden head cannot fall back to the visible torso");
         require(CombatAim.visibleHeadPoint(box, eyeY, point -> false).isEmpty(), "a fully hidden head has no visible aim");
         Vec3 shift = new Vec3(0.2, 0.35, -0.4);
         require(CombatAim.headCenter(box.move(shift), eyeY + shift.y).distanceTo(head.add(shift)) < 0.000001,
            "predicted head aim follows horizontal and vertical target motion");
         for (double playerEyeY : new double[]{1.62, 2.25, 2.8}) {
         for (double sensitivity : new double[]{0, 0.5, 1}) {
            Vec3 eyes = new Vec3(0, playerEyeY, 0);
            var rotation = aimAt(head, eyes, 0, 0, sensitivity);
            require(box.clip(eyes, eyes.add(Vec3.directionFromRotation(rotation.pitch(), rotation.yaw()).scale(4.5))).isPresent(),
               "quantized head aim intersects crouching, small and standing targets while jumping");
         }
         }
      }
      Vec3 eyes = new Vec3(0, 1.62, 0);
      for (double reach : new double[]{1, 2, 3, 3.42, 4.5, 6}) {
         for (double offset : new double[]{-0.02, 0.02}) {
            AABB box = new AABB(-0.3, 0, reach + offset, 0.3, 1.8, reach + offset + 0.6);
            Vec3 head = CombatAim.headCenter(box, 1.62);
            Vec3 ray = head.subtract(eyes).normalize().scale(reach);
            require(box.clip(eyes, eyes.add(ray)).isPresent() == (offset < 0),
               "the head ray uses the configured reach boundary, including distances above three blocks");
         }
      }
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
      var look = aimAt(placement.hit().getLocation(), eyes, 0, 0, 0.5);
      world.solid.add(target);
      require(!PlacementPlanner.lookMatchesSupport(placement, eyes, 4.5, look.yaw(), look.pitch(), world),
         "the just-placed block reproduces occlusion of the support used for that click");
      require(PlacementPlanner.lookIntersectsClickedSupport(placement, eyes, 4.5, look.yaw(), look.pitch()),
         "post-click aim can retain the old support without waiting on its new block");
      require(!PlacementPlanner.lookIntersectsClickedSupport(placement, eyes, 0.5, look.yaw(), look.pitch())
         && !PlacementPlanner.lookIntersectsClickedSupport(placement, eyes, 4.5, look.yaw() + 180, look.pitch()),
         "post-click geometry still rejects out-of-range and opposite-facing looks");
      world.solid.remove(target);
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
      world.confirmations.confirmBlock(support);
      require(!world.confirmations.isPending(support), "server block updates release legacy supports before a sequence ack");
      world.confirmations.predict(support, 21);
      require(world.confirmations.acknowledge(20).isEmpty() && world.confirmations.isPending(support),
         "a later re-prediction is not resolved by an earlier ack");
      world.confirmations.clear();
      require(!world.confirmations.isPending(support), "changing worlds clears stale sequences");
   }

   private static void placementLookOrdering() {
      FakeWorld world = new FakeWorld();
      world.solid.add(BlockPos.ZERO);
      world.aimEyes = new Vec3(1.1, 2.62, 0.5);
      world.aimYaw = -90;
      world.aimPitch = 0;
      world.aimSensitivity = 0.5;
      var placement = PlacementPlanner.find(List.of(BlockPos.ZERO.east()), world.aimEyes, 4.5, world);
      require(placement != null, "new placement can be aimed at with the upcoming look");
      require(!PlacementPlanner.lookMatchesSupport(placement, world.aimEyes, 4.5,
         world.aimYaw, world.aimPitch, world), "old horizontal look cannot justify a click before the aim packet");
      var aimed = world.aimRotation(placement);
      require(PlacementPlanner.lookMatchesSupport(placement, world.aimEyes, 4.5,
         aimed.yaw(), aimed.pitch(), world), "next tick may place once the transmitted aim reaches the support");
      require(PlacementPlanner.lookMatchesSupport(placement, world.aimEyes, 4.5, 90, 75, world),
         "previous look may reach the support's top while the current click uses its side");
      Vec3 movedEyes = world.aimEyes.add(0, 0, 2);
      require(!PlacementPlanner.lookMatchesSupport(placement, movedEyes, 4.5,
         aimed.yaw(), aimed.pitch(), world), "a previously valid aim is rechecked after movement");
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
         StringBuilder trajectory = new StringBuilder();
         for (int tick = 0; tick < 16; tick++) {
            if (rejected != null && tick >= rejectedAt + ackDelay) {
               world.solid.remove(rejected);
            }
            world.confirmations.acknowledge(tick - ackDelay);
            Vec3 motion = new Vec3(velocityX, velocityY, velocityZ);
            Vec3 eyes = feet.add(0, 1.62, 0);
            var placement = catchOnly
               ? PlacementPlanner.find(PlacementPlanner.catchTargets(feet, motion, 0, true, true, 0.08), eyes, 4.5, world)
               : PlacementPlanner.findAimOrPlace(feet, motion, 0, true, false, 0.08, eyes, 4.5, world, sentYaw, sentPitch);
            trajectory.append("\n").append(tick).append(": ").append(feet)
               .append(" -> ").append(placement == null ? null : placement.target());
            if (placement != null) {
               Vec3 delta = placement.hit().getLocation().subtract(eyes);
               float yaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
               float pitch = (float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
               var rotation = SilentRotationMath.quantizeRotation(sentYaw, sentPitch, yaw, pitch, 0.5);
               boolean ready = PlacementPlanner.lookMatchesSupport(placement, eyes, 4.5, sentYaw, sentPitch, world);
               trajectory.append(ready ? " place" : " aim");
               sentYaw = rotation.yaw();
               sentPitch = rotation.pitch();
               var hit = world.raycast(eyes, eyes.add(Vec3.directionFromRotation(sentPitch, sentYaw).scale(4.5)));
               if (ready && PlacementPlanner.matchesSupport(placement, hit)) {
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
            } else {
               var idleAim = PlacementPlanner.findSupportAim(
                  PlacementPlanner.catchTargets(feet, motion, 0, true, false, 0.08), eyes, 4.5, world);
               if (idleAim != null) {
                  var rotation = aimAt(idleAim.getLocation(), eyes, sentYaw, sentPitch, 0.5);
                  sentYaw = rotation.yaw();
                  sentPitch = rotation.pitch();
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
            // Compare with the flat path under the same transmitted-look gate.
            // Immediate clicks using unsent aim are no longer a valid baseline.
            require(landed || !flatLanded,
               "step selection cannot lose a feasible flat catch: heading=" + heading + ", speed=" + speed
                  + ", ackDelay=" + ackDelay + ", rejectStep=" + rejectStep + trajectory);
         }
      }
      }
      }
      }
      }
      require(stepLandings > 0, "the combined planner can actually land on raised steps");
      require(rejectedSteps > 0, "the jump replay exercises rejected raised placements");
   }

   private static void sprintingBridgeLatency() {
      int unprotectedFailures = 0;
      for (double speed : new double[]{0.215, 0.30}) {
      for (int delay : new int[]{1, 3, 5, 8}) {
         for (int heading = 0; heading < 360; heading += 30) {
            for (double sensitivity : new double[]{0, 0.5, 1}) {
            for (boolean protectEdge : new boolean[]{false, true}) {
               FakeWorld world = new FakeWorld();
               world.aimYaw = heading;
               world.aimSensitivity = sensitivity;
               world.solid.add(BlockPos.ZERO);
               Vec3 feet = new Vec3(0.5, 1, 0.5);
               Vec3 requested = new Vec3(-Math.sin(Math.toRadians(heading)) * speed,
                  0, Math.cos(Math.toRadians(heading)) * speed);
               boolean fell = false;
               int placed = 0;

               int firstPlaced = -1;
               for (int tick = 0; tick < 160; tick++) {
                  world.confirmations.acknowledge(tick - delay);
                  world.aimEyes = feet.add(0, 1.62, 0);
                  var placement = PlacementPlanner.findAimOrPlace(feet, requested, 0, false, false,
                     0.08, feet.add(0, 1.62, 0), 4.5, world, world.aimYaw, world.aimPitch);
                  if (placement != null) {
                     var rotation = world.aimRotation(placement);
                     boolean ready = PlacementPlanner.lookMatchesSupport(placement, world.aimEyes, 4.5,
                        world.aimYaw, world.aimPitch, world);
                     world.aimYaw = rotation.yaw();
                     world.aimPitch = rotation.pitch();
                     if (ready) {
                        world.solid.add(placement.target());
                        world.confirmations.predict(placement.target(), tick);
                        placed++;
                        if (firstPlaced < 0) firstPlaced = tick;
                     }
                  } else {
                     var idleAim = PlacementPlanner.findSupportAim(
                        PlacementPlanner.catchTargets(feet, requested, 0, false, false, 0.08),
                        world.aimEyes, 4.5, world);
                     if (idleAim != null) {
                        var rotation = aimAt(idleAim.getLocation(), world.aimEyes, world.aimYaw,
                           world.aimPitch, sensitivity);
                        world.aimYaw = rotation.yaw();
                        world.aimPitch = rotation.pitch();
                     } else {
                        world.aimYaw = heading;
                        world.aimPitch = 0;
                     }
                  }
                  AABB box = new AABB(feet.x - 0.3, feet.y, feet.z - 0.3,
                     feet.x + 0.3, feet.y + 1.8, feet.z + 0.3);
                  boolean sneak = protectEdge && ScaffoldMovement.needsEdgeSneak(box,
                     requested, new Vec2(0, 1), heading, 0.13, 0.6,
                     probe -> world.solid.stream().noneMatch(pos -> new AABB(pos).intersects(probe)));
                  // Replay vanilla Player.maybeBackOffFromEdge's 0.05 steps
                  // only when actual sneak input will be sent to the server.
                  double x = requested.x, z = requested.z;
                  if (sneak) {
                     while (x != 0 && !hasFooting(feet.add(x, 0, 0), world)) x = backOff(x);
                     while (z != 0 && !hasFooting(feet.add(0, 0, z), world)) z = backOff(z);
                     while (x != 0 && z != 0 && !hasFooting(feet.add(x, 0, z), world)) {
                        x = backOff(x);
                        z = backOff(z);
                     }
                  }
                  feet = feet.add(x, 0, z);
                  if (!hasFooting(feet, world)) {
                     fell = true;
                     break;
                  }
               }
               if (protectEdge) {
                  require(!fell, "walking/sprinting bridge retains footing while aiming and awaiting ack: speed="
                     + speed + ", delay=" + delay + ", heading=" + heading);
                  require(placed >= 8, "edge protection resumes placement rather than freezing the bridge: delay="
                     + delay + ", heading=" + heading + ", placed=" + placed + ", feet=" + feet + ", blocks=" + world.solid);
                  require(firstPlaced >= 0 && firstPlaced <= 12,
                     "cold-start scaffold must actually begin placing in walking and sprinting modes");
               } else if (fell) unprotectedFailures++;
            }
            }
         }
      }
      }
      require(unprotectedFailures > 0, "reproduce sprint outrunning unacknowledged bridge support");
   }

   private static boolean hasFooting(Vec3 feet, FakeWorld world) {
      return world.solid.stream().anyMatch(pos -> pos.getY() == 0
         && feet.x + 0.3 > pos.getX() && feet.x - 0.3 < pos.getX() + 1
         && feet.z + 0.3 > pos.getZ() && feet.z - 0.3 < pos.getZ() + 1);
   }

   private static double backOff(double value) {
      return Math.abs(value) <= 0.05 ? 0 : value - Math.signum(value) * 0.05;
   }

   private static SilentRotationMath.Rotation aimAt(Vec3 point, Vec3 eyes, float yaw, float pitch, double sensitivity) {
      Vec3 delta = point.subtract(eyes);
      float targetYaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
      float targetPitch = (float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
      return SilentRotationMath.quantizeRotation(yaw, pitch, targetYaw, targetPitch, sensitivity);
   }

   private static void sprintInputEligibility() {
      for (int forward = -1; forward <= 1; forward++) {
         for (int sideways = -1; sideways <= 1; sideways++) {
            for (boolean sneak : new boolean[]{false, true}) {
               Input keys = new Input(forward > 0, forward < 0, sideways > 0, sideways < 0, false, sneak, true);
               require(ScaffoldMovement.forwardSprint(keys) == (forward > 0 && !sneak),
                  "only forward, non-sneaking input can sprint, including diagonals");
            }
         }
      }
      require(!ScaffoldMovement.forwardSprint(new Input(true, true, false, false, false, false, true)),
         "opposing forward/back keys cannot retain sprint");
      AABB box = new AABB(0.2, 1, 0.2, 0.8, 2.8, 0.8);
      require(!ScaffoldMovement.needsEdgeSneak(box, new Vec3(0.3, 0, 0), new Vec2(0, 1),
         -90, 0.13, 0.6, probe -> false), "flat supported ground never injects sneak");
      require(ScaffoldMovement.needsEdgeSneak(box, new Vec3(0.3, 0, 0), new Vec2(0, 1),
         -90, 0.13, 0.6, probe -> true), "an unsupported sprint step requests real sneak input");
      Input backwards = new Input(false, true, false, false, false, false, true);
      Input stopped = Input.EMPTY;
      Input forwards = new Input(true, false, false, false, false, false, true);
      require(!ScaffoldMovement.forwardSprint(backwards) && !ScaffoldMovement.forwardSprint(stopped)
         && ScaffoldMovement.forwardSprint(forwards), "sprint follows fresh input when placement rotation is released");
   }

   private static void automaticSprintDiagonals() {
      for (boolean normalized : new boolean[]{false, true}) {
         for (int sideways : new int[]{-1, 0, 1}) {
            Input keys = new Input(true, false, sideways > 0, sideways < 0, false, false, false);
            var direction = SilentRotationMath.correctMovement(keys, 0, 0);
            Vec2 vector = direction.inputVector(normalized);
            ClientInput input = testSprintInput(keys, vector);
            require(input.hasForwardImpulse(), "automatic sprint accepts straight and forward-diagonal native input");
            if (normalized && sideways != 0) {
               require(vector.y < 0.8F, "reproduce the old sprint threshold rejecting normalized diagonals");
            }
            for (float yaw : new float[]{-180, -90, -45, 0, 45, 90, 180}) {
               var corrected = SilentRotationMath.correctMovement(keys, 0, yaw);
               Input correctedKeys = new Input(corrected.forward() > 0, corrected.forward() < 0,
                  corrected.sideways() > 0, corrected.sideways() < 0, false, false, false);
               ClientInput correctedInput = testSprintInput(correctedKeys, corrected.inputVector(normalized));
               require(correctedInput.hasForwardImpulse() == (corrected.forward() > 0),
                  "sprint eligibility follows corrected server-relative movement rather than physical W");
            }
         }
      }
      require(!testSprintInput(Input.EMPTY, Vec2.ZERO).hasForwardImpulse(), "stopped movement cannot request sprint");
      require(!testSprintInput(new Input(false, false, true, false, false, false, true), new Vec2(1, 0)).hasForwardImpulse(),
         "pure sideways movement cannot request forward sprint");
      require(!testSprintInput(new Input(false, true, false, false, false, false, true), new Vec2(0, -1)).hasForwardImpulse(),
         "backward movement cannot request forward sprint");
      require(!testSprintInput(new Input(true, true, false, false, false, false, true), Vec2.ZERO).hasForwardImpulse(),
         "opposing movement keys cancel before sprint is requested");
   }

   private static ClientInput testSprintInput(Input keys, Vec2 vector) {
      return new ClientInput() {
         { this.keyPresses = keys; this.moveVector = vector; }
      };
   }

   private static void scaffoldClickRates() {
      for(int cps:new int[]{1,5,10,20,40}) {
         var rate=new PlacementClickRate(new java.util.Random(5)); int total=0;
         for(int tick=0;tick<=200;tick++) {
            int count=0;
            while(rate.tryAcquire(tick*50_000_000L,tick,cps,false,10,14)) { total++; count++; }
            require(count<=2,"Scaffold cannot burst above two clicks in a native tick");
         }
         require(total==cps*10+1,"fixed Scaffold CPS preserves its cumulative rate from 1 through 40: cps="+cps+" total="+total);
      }
      var rate=new PlacementClickRate(new java.util.Random(10));
      long previous=-1; Set<Long> intervals=new HashSet<>();
      for(long now=0;now<10_000_000_000L;now+=1_000_000L) {
         if(rate.tryAcquire(now,(int)(now/50_000_000L),40,true,10,20)) {
            if(previous>=0) {
               long delta=now-previous; intervals.add(delta);
               require(delta>=49_000_000L&&delta<=101_000_000L,"randomized Scaffold intervals remain inside MinCPS/MaxCPS");
            }
            previous=now;
         }
      }
      require(intervals.size()>3,"randomization actually changes the placement intervals");
      require(rate.tryAcquire(30_000_000_000L,600,1,true,40,40),"changing CPS settings applies without a stale wait");
      require(!rate.tryAcquire(30_000_000_000L,600,1,true,40,40),"a resumed frame has no accumulated placement backlog");
      rate.reset();
      require(rate.readyWithin(0,50_000_000L,10,false,1,40),"One Tick can prepare an aim before the next CPS deadline");
   }

   private static void recordedClicks() {
      var dispatcher=new com.mojang.brigadier.CommandDispatcher<me.alpha432.oyvey.manager.CommandManager>();
      var command=new me.alpha432.oyvey.features.commands.impl.ClickCommand();
      var builder=me.alpha432.oyvey.features.commands.Command.literal("click");
      command.createArgumentBuilder(builder); dispatcher.register(builder);
      for(String input:List.of("click record start","click record stop butterfly","click record cancel","click use butterfly","click list","click status")) {
         var parsed=dispatcher.parse(input,(me.alpha432.oyvey.manager.CommandManager)null);
         require(!parsed.getReader().canRead()&&parsed.getContext().getCommand()!=null,"the actual .click command tree accepts the requested recording workflow");
      }
      var manager=new RecordedClickManager();
      require(manager.start()&&!manager.start(),"duplicate start cannot erase an active click recording");
      manager.click(1_000_000_000L); manager.click(1_031_000_001L); manager.click(1_103_000_003L); manager.click(1_121_000_008L);
      require(manager.stop("Butterfly")==4,"a named recording contains each physical click");
      var expected=List.of(31_000_001L,72_000_002L,18_000_005L);
      require(manager.pattern().equals(expected),"recording preserves nanosecond intervals without CPS rounding");
      require(manager.selectedName().equals("butterfly"),"new recordings are selected under their normalized name");
      manager.start(); manager.click(10); manager.click(20); manager.pause(); manager.click(1000); manager.click(1040);
      require(manager.stop("pause")==3&&manager.pattern().equals(List.of(10L,40L)),"time outside Sneak/GUI capture is excluded between recording segments");
      manager.start(); manager.click(1); manager.click(1);
      require(manager.stop("empty")<0&&manager.isRecording(),"an all-zero/empty pattern cannot become an infinite playback loop");
      manager.cancel(); require(manager.names().equals(List.of("butterfly","pause")),"cancel preserves saved recordings");
      manager.use("butterfly");
      var restored=new RecordedClickManager();
      restored.fromJson(com.google.gson.JsonParser.parseString(new com.google.gson.Gson().toJson(manager.toJson())));
      require(restored.selectedName().equals("butterfly")&&restored.pattern().equals(expected)
         && restored.names().equals(manager.names()),"JSON round-trip retains every interval, name and selected recording");
      restored.fromJson(com.google.gson.JsonParser.parseString("{\"patterns\":{\"bad\":[-1],\"fraction\":[1.5],\"good\":[31,72]},\"selected\":\"bad\"}"));
      require(restored.names().equals(List.of("good"))&&restored.pattern().equals(List.of(31L,72L)),"malformed patterns do not discard other valid recordings");
      for(String bad:new String[]{"../outside","","a/b","a\\b","x".repeat(49)}) {
         boolean rejected=false;
         try { RecordedClickManager.normalizeName(bad); } catch(IllegalArgumentException e) { rejected=true; }
         require(rejected,"pattern names stay bounded and cannot act as file paths");
      }
      var playback=new RecordedClickPlayback();
      playback.prepare(expected,7,true,0);
      require(playback.poll(49_999_999L)==0&&playback.poll(50_000_000L)==1,"the first native attack follows its aim opportunity");
      long next=50_000_000L;
      for(int i=0;i<300;i++) {
         next+=expected.get(i%expected.size());
         require(playback.poll(next-1)==0&&playback.poll(next)==1,"playback repeats the exact cumulative interval sequence without timing drift");
      }
      playback.reset(); playback.prepare(expected,7,true,0);
      int polled=0;
      for(long now=0;now<=10_000_000_000L;now+=50_000_000L) polled+=playback.poll(now);
      int scheduled=0; long deadline=50_000_000L;
      while(deadline<=10_000_000_000L) { deadline+=expected.get(scheduled%expected.size()); scheduled++; }
      require(polled==scheduled,"native tick polling retains all recorded burst clicks instead of capping the pattern at 20 CPS");
      require(playback.poll(30_000_000_000L)==1,"a long freeze does not send a backlog of stale clicks");
      playback.prepare(expected,7,false,31_000_000_000L);
      require(playback.poll(31_000_000_000L)==0,"menus, invalid targets, recording and disable stop playback");
      playback.prepare(expected,8,true,31_000_000_000L);
      require(playback.poll(31_000_000_000L)==0,"a different target starts a fresh sequence without old click debt");
   }

   private static void oneTickAttackRotation() {
      var pulse = new OneTickAttackRotation();
      var look = new TransmittedRotation(); look.reset(0,0);
      require(pulse.update(7,100,false)==OneTickAttackRotation.Step.WAIT,"cooldown/crit/range gates keep One Tick idle");
      require(pulse.update(7,101,true)==OneTickAttackRotation.Step.AIM,"a due attack prepares a native aim tick first");
      look.record(new ServerboundMovePlayerPacket.Rot(90,10,true,false),true,7,90,10);
      pulse.transmitted(101,look.targetId());
      require(pulse.update(7,102,true)==OneTickAttackRotation.Step.AIM,"a cancelled look packet cannot authorize an attack");
      look.record(new ServerboundMovePlayerPacket.Rot(0,0,true,false),false,7,90,10);
      pulse.transmitted(102,look.targetId());
      require(pulse.update(7,103,true)==OneTickAttackRotation.Step.AIM,"a look overwritten by another module cannot authorize an attack");
      look.record(new ServerboundMovePlayerPacket.Rot(90,10,true,false),false,7,90,10);
      pulse.transmitted(103,look.targetId());
      require(pulse.update(7,104,true)==OneTickAttackRotation.Step.ATTACK,"the following tick may use the actually transmitted target look");
      require(pulse.update(7,105,true)==OneTickAttackRotation.Step.AIM,"a second attack requires a fresh pulse instead of reusing old aim");
      pulse.transmitted(105,7);
      require(pulse.update(8,106,true)==OneTickAttackRotation.Step.AIM,"switching targets cannot consume the previous enemy's pulse");
      pulse.transmitted(106,8);
      require(pulse.update(8,108,true)==OneTickAttackRotation.Step.AIM,"a skipped tick expires the prepared attack");
      pulse.transmitted(108,8);
      require(pulse.update(8,109,false)==OneTickAttackRotation.Step.WAIT,"losing the crit/range window cancels a transmitted pulse");
      require(pulse.update(-1,110,true)==OneTickAttackRotation.Step.WAIT,"no target releases the rotation");
      pulse.update(8,111,true); pulse.transmitted(111,8); pulse.reset();
      require(pulse.update(8,112,true)==OneTickAttackRotation.Step.AIM,"disable, world changes, corrections and Scaffold ownership invalidate old aim");
      int aimTicks=0,attacks=0,nextDue=0;
      pulse.reset();
      for(int tick=0;tick<160;tick++) {
         var step=pulse.update(7,tick,tick>=nextDue);
         if(step==OneTickAttackRotation.Step.AIM) { aimTicks++; pulse.transmitted(tick,7); }
         else if(step==OneTickAttackRotation.Step.ATTACK) { attacks++; nextDue=tick+6; }
         else require(tick<nextDue,"the camera is free between scheduled attack pulses");
      }
      require(attacks>=20&&aimTicks==attacks,"repeated combat transmits exactly one aim tick for each attack opportunity");
   }

   private static void polarRotations() {
      for(int seed=0;seed<5;seed++) for(double sensitivity:new double[]{0,.5,1}) {
         var polar=new PolarRotation(new java.util.Random(seed));
         AABB box=new AABB(-.3,0,2.7,.3,1.8,3.3);
         Vec3 eyes=new Vec3(0,1.62,0);
         float yaw=540,pitch=-35;
         Set<Vec3> points=new HashSet<>(); Set<Integer> steps=new HashSet<>();
         for(int tick=0;tick<100;tick++) {
            Vec3 point=polar.aimPoint(box,1.62,7,tick); points.add(point);
            require(box.deflate(.005).contains(point)&&Math.abs(point.y-1.62)<.05,"Polar randomization stays inside the eye-level head area");
            require(point.equals(polar.aimPoint(box,1.62,7,tick)),"head drift is stable within a native tick");
            Vec3 delta=point.subtract(eyes);
            float desiredYaw=(float)Math.toDegrees(Math.atan2(delta.z,delta.x))-90;
            float desiredPitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z)));
            var step=polar.step(yaw,pitch,desiredYaw,desiredPitch,2,6,7,tick);
            require(Math.abs(step.yaw()-yaw)<=24.001&&Math.abs(step.pitch()-pitch)<=12.001,"Polar respects configured speed limits and continuous yaw");
            var quantized=SilentRotationMath.quantizeRotation(yaw,pitch,step.yaw(),step.pitch(),sensitivity);
            steps.add(Math.round((quantized.yaw()-yaw)*1000));
            yaw=quantized.yaw(); pitch=quantized.pitch();
            require(Float.isFinite(yaw)&&Float.isFinite(pitch)&&Math.abs(pitch)<=90,"Polar remains finite and inside native pitch bounds");
            if(tick>=50) require(box.clip(eyes,eyes.add(Vec3.directionFromRotation(pitch,yaw).scale(4.5))).isPresent(),
               "randomized tracking converges to an actual target ray rather than orbiting outside the hitbox");
         }
         require(points.size()>50&&steps.size()>10,"Polar varies its head samples and turn steps across combat");
         polar.reset();
         var first=polar.step(720,89,-179,-89,6,2,8,0);
         require(Math.abs(first.yaw()-720)<=24.001&&Math.abs(first.pitch())<=90,"reset and reversed slider bounds cannot snap across 360 degrees");
      }
   }

   private static void transmittedAttackLook() {
      TransmittedRotation look = new TransmittedRotation();
      look.reset(720, 15);
      require(look.targetId() == -1, "initial camera baseline cannot authorize an attack");
      look.record(new ServerboundMovePlayerPacket.Rot(750, 30, true, false), true, 42, 750, 30);
      require(look.yaw() == 720 && look.pitch() == 15 && look.targetId() == -1,
         "cancelled movement cannot become an assumed transmitted attack look");
      look.record(new ServerboundMovePlayerPacket.Rot(750, 30, true, false), false, 42, 750, 30);
      require(look.targetId() == 42 && look.yaw() == 750, "an outgoing aimed look belongs to its prepared target");
      look.record(new ServerboundMovePlayerPacket.Pos(1, 64, 2, true, false), false, 42, 750, 30);
      require(look.targetId() == 42 && look.pitch() == 30, "position-only movement retains the preceding look");
      look.record(new ServerboundMovePlayerPacket.StatusOnly(true, false), false, 43, 770, 30);
      require(look.targetId() == -1, "an unsent target rotation cannot be assumed after a status-only packet");
      look.record(new ServerboundMovePlayerPacket.Rot(760, 40, true, false), false, 42, 750, 30);
      require(look.targetId() == -1 && look.yaw() == 760, "another module's overwritten look cannot authorize an attack");
      look.record(new ServerboundMovePlayerPacket.Rot(30, 30, true, false), false, 42, 750, 30);
      require(look.targetId() == 42 && look.yaw() == 30, "equivalent turns retain the actual transmitted numeric baseline");
      look.record(new ServerboundMovePlayerPacket.Rot(90, 80, true, false), false, -1, 90, 80);
      require(look.targetId() == -1, "a scaffold or ordinary camera tick releases the combat look");
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

   private static void backwardBridge() {
      Input forward = new Input(true, false, false, false, false, false, false);
      Input idle = new Input(false, false, false, false, false, false, false);
      for (int heading = 0; heading < 360; heading += 15) {
      for (double sensitivity : new double[]{0, 0.5, 1}) {
      for (int ackDelay : new int[]{1, 3, 8}) {
         BackwardBridgeRotation rotation = new BackwardBridgeRotation();
         FakeWorld world = new FakeWorld();
         world.allowPredictedSupports = true;
         world.solid.add(BlockPos.ZERO);
         Vec3 feet = new Vec3(0.5, 1, 0.5);
         Vec3 requested = new Vec3(-Math.sin(Math.toRadians(heading)) * 0.30, 0,
            Math.cos(Math.toRadians(heading)) * 0.30);
         float sentYaw = heading, sentPitch = 0;
         boolean primed = false;
         int placed = 0;

         int unsupportedTicks = 0;
         for (int tick = 0; tick < 180; tick++) {
            world.confirmations.acknowledge(tick - ackDelay);
            Vec3 eyes = feet.add(0, 1.62, 0);
            var candidates = PlacementPlanner.catchTargets(feet, requested, 0, false, false, 0.08);
            var backward = rotation.next(forward, heading, sentYaw, sentPitch, 82, sensitivity);
            float pitch = PlacementPlanner.backwardPitch(candidates, eyes, 4.5, backward.yaw(), sentPitch, sensitivity, world);
            var look = rotation.next(forward, heading, sentYaw, sentPitch, pitch, sensitivity);
            var placement = PlacementPlanner.findAlongLook(candidates, eyes, 4.5, sentYaw, sentPitch, world);
            if (placement != null && primed) look = new SilentRotationMath.Rotation(sentYaw, sentPitch);
            primed |= Math.abs(net.minecraft.util.Mth.wrapDegrees(rotation.heading(forward, heading) - look.yaw())) < 1
               && look.pitch() >= 70;
            require(Math.abs(net.minecraft.util.Mth.wrapDegrees(look.yaw() - sentYaw)) < 63.4,
               "backward turns advance continuously instead of snapping at placement");
            require(Math.abs(look.pitch() - sentPitch) < 10.4, "pitch changes are gradual at bridge corners");
            // Match native input/action order: click from the known position
            // and transmitted look, physics, then the normal movement packet.
            boolean clicked = placement != null && PlacementPlanner.lookMatchesSupport(placement, eyes, 4.5,
               look.yaw(), look.pitch(), world);
            Vec3 clickEyes = eyes;
            if (clicked) {
               world.solid.add(placement.target());
               world.confirmations.predict(placement.target(), tick);
               placed++;
            }
            if (primed) feet = feet.add(Vec3.directionFromRotation(0, look.yaw()).scale(-0.30));
            world.playerBox = new AABB(feet.x - 0.3, feet.y, feet.z - 0.3, feet.x + 0.3, feet.y + 1.8, feet.z + 0.3);
            eyes = feet.add(0, 1.62, 0);
            candidates = PlacementPlanner.catchTargets(feet, requested, 0, false, false, 0.08);
            pitch = PlacementPlanner.backwardPitch(candidates, eyes, 4.5, look.yaw(), sentPitch, sensitivity, world);
            float refinedPitch = SilentRotationMath.quantizeRotation(sentYaw, sentPitch, look.yaw(),
               look.pitch() + net.minecraft.util.Mth.clamp(pitch - look.pitch(), -10, 10), sensitivity).pitch();
            sentYaw = look.yaw();
            sentPitch = !clicked || PlacementPlanner.lookIntersectsClickedSupport(placement, clickEyes, 4.5,
               sentYaw, refinedPitch) ? refinedPitch : look.pitch();
            // Stepping beyond an edge initially has zero vertical velocity.
            // A reachable click at this position must be ready before the next
            // physics tick, when falling would otherwise start.
            var nextClick = PlacementPlanner.findAlongLook(candidates, eyes, 4.5, sentYaw, sentPitch, world);
            unsupportedTicks = hasFooting(feet, world) ? 0 : unsupportedTicks + 1;
            require(unsupportedTicks <= 1, "prepare the next click before a second unsupported physics tick");
            require(hasFooting(feet, world) || nextClick != null, "backward bridging without sneak preserves footing: heading=" + heading
               + " sensitivity=" + sensitivity + " tick=" + tick + " feet=" + feet + " pitch=" + sentPitch);
         }
         require(placed >= 8, "held look must keep placing: heading=" + heading + " sensitivity=" + sensitivity
            + " delay=" + ackDelay + " placed=" + placed + " feet=" + feet);
         float heldYaw = sentYaw, heldPitch = sentPitch;
         var held = rotation.next(idle, heading + 90, sentYaw, sentPitch, sentPitch, sensitivity);
         require(held.yaw() == heldYaw && held.pitch() == heldPitch,
            "idle gaps retain the bridge look even when the camera moves");
      }
      }
      }
   }

   private static void jumpingBridgeHeight() {
      FakeWorld world = new FakeWorld();
      world.allowPredictedSupports = true;
      world.solid.add(BlockPos.ZERO);
      Vec3 feet = new Vec3(0.5, 2.05, 0.5);
      world.playerBox = new AABB(0.2, feet.y, 0.2, 0.8, feet.y + 1.8, 0.8);
      Vec3 eyes = feet.add(0, 1.62, 0);
      int freeLayer = PlacementPlanner.bridgeLayer(feet.y, 0, false);
      int lockedLayer = PlacementPlanner.bridgeLayer(feet.y, 0, true);
      require(freeLayer == 1 && lockedLayer == 0, "only KeepY locks a jumping bridge to its original height");
      var raised = PlacementPlanner.findAlongLook(
         PlacementPlanner.catchTargets(feet, Vec3.ZERO, freeLayer, true, true, 0.08), eyes, 4.5, 180.5F, 82, world);
      require(raised != null && raised.target().equals(new BlockPos(0, 1, 0)),
         "a normal jump can place the raised block after the feet clear it");
      require(PlacementPlanner.findAlongLook(
         PlacementPlanner.catchTargets(feet, Vec3.ZERO, lockedLayer, true, false, 0.08), eyes, 4.5, 180.5F, 82, world) == null,
         "KeepY forbids the same raised placement");
      require(PlacementPlanner.bridgeLayer(1.75, 0, false) == 0,
         "ascending feet must clear a full block before raising the placement layer");
   }

   private static void backwardTurnEasing() {
      var rotation = new BackwardBridgeRotation();
      Input forward = new Input(true, false, false, false, false, false, false);
      float yaw = 0, pitch = 0, previousDelta = 180;
      for (int tick = 0; tick < 10; tick++) {
         var next = rotation.next(forward, 0, yaw, pitch, 82, 0.5);
         float delta = Math.abs(net.minecraft.util.Mth.wrapDegrees(next.yaw() - yaw));
         require(delta < previousDelta, "a turn eases toward its heading rather than repeating fixed yaw deltas");
         previousDelta = delta;
         yaw = next.yaw();
         pitch = next.pitch();
      }
   }

   private static void legacyInputEncoding() {
      Input forward = new Input(true, false, false, false, false, false, false);
      var diagonal = SilentRotationMath.correctMovement(forward, 0, 45);
      require(diagonal.sideways() != 0 && diagonal.forward() != 0,
         "straight camera input can require diagonal corrected input");
      for (int sideways = -1; sideways <= 1; sideways++) {
         for (int ahead = -1; ahead <= 1; ahead++) {
            Input keys = new Input(ahead > 0, ahead < 0, sideways > 0, sideways < 0, false, false, false);
            var corrected = SilentRotationMath.correctMovement(keys, 0, 0);
            Vec2 legacy = corrected.inputVector(false);
            Vec2 modern = corrected.inputVector(true);
            require(legacy.x == sideways && legacy.y == ahead,
               "legacy keyboard vectors retain raw discrete components");
            require(Math.abs(Math.hypot(modern.x, modern.y) - (ahead == 0 && sideways == 0 ? 0 : 1)) < 0.000001,
               "modern keyboard vectors normalize diagonals before physics");
            for (float slowdown : new float[]{0.98F, 0.294F, 0.196F}) {
               Vec3 rawAfterSlowdown = new Vec3(sideways * slowdown, 0, ahead * slowdown);
               Vec3 expected = rawAfterSlowdown.lengthSqr() > 1 ? rawAfterSlowdown.normalize() : rawAfterSlowdown;
               Vec3 actual = new Vec3(legacy.x * slowdown, 0, legacy.y * slowdown);
               if (actual.lengthSqr() > 1) actual = actual.normalize();
               require(expected.distanceTo(actual) < 0.0000001,
                  "legacy movement matches native slowdown-before-normalization for all keys");
            }
         }
      }
      Vec2 rawDiagonal = diagonal.inputVector(false);
      Vec2 normalizedDiagonal = diagonal.inputVector(true);
      require(Math.abs(rawDiagonal.x) == 1 && Math.abs(rawDiagonal.y) == 1,
         "straight-to-diagonal correction also uses legacy encoding");
      require(Math.hypot(normalizedDiagonal.x * 0.98F, normalizedDiagonal.y * 0.98F) < 0.981,
         "early normalization reproduces the incorrect two-percent diagonal slowdown");
      require(new Vec3(rawDiagonal.x * 0.98F, 0, rawDiagonal.y * 0.98F).normalize().length() > 0.999,
         "legacy diagonal acceleration remains full length after native normalization");
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
      Vec3 aimEyes;
      float aimYaw, aimPitch;
      double aimSensitivity;
      boolean allowPredictedSupports;
      boolean requireConfirmedLanding;
      boolean nativeRaycast;
      AABB playerBox;

      SilentRotationMath.Rotation aimRotation(PlacementPlanner.Placement placement) {
         Vec3 delta = placement.hit().getLocation().subtract(this.aimEyes);
         float yaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
         float pitch = (float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
         return SilentRotationMath.quantizeRotation(this.aimYaw, this.aimPitch, yaw, pitch, this.aimSensitivity);
      }

      public boolean rotationValid(PlacementPlanner.Placement placement) {
         if (placement.target().getY() > this.maxRotationLayer) return false;
         if (this.aimEyes == null) return true;
         var rotation = this.aimRotation(placement);
         return PlacementPlanner.matchesSupport(placement, this.raycast(this.aimEyes,
            this.aimEyes.add(Vec3.directionFromRotation(rotation.pitch(), rotation.yaw()).scale(4.5))));
      }

      public boolean replaceable(BlockPos pos) {
         return this.replaceableAt(pos,this.playerBox);
      }

      public boolean replaceableAt(BlockPos pos, AABB playerBox) {
         return !this.solid.contains(pos) && (playerBox == null || !playerBox.intersects(new AABB(pos)));
      }

      public boolean supportsLanding(BlockPos pos) {
         return this.solid.contains(pos) && (!this.requireConfirmedLanding || !this.confirmations.isPending(pos));
      }

      public boolean attachable(BlockPos pos, Direction face) {
         return this.solid.contains(pos) && (this.allowPredictedSupports || !this.confirmations.isPending(pos));
      }

      public BlockHitResult raycast(Vec3 from, Vec3 to) {
         if (this.nativeRaycast || this.allowPredictedSupports) {
            // Use Minecraft's voxel traversal for exact diagonal corner ties,
            // just like ClientLevel.clip in the live placement path.
            return net.minecraft.world.level.BlockGetter.traverseBlocks(from, to, this,
               (world, pos) -> world.solid.contains(pos)
                  ? net.minecraft.world.phys.shapes.Shapes.block().clip(from, to, pos) : null,
               world -> null);
         }
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
