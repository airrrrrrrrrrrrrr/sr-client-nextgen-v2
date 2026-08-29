package me.alpha432.oyvey.features.gui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.alpha432.oyvey.OyVey;
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
import org.lwjgl.glfw.GLFW;

public class DropdownGui extends Screen {
   private static DropdownGui INSTANCE;
   private static final Map<Module.Category, int[]> panelPos = new LinkedHashMap<>();
   private static final Set<Module> expanded = new HashSet<>();
   private static final Set<LuaScript> expandedScripts = new HashSet<>();
   private static LuaScriptSetting scriptSlider = null;
   private static int scriptSliderX;
   private static int scriptSliderW;
   private boolean scriptsOpen = false;
   private int scriptScroll = 0;
   private long openTime;
   private static final float OPEN_ANIM_MS = 300.0F;
   private Module.Category dragging = null;
   private int dragOffX;
   private int dragOffY;
   private static Setting<?> sliderDrag = null;
   private static int sliderX;
   private static int sliderW;
   private Setting<?> captureBind = null;
   private Module captureBindModule = null;
   private boolean configsOpen = false;
   private List<String> configList = new ArrayList<>();
   private String configSaveName = "";
   private boolean configTyping = false;
   private int configScroll = 0;
   private static final Path CFG_DIR = FabricLoader.getInstance().getGameDir().resolve("oyvey").resolve("configs");
   private static Setting<Color> colorPicker = null;
   private static int cpX;
   private static int cpY;
   private static float[] cpHsb = new float[3];
   private static boolean cpDragSV;
   private static boolean cpDragH;
   private static boolean cpDragA;
   private static final int PW = 110;
   private static final int HDR = 14;
   private static final int MH = 13;
   private static final int SH = 11;
   private static final int CB = 7;
   private static final int CP_SQ = 88;
   private static final int CP_BAR = 7;
   private static final int C_HEADER = -267119584;
   private static final int C_BODY = -267580136;
   private static final int C_BORDER = -14934996;
   private static final int C_SEP = -14803408;
   private static final int C_TEXT = -2236929;
   private static final int C_SUBTEXT = -8947798;
   private static final int C_SETTING = -871888881;
   private static final int C_HOVER = 548055807;
   private static final int PANEL_R = 4;
   private static final int BAR_BTN_W = 64;
   private static final int BAR_BTN_H = 16;
   private static final int BAR_BTN_GAP = 4;
   private static final int MENU_W = 132;
   private static final int MENU_HEADER_H = 16;
   private static final int MENU_GAP = 4;

   public static DropdownGui getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new DropdownGui();
      }

      return INSTANCE;
   }

   public DropdownGui() {
      super(Component.literal("DropdownGui"));
      INSTANCE = this;
   }

   protected void init() {
      if (panelPos.isEmpty()) {
         this.initPositions();
      }

      this.openTime = System.currentTimeMillis();
   }

   private float openAnim() {
      float t = (float)(System.currentTimeMillis() - this.openTime) / 300.0F;
      if (t < 0.0F) {
         t = 0.0F;
      }

      if (t > 1.0F) {
         t = 1.0F;
      }

      return t;
   }

   private static float easeOutBack(float t) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float p = t - 1.0F;
      return 1.0F + c3 * p * p * p + c1 * p * p;
   }

   private void initPositions() {
      int x = 4;

      for (Module.Category cat : Module.Category.values()) {
         if (cat != Module.Category.HUD) {
            panelPos.put(cat, new int[]{x, 4});
            x += 114;
         }
      }
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
      if (sliderDrag != null) {
         long win = Minecraft.getInstance().getWindow().handle();
         if (GLFW.glfwGetMouseButton(win, 0) == 1) {
            this.applySlider(smx);
         } else {
            sliderDrag = null;
         }
      }

      if (scriptSlider != null) {
         long win = Minecraft.getInstance().getWindow().handle();
         if (GLFW.glfwGetMouseButton(win, 0) == 1) {
            this.applyScriptSlider(smx);
         } else {
            scriptSlider = null;
         }
      }

      if (this.dragging != null) {
         long win = Minecraft.getInstance().getWindow().handle();
         if (GLFW.glfwGetMouseButton(win, 0) == 1) {
            int[] p = panelPos.get(this.dragging);
            p[0] = Math.max(0, smx - this.dragOffX);
            p[1] = Math.max(0, smy - this.dragOffY);
         } else {
            this.dragging = null;
         }
      }

      float openT = this.openAnim();
      float animScale = easeOutBack(openT);
      int dimAlpha = (int)(85.0F * openT);
      ctx.fill(0, 0, this.width, this.height, dimAlpha << 24);
      ctx.pose().pushMatrix();
      ctx.pose().translate(cx, cy);
      ctx.pose().scale(scale * animScale, scale * animScale);
      ctx.pose().translate(-cx, -cy);

      for (Module.Category cat : panelPos.keySet()) {
         this.renderPanel(ctx, smx, smy, cat);
      }

      if (colorPicker != null) {
         this.renderColorPicker(ctx, smx, smy);
      }

      this.renderBottomBar(ctx, smx, smy);
      if (this.configsOpen) {
         this.renderConfigsMenu(ctx, smx, smy);
      }

      if (this.scriptsOpen && OyVey.scriptManager != null) {
         this.renderScriptsMenu(ctx, smx, smy);
      }

      ctx.pose().popMatrix();
   }

   private void renderPanel(GuiGraphicsExtractor ctx, int mx, int my, Module.Category cat) {
      int[] pos = panelPos.get(cat);
      int px = pos[0];
      int py = pos[1];
      int totH = this.panelH(cat);
      int R = 4;
      RenderUtil.roundedRect(ctx, px + 2, py + 3, 110, totH, R, 1426063360);
      RenderUtil.roundedRect(ctx, px - 1, py - 1, 112, totH + 2, R + 1, -14934996);
      RenderUtil.roundedRect(ctx, px, py, 110, totH, R, -267580136);
      RenderUtil.roundedRect(ctx, px, py, 110, 14, R, -267119584);
      ctx.fill(px, py + R, px + 110, py + 14, -267119584);
      ctx.fill(px + R, py + 1, px + 110 - R, py + 2, 587202559);
      int acc = OyVey.colorManager.getColorWithAlpha(py, 220);
      ctx.fill(px + R, py + 14 - 2, px + 110 - R, py + 14, acc);
      String label = ClickGuiModule.t(cat.getName());
      int lw = Minecraft.getInstance().font.width(label);
      ctx.text(Minecraft.getInstance().font, label, px + (110 - lw) / 2, py + 3 + 1, -1, false);
      int ey = py + 14;

      for (Module mod : this.modsOf(cat)) {
         this.renderModuleRow(ctx, mx, my, mod, px, ey);
         ey += 13;
         if (expanded.contains(mod)) {
            this.renderBindRow(ctx, mx, my, mod, px, ey);
            ey += 11;

            for (Setting<?> s : this.visibleSettings(mod)) {
               this.renderSettingRow(ctx, mx, my, s, px, ey);
               ey += 11;
            }
         }
      }

      RenderUtil.maskCorners(ctx, px, py, 110, totH, R, -14934996, false, false, true, true);
   }

   private void renderModuleRow(GuiGraphicsExtractor ctx, int mx, int my, Module mod, int px, int ey) {
      boolean on = mod.isEnabled();
      boolean hov = mx >= px && mx <= px + 110 && my >= ey && my <= ey + 13;
      if (hov) {
         ctx.fill(px, ey, px + 110, ey + 13, 548055807);
      }

      ctx.text(Minecraft.getInstance().font, mod.getDisplayName(), px + 4, ey + 2 + 1, -2236929, false);
      int cbX = px + 110 - 7 - 4;
      int cbY = ey + 3;
      drawCheckbox(ctx, cbX, cbY, on, ey);
      if (!this.visibleSettings(mod).isEmpty()) {
         boolean exp = expanded.contains(mod);
         String g = exp ? "v" : ">";
         ctx.text(Minecraft.getInstance().font, g, cbX - Minecraft.getInstance().font.width(g) - 3, ey + 2 + 1, on ? -7829368 : -12303292, false);
      }

      ctx.fill(px + 2, ey + 13 - 1, px + 110 - 2, ey + 13, -14803408);
   }

   private void renderSettingRow(GuiGraphicsExtractor ctx, int mx, int my, Setting<?> s, int px, int ey) {
      boolean hov = mx >= px && mx <= px + 110 && my >= ey && my <= ey + 11;
      ctx.fill(px, ey, px + 110, ey + 11, hov ? 548055807 : -871888881);
      ctx.fill(px + 3, ey, px + 4, ey + 11, 872415231);
      String name = s.getName();
      int maxW;
      if (s.isEnumSetting()) {
         int evW = Minecraft.getInstance().font.width(s.currentEnumName());
         maxW = Math.max(18, 110 - evW - 15);
      } else {
         maxW = 51;
      }

      while (name.length() > 3 && Minecraft.getInstance().font.width(name) > maxW) {
         name = name.substring(0, name.length() - 2) + ".";
      }

      ctx.text(Minecraft.getInstance().font, name, px + 7, ey + 1 + 1, -4473925, false);
      int re = px + 110 - 4;
      if (s.getValue() instanceof Boolean bool) {
         int cbX = re - 7;
         int cbY = ey + 2;
         drawCheckbox(ctx, cbX, cbY, bool, ey);
      } else if (s.isNumberSetting() && s.hasRestriction()) {
         float min = ((Number)s.getMin()).floatValue();
         float max = ((Number)s.getMax()).floatValue();
         float val = ((Number)s.getValue()).floatValue();
         float pct = Math.max(0.0F, Math.min(1.0F, (val - min) / (max - min)));
         String vs = s.getValue() instanceof Integer ? String.valueOf((int)val) : String.format("%.1f", val);
         ctx.text(Minecraft.getInstance().font, vs, re - Minecraft.getInstance().font.width(vs), ey + 1 + 1, -8947798, false);
         int bx = px + 4;
         int bw = 102;
         int by = ey + 11 - 2;
         ctx.fill(bx, by, bx + bw, by + 2, 872415231);
         ctx.fill(bx, by, bx + (int)(bw * pct), by + 2, acc(ey, 200));
         if (hov || s == sliderDrag) {
            int kx = bx + (int)(bw * pct);
            ctx.fill(kx - 1, by - 1, kx + 2, by + 3, -1);
         }
      } else if (s.isEnumSetting()) {
         String ev = s.currentEnumName();
         ctx.text(Minecraft.getInstance().font, ev, re - Minecraft.getInstance().font.width(ev), ey + 1 + 1, -11171670, false);
      } else if (s.getValue() instanceof Bind bind) {
         boolean cap = s == this.captureBind;
         String bs = cap ? "..." : bind.toString();
         ctx.text(Minecraft.getInstance().font, bs, re - Minecraft.getInstance().font.width(bs), ey + 1 + 1, cap ? acc(ey, 255) : -11180425, false);
      } else if (s.isColorSetting()) {
         Color c = (Color)s.getValue();
         int sw = 10;
         int sh2 = 7;
         int sx = re - sw;
         int sy = ey + 2;
         ctx.fill(sx, sy, sx + sw / 2, sy + sh2 / 2, -5592406);
         ctx.fill(sx + sw / 2, sy, sx + sw, sy + sh2 / 2, -10066330);
         ctx.fill(sx, sy + sh2 / 2, sx + sw / 2, sy + sh2, -10066330);
         ctx.fill(sx + sw / 2, sy + sh2 / 2, sx + sw, sy + sh2, -5592406);
         ctx.fill(sx, sy, sx + sw, sy + sh2, c.getRGB());
         ctx.fill(sx - 1, sy - 1, sx + sw + 1, sy, 1627389951);
         ctx.fill(sx - 1, sy + sh2, sx + sw + 1, sy + sh2 + 1, 1627389951);
         ctx.fill(sx - 1, sy, sx, sy + sh2, 1627389951);
         ctx.fill(sx + sw, sy, sx + sw + 1, sy + sh2, 1627389951);
      }

      ctx.fill(px + 4, ey + 11 - 1, px + 110 - 4, ey + 11, 419430399);
   }

   private void renderColorPicker(GuiGraphicsExtractor ctx, int mx, int my) {
      Color cur = colorPicker.getValue();
      Color hueCol = Color.getHSBColor(cpHsb[0], 1.0F, 1.0F);
      int bx = cpX;
      int by = cpY;
      int W = 88;
      int BAR = 7;
      int ay = by + W + 3;
      int hy = ay + BAR + 3;
      int totH = W + (BAR + 3) * 2 + 8;
      RenderUtil.roundedRect(ctx, bx - 5, by - 5, W + 10, totH + 5, 4, 1442840575);
      RenderUtil.roundedRect(ctx, bx - 4, by - 4, W + 8, totH + 4, 4, -233826272);
      RenderUtil.horizontalGradient(ctx, bx, by, bx + W, by + W, Color.WHITE, hueCol);
      RenderUtil.verticalGradient(ctx, bx, by, bx + W, by + W, new Color(0, 0, 0, 0), Color.BLACK);
      int sx = Math.max(bx, Math.min(bx + W, bx + (int)(cpHsb[1] * W)));
      int sy = Math.max(by, Math.min(by + W, by + (int)((1.0F - cpHsb[2]) * W)));
      ctx.fill(sx - 3, sy - 3, sx + 3, sy + 3, -16777216);
      ctx.fill(sx - 2, sy - 2, sx + 2, sy + 2, -1);
      ctx.fill(bx, ay, bx + W / 2, ay + BAR / 2, -5592406);
      ctx.fill(bx + W / 2, ay, bx + W, ay + BAR / 2, -10066330);
      ctx.fill(bx, ay + BAR / 2, bx + W / 2, ay + BAR, -10066330);
      ctx.fill(bx + W / 2, ay + BAR / 2, bx + W, ay + BAR, -5592406);
      RenderUtil.horizontalGradient(
         ctx, bx, ay, bx + W, ay + BAR, new Color(cur.getRed(), cur.getGreen(), cur.getBlue(), 0), new Color(cur.getRed(), cur.getGreen(), cur.getBlue(), 255)
      );
      int ap = Math.max(bx, Math.min(bx + W, bx + (int)(cur.getAlpha() / 255.0F * W)));
      ctx.fill(ap - 2, ay - 1, ap + 2, ay + BAR + 1, -16777216);
      ctx.fill(ap - 1, ay, ap + 1, ay + BAR, -1);

      for (int i = 0; i < W; i++) {
         ctx.fill(bx + i, hy, bx + i + 1, hy + BAR, Color.getHSBColor((float)i / W, 1.0F, 1.0F).getRGB() | 0xFF000000);
      }

      int hp = Math.max(bx, Math.min(bx + W, bx + (int)(cpHsb[0] * W)));
      ctx.fill(hp - 2, hy - 1, hp + 2, hy + BAR + 1, -16777216);
      ctx.fill(hp - 1, hy, hp + 1, hy + BAR, -1);
      if (cpDragSV) {
         cpHsb[1] = Math.max(0.0F, Math.min(1.0F, (float)(mx - bx) / W));
         cpHsb[2] = Math.max(0.0F, Math.min(1.0F, 1.0F - (float)(my - by) / W));
         this.applyHsb();
      }

      if (cpDragA) {
         float a = Math.max(0.0F, Math.min(1.0F, (float)(mx - bx) / W));
         Color c = colorPicker.getValue();
         colorPicker.setValue(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(a * 255.0F)));
      }

      if (cpDragH) {
         cpHsb[0] = Math.max(0.0F, Math.min(1.0F, (float)(mx - bx) / W));
         this.applyHsb();
      }
   }

   private void applyHsb() {
      Color c = Color.getHSBColor(cpHsb[0], cpHsb[1], cpHsb[2]);
      int alpha = colorPicker.getValue().getAlpha();
      colorPicker.setValue(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      ClickGuiModule cgm = ClickGuiModule.getInstance();
      float sc = cgm != null ? cgm.guiScale.getValue() : 1.0F;
      int mx = (int)((click.x() - this.width / 2.0) / sc + this.width / 2.0);
      int my = (int)((click.y() - this.height / 2.0) / sc + this.height / 2.0);
      int btn = click.button();
      if (this.scriptsOpen) {
         return this.clickScriptsMenu(mx, my, btn);
      }

      if (this.configsOpen) {
         return this.clickConfigsMenu(mx, my, btn);
      }

      if (colorPicker != null) {
         int bx = cpX;
         int by = cpY;
         int W = 88;
         int BAR = 7;
         int ay = by + W + 3;
         int hy = ay + BAR + 3;
         int totH = W + (BAR + 3) * 2 + 8;
         boolean inP = mx >= bx - 4 && mx <= bx + W + 4 && my >= by - 4 && my <= by + totH;
         if (inP && btn == 0) {
            if (mx >= bx && mx <= bx + W) {
               if (my >= by && my <= by + W) {
                  cpDragSV = true;
                  return true;
               }

               if (my >= ay && my <= ay + BAR) {
                  cpDragA = true;
                  return true;
               }

               if (my >= hy && my <= hy + BAR) {
                  cpDragH = true;
                  return true;
               }
            }

            return true;
         }

         if (!inP) {
            colorPicker = null;
         }
      }

      for (Module.Category cat : panelPos.keySet()) {
         int[] pos = panelPos.get(cat);
         int px = pos[0];
         int py = pos[1];
         if (btn == 0 && mx >= px && mx <= px + 110 && my >= py && my <= py + 14) {
            this.dragging = cat;
            this.dragOffX = mx - px;
            this.dragOffY = my - py;
            return true;
         }

         int ey = py + 14;

         for (Module mod : this.modsOf(cat)) {
            if (mx >= px && mx <= px + 110 && my >= ey && my <= ey + 13) {
               if (btn == 0) {
                  mod.toggle();
               } else if (btn == 1) {
                  this.toggleExpand(mod);
               }

               return true;
            }

            ey += 13;
            if (expanded.contains(mod)) {
               if (mx >= px && mx <= px + 110 && my >= ey && my <= ey + 11) {
                  if (btn == 1) {
                     mod.setBind(-1);
                     this.captureBindModule = null;
                  } else {
                     this.captureBindModule = this.captureBindModule == mod ? null : mod;
                     this.captureBind = null;
                  }

                  return true;
               }

               ey += 11;

               for (Setting<?> s : this.visibleSettings(mod)) {
                  if (mx >= px && mx <= px + 110 && my >= ey && my <= ey + 11) {
                     this.handleSetting(s, mx, ey, px, btn);
                     return true;
                  }

                  ey += 11;
               }
            }
         }
      }

      int barY = this.height - 20;
      int scrBtnX = 72;
      if (OyVey.scriptManager != null && mx >= scrBtnX && mx <= scrBtnX + 64 && my >= barY && my <= barY + 16) {
         this.openScripts();
         return true;
      } else if (mx >= 4 && mx <= 68 && my >= barY && my <= barY + 16) {
         this.openConfigs();
         return true;
      } else {
         return super.mouseClicked(click, doubled);
      }
   }

   private void openScripts() {
      this.scriptsOpen = true;
      this.configsOpen = false;
      this.configTyping = false;
      this.scriptScroll = 0;
   }

   private void openConfigs() {
      this.configsOpen = true;
      this.scriptsOpen = false;
      this.refreshConfigList();
      this.configScroll = 0;
   }

   private boolean clickScriptsMenu(int mx, int my, int btn) {
      int barY = this.height - 20;
      int scrBtnX = 72;
      if (mx >= scrBtnX && mx <= scrBtnX + 64 && my >= barY && my <= barY + 16) {
         this.scriptsOpen = false;
         return true;
      }

      if (mx >= 4 && mx <= 68 && my >= barY && my <= barY + 16) {
         this.openConfigs();
         return true;
      }

      if (OyVey.scriptManager == null) {
         this.scriptsOpen = false;
         return true;
      }

      int[] g = this.scriptsMenuGeom();
      int px = g[0];
      int py = g[1];
      int menuH = g[2];
      int listTop = g[3];
      int listH = g[4];
      int footerY = g[6];
      int footerH = g[7];
      if (mx >= px && mx <= px + 132 && my >= footerY && my <= footerY + footerH) {
         OyVey.scriptManager.reload();
         expandedScripts.clear();
         this.scriptScroll = 0;
         return true;
      }

      if (mx >= px && mx <= px + 132 && my >= listTop && my < listTop + listH) {
         int ey = listTop - this.scriptScroll;

         for (LuaScript s : OyVey.scriptManager.getScripts()) {
            if (my >= ey && my < ey + 13) {
               if (btn == 0) {
                  if (s.isEnabled()) {
                     s.disable();
                  } else {
                     s.enable();
                  }
               } else if (btn == 1) {
                  if (expandedScripts.contains(s)) {
                     expandedScripts.remove(s);
                  } else {
                     expandedScripts.add(s);
                  }
               }

               return true;
            }

            ey += 13;
            if (expandedScripts.contains(s)) {
               for (LuaScriptSetting ss : s.getScriptSettings()) {
                  if (my >= ey && my < ey + 11) {
                     if (ss.getType() == LuaScriptSetting.Type.BOOLEAN) {
                        ss.setBoolValue(!ss.getBoolean());
                     } else if (ss.getType() == LuaScriptSetting.Type.SLIDER) {
                        scriptSlider = ss;
                        scriptSliderX = px + 4;
                        scriptSliderW = 124;
                        this.applyScriptSlider(mx);
                     } else if (ss.getType() == LuaScriptSetting.Type.ENUM) {
                        ss.cycleEnum();
                     }

                     return true;
                  }

                  ey += 11;
               }
            }
         }

         return true;
      } else {
         if (mx >= px && mx <= px + 132 && my >= py && my <= py + menuH) {
            return true;
         }

         this.scriptsOpen = false;
         return true;
      }
   }

   private boolean clickConfigsMenu(int mx, int my, int btn) {
      int barY = this.height - 20;
      int scrBtnX = 72;
      if (mx >= 4 && mx <= 68 && my >= barY && my <= barY + 16) {
         this.configsOpen = false;
         this.configTyping = false;
         return true;
      }

      if (OyVey.scriptManager != null && mx >= scrBtnX && mx <= scrBtnX + 64 && my >= barY && my <= barY + 16) {
         this.openScripts();
         return true;
      }

      int[] g = this.configsMenuGeom();
      int px = g[0];
      int py = g[1];
      int menuH = g[2];
      int listTop = g[3];
      int listH = g[4];
      int footerY = g[6];
      int footerH = g[7];
      if (mx >= px && mx <= px + 132 && my >= listTop && my < listTop + listH) {
         if (!this.configList.isEmpty()) {
            int ey = listTop - this.configScroll;

            for (String cfg : this.configList) {
               if (my >= ey && my < ey + 16) {
                  this.loadConfig(cfg);
                  this.configsOpen = false;
                  this.configTyping = false;
                  return true;
               }

               ey += 16;
            }
         }

         return true;
      } else {
         int inputW = 88;
         if (mx >= px + 4 && mx <= px + inputW && my >= footerY + 3 && my <= footerY + footerH - 3) {
            this.configTyping = true;
            return true;
         }

         int saveBtnX = px + inputW + 2;
         if (mx >= saveBtnX && mx <= px + 132 - 4 && my >= footerY + 3 && my <= footerY + footerH - 3) {
            if (!this.configSaveName.isBlank()) {
               this.saveConfig(this.configSaveName.trim());
               this.configSaveName = "";
               this.configTyping = false;
               this.refreshConfigList();
            }

            return true;
         } else {
            if (mx >= px && mx <= px + 132 && my >= py && my <= py + menuH) {
               return true;
            }

            this.configsOpen = false;
            this.configTyping = false;
            return true;
         }
      }
   }

   private void handleSetting(Setting<?> s, int mx, int ey, int px, int btn) {
      if (s.getValue() instanceof Boolean) {
         ((Setting<Boolean>)s).setValue(!(Boolean)s.getValue());
      } else if (s.isNumberSetting() && s.hasRestriction()) {
         int bx = px + 4;
         int bw = 102;
         if (mx >= bx - 3 && mx <= bx + bw + 3) {
            sliderDrag = s;
            sliderX = bx;
            sliderW = bw;
            this.applySlider(mx);
         }
      } else if (s.isEnumSetting()) {
         s.increaseEnum();
      } else if (s.getValue() instanceof Bind) {
         if (btn == 1) {
            ((Setting<Bind>)s).setValue(new Bind(-1));
            this.captureBind = null;
         } else {
            this.captureBind = this.captureBind == s ? null : s;
         }
      } else if (s.isColorSetting()) {
         if (colorPicker == s) {
            colorPicker = null;
            return;
         }

         colorPicker = (Setting<Color>)s;
         Color c = (Color)s.getValue();
         cpHsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
         int rightEdge = px + 110 + 6 + 88 + 8;
         cpX = rightEdge > this.width ? px - 88 - 10 : px + 110 + 6;
         cpY = ey;
      }
   }

   public boolean mouseReleased(MouseButtonEvent click) {
      if (click.button() == 0) {
         this.dragging = null;
         sliderDrag = null;
         scriptSlider = null;
         cpDragA = false;
         cpDragH = false;
         cpDragSV = false;
      }

      return super.mouseReleased(click);
   }

   public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
      if (this.scriptsOpen) {
         int[] g = this.scriptsMenuGeom();
         int maxScroll = Math.max(0, g[5] - g[4]);
         this.scriptScroll = Math.max(0, Math.min(maxScroll, this.scriptScroll - (int)Math.signum(vAmt) * 13));
         return true;
      } else if (this.configsOpen) {
         int[] g = this.configsMenuGeom();
         int maxScroll = Math.max(0, g[5] - g[4]);
         this.configScroll = Math.max(0, Math.min(maxScroll, this.configScroll - (int)Math.signum(vAmt) * 16));
         return true;
      } else {
         return super.mouseScrolled(mx, my, hAmt, vAmt);
      }
   }

   public boolean keyPressed(KeyEvent input) {
      int key = input.input();
      if (this.captureBindModule != null) {
         if (key == 256) {
            this.captureBindModule = null;
         } else if (key != 261 && key != 259) {
            this.captureBindModule.setBind(key);
            OyVey.configManager.save();
            this.captureBindModule = null;
         } else {
            this.captureBindModule.setBind(-1);
            OyVey.configManager.save();
            this.captureBindModule = null;
         }

         return true;
      } else if (this.configTyping) {
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
      } else if (this.captureBind == null) {
         if (key == 256) {
            if (colorPicker != null) {
               colorPicker = null;
               return true;
            } else if (this.configsOpen) {
               this.configsOpen = false;
               this.configTyping = false;
               return true;
            } else if (this.scriptsOpen) {
               this.scriptsOpen = false;
               return true;
            } else {
               this.onClose();
               return true;
            }
         } else {
            return super.keyPressed(input);
         }
      } else {
         Bind b = key != 256 && key != 261 && key != 259 ? new Bind(key) : new Bind(-1);
         ((Setting<Bind>)this.captureBind).setValue(b);
         this.captureBind = null;
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
         return true;
      }
   }

   private List<Module> modsOf(Module.Category cat) {
      return OyVey.moduleManager.getModulesByCategory(cat).stream().filter(m -> !m.hidden).collect(Collectors.toList());
   }

   private List<Setting<?>> visibleSettings(Module mod) {
      return mod.getSettings()
         .stream()
         .filter(Setting::isVisible)
         .filter(
            s -> !s.getName().equalsIgnoreCase("Enabled")
               && !s.getName().equalsIgnoreCase("Drawn")
               && !s.getName().equalsIgnoreCase("DisplayName")
               && !s.getName().equalsIgnoreCase("Keybind")
         )
         .collect(Collectors.toList());
   }

   private int panelH(Module.Category cat) {
      int h = 14;

      for (Module mod : this.modsOf(cat)) {
         h += 13;
         if (expanded.contains(mod)) {
            h += (1 + this.visibleSettings(mod).size()) * 11;
         }
      }

      return h;
   }

   private void toggleExpand(Module mod) {
      if (expanded.contains(mod)) {
         expanded.remove(mod);
      } else {
         expanded.add(mod);
      }
   }

   private void applySlider(int mx) {
      if (sliderDrag != null) {
         float pct = Math.max(0.0F, Math.min(1.0F, (float)(mx - sliderX) / sliderW));
         float min = ((Number)sliderDrag.getMin()).floatValue();
         float max = ((Number)sliderDrag.getMax()).floatValue();
         float nv = min + pct * (max - min);
         Object v = sliderDrag.getValue();
         if (v instanceof Integer) {
            ((Setting<Integer>)sliderDrag).setValueNoEvent((int)nv);
         } else if (v instanceof Float) {
            ((Setting<Float>)sliderDrag).setValueNoEvent(nv);
         } else if (v instanceof Double) {
            ((Setting<Double>)sliderDrag).setValueNoEvent((double)nv);
         }
      }
   }

   private static int acc(int y, int alpha) {
      return OyVey.colorManager.getColorWithAlpha(y, alpha);
   }

   private static void drawCheckbox(GuiGraphicsExtractor ctx, int cbX, int cbY, boolean on, int ey) {
      ctx.fill(cbX - 1, cbY - 1, cbX + 8, cbY + 8, -14013888);
      ctx.fill(cbX, cbY, cbX + 7, cbY + 7, on ? acc(ey, 255) : -16250862);
   }

   private void renderBindRow(GuiGraphicsExtractor ctx, int mx, int my, Module mod, int px, int ey) {
      boolean hov = mx >= px && mx <= px + 110 && my >= ey && my <= ey + 11;
      boolean cap = mod == this.captureBindModule;
      ctx.fill(px, ey, px + 110, ey + 11, hov ? 548055807 : -1442182636);
      ctx.fill(px + 3, ey, px + 4, ey + 11, 1442840575);
      ctx.text(Minecraft.getInstance().font, ClickGuiModule.t("Bind"), px + 7, ey + 1 + 1, -3355444, false);
      String label = cap ? ClickGuiModule.t("Press key...") : (mod.getBind().isEmpty() ? ClickGuiModule.t("None") : mod.getBind().toString());
      int col = cap ? acc(ey, 255) : -11176039;
      ctx.text(Minecraft.getInstance().font, label, px + 110 - Minecraft.getInstance().font.width(label) - 4, ey + 1 + 1, col, cap);
      ctx.fill(px + 4, ey + 11 - 1, px + 110 - 4, ey + 11, 419430399);
   }

   private int scriptsContentH() {
      int h = 0;

      for (LuaScript s : OyVey.scriptManager.getScripts()) {
         h += 13;
         if (expandedScripts.contains(s)) {
            h += s.getScriptSettings().size() * 11;
         }
      }

      return Math.max(h, 12);
   }

   private int configsContentH() {
      return this.configList.isEmpty() ? 14 : this.configList.size() * 16;
   }

   private int[] menuGeom(int anchorX, int contentH, int footerH) {
      int barY = this.height - 20;
      int maxTotalH = barY - 4 - 4;
      int maxListH = Math.max(11, maxTotalH - 16 - footerH);
      int listH = Math.min(contentH, maxListH);
      int menuH = 16 + listH + footerH;
      int py = barY - 4 - menuH;
      if (py < 4) {
         py = 4;
      }

      int px = anchorX;
      if (px + 132 > this.width - 2) {
         px = this.width - 2 - 132;
      }

      if (px < 2) {
         px = 2;
      }

      int listTop = py + 16;
      int footerY = listTop + listH;
      return new int[]{px, py, menuH, listTop, listH, contentH, footerY, footerH};
   }

   private int[] scriptsMenuGeom() {
      return this.menuGeom(72, this.scriptsContentH(), 14);
   }

   private int[] configsMenuGeom() {
      return this.menuGeom(4, this.configsContentH(), 20);
   }

   private void drawMenuFrame(GuiGraphicsExtractor ctx, int px, int py, int h, String title, long badge) {
      Font font = Minecraft.getInstance().font;
      int R = 4;
      RenderUtil.roundedRect(ctx, px + 2, py + 3, 132, h, R, 1426063360);
      RenderUtil.roundedRect(ctx, px - 1, py - 1, 134, h + 2, R + 1, -14934996);
      RenderUtil.roundedRect(ctx, px, py, 132, h, R, -267580136);
      RenderUtil.roundedRect(ctx, px, py, 132, 16, R, -267119584);
      ctx.fill(px, py + R, px + 132, py + 16, -267119584);
      ctx.fill(px + R, py + 1, px + 132 - R, py + 2, 587202559);
      int acc = OyVey.colorManager.getColorWithAlpha(py, 220);
      ctx.fill(px + R, py + 16 - 2, px + 132 - R, py + 16, acc);
      int lw = font.width(title);
      ctx.text(font, title, px + (132 - lw) / 2, py + 4, -1, false);
      if (badge > 0L) {
         String cnt = String.valueOf(badge);
         int bw = font.width(cnt) + 6;
         int bx2 = px + 132 - bw - 4;
         int by2 = py + 3;
         RenderUtil.roundedRect(ctx, bx2, by2, bw, 9, 2, -1716885982);
         ctx.text(font, cnt, bx2 + 3, by2 + 1, -17596, false);
      }
   }

   private void renderScriptsMenu(GuiGraphicsExtractor ctx, int mx, int my) {
      Font font = Minecraft.getInstance().font;
      int[] g = this.scriptsMenuGeom();
      int px = g[0];
      int py = g[1];
      int menuH = g[2];
      int listTop = g[3];
      int listH = g[4];
      int contentH = g[5];
      int footerY = g[6];
      int footerH = g[7];
      List<LuaScript> scripts = OyVey.scriptManager.getScripts();
      long enabledCount = scripts.stream().filter(LuaScript::isEnabled).count();
      this.drawMenuFrame(ctx, px, py, menuH, ClickGuiModule.t("Scripts"), enabledCount);
      int maxScroll = Math.max(0, contentH - listH);
      if (this.scriptScroll > maxScroll) {
         this.scriptScroll = maxScroll;
      }

      ctx.enableScissor(px, listTop, px + 132, listTop + listH);
      int ey = listTop - this.scriptScroll;
      if (scripts.isEmpty()) {
         String msg = "No scripts found";
         ctx.text(font, msg, px + (132 - font.width(msg)) / 2, listTop + 2, -8947798, false);
      } else {
         for (LuaScript sx : scripts) {
            this.renderScriptRow(ctx, mx, my, sx, px, ey);
            ey += 13;
            if (expandedScripts.contains(sx)) {
               for (LuaScriptSetting ss : sx.getScriptSettings()) {
                  this.renderScriptSettingRow(ctx, mx, my, ss, px, ey);
                  ey += 11;
               }
            }
         }
      }

      ctx.disableScissor();
      if (contentH > listH) {
         int sbX = px + 132 - 3;
         ctx.fill(sbX, listTop, sbX + 2, listTop + listH, 587202559);
         int thumbH = Math.max(12, (int)((float)listH * listH / contentH));
         int thumbY = listTop + (maxScroll > 0 ? (int)((float)(listH - thumbH) * this.scriptScroll / maxScroll) : 0);
         ctx.fill(sbX, thumbY, sbX + 2, thumbY + thumbH, -1996488705);
      }

      boolean rHov = mx >= px && mx <= px + 132 && my >= footerY && my <= footerY + footerH;
      ctx.fill(px, footerY, px + 132, footerY + footerH, rHov ? 548055807 : -871888881);
      String rl = ClickGuiModule.t("[ Reload Scripts ]");
      ctx.text(font, rl, px + (132 - font.width(rl)) / 2, footerY + (footerH - 8) / 2, -8939077, false);
      RenderUtil.maskCorners(ctx, px, py, 132, menuH, 4, -14934996, false, false, true, true);
   }

   private void renderScriptRow(GuiGraphicsExtractor ctx, int mx, int my, LuaScript sx, int px, int ey) {
      Font font = Minecraft.getInstance().font;
      boolean on = sx.isEnabled();
      boolean hov = mx >= px && mx <= px + 132 && my >= ey && my <= ey + 13;
      if (hov) {
         ctx.fill(px, ey, px + 132, ey + 13, 548055807);
      }

      ctx.text(font, sx.getName(), px + 4, ey + 2 + 1, -2236929, false);
      if (!sx.getDescription().isEmpty()) {
         String desc = sx.getDescription();
         int maxDescW = 132 - font.width(sx.getName()) - 30;

         while (desc.length() > 3 && font.width(desc) > maxDescW) {
            desc = desc.substring(0, desc.length() - 2) + ".";
         }

         ctx.text(font, desc, px + 4 + font.width(sx.getName()) + 4, ey + 2 + 1, -8947798, false);
      }

      int cbX = px + 132 - 7 - 4;
      int cbY = ey + 3;
      drawCheckbox(ctx, cbX, cbY, on, ey);
      if (!sx.getScriptSettings().isEmpty()) {
         String g = expandedScripts.contains(sx) ? "v" : ">";
         ctx.text(font, g, cbX - font.width(g) - 3, ey + 2 + 1, on ? -7829368 : -12303292, false);
      }

      ctx.fill(px + 2, ey + 13 - 1, px + 132 - 2, ey + 13, -14803408);
   }

   private void renderScriptSettingRow(GuiGraphicsExtractor ctx, int mx, int my, LuaScriptSetting ss, int px, int ey) {
      Font font = Minecraft.getInstance().font;
      boolean hov = mx >= px && mx <= px + 132 && my >= ey && my <= ey + 11;
      ctx.fill(px, ey, px + 132, ey + 11, hov ? 548055807 : -871888881);
      ctx.fill(px + 3, ey, px + 4, ey + 11, 872415231);
      String name = ss.getName();
      ctx.text(font, name, px + 7, ey + 1 + 1, -4473925, false);
      int re = px + 132 - 4;
      switch (ss.getType()) {
         case BOOLEAN:
            int cbX = re - 7;
            int cbY = ey + 2;
            drawCheckbox(ctx, cbX, cbY, ss.getBoolean(), ey);
            break;
         case SLIDER:
            double pct = (ss.getDouble() - ss.getMin()) / (ss.getMax() - ss.getMin());
            int bx = px + 4;
            int bw = 124;
            int barY = ey + 11 - 4;
            RenderUtil.roundedRect(ctx, bx, barY, bw, 2, 1, -15066584);
            RenderUtil.roundedRect(ctx, bx, barY, (int)(bw * pct), 2, 1, acc(ey, 200));
            String val = String.format("%.2f", ss.getDouble());
            ctx.text(font, val, re - font.width(val), ey + 1 + 1, -7829334, false);
            break;
         case ENUM:
            String cur = ss.getString();
            ctx.text(font, cur, re - font.width(cur), ey + 1 + 1, acc(ey, 220), false);
      }

      ctx.fill(px + 4, ey + 11 - 1, px + 132 - 4, ey + 11, 419430399);
   }

   private void applyScriptSlider(int mx) {
      if (scriptSlider != null) {
         float pct = Math.max(0.0F, Math.min(1.0F, (float)(mx - scriptSliderX) / scriptSliderW));
         double nv = scriptSlider.getMin() + pct * (scriptSlider.getMax() - scriptSlider.getMin());
         scriptSlider.setDoubleValue(nv);
      }
   }

   private void renderBottomBar(GuiGraphicsExtractor ctx, int mx, int my) {
      int by = this.height - 20;
      this.renderBarButton(ctx, mx, my, 4, by, ClickGuiModule.t("Configs"), this.configsOpen);
      if (OyVey.scriptManager != null) {
         this.renderBarButton(ctx, mx, my, 72, by, ClickGuiModule.t("Scripts"), this.scriptsOpen);
      }
   }

   private void renderBarButton(GuiGraphicsExtractor ctx, int mx, int my, int bx, int by, String lbl, boolean open) {
      Font font = Minecraft.getInstance().font;
      int bw = 64;
      int bh = 16;
      boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
      ctx.fill(bx, by, bx + bw, by + bh, open ? acc(by, 190) : (hov ? 1157627903 : -871099372));
      ctx.text(font, lbl, bx + (bw - font.width(lbl)) / 2, by + (bh - 8) / 2, open ? -1 : -2236963, false);
   }

   private void renderConfigsMenu(GuiGraphicsExtractor ctx, int mx, int my) {
      Font font = Minecraft.getInstance().font;
      int[] g = this.configsMenuGeom();
      int px = g[0];
      int py = g[1];
      int menuH = g[2];
      int listTop = g[3];
      int listH = g[4];
      int contentH = g[5];
      int footerY = g[6];
      int footerH = g[7];
      this.drawMenuFrame(ctx, px, py, menuH, ClickGuiModule.t("Configs"), 0L);
      int maxScroll = Math.max(0, contentH - listH);
      if (this.configScroll > maxScroll) {
         this.configScroll = maxScroll;
      }

      ctx.enableScissor(px, listTop, px + 132, listTop + listH);
      if (this.configList.isEmpty()) {
         String msg = ClickGuiModule.t("No configs yet");
         ctx.text(font, msg, px + (132 - font.width(msg)) / 2, listTop + 3, -8947798, false);
      } else {
         int ey = listTop - this.configScroll;

         for (String cfg : this.configList) {
            boolean hov = mx >= px && mx <= px + 132 && my >= ey && my < ey + 16;
            if (hov) {
               ctx.fill(px, ey, px + 132, ey + 16, 548055807);
            }

            String name = cfg;

            while (name.length() > 3 && font.width(name) > 118) {
               name = name.substring(0, name.length() - 2) + ".";
            }

            ctx.text(font, name, px + 6, ey + 4, hov ? -1 : -3355444, false);
            ctx.fill(px + 2, ey + 16 - 1, px + 132 - 2, ey + 16, -14803408);
            ey += 16;
         }
      }

      ctx.disableScissor();
      if (contentH > listH) {
         int sbX = px + 132 - 3;
         ctx.fill(sbX, listTop, sbX + 2, listTop + listH, 587202559);
         int thumbH = Math.max(12, (int)((float)listH * listH / contentH));
         int thumbY = listTop + (maxScroll > 0 ? (int)((float)(listH - thumbH) * this.configScroll / maxScroll) : 0);
         ctx.fill(sbX, thumbY, sbX + 2, thumbY + thumbH, -1996488705);
      }

      ctx.fill(px, footerY, px + 132, footerY + footerH, -871888881);
      ctx.fill(px, footerY, px + 132, footerY + 1, 872415231);
      int inputW = 88;
      boolean inputHov = mx >= px + 4 && mx <= px + inputW && my >= footerY + 3 && my <= footerY + footerH - 3;
      ctx.fill(px + 4, footerY + 3, px + inputW, footerY + footerH - 3, this.configTyping ? 1157627903 : (inputHov ? 587202559 : 419430399));
      boolean blink = this.configTyping && System.currentTimeMillis() % 900L < 450L;
      String dispName = this.configSaveName.isEmpty() ? (this.configTyping ? "" : ClickGuiModule.t("name...")) : this.configSaveName;
      ctx.text(font, dispName + (blink ? "|" : ""), px + 7, footerY + (footerH - 8) / 2, this.configSaveName.isEmpty() ? -12303292 : -1118482, false);
      int saveBtnX = px + inputW + 2;
      int saveBtnRight = px + 132 - 4;
      boolean shov = mx >= saveBtnX && mx <= saveBtnRight && my >= footerY + 3 && my <= footerY + footerH - 3;
      ctx.fill(saveBtnX, footerY + 3, saveBtnRight, footerY + footerH - 3, shov ? acc(footerY, 200) : 872415231);
      String save = ClickGuiModule.t("Save");
      ctx.text(font, save, saveBtnX + (saveBtnRight - saveBtnX - font.width(save)) / 2, footerY + (footerH - 8) / 2, -1, false);
      RenderUtil.maskCorners(ctx, px, py, 132, menuH, 4, -14934996, false, false, true, true);
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
            JsonElement json = OyVey.moduleManager.toJson();
            Files.writeString(CFG_DIR.resolve(name + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(json));
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
