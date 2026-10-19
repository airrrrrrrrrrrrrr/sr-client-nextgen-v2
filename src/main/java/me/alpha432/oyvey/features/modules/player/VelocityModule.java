package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.movement.Scaffold;
import me.alpha432.oyvey.features.modules.movement.Speed;
import me.alpha432.oyvey.util.JumpResetInput;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Input;
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
   private final JumpResetInput jumpReset = new JumpResetInput();
   private LocalPlayer jumpOwner;
   private ClientLevel jumpLevel;

   public VelocityModule() {
      super("Velocity", "Reduces knockback.", Module.Category.PLAYER);
      this.horizontal.setVisibility(v -> this.mode.getValue() == VelocityModule.VeloMode.CANCEL);
      this.vertical.setVisibility(v -> this.mode.getValue() == VelocityModule.VeloMode.CANCEL);
      this.explosions.setVisibility(v -> this.mode.getValue() == VelocityModule.VeloMode.CANCEL);
   }

   @Override
   public void onDisable() {
      this.rawKnockback = null;
      this.jumpReset.reset(); this.jumpOwner = null; this.jumpLevel = null;
   }

   @Subscribe
   private void onPacketReceive(PacketEvent.Receive event) {
      if (mc.player != null && this.isEnabled()) {
         if (event.getPacket() instanceof ClientboundSetEntityMotionPacket pkt && pkt.id() == mc.player.getId()) {
            if (this.mode.getValue() == VelocityModule.VeloMode.JUMPRESET) {
               this.rawKnockback = null;
               // Observe only after vanilla applies this packet on the client
               // thread. Never replace or cancel knockback in JumpReset mode.
            } else {
               event.cancel();
               this.rawKnockback = pkt.movement();
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
            return;
         }

         this.jumpReset.reset();
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

   /** Called at the native motion-packet handler's RETURN on the client thread. */
   public void onMotionApplied(ClientboundSetEntityMotionPacket packet) {
      if (!this.isEnabled() || this.mode.getValue() != VeloMode.JUMPRESET || nullCheck() || packet.id() != mc.player.getId()) return;
      this.syncJumpSession();
      this.jumpReset.arm(mc.player.tickCount, mc.player.onGround(), packet.movement().horizontalDistanceSqr() > 1.0E-6);
   }

   private void syncJumpSession() {
      if (this.jumpOwner != mc.player || this.jumpLevel != mc.level) {
         this.jumpReset.reset(); this.jumpOwner = mc.player; this.jumpLevel = mc.level;
      }
   }

   public void onServerCorrection() {
      if (mc.player != null) { this.syncJumpSession(); this.jumpReset.correction(mc.player.tickCount); }
   }

   public void applyJumpResetInput(ClientInput input) {
      if (!this.isEnabled() || this.mode.getValue() != VeloMode.JUMPRESET || nullCheck()) { this.jumpReset.reset(); return; }
      this.syncJumpSession();
      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      Speed speed = OyVey.moduleManager.getModuleByClass(Speed.class);
      Input keys = input.keyPresses;
      boolean eligible = mc.screen == null && mc.player.isAlive() && !mc.player.isSpectator() && mc.player.onGround()
         && !keys.shift() && !mc.player.isPassenger() && !mc.player.isFallFlying() && !mc.player.getAbilities().flying
         && !mc.player.onClimbable() && !mc.player.isInWater() && !mc.player.isInLava() && !mc.player.isMobilityRestricted()
         && (scaffold == null || !scaffold.ownsRotation()) && (speed == null || !speed.isEnabled());
      if (this.jumpReset.consume(mc.player.tickCount, eligible, mc.player.hurtTime > 0, keys.jump()))
         input.keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(), true, keys.shift(), keys.sprint());
   }

   public enum VeloMode {
      CANCEL,
      REVERSE,
      JUMPRESET;
   }
}
