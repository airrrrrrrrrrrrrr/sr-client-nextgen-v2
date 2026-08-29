package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.features.modules.Module;

public class FastUse extends Module {
   public FastUse() {
      super("FastUse", "Speeds up item use \u2014 eats/drinks in 1 tick.", Module.Category.PLAYER);
   }

   @Override
   public void onTick() {
      if (!nullCheck() && mc.player.isUsingItem()) {
         mc.player.useItemRemaining = 1;
      }
   }
}
