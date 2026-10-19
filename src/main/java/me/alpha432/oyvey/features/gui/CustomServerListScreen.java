package me.alpha432.oyvey.features.gui;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class CustomServerListScreen extends ClientMenuScreen {
   private static final int ROW_H = 44;
   private ServerList servers;
   private ServerStatusPinger pinger;
   private ExecutorService pingExecutor;
   private long pingGeneration;
   private final Map<ServerData, FaviconTexture> favicons = new HashMap<>();
   private final Set<ServerData> hasIcon = new HashSet<>();
   private final Map<ServerData, byte[]> uploadedBytes = new HashMap<>();
   private int selectedIndex = -1, scroll, rows, listX, listY, listW;
   private MenuField nameField, ipField;
   private Button connectButton, editButton, deleteButton;
   private String status = "";

   public CustomServerListScreen() { super("Multiplayer"); }

   @Override protected void init() {
      String oldName = nameField == null ? "" : nameField.getValue();
      String oldIp = ipField == null ? "" : ipField.getValue();
      layout(640, 380);
      listX = px + 14; listY = py + 65; listW = pw - 28;
      rows = Math.max(1, (ph - 159) / ROW_H);
      if (servers == null) { servers = new ServerList(minecraft); servers.load(); }
      closePings();
      pingExecutor = Executors.newFixedThreadPool(4, task -> {
         Thread t = new Thread(task, "srclient-server-ping"); t.setDaemon(true); return t;
      });
      button("Accounts", px + pw - 144, py + 11, 68, 22, () -> minecraft.setScreen(new AccountManagerScreen(this)));
      button("Refresh", px + pw - 70, py + 11, 56, 22, this::pingAll);
      if (FabricLoader.getInstance().isModLoaded("viafabricplus"))
         button("Version", px + pw - 78, py + 35, 64, 18, this::openViaFabricPlus);
      int fieldY = py + ph - 69, nameW = (listW - 8) / 3;
      nameField = field("Server name", listX, fieldY, nameW, 24, 64, false);
      ipField = field("Address (host:port)", listX + nameW + 8, fieldY, listW - nameW - 8, 24, 255, false);
      nameField.setValue(oldName); ipField.setValue(oldIp);
      int gap = 5, bw = (listW - 4 * gap) / 5, by = py + ph - 34;
      connectButton = button("Join", listX, by, bw, 23, this::connectForm);
      button("Add", listX + (bw + gap), by, bw, 23, this::saveForm);
      editButton = button("Save edit", listX + 2 * (bw + gap), by, bw, 23, this::editSelected);
      deleteButton = button("Delete", listX + 3 * (bw + gap), by, bw, 23, this::deleteSelected);
      int last = listX + 4 * (bw + gap);
      button("Back", last, by, listX + listW - last, 23, this::onClose);
      pingAll();
   }

   @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      if (pinger != null) pinger.tick();
      background(g, "Multiplayer", "Playing as " + currentAccount());
      text(g, servers.size() + " saved servers", listX, listY - 10, MUTED, false);
      scroll = Mth.clamp(scroll, 0, Math.max(0, servers.size() - rows));
      for (int row = 0; row < rows && scroll + row < servers.size(); row++) {
         int index = scroll + row, y = listY + row * ROW_H;
         ServerData data = servers.get(index);
         boolean hover = hit(mx, my, listX, y, listW, ROW_H - 3);
         RenderUtil.roundedRect(g, listX, y, listW, ROW_H - 3, 5,
            index == selectedIndex ? 0xFF24374F : hover ? 0xFF1B2A40 : 0xFF0D1725);
         if (index == selectedIndex) g.fill(listX, y + 6, listX + 2, y + ROW_H - 9, accent);
         FaviconTexture icon = favicons.get(data);
         byte[] bytes = data.getIconBytes();
         if (icon != null && bytes != uploadedBytes.get(data)) {
            if (bytes != null) uploadedBytes.put(data, bytes); else uploadedBytes.remove(data);
            uploadFavicon(data);
         }
         if (icon != null && hasIcon.contains(data)) {
            g.blit(RenderPipelines.GUI_TEXTURED, icon.textureLocation(), listX + 8, y + 8, 0F, 0F, 24, 24, 24, 24);
         } else {
            RenderUtil.roundedRect(g, listX + 8, y + 8, 24, 24, 4, 0xFF253B55);
            text(g, "SR", listX + 12, y + 14, accent, true);
         }
         String ping = pingText(data);
         int pingWidth = font.width(styled(ping, false));
         int contentWidth = listW - 57 - pingWidth;
         text(g, fit(stripCodes(data.name), contentWidth), listX + 41, y + 4, TEXT, true);
         text(g, fit(data.ip == null ? "" : data.ip, listW - 54), listX + 41, y + 23, MUTED, false);
         text(g, ping, listX + listW - pingWidth - 8, y + 4, pingColor(data), false);
      }
      if (servers.size() == 0) {
         RenderUtil.roundedRect(g, listX, listY, listW, rows * ROW_H - 3, 5, 0xFF0D1725);
         text(g, "Your server list is empty.", listX + 12, listY + 8, TEXT, true);
         text(g, "Enter an address below, then Add or Join.", listX + 12, listY + 26, MUTED, false);
      }
      if (servers.size() > rows) {
         int height = rows * ROW_H - 3, thumb = Math.max(12, height * rows / servers.size());
         int y = listY + (height - thumb) * scroll / (servers.size() - rows);
         g.fill(listX + listW - 3, y, listX + listW - 1, y + thumb, accent);
      }
      String hint = status.isEmpty() ? "Double-click to join / scroll for more servers" : status;
      text(g, fit(hint, listW), listX, py + ph - 84, MUTED, false);
      boolean valid = selectedIndex >= 0 && selectedIndex < servers.size();
      editButton.active = deleteButton.active = valid;
      connectButton.active = !ipField.getValue().isBlank();
      super.extractRenderState(g, mx, my, delta);
   }

   @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
      if (event.button() == 0 && hit(event.x(), event.y(), listX, listY, listW, rows * ROW_H - 3)) {
         int index = scroll + (int)((event.y() - listY) / ROW_H);
         if (index < servers.size()) {
            if (doubled && index == selectedIndex) connect(servers.get(index)); else select(index);
            return true;
         }
      }
      return super.mouseClicked(event, doubled);
   }

   @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
      if (!hit(x, y, listX, listY, listW, rows * ROW_H)) return super.mouseScrolled(x, y, dx, dy);
      scroll = Mth.clamp(scroll - (int)Math.signum(dy), 0, Math.max(0, servers.size() - rows));
      return true;
   }

   @Override public boolean keyPressed(KeyEvent event) {
      if (event.input() == GLFW.GLFW_KEY_ENTER || event.input() == GLFW.GLFW_KEY_KP_ENTER) {
         if (nameField.isFocused() || ipField.isFocused()) { connectForm(); return true; }
      }
      if (event.input() == GLFW.GLFW_KEY_F5) { pingAll(); return true; }
      return super.keyPressed(event);
   }

   private void select(int index) {
      selectedIndex = index;
      ServerData data = servers.get(index);
      nameField.setValue(stripCodes(data.name));
      ipField.setValue(data.ip == null ? "" : data.ip);
      status = "Selected " + stripCodes(data.name);
   }

   private ServerData form() {
      String ip = ipField.getValue().trim();
      if (ip.isEmpty() || !ServerAddress.isValidAddress(ip)) { status = "Enter a valid server address."; return null; }
      String name = nameField.getValue().trim();
      return new ServerData(name.isEmpty() ? ip : name, ip, Type.OTHER);
   }

   private void saveForm() {
      ServerData data = form(); if (data == null) return;
      servers.add(data, false); servers.save();
      selectedIndex = servers.size() - 1;
      scroll = Math.max(0, selectedIndex - rows + 1);
      pingOne(data); status = "Added " + stripCodes(data.name);
   }

   private void editSelected() {
      if (selectedIndex < 0 || selectedIndex >= servers.size()) return;
      ServerData data = form(); if (data == null) return;
      ServerData old = servers.get(selectedIndex);
      closeIcon(old);
      old.name = data.name; old.ip = data.ip;
      servers.save(); pingOne(old); status = "Saved changes.";
   }

   private void deleteSelected() {
      if (selectedIndex < 0 || selectedIndex >= servers.size()) return;
      ServerData old = servers.get(selectedIndex);
      closeIcon(old); servers.remove(old); servers.save(); selectedIndex = -1;
      nameField.setValue(""); ipField.setValue(""); status = "Server removed.";
   }

   private void connectForm() {
      ServerData data = form(); if (data == null) return;
      if (selectedIndex >= 0 && selectedIndex < servers.size()) {
         ServerData selected = servers.get(selectedIndex);
         if (data.ip.equals(selected.ip)) data = selected;
      }
      connect(data);
   }

   private void closeIcon(ServerData data) {
      FaviconTexture icon = favicons.remove(data);
      if (icon != null) icon.close();
      hasIcon.remove(data); uploadedBytes.remove(data);
   }

   private void connect(ServerData data) {
      ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(data.ip), data, false, null);
   }

   private String currentAccount() { return minecraft.getUser().getName(); }

   private void openViaFabricPlus() {
      if (!me.alpha432.oyvey.util.ViaProtocol.openSelector(this))
         status = "Could not open the installed ViaFabricPlus version selector.";
   }

   private void closePings() {
      pingGeneration++;
      if (pinger != null) pinger.removeAll();
      pinger = null;
      if (pingExecutor != null) pingExecutor.shutdownNow();
      pingExecutor = null;
   }

   private void pingAll() {
      if (servers == null || pingExecutor == null || pingExecutor.isShutdown()) return;
      if (pinger != null) pinger.removeAll();
      pinger = new ServerStatusPinger(); pingGeneration++;
      for (int i = 0; i < servers.size(); i++) pingOne(servers.get(i));
   }

   private void pingOne(ServerData data) {
      data.ping = -2; data.setIconBytes(null);
      hasIcon.remove(data); uploadedBytes.remove(data);
      favicons.computeIfAbsent(data, s -> FaviconTexture.forServer(minecraft.getTextureManager(), s.ip));
      ServerStatusPinger active = pinger;
      long generation = pingGeneration;
      if (active == null || pingExecutor == null || pingExecutor.isShutdown()) return;
      try {
         pingExecutor.execute(() -> {
            try { active.pingServer(data, () -> { }, () -> { }, EventLoopGroupHolder.remote(false)); }
            catch (Exception ignored) { data.ping = -1; }
            finally { if (generation != pingGeneration || active != pinger) active.removeAll(); }
         });
      } catch (RejectedExecutionException ignored) { }
   }

   private void uploadFavicon(ServerData data) {
      FaviconTexture icon = favicons.get(data);
      if (icon == null) return;
      byte[] bytes = data.getIconBytes();
      if (bytes == null) { icon.clear(); hasIcon.remove(data); return; }
      try { icon.upload(NativeImage.read(bytes)); hasIcon.add(data); }
      catch (Exception ignored) { icon.clear(); hasIcon.remove(data); }
   }

   private static String pingText(ServerData data) { return data.ping == -2 ? "Pinging…" : data.ping < 0 ? "Offline" : data.ping + " ms"; }
   private int pingColor(ServerData data) {
      return data.ping < 0 ? MUTED : data.ping < 100 ? 0xFF8CDBAA : data.ping < 250 ? 0xFFE7CE89 : 0xFFF49D9D;
   }
   private static String stripCodes(String value) { return value == null ? "" : value.replaceAll("\u00a7.", ""); }
   @Override public void onClose() { minecraft.setScreen(new CustomMainMenuScreen()); }
   @Override public void removed() {
      closePings();
      for (FaviconTexture icon : favicons.values()) icon.close();
      favicons.clear(); hasIcon.clear(); uploadedBytes.clear();
   }
}
