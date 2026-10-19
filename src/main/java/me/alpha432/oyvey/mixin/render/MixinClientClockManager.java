package me.alpha432.oyvey.mixin.render;

import me.alpha432.oyvey.features.modules.render.Ambience;
import me.alpha432.oyvey.util.render.AmbienceClock;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.class)
public class MixinClientClockManager {
   @Inject(method = "getTotalTicks", at = @At("RETURN"), cancellable = true)
   private void oyvey$visualTime(Holder<WorldClock> clock, CallbackInfoReturnable<Long> cir) {
      Ambience ambience = Ambience.INSTANCE;
      if (ambience != null && ambience.isEnabled() && ambience.customTime.getValue() && clock.is(WorldClocks.OVERWORLD))
         cir.setReturnValue(AmbienceClock.withTime(cir.getReturnValue(), ambience.time.getValue()));
   }
}
