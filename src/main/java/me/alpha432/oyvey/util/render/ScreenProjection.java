package me.alpha432.oyvey.util.render;

import org.joml.Matrix4fc;
import org.joml.Vector4f;

/** Projects camera-relative coordinates with the actual world camera matrix. */
public final class ScreenProjection {
   private static final float NEAR = 0.05F;
   private static final int[][] EDGES = {
      {0,1}, {0,2}, {0,4}, {1,3}, {1,5}, {2,3},
      {2,6}, {3,7}, {4,5}, {4,6}, {5,7}, {6,7}
   };

   private ScreenProjection() { }

   public static float[] point(Matrix4fc matrix, double x, double y, double z, int width, int height) {
      float[] projected = pointUnclipped(matrix, x, y, z, width, height);
      if (projected == null || projected[0] < 0 || projected[0] > width || projected[1] < 0 || projected[1] > height) return null;
      return projected;
   }

   /** Keep off-screen endpoints when measuring a visible sprite's size. */
   public static float[] pointUnclipped(Matrix4fc matrix, double x, double y, double z, int width, int height) {
      Vector4f clip = matrix.transform(new Vector4f((float)x, (float)y, (float)z, 1));
      if (!clip.isFinite() || clip.w < NEAR) {
         return null;
      }
      return screen(clip, width, height);
   }

   public static float[] bounds(Matrix4fc matrix, double minX, double minY, double minZ,
      double maxX, double maxY, double maxZ, int width, int height) {
      Vector4f[] corners = new Vector4f[8];
      for (int i = 0; i < 8; i++) {
         corners[i] = matrix.transform(new Vector4f((float)((i & 1) == 0 ? minX : maxX),
            (float)((i & 2) == 0 ? minY : maxY), (float)((i & 4) == 0 ? minZ : maxZ), 1));
         if (!corners[i].isFinite()) return null;
      }
      float left = Float.POSITIVE_INFINITY, top = Float.POSITIVE_INFINITY;
      float right = Float.NEGATIVE_INFINITY, bottom = Float.NEGATIVE_INFINITY;
      for (int[] edge : EDGES) {
         Vector4f a = new Vector4f(corners[edge[0]]);
         Vector4f b = new Vector4f(corners[edge[1]]);
         boolean aVisible = a.w >= NEAR, bVisible = b.w >= NEAR;
         if (!aVisible && !bVisible) continue;
         if (aVisible != bVisible) {
            Vector4f intersection = new Vector4f(a).lerp(b, (NEAR - a.w) / (b.w - a.w));
            if (aVisible) b = intersection; else a = intersection;
         }
         float[] sa = screen(a, width, height), sb = screen(b, width, height);
         left = Math.min(left, Math.min(sa[0], sb[0]));
         top = Math.min(top, Math.min(sa[1], sb[1]));
         right = Math.max(right, Math.max(sa[0], sb[0]));
         bottom = Math.max(bottom, Math.max(sa[1], sb[1]));
      }
      if (left >= width || right <= 0 || top >= height || bottom <= 0 || left >= right || top >= bottom) {
         return null;
      }
      return new float[]{Math.max(0, left), Math.max(0, top), Math.min(width, right), Math.min(height, bottom)};
   }

   private static float[] screen(Vector4f clip, int width, int height) {
      return new float[]{(clip.x / clip.w + 1) * 0.5F * width, (1 - clip.y / clip.w) * 0.5F * height};
   }
}
