package me.alpha432.oyvey.features.modules.movement.scaffold;

/** Five-millisecond packet pulse deadlines, with tickets that invalidate queued callbacks. */
public final class ShortRotationPulse {
   private static final long DURATION_NANOS = 5_000_000L;
   private boolean active;
   private long startedAt, generation;

   public long start(long now) { active = true; startedAt = now; return ++generation; }
   public void clear() { active = false; generation++; }
   public boolean active() { return active; }
   public long ticket() { return generation; }
   public long remaining(long ticket, long now) {
      return active && ticket == generation ? Math.max(0L, DURATION_NANOS - (now - startedAt)) : 0L;
   }

   /** Callers perform the return on the client thread only when this succeeds. */
   public boolean finish(long ticket, long now, boolean force) {
      if (!active || ticket != generation) return false;
      if (!force && now >= startedAt && now - startedAt < DURATION_NANOS) return false;
      clear();
      return true;
   }
}
