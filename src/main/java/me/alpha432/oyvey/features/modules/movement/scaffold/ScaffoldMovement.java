package me.alpha432.oyvey.features.modules.movement.scaffold;

import java.util.function.Predicate;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/** Input decisions only; acceleration, slowdown and edge clipping stay vanilla. */
public final class ScaffoldMovement {
   private ScaffoldMovement() { }

   public static boolean forwardSprint(Input keys) {
      return keys.forward() && !keys.backward() && !keys.shift();
   }

   public static boolean needsEdgeSneak(AABB box, Vec3 velocity, Vec2 input, float yaw,
      double acceleration, double stepDown, Predicate<AABB> empty) {
      float radians = yaw * ((float)Math.PI / 180);
      double sin = Mth.sin(radians), cos = Mth.cos(radians);
      // Look ahead using existing momentum plus a conservative sprint input.
      // This is a support probe, never an assignment to the player's velocity.
      double x = velocity.x + (input.x * cos - input.y * sin) * acceleration;
      double z = velocity.z + (input.y * cos + input.x * sin) * acceleration;
      AABB feet = new AABB(box.minX + 1.0E-7, box.minY - stepDown - 1.0E-7,
         box.minZ + 1.0E-7, box.maxX - 1.0E-7, box.minY, box.maxZ - 1.0E-7);
      return empty.test(feet.move(x, 0, z))
         || x != 0 && empty.test(feet.move(x, 0, 0))
         || z != 0 && empty.test(feet.move(0, 0, z));
   }
}
