package me.alpha432.oyvey.mixin.render;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import me.alpha432.oyvey.OyVey;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ShaderManager.class)
public class MixinShaderManager {
   private static final Identifier TEXT_VERTEX_SHADER = Identifier.fromNamespaceAndPath(
      "minecraft", "shaders/core/rendertype_text.vsh"
   );
   private static final String BROAD_PORTRAIT_MARKER = "if (Color.r == 1/255.) {";
   private static final String EXACT_PORTRAIT_MARKER = "if (Color.r == 1/255. && Color.g == 0.0 && Color.b == 0.0) {";

   @Redirect(
      method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/client/renderer/ShaderManager$Configs;",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/packs/resources/ResourceManager;listResources(Ljava/lang/String;Ljava/util/function/Predicate;)Ljava/util/Map;"
      )
   )
   private Map<Identifier, Resource> oyvey$fixBroadTextColorMarker(
      ResourceManager resourceManager, String path, Predicate<Identifier> filter
   ) {
      Map<Identifier, Resource> resources = new HashMap<>(resourceManager.listResources(path, filter));
      Resource textShader = resources.get(TEXT_VERTEX_SHADER);
      if (textShader == null) {
         return resources;
      }

      try (var input = textShader.open()) {
         String source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
         if (!source.contains(BROAD_PORTRAIT_MARKER)) {
            return resources;
         }

         // CubeCraft uses RGB 1,0,0 as a private marker for its portrait glyph.
         // Testing red alone also captures ordinary colors such as SRClient's
         // 1,150,210 accent, turning every glyph into oversized portrait geometry.
         byte[] fixedSource = source.replace(BROAD_PORTRAIT_MARKER, EXACT_PORTRAIT_MARKER).getBytes(StandardCharsets.UTF_8);
         resources.put(TEXT_VERTEX_SHADER, new Resource(textShader.source(), () -> new ByteArrayInputStream(fixedSource)));
         OyVey.LOGGER.info("Narrowed unsafe text color marker in resource-pack shader from {}", textShader.sourcePackId());
      } catch (IOException exception) {
         OyVey.LOGGER.warn("Could not inspect resource-pack text shader; leaving it unchanged", exception);
      }

      return resources;
   }
}
