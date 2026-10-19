package me.alpha432.oyvey.util;

import java.util.function.BiPredicate;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Mean native hook flight, excluding its small random launch spread. */
public final class RodLogic {
   private RodLogic() { }
   public enum Phase { READY, AIMING, FLYING, RETRIEVING, COOLDOWN }
   public record Shot(Vec3 point, float yaw, float pitch, int flightTicks) { }
   private record Crossing(double height, double ticks) { }

   public static Vec3 boundedMotion(Vec3 motion) {
      return Double.isFinite(motion.lengthSqr())
         ? new Vec3(Mth.clamp(motion.x, -0.45, 0.45), Mth.clamp(motion.y, -0.3, 0.3), Mth.clamp(motion.z, -0.45, 0.45)) : Vec3.ZERO;
   }

   public static Shot solve(Vec3 eyes, AABB box, Vec3 motion, boolean prediction) {
      Vec3 velocity = prediction ? boundedMotion(motion) : Vec3.ZERO;
      Vec3 center = new Vec3((box.minX + box.maxX) * 0.5, box.minY + box.getYsize() * 0.55, (box.minZ + box.maxZ) * 0.5);
      double time = Math.max(1, eyes.distanceTo(center) / 1.1);
      Vec3 lead = center;
      float pitch = 0;
      for (int iteration = 0; iteration < 5; iteration++) {
         lead = center.add(velocity.scale(time));
         double distance = Math.max(0.05, Math.hypot(lead.x - eyes.x, lead.z - eyes.z) - 0.3);
         double low = -70, high = 70;
         for (int search = 0; search < 24; search++) {
            double mid = (low + high) * 0.5;
            if (crossing(distance, mid).height > lead.y - eyes.y) low = mid;
            else high = mid;
         }
         pitch = (float)((low + high) * 0.5);
         time = Math.min(16, crossing(distance, pitch).ticks);
      }
      float yaw = (float)Math.toDegrees(Math.atan2(lead.z - eyes.z, lead.x - eyes.x)) - 90;
      Vec3 point = eyes.add(Vec3.directionFromRotation(pitch, yaw).scale(Math.max(1, eyes.distanceTo(lead))));
      return new Shot(point, yaw, pitch, Math.max(1, (int)Math.ceil(time)));
   }

   private static Crossing crossing(double distance, double pitch) {
      double horizontal = 0.6 * Math.cos(Math.toRadians(pitch)) + 0.5;
      double vertical = -Math.tan(Math.toRadians(pitch)) * horizontal;
      double travelled = 0, height = 0;
      for (int tick = 1; tick <= 24; tick++) {
         vertical -= 0.03;
         if (travelled + horizontal >= distance) {
            double fraction = (distance - travelled) / horizontal;
            return new Crossing(height + vertical * fraction, tick - 1 + fraction);
         }
         travelled += horizontal; height += vertical;
         horizontal *= 0.92; vertical *= 0.92;
      }
      return new Crossing(height, 24);
   }

   /** Returns the first intersecting tick, or -1 if this actual look misses/gets blocked. */
   public static int intercept(Vec3 eyes, float yaw, float pitch, AABB target, Vec3 motion,
      BiPredicate<Vec3, Vec3> clearPath) {
      double angle = Math.toRadians(yaw), horizontal = 0.6 * Math.cos(Math.toRadians(pitch)) + 0.5;
      Vec3 forward = new Vec3(-Math.sin(angle), 0, Math.cos(angle));
      Vec3 position = eyes.add(forward.scale(0.3));
      Vec3 velocity = forward.scale(horizontal).add(0, -Mth.clamp(Math.tan(Math.toRadians(pitch)), -5, 5) * horizontal, 0);
      Vec3 movement = boundedMotion(motion);
      AABB box = target.inflate(0.125);
      if (!clearPath.test(eyes, position)) return -1;
      for (int tick = 1; tick <= 18; tick++) {
         velocity = velocity.add(0, -0.03, 0);
         Vec3 end = position.add(velocity);
         Vec3 relativeStart = position.subtract(movement.scale(tick - 1));
         Vec3 relativeEnd = end.subtract(movement.scale(tick));
         var hit = box.clip(relativeStart, relativeEnd);
         if (box.contains(relativeStart) || hit.isPresent()) {
            double total = relativeStart.distanceTo(relativeEnd);
            double fraction = box.contains(relativeStart) || total < 1.0E-8 ? 0 : relativeStart.distanceTo(hit.get()) / total;
            Vec3 contact = position.add(end.subtract(position).scale(fraction));
            return clearPath.test(position, contact) ? tick : -1;
         }
         if (!clearPath.test(position, end)) return -1;
         position = end; velocity = velocity.scale(0.92);
      }
      return -1;
   }

   public static int restoreSlot(int current, int rod, int original, boolean holdingRod, boolean sameSession) {
      return sameSession && holdingRod && current == rod && rod >= 0 && rod < 9 && original >= 0 && original < 9 ? original : -1;
   }

   /** Aim never owns the weapon; only an accepted cast temporarily holds melee. */
   public static final class Cycle {
      private Phase phase = Phase.READY;
      private int target = -1, started, castAt, deadline, readyAt;
      private boolean observedHook;
      public Phase phase() { return phase; }
      public int target() { return target; }
      public int age(int tick) { return tick - castAt; }
      public boolean active() { return phase == Phase.AIMING || blocksMelee(); }
      public boolean blocksMelee() { return phase == Phase.FLYING || phase == Phase.RETRIEVING; }
      public boolean ready(int tick) { return phase == Phase.READY || phase == Phase.COOLDOWN && tick >= readyAt; }
      public void begin(int id, int tick) { if (ready(tick)) { target = id; started = tick; phase = Phase.AIMING; } }
      public void cast(int tick, int flightTicks, int latencyTicks) {
         if (phase != Phase.AIMING) return;
         phase = Phase.FLYING; castAt = tick; observedHook = false;
         deadline = tick + Math.min(18, Math.max(3, flightTicks) + Mth.clamp(latencyTicks, 2, 6));
      }
      public void update(int tick, boolean hookPresent, boolean contact, boolean meleePriority, int spawnGrace) {
         if (phase == Phase.AIMING && (meleePriority || tick - started >= 6)) finish(tick, 3);
         if (phase != Phase.FLYING) return;
         observedHook |= hookPresent;
         if (meleePriority || contact || tick >= deadline || (observedHook && !hookPresent)
            || (!observedHook && age(tick) >= spawnGrace)) phase = Phase.RETRIEVING;
      }
      public void requestReturn(int tick) {
         if (phase == Phase.AIMING) finish(tick, 3);
         else if (phase == Phase.FLYING) phase = Phase.RETRIEVING;
      }
      public void finish(int tick, int delay) { phase = Phase.COOLDOWN; readyAt = tick + Math.max(1, delay); target = -1; }
      public void reset() { phase = Phase.READY; target = -1; started = castAt = deadline = readyAt = 0; observedHook = false; }
   }
}
