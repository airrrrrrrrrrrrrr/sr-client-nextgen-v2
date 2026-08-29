package me.alpha432.oyvey.features.modules.misc;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record LunarApolloPayload(byte[] data) implements CustomPacketPayload {
   public static final Identifier CHANNEL = Identifier.parse("lunar:apollo");
   public static final Type<LunarApolloPayload> TYPE = new Type(CHANNEL);
   public static final StreamCodec<FriendlyByteBuf, LunarApolloPayload> CODEC = CustomPacketPayload.codec(
      (payload, buf) -> buf.writeBytes(payload.data()), buf -> {
         byte[] data = new byte[buf.readableBytes()];
         buf.readBytes(data);
         return new LunarApolloPayload(data);
      }
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
