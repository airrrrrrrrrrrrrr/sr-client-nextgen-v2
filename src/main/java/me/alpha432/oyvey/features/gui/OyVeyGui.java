package me.alpha432.oyvey.features.gui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.gui.items.Item;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import me.alpha432.oyvey.features.script.LuaScript;
import me.alpha432.oyvey.features.script.LuaScriptSetting;
import me.alpha432.oyvey.features.settings.Bind;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.fabricmc.loader.api.FabricLoader;
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

public class OyVeyGui extends Screen {
   private static OyVeyGui INSTANCE;
   private static Color colorClipboard;
   private static Module.Category selectedCategory;
   private static float px;
   private static float py;
   private static final int PANEL_W = 480;
   private static final int PANEL_H = 310;
   private static final int SIDEBAR_W = 92;
   private static final int HEADER_H = 28;
   private static final int CAT_H = 20;
   private static final int ENTRY_H = 48;
   private static final int SETTING_H = 26;
   private static final int PKS = 100;
   private static final int PKB = 8;
   private static Setting<?> draggingSlider;
   private static Module settingsModule;
   private static boolean scriptsView = false;
   private static LuaScript scriptSettingsTarget = null;
   private static LuaScriptSetting scriptSliderDrag = null;
   private static int scriptSliderBarX;
   private static int scriptSliderBarW;
   private static Setting<Color> colorPicker;
   private static String searchQuery = "";
   private static final int C_PANEL = -737801188;
   private static final int C_SIDEBAR = -670824432;
   private static final int C_HEADER = -670560738;
   private static final int C_SEP = 687865855;
   private static final int C_BORDER = 687865855;
   private static final int C_HOVER = 419430399;
   private static final int C_DRAG = -2131824914;
   private static final int C_TEXT = -3155741;
   private static final int C_SUBTEXT = -12036762;
   private int scrollOffset = 0;
   private int settingsScroll = 0;
   private Setting<?> capturingBind = null;
   private int sliderBarX = 0;
   private int sliderBarW = 0;
   private int colorPickerX = 0;
   private int colorPickerY = 0;
   private float[] pickerHsb = new float[3];
   private boolean pickerDragSV = false;
   private boolean pickerDragH = false;
   private boolean pickerDragA = false;
   private long openTime = 0L;
   private boolean configsOpen = false;
   private List<String> configList = new ArrayList<>();
   private String configSaveName = "";
   private boolean configTyping = false;
   private int configScroll = 0;
   private static final Path CFG_DIR = FabricLoader.getInstance().getGameDir().resolve("oyvey").resolve("configs");
   private static final int SCR_MH = 20;
   private static final int SCR_SH = 18;
   private static final int SCR_CB = 7;

   public OyVeyGui() {
      super(Component.literal("sr client"));
      INSTANCE = this;

      for (Module.Category cat : OyVey.moduleManager.getCategories()) {
         if (cat != Module.Category.HUD) {
            selectedCategory = cat;
            break;
         }
      }
   }

   public void init() {
      super.init();
      this.openTime = System.currentTimeMillis();
   }

   public static OyVeyGui getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new OyVeyGui();
      }

      return INSTANCE;
   }

   public static Screen getClickGui() {
      OyVeyGui gui = getInstance();
      gui.openTime = System.currentTimeMillis();
      return gui;
   }

   public static void setColorClipboard(Color color) {
      colorClipboard = color;
   }

   public static Color getColorClipboard() {
      return colorClipboard;
   }

   public ArrayList<Widget> getComponents() {
      return new ArrayList<>();
   }

   public int getTextOffset() {
      return -6;
   }

   public void extractBackground(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
   }

   public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
      ClickGuiModule cgm = ClickGuiModule.getInstance();
      float scale = cgm != null ? cgm.guiScale.getValue() : 1.0F;
      int cx = this.width / 2;
      int cy = this.height / 2;
      int smx = (int)((mx - cx) / scale + cx);
      int smy = (int)((my - cy) / scale + cy);
      Item.context = ctx;
      px = (this.width - 480) / 2.0F;
      py = (this.height - 310) / 2.0F;
      if (draggingSlider != null) {
         long win = Minecraft.getInstance().getWindow().handle();
         if (GLFW.glfwGetMouseButton(win, 0) == 1) {
            this.applySliderDrag(smx);
         } else {
            draggingSlider = null;
         }
      }

      float anim = this.openTime == 0L ? 1.0F : Mth.clamp((float)(System.currentTimeMillis() - this.openTime) / 280.0F, 0.0F, 1.0F);
      float eased = anim * anim * (3.0F - 2.0F * anim);
      ctx.fill(0, 0, this.width, this.height, argb((int)(187.0F * eased), 2, 4, 8));
      ctx.pose().pushMatrix();
      ctx.pose().translate(cx, cy);
      ctx.pose().scale(scale, scale);
      ctx.pose().translate(-cx, -cy);
      float slideY = (1.0F - eased) * 12.0F;
      ctx.pose().translate(0.0F, slideY);
      int acc = OyVey.colorManager.getColorAsInt();
      int aR = acc >> 16 & 0xFF;
      int aG = acc >> 8 & 0xFF;
      int aB = acc & 0xFF;
      float time = (float)(System.currentTimeMillis() - this.openTime) / 1000.0F;
      float pulse = (float)(Math.sin(time * 1.4) * 0.5 + 0.5);
      int ipx = (int)px;
      int ipy = (int)py;
      RenderUtil.gradient(ctx, ipx - 60, ipy + 310 - 180, ipx + 260, ipy + 310 + 70, 0, argb((int)(90.0F * eased), aR / 2, aG / 2, aB / 2), 0, 0);
      RenderUtil.gradient(ctx, ipx + 480 - 200, ipy - 60, ipx + 480 + 60, ipy + 200, 0, 0, 0, argb((int)(65.0F * eased), aR / 3, aG / 3, aB / 3));
      RenderUtil.gradient(
         ctx, ipx + 240 - 140, ipy + 310 - 20, ipx + 240 + 140, ipy + 310 + 80, 0, argb((int)((15.0F + pulse * 20.0F) * eased), aR / 2, aG / 2, aB / 2), 0, 0
      );
      RenderUtil.dropShadow(ctx, ipx, ipy, 480, 310, 9.0F, 18.0F, (int)(125.0F * eased));
      RenderUtil.roundedRectGradient(ctx, ipx, ipy, 480, 310, 9, blendAlpha(-670560738, eased), blendAlpha(-804910052, eased), true);
      RenderUtil.roundedRect(ctx, ipx, ipy, 92, 310, 9, blendAlpha(-670824432, eased));
      ctx.fill(ipx + 46, ipy, ipx + 92, ipy + 310, blendAlpha(-670824432, eased));
      RenderUtil.roundedRectGradient(ctx, ipx, ipy, 480, 28, 9, blendAlpha(-602914530, eased), blendAlpha(-737800995, eased), false);
      ctx.fill(ipx, ipy + 14, ipx + 480, ipy + 28, blendAlpha(-737800995, eased));
      RenderUtil.horizontalGradient(ctx, ipx, ipy, ipx + 240, ipy + 28, new Color(aR, aG, aB, (int)(18.0F * eased)), new Color(0, 0, 0, 0));
      ctx.fill(ipx, ipy + 28 - 1, ipx + 480, ipy + 28, argb((int)(70.0F * eased), aR, aG, aB));
      ctx.fill(ipx + 92, ipy + 28, ipx + 92 + 1, ipy + 310, argb((int)(40.0F * eased), 255, 255, 255));
      RenderUtil.roundedOutline(ctx, ipx, ipy, 480, 310, 9, 1.0F, argb((int)(85.0F * eased), 255, 255, 255));
      RenderUtil.roundedRect(ctx, ipx + 14, ipy + 3, 54, 2, 1, 0xFF000000 | acc);
      Font font = Minecraft.getInstance().font;
      ctx.text(font, "SR CLIENT", ipx + 14, ipy + 10, 0xFF000000 | acc, true);
      long enCount = OyVey.moduleManager.getModules().stream().filter(m -> m.isEnabled() && !m.hidden).count();
      ctx.text(font, "\u00a78[\u00a7f" + enCount + "\u00a78]", ipx + 14 + font.width("SR CLIENT") + 5, ipy + 10, -1, false);
      if (settingsModule == null) {
         int sw = 190;
         int sh = 20;
         int sx = ipx + 240 - sw / 2;
         int sy = ipy + (28 - sh) / 2;
         RenderUtil.roundedRect(ctx, sx, sy, sw, sh, 6, argb((int)(32.0F * eased), 255, 255, 255));
         RenderUtil.roundedOutline(ctx, sx, sy, sw, sh, 6, 1.0F, argb((int)(56.0F * eased), 255, 255, 255));
         boolean cur = System.currentTimeMillis() % 1000L < 500L;
         String disp = searchQuery.isEmpty() ? "\u00a78search modules..." : searchQuery + (cur ? "\u00a77|" : "");
         ctx.text(font, disp, sx + 8, sy + 6, searchQuery.isEmpty() ? -13090736 : -1118482, false);
      }

      int catY = ipy + 28 + 8;

      for (Module.Category cat : OyVey.moduleManager.getCategories()) {
         if (cat != Module.Category.HUD) {
            boolean sel = cat == selectedCategory;
            boolean hov = !sel && settingsModule == null && smx >= ipx + 6 && smx <= ipx + 92 - 6 && smy >= catY && smy <= catY + 20;
            if (sel) {
               RenderUtil.roundedRect(ctx, ipx + 6, catY, 80, 20, 3, argb(180, aR / 4, aG / 4, aB / 4));
               ctx.fill(ipx + 6, catY + 3, ipx + 8, catY + 20 - 3, 0xFF000000 | acc);
            } else if (hov) {
               RenderUtil.roundedRect(ctx, ipx + 6, catY, 80, 20, 3, 419430399);
            }

            String label = ClickGuiModule.t(cat.getName());
            int lx = ipx + 6 + (80 - font.width(label)) / 2;
            int ly = catY + 6;
            ctx.text(font, label, lx, ly, sel ? 0xFF000000 | acc : (hov ? -3155741 : -12036762), sel);
            long cntOn = OyVey.moduleManager.getModulesByCategory(cat).stream().filter(m -> m.isEnabled() && !m.hidden).count();
            if (cntOn > 0L) {
               int bx2 = ipx + 92 - 18;
               int by2 = catY + 5;
               RenderUtil.roundedRect(ctx, bx2, by2, 12, 9, 2, argb(170, aR / 2, aG / 2, aB / 2));
               String cnt = String.valueOf(cntOn);
               ctx.text(font, cnt, bx2 + (12 - font.width(cnt)) / 2, by2 + 1, 0xFF000000 | acc, false);
            }

            catY += 22;
         }
      }

      if (OyVey.scriptManager != null) {
         boolean selScr = scriptsView;
         boolean hovScr = !selScr && settingsModule == null && smx >= ipx + 6 && smx <= ipx + 92 - 6 && smy >= catY && smy <= catY + 20;
         if (selScr) {
            RenderUtil.roundedRect(ctx, ipx + 6, catY, 80, 20, 3, argb(180, aR / 4, aG / 4, aB / 4));
            ctx.fill(ipx + 6, catY + 3, ipx + 8, catY + 20 - 3, 0xFF000000 | acc);
         } else if (hovScr) {
            RenderUtil.roundedRect(ctx, ipx + 6, catY, 80, 20, 3, 419430399);
         }

         String scrLabel = ClickGuiModule.t("Scripts");
         int lxS = ipx + 6 + (80 - font.width(scrLabel)) / 2;
         int lyS = catY + 6;
         ctx.text(font, scrLabel, lxS, lyS, selScr ? 0xFF000000 | acc : (hovScr ? -3155741 : -12036762), selScr);
         long activeCnt = OyVey.scriptManager.getScripts().stream().filter(LuaScript::isEnabled).count();
         if (activeCnt > 0L) {
            int bx2 = ipx + 92 - 18;
            int by2 = catY + 5;
            RenderUtil.roundedRect(ctx, bx2, by2, 12, 9, 2, argb(170, aR / 2, aG / 2, aB / 2));
            String cnt = String.valueOf(activeCnt);
            ctx.text(font, cnt, bx2 + (12 - font.width(cnt)) / 2, by2 + 1, 0xFF000000 | acc, false);
         }
      }

      int contentX = ipx + 92 + 1;
      int contentW = 387;
      int contentTop = ipy + 28;
      int contentH = 282;
      if (scriptsView && OyVey.scriptManager != null) {
         if (scriptSliderDrag != null) {
            long win = Minecraft.getInstance().getWindow().handle();
            if (GLFW.glfwGetMouseButton(win, 0) == 1) {
               this.applyOyScriptSlider(smx);
            } else {
               scriptSliderDrag = null;
            }
         }

         this.renderScriptsView(ctx, smx, smy, contentX, contentW, contentTop, contentH, aR, aG, aB);
      } else if (settingsModule != null) {
         this.renderSettingsPanel(ctx, smx, smy, contentX, contentW, contentTop, contentH, aR, aG, aB);
      } else {
         this.renderModuleList(ctx, smx, smy, contentX, contentW, contentTop, contentH, aR, aG, aB, time);
      }

      if (colorPicker != null && settingsModule != null) {
         this.renderColorPicker(ctx, smx, smy);
      }

      this.renderConfigsButton(ctx, smx, smy);
      if (this.configsOpen) {
         this.renderConfigsPanel(ctx, smx, smy);
      }

      ctx.pose().popMatrix();
   }

   private void renderModuleList(
      GuiGraphicsExtractor ctx, int mx, int my, int contentX, int contentW, int contentTop, int contentH, int aR, int aG, int aB, float time
   ) {
      for (int gx = contentX + 14; gx < contentX + contentW - 1; gx += 20) {
         for (int gy = contentTop + 14; gy < contentTop + contentH - 1; gy += 20) {
            ctx.fill(gx, gy, gx + 1, gy + 1, argb(14, aR, aG, aB));
         }
      }

      ctx.enableScissor(contentX, contentTop, contentX + contentW, contentTop + contentH);
      RenderUtil.verticalGradient(ctx, contentX, contentTop, contentX + contentW, contentTop + 8, new Color(0, 0, 0, 40), new Color(0, 0, 0, 0));
      RenderUtil.verticalGradient(
         ctx, contentX, contentTop + contentH - 8, contentX + contentW, contentTop + contentH, new Color(0, 0, 0, 0), new Color(0, 0, 0, 40)
      );
      List<Module> mods = this.getFilteredModules();
      int ey = contentTop + 6 - this.scrollOffset;

      for (Module mod : mods) {
         if (ey + 48 >= contentTop && ey <= contentTop + contentH) {
            boolean enabled = mod.isEnabled();
            boolean hov = mx >= contentX + 4 && mx <= contentX + contentW - 4 && my >= ey + 2 && my <= ey + 48 - 2;
            RenderUtil.roundedRectGradient(ctx, contentX + 4, ey + 2, contentW - 8, 44, 7, enabled ? -1089726424 : -1945499608, enabled ? -1291059096 : -2113265752, true);
            if (hov) {
               RenderUtil.roundedRect(ctx, contentX + 4, ey + 2, contentW - 8, 44, 7, 419430399);
            }

            RenderUtil.roundedOutline(ctx, contentX + 4, ey + 2, contentW - 8, 44, 7, 1.0F, enabled ? argb(65, aR, aG, aB) : 318767103);
            if (enabled) {
               int acc2 = OyVey.colorManager.getColorWithAlpha(ey, 255);
               RenderUtil.roundedRect(ctx, contentX + 7, ey + 8, 3, 32, 1.5F, acc2);
            }

            ctx.text(Minecraft.getInstance().font, mod.getName(), contentX + 14, ey + 11, enabled ? -1 : -3155741, enabled);
            String desc = mod.getDescription();
            if (desc != null && !desc.isEmpty()) {
               String d = desc;
               int maxW = contentW - 70;

               while (Minecraft.getInstance().font.width(d) > maxW && d.length() > 3) {
                  d = d.substring(0, d.length() - 4) + "...";
               }

               ctx.text(Minecraft.getInstance().font, d, contentX + 14, ey + 26, -12036762, false);
            }

            int tx = contentX + contentW - 42;
            int ty = ey + 24 - 6;
            if (enabled) {
               int acc2 = OyVey.colorManager.getColorWithAlpha(ey, 200);
               renderToggle(ctx, tx, ty, 24, 12, true, acc2);
            } else {
               renderToggle(ctx, tx, ty, 24, 12, false, 872415231);
            }

            if (hov) {
               ctx.text(Minecraft.getInstance().font, "\u00a78right-click: settings", contentX + 14, ey + 48 - 12, 0, false);
            }
         }

         ey += 48;
      }

      ctx.disableScissor();
   }

   private void renderScriptsView(GuiGraphicsExtractor ctx, int mx, int my, int cx, int cw, int ctop, int ch, int aR, int aG, int aB) {
      Font font = Minecraft.getInstance().font;
      List<LuaScript> scripts = OyVey.scriptManager.getScripts();
      ctx.enableScissor(cx, ctop, cx + cw, ctop + ch);
      int ey = ctop + 4 - this.scrollOffset;
      if (scripts.isEmpty()) {
         String msg = "No scripts in: oyvey/scripts/";
         ctx.text(font, msg, cx + (cw - font.width(msg)) / 2, ctop + ch / 2 - 4, -12298906, false);
         ctx.disableScissor();
      } else {
         for (LuaScript s : scripts) {
            boolean on = s.isEnabled();
            boolean hov = mx >= cx && mx < cx + cw && my >= ey && my < ey + 20;
            boolean sel = scriptSettingsTarget == s;
            if (sel || on && !sel) {
               RenderUtil.roundedRect(ctx, cx + 4, ey + 1, cw - 8, 18, 5, sel ? argb(60, aR, aG, aB) : argb(30, aR, aG, aB));
            } else if (hov) {
               RenderUtil.roundedRect(ctx, cx + 4, ey + 1, cw - 8, 18, 5, 285212671);
            }

            if (on) {
               RenderUtil.roundedRect(ctx, cx + 5, ey + 5, 2, 10, 1, 0xFF000000 | argb(255, aR, aG, aB));
            }

            ctx.text(font, s.getName(), cx + 8, ey + 4, on ? 0xFF000000 | argb(255, aR, aG, aB) : -3155741, on);
            if (!s.getDescription().isEmpty()) {
               ctx.text(font, s.getDescription(), cx + 8 + font.width(s.getName()) + 4, ey + 4, -12298906, false);
            }

            int cbX = cx + cw - 7 - 6;
            int cbY = ey + 6;
            RenderUtil.roundedRect(ctx, cbX, cbY, 7, 7, 2, on ? argb(255, aR, aG, aB) : -16382448);
            RenderUtil.roundedOutline(ctx, cbX, cbY, 7, 7, 2, 1.0F, -15066584);

            if (!s.getScriptSettings().isEmpty()) {
               String g = sel ? "v" : ">";
               ctx.text(font, g, cbX - font.width(g) - 3, ey + 6, -12298906, false);
            }

            ctx.fill(cx + 6, ey + 20 - 1, cx + cw - 6, ey + 20, 369098751);
            ey += 20;
            if (sel) {
               for (LuaScriptSetting ss : s.getScriptSettings()) {
                  boolean sHov = mx >= cx && mx < cx + cw && my >= ey && my < ey + 18;
                  RenderUtil.roundedRect(ctx, cx + 4, ey + 1, cw - 8, 16, 4, sHov ? 419430399 : 150994943);
                  RenderUtil.roundedRect(ctx, cx + 5, ey + 4, 2, 10, 1, 687865855);
                  ctx.text(font, ss.getName(), cx + 10, ey + 5, -7824982, false);
                  int re = cx + cw - 4;
                  switch (ss.getType()) {
                     case BOOLEAN: {
                        int bx = re - 7;
                        int by = ey + 5;
                        RenderUtil.roundedRect(ctx, bx, by, 7, 7, 2, ss.getBoolean() ? argb(255, aR, aG, aB) : -16382448);
                        RenderUtil.roundedOutline(ctx, bx, by, 7, 7, 2, 1.0F, -15066584);
                        break;
                     }
                     case SLIDER: {
                        double pct = (ss.getDouble() - ss.getMin()) / (ss.getMax() - ss.getMin());
                        int bx = cx + 6;
                        int bw = cw - 12;
                        int barY = ey + 18 - 4;
                        RenderUtil.roundedRect(ctx, bx, barY, bw, 2, 1, -15066584);
                        RenderUtil.roundedRect(ctx, bx, barY, (int)(bw * pct), 2, 1, argb(200, aR, aG, aB));
                        String val = String.format("%.2f", ss.getDouble());
                        ctx.text(font, val, re - font.width(val), ey + 5, -10061944, false);
                        break;
                     }
                     case ENUM:
                        String cur = ss.getString();
                        ctx.text(font, cur, re - font.width(cur), ey + 5, argb(220, aR, aG, aB), false);
                  }

                  ctx.fill(cx + 6, ey + 18 - 1, cx + cw - 6, ey + 18, 285212671);
                  ey += 18;
               }
            }
         }

         int rlY = ctop + ch - 18;
         boolean rlHov = mx >= cx + 4 && mx < cx + 80 && my >= rlY && my < rlY + 14;
         RenderUtil.roundedRect(ctx, cx + 4, rlY, 76, 14, 5, rlHov ? 816491263 : 413838079);
         RenderUtil.roundedOutline(ctx, cx + 4, rlY, 76, 14, 5, 1.0F, 369098751);
         ctx.text(font, ClickGuiModule.t("[ Reload Scripts ]"), cx + 8, rlY + 3, -8939077, false);
         ctx.disableScissor();
      }
   }

   private void applyOyScriptSlider(int mx) {
      if (scriptSliderDrag != null) {
         float pct = Math.max(0.0F, Math.min(1.0F, (float)(mx - scriptSliderBarX) / scriptSliderBarW));
         scriptSliderDrag.setDoubleValue(scriptSliderDrag.getMin() + pct * (scriptSliderDrag.getMax() - scriptSliderDrag.getMin()));
      }
   }

   private void renderSettingsPanel(GuiGraphicsExtractor ctx, int mx, int my, int cx, int cw, int cy, int ch, int aR, int aG, int aB) {
      int acc = OyVey.colorManager.getColorAsInt();
      RenderUtil.roundedRectGradient(ctx, cx + 3, cy + 3, cw - 6, ch - 6, 7, -267973869, -401929709, true);
      int headerH = 38;
      RenderUtil.roundedRectGradient(ctx, cx + 3, cy + 3, cw - 6, headerH - 3, 7, -200996336, -335214576, true);
      ctx.fill(cx + 3, cy + headerH - 7, cx + cw - 3, cy + headerH, -335214576);
      RenderUtil.roundedRect(ctx, cx + 14, cy + 4, 64, 2, 1, 0xFF000000 | acc);
      ctx.fill(cx, cy + headerH - 1, cx + cw, cy + headerH, argb(50, aR, aG, aB));
      boolean backHov = mx >= cx + 8 && mx <= cx + 30 && my >= cy + 8 && my <= cy + headerH - 8;
      RenderUtil.roundedRect(ctx, cx + 8, cy + 8, 22, headerH - 16, 3, backHov ? 1157627903 : 587202559);
      ctx.text(Minecraft.getInstance().font, "<", cx + 14, cy + 15, -4473925, false);
      ctx.text(Minecraft.getInstance().font, settingsModule.getName(), cx + 38, cy + 11, -1, true);
      String desc = settingsModule.getDescription();
      if (desc != null && !desc.isEmpty()) {
         ctx.text(Minecraft.getInstance().font, desc, cx + 38, cy + 23, -12036762, false);
      }

      boolean enabled = settingsModule.isEnabled();
      int tx = cx + cw - 44;
      int ty = cy + headerH / 2 - 5;
      if (enabled) {
         renderToggle(ctx, tx, ty, 22, 11, true, OyVey.colorManager.getColorWithAlpha(ty, 200));
      } else {
         renderToggle(ctx, tx, ty, 22, 11, false, 872415231);
      }

      ctx.text(
         Minecraft.getInstance().font,
         enabled ? "\u00a7aON" : "\u00a7cOFF",
         tx - Minecraft.getInstance().font.width(enabled ? "ON" : "OFF") - 6,
         cy + 15,
         enabled ? -11149995 : -2271915,
         false
      );
      List<Setting<?>> settings = this.getVisibleSettings();
      ctx.enableScissor(cx, cy + headerH, cx + cw, cy + ch);
      int sy = cy + headerH + 6 - this.settingsScroll;

      for (Setting<?> s : settings) {
         if (sy + 26 >= cy + headerH && sy <= cy + ch) {
            this.renderSettingRow(ctx, mx, my, s, cx, sy, cw);
         }

         sy += 27;
      }

      if (settings.isEmpty()) {
         String msg = "No configurable settings";
         ctx.text(Minecraft.getInstance().font, msg, cx + (cw - Minecraft.getInstance().font.width(msg)) / 2, cy + headerH + 20, -12036762, false);
      }

      ctx.disableScissor();
   }

   private void renderSettingRow(GuiGraphicsExtractor ctx, int mx, int my, Setting<?> s, int cx, int sy, int cw) {
      boolean hov = mx >= cx && mx <= cx + cw && my >= sy && my <= sy + 26;
      if (hov) {
         RenderUtil.roundedRect(ctx, cx + 5, sy + 2, cw - 10, 22, 6, 419430399);
      }
      ctx.fill(cx + 8, sy + 26, cx + cw - 8, sy + 26 + 1, 318767103);
      ctx.text(Minecraft.getInstance().font, s.getName(), cx + 14, sy + 9, -3155741, false);
      if (s.getValue() instanceof Boolean bool) {
         int tx = cx + cw - 38;
         int ty = sy + 13 - 5;
         if (bool) {
            renderToggle(ctx, tx, ty, 22, 11, true, OyVey.colorManager.getColorWithAlpha(sy, 200));
         } else {
            renderToggle(ctx, tx, ty, 22, 11, false, 872415231);
         }
      } else if (s.isNumberSetting() && s.hasRestriction()) {
         float min = ((Number)s.getMin()).floatValue();
         float max = ((Number)s.getMax()).floatValue();
         float val = ((Number)s.getValue()).floatValue();
         float pct = Math.max(0.0F, Math.min(1.0F, (val - min) / (max - min)));
         int barX = cx + (int)(cw * 0.42F);
         int barW = (int)(cw * 0.38F);
         int barY = sy + 13 - 3;
         RenderUtil.roundedRect(ctx, barX, barY, barW, 6, 3, 872415231);
         RenderUtil.roundedRect(ctx, barX, barY, (int)(barW * pct), 6, 3, OyVey.colorManager.getColorWithAlpha(sy, 200));
         int kx = barX + (int)(barW * pct) - 3;
         RenderUtil.roundedRect(ctx, kx, barY - 2, 6, 10, 3, -1);
         String vs = s.getValue() instanceof Integer ? String.valueOf((int)val) : String.format("%.2f", val);
         ctx.text(Minecraft.getInstance().font, vs, cx + cw - 8 - Minecraft.getInstance().font.width(vs), sy + 9, -7829368, false);
      } else if (s.isEnumSetting()) {
         String ev = "< " + s.currentEnumName() + " >";
         ctx.text(Minecraft.getInstance().font, ev, cx + cw - 10 - Minecraft.getInstance().font.width(ev), sy + 9, -10053172, false);
      } else if (s.getValue() instanceof Bind bind) {
         boolean cap = s == this.capturingBind;
         String bs = cap ? "Press a key..." : "[" + bind.toString() + "]";
         ctx.text(
            Minecraft.getInstance().font,
            bs,
            cx + cw - 10 - Minecraft.getInstance().font.width(bs),
            sy + 9,
            cap ? OyVey.colorManager.getColorWithAlpha(sy, 255) : -8943463,
            false
         );
      } else if (s.isColorSetting()) {
         Color c = (Color)s.getValue();
         int swX = cx + cw - 42;
         int swY = sy + 5;
         int swW = 30;
         int swH = 16;
         ctx.fill(swX, swY, swX + swW / 2, swY + swH / 2, -5592406);
         ctx.fill(swX + swW / 2, swY, swX + swW, swY + swH / 2, -10066330);
         ctx.fill(swX, swY + swH / 2, swX + swW / 2, swY + swH, -10066330);
         ctx.fill(swX + swW / 2, swY + swH / 2, swX + swW, swY + swH, -5592406);
         RenderUtil.roundedRect(ctx, swX, swY, swW, swH, 5, c.getRGB());
         RenderUtil.roundedOutline(ctx, swX, swY, swW, swH, 5, 1.0F, 1627389951);
      }
   }

   private static void renderToggle(GuiGraphicsExtractor ctx, int x, int y, int w, int h, boolean enabled, int color) {
      RenderUtil.roundedRect(ctx, x, y, w, h, h / 2.0F, color);
      int knob = h - 2;
      RenderUtil.roundedRect(ctx, enabled ? x + w - knob - 1 : x + 1, y + 1, knob, knob, knob / 2.0F, enabled ? -1 : -10854288);
      RenderUtil.roundedOutline(ctx, x, y, w, h, h / 2.0F, 1.0F, enabled ? 553648127 : 369098751);
   }

   private static int argb(int a, int r, int g, int b) {
      return Mth.clamp(a, 0, 255) << 24 | r << 16 | g << 8 | b;
   }

   private static int blendAlpha(int color, float t) {
      int a = (int)((color >>> 24 & 0xFF) * t);
      return a << 24 | color & 16777215;
   }

   private List<Module> getFilteredModules() {
      List<Module> out = new ArrayList<>();
      if (searchQuery.isEmpty()) {
         if (selectedCategory != null) {
            OyVey.moduleManager.getModulesByCategory(selectedCategory).stream().filter(m -> !m.hidden).forEach(out::add);
         }
      } else {
         String q = searchQuery.toLowerCase();
         OyVey.moduleManager.getModules().stream().filter(m -> !m.hidden && m.getName().toLowerCase().contains(q)).forEach(out::add);
      }

      return out;
   }

   private List<Setting<?>> getVisibleSettings() {
      return settingsModule == null
         ? new ArrayList<>()
         : settingsModule.getSettings()
            .stream()
            .filter(Setting::isVisible)
            .filter(s -> !s.getName().equalsIgnoreCase("Enabled") && !s.getName().equalsIgnoreCase("Drawn") && !s.getName().equalsIgnoreCase("DisplayName"))
            .collect(Collectors.toList());
   }

   private void applySliderDrag(int mx) {
      if (draggingSlider != null) {
         float pct = Math.max(0.0F, Math.min(1.0F, (float)(mx - this.sliderBarX) / this.sliderBarW));
         float min = ((Number)draggingSlider.getMin()).floatValue();
         float max = ((Number)draggingSlider.getMax()).floatValue();
         float newVal = min + pct * (max - min);
         Object v = draggingSlider.getValue();
         if (v instanceof Integer) {
            ((Setting<Integer>)draggingSlider).setValueNoEvent((int)newVal);
         } else if (v instanceof Float) {
            ((Setting<Float>)draggingSlider).setValueNoEvent(newVal);
         } else if (v instanceof Double) {
            ((Setting<Double>)draggingSlider).setValueNoEvent((double)newVal);
         }
      }
   }

   private void triggerGameClick(int button) {
      if (this.minecraft != null && this.minecraft.player != null && this.minecraft.gameMode != null) {
         try {
            if (button == 0) {
               Method m = Minecraft.class.getDeclaredMethod("startAttack");
               m.setAccessible(true);
               m.invoke(this.minecraft);
            } else if (button == 1) {
               Method m = Minecraft.class.getDeclaredMethod("startUseItem");
               m.setAccessible(true);
               m.invoke(this.minecraft);
            }
         } catch (Exception var3) {
         }
      }
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      ClickGuiModule cgm2 = ClickGuiModule.getInstance();
      float sc2 = cgm2 != null ? cgm2.guiScale.getValue() : 1.0F;
      double mx = (click.x() - this.width / 2.0) / sc2 + this.width / 2.0;
      double my = (click.y() - this.height / 2.0) / sc2 + this.height / 2.0;
      int btn = click.button();
      if (mx < px || mx > px + 480.0F || my < py || my > py + 310.0F) {
         this.triggerGameClick(btn);
      }

      int contentX = (int)px + 92 + 1;
      int contentW = 387;
      int contentTop = (int)py + 28;
      int contentH = 282;
      if (colorPicker != null && settingsModule != null) {
         int bx = this.colorPickerX;
         int by = this.colorPickerY;
         int W = 100;
         int SV = 100;
         int BAR = 8;
         int ay = by + SV + 3;
         int hy = ay + BAR + 3;
         int totalH = SV + (BAR + 3) * 2 + 8;
         boolean inPicker = mx >= bx - 4 && mx <= bx + W + 4 && my >= by - 4 && my <= by + totalH;
         if (inPicker && btn == 0) {
            if (mx >= bx && mx <= bx + W) {
               if (my >= by && my <= by + SV) {
                  this.pickerDragSV = true;
                  return true;
               }

               if (my >= ay && my <= ay + BAR) {
                  this.pickerDragA = true;
                  return true;
               }

               if (my >= hy && my <= hy + BAR) {
                  this.pickerDragH = true;
                  return true;
               }
            }

            return true;
         }

         if (!inPicker) {
            colorPicker = null;
         }
      }

      if (settingsModule != null) {
         int headerH = 38;
         if (mx >= contentX + 8 && mx <= contentX + 30 && my >= contentTop + 8 && my <= contentTop + headerH - 8) {
            this.closeSettings();
            return true;
         }

         int tx = contentX + contentW - 44;
         int ty = contentTop + headerH / 2 - 5;
         if (mx >= tx && mx <= tx + 22 && my >= ty && my <= ty + 11) {
            settingsModule.toggle();
            return true;
         }

         List<Setting<?>> settings = this.getVisibleSettings();
         int sy = contentTop + headerH + 6 - this.settingsScroll;

         for (Setting<?> s : settings) {
            if (my >= sy && my <= sy + 26 && mx >= contentX && mx <= contentX + contentW) {
               this.handleSettingClick(s, mx, sy, contentX, contentW, btn);
               return true;
            }

            sy += 27;
         }

         if (mx < px || mx > px + 480.0F || my < py || my > py + 310.0F) {
            this.closeSettings();
         }

         return true;
      } else {
         int catY = (int)py + 28 + 8;

         for (Module.Category cat : OyVey.moduleManager.getCategories()) {
            if (cat != Module.Category.HUD) {
               if (mx >= (int)px + 6 && mx <= (int)px + 92 - 6 && my >= catY && my <= catY + 20) {
                  selectedCategory = cat;
                  scriptsView = false;
                  scriptSettingsTarget = null;
                  this.scrollOffset = 0;
                  return true;
               }

               catY += 22;
            }
         }

         if (OyVey.scriptManager != null && mx >= (int)px + 6 && mx <= (int)px + 92 - 6 && my >= catY && my <= catY + 20) {
            scriptsView = true;
            scriptSettingsTarget = null;
            this.scrollOffset = 0;
            return true;
         }

         if (scriptsView && OyVey.scriptManager != null && mx >= contentX && mx <= contentX + contentW && my >= contentTop && my <= contentTop + contentH) {
            int rlY = contentTop + contentH - 18;
            if (mx >= contentX + 4 && mx < contentX + 80 && my >= rlY && my < rlY + 14) {
               OyVey.scriptManager.reload();
               scriptSettingsTarget = null;
               return true;
            }

            int ey = contentTop + 4 - this.scrollOffset;

            for (LuaScript s : OyVey.scriptManager.getScripts()) {
               if (my >= ey && my < ey + 20) {
                  if (btn == 1) {
                     scriptSettingsTarget = scriptSettingsTarget == s ? null : s;
                  } else if (s.isEnabled()) {
                     s.disable();
                  } else {
                     s.enable();
                  }

                  return true;
               }

               ey += 20;
               if (scriptSettingsTarget == s) {
                  for (LuaScriptSetting ss : s.getScriptSettings()) {
                     if (my >= ey && my < ey + 18) {
                        if (ss.getType() == LuaScriptSetting.Type.BOOLEAN) {
                           ss.setBoolValue(!ss.getBoolean());
                        } else if (ss.getType() == LuaScriptSetting.Type.SLIDER) {
                           scriptSliderDrag = ss;
                           scriptSliderBarX = contentX + 6;
                           scriptSliderBarW = contentW - 12;
                           this.applyOyScriptSlider((int)mx);
                        } else if (ss.getType() == LuaScriptSetting.Type.ENUM) {
                           ss.cycleEnum();
                        }

                        return true;
                     }

                     ey += 18;
                  }
               }
            }

            return true;
         } else {
            if (mx >= contentX && mx <= contentX + contentW && my >= contentTop && my <= contentTop + contentH) {
               int ey = contentTop + 6 - this.scrollOffset;

               for (Module mod : this.getFilteredModules()) {
                  if (my >= ey && my <= ey + 48) {
                     if (btn == 1) {
                        this.openSettings(mod);
                     } else {
                        mod.toggle();
                     }

                     return true;
                  }

                  ey += 48;
               }
            }

            int cbtnX = 4;
            int cbtnY = this.height - 18;
            int cbtnW = 52;
            int cbtnH = 14;
            if (mx >= cbtnX && mx <= cbtnX + cbtnW && my >= cbtnY && my <= cbtnY + cbtnH) {
               this.configsOpen = !this.configsOpen;
               if (this.configsOpen) {
                  this.refreshConfigList();
                  this.configScroll = 0;
               }

               return true;
            } else {
               if (!this.configsOpen) {
                  return super.mouseClicked(click, doubled);
               }

               int margin = 20;
               int headerH = 24;
               int entryH = 18;
               int saveBarH = 36;
               int panW = this.width - margin * 2;
               int panH = this.height - margin * 2;
               int panX = margin;
               int closeW = 20;
               int closeH = 14;
               int closeX = margin + panW - closeW - 4;
               int closeY = margin + (headerH - closeH) / 2;
               if (mx >= closeX && mx <= closeX + closeW && my >= closeY && my <= closeY + closeH) {
                  this.configsOpen = false;
                  this.configTyping = false;
                  return true;
               }

               int listAreaH = panH - headerH - 4 - saveBarH;
               int maxVisible = listAreaH / entryH;
               int ey = margin + headerH + 4;
               int end = Math.min(this.configList.size(), this.configScroll + maxVisible);

               for (int i = this.configScroll; i < end; i++) {
                  String cfg = this.configList.get(i);
                  int lbX = panX + panW - 48;
                  if (mx >= lbX && mx <= lbX + 44 && my >= ey + 2 && my <= ey + entryH - 2) {
                     this.loadConfig(cfg);
                     this.configsOpen = false;
                     return true;
                  }

                  ey += entryH;
               }

               int saveY = margin + panH - saveBarH;
               int inputW = panW - 100;
               if (mx >= panX + 6 && mx <= panX + 6 + inputW && my >= saveY + 6 && my <= saveY + saveBarH - 6) {
                  this.configTyping = true;
                  return true;
               }

               int saveBtnX = panX + 6 + inputW + 6;
               int saveBtnW = panW - 6 - inputW - 18;
               if (mx >= saveBtnX && mx <= saveBtnX + saveBtnW && my >= saveY + 6 && my <= saveY + saveBarH - 6) {
                  if (!this.configSaveName.isBlank()) {
                     this.saveConfig(this.configSaveName.trim());
                     this.configSaveName = "";
                     this.configTyping = false;
                     this.refreshConfigList();
                  }

                  return true;
               } else {
                  if (mx < panX || mx > panX + panW || my < margin || my > margin + panH) {
                     this.configsOpen = false;
                     this.configTyping = false;
                  }

                  return true;
               }
            }
         }
      }
   }

   public boolean mouseReleased(MouseButtonEvent click) {
      if (click.button() == 0) {
         draggingSlider = null;
         this.pickerDragSV = false;
         this.pickerDragH = false;
         this.pickerDragA = false;
      }

      return super.mouseReleased(click);
   }

   private void handleSettingClick(Setting<?> s, double mx, int sy, int cx, int cw, int btn) {
      if (s.getValue() instanceof Boolean) {
         ((Setting<Boolean>)s).setValue(!(Boolean)s.getValue());
      } else if (s.isNumberSetting() && s.hasRestriction()) {
         int barX = cx + (int)(cw * 0.42F);
         int barW = (int)(cw * 0.38F);
         if (mx >= barX - 4 && mx <= barX + barW + 4) {
            draggingSlider = s;
            this.sliderBarX = barX;
            this.sliderBarW = barW;
            this.applySliderDrag((int)mx);
         }
      } else if (s.isEnumSetting()) {
         s.increaseEnum();
      } else if (s.getValue() instanceof Bind) {
         if (btn == 1) {
            ((Setting<Bind>)s).setValue(new Bind(-1));
            this.capturingBind = null;
         } else {
            this.capturingBind = this.capturingBind == s ? null : s;
         }
      } else if (s.isColorSetting()) {
         if (colorPicker == s) {
            colorPicker = null;
            return;
         }

         colorPicker = (Setting<Color>)s;
         Color c = (Color)s.getValue();
         this.pickerHsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
         this.colorPickerX = cx + cw - 100 - 12;
         this.colorPickerY = sy + 26 + 2;
         int totalH = 130;
         if (this.colorPickerY + totalH > (int)py + 310) {
            this.colorPickerY = sy - totalH - 4;
         }
      }
   }

   public boolean mouseScrolled(double rawMx, double rawMy, double hAmt, double vAmt) {
      ClickGuiModule cgm3 = ClickGuiModule.getInstance();
      float sc3 = cgm3 != null ? cgm3.guiScale.getValue() : 1.0F;
      double mx = (rawMx - this.width / 2.0) / sc3 + this.width / 2.0;
      double my = (rawMy - this.height / 2.0) / sc3 + this.height / 2.0;
      if (this.configsOpen) {
         int margin = 20;
         int headerH = 24;
         int entryH = 18;
         int saveBarH = 36;
         int panH = this.height - margin * 2;
         int listAreaH = panH - headerH - 4 - saveBarH;
         int maxScroll = Math.max(0, this.configList.size() - listAreaH / entryH);
         this.configScroll = Math.max(0, Math.min(maxScroll, this.configScroll - (int)Math.signum(vAmt)));
         return true;
      }

      int contentX = (int)px + 92 + 1;
      int contentW = 387;
      if (mx >= contentX && mx <= contentX + contentW) {
         if (settingsModule != null) {
            this.settingsScroll = Math.max(0, this.settingsScroll - (int)(vAmt * 12.0));
         } else if (my >= (int)py + 28 && my <= (int)py + 310) {
            this.scrollOffset = Math.max(0, this.scrollOffset - (int)(vAmt * 14.0));
         }
      }

      return super.mouseScrolled(mx, my, hAmt, vAmt);
   }

   public boolean keyPressed(KeyEvent input) {
      int key = input.input();
      if (this.configTyping) {
         if (key == 256) {
            this.configTyping = false;
            return true;
         } else if (key == 259 && !this.configSaveName.isEmpty()) {
            this.configSaveName = this.configSaveName.substring(0, this.configSaveName.length() - 1);
            return true;
         } else if ((key == 257 || key == 335) && !this.configSaveName.isBlank()) {
            this.saveConfig(this.configSaveName.trim());
            this.configSaveName = "";
            this.configTyping = false;
            this.refreshConfigList();
            return true;
         } else {
            return true;
         }
      } else if (this.capturingBind == null) {
         if (settingsModule == null && key == 259 && !searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            return true;
         } else if (key == 256 && settingsModule != null) {
            this.closeSettings();
            return true;
         } else {
            return super.keyPressed(input);
         }
      } else {
         Bind bind = key != 256 && key != 261 && key != 259 ? new Bind(key) : new Bind(-1);
         ((Setting<Bind>)this.capturingBind).setValue(bind);
         this.capturingBind = null;
         return true;
      }
   }

   public boolean charTyped(CharacterEvent input) {
      if (this.configTyping) {
         String c = input.codepointAsString();
         if (c.matches("[a-zA-Z0-9_\\-]") && this.configSaveName.length() < 24) {
            this.configSaveName = this.configSaveName + c;
         }

         return true;
      } else {
         if (this.capturingBind != null) {
            return true;
         }

         if (settingsModule == null) {
            searchQuery = searchQuery + input.codepointAsString();
            this.scrollOffset = 0;
         }

         return true;
      }
   }

   private void openSettings(Module mod) {
      settingsModule = mod;
      this.settingsScroll = 0;
      this.capturingBind = null;
      draggingSlider = null;
      colorPicker = null;
      searchQuery = "";
   }

   private void closeSettings() {
      settingsModule = null;
      this.settingsScroll = 0;
      this.capturingBind = null;
      draggingSlider = null;
      colorPicker = null;
   }

   private void renderColorPicker(GuiGraphicsExtractor ctx, int mx, int my) {
      if (colorPicker != null) {
         Color cur = colorPicker.getValue();
         Color hueCol = Color.getHSBColor(this.pickerHsb[0], 1.0F, 1.0F);
         int bx = this.colorPickerX;
         int by = this.colorPickerY;
         int W = 100;
         int SV = 100;
         int BAR = 8;
         int ay = by + SV + 3;
         int hy = ay + BAR + 3;
         int totalH = SV + (BAR + 3) * 2 + 8;
         RenderUtil.dropShadow(ctx, bx - 4, by - 4, W + 8, totalH + 4, 8, 10, 100);
         RenderUtil.roundedRectGradient(ctx, bx - 4, by - 4, W + 8, totalH + 4, 8, -1661075936, -233760736, true);
         RenderUtil.roundedOutline(ctx, bx - 4, by - 4, W + 8, totalH + 4, 8, 1.0F, 1442840575);
         RenderUtil.horizontalGradient(ctx, bx, by, bx + W, by + SV, Color.WHITE, hueCol);
         RenderUtil.verticalGradient(ctx, bx, by, bx + W, by + SV, new Color(0, 0, 0, 0), Color.BLACK);
         int svCx = Math.max(bx, Math.min(bx + W, bx + (int)(this.pickerHsb[1] * W)));
         int svCy = Math.max(by, Math.min(by + SV, by + (int)((1.0F - this.pickerHsb[2]) * SV)));
         RenderUtil.roundedRect(ctx, svCx - 3, svCy - 3, 6, 6, 3, -16777216);
         RenderUtil.roundedRect(ctx, svCx - 2, svCy - 2, 4, 4, 2, -1);
         ctx.fill(bx, ay, bx + W / 2, ay + BAR / 2, -5592406);
         ctx.fill(bx + W / 2, ay, bx + W, ay + BAR / 2, -8947849);
         ctx.fill(bx, ay + BAR / 2, bx + W / 2, ay + BAR, -8947849);
         ctx.fill(bx + W / 2, ay + BAR / 2, bx + W, ay + BAR, -5592406);
         RenderUtil.horizontalGradient(
            ctx,
            bx,
            ay,
            bx + W,
            ay + BAR,
            new Color(cur.getRed(), cur.getGreen(), cur.getBlue(), 0),
            new Color(cur.getRed(), cur.getGreen(), cur.getBlue(), 255)
         );
         int aPos = Math.max(bx, Math.min(bx + W, bx + (int)(cur.getAlpha() / 255.0F * W)));
         RenderUtil.roundedRect(ctx, aPos - 2, ay - 1, 4, BAR + 2, 2, -16777216);
         RenderUtil.roundedRect(ctx, aPos - 1, ay, 2, BAR, 1, -1);

         for (int i = 0; i < W; i++) {
            ctx.fill(bx + i, hy, bx + i + 1, hy + BAR, Color.getHSBColor((float)i / W, 1.0F, 1.0F).getRGB() | 0xFF000000);
         }

         int hPos = Math.max(bx, Math.min(bx + W, bx + (int)(this.pickerHsb[0] * W)));
         RenderUtil.roundedRect(ctx, hPos - 2, hy - 1, 4, BAR + 2, 2, -16777216);
         RenderUtil.roundedRect(ctx, hPos - 1, hy, 2, BAR, 1, -1);
         if (this.pickerDragSV) {
            this.pickerHsb[1] = Math.max(0.0F, Math.min(1.0F, (float)(mx - bx) / W));
            this.pickerHsb[2] = Math.max(0.0F, Math.min(1.0F, 1.0F - (float)(my - by) / SV));
            this.applyPickerHsb();
         }

         if (this.pickerDragA) {
            float a = Math.max(0.0F, Math.min(1.0F, (float)(mx - bx) / W));
            Color c = colorPicker.getValue();
            colorPicker.setValue(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(a * 255.0F)));
         }

         if (this.pickerDragH) {
            this.pickerHsb[0] = Math.max(0.0F, Math.min(1.0F, (float)(mx - bx) / W));
            this.applyPickerHsb();
         }
      }
   }

   private void applyPickerHsb() {
      Color c = Color.getHSBColor(this.pickerHsb[0], this.pickerHsb[1], this.pickerHsb[2]);
      int alpha = colorPicker.getValue().getAlpha();
      colorPicker.setValue(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
   }

   private void renderConfigsButton(GuiGraphicsExtractor ctx, int mx, int my) {
      int bx = 4;
      int by = this.height - 18;
      int bw = 52;
      int bh = 14;
      boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
      int acc = OyVey.colorManager.getColorWithAlpha(by, 190);
      RenderUtil.roundedRectGradient(ctx, bx, by, bw, bh, 5, this.configsOpen ? acc : (hov ? 1157627903 : -871099372), this.configsOpen ? OyVey.colorManager.getColorWithAlpha(by + bh, 140) : 587202559, true);
      RenderUtil.roundedOutline(ctx, bx, by, bw, bh, 5, 1.0F, this.configsOpen ? OyVey.colorManager.getColorWithAlpha(by, 220) : 872415231);
      String lbl = ClickGuiModule.t("Configs");
      ctx.text(Minecraft.getInstance().font, lbl, bx + (bw - Minecraft.getInstance().font.width(lbl)) / 2, by + (bh - 8) / 2, -2236963, false);
   }

   private void renderConfigsPanel(GuiGraphicsExtractor ctx, int mx, int my) {
      int margin = 20;
      int headerH = 24;
      int entryH = 18;
      int saveBarH = 36;
      int panW = this.width - margin * 2;
      int panH = this.height - margin * 2;
      int panX = margin;
      ctx.fill(0, 0, this.width, this.height, -1157627904);
      RenderUtil.dropShadow(ctx, margin, margin, panW, panH, 10, 18, 135);
      RenderUtil.roundedRectGradient(ctx, margin, margin, panW, panH, 10, -200208143, -334425871, true);
      RenderUtil.roundedOutline(ctx, margin, margin, panW, panH, 10, 1.0F, -14013910);
      RenderUtil.roundedRectGradient(ctx, margin, margin, panW, headerH, 10, -199353834, -266461666, true);
      ctx.fill(margin, margin + headerH / 2, margin + panW, margin + headerH, -266461666);
      int acc = OyVey.colorManager.getColorWithAlpha(margin, 255);
      RenderUtil.roundedRect(ctx, margin + 8, margin + headerH - 2, panW - 16, 2, 1, acc);
      ctx.text(Minecraft.getInstance().font, ClickGuiModule.t("CONFIGS"), margin + 8, margin + (headerH - 8) / 2, -1, false);
      String closeStr = "X";
      int closeW = 20;
      int closeH = 14;
      int closeX = margin + panW - closeW - 4;
      int closeY = margin + (headerH - closeH) / 2;
      boolean closeHov = mx >= closeX && mx <= closeX + closeW && my >= closeY && my <= closeY + closeH;
      RenderUtil.roundedRect(ctx, closeX, closeY, closeW, closeH, 5, closeHov ? 1157579844 : 587202559);
      RenderUtil.roundedOutline(ctx, closeX, closeY, closeW, closeH, 5, 1.0F, 369098751);
      ctx.text(
         Minecraft.getInstance().font,
         closeStr,
         closeX + (closeW - Minecraft.getInstance().font.width(closeStr)) / 2,
         closeY + (closeH - 8) / 2,
         -5592406,
         false
      );
      int listAreaH = panH - headerH - 4 - saveBarH;
      int maxVisible = listAreaH / entryH;
      int maxScroll = Math.max(0, this.configList.size() - maxVisible);
      if (this.configScroll > maxScroll) {
         this.configScroll = maxScroll;
      }

      int ey = margin + headerH + 4;
      if (this.configList.isEmpty()) {
         ctx.text(Minecraft.getInstance().font, ClickGuiModule.t("No configs saved yet"), margin + 8, ey + 4, -11184811, false);
      } else {
         int end = Math.min(this.configList.size(), this.configScroll + maxVisible);

         for (int i = this.configScroll; i < end; i++) {
            String cfg = this.configList.get(i);
            boolean hov = mx >= panX && mx < panX + panW - 52 && my >= ey && my < ey + entryH;
            if (hov) {
               RenderUtil.roundedRect(ctx, panX + 4, ey + 1, panW - 60, entryH - 2, 5, 419430399);
            }

            ctx.fill(panX, ey + entryH - 1, panX + panW, ey + entryH, 369098751);
            ctx.text(Minecraft.getInstance().font, cfg, panX + 8, ey + (entryH - 8) / 2, -3355444, false);
            int lbX = panX + panW - 48;
            int lbW = 44;
            boolean lhov = mx >= lbX && mx <= lbX + lbW && my >= ey + 2 && my <= ey + entryH - 2;
            RenderUtil.roundedRect(ctx, lbX, ey + 2, lbW, entryH - 4, 5, lhov ? OyVey.colorManager.getColorWithAlpha(ey, 200) : 872415231);
            ctx.text(
               Minecraft.getInstance().font,
               ClickGuiModule.t("Load"),
               lbX + (lbW - Minecraft.getInstance().font.width(ClickGuiModule.t("Load"))) / 2,
               ey + (entryH - 8) / 2,
               -1,
               false
            );
            ey += entryH;
         }
      }

      if (this.configList.size() > maxVisible) {
         int sbX = panX + panW - 5;
         int sbW = 4;
         int sbAreaY = margin + headerH + 4;
         RenderUtil.roundedRect(ctx, sbX, sbAreaY, sbW, listAreaH, 2, 587202559);
         int thumbH = Math.max(16, (int)((float)listAreaH * maxVisible / this.configList.size()));
         int thumbY = sbAreaY + (maxScroll > 0 ? (int)((float)(listAreaH - thumbH) * this.configScroll / maxScroll) : 0);
         RenderUtil.roundedRect(ctx, sbX, thumbY, sbW, thumbH, 2, -1996488705);
      }

      int saveY = margin + panH - saveBarH;
      ctx.fill(panX, saveY, panX + panW, saveY + saveBarH, -267645940);
      ctx.fill(panX, saveY, panX + panW, saveY + 1, -14013910);
      int inputW = panW - 100;
      boolean inputHov = mx >= panX + 6 && mx <= panX + 6 + inputW && my >= saveY + 6 && my <= saveY + saveBarH - 6;
      RenderUtil.roundedRect(ctx, panX + 6, saveY + 6, inputW, saveBarH - 12, 6, this.configTyping ? 1157627903 : (inputHov ? 587202559 : 419430399));
      RenderUtil.roundedOutline(ctx, panX + 6, saveY + 6, inputW, saveBarH - 12, 6, 1.0F, this.configTyping ? acc : 369098751);
      boolean blink = this.configTyping && System.currentTimeMillis() % 900L < 450L;
      String dispName = this.configSaveName.isEmpty() ? (this.configTyping ? "" : ClickGuiModule.t("name...")) : this.configSaveName;
      ctx.text(
         Minecraft.getInstance().font,
         dispName + (blink ? "|" : ""),
         panX + 9,
         saveY + (saveBarH - 8) / 2,
         this.configSaveName.isEmpty() ? -12303292 : -1118482,
         false
      );
      int saveBtnX = panX + 6 + inputW + 6;
      int saveBtnW = panW - 6 - inputW - 18;
      boolean shov = mx >= saveBtnX && mx <= saveBtnX + saveBtnW && my >= saveY + 6 && my <= saveY + saveBarH - 6;
      RenderUtil.roundedRectGradient(ctx, saveBtnX, saveY + 6, saveBtnW, saveBarH - 12, 6, shov ? OyVey.colorManager.getColorWithAlpha(saveY, 220) : 872415231, shov ? OyVey.colorManager.getColorWithAlpha(saveY + 20, 150) : 587202559, true);
      RenderUtil.roundedOutline(ctx, saveBtnX, saveY + 6, saveBtnW, saveBarH - 12, 6, 1.0F, shov ? acc : 369098751);
      ctx.text(
         Minecraft.getInstance().font,
         ClickGuiModule.t("Save"),
         saveBtnX + (saveBtnW - Minecraft.getInstance().font.width(ClickGuiModule.t("Save"))) / 2,
         saveY + (saveBarH - 8) / 2,
         -1,
         false
      );
   }

   private void refreshConfigList() {
      this.configList.clear();

      try {
         if (!Files.exists(CFG_DIR)) {
            return;
         }

         try (Stream<Path> stream = Files.list(CFG_DIR)) {
            this.configList = stream.filter(p -> p.toString().endsWith(".json"))
               .map(p -> p.getFileName().toString().replace(".json", ""))
               .sorted()
               .collect(Collectors.toList());
         }
      } catch (IOException var6) {
      }
   }

   private void saveConfig(String name) {
      if (name.matches("[a-zA-Z0-9_\\-]+")) {
         try {
            Files.createDirectories(CFG_DIR);
            Files.writeString(CFG_DIR.resolve(name + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(OyVey.moduleManager.toJson()));
         } catch (IOException var3) {
         }
      }
   }

   private void loadConfig(String name) {
      try {
         Path f = CFG_DIR.resolve(name + ".json");
         if (!Files.exists(f)) {
            return;
         }

         OyVey.moduleManager.fromJson(JsonParser.parseString(Files.readString(f)));
      } catch (IOException var3) {
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}
