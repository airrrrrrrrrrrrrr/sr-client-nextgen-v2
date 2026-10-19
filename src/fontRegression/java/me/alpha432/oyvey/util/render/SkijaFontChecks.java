package me.alpha432.oyvey.util.render;

import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Loads the packaged native library and renders the actual atlas glyph pixels. */
public final class SkijaFontChecks {
   private static int checks;
   public static void main(String[] args) throws Exception {
      String[] fonts = {"clickgui", "clickgui_header", "hud/arial", "hud/verdana", "hud/tahoma", "hud/consolas",
         "hud/segoe", "hud/comic", "hud/georgia", "hud/trebuchet", "hud/courier"};
      BufferedImage preview = new BufferedImage(1800, 100 + fonts.length * 72, BufferedImage.TYPE_INT_RGB);
      BufferedImage badges = new BufferedImage(1800, fonts.length * 84, BufferedImage.TYPE_INT_RGB);
      BufferedImage islands = new BufferedImage(2700, fonts.length * 84, BufferedImage.TYPE_INT_RGB);
      var islandBackground = islands.createGraphics();
      islandBackground.setColor(new java.awt.Color(0x343B48));
      islandBackground.fillRect(0, 0, islands.getWidth(), islands.getHeight());
      islandBackground.dispose();
      for (int y = 0; y < preview.getHeight(); y++) for (int x = 0; x < preview.getWidth(); x++) preview.setRGB(x, y, 0x15171E);
      int row = 40;
      int badgeRow = 0;
      for (String name : fonts) {
         var definition = JsonParser.parseString(new String(resource("/assets/oyvey/font/" + name + ".json"), StandardCharsets.UTF_8))
            .getAsJsonObject().getAsJsonArray("providers").get(0).getAsJsonObject();
         String file = definition.get("file").getAsString().substring("oyvey:".length());
         byte[] bytes = resource("/assets/oyvey/font/" + file);
         float size = definition.get("size").getAsFloat(), scale = definition.get("oversample").getAsFloat();
         SkijaGlyphProvider provider = new SkijaGlyphProvider(bytes, size, scale, 0, 0, "");
         try {
            require(provider.getSupportedGlyphs().contains('A') && provider.getSupportedGlyphs().contains('g'), name + " loads real font coverage");
            require(provider.getGlyph(0x10FFFF) == null && provider.getGlyph(0xD800) == null, name + " leaves missing characters to Minecraft's fallback");
            require(provider.getGlyph(' ').info().getAdvance() > 0, name + " keeps whitespace advance without a visible bitmap");
            String sample = name + "  SR | 3.0 | 144 FPS   Killaura Recode  gjpqy  [ ]  0123456789";
            double pen = 18;
            boolean antialiased = false;
            for (int codepoint : sample.codePoints().toArray()) {
               var glyph = provider.getGlyph(codepoint);
               require(glyph != null && Float.isFinite(glyph.info().getAdvance()) && glyph.info().getAdvance() >= 0, name + " has finite advances");
               require(glyph == provider.getGlyph(codepoint), name + " reuses cached glyph metrics");
               if (glyph instanceof SkijaGlyphProvider.Glyph bitmap) {
                  byte[] alpha = provider.rasterize(bitmap);
                  require(alpha.length == bitmap.width * bitmap.height, name + " has tightly packed atlas pixels");
                  boolean visible = false;
                  for (int y = 0; y < bitmap.height; y++) for (int x = 0; x < bitmap.width; x++) {
                     int coverage = alpha[y * bitmap.width + x] & 255;
                     visible |= coverage > 0; antialiased |= coverage > 0 && coverage < 255;
                     if (x == 0 || y == 0 || x == bitmap.width - 1 || y == bitmap.height - 1)
                        require(coverage == 0, name + " has padding around every rasterized glyph, including descenders/bearings");
                  }
                  // Resample the complete glyph quad, as the atlas does. A
                  // point per oversampled texel leaves artificial dotted gaps.
                  double originX = pen + bitmap.left / scale * 3, originY = row + (7 + bitmap.top / scale) * 3;
                  for (int py = (int)Math.floor(originY); py < Math.ceil(originY + bitmap.height / scale * 3); py++)
                  for (int px = (int)Math.floor(originX); px < Math.ceil(originX + bitmap.width / scale * 3); px++) {
                     if (px < 0 || px >= preview.getWidth() || py < 0 || py >= preview.getHeight()) continue;
                     double sx = (px + 0.5 - originX) * scale / 3 - 0.5, sy = (py + 0.5 - originY) * scale / 3 - 0.5;
                     int ix = (int)Math.floor(sx), iy = (int)Math.floor(sy);
                     double tx = sx - ix, ty = sy - iy;
                     int coverage = (int)Math.round((sample(alpha, bitmap.width, bitmap.height, ix, iy) * (1 - tx)
                        + sample(alpha, bitmap.width, bitmap.height, ix + 1, iy) * tx) * (1 - ty)
                        + (sample(alpha, bitmap.width, bitmap.height, ix, iy + 1) * (1 - tx)
                        + sample(alpha, bitmap.width, bitmap.height, ix + 1, iy + 1) * tx) * ty);
                     if (coverage > 0) preview.setRGB(px, py, blend(preview.getRGB(px, py), name.startsWith("hud") ? 0xD6E6EF : 0x5CCBEC, coverage));
                  }
                  require(visible, name + " rasterizes visible native Skija pixels");
                  require(bitmap.getPixelHeight() / scale < size * 2 + 4, name + " keeps glyphs within sane layout bounds");
               }
               pen += glyph.info().getAdvance() * 3;
            }
            String[] islandLabels = {"SR | LCGaming | 405fps | 128ms", "SR | gyp_user_1234567 | 61fps | 0ms", "SR | LCGaming | 1000fps | 28ms"};
            for (int i = 0; i < islandLabels.length; i++) {
               String label = islandLabels[i];
               var ink = provider.inkBounds(label);
               var letters = provider.inkBounds(label.replace("|", ""));
               int x = 20 + i * 900, y = badgeRow * 84 + 12;
               int w = (int)Math.ceil(ink.getWidth() + 24) * 3;
               var graphics = islands.createGraphics();
               graphics.setColor(new java.awt.Color(0x1B1C23)); graphics.fillRoundRect(x, y, w, 60, 60, 60);
               graphics.setColor(new java.awt.Color(0x32343F)); graphics.drawRoundRect(x, y, w, 60, 60, 60); graphics.dispose();
               double tx = x + (w - ink.getWidth() * 3) / 2 - ink.getLeft() * 3;
               double ty = y + (60 - letters.getHeight() * 3) / 2 - letters.getTop() * 3;
               drawInk(islands, provider, label, tx, ty, true, i == 2);
            }
            require(antialiased, name + " uses partial alpha antialiasing");
            require(provider.getGlyph('W').info().getAdvance() >= provider.getGlyph('i').info().getAdvance(), name + " measures the selected font's actual widths");
            for (int i = 0; i < 3; i++) {
               String label = "SR | 3.0 | " + new int[]{61, 164, 1000}[i] + " FPS";
               var ink = provider.inkBounds(label);
               var caps = provider.inkBounds(label.replace("|", ""));
               require(ink != null && caps != null && ink.getWidth() > 0 && caps.getHeight() > 0,
                  name + " measures real glyph pixels instead of padded atlas quads");
               int x = 20 + i * 590, y = badgeRow * 84 + 12;
               int w = (int)Math.ceil(ink.getWidth() + 16) * 3;
               var graphics = badges.createGraphics();
               graphics.setColor(new java.awt.Color(0x494949)); graphics.fillRoundRect(x, y, w, 60, 36, 36);
               graphics.setColor(new java.awt.Color(0x858585)); graphics.drawRoundRect(x, y, w, 60, 36, 36); graphics.dispose();
               double tx = x + (w - ink.getWidth() * 3) / 2 - ink.getLeft() * 3;
               double ty = y + (60 - caps.getHeight() * 3) / 2 - caps.getTop() * 3;
               drawInk(badges, provider, label, tx, ty);
               require(Math.abs((ty + caps.getTop() * 3 - y) - (y + 60 - ty - caps.getBottom() * 3)) < 0.001,
                  name + " visibly centers the letters above and below at every FPS width");
            }
            badgeRow++;
            row += 72;
         } finally { provider.close(); provider.close(); }
         boolean rejected = false;
         try { provider.getGlyph('A'); } catch (IllegalStateException expected) { rejected = true; }
         require(rejected, name + " does not reuse closed native resources after a reload");
      }
      Path output = Path.of(System.getProperty("font.preview.path"));
      Files.createDirectories(output.getParent()); ImageIO.write(preview, "png", output.toFile());
      ImageIO.write(badges, "png", output.resolveSibling("watermark-alignment.png").toFile());
      ImageIO.write(islands, "png", output.resolveSibling("dynamic-island-fonts.png").toFile());
      System.out.println("Skija font checks passed: " + checks + "; preview: " + output);
   }
   private static void drawInk(BufferedImage image, SkijaGlyphProvider provider, String text, double x, double y) {
      drawInk(image, provider, text, x, y, false, false);
   }
   private static void drawInk(BufferedImage image, SkijaGlyphProvider provider, String text, double x, double y,
      boolean island, boolean rainbow) {
      double pen = x;
      int field = 0, character = 0;
      for (int cp : text.codePoints().toArray()) {
         if (cp == '|') field++;
         int color = cp == '|' ? (island ? 0x686B78 : 0x969696)
            : rainbow ? java.awt.Color.HSBtoRGB(1.0F / 6 + character * .025F, .82F, 1) & 0xFFFFFF
            : field == 0 ? 0x69CDFF : island ? 0xD7D8E2 : 0xEAEAEA;
         var glyph = provider.getGlyph(cp);
         if (glyph instanceof SkijaGlyphProvider.Glyph bitmap) {
            byte[] alpha = provider.rasterize(bitmap);
            double scale = bitmap.getOversample();
            double ox = pen + bitmap.left / scale * 3, oy = y + (7 + bitmap.top / scale) * 3;
            for (int py = (int)Math.floor(oy); py < Math.ceil(oy + bitmap.height / scale * 3); py++)
            for (int px = (int)Math.floor(ox); px < Math.ceil(ox + bitmap.width / scale * 3); px++) {
               if (px < 0 || py < 0 || px >= image.getWidth() || py >= image.getHeight()) continue;
               double sx = (px + .5 - ox) * scale / 3 - .5, sy = (py + .5 - oy) * scale / 3 - .5;
               int ix = (int)Math.floor(sx), iy = (int)Math.floor(sy);
               double dx = sx - ix, dy = sy - iy;
               int coverage = (int)Math.round((sample(alpha, bitmap.width, bitmap.height, ix, iy) * (1 - dx)
                  + sample(alpha, bitmap.width, bitmap.height, ix + 1, iy) * dx) * (1 - dy)
                  + (sample(alpha, bitmap.width, bitmap.height, ix, iy + 1) * (1 - dx)
                  + sample(alpha, bitmap.width, bitmap.height, ix + 1, iy + 1) * dx) * dy);
               image.setRGB(px, py, blend(image.getRGB(px, py), color, coverage));
            }
         }
         pen += glyph.info().getAdvance() * 3;
         character++;
      }
   }
   private static byte[] resource(String path) throws Exception {
      try (InputStream input = SkijaFontChecks.class.getResourceAsStream(path)) {
         if (input == null) throw new IllegalStateException("Missing bundled font " + path);
         return input.readAllBytes();
      }
   }
   private static int blend(int background, int foreground, int alpha) {
      int result = 0;
      for (int shift : new int[]{0, 8, 16}) result |= ((((foreground >> shift) & 255) * alpha + ((background >> shift) & 255) * (255 - alpha)) / 255) << shift;
      return result;
   }
   private static int sample(byte[] pixels, int width, int height, int x, int y) {
      return x < 0 || y < 0 || x >= width || y >= height ? 0 : pixels[y * width + x] & 255;
   }
   private static void require(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
}
