package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoTotem extends Module {
   public final Setting<Integer> delay = this.register(new Setting<>("Delay", 1, 0, 10));
   public final Setting<Boolean> hotbarOk = this.register(new Setting<>("UseHotbar", true));
   public final Setting<Boolean> hpCheck = this.register(new Setting<>("HPCheck", false));
   public final Setting<Float> hpThreshold = this.register(new Setting<>("HPThreshold", 10.0F, 0.5F, 20.0F, v -> this.hpCheck.getValue()));
   private int tickTimer = 0;
   private int savedOffhandSlot = -1;

   public AutoTotem() {
      super("AutoTotem", "Always keeps a Totem of Undying in your offhand.", Module.Category.PLAYER);
   }

   @Override
   public void onDisable() {
      this.tickTimer = 0;
      this.savedOffhandSlot = -1;
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.level != null) {
         if (this.delay.getValue() > 0) {
            if (++this.tickTimer < this.delay.getValue()) {
               return;
            }

            this.tickTimer = 0;
         }

         if (this.hpCheck.getValue() && mc.player.getHealth() > this.hpThreshold.getValue()) {
            if (this.savedOffhandSlot != -1 && !mc.player.getInventory().getItem(this.savedOffhandSlot).isEmpty()) {
               int menuSlot = invToMenu(this.savedOffhandSlot);
               mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, menuSlot, 40, ContainerInput.SWAP, mc.player);
               this.savedOffhandSlot = -1;
            }

            return;
         }

         ItemStack offhand = mc.player.getOffhandItem();
         if (offhand.getItem() != Items.TOTEM_OF_UNDYING) {
            int invSlot = this.findTotem();
            if (invSlot != -1) {
               int menuSlot = invToMenu(invSlot);
               mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, menuSlot, 40, ContainerInput.SWAP, mc.player);
               if (this.hpCheck.getValue() && this.savedOffhandSlot == -1) {
                  this.savedOffhandSlot = invSlot;
               }
            }
         }
      }
   }

   private int findTotem() {
      for (int i = 9; i < 36; i++) {
         if (mc.player.getInventory().getItem(i).getItem() == Items.TOTEM_OF_UNDYING) {
            return i;
         }
      }

      if (this.hotbarOk.getValue()) {
         for (int ix = 0; ix < 9; ix++) {
            if (mc.player.getInventory().getItem(ix).getItem() == Items.TOTEM_OF_UNDYING) {
               return ix;
            }
         }
      }

      return -1;
   }

   private static int invToMenu(int invSlot) {
      return invSlot < 9 ? 36 + invSlot : invSlot;
   }
}
