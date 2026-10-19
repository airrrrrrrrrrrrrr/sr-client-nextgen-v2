package me.alpha432.oyvey.util;

/** A melee pulse must transmit one ordinary movement look before its attack. */
public final class OneTickAttackRotation {
   public enum Step { WAIT, AIM, ATTACK }
   private int target = -1, armedTick = -1, sentTick = -1;

   public void reset() { target = armedTick = sentTick = -1; }

   public Step update(int targetId, int tick, boolean attackReady) {
      if (targetId < 0 || !attackReady) { reset(); return Step.WAIT; }
      if (target == targetId && armedTick == tick) return Step.AIM;
      if (target == targetId && tick == armedTick + 1 && sentTick == armedTick) {
         reset();
         return Step.ATTACK;
      }
      target = targetId;
      armedTick = tick;
      sentTick = -1;
      return Step.AIM;
   }

   /** Only called after a noncancelled packet actually matches this target's aim. */
   public void transmitted(int tick, int targetId) {
      if (tick == armedTick && targetId == target) sentTick = tick;
   }
}
