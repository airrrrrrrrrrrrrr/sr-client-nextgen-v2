package me.alpha432.oyvey.mixin.input;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.features.modules.movement.Scaffold;
import me.alpha432.oyvey.features.modules.movement.scaffold.ScaffoldUseInput;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinScaffoldUseInput {
   @Inject(method = "handleKeybinds", at = @At("HEAD"))
   private void oyvey$legitBlockInput(CallbackInfo ci) {
      if (OyVey.moduleManager==null) return;
      KillauraRecode aura=OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      if (aura!=null) aura.beginLegitBlockInput();
   }

   @Unique
   private boolean oyvey$blockManualUse() {
      if (OyVey.moduleManager == null || Util.mc.player == null || Util.mc.level == null) return false;
      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      return scaffold != null && scaffold.isEnabled();
   }

   @WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z"))
   private boolean oyvey$scaffoldUseClicks(KeyMapping key, Operation<Boolean> original) {
      if (key == Util.mc.options.keyUse) {
         if (this.oyvey$blockManualUse()) return ScaffoldUseInput.consumeClick(true, () -> original.call(key));
         boolean manual=original.call(key);
         KillauraRecode aura=OyVey.moduleManager==null?null:OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
         boolean simulated=aura!=null&&aura.consumeLegitBlockClick();
         return manual||simulated;
      }
      return original.call(key);
   }

   @WrapOperation(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
   private boolean oyvey$scaffoldUseHeld(KeyMapping key, Operation<Boolean> original) {
      if (key == Util.mc.options.keyUse) {
         if (this.oyvey$blockManualUse()) return false;
         KillauraRecode aura = OyVey.moduleManager == null ? null : OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
         // Preserve an owned shield/Pro sword block through vanilla's release
         // check; no held input is forged before an actual use action exists.
         if (aura != null && aura.holdsShieldInput()) return true;
      }
      return original.call(key);
   }

   @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
   private void oyvey$scaffoldManualUse(CallbackInfo ci) {
      if (this.oyvey$blockManualUse()) ci.cancel();
   }

   @Inject(method = "startUseItem", at = @At("RETURN"))
   private void oyvey$legitUseResult(CallbackInfo ci) {
      if (OyVey.moduleManager==null) return;
      KillauraRecode aura=OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      if (aura!=null) aura.afterNativeLegitClick();
   }
}
