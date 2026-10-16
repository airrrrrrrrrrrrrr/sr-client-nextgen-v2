package me.alpha432.oyvey.features.gui;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
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
import me.alpha432.oyvey.util.render.HudFont;
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
   private static final Map<Module.Category, Integer> panelScroll = new LinkedHashMap<>();
   private int layoutWidth;
   private int layoutHeight;
   private float layoutScale;
   private int panelLimit;
   private static final Set<Module> expanded = new HashSet<>();
   private static final Set<LuaScript> expandedScripts = new HashSet<>();
   private static LuaScriptSetting scriptSlider = null;
   private static int scriptSliderX;
   private static int scriptSliderW;
   private int scriptScroll = 0;
   private long openTime;
   private static final float OPEN_ANIM_MS = 300.0F;
   private enum ToolPanel { CONFIGS, SCRIPTS }
   private final Map<ToolPanel, int[]> toolPositions = new LinkedHashMap<>();
   private ToolPanel draggingTool;
   private Module.Category dragging = null;
   private int dragOffX;
   private int dragOffY;
   private static Setting<?> sliderDrag = null;
   private static int sliderX;
   private static int sliderW;
   private Setting<?> captureBind = null;
   private Module captureBindModule = null;
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
   private static final int PW = 132;
   private static final int HDR = 18;
   private static final int MH = 18;
   private static final int SH = 16;
   private static final int CB = 7;
   private static final int CP_SQ = 88;
   private static final int CP_BAR = 7;
   private static final int C_HEADER = 0xF50D0E13;
   private static final int C_BODY = 0xF2151924;
   private static final int C_BORDER = 0xFF2A303D;
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
      if (panelPos.isEmpty() || this.layoutWidth != this.width || this.layoutHeight != this.height) {
         this.initPositions();
      }

      this.refreshConfigList();
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
      panelPos.clear();
      panelScroll.clear();
      this.toolPositions.clear();
      this.layoutWidth = this.width;
      this.layoutHeight = this.height;
      float scale = this.guiScale();
      this.layoutScale = scale;
      int usableWidth = (int)((this.width - 24) / scale);
      int usableHeight = (int)((this.height - 52) / scale);
      int count = 8;
      int columns = Math.max(1, Math.min(count, (usableWidth + 10) / (PW + 10)));
      int rows = (count + columns - 1) / columns;
      this.panelLimit = Math.max(HDR + MH, (usableHeight - (rows - 1) * 12) / rows);
      int originX = (int)(this.width / 2.0F - (columns * (PW + 10) - 10) / 2.0F);
      int originY = (int)(this.height / 2.0F - usableHeight / 2.0F);
      Module.Category[] order = {Module.Category.COMBAT, Module.Category.MOVEMENT, Module.Category.RENDER,
         Module.Category.PLAYER, Module.Category.MISC, Module.Category.GHOST, Module.Category.CLIENT, Module.Category.OLD};
      for (int i = 0; i < order.length; i++) {
         panelPos.put(order[i], new int[]{originX + (i % columns) * (PW + 10), originY + (i / columns) * (this.panelLimit + 12)});
      }
      List<int[]> occupied = new ArrayList<>();
      for (Module.Category category : order) {
         int[] position = panelPos.get(category);
         occupied.add(new int[]{position[0], position[1], this.panelH(category)});
      }
      for (ToolPanel tool : ToolPanel.values()) {
         if (tool == ToolPanel.SCRIPTS && OyVey.scriptManager == null) continue;
         this.placeToolPanel(tool, occupied, originX, originY, columns, usableHeight);
      }
   }

   private void placeToolPanel(ToolPanel tool, List<int[]> occupied, int originX, int originY,
                               int columns, int usableHeight) {
      int minimum = HDR + (tool == ToolPanel.CONFIGS ? 22 : 18) + 12;
      int[] best = null;
      List<Integer> candidates = new ArrayList<>();
      candidates.add(originY);
      for (int[] rectangle : occupied) candidates.add(rectangle[1] + rectangle[2] + 12);
      for (int column = 0; column < columns; column++) {
         int x = originX + column * (PW + 10);
         for (int y : candidates) {
            int bottom = originY + usableHeight;
            boolean blocked = false;
            for (int[] rectangle : occupied) {
               if (x >= rectangle[0] + PW || x + PW <= rectangle[0]) continue;
               if (y < rectangle[1] + rectangle[2] + 12 && y + minimum > rectangle[1] - 12) {
                  blocked = true;
                  break;
               }
               if (rectangle[1] >= y) bottom = Math.min(bottom, rectangle[1] - 12);
            }
            int height = Math.min(this.panelLimit, bottom - y);
            if (!blocked && height >= minimum && (best == null || height > best[2])) {
               best = new int[]{x, y, height};
            }
         }
      }
      if (best == null) {
         int offset = (tool.ordinal() + 1) * 16;
         best = new int[]{originX + (columns - 1) * (PW + 10) - offset, originY + offset,
            Math.max(minimum, this.panelLimit)};
      }
      this.toolPositions.put(tool, best);
      occupied.add(best);
   }
   private float guiScale() {
      ClickGuiModule module = ClickGuiModule.getInstance();
      float requested = module == null ? 1.0F : module.guiScale.getValue();
      float fit = Math.min((this.width - 24.0F) / (3 * (PW + 10) - 10), (this.height - 52.0F) / 250.0F);
      return Math.max(0.25F, Math.min(requested, fit));
   }

   private static void guiText(GuiGraphicsExtractor ctx, String text, int x, int y, int color, boolean shadow) {
      ctx.text(Minecraft.getInstance().font, HudFont.ARIAL.text(text), x, y, color, shadow);
   }

   private static int guiWidth(String text) {
      return Minecraft.getInstance().font.width(HudFont.ARIAL.text(text));
   }

   private static List<String> wrapText(String text, int width) {
      List<String> lines = new ArrayList<>();
      while (!text.isEmpty()) {
         int end = 0;
         while (end < text.length()) {
            int next = text.offsetByCodePoints(end, 1);
            if (end > 0 && guiWidth(text.substring(0, next)) > width) {
               break;
            }
            end = next;
         }
         if (end < text.length()) {
            int space = text.lastIndexOf(' ', end);
            if (space > 0) {
               end = space;
            }
         }
         lines.add(text.substring(0, end));
         text = text.substring(end).stripLeading();
      }
      return lines.isEmpty() ? List.of("") : lines;
   }

   private record TextRow(List<String> label, List<String> value, boolean stacked) {
      int height() {
         return 6 + 12 * (this.label.size() + (this.stacked ? this.value.size() : 0));
      }
   }

   private static TextRow textRow(String label, String value, int width) {
      boolean stacked = !value.isEmpty() && guiWidth(label) + 10 + guiWidth(value) > width;
      return new TextRow(wrapText(label, width), wrapText(value, width), stacked);
   }

   private static void drawTextRow(GuiGraphicsExtractor ctx, TextRow row, int x, int y, int right, int valueColor) {
      for (int i = 0; i < row.label.size(); i++) {
         guiText(ctx, row.label.get(i), x, y + 3 + i * 12, 0xFFD8DAE1, false);
      }
      for (int i = 0; i < row.value.size(); i++) {
         String value = row.value.get(i);
         int valueY = row.stacked ? y + 3 + (row.label.size() + i) * 12 : y + 3;
         guiText(ctx, value, row.stacked ? x : right - guiWidth(value), valueY, valueColor, false);
      }
   }

   private String bindLabel(Bind bind) {
      if (bind.getKey() < 0) {
         return bind.isEmpty() ? ClickGuiModule.t("None") : bind.toString();
      }
      return InputConstants.getKey(new KeyEvent(bind.getKey(), 0, 0)).getDisplayName().getString();
   }

   private String settingValue(Setting<?> setting) {
      if (setting.isEnumSetting()) {
         return setting.currentEnumName();
      }
      if (setting.getValue() instanceof Bind bind) {
         return setting == this.captureBind ? ClickGuiModule.t("Press key...") : this.bindLabel(bind);
      }
      if (setting.getValue() instanceof Number number) {
         return number instanceof Integer ? number.toString()
            : String.format(java.util.Locale.ROOT, "%.4f", number.doubleValue()).replaceAll("0+$", "").replaceAll("\\.$", "");
      }
      return setting.getValue() instanceof Boolean || setting.isColorSetting() ? "" : String.valueOf(setting.getValue());
   }

   private TextRow settingText(Setting<?> setting) {
      int reserved = setting.getValue() instanceof Boolean || setting.isColorSetting() ? 32 : 14;
      return textRow(setting.getName(), this.settingValue(setting), PW - reserved);
   }

   private int settingH(Setting<?> setting) {
      return this.settingText(setting).height() + (setting.isNumberSetting() && setting.hasRestriction() ? 4 : 0);
   }

   private int moduleH(Module module) {
      return 6 + 12 * wrapText(moduleLabel(module), PW - 24).size();
   }

   private TextRow bindText(Module module) {
      String value = module == this.captureBindModule ? ClickGuiModule.t("Press key...") : this.bindLabel(module.getBind());
      return textRow(ClickGuiModule.t("Bind"), value, PW - 14);
   }

   private int bindH(Module module) {
      return this.bindText(module).height();
   }

   private static String moduleLabel(Module module) {
      return module.getDisplayName().replaceAll("([a-z0-9])([A-Z])", "$1 $2");
   }

   private static void chevron(GuiGraphicsExtractor ctx, int x, int y, boolean open, int color) {
      // Tiny thin 'v' exactly like reference
      for (int i = 0; i < 3; i++) {
         int off = open ? 2 - i : i;
         ctx.fill(x + i, y + off, x + i + 1, y + off + 1, color);
         ctx.fill(x + 4 - i, y + off, x + 5 - i, y + off + 1, color);
      }
   }

   private static String categoryIcon(Module.Category cat) {
      return switch (cat) {
         case COMBAT -> "\u25CE";
         case MOVEMENT -> "\u21AF";
         case RENDER -> "\u25C9";
         case PLAYER -> "\u25CB";
         case MISC -> "\u2699";
         case GHOST -> "\u2726";
         case CLIENT -> "\u2630";
         case OLD -> "\u2022";
         default -> "\u25CB";
      };
   }

   public void extractBackground(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
   }

   public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
      float scale = this.guiScale();
      if (this.layoutScale != scale) {
         this.initPositions();
      }
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

      if (this.draggingTool != null) {
         if (GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), 0) == 1) {
            int[] position = this.toolPositions.get(this.draggingTool);
            position[0] = smx - this.dragOffX;
            position[1] = smy - this.dragOffY;
         } else {
            this.draggingTool = null;
         }
      }

      float openT = this.openAnim();
      float animScale = easeOutBack(openT);
      int dimAlpha = (int)(205.0F * openT);
      ctx.fill(0, 0, this.width, this.height, dimAlpha << 24);
      ctx.pose().pushMatrix();
      ctx.pose().translate(cx, cy);
      ctx.pose().scale(scale * animScale, scale * animScale);
      ctx.pose().translate(-cx, -cy);

      for (Module.Category cat : panelPos.keySet()) {
         this.renderPanel(ctx, smx, smy, cat);
      }

      this.renderConfigsMenu(ctx, smx, smy);
      if (OyVey.scriptManager != null) {
         this.renderScriptsMenu(ctx, smx, smy);
      }
      if (colorPicker != null) {
         this.renderColorPicker(ctx, smx, smy);
      }

      ctx.pose().popMatrix();
   }

   private void renderPanel(GuiGraphicsExtractor ctx, int mx, int my, Module.Category cat) {
      int[] pos = panelPos.get(cat);
      int px = pos[0];
      int py = pos[1];
      int height = this.panelH(cat);
      int scroll = this.clampPanelScroll(cat);
      // Screenshot style: soft shadow + dark body + near-black header + thin border
      RenderUtil.roundedRect(ctx, px + 1, py + 3, PW, height, PANEL_R, 0x55000000);
      RenderUtil.roundedRect(ctx, px, py, PW, height, PANEL_R, C_BODY);
      RenderUtil.roundedRect(ctx, px, py, PW, HDR, PANEL_R, C_HEADER);
      ctx.fill(px, py + HDR - PANEL_R, px + PW, py + HDR, C_HEADER);
      guiText(ctx, ClickGuiModule.t(cat.getName()), px + 6, py + 5, 0xFFF2F4F7, false);
      String catIcon = categoryIcon(cat);
      guiText(ctx, catIcon, px + PW - guiWidth(catIcon) - 7, py + 5, 0xFFB9C0CC, false);
      ctx.enableScissor(px, py + HDR, px + PW, py + height);
      int ey = py + HDR - scroll;
      int clippedMouseY = my >= py + HDR && my < py + height ? my : Integer.MIN_VALUE;
      for (Module mod : this.modsOf(cat)) {
         this.renderModuleRow(ctx, mx, clippedMouseY, mod, px, ey);
         ey += this.moduleH(mod);
         if (expanded.contains(mod)) {
            this.renderBindRow(ctx, mx, clippedMouseY, mod, px, ey);
            ey += this.bindH(mod);
            for (Setting<?> setting : this.visibleSettings(mod)) {
               this.renderSettingRow(ctx, mx, clippedMouseY, setting, px, ey);
               ey += this.settingH(setting);
            }
         }
      }
      ctx.disableScissor();
      RenderUtil.maskCorners(ctx, px, py, PW, height, PANEL_R, C_BORDER, false, false, true, true);
      RenderUtil.roundedOutline(ctx, px, py, PW, height, PANEL_R, 1.0F, C_BORDER);
      if (this.contentH(cat) > height) {
         int body = height - HDR;
         int thumb = Math.max(10, body * body / (this.contentH(cat) - HDR));
         int offset = scroll * (body - thumb) / (this.contentH(cat) - height);
         ctx.fill(px + PW - 2, py + HDR + offset, px + PW - 1, py + HDR + offset + thumb, 0x887EACBD);
      }
   }

   private void renderModuleRow(GuiGraphicsExtractor ctx, int mx, int my, Module mod, int px, int ey) {
      int height = this.moduleH(mod);
      boolean hover = mx >= px && mx < px + PW && my >= ey && my < ey + height;
      if (mod.isEnabled()) {
         // Reference teal -> blue highlight, full-bleed like screenshot
         RenderUtil.horizontalGradient(ctx, px, ey, px + PW, ey + height,
            new Color(74, 157, 174, 238), new Color(44, 108, 169, 238));
      }
      if (hover) {
         ctx.fill(px, ey, px + PW, ey + height, 0x18FFFFFF);
      }
      List<String> lines = wrapText(moduleLabel(mod), PW - 24);
      for (int i = 0; i < lines.size(); i++) {
         guiText(ctx, lines.get(i), px + 6, ey + 3 + i * 12, mod.isEnabled() ? 0xFFF4F8FB : 0xFFAEB6C2, false);
      }
      chevron(ctx, px + PW - 12, ey + 7, expanded.contains(mod), mod.isEnabled() ? 0xFFEAF4F7 : 0xFF7E8694);
   }

   private void renderSettingRow(GuiGraphicsExtractor ctx, int mx, int my, Setting<?> setting, int px, int ey) {
      int height = this.settingH(setting);
      boolean hover = mx >= px && mx < px + PW && my >= ey && my < ey + height;
      ctx.fill(px, ey, px + PW, ey + height, hover ? 0xFF1B2831 : 0xF013151D);
      ctx.fill(px + 3, ey, px + 4, ey + height, 0xFF4A9DAE);
      int right = px + PW - 7;
      drawTextRow(ctx, this.settingText(setting), px + 7, ey, right, 0xFF80BDD8);
      if (setting.getValue() instanceof Boolean value) {
         drawCheckbox(ctx, right - 7, ey + 5, value, ey);
      } else if (setting.isNumberSetting() && setting.hasRestriction()) {
         double min = ((Number)setting.getMin()).doubleValue();
         double max = ((Number)setting.getMax()).doubleValue();
         double value = ((Number)setting.getValue()).doubleValue();
         float fraction = max <= min ? 0 : (float)Math.max(0, Math.min(1, (value - min) / (max - min)));
         int start = px + 4;
         int width = PW - 8;
         int barY = ey + height - 3;
         ctx.fill(start, barY, start + width, barY + 2, 0x334A6170);
         ctx.fill(start, barY, start + Math.round(width * fraction), barY + 2, acc(ey, 220));
      } else if (setting.isColorSetting()) {
         Color color = (Color)setting.getValue();
         ctx.fill(right - 11, ey + 4, right + 1, ey + 13, 0xFF808080);
         ctx.fill(right - 10, ey + 5, right, ey + 12, color.getRGB());
      }
      ctx.fill(px + 4, ey + height - 1, px + PW - 4, ey + height, 0x182A3540);
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
      float sc = this.guiScale();
      int mx = (int)((click.x() - this.width / 2.0) / sc + this.width / 2.0);
      int my = (int)((click.y() - this.height / 2.0) / sc + this.height / 2.0);
      int btn = click.button();
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

      for (ToolPanel tool : this.toolPositions.keySet()) {
         int[] geometry = this.toolGeometry(tool);
         if (!this.inToolPanel(mx, my, geometry)) continue;
         if (my < geometry[1] + HDR) {
            this.configTyping = false;
            if (btn == 0) {
               this.dragging = null;
               this.draggingTool = tool;
               this.dragOffX = mx - geometry[0];
               this.dragOffY = my - geometry[1];
            }
            return true;
         }
         if (tool != ToolPanel.CONFIGS) this.configTyping = false;
         return tool == ToolPanel.CONFIGS ? this.clickConfigsMenu(mx, my, btn) : this.clickScriptsMenu(mx, my, btn);
      }
      this.configTyping = false;

      for (Module.Category cat : panelPos.keySet()) {
         int[] pos = panelPos.get(cat);
         int px = pos[0];
         int py = pos[1];
         if (btn == 0 && mx >= px && mx <= px + PW && my >= py && my <= py + HDR) {
            this.draggingTool = null;
            this.dragging = cat;
            this.dragOffX = mx - px;
            this.dragOffY = my - py;
            return true;
         }

         if (my < py + HDR || my >= py + this.panelH(cat)) {
            continue;
         }
         int ey = py + HDR - this.clampPanelScroll(cat);

         for (Module mod : this.modsOf(cat)) {
            if (mx >= px && mx <= px + PW && my >= ey && my <= ey + this.moduleH(mod)) {
               if (btn == 0 && mx >= px + PW - 18) {
                  this.toggleExpand(mod);
               } else if (btn == 0) {
                  mod.toggle();
               } else if (btn == 1) {
                  this.toggleExpand(mod);
               }

               return true;
            }

            ey += this.moduleH(mod);
            if (expanded.contains(mod)) {
               if (mx >= px && mx <= px + PW && my >= ey && my <= ey + this.bindH(mod)) {
                  if (btn == 1) {
                     mod.setBind(-1);
                     this.captureBindModule = null;
                  } else {
                     this.captureBindModule = this.captureBindModule == mod ? null : mod;
                     this.captureBind = null;
                  }

                  return true;
               }

               ey += this.bindH(mod);

               for (Setting<?> s : this.visibleSettings(mod)) {
                  if (mx >= px && mx <= px + PW && my >= ey && my <= ey + this.settingH(s)) {
                     this.handleSetting(s, mx, ey, px, btn);
                     return true;
                  }

                  ey += this.settingH(s);
               }
            }
         }
      }

      return super.mouseClicked(click, doubled);
   }

   private boolean clickScriptsMenu(int mx, int my, int btn) {
      if (OyVey.scriptManager == null) return false;
      int[] g = this.scriptsMenuGeom();
      int px = g[0];
      int py = g[1];
      int menuH = g[2];
      int listTop = g[3];
      int listH = g[4];
      int footerY = g[6];
      int footerH = g[7];
      if (mx >= px && mx <= px + PW && my >= footerY && my <= footerY + footerH) {
         OyVey.scriptManager.reload();
         expandedScripts.clear();
         this.scriptScroll = 0;
         return true;
      }

      if (mx >= px && mx <= px + PW && my >= listTop && my < listTop + listH) {
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
                        scriptSliderW = PW - 8;
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
         if (mx >= px && mx <= px + PW && my >= py && my <= py + menuH) {
            return true;
         }

         return true;
      }
   }

   private boolean clickConfigsMenu(int mx, int my, int btn) {
      int[] g = this.configsMenuGeom();
      int px = g[0];
      int py = g[1];
      int menuH = g[2];
      int listTop = g[3];
      int listH = g[4];
      int footerY = g[6];
      int footerH = g[7];
      if (mx >= px && mx <= px + PW && my >= listTop && my < listTop + listH) {
         if (!this.configList.isEmpty()) {
            int ey = listTop - this.configScroll;

            for (String cfg : this.configList) {
               if (my >= ey && my < ey + this.configRowH(cfg)) {
                  this.loadConfig(cfg);

                  this.configTyping = false;
                  return true;
               }

               ey += this.configRowH(cfg);
            }
         }

         return true;
      } else {
         int inputW = PW - 44;
         if (mx >= px + 4 && mx <= px + inputW && my >= footerY + 3 && my <= footerY + footerH - 3) {
            this.configTyping = true;
            return true;
         }

         int saveBtnX = px + inputW + 2;
         if (mx >= saveBtnX && mx <= px + PW - 4 && my >= footerY + 3 && my <= footerY + footerH - 3) {
            if (!this.configSaveName.isBlank()) {
               this.saveConfig(this.configSaveName.trim());
               this.configSaveName = "";
               this.configTyping = false;
               this.refreshConfigList();
            }

            return true;
         } else {
            if (mx >= px && mx <= px + PW && my >= py && my <= py + menuH) {
               return true;
            }

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
         int bw = PW - 8;
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
         int rightEdge = px + PW + 6 + 88 + 8;
         cpX = rightEdge > this.width ? px - 88 - 10 : px + PW + 6;
         cpY = ey;
      }
   }

   public boolean mouseReleased(MouseButtonEvent click) {
      if (click.button() == 0) {
         this.dragging = null;
         this.draggingTool = null;
         sliderDrag = null;
         scriptSlider = null;
         cpDragA = false;
         cpDragH = false;
         cpDragSV = false;
      }

      return super.mouseReleased(click);
   }

   public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
      float scale = this.guiScale();
      int x = (int)((mx - this.width / 2.0) / scale + this.width / 2.0);
      int y = (int)((my - this.height / 2.0) / scale + this.height / 2.0);
      for (ToolPanel tool : this.toolPositions.keySet()) {
         int[] geometry = this.toolGeometry(tool);
         if (this.inToolPanel(x, y, geometry)) {
            int max = Math.max(0, geometry[5] - geometry[4]);
            if (tool == ToolPanel.CONFIGS) {
               this.configScroll = Math.max(0, Math.min(max, this.configScroll - (int)(vAmt * 32)));
            } else {
               this.scriptScroll = Math.max(0, Math.min(max, this.scriptScroll - (int)(vAmt * 26)));
            }
            return true;
         }
      }
      for (Module.Category category : panelPos.keySet()) {
         int[] pos = panelPos.get(category);
         if (x >= pos[0] && x < pos[0] + PW && y >= pos[1] && y < pos[1] + this.panelH(category)) {
            panelScroll.put(category, this.clampPanelScroll(category) - (int)(vAmt * MH * 2));
            this.clampPanelScroll(category);
            return true;
         }
      }
      return super.mouseScrolled(mx, my, hAmt, vAmt);
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

   private int contentH(Module.Category cat) {
      int height = HDR;
      for (Module module : this.modsOf(cat)) {
         height += this.moduleH(module);
         if (expanded.contains(module)) {
            height += this.bindH(module);
            for (Setting<?> setting : this.visibleSettings(module)) {
               height += this.settingH(setting);
            }
         }
      }
      return height;
   }

   private int panelH(Module.Category cat) {
      return Math.min(this.contentH(cat), this.panelLimit);
   }

   private int clampPanelScroll(Module.Category cat) {
      int value = Math.max(0, Math.min(panelScroll.getOrDefault(cat, 0), this.contentH(cat) - this.panelH(cat)));
      panelScroll.put(cat, value);
      return value;
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
      int height = this.bindH(mod);
      boolean hover = mx >= px && mx < px + PW && my >= ey && my < ey + height;
      ctx.fill(px, ey, px + PW, ey + height, hover ? 0xFF1A2730 : 0xF0121319);
      drawTextRow(ctx, this.bindText(mod), px + 7, ey, px + PW - 7,
         mod == this.captureBindModule ? 0xFFB1E4F6 : 0xFF80BDD8);
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

   private int configRowH(String name) {
      return 6 + 12 * wrapText(name, PW - 12).size();
   }

   private int configsContentH() {
      return this.configList.isEmpty() ? 18 : this.configList.stream().mapToInt(this::configRowH).sum();
   }

   private int[] menuGeom(ToolPanel tool, int contentH, int footerH) {
      int[] position = this.toolPositions.get(tool);
      int listH = Math.min(contentH, Math.max(12, position[2] - HDR - footerH));
      int listTop = position[1] + HDR;
      int footerY = listTop + listH;
      return new int[]{position[0], position[1], HDR + listH + footerH,
         listTop, listH, contentH, footerY, footerH};
   }

   private int[] scriptsMenuGeom() {
      return this.menuGeom(ToolPanel.SCRIPTS, this.scriptsContentH(), 18);
   }

   private int[] configsMenuGeom() {
      return this.menuGeom(ToolPanel.CONFIGS, this.configsContentH(), 22);
   }

   private int[] toolGeometry(ToolPanel tool) {
      return tool == ToolPanel.CONFIGS ? this.configsMenuGeom() : this.scriptsMenuGeom();
   }

   private boolean inToolPanel(int x, int y, int[] geometry) {
      return x >= geometry[0] && x < geometry[0] + PW && y >= geometry[1] && y < geometry[1] + geometry[2];
   }

   private void drawMenuFrame(GuiGraphicsExtractor ctx, int px, int py, int height, String title, long badge) {
      RenderUtil.roundedRect(ctx, px + 1, py + 2, PW, height, 3, 0x55000000);
      RenderUtil.roundedRect(ctx, px, py, PW, height, 3, 0xF019191F);
      RenderUtil.roundedRect(ctx, px, py, PW, HDR, 3, 0xFA202026);
      ctx.fill(px, py + HDR - 3, px + PW, py + HDR, 0xFA202026);
      guiText(ctx, title, px + 6, py + 5, 0xFFE6E7EB, false);
      if (badge > 0) {
         String count = Long.toString(badge);
         guiText(ctx, count, px + PW - guiWidth(count) - 7, py + 5, 0xFF80BDD8, false);
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

      ctx.enableScissor(px, listTop, px + PW, listTop + listH);
      int ey = listTop - this.scriptScroll;
      if (scripts.isEmpty()) {
         String msg = "No scripts found";
         guiText(ctx, msg, px + (PW - guiWidth(msg)) / 2, listTop + 2, -8947798, false);
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
         int sbX = px + PW - 3;
         ctx.fill(sbX, listTop, sbX + 2, listTop + listH, 587202559);
         int thumbH = Math.max(12, (int)((float)listH * listH / contentH));
         int thumbY = listTop + (maxScroll > 0 ? (int)((float)(listH - thumbH) * this.scriptScroll / maxScroll) : 0);
         ctx.fill(sbX, thumbY, sbX + 2, thumbY + thumbH, -1996488705);
      }

      boolean rHov = mx >= px && mx <= px + PW && my >= footerY && my <= footerY + footerH;
      ctx.fill(px, footerY, px + PW, footerY + footerH, rHov ? 548055807 : -871888881);
      String rl = ClickGuiModule.t("[ Reload Scripts ]");
      guiText(ctx, rl, px + (PW - guiWidth(rl)) / 2, footerY + (footerH - 8) / 2, -8939077, false);
      RenderUtil.maskCorners(ctx, px, py, PW, menuH, 4, -14934996, false, false, true, true);
   }

   private void renderScriptRow(GuiGraphicsExtractor ctx, int mx, int my, LuaScript sx, int px, int ey) {
      Font font = Minecraft.getInstance().font;
      boolean on = sx.isEnabled();
      boolean hov = mx >= px && mx <= px + PW && my >= ey && my <= ey + 13;
      if (hov) {
         ctx.fill(px, ey, px + PW, ey + 13, 548055807);
      }

      guiText(ctx, sx.getName(), px + 4, ey + 2 + 1, -2236929, false);
      if (!sx.getDescription().isEmpty()) {
         String desc = sx.getDescription();
         int maxDescW = PW - guiWidth(sx.getName()) - 30;

         while (desc.length() > 3 && guiWidth(desc) > maxDescW) {
            desc = desc.substring(0, desc.length() - 2) + ".";
         }

         guiText(ctx, desc, px + 4 + guiWidth(sx.getName()) + 4, ey + 2 + 1, -8947798, false);
      }

      int cbX = px + PW - 7 - 4;
      int cbY = ey + 3;
      drawCheckbox(ctx, cbX, cbY, on, ey);
      if (!sx.getScriptSettings().isEmpty()) {
         String g = expandedScripts.contains(sx) ? "v" : ">";
         guiText(ctx, g, cbX - guiWidth(g) - 3, ey + 2 + 1, on ? -7829368 : -12303292, false);
      }

      ctx.fill(px + 2, ey + 13 - 1, px + PW - 2, ey + 13, -14803408);
   }

   private void renderScriptSettingRow(GuiGraphicsExtractor ctx, int mx, int my, LuaScriptSetting ss, int px, int ey) {
      Font font = Minecraft.getInstance().font;
      boolean hov = mx >= px && mx <= px + PW && my >= ey && my <= ey + 11;
      ctx.fill(px, ey, px + PW, ey + 11, hov ? 548055807 : -871888881);
      ctx.fill(px + 3, ey, px + 4, ey + 11, 872415231);
      String name = ss.getName();
      guiText(ctx, name, px + 7, ey + 1 + 1, -4473925, false);
      int re = px + PW - 4;
      switch (ss.getType()) {
         case BOOLEAN:
            int cbX = re - 7;
            int cbY = ey + 2;
            drawCheckbox(ctx, cbX, cbY, ss.getBoolean(), ey);
            break;
         case SLIDER:
            double pct = (ss.getDouble() - ss.getMin()) / (ss.getMax() - ss.getMin());
            int bx = px + 4;
            int bw = PW - 8;
            int barY = ey + 11 - 4;
            RenderUtil.roundedRect(ctx, bx, barY, bw, 2, 1, -15066584);
            RenderUtil.roundedRect(ctx, bx, barY, (int)(bw * pct), 2, 1, acc(ey, 200));
            String val = String.format("%.2f", ss.getDouble());
            guiText(ctx, val, re - guiWidth(val), ey + 1 + 1, -7829334, false);
            break;
         case ENUM:
            String cur = ss.getString();
            guiText(ctx, cur, re - guiWidth(cur), ey + 1 + 1, acc(ey, 220), false);
      }

      ctx.fill(px + 4, ey + 11 - 1, px + PW - 4, ey + 11, 419430399);
   }

   private void applyScriptSlider(int mx) {
      if (scriptSlider != null) {
         float pct = Math.max(0.0F, Math.min(1.0F, (float)(mx - scriptSliderX) / scriptSliderW));
         double nv = scriptSlider.getMin() + pct * (scriptSlider.getMax() - scriptSlider.getMin());
         scriptSlider.setDoubleValue(nv);
      }
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

      ctx.enableScissor(px, listTop, px + PW, listTop + listH);
      if (this.configList.isEmpty()) {
         String msg = ClickGuiModule.t("No configs yet");
         guiText(ctx, msg, px + (PW - guiWidth(msg)) / 2, listTop + 3, -8947798, false);
      } else {
         int ey = listTop - this.configScroll;

         for (String cfg : this.configList) {
            int rowH = this.configRowH(cfg);
            boolean hover = mx >= px && mx < px + PW && my >= Math.max(ey, listTop)
               && my < Math.min(ey + rowH, listTop + listH);
            if (hover) ctx.fill(px, ey, px + PW, ey + rowH, 0x202D8AA5);
            List<String> lines = wrapText(cfg, PW - 12);
            for (int i = 0; i < lines.size(); i++) {
               guiText(ctx, lines.get(i), px + 6, ey + 3 + i * 12, hover ? -1 : 0xFFCFD3DD, false);
            }
            ey += rowH;
         }
      }

      ctx.disableScissor();
      if (contentH > listH) {
         int sbX = px + PW - 3;
         ctx.fill(sbX, listTop, sbX + 2, listTop + listH, 587202559);
         int thumbH = Math.max(12, (int)((float)listH * listH / contentH));
         int thumbY = listTop + (maxScroll > 0 ? (int)((float)(listH - thumbH) * this.configScroll / maxScroll) : 0);
         ctx.fill(sbX, thumbY, sbX + 2, thumbY + thumbH, -1996488705);
      }

      ctx.fill(px, footerY, px + PW, footerY + footerH, -871888881);
      ctx.fill(px, footerY, px + PW, footerY + 1, 872415231);
      int inputW = PW - 44;
      boolean inputHov = mx >= px + 4 && mx <= px + inputW && my >= footerY + 3 && my <= footerY + footerH - 3;
      ctx.fill(px + 4, footerY + 3, px + inputW, footerY + footerH - 3, this.configTyping ? 1157627903 : (inputHov ? 587202559 : 419430399));
      boolean blink = this.configTyping && System.currentTimeMillis() % 900L < 450L;
      String dispName = this.configSaveName.isEmpty() ? (this.configTyping ? "" : ClickGuiModule.t("name...")) : this.configSaveName;
      guiText(ctx, dispName + (blink ? "|" : ""), px + 7, footerY + (footerH - 8) / 2, this.configSaveName.isEmpty() ? -12303292 : -1118482, false);
      int saveBtnX = px + inputW + 2;
      int saveBtnRight = px + PW - 4;
      boolean shov = mx >= saveBtnX && mx <= saveBtnRight && my >= footerY + 3 && my <= footerY + footerH - 3;
      ctx.fill(saveBtnX, footerY + 3, saveBtnRight, footerY + footerH - 3, shov ? acc(footerY, 200) : 872415231);
      String save = ClickGuiModule.t("Save");
      guiText(ctx, save, saveBtnX + (saveBtnRight - saveBtnX - guiWidth(save)) / 2, footerY + (footerH - 8) / 2, -1, false);
      RenderUtil.maskCorners(ctx, px, py, PW, menuH, 4, -14934996, false, false, true, true);
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
