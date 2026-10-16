package me.alpha432.oyvey.util.player;

/** Tracks downward distance, restarting the distance after each completed segment. */
public final class FallSegmentCounter {
   private double previousY = Double.NaN;
   private double distance;

   public void reset() {
      this.previousY = Double.NaN;
      this.distance = 0;
   }

   public boolean advance(double y, boolean falling) {
      double descent = this.previousY - y;
      this.previousY = y;
      if (!falling || !Double.isFinite(descent) || descent < 0 || descent > 8) {
         this.distance = 0;
         return false;
      }
      this.distance += descent;
      if (this.distance + 1.0E-6 >= 3.0) {
         this.distance = 0;
         return true;
      }
      return false;
   }
}
