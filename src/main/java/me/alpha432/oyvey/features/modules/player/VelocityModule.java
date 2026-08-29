package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

public class VelocityModule extends Module {
   public final Setting<VelocityModule.VeloMode> mode = this.register(new Setting<>("Mode", VelocityModule.VeloMode.CANCEL));
   public final Setting<Integer> horizontal = this.register(new Setting<>("Horizontal", 0, 0, 100));
   public final Setting<Integer> vertical = this.register(new Setting<>("Vertical", 0, 0, 100));
   public final Setting<Boolean> explosions = this.register(new Setting<>("Explosions", true));
   private volatile Vec3 rawKnockback = null;
   private volatile boolean pendingJumpReset = false;

   public VelocityModule() {
      super("Velocity", "Reduces knockback.", Module.Category.PLAYER);
      this.horizontal.setVisibility(v -> this.mode.getValue() == VelocityModule.VeloMode.CANCEL);
      this.vertical.setVisibility(v -> this.mode.getValue() == VelocityModule.VeloMode.CANCEL);
      this.explosions.setVisibility(v -> this.mode.getValue() == VelocityModule.VeloMode.CANCEL);
   }

   @Override
   public void onDisable() {
      this.rawKnockback = null;
      this.pendingJumpReset = false;
   }

   @Subscribe
   private void onPacketReceive(PacketEvent.Receive event) {
      if (mc.player != null && this.isEnabled()) {
         if (event.getPacket() instanceof ClientboundSetEntityMotionPacket pkt && pkt.id() == mc.player.getId()) {
            if (this.mode.getValue() == VelocityModule.VeloMode.JUMPRESET) {
               this.rawKnockback = null;
               Vec3 movement = pkt.movement();
               this.pendingJumpReset = mc.player.onGround()
                  && (movement.horizontalDistanceSqr() > 1.0E-6 || Math.abs(movement.y) > 1.0E-3);
            } else {
               event.cancel();
               this.rawKnockback = pkt.movement();
               this.pendingJumpReset = false;
            }
         }

         if (this.mode.getValue() == VelocityModule.VeloMode.CANCEL && this.explosions.getValue() && event.getPacket() instanceof ClientboundExplodePacket) {
            event.cancel();
         }
      }
   }

   @Subscribe
   private void onMotionPre(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == Stage.MOTION_PRE) {
         if (this.mode.getValue() == VelocityModule.VeloMode.JUMPRESET) {
            this.rawKnockback = null;
            if (this.pendingJumpReset && mc.player != null) {
               this.pendingJumpReset = false;
               if (mc.player.onGround()
                  && !mc.player.isPassenger()
                  && !mc.player.isFallFlying()
                  && !mc.player.isInWater()
                  && !mc.player.isInLava()) {
                  mc.player.jumpFromGround();
               }
            }

            return;
         }

         this.pendingJumpReset = false;
         Vec3 kb = this.rawKnockback;
         if (kb != null && mc.player != null) {
            this.rawKnockback = null;
            if (this.mode.getValue() == VelocityModule.VeloMode.REVERSE) {
               mc.player.setDeltaMovement(-kb.x, Math.abs(kb.y), -kb.z);
            } else if (this.horizontal.getValue() != 0 || this.vertical.getValue() != 0) {
               double hPct = this.horizontal.getValue().intValue() / 100.0;
               double vPct = this.vertical.getValue().intValue() / 100.0;
               double x = mc.player.getDeltaMovement().x;
               double y = mc.player.getDeltaMovement().y;
               double z = mc.player.getDeltaMovement().z;
               mc.player.setDeltaMovement(x + kb.x * hPct, y + kb.y * vPct, z + kb.z * hPct);
            }
         }
      }
   }

   public enum VeloMode {
      CANCEL,
      REVERSE,
      JUMPRESET;
   }
}
