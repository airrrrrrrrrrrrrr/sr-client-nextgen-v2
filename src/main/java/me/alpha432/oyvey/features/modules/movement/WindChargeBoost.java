package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;

public class WindChargeBoost extends Module {
   public final Setting<Double> boost = this.register(new Setting<>("Boost", 50.0, 1.0, 1000.0));
   private double prevDeltaY = 0.0;

   public WindChargeBoost() {
      super("WindChargeBoost", "Multiplies wind charge launch height by a configurable amount.", Module.Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      this.prevDeltaY = 0.0;
   }

   @Subscribe
   public void onUpdateWalkingPlayer(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == Stage.PRE && mc.player != null) {
         double currentY = mc.player.getDeltaMovement().y;
         double spike = currentY - this.prevDeltaY;
         if (spike > 0.5 && currentY > 0.0) {
            double multiplier = 1.0 + this.boost.getValue() / 100.0;
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, currentY * multiplier, mc.player.getDeltaMovement().z);
         }

         this.prevDeltaY = mc.player.getDeltaMovement().y;
      }
   }

   @Override
   public String getDisplayInfo() {
      return String.format("+%.0f%%", this.boost.getValue());
   }
}
