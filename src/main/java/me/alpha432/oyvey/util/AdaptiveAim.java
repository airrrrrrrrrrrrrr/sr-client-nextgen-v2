package me.alpha432.oyvey.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Finds a short visible ray into the hitbox, including ankle and side openings. */
public final class AdaptiveAim {
   private static final double[] HORIZONTAL = {0.005, 0.25, 0.5, 0.75, 0.995};
   private static final double[] VERTICAL = {0.005, 0.025, 0.075, 0.15, 0.3, 0.5, 0.7, 0.85, 0.925, 0.975, 0.995};

   private AdaptiveAim() { }

   public static Vec3 closestPoint(Vec3 eyes, AABB box) {
      double inset = Math.min(0.002, Math.min(box.getXsize(), Math.min(box.getYsize(), box.getZsize())) * 0.01);
      return new Vec3(Math.clamp(eyes.x, box.minX + inset, box.maxX - inset),
         Math.clamp(eyes.y, box.minY + inset, box.maxY - inset), Math.clamp(eyes.z, box.minZ + inset, box.maxZ - inset));
   }

   public static Optional<Vec3> bestPoint(Vec3 eyes, AABB box, Predicate<Vec3> visible) {
      Vec3 nearest = closestPoint(eyes, box);
      if (visible.test(nearest)) return Optional.of(nearest);
      List<Vec3> points = new ArrayList<>(180);
      double[] xs = coordinates(box.minX, box.maxX, eyes.x, HORIZONTAL);
      double[] ys = coordinates(box.minY, box.maxY, eyes.y, VERTICAL);
      double[] zs = coordinates(box.minZ, box.maxZ, eyes.z, HORIZONTAL);
      // Only faces turned toward the eye can be the first hit on a convex box.
      if (eyes.x < box.minX || eyes.x > box.maxX) {
         double x = nearest.x;
         for (double y : ys) for (double z : zs) points.add(new Vec3(x, y, z));
      }
      if (eyes.z < box.minZ || eyes.z > box.maxZ) {
         for (double x : xs) for (double y : ys) points.add(new Vec3(x, y, nearest.z));
      }
      if (eyes.y < box.minY || eyes.y > box.maxY) {
         for (double x : xs) for (double z : zs) points.add(new Vec3(x, nearest.y, z));
      }
      points.sort(Comparator.comparingDouble(eyes::distanceToSqr));
      for (Vec3 candidate : points) {
         if (!visible.test(candidate)) continue;
         // Refine toward the closest point without leaving the visible opening.
         Vec3 best = candidate;
         double low = 0, high = 1;
         for (int i = 0; i < 8; i++) {
            double mid = (low + high) * 0.5;
            Vec3 probe = candidate.lerp(nearest, mid);
            if (visible.test(probe)) { best = probe; low = mid; } else high = mid;
         }
         return Optional.of(best);
      }
      return Optional.empty();
   }

   private static double[] coordinates(double min, double max, double eye, double[] fractions) {
      double[] result = new double[fractions.length + 1];
      double inset = Math.min(0.002, (max - min) * 0.01);
      result[0] = Math.clamp(eye, min + inset, max - inset);
      for (int i = 0; i < fractions.length; i++) result[i + 1] = min + (max - min) * fractions[i];
      return result;
   }
}
