package me.alpha432.oyvey.util.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.alpha432.oyvey.util.render.Pipelines;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

/** Smooth inverse quarter-circle, used to round images and clipped panel rows. */
public record CornerMaskRenderState(Matrix3x2f pose, float x, float y, float radius,
                                    boolean right, boolean bottom, int color,
                                    ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
   public static CornerMaskRenderState create(Matrix3x2f pose, float x, float y, float radius,
                                              boolean right, boolean bottom, int color, ScreenRectangle scissor) {
      return new CornerMaskRenderState(new Matrix3x2f(pose), x, y, radius, right, bottom, color, scissor,
         RoundedGeometry.bounds(pose, x, y, radius, radius, 0, scissor));
   }

   @Override public void buildVertices(VertexConsumer consumer) {
      Vector2f scratch = new Vector2f();
      vertex(consumer, scratch, 0, 0);
      vertex(consumer, scratch, 0, 1);
      vertex(consumer, scratch, 1, 1);
      vertex(consumer, scratch, 1, 0);
   }

   private void vertex(VertexConsumer consumer, Vector2f scratch, float u, float v) {
      pose.transformPosition(x + u * radius, y + v * radius, scratch);
      consumer.addVertex(scratch.x, scratch.y, 0).setUv(right ? u : u - 1, bottom ? v : v - 1)
         .setUv2(0, -1).setColor(color);
   }

   @Override public RenderPipeline pipeline() { return Pipelines.ROUNDED_RECT_PIPELINE; }
   @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
}
