package me.alpha432.oyvey.mixin.network;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.movement.Scaffold;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.event.impl.network.ChatEvent;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
   @Inject(method = "handleMovePlayer", at = @At("RETURN"))
   private void oyvey$resetCorrectedCombat(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
      if (OyVey.moduleManager == null) return;
      KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      if (aura != null) aura.onServerCorrection();
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
