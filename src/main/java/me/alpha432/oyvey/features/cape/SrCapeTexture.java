package me.alpha432.oyvey.features.cape;

import com.mojang.blaze3d.platform.NativeImage;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.render.Cape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** The supplied artwork is bundled locally; no account or URL is needed. */
public final class SrCapeTexture {
   private static final Identifier ART = Identifier.fromNamespaceAndPath("oyvey", "textures/capes/sr_artwork.png");
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("oyvey", "sr_cape");
   private static boolean loaded, failed;

   private SrCapeTexture() { }

   public static Identifier get() {
      if (Cape.INSTANCE == null || !Cape.INSTANCE.isEnabled() || failed) return null;
      if (!loaded) {
         Minecraft mc = Minecraft.getInstance();
         NativeImage atlas = null;
         try (var input = mc.getResourceManager().open(ART); var artwork = NativeImage.read(input)) {
            // Native capes use a 64x32 UV atlas. Map the reference's left
            // 10x16 panel to BOTH visible faces, with black edges and seams.
            int scale = 16, width = 64 * scale, height = 32 * scale;
            atlas = new NativeImage(width, height, false);
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) atlas.setPixel(x, y, 0xFF000000);
            for (int y = 0; y < 16 * scale; y++) for (int x = 0; x < 10 * scale; x++) {
               int sourceX = (int)((1 + (x + .5) / scale) * artwork.getWidth() / 64.0);
               int sourceY = (int)((1 + (y + .5) / scale) * artwork.getHeight() / 32.0);
               int pixel = artwork.getPixel(sourceX, sourceY) | 0xFF000000;
               atlas.setPixel(scale + x, scale + y, pixel);
               atlas.setPixel(12 * scale + x, scale + y, pixel);
            }
            mc.getTextureManager().register(TEXTURE, new DynamicTexture(() -> "SR Client cape", atlas));
            loaded = true;
         } catch (Exception error) {
            if (atlas != null) atlas.close();
            failed = true;
            OyVey.LOGGER.error("Unable to load the bundled SR cape", error);
            return null;
         }
      }
      return TEXTURE;
   }
}
