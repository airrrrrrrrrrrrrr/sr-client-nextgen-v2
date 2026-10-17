package me.alpha432.oyvey.mixin.input;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.features.modules.combat.WTap;
import me.alpha432.oyvey.features.modules.movement.Scaffold;
import me.alpha432.oyvey.features.modules.movement.SprintModule;
import me.alpha432.oyvey.features.modules.movement.Speed;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {
   @Inject(method = "tick", at = @At("RETURN"))
   private void oyvey$correctSilentRotationMovement(CallbackInfo ci) {
      if (OyVey.moduleManager == null) {
         return;
      }

      KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      if (aura != null) {
         aura.applyGrimMovementCorrection((ClientInput)(Object)this);
      }

      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      if (scaffold != null) {
         scaffold.applyInput((ClientInput)(Object)this);
      }

      WTap wTap = OyVey.moduleManager.getModuleByClass(WTap.class);
      if (wTap != null) {
         wTap.applyInputReset((ClientInput)(Object)this);
      }

      SprintModule sprint = OyVey.moduleManager.getModuleByClass(SprintModule.class);
      if (sprint != null) {
         sprint.applySprintInput((ClientInput)(Object)this);
      }

      Speed speed = OyVey.moduleManager.getModuleByClass(Speed.class);
      if (speed != null) {
         speed.applyVulcan2Input((ClientInput)(Object)this);
      }
   }
}
