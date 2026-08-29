package me.alpha432.oyvey.mixin.world;

import me.alpha432.oyvey.features.modules.render.Ambience;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class MixinLevel {
   @Inject(method = "getOverworldClockTime()J", at = @At("HEAD"), cancellable = true)
   private void oyvey$overrideTime(CallbackInfoReturnable<Long> cir) {
      Ambience a = Ambience.INSTANCE;
      if (a != null && a.isEnabled() && a.customTime.getValue()) {
         cir.setReturnValue((long)a.time.getValue().intValue());
      }
   }

   @Inject(method = "getRainLevel(F)F", at = @At("HEAD"), cancellable = true)
   private void oyvey$overrideRain(float partial, CallbackInfoReturnable<Float> cir) {
      Ambience a = Ambience.INSTANCE;
      if (a != null && a.isEnabled() && a.currentRain >= 0.0F) {
         cir.setReturnValue(a.currentRain);
      }
   }

   @Inject(method = "getThunderLevel(F)F", at = @At("HEAD"), cancellable = true)
   private void oyvey$overrideThunder(float partial, CallbackInfoReturnable<Float> cir) {
      Ambience a = Ambience.INSTANCE;
      if (a != null && a.isEnabled() && a.currentThunder >= 0.0F) {
         cir.setReturnValue(a.currentThunder);
      }
   }
}
