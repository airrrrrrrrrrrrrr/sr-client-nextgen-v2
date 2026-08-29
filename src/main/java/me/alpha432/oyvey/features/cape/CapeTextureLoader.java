package me.alpha432.oyvey.features.cape;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CapeTextureLoader {
   private static final Logger LOGGER = LogManager.getLogger("CapeTextureLoader");
   private static final Map<String, Identifier> CACHE = new HashMap<>();
   private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
   private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();
   private static final Path CAPE_DIR = FabricLoader.getInstance().getGameDir().resolve("oyvey").resolve("capes");

   public static Identifier get(String source) {
      if (source != null && !source.isBlank()) {
         source = normalize(source);
         Identifier cached = CACHE.get(source);
         if (cached != null) {
            return cached;
         }

         if (FAILED.contains(source)) {
            return null;
         }

         if (LOADING.add(source)) {
            String finalSource = source;
            Thread t = new Thread(() -> loadAsync(finalSource), "oyvey-cape-loader");
            t.setDaemon(true);
            t.start();
         }

         return null;
      } else {
         return null;
      }
   }

   public static CapeTextureLoader.LoadStatus statusOf(String source) {
      source = normalize(source);
      if (CACHE.containsKey(source)) {
         return CapeTextureLoader.LoadStatus.LOADED;
      } else if (FAILED.contains(source)) {
         return CapeTextureLoader.LoadStatus.FAILED;
      } else {
         return LOADING.contains(source) ? CapeTextureLoader.LoadStatus.LOADING : CapeTextureLoader.LoadStatus.NONE;
      }
   }

   public static void invalidate(String source) {
      source = normalize(source);
      Identifier old = CACHE.remove(source);
      LOADING.remove(source);
      FAILED.remove(source);
      if (old != null) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null) {
            mc.submit(() -> {
               try {
                  mc.getTextureManager().release(old);
               } catch (Exception var3x) {
               }
            });
         }
      }
   }

   public static void invalidateAll() {
      new HashSet<>(CACHE.keySet()).forEach(CapeTextureLoader::invalidate);
   }

   private static void loadAsync(String source) {
      try {
         byte[] data = !source.startsWith("http://") && !source.startsWith("https://") ? loadLocalFile(source) : downloadUrl(source);
         if (data == null || data.length == 0) {
            LOGGER.error("[CapeTextureLoader] Cape not found or empty: {}", source);
            FAILED.add(source);
            LOADING.remove(source);
            return;
         }

         String fmt = detectFormat(data);
         if (!"PNG".equals(fmt) && !"JPEG".equals(fmt)) {
            LOGGER.error("[CapeTextureLoader] Unsupported image format ({}) for cape: {} \u2014 use a PNG file.", fmt, source);
            FAILED.add(source);
            LOADING.remove(source);
            return;
         }

         NativeImage image;
         try (InputStream is = new ByteArrayInputStream(data)) {
            image = NativeImage.read(is);
         }

         Identifier var101 = Identifier.fromNamespaceAndPath("oyvey", "capes/" + sanitize(source));
         Minecraft mc = Minecraft.getInstance();
         mc.submit(() -> {
            try {
               DynamicTexture texture = new DynamicTexture(() -> "oyvey_cape_" + sanitize(source), image);
               mc.getTextureManager().register(var101, texture);
               CACHE.put(source, var101);
               LOGGER.info("[CapeTextureLoader] Loaded cape: {} \u2192 {}", source, var101);
            } catch (Exception var8x) {
               LOGGER.error("[CapeTextureLoader] Failed to register texture for: {}", source, var8x);
               image.close();
               FAILED.add(source);
            } finally {
               LOADING.remove(source);
            }
         });
      } catch (Exception var9) {
         LOGGER.error("[CapeTextureLoader] Failed to load cape: {}", source, var9);
         FAILED.add(source);
         LOADING.remove(source);
      }
   }

   private static byte[] downloadUrl(String urlStr) throws IOException {
      URL url = URI.create(urlStr).toURL();
      HttpURLConnection conn = (HttpURLConnection)url.openConnection();
      conn.setConnectTimeout(10000);
      conn.setReadTimeout(15000);
      conn.setRequestProperty("User-Agent", "Mozilla/5.0 OyVey-Client/1.0");
      conn.setInstanceFollowRedirects(true);
      if (conn.getResponseCode() != 200) {
         return null;
      }

      try (InputStream is = conn.getInputStream()) {
         return is.readAllBytes();
      }
   }

   private static byte[] loadLocalFile(String name) throws IOException {
      if (!Files.exists(CAPE_DIR)) {
         Files.createDirectories(CAPE_DIR);
      }

      Path path = CAPE_DIR.resolve(name);
      return Files.exists(path) && Files.isRegularFile(path) ? Files.readAllBytes(path) : null;
   }

   private static String sanitize(String source) {
      String s = source.toLowerCase().replaceAll("[^a-z0-9._/-]", "_").replaceAll("_{2,}", "_");
      return s.substring(0, Math.min(s.length(), 60));
   }

   private static String detectFormat(byte[] data) {
      if (data.length < 4) {
         return "UNKNOWN";
      } else if (data[0] == -119 && data[1] == 80 && data[2] == 78 && data[3] == 71) {
         return "PNG";
      } else if (data[0] == -1 && data[1] == -40 && data[2] == -1) {
         return "JPEG";
      } else if (data.length >= 12
         && data[0] == 82
         && data[1] == 73
         && data[2] == 70
         && data[3] == 70
         && data[8] == 87
         && data[9] == 69
         && data[10] == 66
         && data[11] == 80) {
         return "WebP";
      } else {
         return data[0] == 71 && data[1] == 73 && data[2] == 70 && data[3] == 56 ? "GIF" : "UNKNOWN";
      }
   }

   private static String normalize(String source) {
      if (source == null) {
         return source;
      } else {
         return !source.startsWith("http://") && !source.startsWith("https://") && source.contains(".") && source.contains("/") ? "https://" + source : source;
      }
   }

   public enum LoadStatus {
      NONE,
      LOADING,
      LOADED,
      FAILED;
   }
}
