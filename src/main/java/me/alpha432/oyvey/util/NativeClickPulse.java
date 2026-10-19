package me.alpha432.oyvey.util;

/** One native input click per deadline, with no catch-up burst after a stall. */
public final class NativeClickPulse {
   private long next;
   private int rate;
   private boolean active;

   public void reset() { active=false; next=0; }

   public boolean consume(boolean eligible, int cps, long now) {
      if (!eligible) { reset(); return false; }
      cps=Math.clamp(cps,5,20);
      long period=1_000_000_000L/cps;
      if (!active || rate!=cps) { active=true; rate=cps; next=now; }
      if (now<next) return false;
      next=now-next>=period ? now+period : next+period;
      return true;
   }
}
