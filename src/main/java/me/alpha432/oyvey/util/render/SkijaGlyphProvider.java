package me.alpha432.oyvey.util.render;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import io.github.humbleui.skija.Bitmap;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontEdging;
import io.github.humbleui.skija.FontHinting;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.types.Rect;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import org.lwjgl.system.MemoryUtil;

/** Skija owns outlines, antialiasing and advances; Minecraft owns layout and the atlas. */
public final class SkijaGlyphProvider implements GlyphProvider {
   private record ResourceKey(String file, float size) { }
   private static final ConcurrentHashMap<ResourceKey, SkijaGlyphProvider> RESOURCES = new ConcurrentHashMap<>();
   private ResourceKey resourceKey;
   private final Typeface typeface;
   private final Font font;
   private final Paint paint;
   private final float oversample, shiftX, shiftY;
   private final Int2ShortOpenHashMap indices = new Int2ShortOpenHashMap();
   private final Int2ObjectOpenHashMap<UnbakedGlyph> glyphs = new Int2ObjectOpenHashMap<>();
   private final IntSet supported;
   private boolean closed;

   public SkijaGlyphProvider register(String file, float size) {
      this.resourceKey = new ResourceKey(file, size);
      RESOURCES.put(this.resourceKey, this);
      return this;
   }

   public static SkijaGlyphProvider find(String file, float size) { return RESOURCES.get(new ResourceKey(file, size)); }

   /** Exact nontransparent pixels in Minecraft text coordinates, excluding atlas padding. */
   public synchronized Rect inkBounds(String text) {
      this.ensureOpen();
      float pen = 0, left = Float.POSITIVE_INFINITY, top = left, right = Float.NEGATIVE_INFINITY, bottom = right;
      for (int cp : text.codePoints().toArray()) {
         UnbakedGlyph glyph = this.getGlyph(cp);
         if (glyph == null) return null; // Let Minecraft measure strings requiring a fallback font.
         if (glyph instanceof Glyph bitmap) {
            if (bitmap.ink == null) {
               byte[] pixels = this.rasterize(bitmap);
               int minX = bitmap.width, minY = bitmap.height, maxX = -1, maxY = -1;
               for (int y = 0; y < bitmap.height; y++) for (int x = 0; x < bitmap.width; x++) {
                  if ((pixels[y * bitmap.width + x] & 255) == 0) continue;
                  minX = Math.min(minX, x); minY = Math.min(minY, y);
                  maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
               }
               bitmap.ink = Rect.makeLTRB((bitmap.left + minX) / oversample, 7 + (bitmap.top + minY) / oversample,
                  (bitmap.left + maxX + 1) / oversample, 7 + (bitmap.top + maxY + 1) / oversample);
            }
            left = Math.min(left, pen + bitmap.ink.getLeft()); top = Math.min(top, bitmap.ink.getTop());
            right = Math.max(right, pen + bitmap.ink.getRight()); bottom = Math.max(bottom, bitmap.ink.getBottom());
         }
         pen += glyph.info().getAdvance();
      }
      return Float.isFinite(left) ? Rect.makeLTRB(left, top, right, bottom) : null;
   }

   public SkijaGlyphProvider(byte[] bytes, float size, float oversample, float shiftX, float shiftY, String skip) throws IOException {
      if (!Float.isFinite(size) || size <= 0 || !Float.isFinite(oversample) || oversample < 1 || oversample > 16)
         throw new IOException("Invalid Skija font size/oversample");
      this.oversample = oversample; this.shiftX = shiftX * oversample; this.shiftY = shiftY * oversample;
      try (Data data = Data.makeFromBytes(bytes)) { this.typeface = FontMgr.getDefault().makeFromData(data); }
      if (this.typeface == null) throw new IOException("Skija could not decode the TrueType font");
      this.font = new Font(this.typeface, size * oversample).setEdging(FontEdging.ANTI_ALIAS)
         .setHinting(FontHinting.NORMAL).setSubpixel(true).setMetricsLinear(true).setBaselineSnapped(false);
      this.paint = new Paint().setColor(0xFFFFFFFF).setAntiAlias(true);
      try {
         IntSet excluded = new IntOpenHashSet(skip.codePoints().toArray());
         int[] block = new int[4096];
         for (int start = 0; start <= Character.MAX_CODE_POINT; start += block.length) {
            for (int i = 0; i < block.length; i++) block[i] = start + i;
            short[] ids = this.typeface.getUTF32Glyphs(block);
            for (int i = 0; i < block.length; i++) {
               int codepoint = block[i];
               if (ids[i] != 0 && !(codepoint >= 0xD800 && codepoint <= 0xDFFF) && !excluded.contains(codepoint)) this.indices.put(codepoint, ids[i]);
            }
         }
         this.supported = IntSets.unmodifiable(new IntOpenHashSet(this.indices.keySet()));
      } catch (RuntimeException | Error failure) { this.paint.close(); this.font.close(); this.typeface.close(); throw failure; }
   }

   @Override public IntSet getSupportedGlyphs() { return this.supported; }

   @Override public synchronized UnbakedGlyph getGlyph(int codepoint) {
      this.ensureOpen();
      if (!this.indices.containsKey(codepoint)) return null; // Preserve the resource's Minecraft fallback.
      UnbakedGlyph cached = this.glyphs.get(codepoint);
      if (cached != null) return cached;
      short[] id = {this.indices.get(codepoint)};
      float advance = this.font.getWidths(id)[0] / this.oversample;
      Rect bounds = this.font.getBounds(id, this.paint)[0];
      UnbakedGlyph glyph;
      if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) glyph = new EmptyGlyph(advance);
      else {
         int left = (int)Math.floor(bounds.getLeft() + this.shiftX) - 1;
         int top = (int)Math.floor(bounds.getTop() + this.shiftY) - 1;
         int right = (int)Math.ceil(bounds.getRight() + this.shiftX) + 1;
         int bottom = (int)Math.ceil(bounds.getBottom() + this.shiftY) + 1;
         glyph = new Glyph(codepoint, advance, left, top, right - left, bottom - top);
      }
      this.glyphs.put(codepoint, glyph);
      return glyph;
   }

   /** The exact single-channel pixels uploaded by GlyphBitmap; also used by visual checks. */
   synchronized byte[] rasterize(Glyph glyph) {
      this.ensureOpen();
      ImageInfo info = ImageInfo.makeA8(glyph.width, glyph.height);
      try (Bitmap bitmap = new Bitmap()) {
         if (!bitmap.allocPixels(info)) throw new IllegalStateException("Could not allocate Skija glyph bitmap");
         try (Canvas canvas = new Canvas(bitmap)) {
            canvas.clear(0);
            canvas.drawString(new String(Character.toChars(glyph.codepoint)), -glyph.left + this.shiftX, -glyph.top + this.shiftY, this.font, this.paint);
         }
         byte[] alpha = bitmap.readPixels(info, glyph.width, 0, 0);
         if (alpha == null || alpha.length != glyph.width * glyph.height) throw new IllegalStateException("Could not read Skija glyph pixels");
         return alpha;
      }
   }

   private void ensureOpen() { if (this.closed) throw new IllegalStateException("Skija font provider is closed"); }
   @Override public synchronized void close() {
      if (this.closed) return;
      this.closed = true; this.glyphs.clear();
      if (this.resourceKey != null) RESOURCES.remove(this.resourceKey, this);
      this.paint.close(); this.font.close(); this.typeface.close();
   }

   final class Glyph implements UnbakedGlyph, GlyphBitmap {
      final int codepoint, left, top, width, height;
      private final GlyphInfo info;
      private Rect ink;
      Glyph(int codepoint, float advance, int left, int top, int width, int height) {
         this.codepoint = codepoint; this.info = GlyphInfo.simple(advance);
         this.left = left; this.top = top; this.width = width; this.height = height;
      }
      @Override public GlyphInfo info() { return this.info; }
      @Override public BakedGlyph bake(UnbakedGlyph.Stitcher stitcher) { return stitcher.stitch(this.info, this); }
      @Override public int getPixelWidth() { return this.width; }
      @Override public int getPixelHeight() { return this.height; }
      @Override public float getOversample() { return SkijaGlyphProvider.this.oversample; }
      @Override public float getBearingLeft() { return this.left / this.getOversample(); }
      @Override public float getBearingTop() { return -this.top / this.getOversample(); }
      @Override public boolean isColored() { return false; }
      @Override public void upload(int x, int y, GpuTexture texture) {
         byte[] alpha = SkijaGlyphProvider.this.rasterize(this);
         try (NativeImage image = new NativeImage(NativeImage.Format.LUMINANCE, this.width, this.height, false)) {
            MemoryUtil.memByteBuffer(image.getPointer(), alpha.length).put(alpha);
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, image, 0, 0, x, y, this.width, this.height, 0, 0);
         }
      }
   }
}
