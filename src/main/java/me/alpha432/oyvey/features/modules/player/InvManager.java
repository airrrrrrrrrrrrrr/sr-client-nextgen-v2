package me.alpha432.oyvey.features.modules.player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.EnchantmentUtil;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.Equippable;

public class InvManager extends Module {
   public final Setting<Integer> delay = this.register(new Setting<>("Delay", 150, 0, 2000));
   public final Setting<Boolean> silent = this.register(new Setting<>("Silent", false));
   public final Setting<Boolean> autoArmor = this.register(new Setting<>("AutoArmor", true));
   public final Setting<Boolean> autoSort = this.register(new Setting<>("AutoSort", true));
   public final Setting<Boolean> throwJunk = this.register(new Setting<>("ThrowJunk", false));
   public final Setting<Boolean> dropDupes = this.register(new Setting<>("DropDupArmor", false));
   public final Setting<Boolean> dupeWeapons = this.register(new Setting<>("AlsoWeapons", true, v -> this.dropDupes.getValue()));
   public final Setting<Boolean> limitBlocks = this.register(new Setting<>("LimitBlocks", false));
   public final Setting<Integer> dropDelay = this.register(new Setting<>("DropDelay", 200, 0, 2000, v -> this.dropDupes.getValue()));
   public final Setting<Integer> dropJitter = this.register(new Setting<>("DropJitter", 100, 0, 1000, v -> this.dropDupes.getValue()));
   public final Setting<Integer> maxDropsPerCycle = this.register(new Setting<>("MaxDropsPerCycle", 2, 1, 10, v -> this.dropDupes.getValue()));
   public final Setting<Boolean> keepBackup = this.register(new Setting<>("KeepBackup", true, v -> this.dropDupes.getValue()));
   public final Setting<Integer> minDurability = this.register(
      new Setting<>("MinDurability", 20, 0, 100, v -> this.dropDupes.getValue() && this.keepBackup.getValue())
   );
   public final Setting<Boolean> preferEnchanted = this.register(
      new Setting<>("PreferEnchanted", true, v -> this.dropDupes.getValue() || this.autoArmor.getValue())
   );
   public final Setting<Boolean> saveArmor = this.register(new Setting<>("SaveArmor", false, v -> this.autoArmor.getValue()));
   public final Setting<Integer> durabilityThreshold = this.register(
      new Setting<>("DurabilityThreshold", 24, 0, 100, v -> this.autoArmor.getValue() && this.saveArmor.getValue())
   );
   public final Setting<Boolean> autoOpenInv = this.register(
      new Setting<>("AutoOpenInventory", true, v -> this.autoArmor.getValue() && this.saveArmor.getValue() && !this.silent.getValue())
   );
   public final Setting<Integer> enchantWeight = this.register(
      new Setting<>("EnchantWeight", 10, 1, 30, v -> this.dropDupes.getValue() || this.autoArmor.getValue())
   );
   public final Setting<Integer> durabilityWeight = this.register(
      new Setting<>("DurabilityWeight", 5, 1, 20, v -> this.dropDupes.getValue() || this.autoArmor.getValue())
   );
   public final Setting<Integer> maxBlocks = this.register(new Setting<>("MaxBlocks", 256, 1, 640, v -> this.limitBlocks.getValue()));
   public final Setting<Integer> swordSlot = this.register(new Setting<>("SwordSlot", 1, 1, 9, v -> this.autoSort.getValue()));
   public final Setting<Integer> pickaxeSlot = this.register(new Setting<>("PickaxeSlot", 2, 1, 9, v -> this.autoSort.getValue()));
   public final Setting<Integer> axeSlot = this.register(new Setting<>("AxeSlot", 3, 1, 9, v -> this.autoSort.getValue()));
   public final Setting<Integer> blockSlot = this.register(new Setting<>("BlockSlot", 4, 1, 9, v -> this.autoSort.getValue()));
   public final Setting<Integer> foodSlot = this.register(new Setting<>("FoodSlot", 5, 1, 9, v -> this.autoSort.getValue()));
   public final Setting<Integer> pearlSlot = this.register(new Setting<>("PearlSlot", 6, 1, 9, v -> this.autoSort.getValue()));
   public final Setting<Integer> rodSlot = this.register(new Setting<>("RodSlot", 7, 1, 9, v -> this.autoSort.getValue()));
   public final Setting<Integer> gapSlot = this.register(new Setting<>("GapSlot", 8, 1, 9, v -> this.autoSort.getValue()));
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final float EXPECTED_DAMAGE = 10.0F;
   private long lastActionMs = 0L;
   private long dropReadyAtMs = 0L;
   private boolean autoOpenedInventory = false;
   private final Deque<Integer> dropQueue = new ArrayDeque<>();
   private final Random rng = new Random();

   public InvManager() {
      super("InvManager", "Manages inventory when open.", Module.Category.PLAYER);
   }

   @Override
   public void onDisable() {
      this.lastActionMs = 0L;
      this.dropReadyAtMs = 0L;
      this.dropQueue.clear();
      if (this.autoOpenedInventory && !this.silent.getValue() && mc.screen instanceof InventoryScreen && mc.player != null) {
         mc.player.closeContainer();
      }

      this.autoOpenedInventory = false;
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.level != null) {
         if (this.silent.getValue()) {
            this.runInvLogic(mc.player.inventoryMenu);
         } else {
            if (this.autoArmor.getValue() && this.saveArmor.getValue() && this.autoOpenInv.getValue()) {
               if (this.needsArmorReplacement() && !(mc.screen instanceof InventoryScreen)) {
                  mc.setScreen(new InventoryScreen(mc.player));
                  this.autoOpenedInventory = true;
               } else if (this.autoOpenedInventory && !this.needsArmorReplacement() && mc.screen instanceof InventoryScreen) {
                  mc.player.closeContainer();
                  this.autoOpenedInventory = false;
               }
            }

            if (mc.screen instanceof InventoryScreen) {
               this.runInvLogic(mc.player.inventoryMenu);
            }
         }
      }
   }

   private void runInvLogic(InventoryMenu inv) {
      long now = System.currentTimeMillis();
      if (!this.dropQueue.isEmpty()) {
         if (now >= this.dropReadyAtMs) {
            this.processDropQueue(inv, now);
         }
      } else if (now - this.lastActionMs >= this.delay.getValue().intValue()) {
         this.lastActionMs = now;
         if ((!this.autoArmor.getValue() || !this.tryAutoArmor(inv))
            && (!this.autoSort.getValue() || !this.tryAutoSort(inv))
            && (!this.throwJunk.getValue() || !this.tryThrowJunk(inv))
            && (!this.dropDupes.getValue() || !this.tryQueueDupes(inv))
            && this.limitBlocks.getValue()) {
            this.tryLimitBlocks(inv);
         }
      }
   }

   private void processDropQueue(InventoryMenu inv, long now) {
      int limit = this.maxDropsPerCycle.getValue();
      int dropped = 0;

      while (!this.dropQueue.isEmpty() && dropped < limit) {
         int slotIdx = this.dropQueue.poll();
         if (slotIdx >= 0 && slotIdx < inv.slots.size()) {
            Slot slot = (Slot)inv.slots.get(slotIdx);
            if (slot.hasItem() && this.isDupeDroppable(slot.getItem())) {
               mc.gameMode.handleContainerInput(inv.containerId, slotIdx, 1, ContainerInput.THROW, mc.player);
               dropped++;
            }
         }
      }

      int jitter = this.dropJitter.getValue() > 0 ? this.rng.nextInt(this.dropJitter.getValue()) : 0;
      this.dropReadyAtMs = now + this.dropDelay.getValue().intValue() + jitter;
   }

   private boolean needsArmorReplacement() {
      if (mc.player == null) {
         return false;
      }

      InventoryMenu inv = mc.player.inventoryMenu;

      for (EquipmentSlot eqSlot : ARMOR_SLOTS) {
         ItemStack equipped = mc.player.getItemBySlot(eqSlot);
         boolean belowThreshold = !equipped.isEmpty() && this.durabilityPct(equipped) * 100.0 <= this.durabilityThreshold.getValue().intValue();
         boolean slotEmpty = equipped.isEmpty();
         if (belowThreshold || slotEmpty) {
            for (int i = 9; i < 45; i++) {
               ItemStack stack = ((Slot)inv.slots.get(i)).getItem();
               Equippable eq = (Equippable)stack.get(DataComponents.EQUIPPABLE);
               if (eq != null && eq.slot() == eqSlot && (slotEmpty || this.evaluateArmor(stack, eqSlot) > 0.0)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean tryAutoArmor(InventoryMenu inv) {
      for (EquipmentSlot eqSlot : ARMOR_SLOTS) {
         ItemStack currentEquipped = mc.player.getItemBySlot(eqSlot);
         double currentScore = this.evaluateArmor(currentEquipped, eqSlot);
         if (this.saveArmor.getValue()
            && !currentEquipped.isEmpty()
            && this.durabilityPct(currentEquipped) * 100.0 <= this.durabilityThreshold.getValue().intValue()) {
            currentScore = 0.0;
         }

         int bestSlot = -1;
         double bestScore = currentScore;

         for (int i = 9; i < 45; i++) {
            ItemStack stack = ((Slot)inv.slots.get(i)).getItem();
            Equippable eq = (Equippable)stack.get(DataComponents.EQUIPPABLE);
            if (eq != null
               && eq.slot() == eqSlot
               && (!this.saveArmor.getValue() || !(this.durabilityPct(stack) * 100.0 <= this.durabilityThreshold.getValue().intValue()))) {
               double score = this.evaluateArmor(stack, eqSlot);
               if (score > bestScore) {
                  bestScore = score;
                  bestSlot = i;
               }
            }
         }

         if (bestSlot != -1) {
            this.equipArmor(inv, bestSlot, eqSlot);
            return true;
         }
      }

      return false;
   }

   private void equipArmor(InventoryMenu inv, int fromSlot, EquipmentSlot eqSlot) {
      ItemStack equipped = mc.player.getItemBySlot(eqSlot);
      if (equipped.isEmpty()) {
         mc.gameMode.handleContainerInput(inv.containerId, fromSlot, 0, ContainerInput.QUICK_MOVE, mc.player);
      } else {
         int armorSlot = armorMenuSlot(eqSlot);
         mc.gameMode.handleContainerInput(inv.containerId, fromSlot, 0, ContainerInput.PICKUP, mc.player);
         mc.gameMode.handleContainerInput(inv.containerId, armorSlot, 0, ContainerInput.PICKUP, mc.player);
         mc.gameMode.handleContainerInput(inv.containerId, fromSlot, 0, ContainerInput.PICKUP, mc.player);
      }
   }

   private static int armorMenuSlot(EquipmentSlot eqSlot) {
      if (eqSlot == EquipmentSlot.HEAD) {
         return 5;
      } else if (eqSlot == EquipmentSlot.CHEST) {
         return 6;
      } else {
         return eqSlot == EquipmentSlot.LEGS ? 7 : 8;
      }
   }

   private boolean tryAutoSort(InventoryMenu inv) {
      if (this.sortWeaponToSlot(inv, ItemTags.SWORDS, this.swordSlot.getValue() - 1)) {
         return true;
      } else if (this.sortWeaponToSlot(inv, ItemTags.PICKAXES, this.pickaxeSlot.getValue() - 1)) {
         return true;
      } else if (this.sortWeaponToSlot(inv, ItemTags.AXES, this.axeSlot.getValue() - 1)) {
         return true;
      } else if (this.sortBlockToSlot(inv, this.blockSlot.getValue() - 1)) {
         return true;
      } else if (this.sortFoodToSlot(inv, this.foodSlot.getValue() - 1)) {
         return true;
      } else if (this.sortItemToSlot(inv, this::isPearl, this.pearlSlot.getValue() - 1)) {
         return true;
      } else {
         return this.sortItemToSlot(inv, this::isRod, this.rodSlot.getValue() - 1) ? true : this.sortItemToSlot(inv, this::isGap, this.gapSlot.getValue() - 1);
      }
   }

   private boolean sortWeaponToSlot(InventoryMenu inv, TagKey<Item> tag, int hotbarIndex) {
      int targetSlot = 36 + hotbarIndex;
      ItemStack current = ((Slot)inv.slots.get(targetSlot)).getItem();
      int bestSlot = -1;
      double bestScore = current.is(tag) ? this.weaponScore(current) : -1.0;

      for (int i = 9; i < 45; i++) {
         if (i != targetSlot) {
            ItemStack stack = ((Slot)inv.slots.get(i)).getItem();
            if (stack.is(tag)) {
               double score = this.weaponScore(stack);
               if (score > bestScore) {
                  bestScore = score;
                  bestSlot = i;
               }
            }
         }
      }

      if (bestSlot == -1) {
         return false;
      }

      mc.gameMode.handleContainerInput(inv.containerId, bestSlot, hotbarIndex, ContainerInput.SWAP, mc.player);
      return true;
   }

   private boolean sortBlockToSlot(InventoryMenu inv, int hotbarIndex) {
      int targetSlot = 36 + hotbarIndex;
      if (((Slot)inv.slots.get(targetSlot)).getItem().getItem() instanceof BlockItem) {
         return false;
      }

      for (int i = 9; i < 45; i++) {
         if (i != targetSlot) {
            ItemStack stack = ((Slot)inv.slots.get(i)).getItem();
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
               mc.gameMode.handleContainerInput(inv.containerId, i, hotbarIndex, ContainerInput.SWAP, mc.player);
               return true;
            }
         }
      }

      return false;
   }

   private boolean sortFoodToSlot(InventoryMenu inv, int hotbarIndex) {
      int targetSlot = 36 + hotbarIndex;
      if (((Slot)inv.slots.get(targetSlot)).getItem().has(DataComponents.FOOD)) {
         return false;
      }

      for (int i = 9; i < 45; i++) {
         if (i != targetSlot) {
            ItemStack stack = ((Slot)inv.slots.get(i)).getItem();
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
               mc.gameMode.handleContainerInput(inv.containerId, i, hotbarIndex, ContainerInput.SWAP, mc.player);
               return true;
            }
         }
      }

      return false;
   }

   private boolean sortItemToSlot(InventoryMenu inv, Predicate<ItemStack> matcher, int hotbarIndex) {
      int targetSlot = 36 + hotbarIndex;
      if (matcher.test(((Slot)inv.slots.get(targetSlot)).getItem())) {
         return false;
      }

      for (int i = 9; i < 45; i++) {
         if (i != targetSlot) {
            ItemStack stack = ((Slot)inv.slots.get(i)).getItem();
            if (!stack.isEmpty() && matcher.test(stack)) {
               mc.gameMode.handleContainerInput(inv.containerId, i, hotbarIndex, ContainerInput.SWAP, mc.player);
               return true;
            }
         }
      }

      return false;
   }

   private boolean tryThrowJunk(InventoryMenu inv) {
      for (int i = 9; i < 36; i++) {
         Slot slot = (Slot)inv.slots.get(i);
         if (slot.hasItem() && this.isJunk(slot.getItem())) {
            mc.gameMode.handleContainerInput(inv.containerId, i, 1, ContainerInput.THROW, mc.player);
            return true;
         }
      }

      if (this.throwDuplicateTool(inv, ItemTags.SWORDS)) {
         return true;
      } else {
         return this.throwDuplicateTool(inv, ItemTags.PICKAXES) ? true : this.throwDuplicateTool(inv, ItemTags.AXES);
      }
   }

   private boolean throwDuplicateTool(InventoryMenu inv, TagKey<Item> tag) {
      int bestSlot = -1;
      double bestScore = -1.0;

      for (int i = 9; i < 45; i++) {
         ItemStack stack = ((Slot)inv.slots.get(i)).getItem();
         if (stack.is(tag)) {
            double score = this.toolScore(stack);
            if (score > bestScore) {
               bestScore = score;
               bestSlot = i;
            }
         }
      }

      if (bestSlot == -1) {
         return false;
      }

      for (int ix = 9; ix < 45; ix++) {
         if (ix != bestSlot) {
            ItemStack stack = ((Slot)inv.slots.get(ix)).getItem();
            if (stack.is(tag)) {
               mc.gameMode.handleContainerInput(inv.containerId, ix, 1, ContainerInput.THROW, mc.player);
               return true;
            }
         }
      }

      return false;
   }

   private boolean tryQueueDupes(InventoryMenu inv) {
      if (!this.dropQueue.isEmpty()) {
         return false;
      }

      int maxQueue = this.maxDropsPerCycle.getValue();
      this.queueDupeArmor(inv, maxQueue);
      if (this.dupeWeapons.getValue()) {
         this.queueDupeWeapons(inv, ItemTags.SWORDS, maxQueue);
         this.queueDupeWeapons(inv, ItemTags.PICKAXES, maxQueue);
         this.queueDupeWeapons(inv, ItemTags.AXES, maxQueue);
      }

      return !this.dropQueue.isEmpty();
   }

   private void queueDupeArmor(InventoryMenu inv, int maxQueue) {
      for (EquipmentSlot eqSlot : ARMOR_SLOTS) {
         if (this.dropQueue.size() >= maxQueue) {
            break;
         }

         List<InvManager.ArmorCandidate> loose = new ArrayList<>();

         for (int i = 9; i < 45; i++) {
            ItemStack stack = ((Slot)inv.slots.get(i)).getItem();
            Equippable eq = (Equippable)stack.get(DataComponents.EQUIPPABLE);
            if (eq != null && eq.slot() == eqSlot) {
               loose.add(new InvManager.ArmorCandidate(i, stack, this.evaluateArmor(stack, eqSlot)));
            }
         }

         if (!loose.isEmpty()) {
            loose.sort((a, b) -> Double.compare(b.score, a.score));
            ItemStack equipped = mc.player.getItemBySlot(eqSlot);
            double equippedScore = equipped.isEmpty() ? -1.0 : this.evaluateArmor(equipped, eqSlot);
            int keepCount = !equipped.isEmpty() && equippedScore >= loose.get(0).score ? 0 : 1;
            if (this.keepBackup.getValue() && loose.size() > keepCount) {
               ItemStack primary = keepCount == 0 ? equipped : loose.get(0).stack;
               if (this.shouldKeepBackup(primary, loose.get(keepCount).stack)) {
                  keepCount++;
               }
            }

            for (int k = keepCount; k < loose.size() && this.dropQueue.size() < maxQueue; k++) {
               this.dropQueue.add(loose.get(k).slot);
            }
         }
      }
   }

   private void queueDupeWeapons(InventoryMenu inv, TagKey<Item> tag, int maxQueue) {
      if (this.dropQueue.size() < maxQueue) {
         List<InvManager.ArmorCandidate> loose = new ArrayList<>();

         for (int i = 9; i < 45; i++) {
            ItemStack stack = ((Slot)inv.slots.get(i)).getItem();
            if (stack.is(tag)) {
               loose.add(new InvManager.ArmorCandidate(i, stack, this.toolScore(stack)));
            }
         }

         if (loose.size() >= 2) {
            loose.sort((a, b) -> Double.compare(b.score, a.score));

            for (int k = 1; k < loose.size() && this.dropQueue.size() < maxQueue; k++) {
               this.dropQueue.add(loose.get(k).slot);
            }
         }
      }
   }

   private boolean tryLimitBlocks(InventoryMenu inv) {
      int cap = this.maxBlocks.getValue();
      int total = 0;

      for (int i = 9; i < 36; i++) {
         ItemStack s = ((Slot)inv.slots.get(i)).getItem();
         if (!s.isEmpty() && s.getItem() instanceof BlockItem) {
            total += s.getCount();
         }
      }

      if (total <= cap) {
         return false;
      }

      for (int ix = 9; ix < 36; ix++) {
         ItemStack s = ((Slot)inv.slots.get(ix)).getItem();
         if (!s.isEmpty() && s.getItem() instanceof BlockItem) {
            mc.gameMode.handleContainerInput(inv.containerId, ix, 1, ContainerInput.THROW, mc.player);
            return true;
         }
      }

      return false;
   }

   private double evaluateArmor(ItemStack stack, EquipmentSlot slot) {
      if (stack.isEmpty()) {
         return 0.0;
      }

      double base = this.baseArmorScore(stack);
      double enchants = this.enchantmentScore(stack, slot);
      double durFactor = this.durabilityFactor(stack);
      double ew = this.enchantWeight.getValue().intValue() / 10.0;
      double dw = this.durabilityWeight.getValue().intValue() / 10.0;
      double enchantBonus = this.preferEnchanted.getValue() && !stack.getEnchantments().isEmpty() ? 1.5 : 0.0;
      double rawScore = base + enchantBonus + enchants * ew;
      double durPenalty = 1.0 - dw * (1.0 - durFactor);
      return rawScore * durPenalty;
   }

   private double baseArmorScore(ItemStack stack) {
      ItemAttributeModifiers mods = (ItemAttributeModifiers)stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
      if (mods == null) {
         return 0.0;
      }

      double defense = 0.0;
      double toughness = 0.0;

      for (Entry e : mods.modifiers()) {
         if (e.attribute().is(Attributes.ARMOR)) {
            defense += e.modifier().amount();
         }

         if (e.attribute().is(Attributes.ARMOR_TOUGHNESS)) {
            toughness += e.modifier().amount();
         }
      }

      double f = 2.0 + toughness / 4.0;
      return Math.min(20.0, Math.max(defense * 0.2, defense - 10.0 / f));
   }

   private double enchantmentScore(ItemStack stack, EquipmentSlot slot) {
      double score = 0.0;
      score += EnchantmentUtil.getLevel(Enchantments.PROTECTION, stack) * 1.5;
      score += EnchantmentUtil.getLevel(Enchantments.BLAST_PROTECTION, stack) * 1.2;
      score += EnchantmentUtil.getLevel(Enchantments.PROJECTILE_PROTECTION, stack) * 1.0;
      score += EnchantmentUtil.getLevel(Enchantments.FIRE_PROTECTION, stack) * 0.8;
      score += EnchantmentUtil.getLevel(Enchantments.THORNS, stack) * 0.8;
      score += EnchantmentUtil.getLevel(Enchantments.UNBREAKING, stack) * 0.5;
      score += EnchantmentUtil.getLevel(Enchantments.MENDING, stack) * 3.0;
      if (slot == EquipmentSlot.FEET) {
         score += EnchantmentUtil.getLevel(Enchantments.FEATHER_FALLING, stack) * 1.5;
      }

      return score;
   }

   private double durabilityFactor(ItemStack stack) {
      int maxDamage = stack.getMaxDamage();
      return maxDamage <= 0 ? 1.0 : Math.max(0.3, 1.0 - (double)stack.getDamageValue() / maxDamage);
   }

   private double durabilityPct(ItemStack stack) {
      int maxDamage = stack.getMaxDamage();
      return maxDamage <= 0 ? 1.0 : 1.0 - (double)stack.getDamageValue() / maxDamage;
   }

   private boolean shouldKeepBackup(ItemStack best, ItemStack backup) {
      double bestPct = this.durabilityPct(best) * 100.0;
      double backupPct = this.durabilityPct(backup) * 100.0;
      return bestPct < this.minDurability.getValue().intValue() && backupPct > bestPct * 1.5;
   }

   private boolean isArmorItem(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      Equippable eq = (Equippable)stack.get(DataComponents.EQUIPPABLE);
      if (eq == null) {
         return false;
      }

      EquipmentSlot s = eq.slot();
      return s == EquipmentSlot.HEAD || s == EquipmentSlot.CHEST || s == EquipmentSlot.LEGS || s == EquipmentSlot.FEET;
   }

   private boolean isDupeDroppable(ItemStack stack) {
      return this.isArmorItem(stack) || stack.is(ItemTags.SWORDS) || stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES);
   }

   private boolean isJunk(ItemStack stack) {
      if (!stack.is(ItemTags.SHOVELS) && !stack.is(ItemTags.HOES)) {
         Equippable eq = (Equippable)stack.get(DataComponents.EQUIPPABLE);
         if (eq != null) {
            EquipmentSlot eqSlot = eq.slot();
            if (eqSlot == EquipmentSlot.HEAD || eqSlot == EquipmentSlot.CHEST || eqSlot == EquipmentSlot.LEGS || eqSlot == EquipmentSlot.FEET) {
               ItemStack equipped = mc.player.getItemBySlot(eqSlot);
               if (equipped.isEmpty()) {
                  return false;
               }

               return this.evaluateArmor(equipped, eqSlot) > this.evaluateArmor(stack, eqSlot);
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private double weaponScore(ItemStack stack) {
      if (stack.isEmpty()) {
         return 0.0;
      }

      ItemAttributeModifiers mods = (ItemAttributeModifiers)stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
      if (mods == null) {
         return 0.0;
      }

      double total = 0.0;

      for (Entry e : mods.modifiers()) {
         if (e.attribute().is(Attributes.ATTACK_DAMAGE)) {
            total += e.modifier().amount();
         }
      }

      return total;
   }

   private double toolScore(ItemStack stack) {
      if (stack.isEmpty()) {
         return 0.0;
      }

      double score = this.weaponScore(stack);
      score += EnchantmentUtil.getLevel(Enchantments.SHARPNESS, stack) * 0.5;
      score += EnchantmentUtil.getLevel(Enchantments.SMITE, stack) * 0.3;
      score += EnchantmentUtil.getLevel(Enchantments.FIRE_ASPECT, stack) * 0.4;
      score += EnchantmentUtil.getLevel(Enchantments.EFFICIENCY, stack) * 0.4;
      score += EnchantmentUtil.getLevel(Enchantments.FORTUNE, stack) * 0.5;
      score += EnchantmentUtil.getLevel(Enchantments.SILK_TOUCH, stack) * 0.5;
      score += EnchantmentUtil.getLevel(Enchantments.UNBREAKING, stack) * 0.2;
      return score + EnchantmentUtil.getLevel(Enchantments.MENDING, stack) * 1.0;
   }

   private boolean isPearl(ItemStack stack) {
      return stack.getItem() == Items.ENDER_PEARL;
   }

   private boolean isRod(ItemStack stack) {
      return stack.getItem() == Items.FISHING_ROD;
   }

   private boolean isGap(ItemStack stack) {
      return stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
   }

   private static final class ArmorCandidate {
      final int slot;
      final ItemStack stack;
      final double score;

      ArmorCandidate(int slot, ItemStack stack, double score) {
         this.slot = slot;
         this.stack = stack;
         this.score = score;
      }
   }
}
