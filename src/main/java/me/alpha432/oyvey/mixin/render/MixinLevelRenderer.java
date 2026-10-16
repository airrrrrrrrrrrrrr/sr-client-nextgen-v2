package me.alpha432.oyvey.mixin.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.event.impl.render.RenderBlockOutlineEvent;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
   @Inject(method = "renderBlockOutline", at = @At("HEAD"), cancellable = true)
   public void renderBlockOutline(BufferSource bufferSource, PoseStack poseStack, boolean bl, LevelRenderState levelRenderState, CallbackInfo ci) {
      if (Util.EVENT_BUS.post(new RenderBlockOutlineEvent())) {
         ci.cancel();
      }
   }

   @Inject(method = "renderLevel", at = @At("RETURN"))
   private void render(
      GraphicsResourceAllocator allocator,
      DeltaTracker tickCounter,
      boolean renderBlockOutline,
      CameraRenderState cameraRenderState,
      Matrix4fc positionMatrix,
      GpuBufferSlice fogBuffer,
      Vector4f fogColor,
      boolean renderSky,
      ChunkSectionsToRender chunkSectionsToRender,
      CallbackInfo ci
   ) {
      PoseStack stack = new PoseStack();
      stack.pushPose();
      stack.mulPose(Axis.XP.rotationDegrees(Util.mc.gameRenderer.getMainCamera().xRot()));
      stack.mulPose(Axis.YP.rotationDegrees(Util.mc.gameRenderer.getMainCamera().yRot() + 180.0F));
      Render3DEvent event = new Render3DEvent(stack, tickCounter.getGameTimeDeltaPartialTick(true));
      Util.EVENT_BUS.post(event);
      stack.popPose();
   }
}
