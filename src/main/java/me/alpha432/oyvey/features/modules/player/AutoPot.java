package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public class AutoPot extends Module {
   public static volatile boolean isRotating = false;
   public static volatile float visualYaw = 0.0F;
   public static volatile float visualPitch = 0.0F;
   public final Setting<Double> hpThreshold = this.register(new Setting<>("Health", 15.0, 1.0, 19.0));
   public final Setting<Integer> switchDelay = this.register(new Setting<>("SwitchDelay", 1, 0, 4));
   private AutoPot.State state = AutoPot.State.IDLE;
   private int ticks = 0;
   private int savedSlot = -1;
   private int potHotbar = -1;

   public AutoPot() {
      super("AutoPot", "Silently looks down and throws a health potion when health is low.", Module.Category.PLAYER);
   }

   @Override
   public void onEnable() {
      this.clearState();
   }

   @Override
   public void onDisable() {
      if (isRotating && mc.player != null) {
         mc.player.setYRot(visualYaw);
         mc.player.setXRot(visualPitch);
         isRotating = false;
      }

      if (this.savedSlot != -1 && mc.player != null) {
         this.setSlot(this.savedSlot);
      }

      this.clearState();
   }

   @Override
   public void onTick() {
      if (!nullCheck() && mc.gameMode != null) {
         InventoryMenu inv = mc.player.inventoryMenu;
         switch (this.state) {
            case IDLE:
               if (mc.player.getHealth() > this.hpThreshold.getValue().floatValue()) {
                  return;
               }

               int hotbarIdx = this.findPotHotbar(inv);
               if (hotbarIdx == -1) {
                  int invSlot = this.findPotInventory(inv);
                  if (invSlot == -1) {
                     return;
                  }

                  this.savedSlot = mc.player.getInventory().getSelectedSlot();
                  this.potHotbar = this.savedSlot;
                  mc.gameMode.handleContainerInput(inv.containerId, invSlot, this.savedSlot, ContainerInput.SWAP, mc.player);
               } else {
                  this.savedSlot = mc.player.getInventory().getSelectedSlot();
                  this.potHotbar = hotbarIdx;
                  this.setSlot(this.potHotbar);
               }

               this.ticks = this.switchDelay.getValue();
               this.state = AutoPot.State.THROW;
               break;
            case THROW:
               if (this.ticks-- > 0) {
                  return;
               }

               if (mc.player.getInventory().getSelectedSlot() != this.potHotbar) {
                  this.setSlot(this.potHotbar);
               }

               visualYaw = mc.player.getYRot();
               visualPitch = mc.player.getXRot();
               mc.player.setXRot(90.0F);
               isRotating = true;
               mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
               mc.player.swing(InteractionHand.MAIN_HAND);
               this.ticks = this.switchDelay.getValue();
               this.state = AutoPot.State.RESTORE;
               break;
            case RESTORE:
               if (this.ticks-- > 0) {
                  return;
               }

               if (isRotating) {
                  mc.player.setYRot(visualYaw);
                  mc.player.setXRot(visualPitch);
                  isRotating = false;
               }

               this.setSlot(this.savedSlot);
               this.clearState();
         }
      }
   }

   private void setSlot(int hotbarSlot) {
      mc.player.getInventory().setSelectedSlot(hotbarSlot);
      mc.player.connection.send(new ServerboundSetCarriedItemPacket(hotbarSlot));
   }

   private void clearState() {
      this.state = AutoPot.State.IDLE;
      this.ticks = 0;
      this.savedSlot = -1;
      this.potHotbar = -1;
   }

   private boolean isHealPotion(ItemStack stack) {
      if (!stack.is(Items.SPLASH_POTION)) {
         return false;
      }

      PotionContents contents = (PotionContents)stack.get(DataComponents.POTION_CONTENTS);
      return contents == null ? false : contents.potion().filter(h -> h == Potions.HEALING || h == Potions.STRONG_HEALING).isPresent();
   }

   private int findPotHotbar(InventoryMenu inv) {
      for (int i = 36; i < 45; i++) {
         if (this.isHealPotion(((Slot)inv.slots.get(i)).getItem())) {
            return i - 36;
         }
      }

      return -1;
   }

   private int findPotInventory(InventoryMenu inv) {
      for (int i = 9; i < 36; i++) {
         if (this.isHealPotion(((Slot)inv.slots.get(i)).getItem())) {
            return i;
         }
      }

      return -1;
   }

   @Override
   public String getDisplayInfo() {
      return String.format("%.0f HP", this.hpThreshold.getValue());
   }

   private enum State {
      IDLE,
      THROW,
      RESTORE;
   }
}
