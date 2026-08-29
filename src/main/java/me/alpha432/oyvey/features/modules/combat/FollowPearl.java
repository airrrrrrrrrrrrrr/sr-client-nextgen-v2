package me.alpha432.oyvey.features.modules.combat;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.RotationUtils;
import me.alpha432.oyvey.util.inventory.InventoryUtil;
import me.alpha432.oyvey.util.inventory.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class FollowPearl extends Module {
   public final Setting<Boolean> inventory = this.register(new Setting<>("Inventory", false));
   public final Setting<Float> maxRange = this.register(new Setting<>("MaxRange", 64.0F, 10.0F, 256.0F));
   private Vec3 pendingTarget = null;
   private boolean throwThisTick = false;

   public FollowPearl() {
      super("FollowPearl", "Throws a pearl to where an enemy's pearl lands.", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.pendingTarget = null;
      this.throwThisTick = false;
   }

   @Subscribe
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!nullCheck()) {
         if (event.getPacket() instanceof ClientboundAddEntityPacket packet) {
            if (packet.getType() == EntityType.ENDER_PEARL) {
               int ownerId = packet.getData();
               if (ownerId > 0) {
                  boolean isEnemy = false;

                  for (Player p : mc.level.players()) {
                     if (p.getId() == ownerId && p != mc.player) {
                        isEnemy = true;
                        break;
                     }
                  }

                  if (isEnemy) {
                     Vec3 pos = new Vec3(packet.getX(), packet.getY(), packet.getZ());
                     if (!(pos.distanceTo(mc.player.position()) > this.maxRange.getValue().floatValue())) {
                        Vec3 vel = packet.getMovement();
                        this.pendingTarget = this.simulateLanding(pos, vel);
                        this.throwThisTick = true;
                     }
                  }
               }
            }
         }
      }
   }

   @Subscribe
   public void onUpdateWalking(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == Stage.PRE) {
         if (this.throwThisTick && this.pendingTarget != null) {
            this.throwThisTick = false;
            Result result = InventoryUtil.find(Items.ENDER_PEARL, this.inventory.getValue() ? InventoryUtil.FULL_SCOPE : InventoryUtil.HOTBAR_SCOPE);
            if (!result.found()) {
               this.pendingTarget = null;
            } else {
               Vec3 eyes = mc.player.getEyePosition();
               float[] rot = RotationUtils.lookingAt(this.pendingTarget, eyes);
               float savedYaw = mc.player.getYRot();
               float savedPitch = mc.player.getXRot();
               OyVey.rotationManager.setPlayerRotations(rot[0], rot[1]);
               InventoryUtil.withSwap(result, () -> mc.gameMode.useItem(mc.player, result.hand()));
               OyVey.rotationManager.setPlayerRotations(savedYaw, savedPitch);
               this.pendingTarget = null;
            }
         }
      }
   }

   private Vec3 simulateLanding(Vec3 pos, Vec3 vel) {
      double x = pos.x;
      double y = pos.y;
      double z = pos.z;
      double vx = vel.x;
      double vy = vel.y;
      double vz = vel.z;

      for (int i = 0; i < 250; i++) {
         x += vx;
         y += vy;
         z += vz;
         vx *= 0.99;
         vy = vy * 0.99 - 0.03;
         vz *= 0.99;
         if (!mc.level.getBlockState(BlockPos.containing(x, y - 0.1, z)).isAir()) {
            return new Vec3(x, y, z);
         }
      }

      return new Vec3(x, y, z);
   }
}
