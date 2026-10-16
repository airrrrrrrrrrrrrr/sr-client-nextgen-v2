package me.alpha432.oyvey.mixin.network.accessor;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface MovePlayerPacketAccessor {
   @Mutable
   @Accessor("onGround")
   void oyvey$setOnGround(boolean onGround);
}
