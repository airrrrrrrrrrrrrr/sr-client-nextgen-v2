package me.alpha432.oyvey.util.render;

import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public final class WorldRenderer {
   private WorldRenderer() {
   }

   @Nullable
   public static float[] toScreen(double worldX, double worldY, double worldZ) {
      Camera cam = Util.mc.gameRenderer.getMainCamera();
      double dx = worldX - cam.position().x;
      double dy = worldY - cam.position().y;
      double dz = worldZ - cam.position().z;
      double yawRad = Math.toRadians(cam.yRot() + 180.0);
      double sinY = Math.sin(yawRad);
      double cosY = Math.cos(yawRad);
      double rx = dx * cosY - dz * sinY;
      double rz = dx * sinY + dz * cosY;
      double pitchRad = Math.toRadians(cam.xRot());
      double sinP = Math.sin(pitchRad);
      double cosP = Math.cos(pitchRad);
      double ry = dy * cosP + rz * sinP;
      double fwd = -dy * sinP + rz * cosP;
      if (fwd < 0.1) {
         return null;
      } else {
         int w = Util.mc.getWindow().getGuiScaledWidth();
         int h = Util.mc.getWindow().getGuiScaledHeight();
         double fov = Math.toRadians(((Integer)Util.mc.options.fov().get()).intValue());
         double tan = Math.tan(fov / 2.0);
         double asp = (double)w / h;
         double ndcX = rx / (fwd * tan * asp);
         double ndcY = ry / (fwd * tan);
         if (!(ndcX < -1.1) && !(ndcX > 1.1) && !(ndcY < -1.1) && !(ndcY > 1.1)) {
            float sx = (float)((ndcX + 1.0) / 2.0 * w);
            float sy = (float)((1.0 - ndcY) / 2.0 * h);
            return new float[]{sx, sy};
         } else {
            return null;
         }
      }
   }

   @Nullable
   public static float[] aabbToScreen(AABB box) {
      double[][] v = new double[][]{
         viewSpace(box.minX, box.minY, box.minZ),
         viewSpace(box.maxX, box.minY, box.minZ),
         viewSpace(box.minX, box.maxY, box.minZ),
         viewSpace(box.maxX, box.maxY, box.minZ),
         viewSpace(box.minX, box.minY, box.maxZ),
         viewSpace(box.maxX, box.minY, box.maxZ),
         viewSpace(box.minX, box.maxY, box.maxZ),
         viewSpace(box.maxX, box.maxY, box.maxZ)
      };
      int[][] edges = new int[][]{{0, 1}, {1, 5}, {5, 4}, {4, 0}, {2, 3}, {3, 7}, {7, 6}, {6, 2}, {0, 2}, {1, 3}, {4, 6}, {5, 7}};
      double near = 0.05;
      float minX = 3.4028235E38F;
      float minY = 3.4028235E38F;
      float maxX = -3.4028235E38F;
      float maxY = -3.4028235E38F;
      boolean any = false;

      for (int[] e : edges) {
         double ax = v[e[0]][0];
         double ay = v[e[0]][1];
         double af = v[e[0]][2];
         double bx = v[e[1]][0];
         double by = v[e[1]][1];
         double bf = v[e[1]][2];
         boolean ain = af >= near;
         boolean bin = bf >= near;
         if (ain || bin) {
            if (ain != bin) {
               double t = (near - af) / (bf - af);
               double ix = ax + (bx - ax) * t;
               double iy = ay + (by - ay) * t;
               if (ain) {
                  bx = ix;
                  by = iy;
                  bf = near;
               } else {
                  ax = ix;
                  ay = iy;
                  af = near;
               }
            }

            float[] sa = projectView(ax, ay, af);
            float[] sb = projectView(bx, by, bf);
            minX = Math.min(minX, Math.min(sa[0], sb[0]));
            minY = Math.min(minY, Math.min(sa[1], sb[1]));
            maxX = Math.max(maxX, Math.max(sa[0], sb[0]));
            maxY = Math.max(maxY, Math.max(sa[1], sb[1]));
            any = true;
         }
      }

      if (!any) {
         return null;
      }

      minX = Mth.clamp(minX, -30000.0F, 30000.0F);
      minY = Mth.clamp(minY, -30000.0F, 30000.0F);
      maxX = Mth.clamp(maxX, -30000.0F, 30000.0F);
      maxY = Mth.clamp(maxY, -30000.0F, 30000.0F);
      return new float[]{minX, minY, maxX, maxY};
   }

   private static double[] viewSpace(double worldX, double worldY, double worldZ) {
      Camera cam = Util.mc.gameRenderer.getMainCamera();
      double dx = worldX - cam.position().x;
      double dy = worldY - cam.position().y;
      double dz = worldZ - cam.position().z;
      double yawRad = Math.toRadians(cam.yRot() + 180.0);
      double sinY = Math.sin(yawRad);
      double cosY = Math.cos(yawRad);
      double rx = dx * cosY - dz * sinY;
      double rz = dx * sinY + dz * cosY;
      double pitchRad = Math.toRadians(cam.xRot());
      double sinP = Math.sin(pitchRad);
      double cosP = Math.cos(pitchRad);
      double ry = dy * cosP + rz * sinP;
      double fwd = -dy * sinP + rz * cosP;
      return new double[]{rx, ry, fwd};
   }

   private static float[] projectView(double rx, double ry, double fwd) {
      int w = Util.mc.getWindow().getGuiScaledWidth();
      int h = Util.mc.getWindow().getGuiScaledHeight();
      double fov = Math.toRadians(((Integer)Util.mc.options.fov().get()).intValue());
      double tan = Math.tan(fov / 2.0);
      double asp = (double)w / h;
      double ndcX = rx / (fwd * tan * asp);
      double ndcY = ry / (fwd * tan);
      float sx = (float)((ndcX + 1.0) / 2.0 * w);
      float sy = (float)((1.0 - ndcY) / 2.0 * h);
      return new float[]{sx, sy};
   }
}
