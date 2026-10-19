package me.alpha432.oyvey.util;

import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Legal hitbox-side aim and temporary legacy attack looks. */
public final class KnockbackDisplacement {
   private KnockbackDisplacement() { }

   public enum Direction {
      LEFT(-90.0F), RIGHT(90.0F);

      private final float offset;

      Direction(float offset) {
         this.offset = offset;
      }
   }

   public record Look(float yaw, float pitch) {
      public SilentRotationMath.Movement correctMovement(Input keys, float previousYaw) {
         return SilentRotationMath.correctMovement(keys, previousYaw, this.yaw);
      }
   }

   public static boolean shouldDisplace(int chance, int roll, boolean sprinting, boolean enchanted) {
      return (sprinting || enchanted) && roll >= 0 && roll < Math.clamp(chance, 0, 100);
   }

   /** Furthest usable side of the inset hitbox, including its nearer corner. */
   public static Vec3 safeAimPoint(AABB box, Vec3 eyes, Vec3 aim, Direction direction) {
      return safeAimPoints(box,eyes,aim,direction).getFirst();
   }

   public static List<Vec3> safeAimPoints(AABB box, Vec3 eyes, Vec3 aim, Direction direction) {
      Vec3 center=box.getCenter();
      double dx=center.x-eyes.x, dz=center.z-eyes.z;
      if (Math.hypot(dx,dz)<1.0E-6) return List.of(aim);
      double margin=Math.min(.04,Math.min(box.getXsize(),box.getZsize())*.15);
      double y=Math.clamp(aim.y,box.minY+.001,box.maxY-.001);
      double sign=direction==Direction.RIGHT?1:-1;
      var points=new ArrayList<Vec3>();
      for(double x:new double[]{box.minX+margin,center.x,box.maxX-margin}) {
         for(double z:new double[]{box.minZ+margin,center.z,box.maxZ-margin}) {
            Vec3 point=new Vec3(x,y,z);
            double dot=dx*(point.x-eyes.x)+dz*(point.z-eyes.z);
            double cross=dx*(point.z-eyes.z)-dz*(point.x-eyes.x);
            if (dot>0 && sign*cross>=-1.0E-8) points.add(point);
         }
      }
      points.sort(Comparator.comparingDouble((Vec3 point)->sign*Math.atan2(
         dx*(point.z-eyes.z)-dz*(point.x-eyes.x),dx*(point.x-eyes.x)+dz*(point.z-eyes.z))).reversed());
      return points.isEmpty()?List.of(aim):points;
   }

   public static void attack(Look original, Direction direction, Consumer<Look> apply, Runnable attack) {
      try {
         // Keep the continuous yaw representation, including across +/-180.
         apply.accept(new Look(original.yaw + direction.offset, original.pitch));
         attack.run();
      } finally {
         apply.accept(original);
      }
   }
}
