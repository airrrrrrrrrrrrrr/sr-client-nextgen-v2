package me.alpha432.oyvey.util.player;

import me.alpha432.oyvey.util.traits.Util;

public final class PlayerUtil implements Util {
   private PlayerUtil() {
      throw new AssertionError();
   }

   public static void setMotionY(double y) {
      if (mc.player != null) {
         var mot = mc.player.getDeltaMovement();
         mc.player.setDeltaMovement(mot.x, y, mot.z);
      }
   }
}
