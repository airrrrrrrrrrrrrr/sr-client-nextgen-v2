package me.alpha432.oyvey.features.gui;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.util.render.RenderUtil;
import me.alpha432.oyvey.util.render.SkijaGlyphProvider;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

/** Shared typography, native controls and panel styling for the client menus. */
abstract class ClientMenuScreen extends Screen {
   static final int TEXT = 0xFFEAF1FC, MUTED = 0xFF99AAC2;
   private static final FontDescription BODY = new FontDescription.Resource(Identifier.fromNamespaceAndPath("oyvey", "clickgui"));
   private static final FontDescription HEADER = new FontDescription.Resource(Identifier.fromNamespaceAndPath("oyvey", "clickgui_header"));
   int px, py, pw, ph, accent;

   ClientMenuScreen(String title) { super(Component.literal(title)); }

   void layout(int preferredWidth, int preferredHeight) {
      pw = Math.min(preferredWidth, width - 24);
      ph = Math.min(preferredHeight, height - 24);
      px = (width - pw) / 2;
      py = (height - ph) / 2;
      int raw = OyVey.colorManager.getColorAsInt();
      accent = 0xFF000000 | (((raw >> 16 & 255) + 132) / 2 << 16)
         | (((raw >> 8 & 255) + 191) / 2 << 8) | ((raw & 255) + 255) / 2;
   }

   static Component styled(String text, boolean heading) {
      return Component.literal(text).withStyle(s -> s.withFont(heading ? HEADER : BODY));
   }

   void text(GuiGraphicsExtractor g, String text, int x, int y, int color, boolean heading) {
      g.text(font, styled(text, heading), x, y, color, false);
   }

   String fit(String value, int width) {
      if (width <= 0) return "";
      if (font.width(styled(value, false)) <= width) return value;
      String result = value;
      while (!result.isEmpty() && font.width(styled(result + "…", false)) > width)
         result = result.substring(0, result.offsetByCodePoints(result.length(), -1));
      return result.isEmpty() ? "" : result + "…";
   }

   void background(GuiGraphicsExtractor g, String title, String subtitle) {
      RenderUtil.gradient(g, 0, 0, width, height, 0xFF111C2B, 0xFF080E18, 0xFF10111C, 0xFF182137);
      RenderUtil.dropShadow(g, px, py, pw, ph, 9, 16, 110);
      RenderUtil.roundedRect(g, px, py, pw, ph, 9, 0xF2111926);
      RenderUtil.roundedOutline(g, px, py, pw, ph, 9, 1, 0x384F6685);
      text(g, title, px + 14, py + 13, TEXT, true);
      text(g, fit(subtitle, pw - 28), px + 14, py + 32, MUTED, false);
      g.fill(px + 14, py + 53, px + pw - 14, py + 54, 0x284F6685);
   }

   Button button(String title, int x, int y, int w, int h, Runnable action) {
      return addRenderableWidget(new MenuButton(title, x, y, w, h, action));
   }

   private final class MenuButton extends Button {
      MenuButton(String title, int x, int y, int w, int h, Runnable action) {
         super(x, y, w, h, styled(title, false), b -> action.run(), DEFAULT_NARRATION);
      }
         @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hover = isHoveredOrFocused();
            RenderUtil.roundedRect(g, getX(), getY(), getWidth(), getHeight(), 4,
               !active ? 0xFF151E2C : hover ? 0xFF2A3B54 : 0xFF1B293D);
            RenderUtil.roundedOutline(g, getX(), getY(), getWidth(), getHeight(), 4, 0.75F,
               hover && active ? accent : 0x454D6688);
            var text = getMessage();
            var description = text.getStyle().getFont();
            var provider = SkijaGlyphProvider.find(HEADER.equals(description)
               ? "oyvey:gui/segoe_semibold.ttf" : "oyvey:hud/segoe.ttf", 10);
            var ink = provider == null ? null : provider.inkBounds(text.getString());
            var bounds = font.prepareText(text.getVisualOrderText(), 0, 0, -1, false, false, 0).bounds();
            float textWidth = ink == null ? bounds.width() : ink.getWidth();
            float textLeft = ink == null ? bounds.left() : ink.getLeft();
            float textHeight = ink == null ? bounds.height() : ink.getHeight();
            float textTop = ink == null ? bounds.top() : ink.getTop();
            g.pose().pushMatrix();
            g.pose().translate(getX() + (getWidth() - textWidth) / 2 - textLeft,
               getY() + (getHeight() - textHeight) / 2 - textTop);
            g.text(font, text, 0, 0, active ? TEXT : 0xFF63738A, false);
            g.pose().popMatrix();
         }
      }

   MenuField field(String hint, int x, int y, int w, int h, int max, boolean secret) {
      return addRenderableWidget(new MenuField(hint, x, y, w, h, max, secret));
   }

   final class MenuField extends EditBox {
      private final boolean secret;
      private final String label;

      MenuField(String hint, int x, int y, int w, int h, int max, boolean secret) {
         super(ClientMenuScreen.this.font, x, y, w, h, Component.literal(hint));
         this.secret = secret;
         this.label = hint;
         setMaxLength(max);
         setBordered(false);
         setTextShadow(false);
         setTextColor(TEXT);
         setHint(styled(hint, false));
         addFormatter((value, offset) -> FormattedCharSequence.forward(secret ? "•".repeat(value.length()) : value,
            Style.EMPTY.withFont(BODY)));
      }

      @Override public void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
         // Native EditBox still handles selection, cursor movement, paste and focus.
         RenderUtil.roundedRect(g, getX(), getY(), getWidth(), getHeight(), 4, 0xFF0B121E);
         RenderUtil.roundedOutline(g, getX(), getY(), getWidth(), getHeight(), 4, 0.8F,
            isFocused() ? accent : 0x55506885);
         // Borderless EditBox anchors text directly at getY(). Center the
         // custom font's cap/descender bounds inside our separately drawn box.
         var metrics = font.prepareText(styled("Ag", false).getVisualOrderText(), 0, 0, -1, false, false, 0).bounds();
         float textOffsetY = metrics == null ? (getHeight() - 9) / 2.0F
            : (getHeight() - metrics.height()) / 2.0F - metrics.top();
         g.enableScissor(getX() + 5, getY() + 2, getX() + getWidth() - 5, getY() + getHeight() - 2);
         if (secret) {
            int count = Math.min(getValue().length(), Math.max(1, (getWidth() - 16) / 5));
            String shown = getValue().isEmpty() ? label : "•".repeat(count);
            g.pose().pushMatrix();
            g.pose().translate(7, textOffsetY);
            text(g, shown, getX(), getY(), getValue().isEmpty() ? MUTED : TEXT, false);
            g.pose().popMatrix();
            if (isFocused() && System.currentTimeMillis() % 1000 < 500) {
               int caret = getX() + 7 + (getValue().isEmpty() ? 0 : font.width(styled(shown, false)));
               g.fill(caret, getY() + 6, caret + 1, getY() + getHeight() - 6, accent);
            }
         } else {
            g.pose().pushMatrix();
            g.pose().translate(7, textOffsetY);
            super.extractWidgetRenderState(g, mx - 7, my - Math.round(textOffsetY), delta);
            g.pose().popMatrix();
         }
         g.disableScissor();
      }

      @Override public int getInnerWidth() {
         return Math.max(1, super.getInnerWidth() - 14);
      }

      @Override protected net.minecraft.network.chat.MutableComponent createNarrationMessage() {
         return secret ? Component.literal(label + (getValue().isEmpty() ? ", empty" : ", hidden")) : super.createNarrationMessage();
      }

      @Override public boolean keyPressed(KeyEvent event) {
         // A masked credential must not be copied back to the system clipboard.
         if (secret && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0
            && (event.input() == GLFW.GLFW_KEY_C || event.input() == GLFW.GLFW_KEY_X)) return true;
         return super.keyPressed(event);
      }
   }

   static boolean hit(double mx, double my, int x, int y, int w, int h) {
      return mx >= x && mx < x + w && my >= y && my < y + h;
   }

   @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) { }
   @Override public boolean isPauseScreen() { return false; }
}
