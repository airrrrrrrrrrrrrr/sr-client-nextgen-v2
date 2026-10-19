package me.alpha432.oyvey.util;

/** Timing for genuine jump crits; never changes position or vertical motion. */
public final class CriticalTiming {
   private CriticalTiming() { }

   public record State(boolean grounded, boolean jumpRequested, double verticalMotion, double fallDistance,
      boolean normalMovement, boolean mobilityRestricted, boolean sprinting, boolean livingTarget) {
      public boolean fallingWindow() {
         return !this.grounded && this.normalMovement && this.verticalMotion < 0 && this.fallDistance > 0;
      }

      public boolean canCrit() {
         return this.fallingWindow() && !this.mobilityRestricted && !this.sprinting && this.livingTarget;
      }
   }

   public static boolean requiresCrit(boolean prefer, boolean only, State state) {
      return only || prefer && state.normalMovement && (!state.grounded || state.jumpRequested);
   }

   public static boolean allowsAttack(boolean prefer, boolean only, State state) {
      return !requiresCrit(prefer, only, state) || state.canCrit();
   }

   public static boolean suppressSprint(boolean prefer, boolean only, State state) {
      return (prefer || only) && state.fallingWindow() && !state.mobilityRestricted && state.livingTarget;
   }
}
