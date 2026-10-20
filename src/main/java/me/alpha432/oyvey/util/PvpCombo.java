package me.alpha432.oyvey.util;

import java.util.Objects;
import java.util.random.RandomGenerator;

/** Input decisions only: acceleration, friction and knockback stay in vanilla physics. */
public final class PvpCombo {
   public enum Phase { APPROACH, PRESSURE, SPACE, RESET, RECOVER }
   public record Move(int forward, int sideways, boolean sprint, Phase phase) { }

   private final RandomGenerator random;
   private int target = -1;
   private int resetUntil;
   private int nextStrafe;
   private int side = 1;
   private int lastOwnHurt;
   private int recoveringUntil;
   private int hits;
   private int lastHit = -100;

   public PvpCombo() { this(RandomGenerator.getDefault()); }
   public PvpCombo(RandomGenerator random) { this.random = Objects.requireNonNull(random); }

   public void reset() {
      target = -1; resetUntil = nextStrafe = recoveringUntil = lastOwnHurt = hits = 0; lastHit = -100;
   }

   public void confirmedHit(int tick, boolean sprintReset) {
      hits++; lastHit = tick;
      if (sprintReset) resetUntil = tick + 1;
   }

   public int combo() { return hits; }

   public Move decide(int tick, int targetId, double distance, double preferredDistance, double closingSpeed,
      int ownHurt, boolean strafe, boolean sprintReset, boolean safeLeft, boolean safeRight) {
      if (target != targetId) {
         reset(); target = targetId; lastOwnHurt = ownHurt;
         side = random.nextBoolean() ? 1 : -1;
         nextStrafe = tick + random.nextInt(12, 25);
      }
      if (ownHurt > lastOwnHurt) { hits = 0; recoveringUntil = tick + 4; resetUntil = 0; }
      lastOwnHurt = ownHurt;
      if (tick - lastHit > 30) hits = 0;
      if (tick >= nextStrafe) { side = -side; nextStrafe = tick + random.nextInt(12, 25); }
      int sideways = strafe && distance < preferredDistance + 1.5 ? side : 0;
      if (sideways > 0 && !safeLeft || sideways < 0 && !safeRight) {
         side = -side;
         sideways = side > 0 && safeLeft || side < 0 && safeRight ? side : 0;
         nextStrafe = tick + 12;
      }
      // One-tick look-ahead avoids running into the target while its knockback decays.
      double predicted = distance - Math.clamp(closingSpeed, -0.4, 0.4);
      boolean resetting = sprintReset && tick < resetUntil;
      int forward = predicted < preferredDistance - 0.25 ? -1 : predicted > preferredDistance + 0.12 ? 1 : 0;
      Phase phase = forward < 0 ? Phase.SPACE : distance > preferredDistance + 1 ? Phase.APPROACH : Phase.PRESSURE;
      if (tick < recoveringUntil) phase = Phase.RECOVER;
      if (resetting) { forward = 0; phase = Phase.RESET; }
      return new Move(forward, sideways, forward > 0 && !resetting, phase);
   }
}
