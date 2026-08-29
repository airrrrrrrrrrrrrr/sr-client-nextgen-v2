package me.alpha432.oyvey.features.modules.combat;

import java.util.concurrent.ThreadLocalRandom;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;

public class ReachModule extends Module {
   public static ReachModule INSTANCE;
   public final Setting<Double> maxReach = this.register(new Setting<>("MaxReach", 3.8, 3.0, 6.0));
   public final Setting<Double> minReach = this.register(new Setting<>("MinReach", 3.2, 3.0, 6.0));
   public final Setting<Integer> chance = this.register(new Setting<>("Chance", 100, 1, 100));
   private volatile double currentReach = 3.0;

   public ReachModule() {
      super("Reach", "Extend attack reach with min/max range and chance.", Module.Category.COMBAT);
      INSTANCE = this;
   }

   @Override
   public void onTick() {
      if (ThreadLocalRandom.current().nextInt(100) >= this.chance.getValue()) {
         this.currentReach = 3.0;
      } else {
         double lo = this.minReach.getValue();
         double hi = this.maxReach.getValue();
         if (lo >= hi) {
            this.currentReach = hi;
         } else {
            this.currentReach = lo + ThreadLocalRandom.current().nextDouble() * (hi - lo);
         }
      }
   }

   @Override
   public void onDisable() {
      this.currentReach = 3.0;
      super.onDisable();
   }

   public boolean shouldApply() {
      return this.isEnabled() && this.currentReach > 3.0;
   }

   public double getCurrentReach() {
      return this.currentReach;
   }

   @Override
   public String getDisplayInfo() {
      return !this.isEnabled() ? null : String.format("%.2f\u2013%.2f %d%%", this.minReach.getValue(), this.maxReach.getValue(), this.chance.getValue());
   }
}
