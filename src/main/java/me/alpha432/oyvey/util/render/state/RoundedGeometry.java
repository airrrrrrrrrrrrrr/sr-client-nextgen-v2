package me.alpha432.oyvey.util.render.state;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;

/** Geometry shared by the screen-space antialiased GUI shapes. */
final class RoundedGeometry {
   private RoundedGeometry() { }

   static float fringe(Matrix3x2f pose, double pixelsPerGuiUnit) {
      // The smallest singular value also handles rotated and sheared GUI poses.
      double sum = pose.m00() * pose.m00() + pose.m01() * pose.m01()
         + pose.m10() * pose.m10() + pose.m11() * pose.m11();
      double determinant = pose.m00() * pose.m11() - pose.m01() * pose.m10();
      double largest = Math.sqrt((sum + Math.sqrt(Math.max(0, sum * sum - 4 * determinant * determinant))) / 2);
      double smallest = largest == 0 ? 0 : Math.abs(determinant) / largest;
      return (float)(1.0 / Math.max(0.0001, smallest * pixelsPerGuiUnit));
   }

   static int parameter(float value, float halfShort) {
      // Separate signed-short channels preserve fractional radii and strokes.
      // Relative encoding supports any rectangle size with subpixel precision.
      return Math.round(Math.clamp(value / Math.max(halfShort, 0.0001F), 0, 1) * 32767);
   }

   static ScreenRectangle bounds(Matrix3x2f pose, float x, float y, float width, float height,
                                  float fringe, ScreenRectangle scissor) {
      int left = (int)Math.floor(x - fringe), top = (int)Math.floor(y - fringe);
      int right = (int)Math.ceil(x + width + fringe), bottom = (int)Math.ceil(y + height + fringe);
      var raw = new ScreenRectangle(left, top, Math.max(1, right - left), Math.max(1, bottom - top)).transformMaxBounds(pose);
      return scissor == null ? raw : scissor.intersection(raw);
   }
}
