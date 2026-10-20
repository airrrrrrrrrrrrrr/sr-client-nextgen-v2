package me.alpha432.oyvey.util;

/** Measures the wait from an eligible swap becoming necessary, using monotonic time. */
public final class AutoTotemDelay {
   private boolean waiting;
   private int action;
   private long since;

   public void reset() { waiting = false; }

   public boolean ready(long nowNanos, int milliseconds, int actionKey) {
      if (!waiting || action != actionKey || nowNanos - since < 0) {
         waiting = true; action = actionKey; since = nowNanos;
      }
      return nowNanos - since >= Math.clamp(milliseconds, 0, 1000) * 1_000_000L;
   }
}
