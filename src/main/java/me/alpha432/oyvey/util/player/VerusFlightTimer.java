package me.alpha432.oyvey.util.player;

/** A damage-triggered flight window using a monotonic clock. */
public final class VerusFlightTimer {
   private enum State { IDLE, WAITING, FLYING }
   private State state = State.IDLE;
   private long deadline;

   public void arm(long now) {
      this.state = State.WAITING;
      this.deadline = now + 10_000_000_000L;
   }

   public boolean startOnDamage(long now) {
      if (!this.isWaiting() || this.waitTimedOut(now)) {
         return false;
      }
      this.state = State.FLYING;
      this.deadline = now + 5_000_000_000L;
      return true;
   }

   public boolean isIdle() { return this.state == State.IDLE; }
   public boolean isWaiting() { return this.state == State.WAITING; }
   public boolean isFlying() { return this.state == State.FLYING; }
   public boolean waitTimedOut(long now) { return this.isWaiting() && now >= this.deadline; }
   public boolean flightExpired(long now) { return this.isFlying() && now >= this.deadline; }

   public int secondsRemaining(long now) {
      if (!this.isFlying()) {
         return 0;
      }
      return (int)Math.max(0, (this.deadline - now + 999_999_999L) / 1_000_000_000L);
   }

   public void reset() {
      this.state = State.IDLE;
      this.deadline = 0;
   }
}
