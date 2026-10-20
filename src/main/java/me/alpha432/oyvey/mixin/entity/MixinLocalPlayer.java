package me.alpha432.oyvey.mixin.entity;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.combat.BedNuker;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.features.modules.combat.PvpBot;
import me.alpha432.oyvey.features.modules.movement.Scaffold;
import me.alpha432.oyvey.features.modules.player.AutoPot;
import me.alpha432.oyvey.manager.RotationManager;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {
   @Inject(method = "applyInput", at = @At("HEAD"))
   private void oyvey$scaffoldRawMovement(CallbackInfo ci) {
      if (OyVey.moduleManager == null) return;
      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      if (scaffold != null) scaffold.beforeNativeInput(((LocalPlayer)(Object)this).input);
   }

   @Inject(method = "canStartSprinting", at = @At("RETURN"), cancellable = true)
   private void oyvey$scaffoldSprintEligibility(CallbackInfoReturnable<Boolean> cir) {
      if (OyVey.moduleManager == null) return;
      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      if (scaffold != null && scaffold.isSprintSuppressed()) cir.setReturnValue(false);
      KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      if (aura != null && aura.isCriticalSprintSuppressed()) cir.setReturnValue(false);
      if (aura != null && aura.isProSprintSuppressed()) cir.setReturnValue(false);
      PvpBot pvpBot = OyVey.moduleManager.getModuleByClass(PvpBot.class);
      if (pvpBot != null && pvpBot.suppressesSprint()) cir.setReturnValue(false);
   }

   @Unique
   private Scaffold oyvey$scaffoldHeadPose() {
      if (!RotationManager.isExtractingLocal || OyVey.moduleManager == null) return null;
      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      return scaffold != null && scaffold.hasSilentHeadPose() ? scaffold : null;
   }

   @Inject(method = "getViewYRot", at = @At("HEAD"), cancellable = true)
   private void oyvey$viewYRot(float partialTick, CallbackInfoReturnable<Float> cir) {
      Scaffold scaffold = this.oyvey$scaffoldHeadPose();
      if (scaffold != null) {
         cir.setReturnValue(scaffold.getSilentHeadPose().interpolatedYaw(partialTick));
      } else if (KillauraRecode.isRotating) {
         cir.setReturnValue(KillauraRecode.visualYaw);
      } else if (BedNuker.isRotating) {
         cir.setReturnValue(BedNuker.visualYaw);
      } else if (Scaffold.isRotating) {
         cir.setReturnValue(Scaffold.visualYaw);
      } else if (AutoPot.isRotating) {
         cir.setReturnValue(AutoPot.visualYaw);
      } else if (RotationManager.isExtractingLocal && OyVey.rotationManager != null && OyVey.rotationManager.isRenderOverrideActive()) {
         cir.setReturnValue(OyVey.rotationManager.getInterpolatedRenderYaw(partialTick));
      }
   }

   @Inject(method = "getViewXRot", at = @At("HEAD"), cancellable = true)
   private void oyvey$viewXRot(float partialTick, CallbackInfoReturnable<Float> cir) {
      Scaffold scaffold = this.oyvey$scaffoldHeadPose();
      if (scaffold != null) {
         cir.setReturnValue(scaffold.getSilentHeadPose().interpolatedPitch(partialTick));
      } else if (KillauraRecode.isRotating) {
         cir.setReturnValue(KillauraRecode.visualPitch);
      } else if (BedNuker.isRotating) {
         cir.setReturnValue(BedNuker.visualPitch);
      } else if (Scaffold.isRotating) {
         cir.setReturnValue(Scaffold.visualPitch);
      } else if (AutoPot.isRotating) {
         cir.setReturnValue(AutoPot.visualPitch);
      } else if (RotationManager.isExtractingLocal && OyVey.rotationManager != null && OyVey.rotationManager.isRenderOverrideActive()) {
         cir.setReturnValue(OyVey.rotationManager.getInterpolatedRenderPitch(partialTick));
      }
   }
}
