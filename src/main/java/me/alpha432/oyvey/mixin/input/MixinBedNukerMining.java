package me.alpha432.oyvey.mixin.input;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.combat.BedNuker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinBedNukerMining {
   @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
   private void oyvey$continueBedMining(boolean attackPressed, CallbackInfo ci) {
      if (OyVey.moduleManager == null) return;
      BedNuker nuker = OyVey.moduleManager.getModuleByClass(BedNuker.class);
      // Vanilla otherwise cancels the module's damage progress every frame
      // whenever the physical attack button is released.
      if (nuker != null && nuker.isMining()) ci.cancel();
   }
}
