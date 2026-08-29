package me.alpha432.oyvey.util.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import java.awt.Color;
import me.alpha432.oyvey.util.render.state.RectRenderState;
import me.alpha432.oyvey.util.render.state.RoundedOutlineRenderState;
import me.alpha432.oyvey.util.render.state.RoundedRectRenderState;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;

public class RenderUtil implements Util {
   public static boolean highQuality = true;

   public static Vec3 interpolatedPosition(Entity entity, float partialTick) {
      float delta = Mth.clamp(partialTick, 0.0F, 1.0F);
      return new Vec3(
         Mth.lerp((double)delta, entity.xo, entity.getX()),
         Mth.lerp((double)delta, entity.yo, entity.getY()),
         Mth.lerp((double)delta, entity.zo, entity.getZ())
      );
   }

   public static AABB interpolatedBox(Entity entity, float partialTick) {
      Vec3 position = interpolatedPosition(entity, partialTick);
      return entity.getBoundingBox().move(position.x - entity.getX(), position.y - entity.getY(), position.z - entity.getZ());
   }

   public static void rect(GuiGraphicsExtractor ctx, float x1, float y1, float x2, float y2, int color) {
      ctx.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), color);
   }

   public static void rect(GuiGraphicsExtractor ctx, float x1, float y1, float x2, float y2, int color, float width) {
      int w = Math.max(1, Math.round(width));
      int ix1 = Math.round(x1);
      int iy1 = Math.round(y1);
      int ix2 = Math.round(x2);
      int iy2 = Math.round(y2);
      ctx.fill(ix1, iy1, ix2, iy1 + w, color);
      ctx.fill(ix2 - w, iy1, ix2, iy2, color);
      ctx.fill(ix1, iy2 - w, ix2, iy2, color);
      ctx.fill(ix1, iy1, ix1 + w, iy2, color);
   }

   public static void horizontalGradient(GuiGraphicsExtractor ctx, float x1, float y1, float x2, float y2, Color left, Color right) {
      gradient(ctx, Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), left.getRGB(), left.getRGB(), right.getRGB(), right.getRGB());
   }

   public static void verticalGradient(GuiGraphicsExtractor ctx, float x1, float y1, float x2, float y2, Color top, Color bottom) {
      gradient(ctx, Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), top.getRGB(), bottom.getRGB(), bottom.getRGB(), top.getRGB());
   }

   public static void gradient(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int topLeft, int bottomLeft, int bottomRight, int topRight) {
      ctx.guiRenderState
         .addGuiElement(
            new RectRenderState(
               RenderPipelines.GUI,
               TextureSetup.noTexture(),
               new Matrix3x2f(ctx.pose()),
               x1,
               y1,
               x2,
               y2,
               topLeft,
               bottomLeft,
               bottomRight,
               topRight,
               ctx.scissorStack.peek()
            )
         );
   }

   public static void roundedRect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int r, int color) {
      roundedRect(ctx, (float)x, (float)y, (float)w, (float)h, (float)r, color);
   }

   public static void roundedRect(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float r, int color) {
      if (w <= 0.0F || h <= 0.0F) {
         return;
      }

      if (r <= 0.0F) {
         ctx.fill(Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), color);
      } else if (highQuality) {
         ctx.guiRenderState
            .addGuiElement(RoundedRectRenderState.solid(new Matrix3x2f(ctx.pose()), x, y, w, h, r, color, ctx.scissorStack.peek()));
      } else {
         int ix = Math.round(x);
         int iy = Math.round(y);
         int iw = Math.max(1, Math.round(w));
         int ih = Math.max(1, Math.round(h));
         int ir = Math.max(1, Math.min(Math.round(r), Math.min(iw / 2, ih / 2)));
         ctx.fill(ix, iy + ir, ix + iw, iy + ih - ir, color);

         for (int i = 0; i < ir; i++) {
            int e = (int)Math.sqrt((double)ir * ir - (double)(ir - i) * (ir - i));
            ctx.fill(ix + ir - e, iy + i, ix + iw - ir + e, iy + i + 1, color);
            ctx.fill(ix + ir - e, iy + ih - 1 - i, ix + iw - ir + e, iy + ih - i, color);
         }
      }
   }

   public static void roundedRectGradient(
      GuiGraphicsExtractor ctx, float x, float y, float w, float h, float r, int colorA, int colorB, boolean vertical
   ) {
      if (w <= 0.0F || h <= 0.0F) {
         return;
      }

      if (!highQuality || r <= 0.0F) {
         gradient(
            ctx,
            Math.round(x),
            Math.round(y),
            Math.round(x + w),
            Math.round(y + h),
            colorA,
            vertical ? colorB : colorA,
            colorB,
            vertical ? colorA : colorB
         );
         return;
      }

      ctx.guiRenderState
         .addGuiElement(
            RoundedRectRenderState.gradient(new Matrix3x2f(ctx.pose()), x, y, w, h, r, colorA, colorB, vertical, ctx.scissorStack.peek())
         );
   }

   public static void roundedOutline(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float r, float thickness, int color) {
      if (w <= 0.0F || h <= 0.0F || thickness <= 0.0F) {
         return;
      }

      if (!highQuality) {
         rect(ctx, x, y, x + w, y + h, color, thickness);
         return;
      }

      ctx.guiRenderState
         .addGuiElement(
            RoundedOutlineRenderState.create(
               new Matrix3x2f(ctx.pose()), x, y, w, h, r, thickness, color, ctx.scissorStack.peek()
            )
         );
   }

   public static void dropShadow(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, float spread, int alpha) {
      int steps = highQuality ? 12 : 5;
      int maxAlpha = Mth.clamp(alpha, 0, 255);

      for (int i = steps; i >= 1; i--) {
         float t = (float)i / steps;
         float expansion = spread * t;
         float falloff = 1.0F - t;
         int layerAlpha = Math.max(1, Math.round(maxAlpha * falloff * falloff / Math.max(1.0F, steps * 0.42F)));
         roundedRect(
            ctx,
            x - expansion * 0.5F,
            y - expansion * 0.35F + spread * 0.18F,
            w + expansion,
            h + expansion,
            radius + expansion * 0.45F,
            layerAlpha << 24
         );
      }
   }

   public static void maskCorners(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int r, int mask) {
      maskCorners(ctx, x, y, w, h, r, mask, true, true, true, true);
   }

   public static void maskCorners(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int r, int mask, boolean tl, boolean tr, boolean bl, boolean br) {
      if (r > 0) {
         r = Math.min(r, Math.min(w / 2, h / 2));
         int a = mask >>> 24 & 0xFF;
         int rgb = mask & 16777215;
         if (tl) {
            maskCorner(ctx, x, y, r, false, false, a, rgb);
         }

         if (tr) {
            maskCorner(ctx, x + w - r, y, r, true, false, a, rgb);
         }

         if (bl) {
            maskCorner(ctx, x, y + h - r, r, false, true, a, rgb);
         }

         if (br) {
            maskCorner(ctx, x + w - r, y + h - r, r, true, true, a, rgb);
         }
      }
   }

   private static void maskCorner(GuiGraphicsExtractor ctx, int ox, int oy, int r, boolean posX, boolean posY, int a, int rgb) {
      if (!highQuality) {
         float rsq = (float)r * r;

         for (int py = 0; py < r; py++) {
            float dy = py + 0.5F - (posY ? 0.0F : r);
            float hw = (float)Math.sqrt(Math.max(0.0F, rsq - dy * dy));
            if (!posX) {
               int bx = Math.round(r - hw);
               if (bx > 0) {
                  ctx.fill(ox, oy + py, ox + bx, oy + py + 1, a << 24 | rgb);
               }
            } else {
               int bx = Math.round(hw);
               if (bx < r) {
                  ctx.fill(ox + bx, oy + py, ox + r, oy + py + 1, a << 24 | rgb);
               }
            }
         }
      } else {
         float[] cov = CornerCache.getCov(r, posX, posY);

         for (int py = 0; py < r; py++) {
            int opaqueX0 = -1;

            for (int px = 0; px < r; px++) {
               float outside = 1.0F - cov[px * r + py];
               if (outside == 1.0F) {
                  if (opaqueX0 < 0) {
                     opaqueX0 = px;
                  }
               } else {
                  if (opaqueX0 >= 0) {
                     ctx.fill(ox + opaqueX0, oy + py, ox + px, oy + py + 1, a << 24 | rgb);
                     opaqueX0 = -1;
                  }

                  if (outside > 0.0F) {
                     int pa = Math.round(a * outside);
                     if (pa > 0) {
                        ctx.fill(ox + px, oy + py, ox + px + 1, oy + py + 1, pa << 24 | rgb);
                     }
                  }
               }
            }

            if (opaqueX0 >= 0) {
               ctx.fill(ox + opaqueX0, oy + py, ox + r, oy + py + 1, a << 24 | rgb);
            }
         }
      }
   }

   public static void rect(PoseStack stack, float x1, float y1, float x2, float y2, int color) {
      rectFilled(stack, x1, y1, x2, y2, color);
   }

   public static void rect(PoseStack stack, float x1, float y1, float x2, float y2, int color, float width) {
      drawHorizontalLine(stack, x1, x2, y1, color, width);
      drawVerticalLine(stack, x2, y1, y2, color, width);
      drawHorizontalLine(stack, x1, x2, y2, color, width);
      drawVerticalLine(stack, x1, y1, y2, color, width);
   }

   protected static void drawHorizontalLine(PoseStack stack, float x1, float x2, float y, int color) {
      if (x2 < x1) {
         float t = x1;
         x1 = x2;
         x2 = t;
      }

      rectFilled(stack, x1, y, x2 + 1.0F, y + 1.0F, color);
   }

   protected static void drawVerticalLine(PoseStack stack, float x, float y1, float y2, int color) {
      if (y2 < y1) {
         float t = y1;
         y1 = y2;
         y2 = t;
      }

      rectFilled(stack, x, y1 + 1.0F, x + 1.0F, y2, color);
   }

   protected static void drawHorizontalLine(PoseStack stack, float x1, float x2, float y, int color, float width) {
      if (x2 < x1) {
         float t = x1;
         x1 = x2;
         x2 = t;
      }

      rectFilled(stack, x1, y, x2 + width, y + width, color);
   }

   protected static void drawVerticalLine(PoseStack stack, float x, float y1, float y2, int color, float width) {
      if (y2 < y1) {
         float t = y1;
         y1 = y2;
         y2 = t;
      }

      rectFilled(stack, x, y1 + width, x + width, y2, color);
   }

   public static void rectFilled(PoseStack stack, float x1, float y1, float x2, float y2, int color) {
      if (x1 > x2) {
         float t = x1;
         x1 = x2;
         x2 = t;
      }

      if (y1 > y2) {
         float t = y1;
         y1 = y2;
         y2 = t;
      }

      float a = (color >> 24 & 0xFF) / 255.0F;
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      BufferBuilder buf = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      buf.addVertex(stack.last().pose(), x1, y1, 0.0F).setColor(r, g, b, a);
      buf.addVertex(stack.last().pose(), x1, y2, 0.0F).setColor(r, g, b, a);
      buf.addVertex(stack.last().pose(), x2, y2, 0.0F).setColor(r, g, b, a);
      buf.addVertex(stack.last().pose(), x2, y1, 0.0F).setColor(r, g, b, a);
      Layers.quads().draw(buf.buildOrThrow());
   }

   public static void horizontalGradient(PoseStack stack, float x1, float y1, float x2, float y2, Color left, Color right) {
      BufferBuilder buf = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      float lr = left.getRed() / 255.0F;
      float lg = left.getGreen() / 255.0F;
      float lb = left.getBlue() / 255.0F;
      float la = left.getAlpha() / 255.0F;
      float rr = right.getRed() / 255.0F;
      float rg = right.getGreen() / 255.0F;
      float rb = right.getBlue() / 255.0F;
      float ra = right.getAlpha() / 255.0F;
      buf.addVertex(stack.last().pose(), x1, y1, 0.0F).setColor(lr, lg, lb, la);
      buf.addVertex(stack.last().pose(), x1, y2, 0.0F).setColor(lr, lg, lb, la);
      buf.addVertex(stack.last().pose(), x2, y2, 0.0F).setColor(rr, rg, rb, ra);
      buf.addVertex(stack.last().pose(), x2, y1, 0.0F).setColor(rr, rg, rb, ra);
      Layers.quads().draw(buf.buildOrThrow());
   }

   public static void verticalGradient(PoseStack stack, float x1, float y1, float x2, float y2, Color top, Color bottom) {
      BufferBuilder buf = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      float tr = top.getRed() / 255.0F;
      float tg = top.getGreen() / 255.0F;
      float tb = top.getBlue() / 255.0F;
      float ta = top.getAlpha() / 255.0F;
      float br = bottom.getRed() / 255.0F;
      float bg = bottom.getGreen() / 255.0F;
      float bb = bottom.getBlue() / 255.0F;
      float ba = bottom.getAlpha() / 255.0F;
      buf.addVertex(stack.last().pose(), x1, y1, 0.0F).setColor(tr, tg, tb, ta);
      buf.addVertex(stack.last().pose(), x1, y2, 0.0F).setColor(br, bg, bb, ba);
      buf.addVertex(stack.last().pose(), x2, y2, 0.0F).setColor(br, bg, bb, ba);
      buf.addVertex(stack.last().pose(), x2, y1, 0.0F).setColor(tr, tg, tb, ta);
      Layers.quads().draw(buf.buildOrThrow());
   }

   public static void drawBoxFilled(PoseStack stack, AABB box, Color c) {
      Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
      float x0 = (float)(box.minX - cam.x());
      float y0 = (float)(box.minY - cam.y());
      float z0 = (float)(box.minZ - cam.z());
      float x1 = (float)(box.maxX - cam.x());
      float y1 = (float)(box.maxY - cam.y());
      float z1 = (float)(box.maxZ - cam.z());
      int argb = c.getRGB();
      BufferBuilder buf = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      Matrix4f m = stack.last().pose();
      buf.addVertex(m, x0, y0, z0).setColor(argb);
      buf.addVertex(m, x1, y0, z0).setColor(argb);
      buf.addVertex(m, x1, y0, z1).setColor(argb);
      buf.addVertex(m, x0, y0, z1).setColor(argb);
      buf.addVertex(m, x0, y1, z0).setColor(argb);
      buf.addVertex(m, x0, y1, z1).setColor(argb);
      buf.addVertex(m, x1, y1, z1).setColor(argb);
      buf.addVertex(m, x1, y1, z0).setColor(argb);
      buf.addVertex(m, x0, y0, z0).setColor(argb);
      buf.addVertex(m, x0, y1, z0).setColor(argb);
      buf.addVertex(m, x1, y1, z0).setColor(argb);
      buf.addVertex(m, x1, y0, z0).setColor(argb);
      buf.addVertex(m, x1, y0, z0).setColor(argb);
      buf.addVertex(m, x1, y1, z0).setColor(argb);
      buf.addVertex(m, x1, y1, z1).setColor(argb);
      buf.addVertex(m, x1, y0, z1).setColor(argb);
      buf.addVertex(m, x0, y0, z1).setColor(argb);
      buf.addVertex(m, x1, y0, z1).setColor(argb);
      buf.addVertex(m, x1, y1, z1).setColor(argb);
      buf.addVertex(m, x0, y1, z1).setColor(argb);
      buf.addVertex(m, x0, y0, z0).setColor(argb);
      buf.addVertex(m, x0, y0, z1).setColor(argb);
      buf.addVertex(m, x0, y1, z1).setColor(argb);
      buf.addVertex(m, x0, y1, z0).setColor(argb);
      Layers.quads().draw(buf.buildOrThrow());
   }

   public static void drawBoxFilled(PoseStack stack, Vec3 vec, Color c) {
      drawBoxFilled(stack, AABB.unitCubeFromLowerCorner(vec), c);
   }

   public static void drawBoxFilled(PoseStack stack, BlockPos bp, Color c) {
      drawBoxFilled(stack, new AABB(bp), c);
   }

   public static void drawBox(PoseStack stack, AABB box, Color c, float lineWidth) {
      Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
      float x0 = (float)(box.minX - cam.x());
      float y0 = (float)(box.minY - cam.y());
      float z0 = (float)(box.minZ - cam.z());
      float x1 = (float)(box.maxX - cam.x());
      float y1 = (float)(box.maxY - cam.y());
      float z1 = (float)(box.maxZ - cam.z());
      int argb = c.getRGB();
      BufferBuilder buf = Tesselator.getInstance().begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_LINE_WIDTH);
      Pose pose = stack.last();
      edge(buf, pose, x0, y0, z0, x1, y0, z0, argb, lineWidth);
      edge(buf, pose, x1, y0, z0, x1, y0, z1, argb, lineWidth);
      edge(buf, pose, x1, y0, z1, x0, y0, z1, argb, lineWidth);
      edge(buf, pose, x0, y0, z1, x0, y0, z0, argb, lineWidth);
      edge(buf, pose, x0, y1, z0, x1, y1, z0, argb, lineWidth);
      edge(buf, pose, x1, y1, z0, x1, y1, z1, argb, lineWidth);
      edge(buf, pose, x1, y1, z1, x0, y1, z1, argb, lineWidth);
      edge(buf, pose, x0, y1, z1, x0, y1, z0, argb, lineWidth);
      edge(buf, pose, x0, y0, z0, x0, y1, z0, argb, lineWidth);
      edge(buf, pose, x1, y0, z0, x1, y1, z0, argb, lineWidth);
      edge(buf, pose, x0, y0, z1, x0, y1, z1, argb, lineWidth);
      edge(buf, pose, x1, y0, z1, x1, y1, z1, argb, lineWidth);
      Layers.lines().draw(buf.buildOrThrow());
   }

   private static void edge(BufferBuilder buf, Pose pose, float ax, float ay, float az, float bx, float by, float bz, int argb, float w) {
      buf.addVertex(pose, ax, ay, az).setColor(argb).setLineWidth(w);
      buf.addVertex(pose, bx, by, bz).setColor(argb).setLineWidth(w);
   }

   public static void drawBox(PoseStack stack, Vec3 vec, Color c, float lineWidth) {
      drawBox(stack, AABB.unitCubeFromLowerCorner(vec), c, lineWidth);
   }

   public static void drawBox(PoseStack stack, BlockPos bp, Color c, float lineWidth) {
      drawBox(stack, new AABB(bp), c, lineWidth);
   }

   public static PoseStack matrixFrom(Vec3 pos) {
      PoseStack stack = new PoseStack();
      Camera cam = mc.gameRenderer.getMainCamera();
      stack.mulPose(Axis.XP.rotationDegrees(cam.xRot()));
      stack.mulPose(Axis.YP.rotationDegrees(cam.yRot() + 180.0F));
      stack.translate(pos.x() - cam.position().x, pos.y() - cam.position().y, pos.z() - cam.position().z);
      return stack;
   }

   public static void drawCylinder(PoseStack stack, double wx, double wy, double wz, double radius, double height, int segs, Color color) {
      Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
      float ox = (float)(wx - cam.x());
      float oy = (float)(wy - cam.y());
      float oz = (float)(wz - cam.z());
      float r = (float)radius;
      float h = (float)height;
      float step = (float)(6.283185307179586 / segs);
      int argb = color.getRGB();
      BufferBuilder buf = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      Matrix4f m = stack.last().pose();

      for (int i = 0; i < segs; i++) {
         float a0 = i * step;
         float a1 = a0 + step;
         float x0 = ox + (float)Math.cos(a0) * r;
         float z0 = oz + (float)Math.sin(a0) * r;
         float x1 = ox + (float)Math.cos(a1) * r;
         float z1 = oz + (float)Math.sin(a1) * r;
         buf.addVertex(m, x0, oy, z0).setColor(argb);
         buf.addVertex(m, x1, oy, z1).setColor(argb);
         buf.addVertex(m, x1, oy + h, z1).setColor(argb);
         buf.addVertex(m, x0, oy + h, z0).setColor(argb);
         buf.addVertex(m, ox, oy, oz).setColor(argb);
         buf.addVertex(m, x1, oy, z1).setColor(argb);
         buf.addVertex(m, x0, oy, z0).setColor(argb);
         buf.addVertex(m, ox, oy, oz).setColor(argb);
         buf.addVertex(m, ox, oy + h, oz).setColor(argb);
         buf.addVertex(m, x0, oy + h, z0).setColor(argb);
         buf.addVertex(m, x1, oy + h, z1).setColor(argb);
         buf.addVertex(m, ox, oy + h, oz).setColor(argb);
      }

      Layers.quads().draw(buf.buildOrThrow());
   }

   public static void drawCylinderOriented(
      PoseStack stack, double sx, double sy, double sz, double ex, double ey, double ez, double radius, int segs, Color color
   ) {
      Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
      double dx = ex - sx;
      double dy = ey - sy;
      double dz = ez - sz;
      double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (!(len < 1.0E-6)) {
         dx /= len;
         dy /= len;
         dz /= len;
         double ux;
         double uy;
         double uz;
         if (Math.abs(dy) < 0.99) {
            ux = 0.0;
            uy = 1.0;
            uz = 0.0;
         } else {
            ux = 1.0;
            uy = 0.0;
            uz = 0.0;
         }

         double rx = dy * uz - dz * uy;
         double ry = dz * ux - dx * uz;
         double rz = dx * uy - dy * ux;
         double rl = Math.sqrt(rx * rx + ry * ry + rz * rz);
         rx /= rl;
         ry /= rl;
         rz /= rl;
         ux = ry * dz - rz * dy;
         uy = rz * dx - rx * dz;
         uz = rx * dy - ry * dx;
         int argb = color.getRGB();
         float step = (float)(6.283185307179586 / segs);
         BufferBuilder buf = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         Matrix4f m = stack.last().pose();
         float r = (float)radius;
         float sox = (float)(sx - cam.x());
         float soy = (float)(sy - cam.y());
         float soz = (float)(sz - cam.z());
         float eox = (float)(ex - cam.x());
         float eoy = (float)(ey - cam.y());
         float eoz = (float)(ez - cam.z());

         for (int i = 0; i < segs; i++) {
            float a0 = i * step;
            float a1 = a0 + step;
            float c0 = (float)Math.cos(a0);
            float s0 = (float)Math.sin(a0);
            float c1 = (float)Math.cos(a1);
            float s1 = (float)Math.sin(a1);
            float p0sx = sox + r * (c0 * (float)rx + s0 * (float)ux);
            float p0sy = soy + r * (c0 * (float)ry + s0 * (float)uy);
            float p0sz = soz + r * (c0 * (float)rz + s0 * (float)uz);
            float p1sx = sox + r * (c1 * (float)rx + s1 * (float)ux);
            float p1sy = soy + r * (c1 * (float)ry + s1 * (float)uy);
            float p1sz = soz + r * (c1 * (float)rz + s1 * (float)uz);
            float p0ex = eox + r * (c0 * (float)rx + s0 * (float)ux);
            float p0ey = eoy + r * (c0 * (float)ry + s0 * (float)uy);
            float p0ez = eoz + r * (c0 * (float)rz + s0 * (float)uz);
            float p1ex = eox + r * (c1 * (float)rx + s1 * (float)ux);
            float p1ey = eoy + r * (c1 * (float)ry + s1 * (float)uy);
            float p1ez = eoz + r * (c1 * (float)rz + s1 * (float)uz);
            buf.addVertex(m, p0sx, p0sy, p0sz).setColor(argb);
            buf.addVertex(m, p1sx, p1sy, p1sz).setColor(argb);
            buf.addVertex(m, p1ex, p1ey, p1ez).setColor(argb);
            buf.addVertex(m, p0ex, p0ey, p0ez).setColor(argb);
            buf.addVertex(m, sox, soy, soz).setColor(argb);
            buf.addVertex(m, p1sx, p1sy, p1sz).setColor(argb);
            buf.addVertex(m, p0sx, p0sy, p0sz).setColor(argb);
            buf.addVertex(m, sox, soy, soz).setColor(argb);
            buf.addVertex(m, eox, eoy, eoz).setColor(argb);
            buf.addVertex(m, p0ex, p0ey, p0ez).setColor(argb);
            buf.addVertex(m, p1ex, p1ey, p1ez).setColor(argb);
            buf.addVertex(m, eox, eoy, eoz).setColor(argb);
         }

         Layers.quads().draw(buf.buildOrThrow());
      }
   }

   public static void drawSphere(PoseStack stack, double wx, double wy, double wz, double radius, int segs, Color color) {
      Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
      float ox = (float)(wx - cam.x());
      float oy = (float)(wy - cam.y());
      float oz = (float)(wz - cam.z());
      float r = (float)radius;
      int argb = color.getRGB();
      int rings = Math.max(2, segs / 2);
      BufferBuilder buf = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      Matrix4f m = stack.last().pose();

      for (int lat = 0; lat < rings; lat++) {
         float phi0 = (float)(3.141592653589793 * lat / rings) - 1.5707964F;
         float phi1 = (float)(3.141592653589793 * (lat + 1) / rings) - 1.5707964F;
         float cp0 = (float)Math.cos(phi0);
         float sp0 = (float)Math.sin(phi0);
         float cp1 = (float)Math.cos(phi1);
         float sp1 = (float)Math.sin(phi1);

         for (int lon = 0; lon < segs; lon++) {
            float th0 = (float)(6.283185307179586 * lon / segs);
            float th1 = (float)(6.283185307179586 * (lon + 1) / segs);
            float ct0 = (float)Math.cos(th0);
            float st0 = (float)Math.sin(th0);
            float ct1 = (float)Math.cos(th1);
            float st1 = (float)Math.sin(th1);
            buf.addVertex(m, ox + r * cp0 * ct0, oy + r * sp0, oz + r * cp0 * st0).setColor(argb);
            buf.addVertex(m, ox + r * cp0 * ct1, oy + r * sp0, oz + r * cp0 * st1).setColor(argb);
            buf.addVertex(m, ox + r * cp1 * ct1, oy + r * sp1, oz + r * cp1 * st1).setColor(argb);
            buf.addVertex(m, ox + r * cp1 * ct0, oy + r * sp1, oz + r * cp1 * st0).setColor(argb);
         }
      }

      Layers.quads().draw(buf.buildOrThrow());
   }

   public static void drawHemisphere(PoseStack stack, double wx, double wy, double wz, double radius, int segs, Color color) {
      Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
      float ox = (float)(wx - cam.x());
      float oy = (float)(wy - cam.y());
      float oz = (float)(wz - cam.z());
      float r = (float)radius;
      int argb = color.getRGB();
      int rings = Math.max(1, segs / 2);
      float step = (float)(6.283185307179586 / segs);
      BufferBuilder buf = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      Matrix4f m = stack.last().pose();

      for (int lat = 0; lat < rings; lat++) {
         float phi0 = (float)(1.5707963267948966 * lat / rings);
         float phi1 = (float)(1.5707963267948966 * (lat + 1) / rings);
         float cp0 = (float)Math.cos(phi0);
         float sp0 = (float)Math.sin(phi0);
         float cp1 = (float)Math.cos(phi1);
         float sp1 = (float)Math.sin(phi1);

         for (int lon = 0; lon < segs; lon++) {
            float th0 = lon * step;
            float th1 = th0 + step;
            float ct0 = (float)Math.cos(th0);
            float st0 = (float)Math.sin(th0);
            float ct1 = (float)Math.cos(th1);
            float st1 = (float)Math.sin(th1);
            buf.addVertex(m, ox + r * cp0 * ct0, oy + r * sp0, oz + r * cp0 * st0).setColor(argb);
            buf.addVertex(m, ox + r * cp0 * ct1, oy + r * sp0, oz + r * cp0 * st1).setColor(argb);
            buf.addVertex(m, ox + r * cp1 * ct1, oy + r * sp1, oz + r * cp1 * st1).setColor(argb);
            buf.addVertex(m, ox + r * cp1 * ct0, oy + r * sp1, oz + r * cp1 * st0).setColor(argb);
         }
      }

      for (int i = 0; i < segs; i++) {
         float th0 = i * step;
         float th1 = th0 + step;
         float x0 = ox + r * (float)Math.cos(th0);
         float z0 = oz + r * (float)Math.sin(th0);
         float x1 = ox + r * (float)Math.cos(th1);
         float z1 = oz + r * (float)Math.sin(th1);
         buf.addVertex(m, ox, oy, oz).setColor(argb);
         buf.addVertex(m, x1, oy, z1).setColor(argb);
         buf.addVertex(m, x0, oy, z0).setColor(argb);
         buf.addVertex(m, ox, oy, oz).setColor(argb);
      }

      Layers.quads().draw(buf.buildOrThrow());
   }
}
