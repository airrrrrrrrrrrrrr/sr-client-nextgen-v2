package me.alpha432.oyvey.util.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.alpha432.oyvey.util.render.Pipelines;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

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
   int colorA,
   int colorB,
   boolean vertical,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public static RoundedRectRenderState solid(
      Matrix3x2f pose, float x, float y, float width, float height, float radius, int color, @Nullable ScreenRectangle scissorArea
   ) {
      return gradient(pose, x, y, width, height, radius, color, color, false, scissorArea);
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
      @Nullable ScreenRectangle scissorArea
   ) {
      float clampedRadius = Math.max(0.01F, Math.min(radius, Math.min(width, height) * 0.5F));
      ScreenRectangle rawBounds = new ScreenRectangle(
         (int)Math.floor(x) - 1,
         (int)Math.floor(y) - 1,
         Math.max(1, (int)Math.ceil(width) + 2),
         Math.max(1, (int)Math.ceil(height) + 2)
      ).transformMaxBounds(pose);
      ScreenRectangle clippedBounds = scissorArea == null ? rawBounds : scissorArea.intersection(rawBounds);
      return new RoundedRectRenderState(
         new Matrix3x2f(pose), x, y, x + width, y + height, clampedRadius, colorA, colorB, vertical, scissorArea, clippedBounds
      );
   }

   @Override
   public void buildVertices(VertexConsumer consumer) {
      float halfU = (this.x1 - this.x0) * 0.5F / this.radius;
      float halfV = (this.y1 - this.y0) * 0.5F / this.radius;
      int topLeft = this.colorA;
      int bottomLeft = this.vertical ? this.colorB : this.colorA;
      int bottomRight = this.colorB;
      int topRight = this.vertical ? this.colorA : this.colorB;
      consumer.addVertexWith2DPose(this.pose, this.x0, this.y0).setUv(-halfU, -halfV).setColor(topLeft);
      consumer.addVertexWith2DPose(this.pose, this.x0, this.y1).setUv(-halfU, halfV).setColor(bottomLeft);
      consumer.addVertexWith2DPose(this.pose, this.x1, this.y1).setUv(halfU, halfV).setColor(bottomRight);
      consumer.addVertexWith2DPose(this.pose, this.x1, this.y0).setUv(halfU, -halfV).setColor(topRight);
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
