package me.alpha432.oyvey.util;

import java.util.List;

/** An air-swing clock independent of melee cooldowns and real recorded attack deadlines. */
public final class CombatPreSwing {
   private final RecordedClickPlayback recorded = new RecordedClickPlayback();
   private boolean active;
   private int target = -1;
   private long next;

   public void reset() { active = false; target = -1; next = 0; recorded.reset(); }

   public static boolean inRange(double distanceSquared, double attackReach, double swingRange) {
      return Double.isFinite(distanceSquared) && distanceSquared > attackReach * attackReach
         && distanceSquared <= swingRange * swingRange;
   }

   public int poll(boolean eligible, int targetId, long delayNanos, List<Long> pattern, boolean recordedMode, long now) {
      if (!eligible || targetId < 0) { reset(); return 0; }
      if (target != targetId) { reset(); target = targetId; }
      if (recordedMode) {
         active = false;
         recorded.prepare(pattern, targetId, true, now);
         return recorded.poll(now);
      }
      recorded.reset();
      if (!active) { active = true; next = now; }
      if (now < next) return 0;
      if (now - next > 250_000_000L) next = now;
      next += Math.max(1, delayNanos);
      return 1;
   }
}
