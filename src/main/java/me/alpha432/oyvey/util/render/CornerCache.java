package me.alpha432.oyvey.util.render;

import java.util.Arrays;
import java.util.HashMap;

public final class CornerCache {
   private static final HashMap<Long, CornerCache.CornerData> CORNER_CACHE = new HashMap<>();
   private static final HashMap<Long, float[]> COV_CACHE = new HashMap<>();

   private CornerCache() {
   }

   public static CornerCache.CornerData get(int r, boolean posX, boolean posY) {
      long key = (long)r << 2 | (posX ? 2L : 0L) | (posY ? 1L : 0L);
      return CORNER_CACHE.computeIfAbsent(key, k -> new CornerCache.CornerData((int)(k >> 2), (k & 2L) != 0L, (k & 1L) != 0L));
   }

   public static float[] getCov(int r, boolean posX, boolean posY) {
      long key = (long)r << 2 | (posX ? 2L : 0L) | (posY ? 1L : 0L);
      return COV_CACHE.computeIfAbsent(key, k -> {
         int rad = (int)(k >> 2);
         boolean px_ = (k & 2L) != 0L;
         boolean py_ = (k & 1L) != 0L;
         CornerCache.CornerData d = get(rad, px_, py_);
         float[] cov = new float[rad * rad];

         for (int py = 0; py < rad; py++) {
            if (d.inStart[py] >= 0) {
               for (int px = d.inStart[py]; px < d.inEnd[py]; px++) {
                  cov[px * rad + py] = 1.0F;
               }
            }
         }

         for (int i = 0; i < d.edgeCount; i++) {
            cov[d.edgePx[i] * rad + d.edgePy[i]] = d.edgeCov[i];
         }

         return cov;
      });
   }

   public static final class CornerData {
      public final int[] inStart;
      public final int[] inEnd;
      public final int[] outStart;
      public final int[] outEnd;
      public final int edgeCount;
      public final int[] edgePx;
      public final int[] edgePy;
      public final float[] edgeCov;

      CornerData(int rad, boolean posX, boolean posY) {
         this.inStart = new int[rad];
         Arrays.fill(this.inStart, -1);
         this.inEnd = new int[rad];
         this.outStart = new int[rad];
         Arrays.fill(this.outStart, -1);
         this.outEnd = new int[rad];
         int[] tmpPx = new int[rad * 8];
         int[] tmpPy = new int[rad * 8];
         float[] tmpC = new float[rad * 8];
         int ec = 0;
         float rr = rad;
         float rsq = rr * rr;

         for (int py = 0; py < rad; py++) {
            float ddy = posY ? py + 0.5F : py + 0.5F - rr;

            for (int px = 0; px < rad; px++) {
               float ddx = posX ? px + 0.5F : px + 0.5F - rr;
               float dist = (float)Math.sqrt(ddx * ddx + ddy * ddy);
               float cov;
               if (dist <= rr - 0.7071F) {
                  cov = 1.0F;
               } else if (dist >= rr + 0.7071F) {
                  cov = 0.0F;
               } else {
                  int inside = 0;

                  for (int sx = 0; sx < 4; sx++) {
                     float sdx = posX ? px + (sx + 0.5F) / 4.0F : px + (sx + 0.5F) / 4.0F - rr;

                     for (int sy = 0; sy < 4; sy++) {
                        float sdy = posY ? py + (sy + 0.5F) / 4.0F : py + (sy + 0.5F) / 4.0F - rr;
                        if (sdx * sdx + sdy * sdy <= rsq) {
                           inside++;
                        }
                     }
                  }

                  cov = inside / 16.0F;
               }

               if (cov == 1.0F) {
                  if (this.inStart[py] < 0) {
                     this.inStart[py] = px;
                  }

                  this.inEnd[py] = px + 1;
               } else if (cov == 0.0F) {
                  if (this.outStart[py] < 0) {
                     this.outStart[py] = px;
                  }

                  this.outEnd[py] = px + 1;
               } else {
                  if (ec == tmpPx.length) {
                     tmpPx = Arrays.copyOf(tmpPx, ec * 2);
                     tmpPy = Arrays.copyOf(tmpPy, ec * 2);
                     tmpC = Arrays.copyOf(tmpC, ec * 2);
                  }

                  tmpPx[ec] = px;
                  tmpPy[ec] = py;
                  tmpC[ec] = cov;
                  ec++;
               }
            }
         }

         this.edgeCount = ec;
         this.edgePx = Arrays.copyOf(tmpPx, ec);
         this.edgePy = Arrays.copyOf(tmpPy, ec);
         this.edgeCov = Arrays.copyOf(tmpC, ec);
      }
   }
}
