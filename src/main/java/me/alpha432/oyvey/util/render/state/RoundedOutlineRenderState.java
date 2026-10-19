package me.alpha432.oyvey.util.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.alpha432.oyvey.util.render.Pipelines;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

/** A subpixel SDF outline with continuously rounded inner and outer edges. */
public record RoundedOutlineRenderState(
   Matrix3x2f pose,
   float x0,
   float y0,
   float x1,
   float y1,
   float radius,
   float thickness,
   float fringe,
   int color,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public static RoundedOutlineRenderState create(
      Matrix3x2f pose,
      float x,
      float y,
      float width,
      float height,
      float radius,
      float thickness,
      int color,
      @Nullable ScreenRectangle scissorArea,
      double pixelsPerGuiUnit
   ) {
      float halfShort = Math.min(width, height) * 0.5F;
      float clampedRadius = Math.max(0.0F, Math.min(radius, halfShort));
      float clampedThickness = Math.max(0.1F, Math.min(thickness, Math.max(0.1F, halfShort)));
      float fringe = RoundedGeometry.fringe(pose, pixelsPerGuiUnit);
      ScreenRectangle clippedBounds = RoundedGeometry.bounds(pose, x, y, width, height, fringe, scissorArea);
      return new RoundedOutlineRenderState(
         new Matrix3x2f(pose), x, y, x + width, y + height, clampedRadius, clampedThickness, fringe, color, scissorArea, clippedBounds
      );
   }

   @Override
   public void buildVertices(VertexConsumer consumer) {
      float halfU = (this.x1 - this.x0) * 0.5F + this.fringe;
      float halfV = (this.y1 - this.y0) * 0.5F + this.fringe;
      float halfShort = Math.min(halfU, halfV) - this.fringe;
      int radius = RoundedGeometry.parameter(this.radius, halfShort);
      int thickness = RoundedGeometry.parameter(this.thickness, halfShort);
      Vector2f scratch = new Vector2f();
      this.addVertex(consumer, scratch, this.x0 - this.fringe, this.y0 - this.fringe, -halfU, -halfV, radius, thickness);
      this.addVertex(consumer, scratch, this.x0 - this.fringe, this.y1 + this.fringe, -halfU, halfV, radius, thickness);
      this.addVertex(consumer, scratch, this.x1 + this.fringe, this.y1 + this.fringe, halfU, halfV, radius, thickness);
      this.addVertex(consumer, scratch, this.x1 + this.fringe, this.y0 - this.fringe, halfU, -halfV, radius, thickness);
   }

   private void addVertex(VertexConsumer consumer, Vector2f scratch, float x, float y, float u, float v, int radius, int thickness) {
      this.pose.transformPosition(x, y, scratch);
      consumer.addVertex(scratch.x, scratch.y, this.fringe).setUv(u, v).setUv2(radius, thickness).setColor(this.color);
   }

   @Override
   public RenderPipeline pipeline() {
      return Pipelines.ROUNDED_OUTLINE_PIPELINE;
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }
}
