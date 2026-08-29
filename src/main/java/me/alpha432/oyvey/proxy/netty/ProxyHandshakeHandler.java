package me.alpha432.oyvey.proxy.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Queue;
import me.alpha432.oyvey.proxy.ProxyEntry;

public class ProxyHandshakeHandler extends ChannelDuplexHandler {
   private final ProxyEntry proxy;
   private SocketAddress destination;
   private ChannelPromise connectPromise;
   private final Queue<Object[]> pendingWrites = new ArrayDeque<>();
   private ByteBuf cumulation;
   private ProxyHandshakeHandler.State state = ProxyHandshakeHandler.State.INIT;
   private boolean finished;

   public ProxyHandshakeHandler(ProxyEntry proxy) {
      this.proxy = proxy;
   }

   public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
      this.destination = remoteAddress;
      this.connectPromise = promise;
      SocketAddress proxyAddress = new InetSocketAddress(this.proxy.getHost(), this.proxy.getPort());
      ChannelPromise tcp = ctx.newPromise();
      tcp.addListener(f -> {
         if (!f.isSuccess()) {
            this.fail(ctx, f.cause());
         }
      });
      ctx.connect(proxyAddress, localAddress, tcp);
   }

   public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
      if (this.finished) {
         ctx.write(msg, promise);
      } else {
         this.pendingWrites.add(new Object[]{msg, promise});
      }
   }

   public void channelActive(ChannelHandlerContext ctx) throws Exception {
      this.startHandshake(ctx);
   }

   public void channelRead(ChannelHandlerContext ctx, Object msg) {
      if (!this.finished && msg instanceof ByteBuf in) {
         if (this.cumulation == null) {
            this.cumulation = in;
         } else {
            this.cumulation.writeBytes(in);
            in.release();
         }

         try {
            this.decode(ctx);
         } catch (Exception e) {
            this.fail(ctx, e);
         }
      } else {
         ctx.fireChannelRead(msg);
      }
   }

   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      this.fail(ctx, cause);
   }

   public void handlerRemoved(ChannelHandlerContext ctx) {
      if (this.cumulation != null) {
         this.cumulation.release();
         this.cumulation = null;
      }
   }

   private void startHandshake(ChannelHandlerContext ctx) {
      switch (this.proxy.getType()) {
         case SOCKS5:
            ByteBuf buf = ctx.alloc().buffer();
            buf.writeByte(5);
            if (this.proxy.hasAuth()) {
               buf.writeByte(2).writeByte(0).writeByte(2);
            } else {
               buf.writeByte(1).writeByte(0);
            }

            this.state = ProxyHandshakeHandler.State.SOCKS5_GREETED;
            this.send(ctx, buf);
            break;
         case SOCKS4:
            this.state = ProxyHandshakeHandler.State.SOCKS4_SENT;
            this.send(ctx, this.buildSocks4Request(ctx));
            break;
         case HTTP:
            this.state = ProxyHandshakeHandler.State.HTTP_SENT;
            this.send(ctx, this.buildHttpConnect(ctx));
      }
   }

   private void decode(ChannelHandlerContext ctx) {
      ByteBuf b = this.cumulation;
      switch (this.state) {
         case SOCKS5_GREETED:
            if (b.readableBytes() < 2) {
               return;
            }

            b.readByte();
            int method = b.readUnsignedByte();
            if (method == 2) {
               this.sendSocks5Auth(ctx);
               this.state = ProxyHandshakeHandler.State.SOCKS5_AUTHED;
            } else {
               if (method != 0) {
                  throw new RuntimeException("proxy rejected auth methods (0x" + Integer.toHexString(method) + ")");
               }

               this.sendSocks5Connect(ctx);
               this.state = ProxyHandshakeHandler.State.SOCKS5_CONNECTED;
            }

            this.afterDecode(ctx);
            break;
         case SOCKS5_AUTHED:
            if (b.readableBytes() < 2) {
               return;
            }

            b.readByte();
            int status = b.readUnsignedByte();
            if (status != 0) {
               throw new RuntimeException("proxy authentication failed");
            }

            this.sendSocks5Connect(ctx);
            this.state = ProxyHandshakeHandler.State.SOCKS5_CONNECTED;
            this.afterDecode(ctx);
            break;
         case SOCKS5_CONNECTED:
            if (b.readableBytes() < 4) {
               return;
            }

            b.markReaderIndex();
            b.readByte();
            int rep = b.readUnsignedByte();
            b.readByte();
            int atyp = b.readUnsignedByte();

            int addrLen = switch (atyp) {
               case 1 -> 4;
               default -> -1;
               case 3 -> b.isReadable() ? b.readUnsignedByte() : -1;
               case 4 -> 16;
            };
            if (addrLen < 0 || b.readableBytes() < addrLen + 2) {
               b.resetReaderIndex();
               return;
            }

            b.skipBytes(addrLen + 2);
            if (rep != 0) {
               throw new RuntimeException("SOCKS5 connect failed (reply 0x" + Integer.toHexString(rep) + ")");
            }

            this.succeed(ctx);
            break;
         case SOCKS4_SENT:
            if (b.readableBytes() < 8) {
               return;
            }

            b.readByte();
            int cd = b.readUnsignedByte();
            b.skipBytes(6);
            if (cd != 90) {
               throw new RuntimeException("SOCKS4 request rejected (code 0x" + Integer.toHexString(cd) + ")");
            }

            this.succeed(ctx);
            break;
         case HTTP_SENT:
            int end = this.indexOfHeaderEnd(b);
            if (end < 0) {
               return;
            }

            String headers = b.toString(b.readerIndex(), end - b.readerIndex(), CharsetUtil.US_ASCII);
            b.readerIndex(end);
            String statusLine = headers.split("\r\n", 2)[0];
            if (!statusLine.contains(" 200")) {
               throw new RuntimeException("HTTP proxy CONNECT failed: " + statusLine);
            }

            this.succeed(ctx);
      }
   }

   private void afterDecode(ChannelHandlerContext ctx) {
      if (!this.finished && this.cumulation != null && this.cumulation.isReadable()) {
         this.decode(ctx);
      }
   }

   private int indexOfHeaderEnd(ByteBuf b) {
      int n = b.writerIndex();

      for (int i = b.readerIndex(); i + 3 < n; i++) {
         if (b.getByte(i) == 13 && b.getByte(i + 1) == 10 && b.getByte(i + 2) == 13 && b.getByte(i + 3) == 10) {
            return i + 4;
         }
      }

      return -1;
   }

   private void sendSocks5Auth(ChannelHandlerContext ctx) {
      byte[] u = this.proxy.getUsername().getBytes(CharsetUtil.UTF_8);
      byte[] p = this.proxy.getPassword().getBytes(CharsetUtil.UTF_8);
      ByteBuf buf = ctx.alloc().buffer();
      buf.writeByte(1).writeByte(u.length).writeBytes(u).writeByte(p.length).writeBytes(p);
      this.send(ctx, buf);
   }

   private void sendSocks5Connect(ChannelHandlerContext ctx) {
      InetSocketAddress dst = (InetSocketAddress)this.destination;
      ByteBuf buf = ctx.alloc().buffer();
      buf.writeByte(5).writeByte(1).writeByte(0);
      if (!dst.isUnresolved()) {
         byte[] addr = dst.getAddress().getAddress();
         buf.writeByte(addr.length == 16 ? 4 : 1).writeBytes(addr);
      } else {
         byte[] host = dst.getHostString().getBytes(CharsetUtil.US_ASCII);
         buf.writeByte(3).writeByte(host.length).writeBytes(host);
      }

      buf.writeShort(dst.getPort());
      this.send(ctx, buf);
   }

   private ByteBuf buildSocks4Request(ChannelHandlerContext ctx) {
      InetSocketAddress dst = (InetSocketAddress)this.destination;
      byte[] user = this.proxy.getUsername().getBytes(CharsetUtil.US_ASCII);
      ByteBuf buf = ctx.alloc().buffer();
      buf.writeByte(4).writeByte(1).writeShort(dst.getPort());
      byte[] ipv4 = !dst.isUnresolved() && dst.getAddress().getAddress().length == 4 ? dst.getAddress().getAddress() : null;
      if (ipv4 != null) {
         buf.writeBytes(ipv4).writeBytes(user).writeByte(0);
      } else {
         buf.writeByte(0).writeByte(0).writeByte(0).writeByte(1);
         buf.writeBytes(user).writeByte(0);
         buf.writeBytes(dst.getHostString().getBytes(CharsetUtil.US_ASCII)).writeByte(0);
      }

      return buf;
   }

   private ByteBuf buildHttpConnect(ChannelHandlerContext ctx) {
      InetSocketAddress dst = (InetSocketAddress)this.destination;
      String hostPort = dst.getHostString() + ":" + dst.getPort();
      StringBuilder sb = new StringBuilder();
      sb.append("CONNECT ").append(hostPort).append(" HTTP/1.1\r\n");
      sb.append("Host: ").append(hostPort).append("\r\n");
      if (this.proxy.hasAuth()) {
         String creds = this.proxy.getUsername() + ":" + this.proxy.getPassword();
         sb.append("Proxy-Authorization: Basic ").append(Base64.getEncoder().encodeToString(creds.getBytes(CharsetUtil.UTF_8))).append("\r\n");
      }

      sb.append("\r\n");
      return Unpooled.copiedBuffer(sb.toString(), CharsetUtil.US_ASCII);
   }

   private void send(ChannelHandlerContext ctx, ByteBuf buf) {
      ctx.writeAndFlush(buf).addListener(f -> {
         if (!f.isSuccess()) {
            this.fail(ctx, f.cause());
         }
      });
   }

   private void succeed(ChannelHandlerContext ctx) {
      this.finished = true;
      this.state = ProxyHandshakeHandler.State.DONE;
      if (this.cumulation != null && this.cumulation.isReadable()) {
         ByteBuf leftover = this.cumulation;
         this.cumulation = null;
         ctx.fireChannelRead(leftover);
      }

      ctx.pipeline().remove(this);
      ctx.fireChannelActive();
      if (this.connectPromise != null) {
         this.connectPromise.trySuccess();
      }

      this.flushPending(ctx);
   }

   private void flushPending(ChannelHandlerContext ctx) {
      Object[] w;
      while ((w = this.pendingWrites.poll()) != null) {
         ctx.write(w[0], (ChannelPromise)w[1]);
      }

      ctx.flush();
   }

   private void fail(ChannelHandlerContext ctx, Throwable cause) {
      if (!this.finished) {
         this.finished = true;

         Object[] w;
         while ((w = this.pendingWrites.poll()) != null) {
            ((ChannelPromise)w[1]).tryFailure(cause);
            ReferenceCountUtil.release(w[0]);
         }

         if (this.connectPromise != null) {
            this.connectPromise.tryFailure(cause);
         }

         ctx.close();
      }
   }

   private enum State {
      INIT,
      SOCKS5_GREETED,
      SOCKS5_AUTHED,
      SOCKS5_CONNECTED,
      SOCKS4_SENT,
      HTTP_SENT,
      DONE;
   }
}
