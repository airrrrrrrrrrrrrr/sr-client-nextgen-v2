package me.alpha432.oyvey.features.gui;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.proxy.ProxyEntry;
import me.alpha432.oyvey.proxy.ProxyTester;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** Saved proxy list and native editor using the shared main-menu visuals. */
public class ProxyScreen extends ClientMenuScreen {
   private static final int ROW = 28;
   private MenuField hostField, portField, userField, passField;
   private Button typeButton, saveButton, testButton, activateButton, deleteButton;
   private ProxyEntry.Type editType = ProxyEntry.Type.SOCKS5;
   private int selected = -1, scroll, listX, listY, listW, listH, rows, formX, formW, statusY, generation;
   private boolean compact, listMode, testing;
   private String status = "";
   private int statusColor = MUTED;

   public ProxyScreen() { super("Proxies"); }

   @Override protected void init() {
      String host = hostField == null ? "" : hostField.getValue();
      String port = portField == null ? "" : portField.getValue();
      String user = userField == null ? "" : userField.getValue();
      String password = passField == null ? "" : passField.getValue();
      layout(650, 370);
      compact = pw < 560 || ph < 310;
      if (!compact) listMode = false;
      listX = px + 14; listY = py + 66;
      listW = compact ? pw - 28 : 220;
      listH = compact ? ph - 110 : ph - 116;
      rows = Math.max(1, listH / ROW);
      if (compact && listMode) {
         button("Back to editor", px + 14, py + ph - 34, pw - 28, 22, () -> { listMode = false; rebuild(); });
         return;
      }
      formX = compact ? px + 14 : px + 250;
      formW = compact ? pw - 28 : pw - 264;
      int top = py + (compact ? 86 : 70);
      if (compact) button("Saved proxies (" + OyVey.proxyManager.getProxies().size() + ")", formX, py + 64, formW, 18,
         () -> { listMode = true; rebuild(); });
      typeButton = button("Type", formX, top, formW, compact ? 18 : 22, () -> {
         ProxyEntry.Type[] types = ProxyEntry.Type.values(); editType = types[(editType.ordinal() + 1) % types.length]; updateButtons();
      });
      int hostY = top + (compact ? 22 : 48), authY = top + (compact ? 44 : 100), fieldH = compact ? 18 : 24;
      int portWidth = compact ? 60 : 76;
      hostField = field("Host", formX, hostY, formW - portWidth - 8, fieldH, 256, false);
      portField = field("Port", formX + formW - portWidth, hostY, portWidth, fieldH, 5, false);
      portField.setResponder(value -> {
         String digits = value.replaceAll("[^0-9]", "");
         if (!value.equals(digits)) portField.setValue(digits);
      });
      int half = (formW - 8) / 2;
      userField = field("Username (optional)", formX, authY, half, fieldH, 128, false);
      passField = field("Password (optional)", formX + half + 8, authY, formW - half - 8, fieldH, 256, true);
      hostField.setValue(host); portField.setValue(port); userField.setValue(user); passField.setValue(password);
      int actionY = top + (compact ? 66 : 140);
      int unit = compact ? (formW - 18) / 4 : half;
      saveButton = button("Save", formX, actionY, unit, compact ? 20 : 26, this::saveForm);
      testButton = button("Test", formX + unit + (compact ? 6 : 8), actionY, unit, compact ? 20 : 26, this::runTest);
      int secondY = compact ? actionY : actionY + 36;
      int activateX = compact ? formX + 2 * (unit + 6) : formX;
      int deleteX = compact ? formX + 3 * (unit + 6) : formX + half + 8;
      activateButton = button(compact ? "Use" : "Set active", activateX, secondY, unit, compact ? 20 : 24, this::setActive);
      deleteButton = button("Delete", deleteX, secondY, unit, compact ? 20 : 24, this::deleteSelected);
      int footer = compact ? py + 178 : py + ph - 36;
      int newWidth = compact ? (formW - 96) / 2 : 80;
      button("New", px + 14, footer, newWidth, 20, this::newForm);
      button(compact ? "Disable" : "Disable proxy", px + 22 + newWidth, footer, compact ? newWidth : 132, 20, () -> {
         OyVey.proxyManager.clearActive(); OyVey.configManager.save(); message("Proxy disabled.", 0xFF88D9B0);
      });
      button("Back", px + pw - 94, footer, 80, 20, this::onClose);
      statusY = compact ? py + 202 : top + 214;
      updateButtons(); setInitialFocus(hostField);
   }

   private void rebuild() { clearWidgets(); init(); }
   private void updateButtons() {
      if (typeButton == null) return;
      typeButton.setMessage(styled("Type: " + editType.name(), false));
      saveButton.setMessage(styled(selected < 0 ? "Save" : "Update", false));
      testButton.active = !testing;
      testButton.setMessage(styled(testing ? "Testing…" : "Test", false));
      deleteButton.active = selected >= 0 && selected < OyVey.proxyManager.getProxies().size();
   }

   @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      var active = OyVey.proxyManager.getActive();
      background(g, "Proxies", active == null ? "Manage connections for your next server join." : "Active: " + active.getHost() + ":" + active.getPort());
      if (!compact || listMode) drawList(g, mx, my);
      if (!listMode && !compact) {
         text(g, "Host", hostField.getX(), hostField.getY() - 16, MUTED, false);
         text(g, "Port", portField.getX(), portField.getY() - 16, MUTED, false);
         text(g, "Username (optional)", userField.getX(), userField.getY() - 16, MUTED, false);
         text(g, "Password (optional)", passField.getX(), passField.getY() - 16, MUTED, false);
      }
      if (!status.isEmpty() && !listMode) {
         var lines = font.split(styled(status, false), formW);
         for (int i = 0; i < Math.min(compact ? 1 : 2, lines.size()); i++)
            g.text(font, lines.get(i), formX, statusY + i * 13, statusColor, false);
      }
      super.extractRenderState(g, mx, my, delta);
   }

   private void drawList(GuiGraphicsExtractor g, int mx, int my) {
      var proxies = OyVey.proxyManager.getProxies();
      scroll = Mth.clamp(scroll, 0, Math.max(0, proxies.size() - rows));
      RenderUtil.roundedRect(g, listX, listY, listW, listH, 5, 0xFF0C1624);
      if (proxies.isEmpty()) text(g, "No saved proxies", listX + 12, listY + 10, MUTED, false);
      for (int row = 0; row < rows && row + scroll < proxies.size(); row++) {
         int index = row + scroll, y = listY + row * ROW;
         var proxy = proxies.get(index);
         RenderUtil.roundedRect(g, listX + 1, y + 1, listW - 2, ROW - 3, 4,
            index == selected ? 0xFF24364D : hit(mx, my, listX, y, listW, ROW - 2) ? 0xFF1B293C : 0xFF101C2B);
         String address = proxy.getHost() + ":" + proxy.getPort();
         text(g, fit(address, listW - 66), listX + 10, y + 4, TEXT, false);
         text(g, proxy.getType().name(), listX + 10, y + 15, MUTED, false);
         if (index == OyVey.proxyManager.getActiveIndex()) text(g, "Active", listX + listW - 46, y + 9, 0xFF88D9B0, false);
      }
      if (proxies.size() > rows) {
         int thumb = Math.max(12, listH * rows / proxies.size());
         RenderUtil.roundedRect(g, listX + listW - 3, listY + (listH - thumb) * scroll / Math.max(1, proxies.size() - rows), 2, thumb, 1, accent);
      }
   }

   @Override public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      if ((!compact || listMode) && click.button() == 0 && hit(click.x(), click.y(), listX, listY, listW, rows * ROW)) {
         int index = scroll + (int)(click.y() - listY) / ROW;
         if (index < OyVey.proxyManager.getProxies().size()) {
            var entry = OyVey.proxyManager.getProxies().get(index);
            if (listMode) { listMode = false; rebuild(); }
            selected = index; editType = entry.getType();
            hostField.setValue(entry.getHost()); portField.setValue(Integer.toString(entry.getPort()));
            userField.setValue(entry.getUsername()); passField.setValue(entry.getPassword());
            generation++; testing = false; message("", MUTED); updateButtons();
         }
         return true;
      }
      return super.mouseClicked(click, doubled);
   }
   @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
      if ((!compact || listMode) && hit(x, y, listX, listY, listW, listH)) {
         scroll = Mth.clamp(scroll - (int)Math.signum(dy), 0, Math.max(0, OyVey.proxyManager.getProxies().size() - rows)); return true;
      }
      return super.mouseScrolled(x, y, dx, dy);
   }
   @Override public boolean keyPressed(KeyEvent key) {
      if (!listMode && (key.input() == GLFW.GLFW_KEY_ENTER || key.input() == GLFW.GLFW_KEY_KP_ENTER)
         && (hostField.isFocused() || portField.isFocused() || userField.isFocused() || passField.isFocused())) { saveForm(); return true; }
      return super.keyPressed(key);
   }

   private ProxyEntry formEntry() {
      int port;
      try { port = Integer.parseInt(portField.getValue()); } catch (NumberFormatException ignored) { port = 0; }
      return new ProxyEntry(editType, hostField.getValue(), port, userField.getValue(), passField.getValue());
   }
   private boolean saveForm() {
      ProxyEntry entry = formEntry();
      if (!entry.isValid()) { message("Enter a host and a port from 1 to 65535.", 0xFFFFAAA6); return false; }
      var proxies = OyVey.proxyManager.getProxies();
      if (selected >= 0 && selected < proxies.size()) proxies.set(selected, entry);
      else { OyVey.proxyManager.add(entry); selected = proxies.size() - 1; }
      OyVey.configManager.save(); message("Proxy saved.", 0xFF88D9B0); updateButtons(); return true;
   }
   private void setActive() {
      if (!saveForm()) return;
      OyVey.proxyManager.setActiveIndex(selected); OyVey.configManager.save();
      message("Active proxy set. Applies on your next server join.", 0xFF88D9B0);
   }
   private void deleteSelected() {
      if (selected < 0) return;
      OyVey.proxyManager.remove(selected); OyVey.configManager.save(); newForm(); message("Proxy deleted.", 0xFF88D9B0);
   }
   private void newForm() {
      generation++; testing = false; selected = -1; editType = ProxyEntry.Type.SOCKS5;
      hostField.setValue(""); portField.setValue(""); userField.setValue(""); passField.setValue("");
      message("", MUTED); updateButtons(); setFocused(hostField);
   }
   private void runTest() {
      ProxyEntry entry = formEntry();
      if (!entry.isValid()) { message("Enter a valid host and port before testing.", 0xFFFFAAA6); return; }
      testing = true; message("Testing connection…", MUTED); updateButtons();
      int attempt = ++generation;
      ProxyTester.test(entry, (ok, ms, detail) -> minecraft.execute(() -> {
         if (attempt != generation) return;
         testing = false; message(ok ? "Connected in " + ms + " ms." : "Connection failed. Check the proxy details.", ok ? 0xFF88D9B0 : 0xFFFFAAA6); updateButtons();
      }));
   }
   private void message(String value, int color) { status = value; statusColor = color; }
   @Override public void removed() { generation++; testing = false; }
   @Override public void onClose() { passField.setValue(""); minecraft.setScreen(new CustomMainMenuScreen()); }
}
