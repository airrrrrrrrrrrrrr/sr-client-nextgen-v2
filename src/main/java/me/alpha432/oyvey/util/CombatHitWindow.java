package me.alpha432.oyvey.util;

/** Associates a fresh hurt animation with a recent local attack, once per hit. */
public final class CombatHitWindow {
   private int target = -1;
   private int previousHurt;
   private int attemptedAt = -1;

   public void reset() { target = -1; previousHurt = 0; attemptedAt = -1; }

   public void attack(int tick, int targetId, int hurtTime) {
      if (target != targetId) {
         reset(); target = targetId; previousHurt = hurtTime;
      }
      previousHurt = hurtTime;
      attemptedAt = tick;
   }

   public boolean observe(int tick, int targetId, int hurtTime) {
      if (target != targetId) {
         reset(); target = targetId; previousHurt = hurtTime;
         return false;
      }
      boolean fresh = hurtTime > previousHurt;
      previousHurt = hurtTime;
      boolean confirmed = fresh && attemptedAt >= 0 && tick >= attemptedAt && tick - attemptedAt <= 6;
      if (confirmed || attemptedAt >= 0 && (tick < attemptedAt || tick - attemptedAt > 6)) attemptedAt = -1;
      return confirmed;
   }
}
