package me.alpha432.oyvey.features.gui;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.net.URI;
import java.util.List;
import java.util.Random;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.auth.MicrosoftAuth;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class AccountManagerScreen extends Screen {
   private volatile boolean microsoftBusy;
   private static final int CARD_W = 240;
   private static final int ENTRY_H = 18;
   private static final int ENTRY_GAP = 3;
   private static final int MAX_VISIBLE = 7;
   private static final int FH = 18;
   private static final int STARS = 90;
   private final float[] sx = new float[90];
   private final float[] sy = new float[90];
   private final float[] svx = new float[90];
   private final float[] svy = new float[90];
   private final float[] sa = new float[90];
   private final AccountManagerScreen.SimpleField addField = new AccountManagerScreen.SimpleField();
   private volatile String statusMsg = "";
   private volatile boolean statusOk = false;
   private int scrollOffset = 0;
   private long openTime;

   public AccountManagerScreen() {
      super(Component.literal("accounts"));
   }

   public void init() {
      this.openTime = System.currentTimeMillis();
      this.statusMsg = "";
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

         int a = (int)(this.sa[i] * fadeIn * 140.0F);
         g.fill((int)this.sx[i], (int)this.sy[i], (int)this.sx[i] + 1, (int)this.sy[i] + 1, argb(a, aR, aG, aB));
      }

      String title = "ACCOUNTS";
      float titleScale = 2.2F;
      int titleY = (int)(this.height * 0.18F);
      g.pose().pushMatrix();
      g.pose().scale(titleScale, titleScale);
      int tx = (int)((this.width / 2.0F - this.font.width(title) * titleScale / 2.0F) / titleScale);
      int ty = (int)((titleY - 6.0F) / titleScale);

      for (int d = 3; d >= 1; d--) {
         int ga = (int)(fadeIn * 35.0F / d);
         int gc = argb(ga, aR, aG, aB);
         g.text(this.font, title, tx - d, ty, gc, false);
         g.text(this.font, title, tx + d, ty, gc, false);
         g.text(this.font, title, tx, ty - d, gc, false);
         g.text(this.font, title, tx, ty + d, gc, false);
      }

      g.text(this.font, title, tx + 1, ty + 1, argb((int)(fadeIn * 120.0F), 0, 0, 0), false);
      g.text(this.font, title, tx, ty, argb((int)(fadeIn * 255.0F), aR, aG, aB), false);
      g.pose().popMatrix();
      List<String> accs = OyVey.accountManager.getAccounts();
      String current = OyVey.accountManager.getCurrentName();
      int visibleCount = Math.min(accs.size(), 7);
      int listH = visibleCount * 21;
      int addRowH = 32;
      int headerH = 30;
      int CARD_H = headerH + listH + addRowH + 10 + 24;
      int cx = this.width / 2 - 120;
      int cy = (int)(this.height * 0.32F);
      RenderUtil.dropShadow(g, cx, cy, 240, CARD_H, 8.0F, 14.0F, (int)(fadeIn * 145.0F));
      RenderUtil.roundedRectGradient(
         g, cx, cy, 240, CARD_H, 8.0F, argb((int)(fadeIn * 228.0F), 14, 14, 36), argb((int)(fadeIn * 218.0F), 7, 7, 22), true
      );
      RenderUtil.roundedOutline(g, cx, cy, 240, CARD_H, 8.0F, 0.9F, argb((int)(fadeIn * 115.0F), aR, aG, aB));
      RenderUtil.roundedRect(g, cx + 4, cy + 8, 2, Math.max(4, CARD_H - 16), 1, argb((int)(fadeIn * 220.0F), aR, aG, aB));
      String playingAs = "playing as: " + (current.isEmpty() ? "none" : current);
      g.centeredText(this.font, playingAs, this.width / 2, cy + 8, argb((int)(fadeIn * 180.0F), aR, aG, aB));
      g.fill(cx + 6, cy + headerH - 2, cx + 240 - 6, cy + headerH - 1, argb((int)(fadeIn * 60.0F), aR, aG, aB));
      int lx = cx + 8;
      int lw = 224;
      int ly = cy + headerH;
      int delW = 12;
      int start = Mth.clamp(this.scrollOffset, 0, Math.max(0, accs.size() - 7));
      int end = Math.min(accs.size(), start + 7);

      for (int i = start; i < end; i++) {
         String name = accs.get(i);
         boolean isActive = name.equals(current);
         int ey = ly + (i - start) * 21;
         boolean hov = mx >= lx && mx < lx + lw - delW - 4 && my >= ey && my < ey + 18;
         boolean delHov = mx >= lx + lw - delW && mx < lx + lw && my >= ey && my < ey + 18;
         int ebg = isActive
            ? argb((int)(fadeIn * 180.0F), 24, 24, 48)
            : (hov ? argb((int)(fadeIn * 140.0F), 20, 20, 38) : argb((int)(fadeIn * 100.0F), 12, 12, 28));
         RenderUtil.roundedRect(g, lx, ey, lw, 18, 2, ebg);
         if (isActive) {
            g.fill(lx, ey + 2, lx + 2, ey + 18 - 2, argb((int)(fadeIn * 220.0F), aR, aG, aB));
         }

         int nameColor = isActive
            ? argb((int)(fadeIn * 255.0F), aR, aG, aB)
            : (hov ? argb((int)(fadeIn * 230.0F), 221, 238, 255) : argb((int)(fadeIn * 185.0F), 153, 170, 187));
         boolean isMs = OyVey.accountManager.isMicrosoft(name);
         int nameX = lx + (isMs ? 16 : 6);
         int nameBudget = lw - delW - (isMs ? 26 : 16);
         String display = name;

         while (this.font.width(display) > nameBudget && display.length() > 1) {
            display = display.substring(0, display.length() - 1);
         }

         if (!display.equals(name)) {
            display = display + "\u2026";
         }

         if (isMs) {
            int gx = lx + 5;
            int gy = ey + 5;
            g.fill(gx, gy, gx + 3, gy + 3, argb((int)(fadeIn * 255.0F), 242, 80, 34));
            g.fill(gx + 4, gy, gx + 7, gy + 3, argb((int)(fadeIn * 255.0F), 127, 186, 0));
            g.fill(gx, gy + 4, gx + 3, gy + 7, argb((int)(fadeIn * 255.0F), 0, 164, 239));
            g.fill(gx + 4, gy + 4, gx + 7, gy + 7, argb((int)(fadeIn * 255.0F), 255, 185, 0));
         }

         g.text(this.font, display, nameX, ey + 5, nameColor, false);
         if (isActive) {
            String badge = "\u2713";
            int bx2 = lx + lw - delW - this.font.width(badge) - 6;
            g.text(this.font, badge, bx2, ey + 5, argb((int)(fadeIn * 220.0F), aR, aG, aB), false);
         }

         int xColor = delHov ? argb((int)(fadeIn * 255.0F), 255, 80, 80) : argb((int)(fadeIn * 140.0F), 119, 68, 68);
         g.centeredText(this.font, "\u2715", lx + lw - delW / 2, ey + 5, xColor);
      }

      if (accs.size() > 7) {
         int barX = cx + 240 - 4;
         int barY = cy + headerH;
         float ratio = 7.0F / accs.size();
         int thumbH = Math.max(10, (int)(listH * ratio));
         int thumbY = barY + (int)((float)(listH - thumbH) * start / (accs.size() - 7));
         g.fill(barX, barY, barX + 2, barY + listH, argb((int)(fadeIn * 40.0F), 255, 255, 255));
         g.fill(barX, thumbY, barX + 2, thumbY + thumbH, argb((int)(fadeIn * 160.0F), aR, aG, aB));
      }

      int fy = cy + headerH + listH + 8;
      int addBtnW = 32;
      int fw = lw - addBtnW - 4;
      if (mx >= lx && mx < lx + fw && my >= fy && my < fy + 18) {
         boolean var60 = true;
      } else {
         boolean var59 = false;
      }

      boolean btnHov = mx >= lx + fw + 4 && mx < lx + lw && my >= fy && my < fy + 18;
      RenderUtil.roundedRect(g, lx, fy, fw, 18, 2, argb((int)(fadeIn * 180.0F), 16, 16, 34));
      g.fill(lx, fy, lx + fw, fy + 1, argb((int)(fadeIn * 160.0F), aR, aG, aB));
      String disp = this.addField.text;
      boolean showCursor = this.addField.cursorVisible();
      String rendered = disp + (showCursor ? "|" : "");

      while (this.font.width(rendered) > fw - 8 && rendered.length() > 1) {
         rendered = rendered.substring(1);
      }

      if (disp.isEmpty()) {
         g.text(this.font, "add username...", lx + 4, fy + 5, argb((int)(fadeIn * 70.0F), 170, 187, 204), false);
      } else {
         g.text(this.font, rendered, lx + 4, fy + 5, argb((int)(fadeIn * 230.0F), 221, 238, 255), false);
      }

      int abx = lx + fw + 4;
      int abg = btnHov ? argb((int)(fadeIn * 220.0F), 24, 24, 48) : argb((int)(fadeIn * 160.0F), 12, 12, 28);
      RenderUtil.roundedRect(g, abx, fy, addBtnW, 18, 2, abg);
      g.fill(abx, fy, abx + addBtnW, fy + 1, argb((int)(fadeIn * (btnHov ? 200 : 140)), aR, aG, aB));
      g.centeredText(this.font, "Add", abx + addBtnW / 2, fy + 5, argb((int)(fadeIn * (btnHov ? 255 : 200)), aR, aG, aB));
      int msY = fy + 24;
      boolean msHov = mx >= lx && mx < lx + lw && my >= msY && my < msY + 18;
      RenderUtil.roundedRect(g, lx, msY, lw, 18, 2, argb((int)(fadeIn * (msHov ? 210 : 160)), msHov ? 22 : 14, msHov ? 22 : 14, msHov ? 44 : 30));
      RenderUtil.roundedOutline(g, lx, msY, lw, 18, 3.0F, 0.8F, argb((int)(fadeIn * (msHov ? 220 : 90)), aR, aG, aB));
      g.centeredText(this.font, "Microsoft sign-in", lx + lw / 2, msY + 5, argb((int)(fadeIn * (msHov ? 255 : 220)), aR, aG, aB));
      if (!this.statusMsg.isEmpty()) {
         int sc = this.statusOk ? argb((int)(fadeIn * 220.0F), 90, 220, 120) : argb((int)(fadeIn * 220.0F), 255, 90, 90);
         g.centeredText(this.font, this.statusMsg, this.width / 2, msY + 18 + 6, sc);
      }

      g.text(this.font, "ESC to go back", cx, cy + CARD_H + 4, argb((int)(fadeIn * 100.0F), 68, 85, 102), false);
      String ver = "v2.0";
      g.text(this.font, ver, 4, this.height - 10, argb((int)(fadeIn * 200.0F), 74, 102, 136), false);
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      double mx = click.x();
      double my = click.y();
      List<String> accs = OyVey.accountManager.getAccounts();
      String current = OyVey.accountManager.getCurrentName();
      int visibleCount = Math.min(accs.size(), 7);
      int listH = visibleCount * 21;
      int headerH = 30;
      int cx = this.width / 2 - 120;
      int cy = (int)(this.height * 0.32F);
      int lx = cx + 8;
      int lw = 224;
      int delW = 12;
      int ly = cy + headerH;
      int start = Mth.clamp(this.scrollOffset, 0, Math.max(0, accs.size() - 7));
      int end = Math.min(accs.size(), start + 7);
      if (click.button() == 0) {
         for (int i = start; i < end; i++) {
            String name = accs.get(i);
            int ey = ly + (i - start) * 21;
            if (mx >= lx + lw - delW && mx < lx + lw && my >= ey && my < ey + 18) {
               if (name.equals(current)) {
                  this.statusMsg = "can't remove active account";
                  this.statusOk = false;
               } else {
                  OyVey.accountManager.removeAccount(name);
                  this.statusMsg = "removed " + name;
                  this.statusOk = false;
                  OyVey.configManager.save();
               }

               return true;
            }

            if (mx >= lx && mx < lx + lw - delW - 4 && my >= ey && my < ey + 18) {
               if (!name.equals(current)) {
                  if (OyVey.accountManager.isMicrosoft(name)) {
                     this.reloginMicrosoft(name);
                  } else {
                     boolean ok = OyVey.accountManager.switchAccount(name);
                     this.statusMsg = ok ? "switched to " + name : "switch failed";
                     this.statusOk = ok;
                  }
               }

               return true;
            }
         }

         int addRowTop = cy + headerH + listH + 8;
         int addBtnW = 32;
         int fw = lw - addBtnW - 4;
         int abx = lx + fw + 4;
         if (mx >= abx && mx < abx + addBtnW && my >= addRowTop && my < addRowTop + 18) {
            this.doAdd();
            return true;
         }

         int msY = cy + headerH + listH + 8 + 24;
         if (mx >= lx && mx < lx + lw && my >= msY && my < msY + 18) {
            this.startMicrosoftLogin();
            return true;
         }
      }

      return super.mouseClicked(click, doubled);
   }

   public boolean mouseScrolled(double mx, double my, double dx, double dy) {
      int max = Math.max(0, OyVey.accountManager.getAccounts().size() - 7);
      this.scrollOffset = Mth.clamp(this.scrollOffset - (int)Math.signum(dy), 0, max);
      return true;
   }

   public boolean keyPressed(KeyEvent input) {
      int key = input.input();
      if (key == 256) {
         this.minecraft.setScreen(new CustomMainMenuScreen());
         return true;
      } else if (key == 257 || key == 335) {
         this.doAdd();
         return true;
      } else if (key == 259) {
         this.addField.backspace();
         return true;
      } else {
         return super.keyPressed(input);
      }
   }

   public boolean charTyped(CharacterEvent input) {
      this.addField.type(input.codepoint());
      return true;
   }

   private void doAdd() {
      String name = this.addField.text.trim();
      if (name.isEmpty()) {
         this.statusMsg = "enter a username";
         this.statusOk = false;
      } else if (OyVey.accountManager.getAccounts().contains(name)) {
         this.statusMsg = name + " already saved";
         this.statusOk = false;
      } else {
         OyVey.accountManager.addAccount(name);
         OyVey.configManager.save();
         this.statusMsg = "saved " + name;
         this.statusOk = true;
         this.addField.text = "";
      }
   }

   private void startMicrosoftLogin() {
      if (this.microsoftBusy) {
         this.statusMsg = "Microsoft sign-in is already running...";
         this.statusOk = false;
         return;
      }

      this.microsoftBusy = true;
      this.statusMsg = "starting Microsoft sign-in...";
      this.statusOk = true;
      MicrosoftAuth.deviceCodeLogin((userCode, uri, directUri) -> {
         this.statusMsg = "code copied to clipboard - paste " + userCode + " in the browser";
         this.statusOk = true;
         Minecraft.getInstance().execute(() -> {
            try {
               GLFW.glfwSetClipboardString(this.minecraft.getWindow().handle(), userCode);
            } catch (Exception var4) {
            }

            openUrl(uri);
         });
      }, (name, uuid, token, sessionJson) -> Minecraft.getInstance().execute(() -> {
         this.microsoftBusy = false;
         boolean ok = OyVey.accountManager.setOnlineSession(name, uuid, token);
         OyVey.accountManager.saveMicrosoftSession(name, sessionJson);
         OyVey.configManager.save();
         this.statusMsg = ok ? "signed in as " + name : "session swap failed";
         this.statusOk = ok;
      }), err -> {
         this.microsoftBusy = false;
         this.statusMsg = "sign-in failed: " + err;
         this.statusOk = false;
      });
   }

   private void reloginMicrosoft(String name) {
      if (this.microsoftBusy) {
         this.statusMsg = "Microsoft sign-in is already running...";
         this.statusOk = false;
         return;
      }

      String session = OyVey.accountManager.getMicrosoftSession(name);
      if (session != null && !session.isEmpty()) {
         this.microsoftBusy = true;
         this.statusMsg = "logging into " + name + "...";
         this.statusOk = true;
         MicrosoftAuth.refreshAndLogin(session, (n, uuid, token, newJson) -> Minecraft.getInstance().execute(() -> {
            this.microsoftBusy = false;
            boolean ok = OyVey.accountManager.setOnlineSession(n, uuid, token);
            OyVey.accountManager.saveMicrosoftSession(n, newJson);
            OyVey.configManager.save();
            this.statusMsg = ok ? "signed in as " + n : "session swap failed";
            this.statusOk = ok;
         }), err -> {
            this.microsoftBusy = false;
            this.statusMsg = "session expired - use Microsoft sign-in again";
            this.statusOk = false;
         });
      } else {
         this.statusMsg = "no saved session for " + name + " - use Microsoft sign-in";
         this.statusOk = false;
      }
   }

   private static void openUrl(String url) {
      try {
         if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.BROWSE)) {
            Desktop.getDesktop().browse(URI.create(url));
            return;
         }
      } catch (Throwable var3) {
      }

      try {
         Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
      } catch (Throwable var2) {
      }
   }

   private static int argb(int a, int r, int g, int b) {
      return Mth.clamp(a, 0, 255) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static class SimpleField {
      String text = "";
      private long blinkBase = System.currentTimeMillis();

      void type(int codepoint) {
         if (this.text.length() < 16) {
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

      boolean cursorVisible() {
         return (System.currentTimeMillis() - this.blinkBase) % 1000L < 530L;
      }
   }
}
