package me.alpha432.oyvey.mixin.entity;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.player.FastUse;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public class MixinFastUse {
   @Redirect(method = "updateUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isClientSide()Z"))
   private boolean fastUseClientSide(Level level) {
      FastUse fu = OyVey.moduleManager.getModuleByClass(FastUse.class);
      return fu != null && fu.isEnabled() ? false : level.isClientSide();
   }
}
