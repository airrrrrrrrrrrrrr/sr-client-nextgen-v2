package me.alpha432.oyvey.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.combat.KnockbackDisplace;
import me.alpha432.oyvey.features.modules.render.TargetESP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
   @WrapMethod(method = "attack")
   private void oyvey$attackRotation(Player player, Entity target, Operation<Void> original) {
      if (OyVey.recordedRotationManager != null && OyVey.recordedRotationManager.isRecordingTarget(target)) {
         target.animateHurt(0.0F);
         return;
      }
      KnockbackDisplace displace = OyVey.moduleManager == null ? null
         : OyVey.moduleManager.getModuleByClass(KnockbackDisplace.class);
      if (displace == null || !displace.isEnabled()) {
         original.call(player, target);
      } else {
         displace.attack(player, target, () -> original.call(player, target));
      }
      TargetESP esp = OyVey.moduleManager == null ? null : OyVey.moduleManager.getModuleByClass(TargetESP.class);
      if (player == Minecraft.getInstance().player && esp != null && esp.isEnabled()) esp.onAttack(target);
   }
}
