package me.alpha432.oyvey.mixin.entity;

import me.alpha432.oyvey.features.modules.render.AnimationsModule;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.movement.Speed;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
   @Inject(method = "handleRelativeFrictionAndCalculateMovement", at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
      shift = At.Shift.AFTER
   ))
   private void oyvey$vulcan2BeforeMovement(Vec3 input, float friction, CallbackInfoReturnable<Vec3> cir) {
      if ((LivingEntity)(Object)this == Minecraft.getInstance().player && OyVey.moduleManager != null) {
         Speed speed = OyVey.moduleManager.getModuleByClass(Speed.class);
         if (speed != null) {
            speed.applyVulcan2Motion();
         }
      }
   }

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
