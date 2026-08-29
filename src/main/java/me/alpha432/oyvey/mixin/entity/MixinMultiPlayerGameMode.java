package me.alpha432.oyvey.mixin.entity;

import me.alpha432.oyvey.OyVey;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
   @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
   private void oyvey$clientOnlyRecorderAttack(Player player, Entity target, CallbackInfo ci) {
      if (OyVey.recordedRotationManager != null && OyVey.recordedRotationManager.isRecordingTarget(target)) {
         target.animateHurt(0.0F);
         ci.cancel();
      }
   }
}
