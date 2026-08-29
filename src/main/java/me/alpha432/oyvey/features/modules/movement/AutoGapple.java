package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class AutoGapple extends Module {
   private final Setting<Float> health = this.register(new Setting<>("Health", 15.0F, 0.1F, 20.0F));
   private final Setting<Integer> delay = this.register(new Setting<>("Delay", 2, 1, 5));
   private boolean isEating = false;
   private int eatCooldown = 0;

   public AutoGapple() {
      super("AutoGapple", "Automatically eats Golden Apples", Module.Category.PLAYER);
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         if (this.eatCooldown > 0) {
            this.eatCooldown--;
         } else {
            boolean holdingGap = mc.player.getItemInHand(InteractionHand.OFF_HAND).getItem() == Items.GOLDEN_APPLE
               || mc.player.getMainHandItem().getItem() == Items.GOLDEN_APPLE;
            float effectiveHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (holdingGap && effectiveHealth <= this.health.getValue() && !this.isEating) {
               mc.options.keyUse.setDown(true);
               this.isEating = true;
            }

            if (this.isEating && (effectiveHealth >= this.health.getValue() + 2.0F || !holdingGap)) {
               mc.options.keyUse.setDown(false);
               this.isEating = false;
               this.eatCooldown = this.delay.getValue() * 20;
            }
         }
      }
   }

   @Override
   public void onDisable() {
      if (mc.options != null && mc.options.keyUse.isDown()) {
         mc.options.keyUse.setDown(false);
      }

      this.isEating = false;
      this.eatCooldown = 0;
   }
}
