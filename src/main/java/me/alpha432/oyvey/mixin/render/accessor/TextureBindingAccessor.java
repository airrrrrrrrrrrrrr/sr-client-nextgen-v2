package me.alpha432.oyvey.mixin.render.accessor;

import com.mojang.blaze3d.textures.GpuSampler;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.renderer.rendertype.RenderSetup$TextureBinding")
public interface TextureBindingAccessor {
   @Accessor("location")
   Identifier oyvey$getLocation();

   @Accessor("sampler")
   Supplier<GpuSampler> oyvey$getSampler();
}
