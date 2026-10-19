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

/**
 * RenderKit-style SDF rounded rectangle adapted to Minecraft 26.1's GUI state API.
 * Geometry remains a single quad; radius and antialiasing are resolved in the shader.
 */
public record RoundedRectRenderState(
   Matrix3x2f pose,
   float x0,
   float y0,
   float x1,
   float y1,
   float radius,
   float fringe,
   int colorA,
   int colorB,
   boolean vertical,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public static RoundedRectRenderState solid(
      Matrix3x2f pose, float x, float y, float width, float height, float radius, int color, @Nullable ScreenRectangle scissorArea,
      double pixelsPerGuiUnit
   ) {
      return gradient(pose, x, y, width, height, radius, color, color, false, scissorArea, pixelsPerGuiUnit);
   }

   public static RoundedRectRenderState gradient(
      Matrix3x2f pose,
      float x,
      float y,
      float width,
      float height,
      float radius,
      int colorA,
      int colorB,
      boolean vertical,
      @Nullable ScreenRectangle scissorArea,
      double pixelsPerGuiUnit
   ) {
      float clampedRadius = Math.max(0.01F, Math.min(radius, Math.min(width, height) * 0.5F));
      float fringe = RoundedGeometry.fringe(pose, pixelsPerGuiUnit);
      ScreenRectangle clippedBounds = RoundedGeometry.bounds(pose, x, y, width, height, fringe, scissorArea);
      return new RoundedRectRenderState(
         new Matrix3x2f(pose), x, y, x + width, y + height, clampedRadius, fringe, colorA, colorB, vertical, scissorArea, clippedBounds
      );
   }

   @Override
   public void buildVertices(VertexConsumer consumer) {
      float halfU = (this.x1 - this.x0) * 0.5F + this.fringe;
      float halfV = (this.y1 - this.y0) * 0.5F + this.fringe;
      int radius = RoundedGeometry.parameter(this.radius, Math.min(halfU, halfV) - this.fringe);
      int topLeft = this.colorA;
      int bottomLeft = this.vertical ? this.colorB : this.colorA;
      int bottomRight = this.colorB;
      int topRight = this.vertical ? this.colorA : this.colorB;
      Vector2f scratch = new Vector2f();
      addVertex(consumer, scratch, this.x0 - this.fringe, this.y0 - this.fringe, -halfU, -halfV, radius, topLeft);
      addVertex(consumer, scratch, this.x0 - this.fringe, this.y1 + this.fringe, -halfU, halfV, radius, bottomLeft);
      addVertex(consumer, scratch, this.x1 + this.fringe, this.y1 + this.fringe, halfU, halfV, radius, bottomRight);
      addVertex(consumer, scratch, this.x1 + this.fringe, this.y0 - this.fringe, halfU, -halfV, radius, topRight);
   }

   private void addVertex(VertexConsumer consumer, Vector2f scratch, float x, float y, float u, float v, int radius, int color) {
      this.pose.transformPosition(x, y, scratch);
      consumer.addVertex(scratch.x, scratch.y, this.fringe).setUv(u, v).setUv2(radius, 0).setColor(color);
   }

   @Override
   public RenderPipeline pipeline() {
      return Pipelines.ROUNDED_RECT_PIPELINE;
   }

   @Override
   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }
}
