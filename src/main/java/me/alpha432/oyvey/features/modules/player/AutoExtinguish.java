package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class AutoExtinguish extends Module {
   private AutoExtinguish.State state = AutoExtinguish.State.IDLE;
   private int waitTicks = 0;
   private int savedSlot = -1;
   private int waterSlot = -1;
   private ClientLevel savedLevel = null;
   private float savedPitch = 0.0F;
   private boolean rotInjected = false;

   public AutoExtinguish() {
      super("AutoExtinguish", "Silently places water bucket to extinguish fire.", Module.Category.PLAYER);
   }

   @Override
   public void onEnable() {
      this.resetState();
   }

   @Override
   public void onDisable() {
      if (this.state != AutoExtinguish.State.IDLE) {
         this.restoreSlot();
      }

      if (this.rotInjected && mc.player != null) {
         mc.player.setXRot(this.savedPitch);
         this.rotInjected = false;
      }

      this.state = AutoExtinguish.State.IDLE;
   }

   @Subscribe
   public void onWalk(UpdateWalkingPlayerEvent event) {
      if (mc.player != null && this.state != AutoExtinguish.State.IDLE) {
         if (event.getStage() == Stage.PRE) {
            this.savedPitch = mc.player.getXRot();
            mc.player.setXRot(90.0F);
            this.rotInjected = true;
            if (this.state == AutoExtinguish.State.PLACING) {
               this.placeWater();
            }

            if (this.state == AutoExtinguish.State.PICKING_UP) {
               this.pickUpWater();
            }
         } else if (event.getStage() == Stage.POST && this.rotInjected) {
            mc.player.setXRot(this.savedPitch);
            this.rotInjected = false;
         }
      }
   }

   @Override
   public void onTick() {
      if (mc.player == null || mc.level == null) {
         this.resetState();
      } else if (this.savedLevel != null && mc.level != this.savedLevel) {
         this.resetState();
      } else if (mc.player.isDeadOrDying()) {
         this.resetState();
      } else {
         switch (this.state) {
            case IDLE:
               if (!mc.player.isOnFire()) {
                  return;
               }

               this.waterSlot = this.findWaterBucket();
               if (this.waterSlot == -1) {
                  return;
               }

               this.savedSlot = mc.player.getInventory().getSelectedSlot();
               this.savedLevel = mc.level;
               mc.player.getInventory().setSelectedSlot(this.waterSlot);
               mc.player.connection.send(new ServerboundSetCarriedItemPacket(this.waterSlot));
               this.state = AutoExtinguish.State.PLACING;
               break;
            case PLACING:
               this.waitTicks = 0;
               this.state = AutoExtinguish.State.WAITING;
               break;
            case WAITING:
               if (this.findWaterBucket() == -1) {
                  this.resetState();
                  return;
               }

               if (++this.waitTicks >= 25) {
                  this.state = AutoExtinguish.State.PICKING_UP;
               }
               break;
            case PICKING_UP:
               this.state = AutoExtinguish.State.RESTORE;
               break;
            case RESTORE:
               this.restoreSlot();
               this.state = AutoExtinguish.State.IDLE;
         }
      }
   }

   private void placeWater() {
      BlockPos below = mc.player.blockPosition().below();
      Vec3 hitVec = Vec3.atCenterOf(below).add(0.0, 0.5, 0.0);
      mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hitVec, Direction.UP, below, false));
   }

   private void pickUpWater() {
      BlockPos pos = mc.player.blockPosition();
      Vec3 hitVec = Vec3.atCenterOf(pos).add(0.0, 0.5, 0.0);
      mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hitVec, Direction.UP, pos, false));
   }

   private int findWaterBucket() {
      if (mc.player == null) {
         return -1;
      }

      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getItem(i).is(Items.WATER_BUCKET)) {
            return i;
         }
      }

      return -1;
   }

   private void restoreSlot() {
      if (this.savedSlot != -1 && mc.player != null) {
         mc.player.getInventory().setSelectedSlot(this.savedSlot);
         mc.player.connection.send(new ServerboundSetCarriedItemPacket(this.savedSlot));
         this.savedSlot = -1;
      }
   }

   private void resetState() {
      this.restoreSlot();
      this.state = AutoExtinguish.State.IDLE;
      this.waitTicks = 0;
      this.waterSlot = -1;
      this.savedLevel = null;
   }

   private enum State {
      IDLE,
      PLACING,
      WAITING,
      PICKING_UP,
      RESTORE;
   }
}
