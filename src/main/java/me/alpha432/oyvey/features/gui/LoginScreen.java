package me.alpha432.oyvey.features.gui;

import java.awt.Color;
import java.util.Random;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.auth.AuthClient;
import me.alpha432.oyvey.auth.AuthManager;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class LoginScreen extends Screen {
   public static boolean authenticated = false;
   public static boolean offlineMode = false;
   public static boolean shownThisSession = false;
   public static String loggedInUser = "";
   private static final int STARS = 90;
   private final float[] sx = new float[90];
   private final float[] sy = new float[90];
   private final float[] svx = new float[90];
   private final float[] svy = new float[90];
   private final float[] sa = new float[90];
   private final LoginScreen.SimpleField userField = new LoginScreen.SimpleField(false);
   private final LoginScreen.SimpleField passField = new LoginScreen.SimpleField(true);
   private LoginScreen.SimpleField focused;
   private boolean registerMode = false;
   private boolean refreshAttempted = false;
   private volatile boolean resumeReady = false;
   private volatile String resumeUser = "";
   private volatile LoginScreen.State state = LoginScreen.State.IDLE;
   private volatile String errMsg = "";
   private long openTime;

   public LoginScreen() {
      super(Component.literal("login"));
   }

   private static AuthManager am() {
      return OyVey.authManager;
   }

   public void init() {
      this.openTime = System.currentTimeMillis();
      this.focused = this.userField;
      Random rng = new Random();

      for (int i = 0; i < 90; i++) {
         this.sx[i] = rng.nextFloat() * this.width;
         this.sy[i] = rng.nextFloat() * this.height;
         float angle = rng.nextFloat() * 6.2831855F;
         float spd = 0.04F + rng.nextFloat() * 0.14F;
         this.svx[i] = (float)Math.cos(angle) * spd;
         this.svy[i] = (float)Math.sin(angle) * spd;
         this.sa[i] = 0.25F + rng.nextFloat() * 0.55F;
      }

      if (!this.refreshAttempted && am() != null && am().hasRememberedSession()) {
         this.refreshAttempted = true;
         this.userField.text = am().getUsername() == null ? "" : am().getUsername();
         this.state = LoginScreen.State.LOADING;
         String rt = am().getRefreshToken();
         Thread t = new Thread(() -> {
            AuthClient.Result r = AuthClient.refresh(rt);
            Minecraft.getInstance().execute(() -> {
               if (r.ok() && r.body() != null) {
                  am().applySession(r.body(), true);
                  OyVey.configManager.save();
                  this.resumeReady = true;
                  this.resumeUser = am().getUsername();
               } else {
                  am().logout();
                  OyVey.configManager.save();
               }

               this.state = LoginScreen.State.IDLE;
            });
         }, "auth-resume");
         t.setDaemon(true);
         t.start();
      }
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      float age = (float)(System.currentTimeMillis() - this.openTime) / 1000.0F;
      float fadeIn = Mth.clamp(age / 0.6F, 0.0F, 1.0F);
      int accent = OyVey.colorManager.getColorAsInt();
      int aR = accent >> 16 & 0xFF;
      int aG = accent >> 8 & 0xFF;
      int aB = accent & 0xFF;
      g.fill(0, 0, this.width, this.height, -16316657);
      float pulse = (float)(Math.sin(age * 0.7) * 0.5 + 0.5);
      RenderUtil.verticalGradient(g, 0.0F, 0.0F, this.width, this.height * 0.5F, new Color(aR, aG, aB, (int)(12.0F + pulse * 16.0F)), new Color(0, 0, 0, 0));

      for (int i = 0; i < 90; i++) {
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

      String title = "SR CLIENT";
      float ts = 2.2F;
      int titleY = (int)(this.height * 0.22F);
      g.pose().pushMatrix();
      g.pose().scale(ts, ts);
      int tx = (int)((this.width / 2.0F - this.font.width(title) * ts / 2.0F) / ts);
      int ty = (int)((titleY - 6.0F) / ts);
      g.text(this.font, title, tx + 1, ty + 1, argb((int)(fadeIn * 120.0F), 0, 0, 0), false);
      g.text(this.font, title, tx, ty, argb((int)(fadeIn * 255.0F), aR, aG, aB), false);
      g.pose().popMatrix();
      int cx = this.width / 2 - 120;
      int cy = (int)(this.height * 0.38F);
      RenderUtil.dropShadow(g, cx, cy, 240, 178, 8.0F, 14.0F, (int)(fadeIn * 145.0F));
      RenderUtil.roundedRectGradient(
         g, cx, cy, 240, 178, 8.0F, argb((int)(fadeIn * 228.0F), 14, 14, 36), argb((int)(fadeIn * 218.0F), 7, 7, 22), true
      );
      RenderUtil.roundedOutline(g, cx, cy, 240, 178, 8.0F, 0.9F, argb((int)(fadeIn * 115.0F), aR, aG, aB));
      RenderUtil.roundedRect(g, cx + 4, cy + 8, 2, 162, 1, argb((int)(fadeIn * 220.0F), aR, aG, aB));
      int fx = cx + 20;
      int fy = cy + 32;
      int fy2 = fy + 18 + 22;
      int btnY = fy2 + 18 + 16;
      String heading = this.resumeReady ? "Welcome back" : (this.registerMode ? "Create account" : "Sign in");
      g.text(this.font, heading, fx, cy + 6, argb((int)(fadeIn * 200.0F), aR, aG, aB), false);
      this.drawLabel(g, "Username", fx, fy - 9, fadeIn, aR, aG, aB);
      this.drawField(g, this.userField, "username", fx, fy, 200, 18, fadeIn, aR, aG, aB);
      this.drawLabel(g, "Password", fx, fy2 - 9, fadeIn, aR, aG, aB);
      this.drawField(g, this.passField, "password", fx, fy2, 200, 18, fadeIn, aR, aG, aB);
      boolean btnHov = mx >= fx && mx < fx + 200 && my >= btnY && my < btnY + 20;
      this.drawButton(g, fx, btnY, 200, 20, btnHov, fadeIn, aR, aG, aB);

      String statusText = switch (this.state) {
         case LOADING -> spinner(age);
         case ERROR -> this.errMsg;
         case OFFLINE -> "can't reach auth server";
         default -> "";
      };
      if (!statusText.isEmpty()) {
         int sc = switch (this.state) {
            case ERROR -> argb((int)(fadeIn * 230.0F), 255, 90, 90);
            case OFFLINE -> argb((int)(fadeIn * 230.0F), 230, 140, 40);
            default -> argb((int)(fadeIn * 200.0F), 120, 140, 160);
         };
         g.centeredText(this.font, statusText, this.width / 2, btnY + 26, sc);
      }

      String modeLabel = this.resumeReady ? "use a different account" : (this.registerMode ? "have an account? sign in" : "no account? register");
      int modeY = btnY + 40;
      boolean modeHov = this.hovText(mx, my, this.width / 2, modeY, modeLabel);
      g.centeredText(this.font, modeLabel, this.width / 2, modeY, argb((int)(fadeIn * (modeHov ? 230 : 180)), aR, aG, aB));
      boolean rem = am() != null && am().isRememberMe();
      String remLabel = "remember me: " + (rem ? "on" : "off");
      int remY = btnY + 52;
      boolean remHov = this.hovText(mx, my, this.width / 2, remY, remLabel);
      g.centeredText(this.font, remLabel, this.width / 2, remY, argb((int)(fadeIn * (remHov ? 220 : 160)), rem ? 90 : 150, rem ? 230 : 160, rem ? 120 : 170));
      g.text(this.font, "v2.0", 4, this.height - 10, argb((int)(fadeIn * 200.0F), 74, 102, 136), false);
   }

   private void drawLabel(GuiGraphicsExtractor g, String text, int x, int y, float fadeIn, int aR, int aG, int aB) {
      g.text(this.font, text, x, y, argb((int)(fadeIn * 160.0F), aR, aG, aB), false);
   }

   private void drawField(GuiGraphicsExtractor g, LoginScreen.SimpleField f, String ph, int x, int y, int w, int h, float fadeIn, int aR, int aG, int aB) {
      boolean foc = this.focused == f;
      RenderUtil.roundedRect(g, x, y, w, h, 2, argb((int)(fadeIn * (foc ? 210 : 170)), foc ? 20 : 12, foc ? 20 : 12, foc ? 40 : 28));
      RenderUtil.roundedOutline(g, x, y, w, h, 3.0F, foc ? 0.9F : 0.55F, argb((int)(fadeIn * (foc ? 190 : 55)), aR, aG, aB));
      String shown = f.getDisplay() + (foc && f.cursorVisible() ? "|" : "");

      while (this.font.width(shown) > w - 8 && shown.length() > 0) {
         shown = shown.substring(1);
      }

      if (f.text.isEmpty() && !foc) {
         g.text(this.font, ph, x + 4, y + (h - 8) / 2, argb((int)(fadeIn * 80.0F), 170, 187, 204), false);
      } else {
         g.text(this.font, shown, x + 4, y + (h - 8) / 2, argb((int)(fadeIn * 230.0F), 221, 238, 255), false);
      }
   }

   private void drawButton(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hov, float fadeIn, int aR, int aG, int aB) {
      boolean loading = this.state == LoginScreen.State.LOADING;
      boolean offline = this.state == LoginScreen.State.OFFLINE;
      int bg = loading ? argb((int)(fadeIn * 130.0F), 16, 16, 37) : (hov ? argb((int)(fadeIn * 230.0F), 28, 28, 48) : argb((int)(fadeIn * 180.0F), 11, 11, 28));
      RenderUtil.roundedRect(g, x, y, w, h, 3, bg);
      RenderUtil.roundedOutline(g, x, y, w, h, 4.0F, 0.85F, argb((int)(fadeIn * (hov ? 220 : 95)), aR, aG, aB));
      String label;
      if (loading) {
         label = "working...";
      } else if (offline) {
         label = "Play Offline";
      } else if (this.resumeReady && this.passField.text.isEmpty()) {
         label = "Continue as " + this.resumeUser;
      } else {
         label = this.registerMode ? "Register" : "Login";
      }

      int lc = loading
         ? argb((int)(fadeIn * 150.0F), aR, aG, aB)
         : (hov ? argb((int)(fadeIn * 255.0F), aR, aG, aB) : argb((int)(fadeIn * 230.0F), 187, 204, 221));
      g.centeredText(this.font, label, x + w / 2, y + (h - 8) / 2, lc);
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      if (click.button() != 0) {
         return super.mouseClicked(click, doubled);
      }

      double mx = click.x();
      double my = click.y();
      int cx = this.width / 2 - 120;
      int cy = (int)(this.height * 0.38F);
      int fx = cx + 20;
      int fy = cy + 32;
      int fy2 = fy + 18 + 22;
      int btnY = fy2 + 18 + 16;
      if (in(mx, my, fx, fy, 200, 18)) {
         this.focused = this.userField;
         return true;
      }

      if (in(mx, my, fx, fy2, 200, 18)) {
         this.focused = this.passField;
         return true;
      }

      if (in(mx, my, fx, btnY, 200, 20)) {
         this.primaryAction();
         return true;
      }

      String modeLabel = this.resumeReady ? "use a different account" : (this.registerMode ? "have an account? sign in" : "no account? register");
      if (this.hovText(mx, my, this.width / 2, btnY + 40, modeLabel)) {
         if (this.resumeReady) {
            this.resumeReady = false;
            authenticated = false;
            this.userField.text = "";
            this.passField.text = "";
         } else {
            this.registerMode = !this.registerMode;
         }

         this.state = LoginScreen.State.IDLE;
         this.errMsg = "";
         return true;
      } else {
         if (am() != null) {
            String remLabel = "remember me: " + (am().isRememberMe() ? "on" : "off");
            if (this.hovText(mx, my, this.width / 2, btnY + 52, remLabel)) {
               am().setRememberMe(!am().isRememberMe());
               OyVey.configManager.save();
               return true;
            }
         }

         return super.mouseClicked(click, doubled);
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
            this.focused.clear();
            return true;
         }
      }

      if (key == 258) {
         this.focused = this.focused == this.userField ? this.passField : this.userField;
         return true;
      } else if (key == 259) {
         this.focused.backspace();
         return true;
      } else if (key == 257 || key == 335) {
         this.primaryAction();
         return true;
      } else {
         return key == 256 ? true : super.keyPressed(input);
      }
   }

   public boolean charTyped(CharacterEvent input) {
      this.focused.type(input.codepoint());
      return true;
   }

   private void primaryAction() {
      if (this.state == LoginScreen.State.OFFLINE) {
         offlineMode = true;
         this.proceed();
      } else if (this.resumeReady && this.passField.text.isEmpty()) {
         authenticated = true;
         loggedInUser = this.resumeUser;
         this.proceed();
      } else {
         this.submit();
      }
   }

   private void submit() {
      if (this.state != LoginScreen.State.LOADING) {
         String user = this.userField.text.trim();
         String pass = this.passField.text;
         if (!user.isEmpty() && !pass.isEmpty()) {
            this.state = LoginScreen.State.LOADING;
            this.errMsg = "";
            boolean remember = am() == null || am().isRememberMe();
            boolean reg = this.registerMode;
            Thread t = new Thread(() -> {
               AuthClient.Result r;
               if (reg) {
                  r = AuthClient.register(user, pass);
                  if (r.ok()) {
                     r = AuthClient.login(user, pass);
                  }
               } else {
                  r = AuthClient.login(user, pass);
               }

               AuthClient.Result res = r;
               Minecraft.getInstance().execute(() -> this.handleResult(res, user, remember));
            }, "auth-submit");
            t.setDaemon(true);
            t.start();
         } else {
            this.errMsg = "fill in both fields";
            this.state = LoginScreen.State.ERROR;
         }
      }
   }

   private void handleResult(AuthClient.Result res, String user, boolean remember) {
      if (res.status() == 0) {
         this.state = LoginScreen.State.OFFLINE;
      } else if (res.ok() && res.body() != null && res.body().has("access_token")) {
         if (am() != null) {
            am().applySession(res.body(), remember);
            OyVey.configManager.save();
         }

         authenticated = true;
         loggedInUser = user;
         this.proceed();
      } else {
         this.state = LoginScreen.State.ERROR;
         this.errMsg = res.error() != null ? res.error() : "authentication failed";
      }
   }

   private void proceed() {
      this.minecraft.setScreen(new TitleScreen());
   }

   private static boolean in(double mx, double my, int x, int y, int w, int h) {
      return mx >= x && mx < x + w && my >= y && my < y + h;
   }

   private boolean hovText(double mx, double my, int centerX, int y, String text) {
      int w = this.font.width(text);
      return mx >= centerX - w / 2 - 2 && mx < centerX + w / 2 + 2 && my >= y - 1 && my < y + 9;
   }

   private static String spinner(float age) {
      String[] f = new String[]{"working.  ", "working.. ", "working..."};
      return f[(int)(age * 3.0F) % f.length];
   }

   private static int argb(int a, int r, int g, int b) {
      return Mth.clamp(a, 0, 255) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static class SimpleField {
      String text = "";
      final boolean password;
      private long blinkBase = System.currentTimeMillis();

      SimpleField(boolean password) {
         this.password = password;
      }

      void type(int codepoint) {
         if (this.text.length() < 64) {
            this.text = this.text + new String(Character.toChars(codepoint));
            this.blinkBase = System.currentTimeMillis();
         }
      }

      void append(String s) {
         if (s != null && !s.isEmpty()) {
            int room = 64 - this.text.length();
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

      void clear() {
         this.text = "";
         this.blinkBase = System.currentTimeMillis();
      }

      String getDisplay() {
         return this.password ? "\u2022".repeat(this.text.length()) : this.text;
      }

      boolean cursorVisible() {
         return (System.currentTimeMillis() - this.blinkBase) % 1000L < 530L;
      }
   }

   private enum State {
      IDLE,
      LOADING,
      ERROR,
      OFFLINE;
   }
}
