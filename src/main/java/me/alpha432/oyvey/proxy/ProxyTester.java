package me.alpha432.oyvey.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public final class ProxyTester {
   private static final InetSocketAddress TEST_DEST = new InetSocketAddress("1.1.1.1", 80);
   private static final int TIMEOUT_MS = 8000;

   private ProxyTester() {
   }

   public static void test(ProxyEntry proxy, ProxyTester.Callback callback) {
      if (proxy != null && proxy.isValid()) {
         Thread t = new Thread(() -> run(proxy, callback), "proxy-test");
         t.setDaemon(true);
         t.start();
      } else {
         callback.done(false, 0L, "invalid proxy (host/port)");
      }
   }

   private static void run(final ProxyEntry proxy, ProxyTester.Callback callback) {
      NioEventLoopGroup group = new NioEventLoopGroup(1);
      long start = System.currentTimeMillis();

      try {
         Bootstrap b = (Bootstrap)((Bootstrap)((Bootstrap)((Bootstrap)new Bootstrap().group(group)).channel(NioSocketChannel.class))
               .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000))
            .handler(new ChannelInitializer<Channel>() {
               protected void initChannel(Channel ch) {
                  ch.pipeline().addLast(new ChannelHandler[]{proxy.newHandler()});
               }
            });
         ChannelFuture f = b.connect(TEST_DEST);
         boolean completed = f.await(10000L, TimeUnit.MILLISECONDS);
         long latency = System.currentTimeMillis() - start;
         if (!completed) {
            callback.done(false, latency, "timed out");
         } else if (f.isSuccess()) {
            f.channel().close();
            callback.done(true, latency, "ok");
         } else {
            Throwable c = f.cause();
            callback.done(false, latency, c == null ? "failed" : shortError(c));
         }
      } catch (Throwable e) {
         callback.done(false, System.currentTimeMillis() - start, shortError(e));
      } finally {
         group.shutdownGracefully();
      }
   }

   private static String shortError(Throwable c) {
      String msg = c.getMessage();
      return msg != null && !msg.isEmpty() ? msg : c.getClass().getSimpleName();
   }

   public interface Callback {
      void done(boolean var1, long var2, String var4);
   }
}
