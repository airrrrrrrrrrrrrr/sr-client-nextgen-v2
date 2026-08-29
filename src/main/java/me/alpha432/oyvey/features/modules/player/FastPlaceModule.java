package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;

public class FastPlaceModule extends Module {
   public final Setting<Boolean> randomize = this.register(new Setting<>("Randomize", true));

   public FastPlaceModule() {
      super("FastPlace", "Removes the right-click use delay on all items.", Module.Category.PLAYER);
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         mc.rightClickDelay = this.randomize.getValue() && Math.random() > 0.8 ? 1 : 0;
      }
   }
}
