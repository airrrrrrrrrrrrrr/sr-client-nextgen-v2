package me.alpha432.oyvey.mixin.network;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.movement.Scaffold;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.features.modules.combat.PvpBot;
import me.alpha432.oyvey.features.modules.player.VelocityModule;
import me.alpha432.oyvey.features.modules.misc.KillInsults;
import me.alpha432.oyvey.event.impl.network.ChatEvent;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
   @Inject(method = "handleSetEntityMotion", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
      shift = At.Shift.AFTER), cancellable = true)
   private void oyvey$delayKnockback(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
      if (OyVey.moduleManager==null) return;
      VelocityModule velocity=OyVey.moduleManager.getModuleByClass(VelocityModule.class);
      if (velocity!=null && velocity.delayMotion(packet,(ClientPacketListener)(Object)this)) ci.cancel();
   }

   @Inject(method = "handleEntityEvent", at = @At("RETURN"))
   private void oyvey$killInsultsDeath(ClientboundEntityEventPacket packet, CallbackInfo ci) {
      if (packet.getEventId() != 3 || OyVey.moduleManager == null || Util.mc.level == null) return;
      KillInsults insults = OyVey.moduleManager.getModuleByClass(KillInsults.class);
      if (insults != null) insults.onConfirmedDeath(packet.getEntity(Util.mc.level));
   }

   @Inject(method = "handleMovePlayer", at = @At("RETURN"))
   private void oyvey$resetCorrectedCombat(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
      if (OyVey.moduleManager == null) return;
      KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      if (aura != null) aura.onServerCorrection();
      PvpBot pvpBot = OyVey.moduleManager.getModuleByClass(PvpBot.class);
      if (pvpBot != null) pvpBot.onServerCorrection();
      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      if (scaffold != null) scaffold.onServerCorrection();
      VelocityModule velocity = OyVey.moduleManager.getModuleByClass(VelocityModule.class);
      if (velocity != null) velocity.onServerCorrection();
   }

   @Inject(method = "handleSetEntityMotion", at = @At("RETURN"))
   private void oyvey$jumpResetAfterMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
      if (OyVey.moduleManager == null) return;
      VelocityModule velocity = OyVey.moduleManager.getModuleByClass(VelocityModule.class);
      if (velocity != null) velocity.onMotionApplied(packet);
   }

   @Inject(method = "handleBlockUpdate", at = @At("RETURN"))
   private void oyvey$scaffoldBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
      if (OyVey.moduleManager == null) return;
      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      if (scaffold != null) scaffold.onServerBlockUpdate(packet.getPos(), packet.getBlockState());
   }

   @Inject(method = "handleChunkBlocksUpdate", at = @At("RETURN"))
   private void oyvey$scaffoldSectionUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
      if (OyVey.moduleManager == null) return;
      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      if (scaffold != null) packet.runUpdates(scaffold::onServerBlockUpdate);
   }

   @Inject(method = "handleBlockChangedAck", at = @At("RETURN"))
   private void oyvey$confirmScaffoldPrediction(ClientboundBlockChangedAckPacket packet, CallbackInfo ci) {
      // RETURN runs on the client thread after vanilla has applied server state
      // and rolled back any denied placement. PacketEvent.Receive is too early.
      if (OyVey.moduleManager != null) {
         Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
         if (scaffold != null) scaffold.onPredictionAck(packet.sequence());
      }
   }

   @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
   private void sendChat(String content, CallbackInfo ci) {
      if (Util.EVENT_BUS.post(new ChatEvent(content))) {
         ci.cancel();
      }
   }
}
