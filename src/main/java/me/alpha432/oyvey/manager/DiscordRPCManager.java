package me.alpha432.oyvey.manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.util.traits.Util;

public class DiscordRPCManager implements Util {
   private static final String CLIENT_ID = "1508954745513967697";
   private static final int OP_HANDSHAKE = 0;
   private static final int OP_FRAME = 1;
   private static final int MAX_BODY_BYTES = 65536;
   private static final long UPDATE_COOLDOWN_MS = 15000L;
   private static final long REFRESH_INTERVAL_MS = 60000L;
   private DiscordRPCManager.IPCConnection connection;
   private final AtomicBoolean active = new AtomicBoolean(false);
   private final ScheduledExecutorService executor;
   private volatile String pendingDetails = "In Main Menu";
   private volatile String pendingState = "Made by bielbanu and love with air";
   private String sentKey = "";
   private long lastUpdateMs = 0L;
   private final long startTimeSecs = System.currentTimeMillis() / 1000L;

   public DiscordRPCManager() {
      this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
         Thread t = new Thread(r, "discord-rpc");
         t.setDaemon(true);
         return t;
      });
   }

   public void init() {
      this.executor.scheduleWithFixedDelay(this::tick, 0L, 5L, TimeUnit.SECONDS);
   }

   public void shutdown() {
      this.executor.execute(() -> {
         this.active.set(false);
         this.clearPresence();
         this.disconnect();
      });
      this.executor.shutdown();
   }

   public void setActive(boolean value) {
      this.active.set(value);
      this.executor.execute(() -> {
         if (value) {
            this.sentKey = "";
            if (this.connection != null) {
               this.sendPresence();
            }
         } else {
            this.clearPresence();
         }
      });
   }

   public void notifyUpdate(String details, String state) {
      this.pendingDetails = details;
      this.pendingState = state;
      this.executor.execute(() -> {
         if (this.active.get() && this.connection != null) {
            String key = details + "|" + state;
            long now = System.currentTimeMillis();
            if (!key.equals(this.sentKey) && now - this.lastUpdateMs >= 15000L) {
               this.sendPresence();
            }
         }
      });
   }

   private void tick() {
      try {
         if (this.connection == null) {
            this.tryConnect();
         }

         if (this.connection == null || !this.active.get()) {
            return;
         }

         String key = this.pendingDetails + "|" + this.pendingState;
         long now = System.currentTimeMillis();
         boolean changed = !key.equals(this.sentKey);
         boolean stale = now - this.lastUpdateMs >= 60000L;
         if ((changed || stale) && now - this.lastUpdateMs >= 15000L) {
            this.sendPresence();
         }
      } catch (Exception var6) {
         OyVey.LOGGER.warn("[DiscordRPC] Tick error \u2014 reconnecting: {}", var6.getMessage());
         this.disconnect();
      }
   }

   private void tryConnect() {
      boolean isWin = System.getProperty("os.name", "").toLowerCase().contains("win");

      for (int i = 0; i <= 9; i++) {
         try {
            this.connection = isWin ? new DiscordRPCManager.WindowsPipe(i) : new DiscordRPCManager.UnixSocket(i);
            this.sendHandshake();
            this.readFrame();
            this.sentKey = "";
            OyVey.LOGGER.info("[DiscordRPC] Connected to Discord (pipe {})", i);
            if (this.active.get()) {
               this.sendPresence();
            }

            return;
         } catch (Exception var4) {
            if (this.connection != null) {
               this.connection.close();
               this.connection = null;
            }
         }
      }
   }

   private void disconnect() {
      if (this.connection != null) {
         this.connection.close();
         this.connection = null;
      }

      this.sentKey = "";
   }

   private void sendHandshake() throws IOException {
      this.writeFrame(0, "{\"v\":1,\"client_id\":\"1508954745513967697\"}");
   }

   private void sendPresence() {
      if (this.connection != null) {
         try {
            this.writeFrame(1, this.buildActivityPayload());
            this.readFrame();
            this.sentKey = this.pendingDetails + "|" + this.pendingState;
            this.lastUpdateMs = System.currentTimeMillis();
         } catch (IOException var2) {
            OyVey.LOGGER.warn("[DiscordRPC] Send failed: {}", var2.getMessage());
            this.disconnect();
         }
      }
   }

   private void clearPresence() {
      if (this.connection != null) {
         try {
            this.writeFrame(1, "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + pid() + ",\"activity\":null},\"nonce\":\"" + UUID.randomUUID() + "\"}");
            this.readFrame();
         } catch (IOException var2) {
            this.disconnect();
         }
      }
   }

   private String buildActivityPayload() {
      return "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":"
         + pid()
         + ",\"activity\":{\"details\":\""
         + esc(this.pendingDetails)
         + "\",\"state\":\""
         + esc(this.pendingState)
         + "\",\"timestamps\":{\"start\":"
         + this.startTimeSecs
         + "},\"assets\":{\"large_image\":\"srclient\",\"large_text\":\"SR Client Nextgen\"}}},\"nonce\":\""
         + UUID.randomUUID()
         + "\"}";
   }

   private void writeFrame(int opcode, String json) throws IOException {
      byte[] payload = json.getBytes(StandardCharsets.UTF_8);
      ByteBuffer hdr = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
      hdr.putInt(opcode).putInt(payload.length);
      byte[] frame = new byte[8 + payload.length];
      System.arraycopy(hdr.array(), 0, frame, 0, 8);
      System.arraycopy(payload, 0, frame, 8, payload.length);
      this.connection.write(frame);
   }

   private String readFrame() throws IOException {
      byte[] hdr = new byte[8];
      this.connection.readFully(hdr);
      ByteBuffer buf = ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN);
      buf.getInt();
      int length = buf.getInt();
      if (length > 0 && length <= 65536) {
         byte[] body = new byte[length];
         this.connection.readFully(body);
         return new String(body, StandardCharsets.UTF_8);
      } else {
         return "";
      }
   }

   private static long pid() {
      return ProcessHandle.current().pid();
   }

   private static String esc(String s) {
      return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
   }

   private interface IPCConnection {
      void write(byte[] var1) throws IOException;

      void readFully(byte[] var1) throws IOException;

      void close();
   }

   private static final class UnixSocket implements DiscordRPCManager.IPCConnection {
      private final SocketChannel channel;
      private final InputStream in;
      private final OutputStream out;

      UnixSocket(int index) throws IOException {
         String base = resolveSocketBase();
         UnixDomainSocketAddress addr = UnixDomainSocketAddress.of(Paths.get(base, "discord-ipc-" + index));
         this.channel = SocketChannel.open(StandardProtocolFamily.UNIX);
         this.channel.connect(addr);
         this.in = Channels.newInputStream(this.channel);
         this.out = Channels.newOutputStream(this.channel);
      }

      private static String resolveSocketBase() throws IOException {
         String[] candidates = new String[]{System.getenv("XDG_RUNTIME_DIR"), System.getenv("TMPDIR"), System.getenv("TMP"), "/tmp"};

         for (String c : candidates) {
            if (c != null && !c.isBlank()) {
               return c;
            }
         }

         throw new IOException("Cannot locate Discord IPC socket directory");
      }

      @Override
      public void write(byte[] d) throws IOException {
         this.out.write(d);
         this.out.flush();
      }

      @Override
      public void readFully(byte[] buf) throws IOException {
         int total = 0;

         while (total < buf.length) {
            int n = this.in.read(buf, total, buf.length - total);
            if (n < 0) {
               throw new IOException("Discord IPC connection closed");
            }

            total += n;
         }
      }

      @Override
      public void close() {
         try {
            this.channel.close();
         } catch (Exception var2) {
         }
      }
   }

   private static final class WindowsPipe implements DiscordRPCManager.IPCConnection {
      private final RandomAccessFile raf;

      WindowsPipe(int index) throws IOException {
         this.raf = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + index, "rw");
      }

      @Override
      public void write(byte[] d) throws IOException {
         this.raf.write(d);
      }

      @Override
      public void readFully(byte[] b) throws IOException {
         this.raf.readFully(b);
      }

      @Override
      public void close() {
         try {
            this.raf.close();
         } catch (Exception var2) {
         }
      }
   }
}
