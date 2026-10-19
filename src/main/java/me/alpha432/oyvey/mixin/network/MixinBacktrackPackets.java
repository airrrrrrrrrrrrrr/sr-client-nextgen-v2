package me.alpha432.oyvey.mixin.network;

import me.alpha432.oyvey.features.modules.combat.BacktrackModule;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Intercept after PacketUtils hands processing to the client thread. */
@Mixin(ClientPacketListener.class)
public class MixinBacktrackPackets {
   @Unique private static final String SR_THREAD_HANDOFF = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V";

   @Inject(method = "handleMoveEntity", at = @At(value = "INVOKE", target = SR_THREAD_HANDOFF, shift = At.Shift.AFTER), cancellable = true)
   private void sr$history(ClientboundMoveEntityPacket packet, CallbackInfo ci) {
      BacktrackModule module = BacktrackModule.INSTANCE;
      if (module != null && module.interceptMovement(packet)) ci.cancel();
   }

   @Unique private static void sr$immediate(Packet<?> packet) {
      BacktrackModule module = BacktrackModule.INSTANCE;
      if (module != null) module.beforeImmediate(packet);
   }

   @Inject(method = "handleTeleportEntity", at = @At(value = "INVOKE", target = SR_THREAD_HANDOFF, shift = At.Shift.AFTER))
   private void sr$teleport(ClientboundTeleportEntityPacket packet, CallbackInfo ci) { sr$immediate(packet); }
   @Inject(method = "handleEntityPositionSync", at = @At(value = "INVOKE", target = SR_THREAD_HANDOFF, shift = At.Shift.AFTER))
   private void sr$sync(ClientboundEntityPositionSyncPacket packet, CallbackInfo ci) { sr$immediate(packet); }
   @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = SR_THREAD_HANDOFF, shift = At.Shift.AFTER))
   private void sr$correction(ClientboundPlayerPositionPacket packet, CallbackInfo ci) { sr$immediate(packet); }
   @Inject(method = "handleRespawn", at = @At(value = "INVOKE", target = SR_THREAD_HANDOFF, shift = At.Shift.AFTER))
   private void sr$respawn(ClientboundRespawnPacket packet, CallbackInfo ci) { sr$immediate(packet); }
   @Inject(method = "handleExplosion", at = @At(value = "INVOKE", target = SR_THREAD_HANDOFF, shift = At.Shift.AFTER))
   private void sr$explosion(ClientboundExplodePacket packet, CallbackInfo ci) { sr$immediate(packet); }
   @Inject(method = "handleSetEntityMotion", at = @At(value = "INVOKE", target = SR_THREAD_HANDOFF, shift = At.Shift.AFTER))
   private void sr$velocity(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) { sr$immediate(packet); }
   @Inject(method = "handleRemoveEntities", at = @At(value = "INVOKE", target = SR_THREAD_HANDOFF, shift = At.Shift.AFTER))
   private void sr$remove(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) { sr$immediate(packet); }
}
