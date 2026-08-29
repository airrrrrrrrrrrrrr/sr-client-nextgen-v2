package me.alpha432.oyvey.mixin.entity;

import me.alpha432.oyvey.features.modules.combat.ReachModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class MixinPlayer {
   @Inject(method = "entityInteractionRange", at = @At("RETURN"), cancellable = true)
   private void oyvey$extendReach(CallbackInfoReturnable<Double> cir) {
      ReachModule module = ReachModule.INSTANCE;
      if (module != null && module.shouldApply() && (Object)this instanceof LocalPlayer) {
         cir.setReturnValue(module.getCurrentReach());
      }
   }
}
