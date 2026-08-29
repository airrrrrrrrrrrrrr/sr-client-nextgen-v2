package me.alpha432.oyvey.mixin.entity;

import me.alpha432.oyvey.features.modules.render.AnimationsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
   @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
   private void oyvey$modifySwingDuration(CallbackInfoReturnable<Integer> cir) {
      if ((LivingEntity)(Object)this == Minecraft.getInstance().player) {
         AnimationsModule anim = AnimationsModule.INSTANCE;
         if (anim != null && anim.isEnabled()) {
            cir.setReturnValue(anim.swingDuration.getValue());
         }
      }
   }
}
