package me.alpha432.oyvey.features.gui;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.cape.CapeTextureLoader;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** Cape management with the same native controls and palette as the main menus. */
public class CapeScreen extends ClientMenuScreen {
   private MenuField nameField, sourceField;
   private int listX, listY, listW, listH, rows, scroll, formX, formW, statusY;
   private String status = "";
   private boolean success, compact;
   private static final int ROW = 28;

   public CapeScreen() { super("Capes"); }

   @Override protected void init() {
      String name = nameField == null ? "" : nameField.getValue();
      String source = sourceField == null ? "" : sourceField.getValue();
      layout(620, 360);
      compact = pw < 500 || ph < 300;
      listX = px + 14; listY = py + 66;
      listW = compact ? pw - 28 : (pw - 42) / 2;
      listH = compact ? Math.max(28, ph - 188) : ph - 116;
      rows = Math.max(1, listH / ROW);
      formX = compact ? listX : listX + listW + 14;
      formW = compact ? listW : pw - (formX - px) - 14;
      int formY = compact ? listY + listH + 8 : py + 84;
      int fieldHeight = compact ? 18 : 26;
      nameField = field("Cape name", formX, formY, formW, fieldHeight, 64, false);
      sourceField = field("PNG URL or local file", formX, formY + (compact ? 24 : 58), formW, fieldHeight, 2048, false);
      nameField.setValue(name); sourceField.setValue(source);
      button("Add cape", formX, formY + (compact ? 48 : 100), formW, compact ? 20 : 28, this::addCape);
      statusY = compact ? formY + 72 : formY + 144;
      int footer = py + ph - 26 - (compact ? 0 : 10);
      button("Disable cape", px + 14, footer, Math.min(124, (pw - 38) / 2), 20, () -> {
         OyVey.capeManager.setActiveIndex(-1); OyVey.configManager.save(); message("Cape disabled.", true);
      });
      button("Back", px + pw - 94, footer, 80, 20, this::onClose);
      setInitialFocus(nameField);
   }

   @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
      var active = OyVey.capeManager.getActiveCape();
      background(g, "Capes", active == null ? "Choose a cape or add your own PNG." : "Active: " + active.name());
      var entries = OyVey.capeManager.getCapes();
      scroll = Mth.clamp(scroll, 0, Math.max(0, entries.size() - rows));
      RenderUtil.roundedRect(g, listX, listY, listW, listH, 5, 0xFF0C1624);
      if (entries.isEmpty()) text(g, "No saved capes", listX + 12, listY + 10, MUTED, false);
      for (int row = 0; row < rows && row + scroll < entries.size(); row++) {
         int index = row + scroll, y = listY + row * ROW;
         var entry = entries.get(index);
         boolean selected = index == OyVey.capeManager.getActiveIndex();
         boolean hover = hit(mx, my, listX, y, listW, ROW - 2);
         RenderUtil.roundedRect(g, listX + 1, y + 1, listW - 2, ROW - 3, 4,
            selected ? 0xFF24364D : hover ? 0xFF1B293C : 0xFF101C2B);
         if (selected) RenderUtil.roundedRect(g, listX + 2, y + 5, 2, ROW - 11, 1, accent);
         int dot = switch (CapeTextureLoader.statusOf(entry.source())) {
            case LOADED -> 0xFF88D9B0; case LOADING -> 0xFFE7CE89; case FAILED -> 0xFFFFAAA6; default -> MUTED;
         };
         RenderUtil.roundedRect(g, listX + 9, y + 11, 4, 4, 2, dot);
         text(g, fit(entry.name(), listW - 86), listX + 20, y + 8, selected ? TEXT : MUTED, false);
         text(g, selected ? "On" : "Use", listX + listW - 57, y + 8, selected ? accent : MUTED, false);
         text(g, "×", listX + listW - 18, y + 8, hit(mx, my, listX + listW - 25, y, 25, ROW) ? 0xFFFFAAA6 : MUTED, false);
      }
      if (entries.size() > rows) {
         int thumb = Math.max(12, listH * rows / entries.size());
         int y = listY + (listH - thumb) * scroll / Math.max(1, entries.size() - rows);
         RenderUtil.roundedRect(g, listX + listW - 3, y, 2, thumb, 1, accent);
      }
      if (!compact) {
         text(g, "Cape name", formX, nameField.getY() - 16, MUTED, false);
         text(g, "PNG URL or local file", formX, sourceField.getY() - 16, MUTED, false);
         text(g, "Select a row to enable its cape.", formX, py + ph - 80, MUTED, false);
      }
      if (!status.isEmpty()) {
         var lines = font.split(styled(status, false), formW);
         int maxLines = compact ? 1 : 2;
         for (int i = 0; i < Math.min(maxLines, lines.size()); i++)
            g.text(font, lines.get(i), formX, statusY + i * 13, success ? 0xFF88D9B0 : 0xFFFFAAA6, false);
      }
      super.extractRenderState(g, mx, my, delta);
   }

   @Override public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      if (click.button() == 0 && hit(click.x(), click.y(), listX, listY, listW, rows * ROW)) {
         int index = scroll + (int)(click.y() - listY) / ROW;
         var entries = OyVey.capeManager.getCapes();
         if (index >= entries.size()) return true;
         if (click.x() >= listX + listW - 25) { OyVey.capeManager.removeCape(index); message("Cape removed.", true); }
         else {
            boolean disable = OyVey.capeManager.getActiveIndex() == index;
            OyVey.capeManager.setActiveIndex(disable ? -1 : index);
            if (!disable) {
               String source = entries.get(index).source();
               if (CapeTextureLoader.statusOf(source) == CapeTextureLoader.LoadStatus.FAILED) CapeTextureLoader.invalidate(source);
               CapeTextureLoader.get(source);
            }
            message(disable ? "Cape disabled." : "Cape enabled.", true);
         }
         OyVey.configManager.save(); return true;
      }
      return super.mouseClicked(click, doubled);
   }

   @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
      if (hit(x, y, listX, listY, listW, listH)) {
         scroll = Mth.clamp(scroll - (int)Math.signum(dy), 0, Math.max(0, OyVey.capeManager.getCapes().size() - rows)); return true;
      }
      return super.mouseScrolled(x, y, dx, dy);
   }
   @Override public boolean keyPressed(KeyEvent key) {
      if (key.input() == GLFW.GLFW_KEY_ENTER || key.input() == GLFW.GLFW_KEY_KP_ENTER) {
         if (nameField.isFocused()) setFocused(sourceField); else addCape(); return true;
      }
      return super.keyPressed(key);
   }
   private void addCape() {
      String name = nameField.getValue().trim(), source = sourceField.getValue().trim();
      if (name.isEmpty() || source.isEmpty()) { message("Enter a name and a PNG URL or file path.", false); return; }
      OyVey.capeManager.addCape(name, source); OyVey.configManager.save(); CapeTextureLoader.get(source);
      nameField.setValue(""); sourceField.setValue("");
      scroll = Math.max(0, OyVey.capeManager.getCapes().size() - rows); message("Cape added. Select it to enable it.", true);
   }
   private void message(String value, boolean ok) { status = value; success = ok; }
   @Override public void onClose() { minecraft.setScreen(new CustomMainMenuScreen()); }
}