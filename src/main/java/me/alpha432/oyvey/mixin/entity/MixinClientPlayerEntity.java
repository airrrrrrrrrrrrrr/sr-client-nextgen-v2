package me.alpha432.oyvey.mixin.entity;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.TickEvent;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.manager.RotationManager;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class MixinClientPlayerEntity {
   @Inject(method = "tick", at = @At("TAIL"))
   private void tickHook(CallbackInfo ci) {
      Util.EVENT_BUS.post(new TickEvent());
      RotationManager rm = OyVey.rotationManager;
      if (rm != null) {
         rm.tickRenderRotation();
      }
   }

   @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V", shift = Shift.BEFORE))
   private void preMotionHook(CallbackInfo ci) {
      Util.EVENT_BUS.post(new UpdateWalkingPlayerEvent(Stage.MOTION_PRE));
   }

   @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V", shift = Shift.AFTER))
   private void tickHook2(CallbackInfo ci) {
      Util.EVENT_BUS.post(new UpdateWalkingPlayerEvent(Stage.PRE));
   }

   @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;sendPosition()V", shift = Shift.AFTER))
   private void tickHook3(CallbackInfo ci) {
      Util.EVENT_BUS.post(new UpdateWalkingPlayerEvent(Stage.POST));
   }
}
