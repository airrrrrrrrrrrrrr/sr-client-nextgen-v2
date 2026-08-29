package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

public class AutoSoupModule extends Module {
   public final Setting<Double> hpThreshold = this.register(new Setting<>("Health", 15.0, 1.0, 19.0));
   public final Setting<Integer> switchDelay = this.register(new Setting<>("SwitchDelay", 1, 0, 4));
   private AutoSoupModule.State state = AutoSoupModule.State.IDLE;
   private int ticks = 0;
   private int savedSlot = -1;
   private int soupHotbar = -1;
   private boolean fromInv = false;

   public AutoSoupModule() {
      super("AutoSoup", "Instantly consumes mushroom stew when health drops below the threshold.", Module.Category.PLAYER);
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
      if (!nullCheck() && mc.gameMode != null) {
         InventoryMenu inv = mc.player.inventoryMenu;
         switch (this.state) {
            case IDLE:
               if (mc.player.getHealth() > this.hpThreshold.getValue().floatValue()) {
                  return;
               }

               this.savedSlot = mc.player.getInventory().getSelectedSlot();
               this.fromInv = false;
               int hotbarIdx = this.findSoupHotbar(inv);
               if (hotbarIdx != -1) {
                  this.soupHotbar = hotbarIdx;
                  this.setSlot(this.soupHotbar);
               } else {
                  int invSlot = this.findSoupInventory(inv);
                  if (invSlot == -1) {
                     return;
                  }

                  this.soupHotbar = this.savedSlot;
                  this.fromInv = true;
                  mc.gameMode.handleContainerInput(inv.containerId, invSlot, this.savedSlot, ContainerInput.SWAP, mc.player);
               }

               this.ticks = this.switchDelay.getValue();
               this.state = AutoSoupModule.State.WAIT_USE;
               break;
            case WAIT_USE:
               if (this.ticks-- > 0) {
                  return;
               }

               if (mc.player.getInventory().getSelectedSlot() != this.soupHotbar) {
                  this.setSlot(this.soupHotbar);
               }

               mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
               this.ticks = this.switchDelay.getValue();
               this.state = AutoSoupModule.State.WAIT_RESTORE;
               break;
            case WAIT_RESTORE:
               if (this.ticks-- > 0) {
                  return;
               }

               this.setSlot(this.savedSlot);
               this.state = AutoSoupModule.State.IDLE;
         }
      }
   }

   private void setSlot(int hotbarSlot) {
      mc.player.getInventory().setSelectedSlot(hotbarSlot);
      mc.player.connection.send(new ServerboundSetCarriedItemPacket(hotbarSlot));
   }

   private void clearState() {
      this.state = AutoSoupModule.State.IDLE;
      this.ticks = 0;
      this.savedSlot = -1;
      this.soupHotbar = -1;
      this.fromInv = false;
   }

   private int findSoupHotbar(InventoryMenu inv) {
      for (int i = 36; i < 45; i++) {
         if (((Slot)inv.slots.get(i)).getItem().is(Items.MUSHROOM_STEW)) {
            return i - 36;
         }
      }

      return -1;
   }

   private int findSoupInventory(InventoryMenu inv) {
      for (int i = 9; i < 36; i++) {
         if (((Slot)inv.slots.get(i)).getItem().is(Items.MUSHROOM_STEW)) {
            return i;
         }
      }

      return -1;
   }

   @Override
   public String getDisplayInfo() {
      return !this.isEnabled() ? null : String.format("%.0f HP", this.hpThreshold.getValue());
   }

   private enum State {
      IDLE,
      WAIT_USE,
      WAIT_RESTORE;
   }
}
