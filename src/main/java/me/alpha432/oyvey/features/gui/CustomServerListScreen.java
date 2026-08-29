package me.alpha432.oyvey.features.gui;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class CustomServerListScreen extends Screen {
   private static final int STARS = 100;
   private final float[] sx = new float[100];
   private final float[] sy = new float[100];
   private final float[] svx = new float[100];
   private final float[] svy = new float[100];
   private final float[] sa = new float[100];
   private static final int VISIBLE_ROWS = 7;
   private static final int ROW_H = 16;
   private static final int LW = 300;
   private ServerList servers;
   private ServerStatusPinger pinger;
   private ExecutorService pingExecutor;
   private long pingGeneration;
   private final Map<ServerData, FaviconTexture> favicons = new HashMap<>();
   private final Set<ServerData> hasIcon = new HashSet<>();
   private final Map<ServerData, byte[]> uploadedBytes = new HashMap<>();
   private int selectedIndex = -1;
   private int scroll;
   private boolean viaLoaded;
   private final CustomServerListScreen.SimpleField nameField = new CustomServerListScreen.SimpleField(32);
   private final CustomServerListScreen.SimpleField ipField = new CustomServerListScreen.SimpleField(128);
   private CustomServerListScreen.SimpleField focused;
   private long openTime;

   public CustomServerListScreen() {
      super(Component.literal("servers"));
   }

   public void init() {
      this.openTime = System.currentTimeMillis();
      this.focused = this.nameField;
      this.servers = new ServerList(this.minecraft);
      this.servers.load();
      if (this.pingExecutor != null) {
         this.pingExecutor.shutdownNow();
      }

      this.pingExecutor = Executors.newFixedThreadPool(4, task -> {
         Thread thread = new Thread(task, "srclient-server-ping");
         thread.setDaemon(true);
         return thread;
      });
      this.viaLoaded = FabricLoader.getInstance().isModLoaded("viafabricplus");
      this.pingAll();
      Random rng = new Random();

      for (int i = 0; i < 100; i++) {
         this.sx[i] = rng.nextFloat() * this.width;
         this.sy[i] = rng.nextFloat() * this.height;
         float angle = rng.nextFloat() * 6.2831855F;
         float spd = 0.04F + rng.nextFloat() * 0.14F;
         this.svx[i] = (float)Math.cos(angle) * spd;
         this.svy[i] = (float)Math.sin(angle) * spd;
         this.sa[i] = 0.25F + rng.nextFloat() * 0.55F;
      }
   }

   private int listX() {
      return this.width / 2 - 150;
   }

   private int listTop() {
      return (int)(this.height * 0.22F);
   }

   private int formY() {
      return this.height - 92;
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      if (this.pinger != null) {
         this.pinger.tick();
      }

      float age = (float)(System.currentTimeMillis() - this.openTime) / 1000.0F;
      float fadeIn = Mth.clamp(age / 0.6F, 0.0F, 1.0F);
      int accent = OyVey.colorManager.getColorAsInt();
      int aR = accent >> 16 & 0xFF;
      int aG = accent >> 8 & 0xFF;
      int aB = accent & 0xFF;
      g.fill(0, 0, this.width, this.height, -16316657);
      RenderUtil.verticalGradient(g, 0.0F, 0.0F, this.width, this.height * 0.5F, new Color(aR, aG, aB, 14), new Color(0, 0, 0, 0));

      for (int i = 0; i < 100; i++) {
         this.sx[i] = this.sx[i] + this.svx[i];
         if (this.sx[i] < 0.0F) {
            this.sx[i] = this.sx[i] + this.width;
         }

         if (this.sx[i] > this.width) {
            this.sx[i] = this.sx[i] - this.width;
         }

         this.sy[i] = this.sy[i] + this.svy[i];
         if (this.sy[i] < 0.0F) {
            this.sy[i] = this.sy[i] + this.height;
         }

         if (this.sy[i] > this.height) {
            this.sy[i] = this.sy[i] - this.height;
         }

         g.fill((int)this.sx[i], (int)this.sy[i], (int)this.sx[i] + 1, (int)this.sy[i] + 1, argb((int)(this.sa[i] * fadeIn * 140.0F), aR, aG, aB));
      }

      g.centeredText(this.font, "SERVERS", this.width / 2, (int)(this.height * 0.12F), argb((int)(fadeIn * 255.0F), aR, aG, aB));
      String acct = "account: " + this.currentAccount();
      boolean acctHov = mx >= 8 && mx < 8 + this.font.width(acct) && my >= 6 && my < 16;
      g.text(this.font, acct, 8, 8, argb((int)(fadeIn * (acctHov ? 235 : 180)), aR, aG, aB), false);
      if (this.viaLoaded) {
         String via = "viafabricplus";
         int vw = this.font.width(via);
         boolean vHov = mx >= this.width - 8 - vw && mx < this.width - 8 && my >= 6 && my < 16;
         g.text(this.font, via, this.width - 8 - vw, 8, argb((int)(fadeIn * (vHov ? 235 : 180)), aR, aG, aB), false);
      }

      int x = this.listX();
      int top = this.listTop();
      int count = this.servers == null ? 0 : this.servers.size();
      g.text(this.font, "servers (" + count + ")", x, top - 11, argb((int)(fadeIn * 160.0F), aR, aG, aB), false);
      String refresh = "refresh";
      int rfw = this.font.width(refresh);
      boolean rfHov = mx >= x + 300 - rfw - 2 && mx < x + 300 + 2 && my >= top - 12 && my < top - 2;
      g.text(this.font, refresh, x + 300 - rfw, top - 11, argb((int)(fadeIn * (rfHov ? 235 : 170)), aR, aG, aB), false);

      for (int r = 0; r < 7; r++) {
         int idx = this.scroll + r;
         int ry = top + r * 16;
         boolean hov = mx >= x && mx < x + 300 && my >= ry && my < ry + 16 - 1;
         boolean sel = idx == this.selectedIndex;
         RenderUtil.roundedRect(g, x, ry, 300, 15, 2, argb((int)(fadeIn * (sel ? 200 : (hov ? 150 : 90))), sel ? 24 : 14, sel ? 24 : 14, sel ? 44 : 28));
         if (idx >= 0 && idx < count) {
            ServerData sd = this.servers.get(idx);
            int ix = x + 4;
            int iy = ry + 1;
            FaviconTexture tex = this.favicons.get(sd);
            byte[] iconBytes = sd.getIconBytes();
            if (tex != null && iconBytes != null && iconBytes != this.uploadedBytes.get(sd)) {
               this.uploadedBytes.put(sd, iconBytes);
               this.uploadFavicon(sd);
            }

            if (tex != null && this.hasIcon.contains(sd)) {
               g.blit(RenderPipelines.GUI_TEXTURED, tex.textureLocation(), ix, iy, 0.0F, 0.0F, 12, 12, 12, 12);
            } else {
               RenderUtil.roundedRect(g, ix, iy + 1, 10, 10, 2, argb((int)(fadeIn * 220.0F), aR / 3 + 14, aG / 3 + 14, aB / 3 + 26));
               g.fill(ix + 2, iy + 3, ix + 8, iy + 5, argb((int)(fadeIn * 210.0F), aR, aG, aB));
               g.fill(ix + 2, iy + 7, ix + 7, iy + 9, argb((int)(fadeIn * 140.0F), aR, aG, aB));
            }

            g.text(this.font, stripCodes(sd.name), x + 18, ry + 3, argb((int)(fadeIn * 235.0F), 221, 238, 255), false);
            String ping = pingText(sd);
            int pw = this.font.width(ping);
            g.text(this.font, ping, x + 300 - pw - 5, ry + 3, this.pingColor(sd, fadeIn), false);
            String ip = sd.ip == null ? "" : sd.ip;
            g.text(this.font, ip, x + 300 - pw - 12 - this.font.width(ip), ry + 3, argb((int)(fadeIn * 150.0F), 120, 150, 180), false);
         } else {
            g.text(this.font, "-", x + 6, ry + 3, argb((int)(fadeIn * 70.0F), 120, 140, 160), false);
         }
      }

      int fy = this.formY();
      this.drawField(g, this.nameField, "server name", x, fy, 120, mx, my, fadeIn, aR, aG, aB);
      this.drawField(g, this.ipField, "server address (ip or ip:port)", x + 126, fy, 174, mx, my, fadeIn, aR, aG, aB);
      int by = fy + 26;
      int bw = 72;
      this.drawButton(g, "connect", x, by, bw, 18, mx, my, fadeIn, aR, aG, aB);
      this.drawButton(g, "add", x + bw + 4, by, bw, 18, mx, my, fadeIn, aR, aG, aB);
      this.drawButton(g, "delete", x + 2 * (bw + 4), by, bw, 18, mx, my, fadeIn, aR, aG, aB);
      this.drawButton(g, "back", x + 3 * (bw + 4), by, bw, 18, mx, my, fadeIn, aR, aG, aB);
      g.centeredText(this.font, "double-click a server to connect", this.width / 2, by + 24, argb((int)(fadeIn * 130.0F), 120, 140, 160));
   }

   private void drawButton(GuiGraphicsExtractor g, String label, int x, int y, int w, int h, int mx, int my, float fadeIn, int aR, int aG, int aB) {
      boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
      int top = argb((int)(fadeIn * (hov ? 230 : 175)), hov ? 26 : 12, hov ? 26 : 12, hov ? 46 : 28);
      int bottom = argb((int)(fadeIn * (hov ? 215 : 165)), hov ? 12 : 7, hov ? 12 : 7, hov ? 30 : 20);
      RenderUtil.roundedRectGradient(g, x, y, w, h, 4.0F, top, bottom, true);
      RenderUtil.roundedOutline(g, x, y, w, h, 4.0F, 0.75F, argb((int)(fadeIn * (hov ? 180 : 65)), aR, aG, aB));
      int lc = hov ? argb((int)(fadeIn * 255.0F), aR, aG, aB) : argb((int)(fadeIn * 225.0F), 187, 204, 221);
      g.text(this.font, label, x + (w - this.font.width(label)) / 2, y + (h - 8) / 2, lc, false);
   }

   private void drawField(
      GuiGraphicsExtractor g, CustomServerListScreen.SimpleField f, String ph, int x, int y, int w, int mx, int my, float fadeIn, int aR, int aG, int aB
   ) {
      int h = 18;
      boolean foc = this.focused == f;
      RenderUtil.roundedRect(g, x, y, w, h, 2, argb((int)(fadeIn * (foc ? 210 : 170)), foc ? 20 : 12, foc ? 20 : 12, foc ? 40 : 28));
      RenderUtil.roundedOutline(g, x, y, w, h, 3.0F, foc ? 0.9F : 0.55F, argb((int)(fadeIn * (foc ? 190 : 55)), aR, aG, aB));
      String shown = f.text + (foc && f.cursorVisible() ? "|" : "");

      while (this.font.width(shown) > w - 8 && shown.length() > 0) {
         shown = shown.substring(1);
      }

      if (f.text.isEmpty() && !foc) {
         g.text(this.font, ph, x + 4, y + (h - 8) / 2, argb((int)(fadeIn * 80.0F), 170, 187, 204), false);
      } else {
         g.text(this.font, shown, x + 4, y + (h - 8) / 2, argb((int)(fadeIn * 230.0F), 221, 238, 255), false);
      }
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      if (click.button() != 0) {
         return super.mouseClicked(click, doubled);
      }

      double mx = click.x();
      double my = click.y();
      String acct = "account: " + this.currentAccount();
      if (mx >= 8.0 && mx < 8 + this.font.width(acct) && my >= 6.0 && my < 16.0) {
         this.minecraft.setScreen(new AccountManagerScreen());
         return true;
      }

      if (this.viaLoaded) {
         int vw = this.font.width("viafabricplus");
         if (mx >= this.width - 8 - vw && mx < this.width - 8 && my >= 6.0 && my < 16.0) {
            this.openViaFabricPlus();
            return true;
         }
      }

      int x = this.listX();
      int top = this.listTop();
      int rfw = this.font.width("refresh");
      if (mx >= x + 300 - rfw - 2 && mx < x + 300 + 2 && my >= top - 12 && my < top - 2) {
         this.pingAll();
         return true;
      }

      if (mx >= x && mx < x + 300) {
         int rel = (int)((my - top) / 16.0);
         if (rel >= 0 && rel < 7) {
            int idx = this.scroll + rel;
            if (this.servers != null && idx >= 0 && idx < this.servers.size()) {
               if (doubled && idx == this.selectedIndex) {
                  this.connect(this.servers.get(idx));
               } else {
                  this.select(idx);
               }

               return true;
            }
         }
      }

      int fy = this.formY();
      if (hit(mx, my, x, fy, 120, 18)) {
         this.focused = this.nameField;
         return true;
      } else if (hit(mx, my, x + 126, fy, 174, 18)) {
         this.focused = this.ipField;
         return true;
      } else {
         int by = fy + 26;
         int bw = 72;
         if (hit(mx, my, x, by, bw, 18)) {
            this.connectForm();
            return true;
         } else if (hit(mx, my, x + bw + 4, by, bw, 18)) {
            this.saveForm();
            return true;
         } else if (hit(mx, my, x + 2 * (bw + 4), by, bw, 18)) {
            this.deleteSelected();
            return true;
         } else if (hit(mx, my, x + 3 * (bw + 4), by, bw, 18)) {
            this.minecraft.setScreen(new CustomMainMenuScreen());
            return true;
         } else {
            return super.mouseClicked(click, doubled);
         }
      }
   }

   public boolean keyPressed(KeyEvent input) {
      int key = input.input();
      long win = this.minecraft.getWindow().handle();
      boolean ctrl = GLFW.glfwGetKey(win, 341) == 1 || GLFW.glfwGetKey(win, 345) == 1;
      if (ctrl) {
         if (key == 86) {
            String clip = GLFW.glfwGetClipboardString(win);
            if (clip != null) {
               this.focused.append(clip.replaceAll("[\\r\\n]", ""));
            }

            return true;
         }

         if (key == 67) {
            GLFW.glfwSetClipboardString(win, this.focused.text);
            return true;
         }

         if (key == 88) {
            GLFW.glfwSetClipboardString(win, this.focused.text);
            this.focused.text = "";
            return true;
         }
      }

      if (key == 256) {
         this.minecraft.setScreen(new CustomMainMenuScreen());
         return true;
      }

      if (key == 258) {
         this.focused = this.focused == this.nameField ? this.ipField : this.nameField;
         return true;
      }

      if (key == 259) {
         this.focused.backspace();
         return true;
      }

      if (key != 257 && key != 335) {
         return super.keyPressed(input);
      }

      this.saveForm();
      return true;
   }

   public boolean charTyped(CharacterEvent input) {
      this.focused.type(input.codepoint());
      return true;
   }

   private void select(int idx) {
      this.selectedIndex = idx;
      ServerData sd = this.servers.get(idx);
      this.nameField.text = stripCodes(sd.name);
      this.ipField.text = sd.ip == null ? "" : sd.ip;
   }

   private void saveForm() {
      String name = this.nameField.text.trim();
      String ip = this.ipField.text.trim();
      if (!ip.isEmpty()) {
         if (name.isEmpty()) {
            name = ip;
         }

         this.servers.add(new ServerData(name, ip, Type.OTHER), false);
         this.selectedIndex = this.servers.size() - 1;
         this.servers.save();
         this.pingOne(this.servers.get(this.selectedIndex));
      }
   }

   private String currentAccount() {
      try {
         return this.minecraft.getUser().getName();
      } catch (Exception e) {
         return "account";
      }
   }

   private void openViaFabricPlus() {
      String[] candidates = new String[]{
         "de.florianmichael.viafabricplus.screen.impl.ProtocolSelectionScreen", "de.florianmichael.viafabricplus.screen.ProtocolSelectionScreen"
      };

      for (String cn : candidates) {
         try {
            Class<?> c = Class.forName(cn);
            Object screen = c.getConstructor(Screen.class).newInstance(this);
            this.minecraft.setScreen((Screen)screen);
            return;
         } catch (Throwable var8) {
         }
      }

      OyVey.LOGGER.warn("[servers] ViaFabricPlus is loaded but its screen class wasn't found - tell me the screen class for your VFP version");
   }

   private void deleteSelected() {
      if (this.selectedIndex >= 0 && this.selectedIndex < this.servers.size()) {
         ServerData sd = this.servers.get(this.selectedIndex);
         FaviconTexture t = this.favicons.remove(sd);
         if (t != null) {
            t.close();
         }

         this.hasIcon.remove(sd);
         this.uploadedBytes.remove(sd);
         this.servers.remove(sd);
         this.servers.save();
         this.selectedIndex = -1;
         this.nameField.text = "";
         this.ipField.text = "";
      }
   }

   private void connectForm() {
      if (this.selectedIndex >= 0 && this.selectedIndex < this.servers.size()) {
         this.connect(this.servers.get(this.selectedIndex));
      } else if (!this.ipField.text.trim().isEmpty()) {
         String ip = this.ipField.text.trim();
         String name = this.nameField.text.trim().isEmpty() ? ip : this.nameField.text.trim();
         this.connect(new ServerData(name, ip, Type.OTHER));
      }
   }

   private void connect(ServerData data) {
      if (data.ip != null && !data.ip.isEmpty()) {
         ServerAddress addr = ServerAddress.parseString(data.ip);
         ConnectScreen.startConnecting(this, this.minecraft, addr, data, false, null);
      }
   }

   private void pingAll() {
      if (this.servers != null && this.pingExecutor != null && !this.pingExecutor.isShutdown()) {
         ServerStatusPinger previous = this.pinger;
         if (previous != null) {
            previous.removeAll();
         }

         this.pinger = new ServerStatusPinger();
         this.pingGeneration++;

         for (int i = 0; i < this.servers.size(); i++) {
            this.pingOne(this.servers.get(i));
         }
      }
   }

   private void pingOne(ServerData sd) {
      sd.ping = -2L;
      sd.setIconBytes(null);
      this.hasIcon.remove(sd);
      this.uploadedBytes.remove(sd);
      this.favicons.computeIfAbsent(sd, s -> FaviconTexture.forServer(this.minecraft.getTextureManager(), s.ip));
      ServerStatusPinger activePinger = this.pinger;
      ExecutorService executor = this.pingExecutor;
      long generation = this.pingGeneration;
      if (activePinger == null || executor == null || executor.isShutdown()) {
         return;
      }

      try {
         executor.execute(() -> {
            try {
               activePinger.pingServer(sd, () -> {}, () -> {}, EventLoopGroupHolder.remote(false));
            } catch (Exception e) {
               OyVey.LOGGER.debug("[servers] ping failed for {}: {}", sd.ip, e.toString());
            } finally {
               if (generation != this.pingGeneration || activePinger != this.pinger) {
                  activePinger.removeAll();
               }
            }
         });
      } catch (RejectedExecutionException ignored) {
      }
   }

   private void uploadFavicon(ServerData sd) {
      FaviconTexture tex = this.favicons.get(sd);
      if (tex != null) {
         byte[] bytes = sd.getIconBytes();
         if (bytes == null) {
            tex.clear();
            this.hasIcon.remove(sd);
         } else {
            try {
               tex.upload(NativeImage.read(bytes));
               this.hasIcon.add(sd);
            } catch (Exception e) {
               tex.clear();
               this.hasIcon.remove(sd);
               OyVey.LOGGER.warn("[servers] favicon decode failed for {}: {}", sd.ip, e.toString());
            }
         }
      }
   }

   private static String pingText(ServerData sd) {
      return sd.ping < 0L ? "..." : sd.ping + "ms";
   }

   private int pingColor(ServerData sd, float fadeIn) {
      long p = sd.ping;
      if (p < 0L) {
         return argb((int)(fadeIn * 130.0F), 120, 140, 160);
      } else if (p < 100L) {
         return argb((int)(fadeIn * 225.0F), 90, 230, 120);
      } else {
         return p < 250L ? argb((int)(fadeIn * 225.0F), 220, 200, 60) : argb((int)(fadeIn * 225.0F), 255, 90, 90);
      }
   }

   public void removed() {
      this.pingGeneration++;
      ServerStatusPinger activePinger = this.pinger;
      this.pinger = null;
      if (activePinger != null) {
         activePinger.removeAll();
      }

      if (this.pingExecutor != null) {
         this.pingExecutor.shutdownNow();
         this.pingExecutor = null;
      }

      for (FaviconTexture t : this.favicons.values()) {
         try {
            t.close();
         } catch (Exception var4) {
         }
      }

      this.favicons.clear();
      this.hasIcon.clear();
      this.uploadedBytes.clear();
   }

   private static String stripCodes(String s) {
      return s == null ? "" : s.replaceAll("\u00a7.", "");
   }

   private static boolean hit(double mx, double my, int x, int y, int w, int h) {
      return mx >= x && mx < x + w && my >= y && my < y + h;
   }

   private static int argb(int a, int r, int g, int b) {
      return Mth.clamp(a, 0, 255) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static class SimpleField {
      String text = "";
      final int max;
      private long blinkBase = System.currentTimeMillis();

      SimpleField(int max) {
         this.max = max;
      }

      void type(int codepoint) {
         if (this.text.length() < this.max) {
            this.text = this.text + new String(Character.toChars(codepoint));
            this.blinkBase = System.currentTimeMillis();
         }
      }

      void append(String s) {
         if (s != null && !s.isEmpty()) {
            int room = this.max - this.text.length();
            if (room > 0) {
               this.text = this.text + (s.length() > room ? s.substring(0, room) : s);
               this.blinkBase = System.currentTimeMillis();
            }
         }
      }

      void backspace() {
         if (!this.text.isEmpty()) {
            this.text = this.text.substring(0, this.text.length() - 1);
            this.blinkBase = System.currentTimeMillis();
         }
      }

      boolean cursorVisible() {
         return (System.currentTimeMillis() - this.blinkBase) % 1000L < 530L;
      }
   }
}
