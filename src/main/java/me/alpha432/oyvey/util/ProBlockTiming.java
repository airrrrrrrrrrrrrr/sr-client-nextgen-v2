package me.alpha432.oyvey.util;

/** Legacy block-hits follow a validated melee attack; shields wait for damage confirmation. */
public final class ProBlockTiming {
   private final CombatHitWindow hits = new CombatHitWindow();
   private int blockUntil = -1;
   private int blockFrom = -1;
   private int target = -1;

   public void reset() { hits.reset(); blockUntil = -1; blockFrom = -1; target = -1; }

   public void attack(int tick, int targetId, int hurtTime, boolean legacy) {
      if (target != targetId) reset();
      target = targetId;
      if (legacy) {
         // Hurt animations can arrive after knockback has already moved the
         // victim out of melee reach. A native melee attack is enough to time
         // this brief right-click; air swings never call this method.
         hits.reset(); blockFrom = tick; blockUntil = tick + 2;
      } else hits.attack(tick, targetId, hurtTime);
   }

   public boolean update(int tick, int targetId, int hurtTime, boolean eligible, boolean legacy) {
      if (!eligible) { reset(); return false; }
      if (target != targetId || blockFrom >= 0 && tick < blockFrom) { reset(); target = targetId; }
      if (hits.observe(tick, targetId, hurtTime)) { blockFrom = tick; blockUntil = tick + (legacy ? 2 : 7); }
      return blocking(tick);
   }

   public boolean blocking(int tick) { return blockFrom >= 0 && tick >= blockFrom && tick < blockUntil; }

   /** Always lower a held block before issuing another native attack. */
   public void beforeAttack() { blockUntil = -1; }
}
