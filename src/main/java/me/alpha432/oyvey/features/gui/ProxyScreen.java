package me.alpha432.oyvey.features.gui;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.proxy.ProxyEntry;
import me.alpha432.oyvey.proxy.ProxyTester;
import me.alpha432.oyvey.util.Lang;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class ProxyScreen extends Screen {
   private static final int STARS = 90;
   private final float[] sx = new float[90];
   private final float[] sy = new float[90];
   private final float[] svx = new float[90];
   private final float[] svy = new float[90];
   private final float[] sa = new float[90];
   private static final int VISIBLE_ROWS = 5;
   private static final int ROW_H = 14;
   private static final int FW = 230;
   private final ProxyScreen.SimpleField hostField = new ProxyScreen.SimpleField(false, 128);
   private final ProxyScreen.SimpleField portField = new ProxyScreen.SimpleField(false, 5);
   private final ProxyScreen.SimpleField userField = new ProxyScreen.SimpleField(false, 64);
   private final ProxyScreen.SimpleField passField = new ProxyScreen.SimpleField(true, 64);
   private ProxyScreen.SimpleField focused;
   private ProxyEntry.Type editType = ProxyEntry.Type.SOCKS5;
   private int selectedIndex = -1;
   private int scroll;
   private volatile String testMsg = "";
   private volatile int testColor = -5589824;
   private long openTime;

   public ProxyScreen() {
      super(Component.literal("proxies"));
   }

   public void init() {
      this.openTime = System.currentTimeMillis();
      this.focused = this.hostField;
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
   }

   private int formX() {
      return this.width / 2 - 115;
   }

   private int listTop() {
      return (int)(this.height * 0.24F);
   }

   private int formY() {
      return this.height - 120;
   }

   private int rowAt(double mx, double my) {
      int x = this.formX();
      int top = this.listTop();
      if (!(mx < x) && !(mx >= x + 230)) {
         int rel = (int)((my - top) / 14.0);
         if (rel >= 0 && rel < 5) {
            int idx = this.scroll + rel;
            return idx < OyVey.proxyManager.getProxies().size() ? idx : -2;
         } else {
            return -2;
         }
      } else {
         return -2;
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
      RenderUtil.verticalGradient(g, 0.0F, 0.0F, this.width, this.height * 0.5F, new Color(aR, aG, aB, 14), new Color(0, 0, 0, 0));

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

      g.centeredText(this.font, "PROXIES", this.width / 2, (int)(this.height * 0.16F), argb((int)(fadeIn * 255.0F), aR, aG, aB));
      int x = this.formX();
      int top = this.listTop();
      List<ProxyEntry> proxies = OyVey.proxyManager.getProxies();
      int activeIdx = OyVey.proxyManager.getActiveIndex();
      g.text(this.font, "saved (" + proxies.size() + ")", x, top - 11, argb((int)(fadeIn * 160.0F), aR, aG, aB), false);

      for (int r = 0; r < 5; r++) {
         int idx = this.scroll + r;
         int ry = top + r * 14;
         boolean hov = mx >= x && mx < x + 230 && my >= ry && my < ry + 14 - 1;
         boolean sel = idx == this.selectedIndex;
         RenderUtil.roundedRect(g, x, ry, 230, 13, 2, argb((int)(fadeIn * (sel ? 200 : (hov ? 150 : 90))), sel ? 24 : 14, sel ? 24 : 14, sel ? 44 : 28));
         if (idx >= 0 && idx < proxies.size()) {
            ProxyEntry p = proxies.get(idx);
            int lc = idx == activeIdx ? argb((int)(fadeIn * 255.0F), 90, 230, 120) : argb((int)(fadeIn * 225.0F), 187, 204, 221);
            g.text(this.font, p.label(), x + 5, ry + 3, lc, false);
            if (idx == activeIdx) {
               g.text(this.font, "active", x + 230 - this.font.width("active") - 5, ry + 3, argb((int)(fadeIn * 230.0F), 90, 230, 120), false);
            }
         } else {
            g.text(this.font, "-", x + 5, ry + 3, argb((int)(fadeIn * 70.0F), 120, 140, 160), false);
         }
      }

      int fy = this.formY();
      if (!this.testMsg.isEmpty()) {
         g.centeredText(this.font, this.testMsg, this.width / 2, fy - 13, this.testColor);
      }

      String typeLabel = "type: " + this.editType.name();
      this.drawButton(g, typeLabel, x, fy, 230, 18, mx, my, fadeIn, aR, aG, aB);
      this.drawField(g, this.hostField, "host", x, fy + 24, 160, mx, my, fadeIn, aR, aG, aB);
      this.drawField(g, this.portField, "port", x + 166, fy + 24, 64, mx, my, fadeIn, aR, aG, aB);
      this.drawField(g, this.userField, "user (optional)", x, fy + 46, 112, mx, my, fadeIn, aR, aG, aB);
      this.drawField(g, this.passField, "pass", x + 118, fy + 46, 112, mx, my, fadeIn, aR, aG, aB);
      int by = fy + 70;
      int bw = 54;
      this.drawButton(g, Lang.get(this.selectedIndex < 0 ? "proxy.save" : "proxy.update"), x, by, bw, 18, mx, my, fadeIn, aR, aG, aB);
      this.drawButton(g, Lang.get("proxy.test"), x + bw + 4, by, bw, 18, mx, my, fadeIn, aR, aG, aB);
      this.drawButton(g, Lang.get("proxy.setactive"), x + 2 * (bw + 4), by, bw, 18, mx, my, fadeIn, aR, aG, aB);
      this.drawButton(g, Lang.get("proxy.delete"), x + 3 * (bw + 4), by, bw, 18, mx, my, fadeIn, aR, aG, aB);
      int by2 = by + 22;
      this.drawButton(g, Lang.get("proxy.new"), x, by2, bw, 16, mx, my, fadeIn, aR, aG, aB);
      this.drawButton(g, Lang.get("proxy.disable"), x + bw + 4, by2, bw * 2 + 4, 16, mx, my, fadeIn, aR, aG, aB);
      this.drawButton(g, Lang.get("proxy.back"), x + 3 * (bw + 4), by2, bw, 16, mx, my, fadeIn, aR, aG, aB);
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
      GuiGraphicsExtractor g, ProxyScreen.SimpleField f, String placeholder, int x, int y, int w, int mx, int my, float fadeIn, int aR, int aG, int aB
   ) {
      int h = 18;
      boolean foc = this.focused == f;
      RenderUtil.roundedRect(g, x, y, w, h, 2, argb((int)(fadeIn * (foc ? 210 : 170)), foc ? 20 : 12, foc ? 20 : 12, foc ? 40 : 28));
      RenderUtil.roundedOutline(g, x, y, w, h, 3.0F, foc ? 0.9F : 0.55F, argb((int)(fadeIn * (foc ? 190 : 55)), aR, aG, aB));
      String shown = f.getDisplay() + (foc && f.cursorVisible() ? "|" : "");

      while (this.font.width(shown) > w - 8 && shown.length() > 0) {
         shown = shown.substring(1);
      }

      if (f.text.isEmpty() && !foc) {
         g.text(this.font, placeholder, x + 4, y + (h - 8) / 2, argb((int)(fadeIn * 80.0F), 170, 187, 204), false);
      } else {
         g.text(this.font, shown, x + 4, y + (h - 8) / 2, argb((int)(fadeIn * 230.0F), 221, 238, 255), false);
      }
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      if (click.button() != 0) {
         return super.mouseClicked(click, doubled);
      } else {
         double mx = click.x();
         double my = click.y();
         int row = this.rowAt(mx, my);
         if (row >= 0) {
            this.loadIntoForm(row);
            return true;
         } else {
            int x = this.formX();
            int fy = this.formY();
            if (hit(mx, my, x, fy, 230, 18)) {
               ProxyEntry.Type[] vals = ProxyEntry.Type.values();
               this.editType = vals[(this.editType.ordinal() + 1) % vals.length];
               return true;
            } else if (hit(mx, my, x, fy + 24, 160, 18)) {
               this.focused = this.hostField;
               return true;
            } else if (hit(mx, my, x + 166, fy + 24, 64, 18)) {
               this.focused = this.portField;
               return true;
            } else if (hit(mx, my, x, fy + 46, 112, 18)) {
               this.focused = this.userField;
               return true;
            } else if (hit(mx, my, x + 118, fy + 46, 112, 18)) {
               this.focused = this.passField;
               return true;
            } else {
               int by = fy + 70;
               int bw = 54;
               if (hit(mx, my, x, by, bw, 18)) {
                  this.saveForm();
                  return true;
               } else if (hit(mx, my, x + bw + 4, by, bw, 18)) {
                  this.runTest();
                  return true;
               } else if (hit(mx, my, x + 2 * (bw + 4), by, bw, 18)) {
                  this.setActive();
                  return true;
               } else if (hit(mx, my, x + 3 * (bw + 4), by, bw, 18)) {
                  this.deleteSelected();
                  return true;
               } else {
                  int by2 = by + 22;
                  if (hit(mx, my, x, by2, bw, 16)) {
                     this.newForm();
                     return true;
                  } else if (hit(mx, my, x + bw + 4, by2, bw * 2 + 4, 16)) {
                     OyVey.proxyManager.clearActive();
                     OyVey.configManager.save();
                     this.setStatus("proxy disabled", -2061272);
                     return true;
                  } else if (hit(mx, my, x + 3 * (bw + 4), by2, bw, 16)) {
                     this.minecraft.setScreen(new CustomMainMenuScreen());
                     return true;
                  } else {
                     return super.mouseClicked(click, doubled);
                  }
               }
            }
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
               String add = clip.replaceAll("[\\r\\n]", "");
               if (this.focused == this.portField) {
                  add = add.replaceAll("[^0-9]", "");
               }

               this.focused.append(add);
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

      if (key == 256) {
         this.minecraft.setScreen(new CustomMainMenuScreen());
         return true;
      }

      if (key == 258) {
         this.focused = this.focused == this.hostField
            ? this.portField
            : (this.focused == this.portField ? this.userField : (this.focused == this.userField ? this.passField : this.hostField));
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
      int cp = input.codepoint();
      if (this.focused != this.portField || cp >= 48 && cp <= 57) {
         this.focused.type(cp);
         return true;
      } else {
         return true;
      }
   }

   private void loadIntoForm(int idx) {
      ProxyEntry p = OyVey.proxyManager.getProxies().get(idx);
      this.selectedIndex = idx;
      this.editType = p.getType();
      this.hostField.text = p.getHost();
      this.portField.text = String.valueOf(p.getPort());
      this.userField.text = p.getUsername();
      this.passField.text = p.getPassword();
      this.setStatus("", -5589824);
   }

   private void newForm() {
      this.selectedIndex = -1;
      this.editType = ProxyEntry.Type.SOCKS5;
      this.hostField.text = "";
      this.portField.text = "";
      this.userField.text = "";
      this.passField.text = "";
      this.focused = this.hostField;
      this.setStatus("", -5589824);
   }

   private ProxyEntry buildFromForm() {
      int port;
      try {
         port = Integer.parseInt(this.portField.text.trim());
      } catch (NumberFormatException e) {
         port = 0;
      }

      return new ProxyEntry(this.editType, this.hostField.text.trim(), port, this.userField.text, this.passField.text);
   }

   private void saveForm() {
      ProxyEntry e = this.buildFromForm();
      if (!e.isValid()) {
         this.setStatus("enter a valid host and port", -42406);
      } else {
         if (this.selectedIndex >= 0 && this.selectedIndex < OyVey.proxyManager.getProxies().size()) {
            OyVey.proxyManager.getProxies().set(this.selectedIndex, e);
         } else {
            OyVey.proxyManager.add(e);
            this.selectedIndex = OyVey.proxyManager.getProxies().size() - 1;
         }

         OyVey.configManager.save();
         this.setStatus("saved", -10819976);
      }
   }

   private void setActive() {
      if (this.selectedIndex < 0) {
         this.saveForm();
      }

      if (this.selectedIndex >= 0) {
         OyVey.proxyManager.setActiveIndex(this.selectedIndex);
         OyVey.configManager.save();
         this.setStatus("active proxy set - applies to the next server join", -10819976);
      }
   }

   private void deleteSelected() {
      if (this.selectedIndex >= 0) {
         OyVey.proxyManager.remove(this.selectedIndex);
         OyVey.configManager.save();
         this.newForm();
         this.setStatus("deleted", -2061272);
      }
   }

   private void runTest() {
      ProxyEntry e = this.buildFromForm();
      this.setStatus("testing...", -5589824);
      ProxyTester.test(e, (ok, ms, msg) -> {
         if (ok) {
            this.setStatus("ok - reachable (" + ms + " ms)", -10819976);
         } else {
            this.setStatus("failed: " + msg, -42406);
         }
      });
   }

   private void setStatus(String msg, int color) {
      this.testMsg = msg;
      this.testColor = color;
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
      final boolean password;
      final int max;
      private long blinkBase = System.currentTimeMillis();

      SimpleField(boolean password, int max) {
         this.password = password;
         this.max = max;
      }

      void type(int codepoint) {
         if (this.text.length() < this.max) {
            this.text = this.text + new String(Character.toChars(codepoint));
            this.blinkBase = System.currentTimeMillis();
         }
      }

      void backspace() {
         if (!this.text.isEmpty()) {
            this.text = this.text.substring(0, this.text.length() - 1);
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
}
