package me.alpha432.oyvey.mixin.render.accessor;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface LocalPlayerRotationAccessor {
   @Accessor("yRotLast")
   float oyvey$getLastSentYaw();

   @Accessor("xRotLast")
   float oyvey$getLastSentPitch();
}
