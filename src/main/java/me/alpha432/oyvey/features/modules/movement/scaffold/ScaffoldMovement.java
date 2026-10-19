package me.alpha432.oyvey.features.modules.movement.scaffold;

import java.util.function.Predicate;
import java.util.ArrayList;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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

   /** Forecast only: never writes player motion, ground flags or input. */
   public static Vec3 collisionStep(AABB box, Vec3 requested, Iterable<VoxelShape> collisions) {
      var shapes = new ArrayList<VoxelShape>();
      collisions.forEach(shapes::add);
      Vec3 resolved = Vec3.ZERO;
      for (Direction.Axis axis : Direction.axisStepOrder(requested)) {
         if (requested.get(axis) != 0) resolved = resolved.with(axis,
            Shapes.collide(axis, box.move(resolved), shapes, requested.get(axis)));
      }
      return resolved;
   }

   public static boolean diagonalBridge(Input keys, float cameraYaw) {
      boolean forward = keys.forward() != keys.backward();
      boolean sideways = keys.left() != keys.right();
      if (!forward && !sideways) return false;
      if (forward && sideways) return true;
      // Include a single held direction aimed along a diagonal of the block
      // grid, where Center has the same corner-face problem as W+A / W+D.
      float travelYaw = TellyBridge.forwardYaw(keys, cameraYaw);
      return (Math.floorMod(Math.round(travelYaw / 45.0F), 2)) != 0;
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
