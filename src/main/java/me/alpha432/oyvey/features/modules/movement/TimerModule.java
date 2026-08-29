package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;

public class TimerModule extends Module {
   public final Setting<Integer> speed = this.register(new Setting<>("Speed", 1, 1, 10));

   public TimerModule() {
      super("Timer", "Multiplies game tick speed (1 = 20 TPS, 2 = 40 TPS, ...).", Module.Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      this.apply();
   }

   @Override
   public void onTick() {
      this.apply();
   }

   private void apply() {
      OyVey.TIMER = this.speed.getValue().intValue();
   }

   @Override
   public void onDisable() {
      OyVey.TIMER = 1.0F;
   }

   @Override
   public String getDisplayInfo() {
      return this.speed.getValue() + "x";
   }
}
