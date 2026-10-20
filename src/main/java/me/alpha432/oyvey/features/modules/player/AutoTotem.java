package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.AutoTotemDelay;
import me.alpha432.oyvey.util.InventorySwapSession;
import me.alpha432.oyvey.util.ViaProtocol;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoTotem extends Module {
   public final Setting<Integer> delay = this.register(new Setting<>("DelayMS", 50, 0, 1000,
      "Wait in milliseconds after a totem swap becomes necessary; 0 swaps on the next client tick."));
   public final Setting<Boolean> inventorySpoof = this.register(new Setting<>("InventorySpoof", true,
      "Silently swap on modern (1.12+) protocols. Older protocols or Off require the inventory screen."));
   public final Setting<Boolean> hotbarOk = this.register(new Setting<>("UseHotbar", true));
   public final Setting<Boolean> hpCheck = this.register(new Setting<>("HPCheck", false));
   public final Setting<Float> hpThreshold = this.register(new Setting<>("HPThreshold", 10.0F, 0.5F, 20.0F, v -> this.hpCheck.getValue()));
   private final AutoTotemDelay timer = new AutoTotemDelay();
   private LocalPlayer owner;
   private ClientLevel level;
   private int savedOffhandSlot = -1;
   private ItemStack savedOffhand = ItemStack.EMPTY;

   public AutoTotem() {
      super("AutoTotem", "Always keeps a Totem of Undying in your offhand.", Module.Category.PLAYER);
   }

   @Override
   public void onEnable() { this.clearState(); }

   @Override
   public void onDisable() { this.clearState(); }

   private void clearState() {
      this.timer.reset(); this.owner = mc.player; this.level = mc.level;
      this.savedOffhandSlot = -1;
      this.savedOffhand = ItemStack.EMPTY;
   }

   @Override
   public void onTick() {
      if (this.owner != mc.player || this.level != mc.level) this.clearState();
      if (nullCheck() || mc.gameMode == null || !mc.player.isAlive() || mc.player.isSpectator()
         || mc.player.containerMenu != mc.player.inventoryMenu || !mc.player.inventoryMenu.getCarried().isEmpty()
         || mc.screen != null && !(mc.screen instanceof InventoryScreen)
         || ViaProtocol.usesSwordBlocking()) {
         this.timer.reset(); return;
      }
      boolean spoof = this.inventorySpoof.getValue() && ViaProtocol.supportsModernInventorySpoof();
      if (!spoof && !(mc.screen instanceof InventoryScreen)) { this.timer.reset(); return; }
      if (!this.hpCheck.getValue()) { this.savedOffhandSlot = -1; this.savedOffhand = ItemStack.EMPTY; }
      boolean restore = this.hpCheck.getValue() && mc.player.getHealth() > this.hpThreshold.getValue();
      int slot;
      if (restore) {
         slot = this.savedOffhandSlot;
         if (slot < 0 || !mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
            || !sameStack(this.savedOffhand, mc.player.getInventory().getItem(slot))) {
            this.savedOffhandSlot = -1; this.savedOffhand = ItemStack.EMPTY; this.timer.reset(); return;
         }
      } else {
         if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) { this.timer.reset(); return; }
         slot = this.findTotem();
         if (slot < 0) { this.timer.reset(); return; }
      }
      if (!this.timer.ready(System.nanoTime(), this.delay.getValue(), slot + (restore ? 64 : 0))) return;
      ItemStack oldOffhand = mc.player.getOffhandItem().copy();
      LocalPlayer player = mc.player;
      boolean swapped = InventorySwapSession.run(spoof, mc.screen instanceof InventoryScreen,
         () -> mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, InventorySwapSession.menuSlot(slot), 40, ContainerInput.SWAP, player),
         () -> {
            if (mc.player == player && player.containerMenu == player.inventoryMenu && mc.screen == null)
               player.connection.send(new ServerboundContainerClosePacket(player.inventoryMenu.containerId));
         });
      if (!swapped) return;
      this.timer.reset();
      if (restore) { this.savedOffhandSlot = -1; this.savedOffhand = ItemStack.EMPTY; }
      else if (this.hpCheck.getValue() && this.savedOffhandSlot < 0 && player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
         this.savedOffhandSlot = slot; this.savedOffhand = oldOffhand;
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

   private static boolean sameStack(ItemStack first, ItemStack second) {
      return first.getCount() == second.getCount() && ItemStack.isSameItemSameComponents(first, second);
   }
}
