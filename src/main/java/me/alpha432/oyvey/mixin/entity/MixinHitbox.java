package me.alpha432.oyvey.mixin.entity;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.combat.HitboxModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinHitbox {
   @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
   private void oyvey$expandHitbox(CallbackInfoReturnable<AABB> cir) {
      if (!((Object)this instanceof LocalPlayer)) {
         HitboxModule module = OyVey.moduleManager.getModuleByClass(HitboxModule.class);
         if (module != null && module.isEnabled()) {
            double h = module.horizontal.getValue();
            double v = module.vertical.getValue();
            if (h != 0.0 || v != 0.0) {
               cir.setReturnValue(((AABB)cir.getReturnValue()).inflate(h, v, h));
            }
         }
      }
   }
}
