package me.alpha432.oyvey.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import me.alpha432.oyvey.mixin.render.accessor.RenderSetupAccessor;
import me.alpha432.oyvey.mixin.render.accessor.RenderTypeAccessor;
import me.alpha432.oyvey.mixin.render.accessor.TextureBindingAccessor;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup.RenderSetupBuilder;
import net.minecraft.resources.Identifier;

public class Layers {
   private static final RenderType GLOBAL_QUADS = RenderType.create("global_fill", RenderSetup.builder(Pipelines.GLOBAL_QUADS_PIPELINE).createRenderSetup());
   private static final RenderType GLOBAL_LINES = RenderType.create("global_lines", RenderSetup.builder(Pipelines.GLOBAL_LINES_PIPELINE).createRenderSetup());
   private static final RenderType TARGET_GLOW_QUADS = RenderType.create("target_glow_quads", RenderSetup.builder(Pipelines.TARGET_GLOW_QUADS_PIPELINE).createRenderSetup());
   private static final Map<Identifier, RenderType> CHAMS_TYPES = new HashMap<>();
   private static final Map<String, RenderType> CAPE_CUSTOM_TYPES = new ConcurrentHashMap<>();
   private static final Map<RenderType, RenderType> CHAMS_DEPTH_BIAS_TYPES = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<RenderType, RenderType> CHAMS_THROUGH_WALL_TYPES = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<RenderType, RenderType> HIDDEN_PLAYER_DEPTH_TYPES = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<RenderType, RenderType> HIDDEN_PLAYER_COLOR_TYPES = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<RenderType, RenderType> HIDDEN_PLAYER_CLASSIC_CAPE_COLOR_TYPES = Collections.synchronizedMap(new WeakHashMap<>());

   public static RenderType quads() {
      return GLOBAL_QUADS;
   }

   public static RenderType lines() {
      return GLOBAL_LINES;
   }

   public static RenderType targetGlowQuads() { return TARGET_GLOW_QUADS; }

   public static RenderType capeCustomTexture(RenderType originalType, Identifier customTexture) {
      String key = originalType + ":" + customTexture;
      return CAPE_CUSTOM_TYPES.computeIfAbsent(key, k -> {
         RenderSetup originalSetup = ((RenderTypeAccessor)(Object)originalType).oyvey$getState();
         RenderSetupAccessor accessor = (RenderSetupAccessor)(Object)originalSetup;
         RenderSetupBuilder builder = RenderSetup.builder(originalType.pipeline());

         for (Entry<String, Object> entry : accessor.oyvey$getTextures().entrySet()) {
            TextureBindingAccessor binding = (TextureBindingAccessor)entry.getValue();
            Identifier tex = "Sampler0".equals(entry.getKey()) ? customTexture : binding.oyvey$getLocation();
            builder.withTexture(entry.getKey(), tex, binding.oyvey$getSampler());
         }

         if (accessor.oyvey$usesLightmap()) {
            builder.useLightmap();
         }

         if (accessor.oyvey$usesOverlay()) {
            builder.useOverlay();
         }

         if (accessor.oyvey$affectsCrumbling()) {
            builder.affectsCrumbling();
         }

         if (accessor.oyvey$sortOnUpload()) {
            builder.sortOnUpload();
         }

         builder.bufferSize(accessor.oyvey$getBufferSize());
         builder.setLayeringTransform(accessor.oyvey$getLayeringTransform());
         builder.setOutputTarget(accessor.oyvey$getOutputTarget());
         builder.setTextureTransform(accessor.oyvey$getTextureTransform());
         builder.setOutline(accessor.oyvey$getOutlineProperty());
         return RenderType.create("oyvey_cape_" + k, builder.createRenderSetup());
      });
   }

   public static RenderType chamsEntity(Identifier skinTexture) {
      return CHAMS_TYPES.computeIfAbsent(
         skinTexture,
         loc -> RenderType.create(
            "chams_entity_" + loc,
            RenderSetup.builder(Pipelines.CHAMS_ENTITY_PIPELINE).withTexture("Sampler0", loc).useLightmap().useOverlay().createRenderSetup()
         )
      );
   }

   public static RenderType chamsThroughWall(RenderType originalType) {
      return CHAMS_THROUGH_WALL_TYPES.computeIfAbsent(originalType, Layers::createChamsThroughWallType);
   }

   public static RenderType chamsDepthBias(RenderType originalType) {
      return CHAMS_DEPTH_BIAS_TYPES.computeIfAbsent(originalType, Layers::createChamsDepthBiasType);
   }

   public static RenderType chamsThroughWallClassicCape(RenderType originalType) {
      return chamsThroughWall(originalType);
   }

   public static RenderType hiddenPlayerDepth(RenderType originalType) {
      return HIDDEN_PLAYER_DEPTH_TYPES.computeIfAbsent(originalType, Layers::createHiddenPlayerDepthType);
   }

   public static RenderType hiddenPlayerColor(RenderType originalType) {
      return HIDDEN_PLAYER_COLOR_TYPES.computeIfAbsent(originalType, Layers::createHiddenPlayerColorType);
   }

   public static RenderType hiddenPlayerColorClassicCape(RenderType originalType) {
      return HIDDEN_PLAYER_CLASSIC_CAPE_COLOR_TYPES.computeIfAbsent(originalType, Layers::createHiddenPlayerColorClassicCapeType);
   }

   public static RenderType hiddenCompositeDepth(RenderType originalType) {
      return hiddenPlayerDepth(originalType);
   }

   public static RenderType hiddenCompositeColor(RenderType originalType) {
      return hiddenPlayerColor(originalType);
   }

   public static RenderType classicCapeHiddenCompositeColor(RenderType originalType) {
      return hiddenPlayerColorClassicCape(originalType);
   }

   private static RenderType createChamsThroughWallType(RenderType originalType) {
      return createChamsThroughWallType(originalType, Pipelines.chamsThroughWall(originalType.pipeline()), "chams_through_wall_");
   }

   private static RenderType createChamsDepthBiasType(RenderType originalType) {
      return createChamsThroughWallType(originalType, Pipelines.chamsDepthBias(originalType.pipeline()), "chams_depth_bias_");
   }

   private static RenderType createHiddenPlayerDepthType(RenderType originalType) {
      return createChamsThroughWallType(
         originalType, Pipelines.hiddenPlayerDepth(originalType.pipeline()), "chams_hidden_player_depth_", ChamsCompositeTarget.OUTPUT_TARGET
      );
   }

   private static RenderType createHiddenPlayerColorType(RenderType originalType) {
      return createChamsThroughWallType(
         originalType, Pipelines.hiddenPlayerColor(originalType.pipeline()), "chams_hidden_player_color_", ChamsCompositeTarget.OUTPUT_TARGET
      );
   }

   private static RenderType createHiddenPlayerColorClassicCapeType(RenderType originalType) {
      return createChamsThroughWallType(
         originalType,
         Pipelines.hiddenPlayerColorClassicCape(originalType.pipeline()),
         "chams_hidden_player_classic_cape_color_",
         ChamsCompositeTarget.OUTPUT_TARGET
      );
   }

   private static RenderType createChamsThroughWallType(RenderType sourceType, RenderPipeline pipeline, String namePrefix) {
      return createChamsThroughWallType(sourceType, pipeline, namePrefix, null);
   }

   private static RenderType createChamsThroughWallType(RenderType sourceType, RenderPipeline pipeline, String namePrefix, OutputTarget outputTargetOverride) {
      RenderSetup originalSetup = ((RenderTypeAccessor)(Object)sourceType).oyvey$getState();
      RenderSetupAccessor accessor = (RenderSetupAccessor)(Object)originalSetup;
      RenderSetupBuilder builder = RenderSetup.builder(pipeline);

      for (Entry<String, Object> entry : accessor.oyvey$getTextures().entrySet()) {
         TextureBindingAccessor textureBinding = (TextureBindingAccessor)entry.getValue();
         Identifier texture = textureBinding.oyvey$getLocation();
         Supplier<GpuSampler> sampler = textureBinding.oyvey$getSampler();
         builder.withTexture(entry.getKey(), texture, sampler);
      }

      if (accessor.oyvey$usesLightmap()) {
         builder.useLightmap();
      }

      if (accessor.oyvey$usesOverlay()) {
         builder.useOverlay();
      }

      if (accessor.oyvey$affectsCrumbling()) {
         builder.affectsCrumbling();
      }

      if (accessor.oyvey$sortOnUpload()) {
         builder.sortOnUpload();
      }

      builder.bufferSize(accessor.oyvey$getBufferSize());
      builder.setLayeringTransform(accessor.oyvey$getLayeringTransform());
      builder.setOutputTarget(outputTargetOverride != null ? outputTargetOverride : accessor.oyvey$getOutputTarget());
      builder.setTextureTransform(accessor.oyvey$getTextureTransform());
      builder.setOutline(accessor.oyvey$getOutlineProperty());
      return RenderType.create(namePrefix + Integer.toHexString(System.identityHashCode(sourceType)), builder.createRenderSetup());
   }
}
