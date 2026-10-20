package me.alpha432.oyvey.mixin.input;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.features.modules.combat.KnockbackDisplace;
import me.alpha432.oyvey.features.modules.combat.BedNuker;
import me.alpha432.oyvey.features.modules.combat.WTap;
import me.alpha432.oyvey.features.modules.combat.PvpBot;
import me.alpha432.oyvey.features.modules.movement.Scaffold;
import me.alpha432.oyvey.features.modules.movement.SprintModule;
import me.alpha432.oyvey.features.modules.movement.Speed;
import me.alpha432.oyvey.features.modules.movement.InventoryMove;
import me.alpha432.oyvey.features.modules.player.VelocityModule;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {
   @Inject(method = "tick", at = @At("HEAD"))
   private void oyvey$inventoryMoveInput(CallbackInfo ci) {
      if (OyVey.moduleManager == null) return;
      InventoryMove inventoryMove = OyVey.moduleManager.getModuleByClass(InventoryMove.class);
      if (inventoryMove != null) inventoryMove.updateInputKeys();
   }

   @Inject(method = "tick", at = @At("RETURN"))
   private void oyvey$correctSilentRotationMovement(CallbackInfo ci) {
      if (OyVey.moduleManager == null) {
         return;
      }
      ClientInput nativeInput = (ClientInput)(Object)this;
      Input originalKeys = nativeInput.keyPresses;
      Vec2 originalVector = nativeInput.getMoveVector();
      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      if (scaffold != null) scaffold.captureRawInput(nativeInput, originalKeys, originalVector);

      KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      if (aura != null) {
         aura.applyGrimMovementCorrection((ClientInput)(Object)this);
      }
      KnockbackDisplace displace = OyVey.moduleManager.getModuleByClass(KnockbackDisplace.class);
      if (displace != null) displace.applyInput((ClientInput)(Object)this);
      BedNuker nuker = OyVey.moduleManager.getModuleByClass(BedNuker.class);
      if (nuker != null) nuker.applyInput((ClientInput)(Object)this);

      if (scaffold != null) {
         scaffold.applyInput((ClientInput)(Object)this);
      }

      WTap wTap = OyVey.moduleManager.getModuleByClass(WTap.class);
      if (wTap != null) {
         wTap.applyInputReset((ClientInput)(Object)this);
      }

      PvpBot pvpBot = OyVey.moduleManager.getModuleByClass(PvpBot.class);
      if (pvpBot != null) pvpBot.applyInput((ClientInput)(Object)this);

      SprintModule sprint = OyVey.moduleManager.getModuleByClass(SprintModule.class);
      if (sprint != null) {
         sprint.applySprintInput((ClientInput)(Object)this);
      }

      Speed speed = OyVey.moduleManager.getModuleByClass(Speed.class);
      if (speed != null) {
         speed.applyVulcan2Input((ClientInput)(Object)this);
      }
      if (scaffold != null) {
         scaffold.restoreUncorrectedInput(nativeInput,originalKeys,originalVector);
         scaffold.enforceSprintInput((ClientInput)(Object)this);
      }
      VelocityModule velocity = OyVey.moduleManager.getModuleByClass(VelocityModule.class);
      if (velocity != null) velocity.applyJumpResetInput((ClientInput)(Object)this);
      if (aura != null) {
         aura.applyCriticalSprintInput((ClientInput)(Object)this);
         aura.applyProSprintInput((ClientInput)(Object)this);
         aura.recordInputTrace((ClientInput)(Object)this);
      }
   }
}
