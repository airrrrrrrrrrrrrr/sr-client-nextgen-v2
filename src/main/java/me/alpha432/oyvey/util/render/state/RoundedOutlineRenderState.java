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
      @Nullable ScreenRectangle scissorArea
   ) {
      float halfShort = Math.min(width, height) * 0.5F;
      float clampedRadius = Math.max(0.0F, Math.min(radius, halfShort));
      float clampedThickness = Math.max(0.1F, Math.min(thickness, Math.max(0.1F, halfShort)));
      ScreenRectangle rawBounds = new ScreenRectangle(
         (int)Math.floor(x) - 2,
         (int)Math.floor(y) - 2,
         Math.max(1, (int)Math.ceil(width) + 4),
         Math.max(1, (int)Math.ceil(height) + 4)
      ).transformMaxBounds(pose);
      ScreenRectangle clippedBounds = scissorArea == null ? rawBounds : scissorArea.intersection(rawBounds);
      return new RoundedOutlineRenderState(
         new Matrix3x2f(pose), x, y, x + width, y + height, clampedRadius, clampedThickness, color, scissorArea, clippedBounds
      );
   }

   @Override
   public void buildVertices(VertexConsumer consumer) {
      float halfU = (this.x1 - this.x0) * 0.5F;
      float halfV = (this.y1 - this.y0) * 0.5F;
      float packed = this.radius + Math.min(this.thickness, 255.0F) / 256.0F;
      Vector2f scratch = new Vector2f();
      this.addVertex(consumer, scratch, this.x0, this.y0, -halfU, -halfV, packed);
      this.addVertex(consumer, scratch, this.x0, this.y1, -halfU, halfV, packed);
      this.addVertex(consumer, scratch, this.x1, this.y1, halfU, halfV, packed);
      this.addVertex(consumer, scratch, this.x1, this.y0, halfU, -halfV, packed);
   }

   private void addVertex(VertexConsumer consumer, Vector2f scratch, float x, float y, float u, float v, float packed) {
      this.pose.transformPosition(x, y, scratch);
      consumer.addVertex(scratch.x, scratch.y, packed).setUv(u, v).setColor(this.color);
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
