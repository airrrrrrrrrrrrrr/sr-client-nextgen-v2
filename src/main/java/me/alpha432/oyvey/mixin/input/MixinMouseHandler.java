package me.alpha432.oyvey.mixin.input;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.input.MouseInputEvent;
import me.alpha432.oyvey.features.modules.render.FreeLookModule;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {
   @Unique
   private float oyvey$preYaw;
   @Unique
   private float oyvey$prePitch;
   @Unique
   private boolean oyvey$captureTurn;

   @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
   private void onButton(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
      if (input.button() == 0 && action == 1 && OyVey.recordedClickManager != null
         && window == Util.mc.getWindow().handle()) {
         if (Util.mc.player != null && Util.mc.level != null && Util.mc.screen == null
            && Util.mc.player.isAlive() && Util.mc.options.keyShift.isDown()) {
            OyVey.recordedClickManager.click(System.nanoTime());
         } else {
            OyVey.recordedClickManager.pause();
         }
      }
      if (Util.EVENT_BUS.post(new MouseInputEvent(input.button(), action))) {
         ci.cancel();
      }
   }

   @Inject(method = "turnPlayer", at = @At("HEAD"))
   private void oyvey$preTurnPlayer(double sensitivityScale, CallbackInfo ci) {
      FreeLookModule fl = FreeLookModule.INSTANCE;
      boolean recording = OyVey.recordedRotationManager != null && OyVey.recordedRotationManager.isRecording();
      this.oyvey$captureTurn = Util.mc.player != null && Util.mc.screen == null && (recording || fl != null && fl.isFreelooking());
      if (this.oyvey$captureTurn) {
         this.oyvey$preYaw = Util.mc.player.getYRot();
         this.oyvey$prePitch = Util.mc.player.getXRot();
      }
   }

   @Inject(method = "turnPlayer", at = @At("RETURN"))
   private void oyvey$postTurnPlayer(double sensitivityScale, CallbackInfo ci) {
      if (!this.oyvey$captureTurn || Util.mc.player == null) {
         return;
      }

      FreeLookModule fl = FreeLookModule.INSTANCE;
      float dYaw = Util.mc.player.getYRot() - this.oyvey$preYaw;
      float dPitch = Util.mc.player.getXRot() - this.oyvey$prePitch;
      if (OyVey.recordedRotationManager != null && OyVey.recordedRotationManager.isRecording()) {
         OyVey.recordedRotationManager.captureMouseRotation(dYaw, dPitch);
      }

      if (fl != null && fl.isFreelooking()) {
         Util.mc.player.setYRot(this.oyvey$preYaw);
         Util.mc.player.setXRot(this.oyvey$prePitch);
         fl.addDelta(dYaw, dPitch);
      }

      this.oyvey$captureTurn = false;
   }
}
