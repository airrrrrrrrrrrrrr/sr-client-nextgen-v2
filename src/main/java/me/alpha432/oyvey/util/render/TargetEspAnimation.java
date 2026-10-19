package me.alpha432.oyvey.util.render;

/** Frame-rate independent visual rotation; attacks never reset visibility. */
public final class TargetEspAnimation {
   private long previous;
   private double spin, goal, pulse;
   private double diamondTime, ghostTime;
   private float diamondSpeed = 1, ghostSpeed = 1;

   public void reset() { previous = 0; spin = goal = pulse = diamondTime = ghostTime = 0; }

   public void update(long now) {
      update(now, diamondSpeed, ghostSpeed);
   }

   public void update(long now, float diamondSpeed, float ghostSpeed) {
      double dt = previous == 0 ? 0 : Math.max(0, (now - previous) / 1_000_000_000.0);
      previous = now;
      this.diamondSpeed = diamondSpeed; this.ghostSpeed = ghostSpeed;
      diamondTime += dt * diamondSpeed;
      ghostTime += dt * ghostSpeed;
      spin += (goal - spin) * -Math.expm1(-10 * dt);
      pulse *= Math.exp(-7 * dt);
   }

   public void attack(long now) {
      update(now);
      goal += Math.PI / 2;
      pulse = 1;
   }

   public float rotation() { return (float)spin; }
   public float pulse() { return (float)pulse; }
   public double diamondTime() { return diamondTime; }
   public double ghostTimeMillis() { return ghostTime * 1000; }
}
