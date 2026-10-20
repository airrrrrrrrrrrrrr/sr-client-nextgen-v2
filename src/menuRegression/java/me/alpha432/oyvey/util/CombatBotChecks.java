package me.alpha432.oyvey.util;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode.BlockMode;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CombatBotChecks {
   private static int checks;

   public static void main(String[] args) {
      confirmedHits(); movement(); proBlocks(); adaptiveAim(); totemDelay(); inventorySession(); smoothTurning(); preSwing(); polarResponse(); knockbackDelay(); autoBlockModes(); instantPvpAim();
      System.out.println("PvP combo, Pro, Adaptive and AutoTotem checks passed: " + checks);
   }

   private static void confirmedHits() {
      CombatHitWindow hits = new CombatHitWindow();
      require(!hits.observe(1, 7, 10), "an unrelated hurt animation is not a local hit");
      hits.attack(2, 7, 9);
      require(!hits.observe(3, 7, 8), "decaying immunity cannot count as a hit");
      require(hits.observe(4, 7, 10), "fresh damage shortly after our attack confirms once");
      require(!hits.observe(4, 7, 10), "two observers in one tick cannot double count");
      require(!hits.observe(5, 7, 9), "the same damage animation does not repeat");
      hits.attack(6, 7, 9);
      require(!hits.observe(7, 8, 10), "changing targets discards previous hit credit");
      hits.attack(8, 8, 10);
      require(!hits.observe(15, 8, 10), "expired attempts never count delayed unrelated damage");
      hits.attack(20, 8, 0);
      require(!hits.observe(1, 8, 10), "a new world tick clock cannot credit the old attack");
      hits.reset();
      require(!hits.observe(21, 8, 10), "disabling removes pending hits");
   }

   private static PvpCombo.Move decide(PvpCombo combo, int tick, double distance, double closing, int hurt, boolean left, boolean right) {
      return combo.decide(tick, 7, distance, 2.6, closing, hurt, true, true, left, right);
   }

   private static void movement() {
      PvpCombo combo = new PvpCombo(new Random(4));
      var approach = decide(combo, 1, 8, 0, 0, true, true);
      require(approach.forward() == 1 && approach.sprint() && approach.sideways() == 0, "approach uses native forward sprint without circling at long range");
      require(decide(combo, 2, 2.6, 0, 0, true, true).forward() == 0, "hold spacing at the configured distance");
      require(decide(combo, 3, 2.2, 0, 0, true, true).forward() == -1, "back away instead of running through the opponent");
      require(decide(combo, 4, 2.7, 0.4, 0, true, true).forward() == -1, "closing momentum anticipates the spacing boundary");
      combo.confirmedHit(5, true);
      var reset = decide(combo, 5, 3.2, 0, 0, true, true);
      require(reset.forward() == 0 && !reset.sprint() && reset.phase() == PvpCombo.Phase.RESET, "a confirmed hit releases forward and sprint");
      var resume = decide(combo, 6, 3.2, 0, 0, true, true);
      require(resume.forward() == 1 && resume.sprint(), "the reset lasts one native input tick");
      require(decide(combo, 7, 2.6, 0, 0, false, false).sideways() == 0, "no strafing when neither side has support");
      require(decide(combo, 8, 2.6, 0, 0, true, false).sideways() == 1, "use the supported strafe side");
      require(decide(combo, 9, 2.6, 0, 0, false, true).sideways() == -1, "switch away from an exposed edge");
      require(decide(combo, 10, 2.8, 0, 10, true, true).phase() == PvpCombo.Phase.RECOVER && combo.combo() == 0,
         "taking damage breaks the combo without inventing an airborne jump reset");
      combo.confirmedHit(11, false);
      require(combo.decide(11, 7, 3.2, 2.6, 0, 9, true, false, true, true).forward() == 1,
         "disabling sprint resets preserves forward pressure");
      require(combo.decide(12, 8, 3.2, 2.6, 0, 0, true, true, true, true).phase() != PvpCombo.Phase.RESET
         && combo.combo() == 0, "retargeting clears the old combo and reset");
      Random random = new Random(7);
      for (int tick = 20; tick < 320; tick++) {
         boolean left = random.nextBoolean(), right = random.nextBoolean();
         var move = decide(combo, tick, 0.7 + random.nextDouble() * 7, random.nextDouble() - 0.5, 0, left, right);
         require(Math.abs(move.forward()) <= 1 && Math.abs(move.sideways()) <= 1, "movement remains a legal keyboard tuple");
         require(!move.sprint() || move.forward() == 1, "sprint is never requested backwards or while stationary");
         require(move.sideways() != 1 || left, "left input respects support");
         require(move.sideways() != -1 || right, "right input respects support");
      }
   }

   private static void proBlocks() {
      ProBlockTiming block = new ProBlockTiming();
      require(!block.update(0, 7, 0, true, true), "target acquisition and air swings cannot start a block");
      block.attack(1, 7, 0, true);
      require(block.update(1, 7, 0, true, true), "legacy sword blocks immediately after a validated melee attack");
      require(block.update(2, 7, 0, true, true), "delayed or absent hurt animation cannot suppress the legacy block");
      require(!block.update(3, 7, 10, true, true), "late damage cannot extend the two tick block or restart it");
      require(!block.update(5, 7, 8, true, true), "release rather than permanently blocking");
      block.attack(6, 7, 8, true);
      require(block.update(7, 7, 10, true, true), "the next melee attack can block again");
      block.beforeAttack();
      require(!block.blocking(7), "the next attack always lowers the block first");
      block.attack(8, 7, 9, true);
      require(!block.update(9, 8, 10, true, true), "retargeting cannot inherit a block");
      block.attack(10, 8, 0, false);
      require(!block.update(10, 8, 0, true, false), "modern shield still waits for damage confirmation");
      require(block.update(11, 8, 10, true, false), "modern mode raises an actual shield");
      require(block.update(16, 8, 5, true, false), "modern shield hold extends past its five-tick activation delay");
      require(!block.update(18, 8, 3, true, false), "modern hold still has a finite end");
      block.attack(19, 8, 0, true);
      require(!block.update(20, 8, 10, false, true), "menu, item, range and world eligibility cancel pending block state");
      require(!block.update(21, 8, 10, true, true), "resuming cannot replay stale damage");
      block.attack(25, 8, 0, true);
      require(!block.update(1, 8, 0, true, true), "world tick rollback cannot keep a future block alive");
      require(!block.update(25, 8, 0, true, true), "reset block cannot reappear when the tick clock catches up");
   }

   private static void autoBlockModes() {
      for (BlockMode mode:BlockMode.values()) {
         BlockMode legacy=mode.forProtocol(true), modern=mode.forProtocol(false);
         require(legacy==(mode==BlockMode.SHIELD?BlockMode.FULL2:mode),"1.8 retains each block mode's intended sword path");
         if (mode==BlockMode.OFF) require(modern==BlockMode.OFF,"Off cannot raise a shield");
         else if (mode==BlockMode.PRO || mode==BlockMode.LEGIT) require(modern==mode,"modern Pro and Legit retain their shield timing");
         else require(modern==BlockMode.SHIELD,"every other enabled modern mode uses shield actions");
         require(mode.swordVisual(true,false,false),"a fake overlay can render for every selected packet mode");
      }
      require(BlockMode.PACKET.continuousSwordBlock() && BlockMode.VANILLA.continuousSwordBlock(),
         "Packet and Vanilla must share continuous sword ownership to avoid automatic releases");
      require(BlockMode.FAKE.swordVisual(false,false,false),"the standalone Fake mode renders without a real use-item action");
      require(BlockMode.LEGIT.swordVisual(false,false,true) && BlockMode.PRO.swordVisual(false,false,true),
         "actively using a sword cannot hide its blocking animation");
      require(!BlockMode.OFF.swordVisual(false,false,false),"no selected block, held action or fake overlay leaves normal rendering");
      for (String material:new String[]{"wooden","stone","copper","iron","golden","diamond","netherite"})
         require(CombatItems.vanillaSwordId("minecraft",material+"_sword"),"vanilla swords survive missing legacy item tags");
      require(!CombatItems.vanillaSwordId("minecraft","diamond_axe") && !CombatItems.vanillaSwordId("other","diamond_sword"),
         "fallback recognition does not treat unrelated untagged items as swords");
   }

   private static void instantPvpAim() {
      for (double sensitivity:new double[]{0,.5,1}) for (int heading=-180;heading<180;heading+=5) {
         var snapped=PvpRotation.instant(720,-65,heading,35,0,0,sensitivity);
         require(Math.abs(net.minecraft.util.Mth.wrapDegrees(snapped.yaw()-heading))<.7F && Math.abs(snapped.pitch()-35)<.7F,
            "PvP aim reaches the target in one update instead of accelerating over multiple ticks");
         var jittered=PvpRotation.instant(720,-65,heading,35,1.25F,-.65F,sensitivity);
         require(Math.abs(net.minecraft.util.Mth.wrapDegrees(jittered.yaw()-heading))<2 && Math.abs(jittered.pitch()-35)<1.5F,
            "instant jitter remains small after native mouse quantization");
      }
      var clamped=PvpRotation.instant(0,0,180,100,50,-50,.5);
      require(Math.abs(clamped.pitch())<=90,"instant jitter cannot exceed native pitch limits");
   }

   private static void polarResponse() {
      PolarRotation slow = new PolarRotation(new Random(19)), fast = new PolarRotation(new Random(19));
      var slowStart = slow.step(0,0,150,60,6,6,.1F,.6F,7,0);
      var fastStart = fast.step(0,0,150,60,6,6,1F,.6F,7,0);
      require(fastStart.yaw() > slowStart.yaw() * 2 && fastStart.pitch() > slowStart.pitch() * 2,
         "higher acceleration measurably speeds up initial yaw and pitch acquisition");
      PolarRotation gentle = new PolarRotation(new Random(19)), firm = new PolarRotation(new Random(19));
      float yaw=0, pitch=0;
      for (int tick=0;tick<4;tick++) {
         var step=gentle.step(yaw,pitch,yaw+150,60,6,6,1F,.1F,7,tick);
         firm.step(yaw,pitch,yaw+150,60,6,6,1F,1F,7,tick);
         yaw=step.yaw(); pitch=step.pitch();
      }
      var coast=gentle.step(yaw,pitch,yaw+10,pitch+1,6,6,1F,.1F,7,4);
      var brake=firm.step(yaw,pitch,yaw+10,pitch+1,6,6,1F,1F,7,4);
      require(brake.yaw()-yaw < coast.yaw()-yaw,
         "higher deceleration brakes an existing fast turn sooner near the aim point");
      require(coast.yaw() <= yaw+10 && brake.yaw() <= yaw+10 && Math.abs(brake.pitch()) <= 90,
         "both response extremes retain target overshoot and pitch bounds");
      var instant=new PolarRotation(new Random(19)).step(720,85,-179,-200,2,6,10F,.01F,7,0);
      require(Math.abs(net.minecraft.util.Mth.wrapDegrees(instant.yaw()+179))<.0001F && instant.pitch()==-90,
         "acceleration 1000 aligns immediately regardless of minimum speed or deceleration");
      require(Math.abs(instant.yaw()-720)<=180,"instant Polar still uses continuous shortest-path yaw");
      var boosted=new PolarRotation(new Random(19)).step(0,0,150,60,6,6,2F,.6F,7,0);
      require(boosted.yaw()>fastStart.yaw(),"acceleration above 100 increases acquisition speed instead of saturating the slider");
   }

   private static void knockbackDelay() {
      KnockbackDelay<Object> delay=new KnockbackDelay<>();
      Object first=new Object(), second=new Object();
      require(delay.offer(first,0,10,0,false)==null,"zero delay leaves ordinary velocity on vanilla's immediate path");
      var queued=delay.offer(first,0,10,1000,false);
      require(queued!=null && delay.contains(first),"a local velocity can be retained without applying it");
      delay.observeDamage(11);
      require(delay.drain(999_999_999L,false).isEmpty(),"knockback never releases before its millisecond deadline");
      var ready=delay.drain(1_000_000_000L,false);
      require(ready.size()==1 && ready.getFirst().packet==first && ready.getFirst().damaged(),
         "one-second delay preserves the original damage association after hurt animation expiry");
      JumpResetInput jump=new JumpResetInput();
      jump.arm(30,true,true);
      require(jump.consume(30,true,ready.getFirst().damaged(),false),"replayed combat velocity can trigger the existing native jump reset");
      require(!jump.consume(30,true,true,false),"replaying does not duplicate the jump pulse");
      require(delay.drain(2_000_000_000L,false).isEmpty(),"a queued velocity is delivered exactly once");
      delay.offer(first,0,10,500,false);
      delay.offer(second,100_000_000L,12,0,false);
      require(delay.drain(499_999_999L,false).isEmpty(),"lowering delay to zero cannot overtake an older knockback packet");
      ready=delay.drain(500_000_000L,false);
      require(ready.size()==2 && ready.get(0).packet==first && ready.get(1).packet==second,
         "velocity packets retain wire order across slider changes");
      delay.offer(first,0,10,1000,false);
      delay.observeDamage(13);
      require(!delay.drain(1_000_000_000L,false).getFirst().damaged(),"unrelated later damage is not credited to old velocity");
      delay.offer(first,0,10,1000,false); delay.offer(second,1,10,1000,false);
      require(delay.drain(2,true).size()==2,"mode changes and disabling can flush retained velocity immediately");
      delay.offer(first,0,10,1000,false); delay.clear();
      require(delay.drain(2_000_000_000L,true).isEmpty(),"world changes and corrections discard stale knockback");
      for(int i=0;i<128;i++) require(delay.offer(new Object(),0,10,1000,false)!=null,"normal bounded queue retains packets");
      require(delay.offer(first,0,10,1000,false)==null && delay.size()==128,
         "capacity fallback leaves the newest packet to vanilla instead of losing it");
      require(delay.drain(0,true).size()==128,"capacity fallback can flush every older packet in order");
   }

   private static Predicate<Vec3> visible(Vec3 eyes, AABB obstacle) {
      return point -> !obstacle.contains(eyes) && obstacle.clip(eyes, point).isEmpty();
   }

   private static void adaptiveAim() {
      Vec3 eyes = new Vec3(0, 1.62, 0);
      AABB target = new AABB(3, 0, -0.3, 3.6, 1.8, 0.3);
      AtomicInteger rays = new AtomicInteger();
      Vec3 open = AdaptiveAim.bestPoint(eyes, target, point -> { rays.incrementAndGet(); return true; }).orElseThrow();
      require(open.distanceTo(eyes) < target.getCenter().distanceTo(eyes), "clear aim favors the nearest face instead of the distant center");
      require(Math.abs(open.y - eyes.y) < 0.0001 && Math.abs(open.z - eyes.z) < 0.0001, "unobstructed projection is perpendicular to the nearest face");
      require(rays.get() == 1, "clear sight needs one visibility check");
      Vec3 delta = open.subtract(eyes);
      Vec3 entry = target.clip(eyes, eyes.add(delta.normalize().scale(3.01))).orElseThrow();
      require(Math.abs(eyes.distanceTo(entry) - 3) < 0.00001, "the chosen ray still reaches a target at the exact range boundary");
      AABB upperWall = new AABB(2.8, 0.4, -1, 2.9, 3, 1);
      Vec3 foot = AdaptiveAim.bestPoint(eyes, target, visible(eyes, upperWall)).orElseThrow();
      require(foot.y < 0.4 && visible(eyes, upperWall).test(foot), "feet-only exposure is aimable when the torso and head are hidden");
      AABB lowerWall = new AABB(2.8, -1, -1, 2.9, 1.45, 1);
      Vec3 head = AdaptiveAim.bestPoint(eyes, target, visible(eyes, lowerWall)).orElseThrow();
      require(head.y > 1.45 && visible(eyes, lowerWall).test(head), "head exposure above a low wall stays visible");
      AABB sideWall = new AABB(2.8, -1, -0.05, 2.9, 3, 1);
      Vec3 side = AdaptiveAim.bestPoint(eyes, target, visible(eyes, sideWall)).orElseThrow();
      require(side.z < -0.05 && visible(eyes, sideWall).test(side), "lateral opening is used when the middle is blocked");
      rays.set(0);
      require(AdaptiveAim.bestPoint(eyes, target, point -> { rays.incrementAndGet(); return false; }).isEmpty(), "fully hidden targets produce no visible point");
      require(rays.get() <= 181, "fully blocked scan has a fixed ray budget");
      Vec3 above = new Vec3(3.3, 4, 0);
      Vec3 top = AdaptiveAim.bestPoint(above, target, p -> true).orElseThrow();
      require(top.y < target.maxY && top.y > target.maxY - 0.01, "aim from above uses the top face");
      require(AdaptiveAim.bestPoint(target.getCenter(), target, p -> true).isPresent(), "overlapping hitboxes remain finite");
      for (int rotation = 0; rotation < 8; rotation++) {
         double angle = rotation * Math.PI / 4;
         Vec3 eye = new Vec3(3.3 + Math.sin(angle) * 5, 1.62, Math.cos(angle) * 5);
         Vec3 point = AdaptiveAim.bestPoint(eye, target, p -> true).orElseThrow();
         require(target.contains(point), "all approach headings return an interior point");
         var raw = eye.subtract(point);
         require(Double.isFinite(raw.length()) && raw.length() > 0, "adaptive angle stays finite at every heading");
      }
      Vec3 shifted = new Vec3(-100, 20, -100);
      require(AdaptiveAim.bestPoint(eyes.add(shifted), target.move(shifted), p -> true).orElseThrow()
         .distanceTo(open.add(shifted)) < 0.000001, "world coordinates do not change the selection");
   }

   private static void require(boolean value, String message) {
      checks++;
      if (!value) throw new AssertionError(message);
   }

   private static void smoothTurning() {
      PvpRotation turning = new PvpRotation();
      var first = turning.step(0, 0, 90, 20, 60, 0.5);
      require(first.yaw() > 0 && first.yaw() < 4.2, "turning accelerates from the current camera instead of snapping sixty degrees");
      float yaw = first.yaw(), pitch = first.pitch(), lastDelta = first.yaw();
      for (int i = 0; i < 80; i++) {
         var next = turning.step(yaw, pitch, 90, 20, 60, 0.5);
         float delta = next.yaw() - yaw;
         require(delta >= -0.16 && next.yaw() <= 90.16, "turning brakes without crossing the target");
         require(Math.abs(delta - lastDelta) <= 4.4, "turn acceleration remains bounded through acquisition and braking");
         require(next.pitch() >= -0.16 && next.pitch() <= 20.16, "pitch remains inside the intended turn");
         yaw = next.yaw(); pitch = next.pitch(); lastDelta = delta;
      }
      require(Math.abs(yaw - 90) < 0.16 && Math.abs(pitch - 20) < 0.16, "smoothing converges closely enough for actual hitbox tracking");
      turning.reset();
      var wrapped = turning.step(179, 0, -179, 0, 60, 0.5);
      require(wrapped.yaw() >= 179 && wrapped.yaw() < 182, "turning across the yaw boundary takes the short continuous path");
      AABB box = new AABB(3, 0, -0.3, 3.6, 1.8, 0.3);
      Vec3 still = PvpRotation.trackingPoint(box, 1.62, Vec3.ZERO, Vec3.ZERO);
      require(box.contains(still) && still.y > 1.5, "idle tracking aims inside the lower head");
      Vec3 led = PvpRotation.trackingPoint(box, 1.62, new Vec3(0, 0, 0.25), new Vec3(0.25, 0, 0));
      require(led.x < still.x && led.z > still.z && box.contains(led), "tracking compensates both the opponent's motion and our own approach");
      for (int i = 0; i < 100; i++) {
         Vec3 point = PvpRotation.trackingPoint(box, 1.62, new Vec3(i, i - 50, -i), new Vec3(-i, 0, i));
         require(box.contains(point), "fast motion cannot push the tracking point outside the real hitbox");
      }
   }

   private static void preSwing() {
      require(!CombatPreSwing.inRange(2.9 * 2.9, 3, 10), "pre-swing never replaces the real in-reach attack path");
      require(CombatPreSwing.inRange(3.01 * 3.01, 3, 4), "swing can begin before attack reach");
      require(CombatPreSwing.inRange(100, 3, 10), "ten-block upper slider boundary is inclusive");
      require(!CombatPreSwing.inRange(100.01, 3, 10), "no swing past the configured pre-hit range");
      require(!CombatPreSwing.inRange(Double.NaN, 3, 10), "invalid distance never schedules swings");
      CombatPreSwing clock = new CombatPreSwing();
      require(clock.poll(true, 7, 100_000_000, java.util.List.of(), false, 0) == 1, "first normal pre-swing begins on entry");
      require(clock.poll(true, 7, 100_000_000, java.util.List.of(), false, 50_000_000) == 0, "swing clock preserves its CPS interval");
      require(clock.poll(true, 7, 100_000_000, java.util.List.of(), false, 100_000_000) == 1, "next swing follows the CPS deadline");
      require(clock.poll(false, 7, 100_000_000, java.util.List.of(), false, 110_000_000) == 0, "entering melee reach or opening a GUI stops pre-swings");
      require(clock.poll(true, 7, 100_000_000, java.util.List.of(), false, 5_000_000_000L) == 1, "resuming cannot dump a backlog");
      var pattern = java.util.List.of(40_000_000L, 80_000_000L);
      clock.reset();
      require(clock.poll(true, 7, 0, pattern, true, 0) == 0, "recorded pre-swings get their own initial deadline");
      require(clock.poll(true, 7, 0, pattern, true, 50_000_000) == 1, "recorded pre-swing starts after one native aim interval");
      require(clock.poll(true, 7, 0, pattern, true, 90_000_000) == 1, "recorded pre-swing uses the first captured interval");
      require(clock.poll(true, 7, 0, pattern, true, 170_000_000) == 1, "recorded pre-swing uses the next captured interval");
      require(clock.poll(true, 8, 0, pattern, true, 180_000_000) == 0, "new targets do not inherit the previous swing deadline");
      RecordedClickPlayback attacks = new RecordedClickPlayback();
      attacks.prepare(pattern, 8, true, 180_000_000);
      clock.poll(true, 8, 0, pattern, true, 230_000_000);
      require(attacks.poll(230_000_000) == 1, "air swings cannot consume recorded melee clicks");
      require(clock.poll(true, 8, 0, java.util.List.of(), true, 240_000_000) == 0, "an empty recording cannot turn into arbitrary CPS clicks");
   }

   private static void totemDelay() {
      AutoTotemDelay delay = new AutoTotemDelay();
      require(delay.ready(1_000_000_000, 0, 9), "zero milliseconds is ready on the first eligible tick");
      delay.reset();
      require(!delay.ready(1_000_000_000, 50, 9), "delay starts when the swap becomes necessary");
      require(!delay.ready(1_049_999_999, 50, 9), "milliseconds do not round down to an earlier tick");
      require(delay.ready(1_050_000_000, 50, 9), "fifty milliseconds is exactly ready at its boundary");
      delay.reset();
      require(!delay.ready(2_000_000_000L, 1000, 9), "one second delay arms");
      require(!delay.ready(2_999_999_999L, 1000, 9), "one second is not ten ticks");
      require(delay.ready(3_000_000_000L, 1000, 9), "the full thousand millisecond range works");
      require(!delay.ready(3_000_000_001L, 1000, 10), "a different item slot restarts the wait");
      delay.reset();
      require(!delay.ready(4_000_000_000L, 1000, 9), "ineligible or cancelled work starts a fresh wait");
      require(delay.ready(4_100_000_000L, 100, 9), "editing the delay while waiting uses the new millisecond value");
      require(!delay.ready(1, 100, 9), "a reset clock cannot finish an old pending swap");
   }

   private static void inventorySession() {
      java.util.List<String> order = new java.util.ArrayList<>();
      require(InventorySwapSession.run(true, false,
         () -> order.add("swap"), () -> order.add("close")), "spoofed session can swap without a visible screen");
      require(order.equals(java.util.List.of("swap", "close")), "modern spoof sends a normal swap followed by close without a legacy open action");
      order.clear();
      require(!InventorySwapSession.run(false, false, () -> order.add("swap"), () -> order.add("close")),
         "without spoofing the real inventory screen is required");
      order.clear();
      InventorySwapSession.run(true, true, () -> order.add("swap"), () -> order.add("close"));
      require(order.equals(java.util.List.of("swap")), "never close the user's actual open inventory");
      order.clear();
      try {
         InventorySwapSession.run(true, false,
            () -> { order.add("swap"); throw new IllegalStateException("test"); }, () -> order.add("close"));
         throw new AssertionError("expected click failure");
      } catch (IllegalStateException expected) {
         require(order.equals(java.util.List.of("swap", "close")), "a failed click still closes an owned spoof session");
      }
      for (int slot = 0; slot < 36; slot++) require(InventorySwapSession.menuSlot(slot) == (slot < 9 ? 36 + slot : slot),
         "hotbar and inventory indices map to native player-menu slots");
   }
}
