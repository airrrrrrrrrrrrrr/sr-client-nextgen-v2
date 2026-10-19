package me.alpha432.oyvey.util.render;

import org.joml.Matrix4f;

/** Covers the close-range disappearance and attack animation timing. */
public final class TargetEspChecks {
   private static int checks;
   public static void main(String[] args) {
      var camera = new Matrix4f().perspective((float)Math.toRadians(70), 16F / 9, .05F, 100);
      require(ScreenProjection.point(camera, 0, 2, -1, 960, 540) == null, "ordinary point projection still clips off-screen labels");
      float[] head = ScreenProjection.pointUnclipped(camera, 0, 2, -1, 960, 540);
      float[] feet = ScreenProjection.pointUnclipped(camera, 0, -1, -1, 960, 540);
      require(head != null && feet != null && feet[1] - head[1] > 540, "close-range sprite size survives off-screen head and feet");
      require(ScreenProjection.pointUnclipped(camera, 0, 0, 1, 960, 540) == null, "targets behind the camera are not mirrored on screen");
      require(ScreenProjection.pointUnclipped(camera, Double.NaN, 0, -1, 960, 540) == null, "invalid coordinates are rejected");
      long start = 1_000_000_000L;
      var animation = new TargetEspAnimation();
      animation.attack(start);
      require(animation.rotation() == 0, "an attack does not snap rotation");
      animation.update(start + 150_000_000L);
      float halfway = animation.rotation();
      require(halfway > 0 && halfway < Math.PI / 2, "attack turns smoothly toward a quarter rotation");
      animation.attack(start + 150_000_000L);
      require(animation.rotation() == halfway, "repeated attacks preserve the current animation");
      animation.update(start + 1_150_000_000L);
      require(Math.abs(animation.rotation() - Math.PI) < .001, "two attacks accumulate two quarter turns");
      require(animation.pulse() < .002, "attack scale pulse settles back down");
      var coarse = new TargetEspAnimation(); var fine = new TargetEspAnimation();
      coarse.attack(start); fine.attack(start);
      coarse.update(start + 500_000_000L);
      for (int frame = 1; frame <= 50; frame++) fine.update(start + frame * 10_000_000L);
      require(Math.abs(coarse.rotation() - fine.rotation()) < .00001, "rotation is independent of frame rate");
      animation.reset();
      require(animation.rotation() == 0 && animation.pulse() == 0, "changing worlds or disabling clears the animation");
      animation.update(start, 2, .5F);
      animation.update(start+500_000_000L, 2, .5F);
      require(animation.diamondTime()==1 && animation.ghostTimeMillis()==250, "Diamond and Ghost speed sliders advance independent animation clocks");
      animation.update(start+500_000_000L,0,3);
      require(animation.diamondTime()==1 && animation.ghostTimeMillis()==250, "changing a speed does not jump the current rotation");
      animation.update(start+1_000_000_000L,0,3);
      require(animation.diamondTime()==1 && animation.ghostTimeMillis()==1750, "zero pauses Diamond's ambient spin while Ghost continues at its own speed");
      System.out.println("Target ESP checks passed: " + checks);
   }
   private static void require(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
}
