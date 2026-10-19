package me.alpha432.oyvey.features.gui;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.auth.AuthClient;
import me.alpha432.oyvey.auth.AuthManager;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/** SR account sign-in using the same controls and typography as the client menus. */
public class LoginScreen extends ClientMenuScreen {
   public static boolean authenticated;
   public static boolean offlineMode;
   public static boolean shownThisSession;
   public static String loggedInUser = "";

   private MenuField userField, passField;
   private Button primary, switchMode, remember;
   private boolean registerMode, refreshAttempted, resumeReady, compact;
   private String resumeUser = "", error = "";
   private State state = State.IDLE;
   private int generation, fieldX, fieldWidth, userY, passwordY, rememberY, statusY;

   public LoginScreen() { super("SR Client sign-in"); }
   private static AuthManager am() { return OyVey.authManager; }

   @Override protected void init() {
      String username = userField == null ? "" : userField.getValue();
      String password = passField == null ? "" : passField.getValue();
      layout(340, 292);
      compact = ph < 270;
      fieldX = px + 20;
      fieldWidth = pw - 40;
      userY = py + (compact ? 58 : 92);
      passwordY = userY + (compact ? 28 : 54);
      int fieldHeight = compact ? 22 : 26;
      userField = field("Username", fieldX, userY, fieldWidth, fieldHeight, 64, false);
      passField = field("Password", fieldX, passwordY, fieldWidth, fieldHeight, 64, true);
      userField.setValue(username);
      passField.setValue(password);
      rememberY = passwordY + fieldHeight + (compact ? 8 : 10);
      int secondaryWidth = (fieldWidth - 10) / 2;
      remember = button("Remember me", fieldX, rememberY, secondaryWidth, 20, () -> {
         if (am() != null) {
            am().setRememberMe(!am().isRememberMe());
            OyVey.configManager.save();
            updateControls();
         }
      });
      switchMode = button("Create account", fieldX + secondaryWidth + 10, rememberY,
         fieldWidth - secondaryWidth - 10, 20, this::switchMode);
      int primaryY = rememberY + (compact ? 28 : 30);
      primary = button("Sign in", fieldX, primaryY, fieldWidth, compact ? 24 : 30, this::primaryAction);
      statusY = primaryY + (compact ? 34 : 42);
      updateControls();
      setInitialFocus(userField);

      if (!refreshAttempted && am() != null && am().hasRememberedSession()) {
         refreshAttempted = true;
         userField.setValue(am().getUsername() == null ? "" : am().getUsername());
         refreshSession(am().getRefreshToken());
      }
   }

   private void updateControls() {
      boolean busy = state == State.LOADING;
      userField.visible = passField.visible = !resumeReady;
      userField.setEditable(!busy);
      passField.setEditable(!busy);
      primary.active = switchMode.active = !busy;
      remember.active = !busy && am() != null;
      boolean remembered = am() != null && am().isRememberMe();
      remember.setMessage(styled(fit("Remember: " + (remembered ? "On" : "Off"), remember.getWidth() - 12), false));
      String action = busy ? "Please wait…" : state == State.OFFLINE ? "Play offline"
         : resumeReady ? "Continue as " + resumeUser : registerMode ? "Create account" : "Sign in";
      primary.setMessage(styled(fit(action, fieldWidth - 20), true));
      switchMode.setMessage(styled(fit(resumeReady ? "Change account"
         : registerMode ? "Back to sign in" : "Create account", switchMode.getWidth() - 12), false));
   }

   @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      String heading = resumeReady ? "Welcome back" : registerMode ? "Create your account" : "Sign in to your account";
      background(g, "", "");
      // Compact card and centered heading keep the complete form balanced.
      int titleY = py + (compact ? 12 : 20);
      String brand = "SR Client";
      int brandWidth = font.width(styled(brand, true));
      int versionWidth = font.width(styled("3.0", true));
      int brandX = px + (pw - brandWidth - 8 - versionWidth) / 2;
      text(g, brand, brandX, titleY, TEXT, true);
      text(g, "3.0", brandX + brandWidth + 8, titleY, accent, true);
      centered(g, heading, py + (compact ? 30 : 40), MUTED);
      if (resumeReady) {
         int cardHeight = passwordY + (compact ? 22 : 26) - userY;
         RenderUtil.roundedRect(g, fieldX, userY, fieldWidth, cardHeight, 5, 0xFF0C1624);
         RenderUtil.roundedOutline(g, fieldX, userY, fieldWidth, cardHeight, 5, 0.75F, 0x454D6688);
         int avatarSize = compact ? 30 : 38;
         int avatarX = fieldX + 12, avatarY = userY + (cardHeight - avatarSize) / 2;
         RenderUtil.roundedRect(g, avatarX, avatarY, avatarSize, avatarSize, 8, 0xFF243A52);
         String initial = resumeUser.isEmpty() ? "S" : resumeUser.substring(0, resumeUser.offsetByCodePoints(0, 1)).toUpperCase(java.util.Locale.ROOT);
         var letter = styled(initial, true);
         g.text(font, letter, avatarX + (avatarSize - font.width(letter)) / 2, avatarY + (avatarSize - 11) / 2, accent, false);
         int nameX = avatarX + avatarSize + 12;
         text(g, fit(resumeUser, fieldX + fieldWidth - nameX - 10), nameX, userY + cardHeight / 2 - 12, TEXT, true);
         text(g, "Session restored", nameX, userY + cardHeight / 2 + 6, MUTED, false);
      } else if (!compact) {
         text(g, "Username", fieldX, userY - 16, MUTED, false);
         text(g, "Password", fieldX, passwordY - 16, MUTED, false);
      }
      updateControls();
      super.extractRenderState(g, mx, my, delta);
      String message = switch (state) {
         case LOADING -> refreshAttempted && !registerMode && passField.getValue().isEmpty()
            ? "Restoring your session…" : registerMode ? "Creating your account…" : "Signing you in…";
         case ERROR -> error;
         case OFFLINE -> "The sign-in server is unavailable. You can play offline.";
         case IDLE -> resumeReady ? "Your saved account is ready to use." : "";
      };
      int color = state == State.ERROR ? 0xFFFFAAA6 : state == State.OFFLINE ? 0xFFE7CE89 : MUTED;
      if (!message.isEmpty()) {
         int availableLines = Math.max(1, Math.min(2, (py + ph - statusY - 8) / 13));
         if (availableLines == 1) centered(g, fit(message, fieldWidth), statusY, color);
         else {
            var lines = font.split(styled(message, false), fieldWidth);
            for (int i = 0; i < Math.min(availableLines, lines.size()); i++)
               g.text(font, lines.get(i), px + (pw - font.width(lines.get(i))) / 2, statusY + i * 13, color, false);
         }
      }
   }

   private void centered(GuiGraphicsExtractor g, String value, int y, int color) {
      var label = styled(value, false);
      g.text(font, label, px + (pw - font.width(label)) / 2, y, color, false);
   }

   @Override public boolean keyPressed(KeyEvent event) {
      if ((event.input() == GLFW.GLFW_KEY_ENTER || event.input() == GLFW.GLFW_KEY_KP_ENTER)
         && (userField.isFocused() || passField.isFocused() || getFocused() == null)) {
         primaryAction();
         return true;
      }
      return super.keyPressed(event);
   }

   private void switchMode() {
      if (state == State.LOADING) return;
      if (resumeReady) {
         resumeReady = false;
         authenticated = false;
         resumeUser = "";
         userField.setValue("");
      } else registerMode = !registerMode;
      passField.setValue("");
      error = "";
      state = State.IDLE;
      updateControls();
      setFocused(userField);
   }

   private void primaryAction() {
      if (state == State.LOADING) return;
      if (state == State.OFFLINE) {
         offlineMode = true;
         proceed();
      } else if (resumeReady) {
         authenticated = true;
         offlineMode = false;
         loggedInUser = resumeUser;
         proceed();
      } else submit();
   }

   private void refreshSession(String refreshToken) {
      state = State.LOADING;
      updateControls();
      int attempt = ++generation;
      Thread worker = new Thread(() -> {
         AuthClient.Result result = AuthClient.refresh(refreshToken);
         minecraft.execute(() -> {
            if (attempt != generation) return;
            if (result.ok() && result.body() != null && result.body().has("access_token")) {
               am().applySession(result.body(), true);
               OyVey.configManager.save();
               resumeUser = am().getUsername() == null ? userField.getValue() : am().getUsername();
               resumeReady = true;
               state = State.IDLE;
               updateControls();
               setFocused(primary);
            } else {
               am().logout();
               OyVey.configManager.save();
               state = result.status() == 0 ? State.OFFLINE : State.ERROR;
               error = "Your saved session expired. Please sign in again.";
               updateControls();
               setFocused(passField);
            }
         });
      }, "auth-resume");
      worker.setDaemon(true);
      worker.start();
   }

   private void submit() {
      String user = userField.getValue().trim(), password = passField.getValue();
      if (user.isEmpty() || password.isEmpty()) {
         state = State.ERROR;
         error = "Enter your username and password.";
         updateControls();
         setFocused(user.isEmpty() ? userField : passField);
         return;
      }
      state = State.LOADING;
      error = "";
      updateControls();
      boolean createAccount = registerMode;
      boolean rememberAccount = am() == null || am().isRememberMe();
      int attempt = ++generation;
      Thread worker = new Thread(() -> {
         AuthClient.Result result;
         if (createAccount) {
            result = AuthClient.register(user, password);
            if (result.ok()) result = AuthClient.login(user, password);
         } else result = AuthClient.login(user, password);
         AuthClient.Result completed = result;
         minecraft.execute(() -> {
            if (attempt == generation) handleResult(completed, user, rememberAccount);
         });
      }, "auth-submit");
      worker.setDaemon(true);
      worker.start();
   }

   private void handleResult(AuthClient.Result result, String user, boolean rememberAccount) {
      if (result.status() == 0) state = State.OFFLINE;
      else if (result.ok() && result.body() != null && result.body().has("access_token")) {
         if (am() != null) {
            am().applySession(result.body(), rememberAccount);
            OyVey.configManager.save();
         }
         authenticated = true;
         offlineMode = false;
         loggedInUser = user;
         proceed();
         return;
      } else {
         state = State.ERROR;
         error = result.error() == null ? "Sign-in failed. Please try again." : result.error();
      }
      updateControls();
   }

   private void proceed() {
      passField.setValue("");
      minecraft.setScreen(new TitleScreen());
   }

   @Override public void removed() {
      generation++;
      if (passField != null) passField.setValue("");
   }
   @Override public boolean shouldCloseOnEsc() { return false; }
   private enum State { IDLE, LOADING, ERROR, OFFLINE }
}
