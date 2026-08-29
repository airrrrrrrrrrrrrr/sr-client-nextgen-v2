package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;

public class AutoEat extends Module {
   private final Setting<Float> hunger = this.register(new Setting<>("Hunger", 19.0F, 0.1F, 20.0F));
   private final Setting<Boolean> healthMode = this.register(new Setting<>("HealthMode", false));
   private final Setting<Float> safeHealth = this.register(new Setting<>("SafeHealth", 19.0F, 0.1F, 36.0F));
   private final Setting<Boolean> calcWithAbsorption = this.register(new Setting<>("CalcAbsorption", true));
   private final Setting<Boolean> absorptionCheck = this.register(new Setting<>("AbsorptionCheck", false));
   private final Setting<Float> absorptionAmount = this.register(new Setting<>("AbsorptionAmount", 0.0F, 0.0F, 16.0F));
   private final Setting<Boolean> skipBadEffects = this.register(new Setting<>("SkipBadEffects", true));
   private final Setting<Boolean> always = this.register(new Setting<>("Always", false));
   private boolean isEating = false;
   private boolean usingOffHand = false;
   private int savedSlot = -1;
   private int eatSlot = -1;

   public AutoEat() {
      super("AutoEat", "Automatically eats food when hungry.", Module.Category.PLAYER);
   }

   @Override
   public void onEnable() {
      this.clearState();
   }

   @Override
   public void onDisable() {
      this.clearState();
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         boolean shouldEat = this.always.getValue() || this.needsFood();
         if (!shouldEat) {
            if (this.isEating) {
               this.stopEating();
            }
         } else if (this.isGapple(mc.player.getOffhandItem())) {
            if (this.isEating && !this.usingOffHand) {
               this.stopEating();
            }

            this.usingOffHand = true;
            this.isEating = true;
            mc.options.keyUse.setDown(true);
         } else {
            if (this.usingOffHand) {
               mc.options.keyUse.setDown(false);
               this.usingOffHand = false;
               this.isEating = false;
            }

            int foodSlot = this.findFoodHotbar();
            if (foodSlot == -1) {
               if (this.isEating) {
                  this.stopEating();
               }
            } else {
               if (!this.isEating) {
                  this.savedSlot = mc.player.getInventory().getSelectedSlot();
                  this.eatSlot = foodSlot;
                  this.isEating = true;
               } else {
                  this.eatSlot = foodSlot;
               }

               if (mc.player.getInventory().getSelectedSlot() != this.eatSlot) {
                  this.setSlot(this.eatSlot);
               }

               mc.options.keyUse.setDown(true);
            }
         }
      }
   }

   private boolean needsFood() {
      if (mc.player.getFoodData().getFoodLevel() < this.hunger.getValue()) {
         return true;
      }

      if (this.healthMode.getValue()) {
         float hp = mc.player.getHealth();
         if (this.calcWithAbsorption.getValue()) {
            hp += mc.player.getAbsorptionAmount();
         }

         if (hp < this.safeHealth.getValue()) {
            return true;
         }
      }

      return this.absorptionCheck.getValue() && mc.player.getAbsorptionAmount() < this.absorptionAmount.getValue();
   }

   private void stopEating() {
      mc.options.keyUse.setDown(false);
      if (!this.usingOffHand && this.savedSlot != -1) {
         this.setSlot(this.savedSlot);
      }

      this.isEating = false;
      this.usingOffHand = false;
      this.savedSlot = -1;
      this.eatSlot = -1;
   }

   private void clearState() {
      if (mc.options != null) {
         mc.options.keyUse.setDown(false);
      }

      this.isEating = false;
      this.usingOffHand = false;
      this.savedSlot = -1;
      this.eatSlot = -1;
   }

   private boolean isGapple(ItemStack stack) {
      return stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
   }

   private int findFoodHotbar() {
      InventoryMenu inv = mc.player.inventoryMenu;

      for (int i = 36; i < 45; i++) {
         ItemStack stack = ((Slot)inv.slots.get(i)).getItem();
         if (stack.has(DataComponents.FOOD) && (!this.skipBadEffects.getValue() || !this.hasBadEffect(stack))) {
            return i - 36;
         }
      }

      return -1;
   }

   private boolean hasBadEffect(ItemStack stack) {
      Consumable consumable = (Consumable)stack.get(DataComponents.CONSUMABLE);
      if (consumable == null) {
         return false;
      }

      for (ConsumeEffect effect : consumable.onConsumeEffects()) {
         if (effect instanceof ApplyStatusEffectsConsumeEffect applyEffect) {
            for (MobEffectInstance mobEffect : applyEffect.effects()) {
               if (!((MobEffect)mobEffect.getEffect().value()).isBeneficial()) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private void setSlot(int hotbarSlot) {
      mc.player.getInventory().setSelectedSlot(hotbarSlot);
      mc.player.connection.send(new ServerboundSetCarriedItemPacket(hotbarSlot));
   }

   @Override
   public String getDisplayInfo() {
      return String.format("%.0f hunger", this.hunger.getValue());
   }
}
