package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class StepModule extends Module {
   private final Setting<Float> height = this.num("Height", 2.0F, 1.0F, 3.0F);
   private float prev;

   public StepModule() {
      super("Step", "step..", Module.Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.prev = 0.6F;
      } else {
         this.prev = mc.player.maxUpStep();
      }
   }

   @Override
   public void onDisable() {
      if (!nullCheck()) {
         mc.player.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(this.prev);
      }
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         mc.player.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(this.height.getValue().floatValue());
      }
   }
}
