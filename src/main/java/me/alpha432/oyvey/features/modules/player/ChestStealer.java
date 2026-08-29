package me.alpha432.oyvey.features.modules.player;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.equipment.Equippable;

public class ChestStealer extends Module {
   public final Setting<Integer> delay = this.register(new Setting<>("Delay", 100, 0, 2000));
   public final Setting<Boolean> closeWhenDone = this.register(new Setting<>("CloseWhenDone", false));
   public final Setting<Boolean> fixTest = this.register(new Setting<>("FixTest", false));
   public final Setting<Boolean> filter = this.register(new Setting<>("Filter", false));
   public final Setting<Boolean> skipOwned = this.register(new Setting<>("SkipDupes", true));
   public final Setting<Boolean> takeArmor = this.register(new Setting<>("Armor", true, v -> this.filter.getValue()));
   public final Setting<Boolean> takeWeapons = this.register(new Setting<>("Weapons", true, v -> this.filter.getValue()));
   public final Setting<Boolean> takeTools = this.register(new Setting<>("Tools", true, v -> this.filter.getValue()));
   public final Setting<Boolean> takeFood = this.register(new Setting<>("Food", true, v -> this.filter.getValue()));
   public final Setting<Boolean> takeBlocks = this.register(new Setting<>("Blocks", true, v -> this.filter.getValue()));
   public final Setting<Boolean> takePearls = this.register(new Setting<>("Pearls", true, v -> this.filter.getValue()));
   public final Setting<Boolean> takeGaps = this.register(new Setting<>("GoldenApples", true, v -> this.filter.getValue()));
   public final Setting<Boolean> takeTotems = this.register(new Setting<>("Totems", true, v -> this.filter.getValue()));
   public final Setting<Boolean> takeOther = this.register(new Setting<>("Other", false, v -> this.filter.getValue()));
   public final Setting<Boolean> handleFullInv = this.register(new Setting<>("HandleFullInv", false));
   private long lastActionMs = 0L;
   private int windowId = -1;
   private int dragState = 0;

   public ChestStealer() {
      super("ChestStealer", "Steals all items from an open chest.", Module.Category.PLAYER);
   }

   @Override
   public void onDisable() {
      this.lastActionMs = 0L;
      this.windowId = -1;
      this.dragState = 0;
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.level != null) {
         if (mc.screen instanceof AbstractContainerScreen) {
            if (mc.player.containerMenu instanceof ChestMenu menu) {
               if (menu.containerId != this.windowId) {
                  this.windowId = menu.containerId;
                  this.lastActionMs = 0L;
                  this.dragState = 0;
               }

               long now = System.currentTimeMillis();
               if (now - this.lastActionMs >= this.delay.getValue().intValue()) {
                  int chestSlots = menu.getRowCount() * 9;
                  if (this.fixTest.getValue()) {
                     this.tickFixTest(menu, chestSlots, now);
                  } else {
                     this.tickNormal(menu, chestSlots, now);
                  }
               }
            }
         }
      }
   }

   private void tickNormal(ChestMenu menu, int chestSlots, long now) {
      if (this.handleFullInv.getValue() && this.isPlayerInvFull()) {
         int junkSlot = this.findPlayerJunkSlot(menu, chestSlots);
         if (junkSlot >= 0) {
            mc.gameMode.handleContainerInput(menu.containerId, junkSlot, 1, ContainerInput.THROW, mc.player);
            this.lastActionMs = now;
         }
      } else {
         for (int i = 0; i < chestSlots; i++) {
            Slot slot = (Slot)menu.slots.get(i);
            if (slot.hasItem() && this.isWanted(slot.getItem())) {
               mc.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
               this.lastActionMs = now;
               return;
            }
         }

         if (this.closeWhenDone.getValue()) {
            mc.player.closeContainer();
         }
      }
   }

   private void tickFixTest(ChestMenu menu, int chestSlots, long now) {
      if (this.dragState == 0) {
         if (this.handleFullInv.getValue() && this.isPlayerInvFull()) {
            int junkSlot = this.findPlayerJunkSlot(menu, chestSlots);
            if (junkSlot >= 0) {
               mc.gameMode.handleContainerInput(menu.containerId, junkSlot, 1, ContainerInput.THROW, mc.player);
               this.lastActionMs = now;
            }

            return;
         }

         for (int i = 0; i < chestSlots; i++) {
            Slot slot = (Slot)menu.slots.get(i);
            if (slot.hasItem() && this.isWanted(slot.getItem())) {
               mc.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.PICKUP, mc.player);
               this.lastActionMs = now;
               this.dragState = 1;
               return;
            }
         }

         if (this.closeWhenDone.getValue()) {
            mc.player.closeContainer();
         }
      } else {
         int freeSlot = this.findFreePlayerSlot(menu, chestSlots);
         if (freeSlot >= 0) {
            mc.gameMode.handleContainerInput(menu.containerId, freeSlot, 0, ContainerInput.PICKUP, mc.player);
         } else {
            mc.gameMode.handleContainerInput(menu.containerId, -999, 0, ContainerInput.PICKUP, mc.player);
         }

         this.lastActionMs = now;
         this.dragState = 0;
      }
   }

   private int findFreePlayerSlot(ChestMenu menu, int chestSlots) {
      ItemStack cursor = mc.player.containerMenu.getCarried();
      if (!cursor.isEmpty()) {
         for (int i = chestSlots; i < chestSlots + 36; i++) {
            ItemStack s = ((Slot)menu.slots.get(i)).getItem();
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, cursor) && s.getCount() < s.getMaxStackSize()) {
               return i;
            }
         }
      }

      for (int i = chestSlots; i < chestSlots + 27; i++) {
         if (!((Slot)menu.slots.get(i)).hasItem()) {
            return i;
         }
      }

      for (int i = chestSlots + 27; i < chestSlots + 36; i++) {
         if (!((Slot)menu.slots.get(i)).hasItem()) {
            return i;
         }
      }

      return -1;
   }

   private boolean isWanted(ItemStack stack) {
      if (this.skipOwned.getValue() && this.alreadyOwnBetterOrEqual(stack)) {
         return false;
      }

      if (!this.filter.getValue()) {
         return true;
      }

      Equippable eq = (Equippable)stack.get(DataComponents.EQUIPPABLE);
      if (eq != null) {
         EquipmentSlot s = eq.slot();
         if (s == EquipmentSlot.HEAD || s == EquipmentSlot.CHEST || s == EquipmentSlot.LEGS || s == EquipmentSlot.FEET) {
            return this.takeArmor.getValue();
         }
      }

      if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)) {
         return this.takeWeapons.getValue();
      } else if (stack.is(ItemTags.PICKAXES)) {
         return this.takeTools.getValue();
      } else if (stack.getItem() == Items.ENDER_PEARL) {
         return this.takePearls.getValue();
      } else if (stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
         return this.takeGaps.getValue();
      } else if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
         return this.takeTotems.getValue();
      } else if (stack.has(DataComponents.FOOD)) {
         return this.takeFood.getValue();
      } else {
         return stack.getItem() instanceof BlockItem ? this.takeBlocks.getValue() : this.takeOther.getValue();
      }
   }

   private boolean alreadyOwnBetterOrEqual(ItemStack chestItem) {
      if (!chestItem.isEmpty() && mc.player != null) {
         Equippable eq = (Equippable)chestItem.get(DataComponents.EQUIPPABLE);
         if (eq != null && isArmorSlot(eq.slot())) {
            double chestScore = this.armorScore(chestItem);
            int chestEnch = this.enchantPower(chestItem);
            if (this.dominatesArmor(mc.player.getItemBySlot(eq.slot()), chestScore, chestEnch)) {
               return true;
            }

            for (int i = 0; i < 36; i++) {
               ItemStack s = mc.player.getInventory().getItem(i);
               Equippable e2 = (Equippable)s.get(DataComponents.EQUIPPABLE);
               if (e2 != null && e2.slot() == eq.slot() && this.dominatesArmor(s, chestScore, chestEnch)) {
                  return true;
               }
            }

            return false;
         } else {
            TagKey<Item> tag = gearTag(chestItem);
            if (tag != null) {
               double chestScore = this.meleeScore(chestItem);
               int chestEnch = this.enchantPower(chestItem);
               ItemStack offhand = mc.player.getOffhandItem();
               if (offhand.is(tag) && this.meleeScore(offhand) >= chestScore && this.enchantPower(offhand) >= chestEnch) {
                  return true;
               }

               for (int i = 0; i < 36; i++) {
                  ItemStack s = mc.player.getInventory().getItem(i);
                  if (s.is(tag) && this.meleeScore(s) >= chestScore && this.enchantPower(s) >= chestEnch) {
                     return true;
                  }
               }

               return false;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static boolean isArmorSlot(EquipmentSlot s) {
      return s == EquipmentSlot.HEAD || s == EquipmentSlot.CHEST || s == EquipmentSlot.LEGS || s == EquipmentSlot.FEET;
   }

   private boolean dominatesArmor(ItemStack owned, double chestScore, int chestEnch) {
      return !owned.isEmpty() && this.armorScore(owned) >= chestScore && this.enchantPower(owned) >= chestEnch;
   }

   private static TagKey<Item> gearTag(ItemStack stack) {
      if (stack.is(ItemTags.SWORDS)) {
         return ItemTags.SWORDS;
      } else if (stack.is(ItemTags.AXES)) {
         return ItemTags.AXES;
      } else {
         return stack.is(ItemTags.PICKAXES) ? ItemTags.PICKAXES : null;
      }
   }

   private double meleeScore(ItemStack stack) {
      ItemAttributeModifiers mods = (ItemAttributeModifiers)stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
      if (mods == null) {
         return 0.0;
      }

      double dmg = 0.0;

      for (net.minecraft.world.item.component.ItemAttributeModifiers.Entry e : mods.modifiers()) {
         if (e.attribute().is(Attributes.ATTACK_DAMAGE)) {
            dmg += e.modifier().amount();
         }
      }

      return dmg;
   }

   private int enchantPower(ItemStack stack) {
      int total = 0;

      for (Entry<Holder<Enchantment>> e : stack.getEnchantments().entrySet()) {
         total += e.getIntValue();
      }

      return total;
   }

   private boolean isPlayerInvFull() {
      for (int i = 0; i < 36; i++) {
         ItemStack s = mc.player.getInventory().getItem(i);
         if (s.isEmpty() || s.getCount() < s.getMaxStackSize()) {
            return false;
         }
      }

      return true;
   }

   private int findPlayerJunkSlot(ChestMenu menu, int chestSlots) {
      for (int i = chestSlots; i < chestSlots + 36; i++) {
         Slot slot = (Slot)menu.slots.get(i);
         if (slot.hasItem() && this.isJunk(slot.getItem())) {
            return i;
         }
      }

      return -1;
   }

   private boolean isJunk(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      if (!stack.is(ItemTags.SHOVELS) && !stack.is(ItemTags.HOES)) {
         Equippable eq = (Equippable)stack.get(DataComponents.EQUIPPABLE);
         if (eq != null) {
            EquipmentSlot eqSlot = eq.slot();
            if (eqSlot == EquipmentSlot.HEAD || eqSlot == EquipmentSlot.CHEST || eqSlot == EquipmentSlot.LEGS || eqSlot == EquipmentSlot.FEET) {
               ItemStack equipped = mc.player.getItemBySlot(eqSlot);
               return !equipped.isEmpty() && this.armorScore(equipped) >= this.armorScore(stack);
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private double armorScore(ItemStack stack) {
      ItemAttributeModifiers mods = (ItemAttributeModifiers)stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
      if (mods == null) {
         return 0.0;
      }

      double score = 0.0;

      for (net.minecraft.world.item.component.ItemAttributeModifiers.Entry e : mods.modifiers()) {
         if (e.attribute().is(Attributes.ARMOR)) {
            score += e.modifier().amount();
         }

         if (e.attribute().is(Attributes.ARMOR_TOUGHNESS)) {
            score += e.modifier().amount() * 0.5;
         }
      }

      return score;
   }
}
