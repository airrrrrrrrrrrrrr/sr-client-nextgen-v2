package me.alpha432.oyvey.mixin.entity;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.movement.SafeWalk;
import me.alpha432.oyvey.features.modules.render.FreeLookModule;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {
   @Redirect(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isSteppingCarefully()Z"), require = 0)
   private boolean oyvey$safeWalkEdgeClip(Entity instance) {
      if (instance instanceof LocalPlayer && instance == Util.mc.player) {
         SafeWalk sw = OyVey.moduleManager.getModuleByClass(SafeWalk.class);
         if (sw != null && sw.isEnabled()) {
            return true;
         }
      }

      return instance.isSteppingCarefully();
   }

   @Inject(method = "getViewYRot", at = @At("RETURN"), cancellable = true)
   private void oyvey$freelookYRot(float partial, CallbackInfoReturnable<Float> cir) {
      if ((Object)this == Util.mc.player) {
         FreeLookModule fl = FreeLookModule.INSTANCE;
         if (fl != null && fl.isFreelooking()) {
            cir.setReturnValue((Float)cir.getReturnValue() + fl.getFreeYaw());
         }
      }
   }

   @Inject(method = "getViewXRot", at = @At("RETURN"), cancellable = true)
   private void oyvey$freelookXRot(float partial, CallbackInfoReturnable<Float> cir) {
      if ((Object)this == Util.mc.player) {
         FreeLookModule fl = FreeLookModule.INSTANCE;
         if (fl != null && fl.isFreelooking()) {
            cir.setReturnValue(Math.max(-90.0F, Math.min(90.0F, (Float)cir.getReturnValue() + fl.getFreePitch())));
         }
      }
   }
}
