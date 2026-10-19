package me.alpha432.oyvey.features.gui;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.auth.MicrosoftAuth;
import me.alpha432.oyvey.auth.MinecraftTokenLogin;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class AccountManagerScreen extends ClientMenuScreen {
   private final Screen parent;
   private MenuField username, token;
   private Button login, microsoft, use, remove, offlineTab, tokenTab;
   private boolean tokenMode;
   private boolean busy;
   private int generation, scroll, selected = -1;
   private int listX, listY, listW, rows, rightX, rightW;
   private String status = "Choose an account or sign in.";
   private boolean success = true;

   public AccountManagerScreen() { this(new CustomMainMenuScreen()); }
   public AccountManagerScreen(Screen parent) { super("Accounts"); this.parent = parent; }

   @Override protected void init() {
      String previousName = username == null ? "" : username.getValue();
      String previousToken = token == null ? "" : token.getValue();
      layout(540, 332);
      listX = px + 14; listY = py + 65;
      listW = (pw - 42) / 2; rightX = listX + listW + 14; rightW = pw - 28 - listW - 14;
      rows = Math.max(1, (ph - 117) / 39);
      int half = (rightW - 6) / 2;
      offlineTab = button("Username", rightX, listY, half, 22, () -> setMode(false));
      tokenTab = button("Token", rightX + half + 6, listY, rightW - half - 6, 22, () -> setMode(true));
      username = field("Offline username", rightX, listY + 28, rightW, 22, 16, false);
      username.setValue(previousName);
      token = field("Paste Minecraft access token", rightX, listY + 28, rightW, 22, 16384, true);
      token.setValue(previousToken);
      login = button("Add & use", rightX, listY + 56, rightW, 22, this::submit);
      microsoft = button("Microsoft sign-in", rightX, listY + 84, rightW, 22, this::startMicrosoftLogin);
      use = button("Use selected", listX, py + ph - 44, (listW - 6) / 2, 24, this::loginSelected);
      remove = button("Remove", listX + (listW - 6) / 2 + 6, py + ph - 44,
         listW - (listW - 6) / 2 - 6, 24, this::removeSelected);
      button("Back", rightX + Math.max(0, rightW - 70), py + ph - 44, Math.min(70, rightW), 24, this::onClose);
      setMode(tokenMode);
      updateButtons();
   }

   private void setMode(boolean mode) {
      if (tokenMode != mode && token != null) token.setValue("");
      tokenMode = mode;
      username.visible = !mode;
      token.visible = mode;
      login.setMessage(styled(mode ? "Log in with token" : "Add & use", false));
      offlineTab.active = !busy && mode;
      tokenTab.active = !busy && !mode;
      setFocused(mode ? token : username);
   }

   @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      background(g, "Accounts", "Playing as " + OyVey.accountManager.getCurrentName());
      List<String> accounts = OyVey.accountManager.getAccounts();
      scroll = Mth.clamp(scroll, 0, Math.max(0, accounts.size() - rows));
      RenderUtil.roundedRect(g, listX, listY, listW, rows * 39 - 3, 5, 0xFF0C1420);
      for (int row = 0; row < rows && scroll + row < accounts.size(); row++) {
         int index = scroll + row, y = listY + row * 39;
         String name = accounts.get(index);
         boolean current = name.equals(OyVey.accountManager.getCurrentName());
         boolean hover = hit(mx, my, listX, y, listW, 36);
         if (index == selected || hover) RenderUtil.roundedRect(g, listX, y, listW, 36, 4,
            index == selected ? 0xFF253750 : 0xFF192538);
         if (current) g.fill(listX, y + 5, listX + 2, y + 31, accent);
         text(g, fit(name, listW - 22), listX + 9, y + 4, current ? accent : TEXT, true);
         String type = OyVey.accountManager.isToken(name) ? "Token / session only"
            : OyVey.accountManager.isMicrosoft(name) ? "Microsoft" : "Offline";
         text(g, fit(type + (current ? " / active" : ""), listW - 18), listX + 9, y + 20, MUTED, false);
      }
      if (accounts.isEmpty()) text(g, "No saved accounts", listX + 8, listY + 12, MUTED, false);
      if (accounts.size() > rows) {
         int h = rows * 39 - 3, thumb = Math.max(12, h * rows / accounts.size());
         int y = listY + (h - thumb) * scroll / (accounts.size() - rows);
         g.fill(listX + listW - 3, y, listX + listW - 1, y + thumb, accent);
      }
      if (ph >= 300) {
         text(g, fit(tokenMode ? "Minecraft Java access tokens only." : "Offline accounts work on offline servers.", rightW),
            rightX, listY + 130, MUTED, false);
         text(g, fit(tokenMode ? "Token expires; it is not saved to disk." : "Use Microsoft for authenticated servers.", rightW),
            rightX, listY + 147, MUTED, false);
      }
      updateButtons();
      super.extractRenderState(g, mx, my, delta);
      text(g, fit(status, pw - 28), px + 14, py + ph - 15, success ? MUTED : 0xFFFFA6A6, false);
   }

   private void updateButtons() {
      boolean valid = selected >= 0 && selected < OyVey.accountManager.getAccounts().size();
      use.active = remove.active = !busy && valid;
      login.active = microsoft.active = !busy;
      offlineTab.active = !busy && tokenMode;
      tokenTab.active = !busy && !tokenMode;
      username.setEditable(!busy); token.setEditable(!busy);
   }

   @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
      if (event.button() == 0 && hit(event.x(), event.y(), listX, listY, listW, rows * 39 - 3)) {
         int index = scroll + (int)((event.y() - listY) / 39);
         if (index < OyVey.accountManager.getAccounts().size()) {
            selected = index;
            if (doubled && !busy) loginSelected();
            return true;
         }
      }
      return super.mouseClicked(event, doubled);
   }

   @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
      if (!hit(x, y, listX, listY, listW, rows * 39)) return super.mouseScrolled(x, y, dx, dy);
      scroll = Mth.clamp(scroll - (int)Math.signum(dy), 0,
         Math.max(0, OyVey.accountManager.getAccounts().size() - rows));
      return true;
   }

   @Override public boolean keyPressed(KeyEvent event) {
      if ((event.input() == GLFW.GLFW_KEY_ENTER || event.input() == GLFW.GLFW_KEY_KP_ENTER)
         && (username.isFocused() || token.isFocused())) {
         if (!busy) submit();
         return true;
      }
      return super.keyPressed(event);
   }

   private void submit() {
      if (busy) return;
      if (tokenMode) {
         String value = token.getValue().trim();
         token.setValue("");
         tokenLogin(value);
      } else {
         String name = username.getValue().trim();
         if (!name.matches("[A-Za-z0-9_]{1,16}")) { message("Use 1–16 letters, numbers or underscores.", false); return; }
         if (OyVey.accountManager.isMicrosoft(name) || OyVey.accountManager.isToken(name)) {
            message("Select the saved account to sign in.", false); return;
         }
         if (OyVey.accountManager.switchAccount(name)) {
            OyVey.accountManager.addAccount(name);
            OyVey.configManager.save();
            username.setValue("");
            message("Playing as " + name, true);
         } else message("Could not switch account.", false);
      }
   }

   private void loginSelected() {
      List<String> accounts = OyVey.accountManager.getAccounts();
      if (busy || selected < 0 || selected >= accounts.size()) return;
      String name = accounts.get(selected);
      if (name.equals(OyVey.accountManager.getCurrentName())) { message(name + " is already active.", true); return; }
      if (OyVey.accountManager.isToken(name)) {
         String saved = OyVey.accountManager.getToken(name);
         if (saved == null || saved.isBlank()) { setMode(true); message("Paste a fresh token for " + name + ".", false); }
         else tokenLogin(saved);
      } else if (OyVey.accountManager.isMicrosoft(name)) {
         String session = OyVey.accountManager.getMicrosoftSession(name);
         if (session == null || session.isBlank()) { message("Use Microsoft sign-in for this account.", false); return; }
         busy = true; int attempt = ++generation;
         message("Refreshing Microsoft session…", true);
         MicrosoftAuth.refreshAndLogin(session,
            (n, id, access, refreshed) -> minecraft.execute(() -> complete(attempt, n, id, access, refreshed)),
            error -> minecraft.execute(() -> fail(attempt, "Session expired. Use Microsoft sign-in again.")));
      } else message(OyVey.accountManager.switchAccount(name) ? "Playing as " + name : "Account switch failed.",
         name.equals(OyVey.accountManager.getCurrentName()));
   }

   private void removeSelected() {
      List<String> accounts = OyVey.accountManager.getAccounts();
      if (busy || selected < 0 || selected >= accounts.size()) return;
      String name = accounts.get(selected);
      OyVey.accountManager.removeAccount(name);
      OyVey.configManager.save();
      selected = -1;
      message("Removed " + name + " from the list.", true);
   }

   private void tokenLogin(String value) {
      if (value.isBlank()) { message("Paste a Minecraft access token first.", false); return; }
      busy = true; int attempt = ++generation;
      message("Validating Minecraft profile…", true);
      String normalized;
      try { normalized = MinecraftTokenLogin.normalize(value); }
      catch (IllegalArgumentException invalid) { fail(attempt, "Paste a valid Minecraft access token."); return; }
      MinecraftTokenLogin.login(normalized,
         profile -> minecraft.execute(() -> complete(attempt, profile.name(), profile.id(), normalized, null)),
         error -> minecraft.execute(() -> fail(attempt, error)));
   }

   private void startMicrosoftLogin() {
      if (busy) return;
      busy = true; int attempt = ++generation;
      message("Starting Microsoft sign-in…", true);
      MicrosoftAuth.deviceCodeLogin((code, uri, direct) -> minecraft.execute(() -> {
         if (attempt != generation) return;
         message("Code: " + code + " (copied). Complete sign-in in your browser.", true);
         minecraft.keyboardHandler.setClipboard(code);
         try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(uri)); }
         catch (Exception ignored) { message("Open " + uri + " and enter " + code, true); }
      }), (name, id, access, session) -> minecraft.execute(() -> complete(attempt, name, id, access, session)),
         error -> minecraft.execute(() -> fail(attempt, "Microsoft sign-in failed or was cancelled.")));
   }

   private void complete(int attempt, String name, UUID id, String access, String session) {
      if (attempt != generation) return;
      busy = false;
      if (!OyVey.accountManager.setOnlineSession(name, id, access)) { message("Could not install the Minecraft session.", false); return; }
      if (session == null) OyVey.accountManager.saveToken(name, access);
      else OyVey.accountManager.saveMicrosoftSession(name, session);
      OyVey.configManager.save();
      message("Signed in as " + name, true);
   }

   private void fail(int attempt, String error) {
      if (attempt != generation) return;
      busy = false; message(error, false);
   }

   private void message(String value, boolean ok) { status = value; success = ok; }
   @Override public void onClose() { minecraft.setScreen(parent); }
   @Override public void removed() {
      generation++;
      busy = false;
      if (token != null) token.setValue("");
   }
}
