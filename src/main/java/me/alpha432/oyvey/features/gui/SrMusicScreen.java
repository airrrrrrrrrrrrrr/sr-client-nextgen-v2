package me.alpha432.oyvey.features.gui;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.client.SrMusicModule;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class SrMusicScreen extends Screen {
   private static final int WIN_W = 264;
   private static final int WIN_H = 350;
   private int wx = -1;
   private int wy = -1;
   private boolean dragging = false;
   private int dragOffX;
   private int dragOffY;
   private int queueScroll = 0;
   private boolean addTyping = false;
   private String addInput = "";
   private boolean volDragging = false;
   private int volBarX;
   private int volBarW;
   private float marqueeOffset = 0.0F;
   private long marqueeLast = 0L;
   private int htCtrlY;
   private int htSfX;
   private int htRpX;
   private int htVolY;
   private int htQueueY;
   private int htQueueH;
   private int htAddY;
   private int htAddBtnX;
   private static SrMusicScreen INSTANCE;

   public static SrMusicScreen getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new SrMusicScreen();
      }

      return INSTANCE;
   }

   private SrMusicScreen() {
      super(Component.literal("SR Music"));
   }

   public void init() {
      if (this.wx == -1) {
         this.wx = (this.width - 264) / 2;
         this.wy = (this.height - 350) / 2;
      }

      this.wx = Math.max(0, Math.min(this.width - 264, this.wx));
      this.wy = Math.max(0, Math.min(this.height - 350, this.wy));
   }

   public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      SrMusicModule music = SrMusicModule.INSTANCE;
      Font font = Minecraft.getInstance().font;
      int accent = OyVey.colorManager.getColorAsInt();
      int aR = accent >> 16 & 0xFF;
      int aG = accent >> 8 & 0xFF;
      int aB = accent & 0xFF;
      if (this.dragging) {
         long win = Minecraft.getInstance().getWindow().handle();
         if (GLFW.glfwGetMouseButton(win, 0) == 1) {
            this.wx = Mth.clamp(mx - this.dragOffX, 0, this.width - 264);
            this.wy = Mth.clamp(my - this.dragOffY, 0, this.height - 350);
         } else {
            this.dragging = false;
         }
      }

      if (this.volDragging) {
         long win = Minecraft.getInstance().getWindow().handle();
         if (GLFW.glfwGetMouseButton(win, 0) == 1) {
            float pct = Mth.clamp((float)(mx - this.volBarX) / this.volBarW, 0.0F, 1.0F);
            if (music != null) {
               music.setVolume(pct);
            }
         } else {
            this.volDragging = false;
         }
      }

      g.fill(this.wx + 4, this.wy + 4, this.wx + 264 + 4, this.wy + 350 + 4, 1711276032);
      g.fill(this.wx, this.wy, this.wx + 264, this.wy + 350, -200930282);
      g.fill(this.wx, this.wy, this.wx + 264, this.wy + 1, 0xFF000000 | accent);
      g.fill(this.wx, this.wy, this.wx + 264, this.wy + 1, argb(80, aR, aG, aB));
      g.fill(this.wx, this.wy + 350 - 1, this.wx + 264, this.wy + 350, 419430399);
      g.fill(this.wx, this.wy, this.wx + 1, this.wy + 350, 587202559);
      g.fill(this.wx + 264 - 1, this.wy, this.wx + 264, this.wy + 350, 419430399);
      int bc = 0xFF000000 | accent;
      g.fill(this.wx, this.wy, this.wx + 10, this.wy + 2, bc);
      g.fill(this.wx, this.wy, this.wx + 2, this.wy + 10, bc);
      g.fill(this.wx + 264 - 10, this.wy, this.wx + 264, this.wy + 2, bc);
      g.fill(this.wx + 264 - 2, this.wy, this.wx + 264, this.wy + 10, bc);
      g.fill(this.wx, this.wy + 350 - 2, this.wx + 10, this.wy + 350, bc);
      g.fill(this.wx, this.wy + 350 - 10, this.wx + 2, this.wy + 350, bc);
      g.fill(this.wx + 264 - 10, this.wy + 350 - 2, this.wx + 264, this.wy + 350, bc);
      g.fill(this.wx + 264 - 2, this.wy + 350 - 10, this.wx + 264, this.wy + 350, bc);
      int cur = this.wy;
      g.fill(this.wx, cur, this.wx + 264, cur + 26, -267775968);
      g.pose().pushMatrix();
      g.pose().scale(1.3F, 1.3F);
      g.text(font, "\u266a SR Music", (int)((this.wx + 10) / 1.3F), (int)((cur + 8) / 1.3F), 0xFF000000 | accent, false);
      g.pose().popMatrix();
      int closeX = this.wx + 264 - 22;
      int closeY = cur + 6;
      boolean closeHov = mx >= closeX && mx <= closeX + 16 && my >= closeY && my <= closeY + 14;
      RenderUtil.roundedRect(g, closeX, closeY, 16, 14, 3, closeHov ? 1728005188 : 872415231);
      g.text(font, "\u00d7", closeX + (16 - font.width("\u00d7")) / 2, closeY + 3, closeHov ? -30584 : -5592406, false);
      cur += 26;
      g.fill(this.wx, cur, this.wx + 264, cur + 1, argb(40, aR, aG, aB));
      g.fill(this.wx, ++cur, this.wx + 264, cur + 22, -267907814);
      SrMusicModule.CheckStatus cs = music != null ? music.getCheckStatus() : SrMusicModule.CheckStatus.UNCHECKED;
      String detail = music != null ? music.getCheckDetail() : "\u2014";

      int statusColor = switch (cs) {
         case OK -> -12277146;
         case MISSING_YTDLP, MISSING_FFMPEG -> -3377340;
         case ERROR -> -3390396;
         case CHECKING -> -4478396;
         default -> -11180425;
      };

      String statusIcon = switch (cs) {
         case OK -> "\u2714";
         case MISSING_YTDLP, MISSING_FFMPEG, ERROR -> "\u2717";
         case CHECKING -> "\u2026";
         default -> "?";
      };
      String detailTrunc = detail;
      int maxDetailW = 214;

      while (font.width(statusIcon + " " + detailTrunc) > maxDetailW && detailTrunc.length() > 4) {
         detailTrunc = detailTrunc.substring(0, detailTrunc.length() - 4) + "\u2026";
      }

      g.text(font, statusIcon + " " + detailTrunc, this.wx + 6, cur + 7, statusColor, false);
      int testBtnX = this.wx + 264 - 34;
      int testBtnW = 28;
      int testBtnY = cur + 5;
      boolean testHov = mx >= testBtnX && mx <= testBtnX + testBtnW && my >= testBtnY && my <= testBtnY + 12;
      RenderUtil.roundedRect(g, testBtnX, testBtnY, testBtnW, 12, 2, testHov ? 0xFF000000 | accent : 872415231);
      g.text(font, "Test", testBtnX + (testBtnW - font.width("Test")) / 2, testBtnY + 2, -3355444, false);
      cur += 22;
      g.fill(this.wx, cur, this.wx + 264, cur + 1, 419430399);
      cur++;
      if (music == null) {
         g.text(font, "Module not loaded", this.wx + 8, cur + 10, -12298906, false);
      } else {
         SrMusicModule.State mstate = music.getState();

         int stateColor = switch (mstate) {
            case PLAYING -> -12272794;
            case PAUSED -> -3364301;
            case ERROR -> -3390396;
            case LOADING -> -10053172;
            default -> -12298906;
         };

         String stateStr = switch (mstate) {
            case PLAYING -> "\u25b6  Playing";
            case PAUSED -> "\u23f8  Paused";
            case ERROR -> "\u2717  " + music.getErrorMsg();
            case LOADING -> "Loading\u2026";
            default -> "Idle";
         };
         g.text(font, stateStr, this.wx + 8, cur + 6, stateColor, false);
         cur += 16;
         String title = music.getNowTitle();
         int maxTW = 248;
         int titleW = font.width(title);
         g.enableScissor(this.wx + 6, cur - 1, this.wx + 264 - 6, cur + 10);
         if (titleW > maxTW && mstate == SrMusicModule.State.PLAYING) {
            long now = System.currentTimeMillis();
            if (now - this.marqueeLast > 28L) {
               this.marqueeOffset += 0.55F;
               if (this.marqueeOffset > titleW + 22) {
                  this.marqueeOffset = 0.0F;
               }

               this.marqueeLast = now;
            }

            int ox = -((int)this.marqueeOffset);
            g.text(font, title, this.wx + 6 + ox, cur, -1118465, false);
            if (this.marqueeOffset > titleW - maxTW + 8) {
               g.text(font, title, this.wx + 6 + ox + titleW + 22, cur, -1118465, false);
            }
         } else {
            if (mstate != SrMusicModule.State.PLAYING) {
               this.marqueeOffset = 0.0F;
            }

            String t = title;

            while (font.width(t) > maxTW && t.length() > 3) {
               t = t.substring(0, t.length() - 4) + "\u2026";
            }

            g.text(font, t, this.wx + 6, cur, -1118465, false);
         }

         g.disableScissor();
         cur += 12;
         g.text(font, music.getNowProgress(), this.wx + 6, cur, -12298906, false);
         cur += 9;
         int barX = this.wx + 6;
         int barW2 = 252;
         g.fill(barX, cur, barX + barW2, cur + 5, 754974719);
         int filled2 = (int)(barW2 * music.getProgressPct());
         if (filled2 > 0) {
            g.fill(barX, cur, barX + filled2, cur + 5, OyVey.colorManager.getColorWithAlpha(cur, 230));
            g.fill(barX + filled2 - 4, cur - 2, barX + filled2 + 4, cur + 7, -1);
         }

         cur += 9;
         g.fill(this.wx, cur, this.wx + 264, cur + 1, 318767103);
         cur += 4;
         int btnSz = 22;
         int spacing = 28;
         int ctrlStartX = this.wx + (264 - spacing * 3) / 2;
         this.htCtrlY = cur;
         String[] btns = new String[]{"|<", mstate == SrMusicModule.State.PLAYING ? "\u23f8" : "\u25b6", ">|"};

         for (int i = 0; i < 3; i++) {
            int bx = ctrlStartX + i * spacing;
            boolean bh = mx >= bx && mx <= bx + btnSz && my >= cur && my <= cur + btnSz;
            RenderUtil.roundedRect(g, bx, cur, btnSz, btnSz, 5, bh ? 1728053247 : 687865855);
            if (bh) {
               RenderUtil.roundedRect(g, bx, cur, btnSz, btnSz, 5, argb(40, aR, aG, aB));
            }

            g.text(font, btns[i], bx + (btnSz - font.width(btns[i])) / 2, cur + (btnSz - 8) / 2, bh ? 0xFF000000 | accent : -3355444, false);
         }

         this.htSfX = this.wx + 6;
         boolean sfOn = music.isShuffle();
         boolean sfH = mx >= this.htSfX && mx <= this.htSfX + 20 && my >= cur + 2 && my <= cur + btnSz - 2;
         RenderUtil.roundedRect(g, this.htSfX, cur + 2, 20, btnSz - 4, 4, sfOn ? argb(170, aR / 3, aG / 3, aB / 3) : (sfH ? 1157627903 : 587202559));
         g.text(font, "\u21c4", this.htSfX + (20 - font.width("\u21c4")) / 2, cur + (btnSz - 8) / 2, sfOn ? 0xFF000000 | accent : -10061944, false);
         this.htRpX = this.wx + 264 - 28;
         boolean rpOn = music.isRepeat();
         boolean rpH = mx >= this.htRpX && mx <= this.htRpX + 20 && my >= cur + 2 && my <= cur + btnSz - 2;
         RenderUtil.roundedRect(g, this.htRpX, cur + 2, 20, btnSz - 4, 4, rpOn ? argb(170, aR / 3, aG / 3, aB / 3) : (rpH ? 1157627903 : 587202559));
         g.text(font, "\u21ba", this.htRpX + (20 - font.width("\u21ba")) / 2, cur + (btnSz - 8) / 2, rpOn ? 0xFF000000 | accent : -10061944, false);
         cur += btnSz + 4;
         g.text(font, "vol", this.wx + 6, cur + 2, -12298906, false);
         int vbX = this.wx + 26;
         int vbW = 230;
         this.htVolY = cur + 3;
         this.volBarX = vbX;
         this.volBarW = vbW;
         g.fill(vbX, this.htVolY, vbX + vbW, this.htVolY + 5, 754974719);
         int vFill = (int)(vbW * music.getVolume());
         if (vFill > 0) {
            g.fill(vbX, this.htVolY, vbX + vFill, this.htVolY + 5, OyVey.colorManager.getColorWithAlpha(this.htVolY, 220));
         }

         g.fill(vbX + vFill - 4, this.htVolY - 2, vbX + vFill + 4, this.htVolY + 7, -1);
         cur += 14;
         g.fill(this.wx, cur, this.wx + 264, cur + 1, 318767103);
         cur += 4;
         g.text(font, "Queue  \u00a78(" + music.getQueueSize() + ")", this.wx + 6, cur + 2, -13417387, false);
         cur += 12;
         this.htQueueY = cur;
         this.htQueueH = this.wy + 350 - 28 - cur;
         g.enableScissor(this.wx, cur, this.wx + 264, cur + this.htQueueH);
         int eyQ = cur - this.queueScroll;

         for (int i = 0; i < music.getQueueSize(); i++) {
            boolean active = i == music.getQueueIdx();
            boolean eHov = mx >= this.wx && mx <= this.wx + 264 - 20 && my >= eyQ && my <= eyQ + 16;
            if (active) {
               g.fill(this.wx, eyQ, this.wx + 264 - 20, eyQ + 16, argb(55, aR / 3, aG / 3, aB / 3));
            } else if (eHov) {
               g.fill(this.wx, eyQ, this.wx + 264 - 20, eyQ + 16, 385875967);
            }

            String lbl = music.getQueueTitle(i);

            while (font.width(lbl) > 224 && lbl.length() > 3) {
               lbl = lbl.substring(0, lbl.length() - 4) + "\u2026";
            }

            g.text(font, (active ? "\u25b6 " : "  ") + lbl, this.wx + 4, eyQ + 4, active ? 0xFF000000 | accent : -10850688, false);
            int rmX = this.wx + 264 - 20;
            boolean rmH = mx >= rmX && mx <= rmX + 16 && my >= eyQ + 2 && my <= eyQ + 14;
            if (rmH) {
               g.fill(rmX, eyQ + 2, rmX + 16, eyQ + 14, 1157575475);
            }

            g.text(font, "\u00d7", rmX + (16 - font.width("\u00d7")) / 2, eyQ + 4, -8960922, false);
            eyQ += 16;
         }

         if (music.getQueueSize() == 0) {
            g.text(font, "\u00a78Empty \u2014 add a URL or search below", this.wx + 8, cur + 6, -13417387, false);
         }

         g.disableScissor();
         cur = this.wy + 350 - 28;
         g.fill(this.wx, cur, this.wx + 264, cur + 1, 419430399);
         cur += 3;
         this.htAddY = cur;
         int inpW = 234;
         int inpH = 18;
         boolean inpHov = mx >= this.wx + 4 && mx <= this.wx + 4 + inpW && my >= cur && my <= cur + inpH;
         g.fill(this.wx + 4, cur, this.wx + 4 + inpW, cur + inpH, this.addTyping ? 1090519039 : (inpHov ? 553648127 : 352321535));
         g.fill(this.wx + 4, cur, this.wx + 4 + inpW, cur + 1, 687865855);
         boolean blink2 = this.addTyping && System.currentTimeMillis() % 900L < 450L;
         if (this.addInput.isEmpty() && !this.addTyping) {
            g.text(font, "\u00a78YouTube URL or search query\u2026", this.wx + 7, cur + 5, -13417387, false);
         } else {
            String disp = this.addInput;

            while (font.width(disp) > inpW - 10 && disp.length() > 1) {
               disp = disp.substring(1);
            }

            g.text(font, disp + (blink2 ? "\u00a7f|" : ""), this.wx + 7, cur + 5, -1118482, false);
         }

         this.htAddBtnX = this.wx + 4 + inpW + 4;
         int addBW = 18;
         boolean addHov = mx >= this.htAddBtnX && mx <= this.htAddBtnX + addBW && my >= cur && my <= cur + inpH;
         g.fill(this.htAddBtnX, cur, this.htAddBtnX + addBW, cur + inpH, addHov ? 0xFF000000 | accent : 872415231);
         g.text(font, "+", this.htAddBtnX + (addBW - font.width("+")) / 2, cur + 5, -1, false);
      }
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      int mx = (int)click.x();
      int my = (int)click.y();
      int btn = click.button();
      SrMusicModule music = SrMusicModule.INSTANCE;
      if (my >= this.wy && my <= this.wy + 26 && mx >= this.wx && mx <= this.wx + 264 - 24 && btn == 0) {
         this.dragging = true;
         this.dragOffX = mx - this.wx;
         this.dragOffY = my - this.wy;
         return true;
      }

      int closeX = this.wx + 264 - 22;
      int closeY2 = this.wy + 6;
      if (mx >= closeX && mx <= closeX + 16 && my >= closeY2 && my <= closeY2 + 14) {
         this.onClose();
         return true;
      }

      if (music == null) {
         return super.mouseClicked(click, doubled);
      }

      int testBtnX2 = this.wx + 264 - 34;
      int testBtnY2 = this.wy + 27 + 1 + 5;
      if (mx >= testBtnX2 && mx <= testBtnX2 + 28 && my >= testBtnY2 && my <= testBtnY2 + 12) {
         music.runCheck();
         return true;
      }

      int btnSz = 22;
      int spacing = 28;
      int ctrlStartX = this.wx + (264 - spacing * 3) / 2;

      for (int i = 0; i < 3; i++) {
         int bx = ctrlStartX + i * spacing;
         if (mx >= bx && mx <= bx + btnSz && my >= this.htCtrlY && my <= this.htCtrlY + btnSz) {
            if (i == 0) {
               music.prev();
            } else if (i == 1) {
               music.togglePlayPause();
            } else {
               music.next();
            }

            return true;
         }
      }

      if (mx >= this.htSfX && mx <= this.htSfX + 20 && my >= this.htCtrlY + 2 && my <= this.htCtrlY + btnSz - 2) {
         music.shuffleSetting.setValue(!music.isShuffle());
         return true;
      }

      if (mx >= this.htRpX && mx <= this.htRpX + 20 && my >= this.htCtrlY + 2 && my <= this.htCtrlY + btnSz - 2) {
         music.repeatSetting.setValue(!music.isRepeat());
         return true;
      }

      if (mx >= this.volBarX - 4 && mx <= this.volBarX + this.volBarW + 4 && my >= this.htVolY - 4 && my <= this.htVolY + 9) {
         this.volDragging = true;
         music.setVolume(Mth.clamp((float)(mx - this.volBarX) / this.volBarW, 0.0F, 1.0F));
         return true;
      }

      int eyQ = this.htQueueY - this.queueScroll;

      for (int ix = 0; ix < music.getQueueSize(); ix++) {
         if (my >= eyQ && my <= eyQ + 16) {
            int rmX = this.wx + 264 - 20;
            if (mx >= rmX && mx <= rmX + 16) {
               music.removeAt(ix);
            } else {
               music.playAt(ix);
            }

            return true;
         }

         eyQ += 16;
      }

      if (my >= this.htAddY && my <= this.htAddY + 18) {
         if (mx >= this.htAddBtnX && mx <= this.htAddBtnX + 18) {
            this.commitInput(music);
         } else {
            this.addTyping = true;
         }

         return true;
      } else {
         this.addTyping = false;
         return super.mouseClicked(click, doubled);
      }
   }

   public boolean mouseReleased(MouseButtonEvent click) {
      if (click.button() == 0) {
         this.dragging = false;
         this.volDragging = false;
      }

      return super.mouseReleased(click);
   }

   public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
      SrMusicModule music = SrMusicModule.INSTANCE;
      if (my >= this.htQueueY && my <= this.htQueueY + this.htQueueH && music != null) {
         int maxScroll = Math.max(0, music.getQueueSize() * 16 - this.htQueueH);
         this.queueScroll = (int)Mth.clamp(this.queueScroll - vAmt * 14.0, 0.0, maxScroll);
         return true;
      } else {
         return super.mouseScrolled(mx, my, hAmt, vAmt);
      }
   }

   public boolean keyPressed(KeyEvent input) {
      int key = input.input();
      if (this.addTyping) {
         if (key == 256) {
            this.addTyping = false;
            return true;
         }

         if (key == 259) {
            if (!this.addInput.isEmpty()) {
               this.addInput = this.addInput.substring(0, this.addInput.length() - 1);
            }

            return true;
         } else {
            if (key != 257 && key != 335) {
               return true;
            }

            SrMusicModule m = SrMusicModule.INSTANCE;
            if (m != null) {
               this.commitInput(m);
            }

            return true;
         }
      } else {
         if (key == 256) {
            this.onClose();
            return true;
         }

         if (key == 32) {
            SrMusicModule m = SrMusicModule.INSTANCE;
            if (m != null) {
               m.togglePlayPause();
               return true;
            }
         }

         if (key == 263) {
            SrMusicModule m = SrMusicModule.INSTANCE;
            if (m != null) {
               m.prev();
               return true;
            }
         }

         if (key == 262) {
            SrMusicModule m = SrMusicModule.INSTANCE;
            if (m != null) {
               m.next();
               return true;
            }
         }

         return super.keyPressed(input);
      }
   }

   public boolean charTyped(CharacterEvent input) {
      if (this.addTyping && this.addInput.length() < 300) {
         this.addInput = this.addInput + input.codepointAsString();
         return true;
      } else {
         return super.charTyped(input);
      }
   }

   public void onClose() {
      super.onClose();
   }

   public boolean isPauseScreen() {
      return false;
   }

   private void commitInput(SrMusicModule music) {
      String in = this.addInput.trim();
      if (!in.isEmpty()) {
         if (!in.contains("list=") && !in.contains("/playlist")) {
            music.addToQueue(in);
         } else {
            music.addPlaylist(in);
         }

         this.addInput = "";
         this.addTyping = false;
      }
   }

   private static int argb(int a, int r, int g, int b) {
      return a << 24 | r << 16 | g << 8 | b;
   }
}
