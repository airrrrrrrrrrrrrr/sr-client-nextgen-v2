package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.movement.Scaffold;
import me.alpha432.oyvey.features.modules.movement.Speed;
import me.alpha432.oyvey.util.JumpResetInput;
import me.alpha432.oyvey.util.KnockbackDelay;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.multiplayer.ClientPacketListener;
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
   public final Setting<Integer> delayMs = this.register(new Setting<>("DelayMS",100,0,1000,
      v -> this.mode.getValue()==VeloMode.DELAY));
   private volatile Vec3 rawKnockback = null;
   private final JumpResetInput jumpReset = new JumpResetInput();
   private LocalPlayer jumpOwner;
   private ClientLevel jumpLevel;
   private final KnockbackDelay<ClientboundSetEntityMotionPacket> delayed = new KnockbackDelay<>();
   private LocalPlayer delayOwner;
   private ClientLevel delayLevel;
   private ClientPacketListener delayConnection;
   private boolean replaying;
   private long delayGeneration;
   private int delayedDamageUntil = -1;

   public VelocityModule() {
      super("Velocity", "Reduces knockback.", Module.Category.PLAYER);
      this.horizontal.setVisibility(v -> this.mode.getValue() == VelocityModule.VeloMode.CANCEL);
      this.vertical.setVisibility(v -> this.mode.getValue() == VelocityModule.VeloMode.CANCEL);
      this.explosions.setVisibility(v -> this.mode.getValue() == VelocityModule.VeloMode.CANCEL);
   }

   @Override
   public void onEnable() {
      this.clearDelayed(); this.rawKnockback=null;
      this.jumpReset.reset(); this.jumpOwner=null; this.jumpLevel=null;
   }

   @Override
   public void onDisable() {
      this.releaseDelayed(true); this.clearDelayed();
      this.rawKnockback = null;
      this.jumpReset.reset(); this.jumpOwner = null; this.jumpLevel = null;
   }

   @Subscribe
   private void onPacketReceive(PacketEvent.Receive event) {
      if (mc.player != null && this.isEnabled()) {
         if (event.getPacket() instanceof ClientboundSetEntityMotionPacket pkt && pkt.id() == mc.player.getId()) {
            if (this.usesJumpReset()) {
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
         this.releaseDelayed(this.mode.getValue()!=VeloMode.DELAY);
         if (this.usesJumpReset()) {
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
      if (!this.isEnabled() || !this.usesJumpReset() || nullCheck() || packet.id() != mc.player.getId()
         || !this.replaying && this.delayed.contains(packet)) return;
      this.syncJumpSession();
      this.jumpReset.arm(mc.player.tickCount, mc.player.onGround(), packet.movement().horizontalDistanceSqr() > 1.0E-6);
   }

   private void syncJumpSession() {
      if (this.jumpOwner != mc.player || this.jumpLevel != mc.level) {
         this.jumpReset.reset(); this.jumpOwner = mc.player; this.jumpLevel = mc.level;
      }
   }

   public void onServerCorrection() {
      this.clearDelayed();
      if (mc.player != null) { this.syncJumpSession(); this.jumpReset.correction(mc.player.tickCount); }
   }

   public void applyJumpResetInput(ClientInput input) {
      if (!this.isEnabled() || !this.usesJumpReset() || nullCheck()) { this.jumpReset.reset(); return; }
      this.syncJumpSession();
      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      Speed speed = OyVey.moduleManager.getModuleByClass(Speed.class);
      Input keys = input.keyPresses;
      boolean eligible = mc.screen == null && mc.player.isAlive() && !mc.player.isSpectator() && mc.player.onGround()
         && !keys.shift() && !mc.player.isPassenger() && !mc.player.isFallFlying() && !mc.player.getAbilities().flying
         && !mc.player.onClimbable() && !mc.player.isInWater() && !mc.player.isInLava() && !mc.player.isMobilityRestricted()
         && (scaffold == null || !scaffold.ownsRotation()) && (speed == null || !speed.isEnabled());
      if (this.jumpReset.consume(mc.player.tickCount, eligible,
         mc.player.hurtTime > 0 || this.mode.getValue()==VeloMode.DELAY && mc.player.tickCount<=this.delayedDamageUntil, keys.jump()))
         input.keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(), true, keys.shift(), keys.sprint());
   }

   private boolean usesJumpReset() { return this.mode.getValue()==VeloMode.JUMPRESET || this.mode.getValue()==VeloMode.DELAY; }

   @Override public void onTick() { this.releaseDelayed(this.mode.getValue()!=VeloMode.DELAY); }

   /** Invoked after vanilla's network-to-client-thread handoff, before applying motion. */
   public boolean delayMotion(ClientboundSetEntityMotionPacket packet, ClientPacketListener source) {
      if (!this.isEnabled() || this.mode.getValue()!=VeloMode.DELAY || this.replaying || nullCheck()
         || source!=mc.getConnection() || packet.id()!=mc.player.getId() || !mc.player.isAlive() || mc.player.isSpectator()) return false;
      this.syncDelaySession();
      var entry=this.delayed.offer(packet,System.nanoTime(),mc.player.tickCount,this.delayMs.getValue(),mc.player.hurtTime>0);
      if (entry==null) { this.releaseDelayed(true); return false; }
      long generation=this.delayGeneration;
      long wait=Math.max(0L,entry.deadline-System.nanoTime());
      CompletableFuture.delayedExecutor(wait,TimeUnit.NANOSECONDS).execute(() -> mc.execute(() -> {
         if (generation==this.delayGeneration) this.releaseDelayed(this.mode.getValue()!=VeloMode.DELAY);
      }));
      return true;
   }

   private void syncDelaySession() {
      if (this.delayOwner!=mc.player || this.delayLevel!=mc.level || this.delayConnection!=mc.getConnection()) {
         this.clearDelayed();
         this.delayOwner=mc.player; this.delayLevel=mc.level; this.delayConnection=mc.getConnection();
      }
   }

   private void clearDelayed() {
      this.delayed.clear(); this.delayGeneration++; this.delayedDamageUntil=-1;
      this.delayOwner=null; this.delayLevel=null; this.delayConnection=null;
   }

   private void releaseDelayed(boolean flush) {
      this.syncDelaySession();
      if (nullCheck() || this.delayConnection==null || !mc.player.isAlive()) { this.clearDelayed(); return; }
      if (mc.player.hurtTime>0) this.delayed.observeDamage(mc.player.tickCount);
      for (var entry:this.delayed.drain(System.nanoTime(),flush)) {
         if (this.delayOwner!=mc.player || this.delayLevel!=mc.level || this.delayConnection!=mc.getConnection()) {
            this.clearDelayed(); break;
         }
         if (entry.damaged()) this.delayedDamageUntil=mc.player.tickCount+2;
         this.replaying=true;
         try { this.delayConnection.handleSetEntityMotion(entry.packet); }
         finally { this.replaying=false; }
      }
   }

   public enum VeloMode {
      CANCEL,
      REVERSE,
      JUMPRESET,
      DELAY;
   }
}
