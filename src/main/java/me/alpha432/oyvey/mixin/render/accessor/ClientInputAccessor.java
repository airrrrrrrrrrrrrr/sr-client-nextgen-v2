package me.alpha432.oyvey.mixin.render.accessor;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientInput.class)
public interface ClientInputAccessor {
   @Accessor("moveVector")
   void oyvey$setMoveVector(Vec2 movement);
}
