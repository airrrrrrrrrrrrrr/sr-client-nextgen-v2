package me.alpha432.oyvey.util;

import java.util.ArrayDeque;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PositionHistory {
   private static final int MAX_SNAPSHOTS = 30;
   private static final long EXPIRE_MS = 1250L;
   private static final double TELEPORT_DIST_SQ = 100.0;
   private final ArrayDeque<PositionHistory.EntityPosition> ring = new ArrayDeque<>(30);

   public void add(PositionHistory.EntityPosition snap) {
      if (!this.ring.isEmpty()) {
         Vec3 last = this.ring.peekLast().pos();
         double dx = snap.pos().x - last.x;
         double dy = snap.pos().y - last.y;
         double dz = snap.pos().z - last.z;
         if (dx * dx + dy * dy + dz * dz > 100.0) {
            this.ring.clear();
         }
      }

      this.ring.addLast(snap);
      long cutoff = snap.timestampMs() - 1250L;

      while (this.ring.size() > 2 && this.ring.peekFirst().timestampMs() < cutoff) {
         this.ring.pollFirst();
      }

      while (this.ring.size() > 30) {
         this.ring.pollFirst();
      }
   }

   public void clear() {
      this.ring.clear();
   }

   public boolean isEmpty() {
      return this.ring.isEmpty();
   }

   public int size() {
      return this.ring.size();
   }

   public PositionHistory.EntityPosition getInterpolated(long targetMs) {
      if (this.ring.isEmpty()) {
         return null;
      }

      if (this.ring.size() == 1) {
         return this.ring.peekFirst();
      }

      PositionHistory.EntityPosition before = null;
      PositionHistory.EntityPosition after = null;

      for (PositionHistory.EntityPosition s : this.ring) {
         if (s.timestampMs() <= targetMs) {
            before = s;
         } else if (after == null) {
            after = s;
         }
      }

      if (before == null) {
         return this.ring.peekFirst();
      }

      if (after == null) {
         return before;
      }

      long dt = after.timestampMs() - before.timestampMs();
      if (dt == 0L) {
         return before;
      }

      double t = Math.max(0.0, Math.min(1.0, (double)(targetMs - before.timestampMs()) / dt));
      return before.lerp(after, t);
   }

   public PositionHistory.EntityPosition peekNewest() {
      return this.ring.peekLast();
   }

   public PositionHistory.EntityPosition peekOldest() {
      return this.ring.peekFirst();
   }

   public record EntityPosition(long timestampMs, Vec3 pos, AABB box) {
      public PositionHistory.EntityPosition lerp(PositionHistory.EntityPosition other, double t) {
         Vec3 p = this.pos.lerp(other.pos, t);
         AABB b = new AABB(
            lerpD(this.box.minX, other.box.minX, t),
            lerpD(this.box.minY, other.box.minY, t),
            lerpD(this.box.minZ, other.box.minZ, t),
            lerpD(this.box.maxX, other.box.maxX, t),
            lerpD(this.box.maxY, other.box.maxY, t),
            lerpD(this.box.maxZ, other.box.maxZ, t)
         );
         return new PositionHistory.EntityPosition(this.timestampMs, p, b);
      }

      private static double lerpD(double a, double b, double t) {
         return a + (b - a) * t;
      }
   }
}
