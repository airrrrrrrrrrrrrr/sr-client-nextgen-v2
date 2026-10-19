package me.alpha432.oyvey.util.render;

/** Replace time within the current day without changing the moon/day counter. */
public final class AmbienceClock {
   private AmbienceClock() { }
   public static long withTime(long serverTicks, int selectedTime) {
      return serverTicks - Math.floorMod(serverTicks, 24000L) + Math.floorMod(selectedTime, 24000);
   }
}
