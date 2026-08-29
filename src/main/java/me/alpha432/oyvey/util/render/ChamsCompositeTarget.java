package me.alpha432.oyvey.util.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.OutputTarget;

public final class ChamsCompositeTarget {
   static final OutputTarget OUTPUT_TARGET = new OutputTarget("oyvey_chams_hidden", ChamsCompositeTarget::getTarget);
   private static final RenderPipeline COMPOSITE_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation("pipeline/oyvey_chams_composite")
      .withVertexShader("core/screenquad")
      .withFragmentShader("core/blit_screen")
      .withSampler("InSampler")
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
      .withVertexFormat(DefaultVertexFormat.EMPTY, Mode.TRIANGLES)
      .build();
   private static TextureTarget target;
   private static boolean frameActive;
   private static boolean hasHiddenContent;

   private ChamsCompositeTarget() {
   }

   public static void beginFrame() {
      Minecraft minecraft = Minecraft.getInstance();
      int width = minecraft.getWindow().getWidth();
      int height = minecraft.getWindow().getHeight();
      if (target == null) {
         target = new TextureTarget("oyvey_chams_hidden", width, height, true);
      } else if (target.width != width || target.height != height) {
         target.resize(width, height);
      }

      target.destroyBuffers();
      target.createBuffers(width, height);
      frameActive = true;
      hasHiddenContent = false;
   }

   public static void markUsed() {
      if (frameActive) {
         hasHiddenContent = true;
      }
   }

   public static void compositeToMain() {
      if (frameActive && hasHiddenContent && target != null) {
         RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         RenderPass renderPass = encoder.createRenderPass(() -> "oyvey_chams_composite", main.getColorTextureView(), OptionalInt.empty());

         try {
            renderPass.setPipeline(COMPOSITE_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture("InSampler", target.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            renderPass.draw(0, 3);
         } catch (Throwable var6) {
            if (renderPass != null) {
               try {
                  renderPass.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (renderPass != null) {
            renderPass.close();
         }

         frameActive = false;
         hasHiddenContent = false;
      } else {
         frameActive = false;
         hasHiddenContent = false;
      }
   }

   static RenderTarget getTarget() {
      if (target == null) {
         Minecraft minecraft = Minecraft.getInstance();
         target = new TextureTarget("oyvey_chams_hidden", minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(), true);
      }

      return target;
   }
}
