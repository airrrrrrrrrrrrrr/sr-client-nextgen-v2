package me.alpha432.oyvey.mixin.render.accessor;

import java.util.Map;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup.OutlineProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSetup.class)
public interface RenderSetupAccessor {
   @Accessor("textures")
   Map<String, Object> oyvey$getTextures();

   @Accessor("useLightmap")
   boolean oyvey$usesLightmap();

   @Accessor("useOverlay")
   boolean oyvey$usesOverlay();

   @Accessor("affectsCrumbling")
   boolean oyvey$affectsCrumbling();

   @Accessor("sortOnUpload")
   boolean oyvey$sortOnUpload();

   @Accessor("bufferSize")
   int oyvey$getBufferSize();

   @Accessor("layeringTransform")
   LayeringTransform oyvey$getLayeringTransform();

   @Accessor("outputTarget")
   OutputTarget oyvey$getOutputTarget();

   @Accessor("textureTransform")
   TextureTransform oyvey$getTextureTransform();

   @Accessor("outlineProperty")
   OutlineProperty oyvey$getOutlineProperty();
}
