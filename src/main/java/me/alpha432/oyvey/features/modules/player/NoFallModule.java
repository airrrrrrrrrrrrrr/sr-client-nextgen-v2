package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.mixin.network.accessor.MovePlayerPacketAccessor;
import me.alpha432.oyvey.util.player.FallSegmentCounter;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;

public class NoFallModule extends Module {
   private final Setting<NoFallModule.Mode> mode = this.register(new Setting<>("Mode", NoFallModule.Mode.PACKET));
   private final FallSegmentCounter cubeCraftFall = new FallSegmentCounter();
   private Object trackedLevel;
   private Mode previousMode;

   public NoFallModule() {
      super("NoFall", "Removes fall damage", Module.Category.PLAYER);
   }

   @Override
   public void onEnable() {
      this.cubeCraftFall.reset();
      this.trackedLevel = null;
      this.previousMode = null;
   }

   @Override
   public void onDisable() {
      this.cubeCraftFall.reset();
   }

   @Subscribe
   public void onWalking(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == Stage.MOTION_PRE && this.isCubeCraftFalling()) {
         // Reset before movement instead of waiting until after landing physics.
         mc.player.fallDistance = 0;
      }
   }

   @Subscribe
   public void onPacketSend(PacketEvent.Send event) {
      if (this.isCubeCraftFalling() && event.getPacket() instanceof ServerboundMovePlayerPacket movement) {
         // Preserve the original packet's position, rotations, and collision bit.
         // A separate status packet can arrive after a damaging movement packet.
         ((MovePlayerPacketAccessor)movement).oyvey$setOnGround(true);
      }
   }

   private boolean isCubeCraftFalling() {
      return !nullCheck() && this.mode.getValue() == Mode.CUBECRAFT
         && !mc.player.onGround() && (mc.player.getDeltaMovement().y < 0 || mc.player.fallDistance > 0)
         && !mc.player.isInWater() && !mc.player.isInLava() && !mc.player.onClimbable()
         && !mc.player.isFallFlying() && !mc.player.getAbilities().flying && !mc.player.isPassenger();
   }

   @Override
   public void onTick() {
      if (nullCheck()) {
         this.cubeCraftFall.reset();
         this.trackedLevel = null;
         return;
      }
      if (this.trackedLevel != mc.level || this.previousMode != this.mode.getValue()) {
         this.cubeCraftFall.reset();
         this.trackedLevel = mc.level;
         this.previousMode = this.mode.getValue();
      }
      if (!nullCheck()) {
         switch ((NoFallModule.Mode)this.mode.getValue()) {
            case CUBECRAFT:
               boolean falling = this.isCubeCraftFalling();
               if (falling) {
                  mc.player.fallDistance = 0;
               }
               if (this.cubeCraftFall.advance(mc.player.getY(), falling)) {
                  var velocity = mc.player.getDeltaMovement();
                  mc.player.setDeltaMovement(velocity.x, 0, velocity.z);
                  mc.player.fallDistance = 0;
                  mc.player.connection.send(new StatusOnly(true, mc.player.horizontalCollision));
               }
               break;
            case PACKET:
               if (!mc.player.onGround() && mc.player.fallDistance > 2.5) {
                  mc.player
                     .connection
                     .send(
                        new PosRot(
                           mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), true, mc.player.horizontalCollision
                        )
                     );
                  mc.player.fallDistance = 0.0;
               }
               break;
            case CANCEL:
               mc.player.fallDistance = 0.0;
               break;
            case VULCAN:
               double motY = mc.player.getDeltaMovement().y;
               if (!mc.player.onGround()) {
                  mc.player.fallDistance = 0.0;
                  if (motY < -0.05 && motY > -0.19) {
                     mc.player
                        .connection
                        .send(
                           new PosRot(
                              mc.player.getX(),
                              mc.player.getY(),
                              mc.player.getZ(),
                              mc.player.getYRot(),
                              mc.player.getXRot(),
                              true,
                              mc.player.horizontalCollision
                           )
                        );
                  }
               }
         }
      }
   }

   @Override
   public String getDisplayInfo() {
      return this.mode.getValue().name();
   }

   public enum Mode {
      PACKET,
      CANCEL,
      CUBECRAFT,
      VULCAN;
   }
}
