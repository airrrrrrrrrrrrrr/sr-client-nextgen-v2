package me.alpha432.oyvey.features.modules.misc;

import java.util.Comparator;
import java.util.stream.StreamSupport;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.phys.Vec3;

public class SkyWarsAutoPlay extends Module {
   private double moveX = 0.0;
   private double moveZ = 0.0;
   private boolean doJump = false;

   public SkyWarsAutoPlay() {
      super("SkyWarsAutoPlay", "AI that plays SkyWars automatically.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      this.moveX = this.moveZ = 0.0;
      this.doJump = false;
   }

   @Override
   public void onDisable() {
      this.moveX = this.moveZ = 0.0;
      this.doJump = false;
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         this.equipBestArmor();
         this.equipBestWeapon();
         LivingEntity target = this.nearestPlayer();
         if (target == null) {
            this.moveX = this.moveZ = 0.0;
         } else {
            this.navigateTo(target.position());
         }
      }
   }

   @Subscribe
   public void onWalk(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == Stage.MOTION_PRE && mc.player != null && (this.moveX != 0.0 || this.moveZ != 0.0)) {
         float yaw = (float)(Math.toDegrees(Math.atan2(this.moveZ, this.moveX)) - 90.0);
         OyVey.rotationManager.setPlayerRotations(yaw, 10.0F);
         mc.player.setSprinting(true);
         mc.player.zza = 1.0F;
         mc.player.xxa = 0.0F;
         if (this.doJump && mc.player.onGround()) {
            mc.player.jumpFromGround();
         }
      }
   }

   private void navigateTo(Vec3 goal) {
      double dx = goal.x - mc.player.getX();
      double dz = goal.z - mc.player.getZ();
      double dist = Math.sqrt(dx * dx + dz * dz);
      if (dist < 0.5) {
         this.moveX = this.moveZ = 0.0;
      } else {
         double nx = dx / dist;
         double nz = dz / dist;
         BlockPos belowNext = BlockPos.containing(mc.player.getX() + nx * 1.5, mc.player.getY() - 1.0, mc.player.getZ() + nz * 1.5);
         if (mc.player.onGround() && mc.level.getBlockState(belowNext).isAir()) {
            this.moveX = this.moveZ = 0.0;
            this.doJump = false;
         } else {
            BlockPos front = BlockPos.containing(mc.player.getX() + nx * 0.7, mc.player.getY() + 0.1, mc.player.getZ() + nz * 0.7);
            this.doJump = !mc.level.getBlockState(front).isAir() && mc.player.onGround();
            this.moveX = nx * 0.28;
            this.moveZ = nz * 0.28;
         }
      }
   }

   private LivingEntity nearestPlayer() {
      return StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), false)
         .filter(e -> e instanceof Player && e != mc.player && e.isAlive())
         .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
         .map(e -> (LivingEntity)e)
         .orElse(null);
   }

   private void equipBestArmor() {
      InventoryMenu inv = mc.player.inventoryMenu;

      for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         ItemStack worn = mc.player.getItemBySlot(slot);

         for (int i = 9; i < 45; i++) {
            ItemStack s = ((Slot)inv.slots.get(i)).getItem();
            Equippable eq = (Equippable)s.get(DataComponents.EQUIPPABLE);
            if (eq != null && eq.slot() == slot && (worn.isEmpty() || this.tier(s) > this.tier(worn))) {
               mc.gameMode.handleContainerInput(inv.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
               break;
            }
         }
      }
   }

   private void equipBestWeapon() {
      InventoryMenu inv = mc.player.inventoryMenu;
      int bestSlot = -1;
      int bestScore = -1;

      for (int i = 9; i < 45; i++) {
         int sc = this.weaponScore(((Slot)inv.slots.get(i)).getItem());
         if (sc > bestScore) {
            bestScore = sc;
            bestSlot = i;
         }
      }

      if (bestSlot != -1 && bestScore > 0) {
         mc.gameMode.handleContainerInput(inv.containerId, bestSlot, 0, ContainerInput.SWAP, mc.player);
      }

      if (mc.player.getInventory().getSelectedSlot() != 0) {
         mc.player.getInventory().setSelectedSlot(0);
         mc.player.connection.send(new ServerboundSetCarriedItemPacket(0));
      }
   }

   private int tier(ItemStack s) {
      String id = s.getItem().toString();
      if (id.contains("netherite")) {
         return 5;
      } else if (id.contains("diamond")) {
         return 4;
      } else if (id.contains("iron")) {
         return 3;
      } else if (id.contains("gold")) {
         return 2;
      } else if (id.contains("chainmail")) {
         return 2;
      } else {
         return id.contains("leather") ? 1 : 0;
      }
   }

   private int weaponScore(ItemStack s) {
      if (s.isEmpty()) {
         return -1;
      }

      String id = s.getItem().toString();
      int t = 0;
      if (id.contains("netherite")) {
         t = 50;
      } else if (id.contains("diamond")) {
         t = 40;
      } else if (id.contains("iron")) {
         t = 30;
      } else if (id.contains("stone")) {
         t = 20;
      } else if (id.contains("gold")) {
         t = 15;
      } else if (id.contains("wood")) {
         t = 10;
      }

      if (id.contains("sword")) {
         return t + 2;
      } else {
         return id.contains("_axe") ? t + 1 : -1;
      }
   }
}
