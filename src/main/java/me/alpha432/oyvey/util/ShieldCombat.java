package me.alpha432.oyvey.util;

/** Weapon ownership and use/attack ordering shared by Recode's shield actions. */
public final class ShieldCombat {
   private ShieldCombat() { }

   public static boolean timingReady(boolean breakingShield, boolean cooldown, float strength, float required, boolean delayReady) {
      return breakingShield || (cooldown ? strength >= required : delayReady);
   }

   public static void attack(boolean shieldRaised, boolean keepRaised, boolean breakingShield,
      Runnable lower, Runnable hit, Runnable raise) {
      boolean release = shieldRaised && (!keepRaised || breakingShield);
      if (release) lower.run();
      try { hit.run(); }
      finally { if (release) raise.run(); }
   }

   public static final class AxeSwap {
      private int previous = -1, axe = -1, target = -1;
      public boolean active() { return previous >= 0; }
      public int target() { return target; }
      public void begin(int previous, int axe, int target) {
         if (active() || previous < 0 || previous > 8 || axe < 0 || axe > 8 || previous == axe) return;
         this.previous = previous; this.axe = axe; this.target = target;
      }
      public boolean owns(int selected, boolean holdingAxe, boolean sameSession) {
         return active() && sameSession && holdingAxe && selected == axe;
      }
      public int finish(int selected, boolean holdingAxe, boolean sameSession, boolean restore) {
         int slot = restore && owns(selected, holdingAxe, sameSession) ? previous : -1;
         clear(); return slot;
      }
      public void clear() { previous = axe = target = -1; }
   }
}
