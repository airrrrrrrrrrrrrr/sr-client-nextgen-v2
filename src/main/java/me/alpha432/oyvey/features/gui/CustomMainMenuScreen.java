package me.alpha432.oyvey.features.gui;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import me.alpha432.oyvey.util.Lang;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class CustomMainMenuScreen extends Screen {
   private static final FontDescription BODY_FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath("oyvey", "clickgui"));
   private static final FontDescription HEADER_FONT = new FontDescription.Resource(Identifier.fromNamespaceAndPath("oyvey", "clickgui_header"));
   private static final int TEXT = 0xEDF3FC;
   private static final int MUTED = 0x98A9C1;
   private long openTime;
   private float layoutScale;
   private float fade = 1;
   private int left;
   private int top;
   private int accent = 0x84BFFF;

   public CustomMainMenuScreen() {
      super(Component.literal("SR Client 3.0"));
   }

   @Override
   protected void init() {
      this.openTime = System.nanoTime();
      this.layoutScale = Math.min(1.0F, Math.min((this.width - 32) / 424.0F, (this.height - 64) / 284.0F));
      this.left = Math.round((this.width - 424 * this.layoutScale) / 2);
      this.top = Math.round((this.height - 284 * this.layoutScale) / 2 - 5 * this.layoutScale);

      MenuButton multiplayer = this.addMenuButton("menu.multiplayer", "menu.description.multiplayer", Icon.SERVER,
         0, 92, 206, 52, true, () -> this.minecraft.setScreen(new CustomServerListScreen()));
      this.addMenuButton("menu.singleplayer", "menu.description.singleplayer", Icon.WORLD,
         218, 92, 206, 52, false, () -> this.minecraft.setScreen(new SelectWorldScreen(this)));
      this.addMenuButton("menu.accounts", "", Icon.ACCOUNT,
         0, 156, 206, 36, false, () -> this.minecraft.setScreen(new AccountManagerScreen()));
      this.addMenuButton("menu.options", "", Icon.OPTIONS,
         218, 156, 206, 36, false, () -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options, false)));
      this.addMenuButton("menu.proxies", "", Icon.PROXY,
         0, 200, 424, 36, false, () -> this.minecraft.setScreen(new ProxyScreen()));
      this.addMenuButton("menu.preferences", "", Icon.NONE,
         0, 254, 136, 28, false, () -> this.minecraft.setScreen(new WelcomeScreen(false)));
      this.addMenuButton("menu.vanilla", "", Icon.NONE,
         144, 254, 136, 28, false, this::openVanillaMenu);
      this.addMenuButton("menu.quit", "", Icon.NONE,
         288, 254, 136, 28, false, () -> this.minecraft.stop());

      Component setupText = styled(Lang.get("menu.setup"), false);
      this.addRenderableWidget(new MenuButton(14, this.height - 26, Math.round(this.font.width(setupText) * 0.85F) + 30, 20,
         setupText, "", Icon.TOGGLE, false, true, 0.85F, () -> {
            if (OyVey.preferences != null) {
               OyVey.preferences.setShowOnLaunch(!OyVey.preferences.isShowOnLaunch());
               OyVey.configManager.save();
            }
         }));
      this.setInitialFocus(multiplayer);
   }

   private MenuButton addMenuButton(String label, String description, Icon icon, int x, int y, int w, int h,
      boolean primary, Runnable action) {
      return this.addRenderableWidget(new MenuButton(this.left + Math.round(x * this.layoutScale),
         this.top + Math.round(y * this.layoutScale), Math.round(w * this.layoutScale), Math.round(h * this.layoutScale),
         styled(Lang.get(label), true), description.isEmpty() ? "" : Lang.get(description), icon, primary,
         false, this.layoutScale, action));
   }

   private void openVanillaMenu() {
      ClickGuiModule cfg = ClickGuiModule.getInstance();
      if (cfg != null) {
         cfg.customMenu.setValue(false);
         OyVey.configManager.save();
      }
      this.minecraft.setScreen(new TitleScreen());
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) { }

   @Override
   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      float age = (System.nanoTime() - this.openTime) / 1_000_000_000.0F;
      this.fade = Mth.clamp(age / 0.28F, 0, 1);
      // Lift dark theme colors enough to keep labels and focus outlines legible.
      this.accent = blend(OyVey.colorManager.getColorAsInt(), 0x84BFFF, 0.60F);
      RenderUtil.gradient(g, 0, 0, this.width, this.height,
         opaque(blend(0x101A2A, this.accent, 0.025F)), 0xFF080D16, 0xFF0D0D17, 0xFF171829);

      // Sparse, time-based details avoid a visible seam or frame-dependent motion.
      for (int i = 0; i < 32; i++) {
         int x = (int)(((i * 0.618034 + age * 0.0015) % 1) * this.width);
         int y = (int)(((i * 0.381966 + 0.13) % 1) * this.height);
         g.fill(x, y, x + 1, y + 1, color(0xAAC7ED, 24));
      }

      int cx = this.width / 2;
      float s = this.layoutScale;
      this.centered(g, styled("SR", true), cx, this.top, 4.1F * s, color(TEXT, 255));
      this.drawTracked(g, "CLIENT", cx, this.top + 48 * s, s, color(MUTED, 255));
      RenderUtil.roundedRect(g, cx + 37 * s, this.top + 10 * s, 27 * s, 14 * s, 4 * s, color(this.accent, 20));
      this.centered(g, styled("3.0", true), cx + 50.5F * s, this.top + 11 * s, 0.85F * s, color(this.accent, 255));
      g.fill(cx - Math.round(12 * s), this.top + Math.round(75 * s), cx + Math.round(12 * s),
         this.top + Math.round(75 * s) + 1, color(this.accent, 125));

      Component user = styled(this.minecraft.getUser().getName(), false);
      this.text(g, user, this.width - 18 - this.font.width(user) * 0.9F, 15, 0.9F, color(MUTED, 235));
      g.fill(14, this.height - 34, this.width - 14, this.height - 33, color(MUTED, 24));
      String version = this.width < 430 ? "SR Client 3.0" : "Minecraft " + SharedConstants.getCurrentVersion().name() + "  /  SR Client 3.0";
      Component versionText = styled(version, false);
      this.text(g, versionText, this.width - 18 - this.font.width(versionText) * 0.8F, this.height - 20,
         0.8F, color(MUTED, 220));
      super.extractRenderState(g, mx, my, delta);
   }

   private void drawTracked(GuiGraphicsExtractor g, String text, float cx, float y, float scale, int color) {
      float width = 0;
      for (int i = 0; i < text.length(); i++) width += this.font.width(styled(text.substring(i, i + 1), true)) + 2;
      float x = cx - (width - 2) * scale / 2;
      for (int i = 0; i < text.length(); i++) {
         Component letter = styled(text.substring(i, i + 1), true);
         this.text(g, letter, x, y, scale, color);
         x += (this.font.width(letter) + 2) * scale;
      }
   }

   private void centered(GuiGraphicsExtractor g, Component text, float cx, float y, float scale, int color) {
      this.text(g, text, cx - this.font.width(text) * scale / 2, y, scale, color);
   }

   private void text(GuiGraphicsExtractor g, Component text, float x, float y, float scale, int color) {
      g.pose().pushMatrix();
      g.pose().translate(x, y);
      g.pose().scale(scale, scale);
      g.text(this.font, text, 0, 0, color, false);
      g.pose().popMatrix();
   }

   private int color(int rgb, int alpha) {
      return Math.round(alpha * this.fade) << 24 | rgb & 0xFFFFFF;
   }

   private static int opaque(int rgb) {
      return 0xFF000000 | rgb & 0xFFFFFF;
   }

   private static int blend(int from, int to, float amount) {
      int r = Math.round(Mth.lerp(amount, from >> 16 & 255, to >> 16 & 255));
      int g = Math.round(Mth.lerp(amount, from >> 8 & 255, to >> 8 & 255));
      int b = Math.round(Mth.lerp(amount, from & 255, to & 255));
      return r << 16 | g << 8 | b;
   }

   private static Component styled(String text, boolean header) {
      return Component.literal(text).withStyle(style -> style.withFont(header ? HEADER_FONT : BODY_FONT));
   }

   @Override
   public boolean isPauseScreen() { return false; }

   @Override
   public boolean shouldCloseOnEsc() { return false; }

   private enum Icon { SERVER, WORLD, ACCOUNT, OPTIONS, CAPE, PROXY, TOGGLE, NONE }

   private final class MenuButton extends Button {
      private final String description;
      private final Icon icon;
      private final boolean primary;
      private final boolean footer;
      private final float scale;
      private float hover;
      private long lastFrame = System.nanoTime();

      MenuButton(int x, int y, int w, int h, Component label, String description, Icon icon, boolean primary,
         boolean footer, float scale, Runnable action) {
         super(x, y, w, h, label, ignored -> action.run(), DEFAULT_NARRATION);
         this.description = description;
         this.icon = icon;
         this.primary = primary;
         this.footer = footer;
         this.scale = scale;
      }

      @Override
      protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
         long now = System.nanoTime();
         float elapsed = Math.min(0.05F, (now - this.lastFrame) / 1_000_000_000.0F);
         this.lastFrame = now;
         this.hover += ((this.isHoveredOrFocused() ? 1 : 0) - this.hover) * (1 - (float)Math.exp(-elapsed * 16));
         float s = this.scale;
         int x = this.getX();
         int y = this.getY();
         int w = this.getWidth();
         int h = this.getHeight();
         int base = this.primary ? blend(0x182B43, accent, 0.14F) : 0x171E2B;
         int bg = blend(base, this.primary ? 0x2E4162 : 0x263249, this.hover * 0.8F);
         if (!this.footer) {
            RenderUtil.roundedRectGradient(g, x, y, w, h, 6 * s,
               color(bg, 250), color(blend(bg, 0x0B111C, 0.25F), 250), true);
            RenderUtil.roundedOutline(g, x, y, w, h, 6 * s, 0.75F,
               color(this.primary || this.isFocused() ? accent : MUTED, Math.round((this.primary ? 120 : 38) + this.hover * 65)));
         }
         if (this.icon == Icon.TOGGLE) {
            boolean checked = OyVey.preferences != null && OyVey.preferences.isShowOnLaunch();
            RenderUtil.roundedRect(g, x, y + 5, 13, 10, 3, color(checked ? accent : MUTED, checked ? 190 : 65));
            RenderUtil.roundedRect(g, x + (checked ? 7 : 2), y + 7, 4, 6, 2, color(TEXT, 255));
            text(g, this.getMessage(), x + 20, y + 4, s, color(this.isHoveredOrFocused() ? TEXT : MUTED, 255));
            return;
         }
         boolean large = !this.description.isEmpty();
         float labelScale = (large ? 1.1F : 1.0F) * s;
         float labelY = large ? y + 9 * s : y + (h - 12 * s) / 2;
         if (this.icon == Icon.NONE) {
            centered(g, this.getMessage(), x + w / 2.0F, labelY, labelScale, color(this.isHoveredOrFocused() ? TEXT : MUTED, 255));
            return;
         }
         this.drawIcon(g, x + 12 * s, y + (h - 16 * s) / 2, s, color(this.primary ? accent : MUTED, 255));
         text(g, this.getMessage(), x + 38 * s, labelY, labelScale, color(TEXT, 255));
         if (large) text(g, styled(this.description, false), x + 38 * s, y + 29 * s, 0.85F * s, color(MUTED, 255));
         this.arrow(g, x + w - 17 * s + this.hover * 2 * s, y + h / 2.0F - 2 * s, s,
            color(this.primary ? accent : MUTED, Math.round(100 + this.hover * 155)));
      }

      private void drawIcon(GuiGraphicsExtractor g, float x, float y, float s, int c) {
         g.pose().pushMatrix();
         g.pose().translate(x, y);
         g.pose().scale(s, s);
         switch (this.icon) {
            case SERVER -> {
               RenderUtil.roundedOutline(g, 1, 1, 14, 14, 3, 1, c);
               g.fill(2, 7, 14, 8, c);
               g.fill(4, 4, 6, 5, c);
               g.fill(4, 11, 6, 12, c);
            }
            case WORLD -> {
               RenderUtil.roundedOutline(g, 1, 1, 14, 14, 7, 1, c);
               RenderUtil.roundedOutline(g, 5, 1, 6, 14, 3, 1, c);
               g.fill(2, 7, 14, 8, c);
            }
            case ACCOUNT -> {
               RenderUtil.roundedOutline(g, 5, 1, 6, 6, 3, 1, c);
               RenderUtil.roundedOutline(g, 2, 10, 12, 5, 2, 1, c);
            }
            case OPTIONS -> {
               g.fill(1, 3, 15, 4, c);
               g.fill(1, 8, 15, 9, c);
               g.fill(1, 13, 15, 14, c);
               g.fill(4, 1, 6, 6, c);
               g.fill(10, 6, 12, 11, c);
               g.fill(6, 11, 8, 16, c);
            }
            case CAPE -> {
               RenderUtil.roundedOutline(g, 3, 2, 10, 13, 2, 1, c);
               g.fill(6, 1, 10, 3, c);
               g.fill(7, 5, 8, 12, c);
            }
            case PROXY -> {
               g.fill(1, 4, 14, 5, c);
               g.fill(12, 2, 13, 7, c);
               g.fill(13, 3, 14, 6, c);
               g.fill(2, 11, 15, 12, c);
               g.fill(3, 9, 4, 14, c);
               g.fill(2, 10, 3, 13, c);
            }
            default -> { }
         }
         g.pose().popMatrix();
      }

      private void arrow(GuiGraphicsExtractor g, float x, float y, float s, int c) {
         RenderUtil.rect(g, x, y, x + s, y + 5 * s, c);
         RenderUtil.rect(g, x + s, y + s, x + 2 * s, y + 4 * s, c);
         RenderUtil.rect(g, x + 2 * s, y + 2 * s, x + 3 * s, y + 3 * s, c);
      }
   }
}
