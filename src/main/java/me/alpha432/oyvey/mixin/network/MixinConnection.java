package me.alpha432.oyvey.mixin.network;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.features.modules.misc.ClientSpoof;
import me.alpha432.oyvey.features.modules.player.FakeLagModule;
import me.alpha432.oyvey.proxy.ProxyEntry;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class MixinConnection {
   @Shadow
   private Channel channel;
   @Shadow
   @Final
   private PacketFlow receiving;

   @Redirect(
      method = "connect",
      at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;handler(Lio/netty/channel/ChannelHandler;)Lio/netty/bootstrap/AbstractBootstrap;"),
      require = 0
   )
   private static AbstractBootstrap srclient$injectProxy(Bootstrap bootstrap, final ChannelHandler original) {
      final ProxyEntry active = OyVey.proxyManager == null ? null : OyVey.proxyManager.getActive();
      return active != null && active.isValid() ? bootstrap.handler(new ChannelInitializer<Channel>() {
         protected void initChannel(Channel ch) {
            ch.pipeline().addLast("srclient_proxy", active.newHandler());
            ch.pipeline().addLast(new ChannelHandler[]{original});
         }
      }) : bootstrap.handler(original);
   }

   @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
   public void channelRead0(ChannelHandlerContext chc, Packet<?> packet, CallbackInfo ci) {
      if (this.channel.isOpen() && packet != null) {
         if (Minecraft.getInstance().player == null) {
            return;
         }

         try {
            if (Util.EVENT_BUS.post(new PacketEvent.Receive(packet))) {
               ci.cancel();
            }
         } catch (Exception var5) {
         }
      }
   }

   @ModifyVariable(method = "sendPacket", at = @At("HEAD"), argsOnly = true)
   private Packet<?> spoofClientBrand(Packet<?> packet) {
      if (this.receiving == PacketFlow.CLIENTBOUND
         && packet instanceof ServerboundCustomPayloadPacket payload
         && payload.payload() instanceof BrandPayload brand
         && !brand.brand().equals("lunarclient")) {
         try {
            ClientSpoof spoof = OyVey.moduleManager.getModuleByClass(ClientSpoof.class);
            if (spoof != null && spoof.isEnabled()) {
               return new ServerboundCustomPayloadPacket(new BrandPayload("lunarclient"));
            }
         } catch (Exception var5) {
         }
      }

      return packet;
   }

   @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
   private void sendPacket(Packet<?> packet, ChannelFutureListener callbacks, boolean flush, CallbackInfo ci) {
      if (this.receiving == PacketFlow.CLIENTBOUND) {
         if (Minecraft.getInstance().player == null) {
            return;
         }

         try {
            FakeLagModule fakelag = OyVey.moduleManager.getModuleByClass(FakeLagModule.class);
            if (fakelag != null && fakelag.isEnabled() && fakelag.shouldQueuePacket(packet)) {
               fakelag.queuePacket(packet);
               ci.cancel();
               return;
            }
         } catch (Exception var7) {
         }

         try {
            if (Util.EVENT_BUS.post(new PacketEvent.Send(packet))) {
               ci.cancel();
            }
         } catch (Exception var6) {
         }
      }
   }
}
