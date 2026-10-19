package me.alpha432.oyvey.util;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Interior hitbox and head samples used by combat rotation planning. */
public final class CombatAim {
   private CombatAim() { }

   /** Retarget recorded hand error inside the current hitbox instead of missing forever. */
   public static float[] recordedOffset(Vec3 eyes, AABB box, float yaw, float pitch, float yawError, float pitchError) {
      double distance = eyes.distanceTo(box.getCenter()) + box.getSize() * 3 + 1;
      AABB interior = box.deflate(Math.min(0.03, Math.min(box.getXsize(), Math.min(box.getYsize(), box.getZsize())) * 0.1));
      // A rod's ballistic aim can deliberately point above the player; do not retarget it.
      if (!hits(eyes, interior, distance, yaw, pitch)) return new float[]{yaw, pitch};
      float desiredPitch = Math.clamp(pitch + pitchError, -90, 90);
      float dy = net.minecraft.util.Mth.wrapDegrees(yawError), dp = desiredPitch - pitch;
      if (hits(eyes, interior, distance, yaw + dy, desiredPitch)) return new float[]{yaw + dy, desiredPitch};
      float low = 0, high = 1;
      for (int i = 0; i < 16; i++) {
         float mid = (low + high) / 2;
         if (hits(eyes, interior, distance, yaw + dy * mid, pitch + dp * mid)) low = mid;
         else high = mid;
      }
      // Leave a little margin for the subsequent mouse-sensitivity quantization.
      low *= 0.85F;
      return new float[]{yaw + dy * low, pitch + dp * low};
   }

   private static boolean hits(Vec3 eyes, AABB box, double distance, float yaw, float pitch) {
      return box.contains(eyes) || box.clip(eyes, eyes.add(Vec3.directionFromRotation(pitch, yaw).scale(distance))).isPresent();
   }

   public static Vec3 headCenter(AABB box, double eyeY) {
      double margin = Math.min(0.01, box.getYsize() * 0.05);
      double y = Math.max(box.minY + margin, Math.min(eyeY, box.maxY - margin));
      return new Vec3((box.minX + box.maxX) * 0.5, y, (box.minZ + box.maxZ) * 0.5);
   }

   /** Favor eye-level head aim; visibility sampling never falls back to the torso. */
   public static Optional<Vec3> visibleHeadPoint(AABB box, double eyeY, Predicate<Vec3> visible) {
      Vec3 head = headCenter(box, eyeY);
      if (visible.test(head)) return Optional.of(head);
      double offset = Math.min(0.12, box.getYsize() * 0.1);
      for (double height : new double[]{head.y, head.y + offset, head.y - offset}) {
         for (double x : new double[]{0.5, 0.15, 0.85}) {
            for (double z : new double[]{0.5, 0.15, 0.85}) {
               Vec3 point = headCenter(box, height);
               point = new Vec3(box.minX + box.getXsize() * x, point.y, box.minZ + box.getZsize() * z);
               if (visible.test(point)) return Optional.of(point);
            }
         }
      }
      return Optional.empty();
   }

   public static Optional<Vec3> visiblePoint(AABB box, Predicate<Vec3> visible) {
      Vec3 center = box.getCenter();
      if (visible.test(center)) return Optional.of(center);
      for (double height : new double[]{0.8, 0.2, 0.95, 0.05}) {
         Vec3 point = point(box, 0.5, height, 0.5);
         if (visible.test(point)) return Optional.of(point);
      }
      for (double height : new double[]{0.5, 0.8, 0.2, 0.95}) {
         for (double x : new double[]{0.5, 0.04, 0.96}) {
            for (double z : new double[]{0.5, 0.04, 0.96}) {
               Vec3 point = point(box, x, height, z);
               if (visible.test(point)) return Optional.of(point);
            }
         }
      }
      return Optional.empty();
   }

   private static Vec3 point(AABB box, double x, double y, double z) {
      return new Vec3(box.minX + (box.maxX - box.minX) * x,
         box.minY + (box.maxY - box.minY) * y, box.minZ + (box.maxZ - box.minZ) * z);
   }
}
