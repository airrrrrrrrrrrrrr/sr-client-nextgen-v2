package me.alpha432.oyvey.mixin.entity;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.movement.NoWebModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MixinNoWeb {
   @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
   private void oyvey$noWeb(BlockState state, Vec3 motionMultiplier, CallbackInfo ci) {
      if ((Object)this instanceof LocalPlayer) {
         NoWebModule module = OyVey.moduleManager.getModuleByClass(NoWebModule.class);
         if (module != null && module.isEnabled()) {
            ci.cancel();
         }
      }
   }
}
