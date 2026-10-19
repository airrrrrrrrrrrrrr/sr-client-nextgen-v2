package me.alpha432.oyvey.mixin.render;

import com.mojang.blaze3d.font.GlyphProvider;
import java.io.IOException;
import java.io.InputStream;
import me.alpha432.oyvey.util.render.SkijaGlyphProvider;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrueTypeGlyphProviderDefinition.class)
public class MixinSkijaFontProvider {
   @Inject(method = "load", at = @At("HEAD"), cancellable = true)
   private void oyvey$loadSkijaFont(ResourceManager resources, CallbackInfoReturnable<GlyphProvider> cir) throws IOException {
      var definition = (TrueTypeGlyphProviderDefinition)(Object)this;
      if (!definition.location().getNamespace().equals("oyvey")) return;
      try (InputStream input = resources.open(definition.location().withPrefix("font/"))) {
         cir.setReturnValue(new SkijaGlyphProvider(input.readAllBytes(), definition.size(), definition.oversample(),
            definition.shift().x(), definition.shift().y(), definition.skip()).register(definition.location().toString(), definition.size()));
      } catch (LinkageError | RuntimeException failure) {
         throw new IOException("Failed to load Skija font " + definition.location(), failure);
      }
   }
}
