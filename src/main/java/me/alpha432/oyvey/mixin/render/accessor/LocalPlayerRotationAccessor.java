package me.alpha432.oyvey.mixin.render.accessor;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LocalPlayer.class)
public interface LocalPlayerRotationAccessor {
   @Accessor("yRotLast")
   float oyvey$getLastSentYaw();

   @Accessor("xRotLast")
   float oyvey$getLastSentPitch();

   @Invoker("sendIsSprintingIfNeeded")
   void oyvey$sendSprintState();

   @Invoker("canStartSprinting")
   boolean oyvey$canStartSprinting();

   @Invoker("shouldStopRunSprinting")
   boolean oyvey$shouldStopRunSprinting();

   @Invoker("shouldStopSwimSprinting")
   boolean oyvey$shouldStopSwimSprinting();
}
