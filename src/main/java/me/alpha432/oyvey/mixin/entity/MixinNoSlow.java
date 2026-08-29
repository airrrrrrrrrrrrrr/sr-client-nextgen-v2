package me.alpha432.oyvey.mixin.entity;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.movement.NoSlow;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class MixinNoSlow {
   @Redirect(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
   private boolean noSlowModifyInput(LocalPlayer instance) {
      NoSlow ns = OyVey.moduleManager.getModuleByClass(NoSlow.class);
      return ns != null && ns.isEnabled() ? false : instance.isUsingItem();
   }

   @Inject(method = "isSlowDueToUsingItem", at = @At("HEAD"), cancellable = true)
   private void noSlowSprint(CallbackInfoReturnable<Boolean> cir) {
      NoSlow ns = OyVey.moduleManager.getModuleByClass(NoSlow.class);
      if (ns != null && ns.isEnabled()) {
         cir.setReturnValue(false);
      }
   }
}
