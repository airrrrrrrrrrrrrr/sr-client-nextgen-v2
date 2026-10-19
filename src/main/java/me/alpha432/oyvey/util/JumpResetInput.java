package me.alpha432.oyvey.util;

/** A fresh damage/velocity pair may request one native jump-key pulse. */
public final class JumpResetInput {
   private int pendingAt = -1, nextAllowed;
   public void arm(int tick, boolean grounded, boolean horizontalKnockback) {
      if (grounded && horizontalKnockback && pendingAt < 0 && tick >= nextAllowed) pendingAt = tick;
   }
   public boolean consume(int tick, boolean eligible, boolean damaged, boolean manualJump) {
      if (pendingAt < 0) return false;
      if (!eligible || manualJump || tick < pendingAt || tick - pendingAt > 2) { pendingAt = -1; return false; }
      if (!damaged) return false;
      pendingAt = -1; nextAllowed = tick + 10; return true;
   }
   public void reset() { pendingAt = -1; nextAllowed = 0; }
   public void correction(int tick) { pendingAt = -1; nextAllowed = tick + 10; }
}
