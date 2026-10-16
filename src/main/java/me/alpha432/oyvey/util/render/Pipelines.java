package me.alpha432.oyvey.util.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.pipeline.RenderPipeline.UniformDescription;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;

public class Pipelines {
   private static final Map<RenderPipeline, RenderPipeline> CHAMS_THROUGH_WALL_PIPELINES = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<RenderPipeline, RenderPipeline> CHAMS_DEPTH_BIAS_PIPELINES = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<RenderPipeline, RenderPipeline> HIDDEN_PLAYER_DEPTH_PIPELINES = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<RenderPipeline, RenderPipeline> HIDDEN_PLAYER_COLOR_PIPELINES = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<RenderPipeline, RenderPipeline> HIDDEN_PLAYER_CLASSIC_CAPE_COLOR_PIPELINES = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Identifier ROUNDED_RECT_SHADER = Identifier.fromNamespaceAndPath("oyvey", "core/rounded_rect");
   private static final Identifier ROUNDED_OUTLINE_SHADER = Identifier.fromNamespaceAndPath("oyvey", "core/rounded_outline");
   public static final RenderPipeline ROUNDED_RECT_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withLocation(Identifier.fromNamespaceAndPath("oyvey", "pipeline/rounded_rect"))
      .withVertexShader(ROUNDED_RECT_SHADER)
      .withFragmentShader(ROUNDED_RECT_SHADER)
      .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, Mode.QUADS)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withCull(false)
      .build();
   public static final RenderPipeline ROUNDED_OUTLINE_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withLocation(Identifier.fromNamespaceAndPath("oyvey", "pipeline/rounded_outline"))
      .withVertexShader(ROUNDED_OUTLINE_SHADER)
      .withFragmentShader(ROUNDED_OUTLINE_SHADER)
      .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, Mode.QUADS)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withCull(false)
      .build();
   static final RenderPipeline GLOBAL_QUADS_PIPELINE = RenderPipeline.builder(new Snippet[]{RenderPipelines.DEBUG_FILLED_SNIPPET})
      .withLocation("pipeline/global_fill_pipeline")
      .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, Mode.QUADS)
      .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withCull(false)
      .build();
   static final RenderPipeline GLOBAL_LINES_PIPELINE = RenderPipeline.builder(new Snippet[]{RenderPipelines.LINES_SNIPPET})
      .withLocation("pipeline/global_lines_pipeline")
      .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_LINE_WIDTH, Mode.LINES)
      .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withCull(false)
      .build();
   static final RenderPipeline CHAMS_ENTITY_PIPELINE = RenderPipeline.builder(new Snippet[]{RenderPipelines.ENTITY_SNIPPET})
      .withLocation("pipeline/chams_entity_pipeline")
      .withVertexFormat(DefaultVertexFormat.ENTITY, Mode.QUADS)
      .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withCull(false)
      .withSampler("Sampler1")
      .build();

   static RenderPipeline chamsThroughWall(RenderPipeline originalPipeline) {
      return CHAMS_THROUGH_WALL_PIPELINES.computeIfAbsent(originalPipeline, Pipelines::createThroughWallPipeline);
   }

   static RenderPipeline chamsDepthBias(RenderPipeline originalPipeline) {
      return CHAMS_DEPTH_BIAS_PIPELINES.computeIfAbsent(originalPipeline, Pipelines::createDepthBiasPipeline);
   }

   static RenderPipeline hiddenPlayerDepth(RenderPipeline originalPipeline) {
      return HIDDEN_PLAYER_DEPTH_PIPELINES.computeIfAbsent(originalPipeline, Pipelines::createHiddenPlayerDepthPipeline);
   }

   static RenderPipeline hiddenPlayerColor(RenderPipeline originalPipeline) {
      return HIDDEN_PLAYER_COLOR_PIPELINES.computeIfAbsent(originalPipeline, pipeline -> createHiddenPlayerColorPipeline(pipeline, false));
   }

   static RenderPipeline hiddenPlayerColorClassicCape(RenderPipeline originalPipeline) {
      return HIDDEN_PLAYER_CLASSIC_CAPE_COLOR_PIPELINES.computeIfAbsent(originalPipeline, pipeline -> createHiddenPlayerColorPipeline(pipeline, true));
   }

   private static RenderPipeline createThroughWallPipeline(RenderPipeline originalPipeline) {
      return createThroughWallPipeline(originalPipeline, false, false, false);
   }

   private static RenderPipeline createDepthBiasPipeline(RenderPipeline originalPipeline) {
      DepthStencilState originalDepth = originalPipeline.getDepthStencilState();
      Builder builder = RenderPipeline.builder(new Snippet[0])
         .withLocation("pipeline/chams_depth_bias_" + Integer.toHexString(System.identityHashCode(originalPipeline)))
         .withVertexShader(originalPipeline.getVertexShader())
         .withFragmentShader(originalPipeline.getFragmentShader())
         .withVertexFormat(originalPipeline.getVertexFormat(), originalPipeline.getVertexFormatMode())
         .withPolygonMode(originalPipeline.getPolygonMode())
         .withCull(originalPipeline.isCull())
         .withColorTargetState(originalPipeline.getColorTargetState())
         // Do not move model depth toward the camera. A very large negative polygon
         // offset breaks depth precision and can expose clipped triangles as giant
         // black polygons with server resource-pack models. Chams only needs to
         // bypass the depth test; it must not alter or write scene depth.
         .withDepthStencilState(
            new DepthStencilState(
               CompareOp.ALWAYS_PASS, false, originalDepth.depthBiasScaleFactor(), originalDepth.depthBiasConstant()
            )
         );
      ShaderDefines shaderDefines = originalPipeline.getShaderDefines();
      shaderDefines.flags().forEach(builder::withShaderDefine);
      shaderDefines.values().forEach((name, value) -> {
         if (value.indexOf(46) >= 0) {
            builder.withShaderDefine(name, Float.parseFloat(value));
         } else {
            builder.withShaderDefine(name, Integer.parseInt(value));
         }
      });
      originalPipeline.getSamplers().forEach(builder::withSampler);

      for (UniformDescription uniform : originalPipeline.getUniforms()) {
         if (uniform.type() != null && uniform.textureFormat() != null) {
            builder.withUniform(uniform.name(), uniform.type(), uniform.textureFormat());
         } else {
            if (uniform.type() == null) {
               throw new IllegalStateException("Unsupported uniform description without uniform type: " + uniform.name());
            }

            builder.withUniform(uniform.name(), uniform.type());
         }
      }

      return builder.build();
   }

   private static RenderPipeline createThroughWallPipeline(
      RenderPipeline originalPipeline, boolean disableCardinalLighting, boolean forceNoCull, boolean forceDepthWrite
   ) {
      DepthStencilState originalDepth = originalPipeline.getDepthStencilState();
      Builder builder = RenderPipeline.builder(new Snippet[0])
         .withLocation("pipeline/chams_through_wall_" + Integer.toHexString(System.identityHashCode(originalPipeline)))
         .withVertexShader(originalPipeline.getVertexShader())
         .withFragmentShader(originalPipeline.getFragmentShader())
         .withVertexFormat(originalPipeline.getVertexFormat(), originalPipeline.getVertexFormatMode())
         .withPolygonMode(originalPipeline.getPolygonMode())
         .withCull(forceNoCull ? false : originalPipeline.isCull())
         .withColorTargetState(originalPipeline.getColorTargetState())
         .withDepthStencilState(
            new DepthStencilState(
               CompareOp.GREATER_THAN, forceDepthWrite || originalDepth.writeDepth(), originalDepth.depthBiasScaleFactor(), originalDepth.depthBiasConstant()
            )
         );
      ShaderDefines shaderDefines = originalPipeline.getShaderDefines();
      shaderDefines.flags().forEach(flag -> {
         if (!disableCardinalLighting || !flag.equals("PER_FACE_LIGHTING")) {
            builder.withShaderDefine(flag);
         }
      });
      shaderDefines.values().forEach((name, value) -> {
         if (value.indexOf(46) >= 0) {
            builder.withShaderDefine(name, Float.parseFloat(value));
         } else {
            builder.withShaderDefine(name, Integer.parseInt(value));
         }
      });
      if (disableCardinalLighting) {
         builder.withShaderDefine("NO_CARDINAL_LIGHTING");
      }

      originalPipeline.getSamplers().forEach(builder::withSampler);

      for (UniformDescription uniform : originalPipeline.getUniforms()) {
         if (uniform.type() != null && uniform.textureFormat() != null) {
            builder.withUniform(uniform.name(), uniform.type(), uniform.textureFormat());
         } else {
            if (uniform.type() == null) {
               throw new IllegalStateException("Unsupported uniform description without uniform type: " + uniform.name());
            }

            builder.withUniform(uniform.name(), uniform.type());
         }
      }

      return builder.build();
   }

   private static RenderPipeline createHiddenPlayerDepthPipeline(RenderPipeline originalPipeline) {
      DepthStencilState originalDepth = originalPipeline.getDepthStencilState();
      Builder builder = RenderPipeline.builder(new Snippet[0])
         .withLocation("pipeline/chams_hidden_player_depth_" + Integer.toHexString(System.identityHashCode(originalPipeline)))
         .withVertexShader(originalPipeline.getVertexShader())
         .withFragmentShader(originalPipeline.getFragmentShader())
         .withVertexFormat(originalPipeline.getVertexFormat(), originalPipeline.getVertexFormatMode())
         .withPolygonMode(originalPipeline.getPolygonMode())
         .withCull(originalPipeline.isCull())
         .withColorTargetState(new ColorTargetState(originalPipeline.getColorTargetState().blendFunction(), 0))
         .withDepthStencilState(
            new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, originalDepth.depthBiasScaleFactor(), originalDepth.depthBiasConstant())
         );
      ShaderDefines shaderDefines = originalPipeline.getShaderDefines();
      shaderDefines.flags().forEach(builder::withShaderDefine);
      shaderDefines.values().forEach((name, value) -> {
         if (value.indexOf(46) >= 0) {
            builder.withShaderDefine(name, Float.parseFloat(value));
         } else {
            builder.withShaderDefine(name, Integer.parseInt(value));
         }
      });
      originalPipeline.getSamplers().forEach(builder::withSampler);

      for (UniformDescription uniform : originalPipeline.getUniforms()) {
         if (uniform.type() != null && uniform.textureFormat() != null) {
            builder.withUniform(uniform.name(), uniform.type(), uniform.textureFormat());
         } else {
            if (uniform.type() == null) {
               throw new IllegalStateException("Unsupported uniform description without uniform type: " + uniform.name());
            }

            builder.withUniform(uniform.name(), uniform.type());
         }
      }

      return builder.build();
   }

   private static RenderPipeline createHiddenPlayerColorPipeline(RenderPipeline originalPipeline, boolean disableCardinalLighting) {
      DepthStencilState originalDepth = originalPipeline.getDepthStencilState();
      Builder builder = RenderPipeline.builder(new Snippet[0])
         .withLocation(
            (disableCardinalLighting ? "pipeline/chams_hidden_player_classic_cape_color_" : "pipeline/chams_hidden_player_color_")
               + Integer.toHexString(System.identityHashCode(originalPipeline))
         )
         .withVertexShader(originalPipeline.getVertexShader())
         .withFragmentShader(originalPipeline.getFragmentShader())
         .withVertexFormat(originalPipeline.getVertexFormat(), originalPipeline.getVertexFormatMode())
         .withPolygonMode(originalPipeline.getPolygonMode())
         .withCull(originalPipeline.isCull())
         .withColorTargetState(originalPipeline.getColorTargetState())
         .withDepthStencilState(new DepthStencilState(CompareOp.EQUAL, false, originalDepth.depthBiasScaleFactor(), originalDepth.depthBiasConstant()));
      ShaderDefines shaderDefines = originalPipeline.getShaderDefines();
      shaderDefines.flags().forEach(flag -> {
         if (!disableCardinalLighting || !flag.equals("PER_FACE_LIGHTING")) {
            builder.withShaderDefine(flag);
         }
      });
      shaderDefines.values().forEach((name, value) -> {
         if (value.indexOf(46) >= 0) {
            builder.withShaderDefine(name, Float.parseFloat(value));
         } else {
            builder.withShaderDefine(name, Integer.parseInt(value));
         }
      });
      if (disableCardinalLighting) {
         builder.withShaderDefine("NO_CARDINAL_LIGHTING");
      }

      originalPipeline.getSamplers().forEach(builder::withSampler);

      for (UniformDescription uniform : originalPipeline.getUniforms()) {
         if (uniform.type() != null && uniform.textureFormat() != null) {
            builder.withUniform(uniform.name(), uniform.type(), uniform.textureFormat());
         } else {
            if (uniform.type() == null) {
               throw new IllegalStateException("Unsupported uniform description without uniform type: " + uniform.name());
            }

            builder.withUniform(uniform.name(), uniform.type());
         }
      }

      return builder.build();
   }
}
