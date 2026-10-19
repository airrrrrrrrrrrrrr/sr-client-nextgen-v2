package me.alpha432.oyvey.features.modules.movement.scaffold;

import java.util.List;
import me.alpha432.oyvey.util.SilentRotationMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** One ordinary movement look prepares the next tick's click, then releases it. */
public final class OneTickBridgeRotation {
   public record Step(PlacementPlanner.Placement placement, SilentRotationMath.Rotation look,
                      boolean aiming, boolean returnAfterAttempt) { }
   private PlacementPlanner.Placement armed;

   public void reset() { armed = null; }

   /** Native physics has finished; refine pitch without changing its input yaw. */
   public SilentRotationMath.Rotation refine(Vec3 eyes, double reach, float yaw, float pitch,
      double sensitivity, PlacementPlanner.World world) {
      if (armed == null) return new SilentRotationMath.Rotation(yaw,pitch);
      var targets=List.of(armed.target());
      // Keep a valid prepared ray, including a pitch selected to expose a
      // second corner face. Only repair an actual miss after movement.
      var held=PlacementPlanner.findAlongLook(targets,eyes,reach,yaw,pitch,world);
      if (held!=null) {
         armed=held;
         return new SilentRotationMath.Rotation(yaw,pitch);
      }
      var center=PlacementPlanner.centerAim(targets,eyes,reach,yaw,pitch,sensitivity,world,armed,true);
      if (center!=null) {
         armed=center.placement();
         return center.rotation();
      }
      float corrected=PlacementPlanner.backwardPitch(targets,eyes,reach,yaw,pitch,sensitivity,world);
      var actual=PlacementPlanner.findAlongLook(targets,eyes,reach,yaw,corrected,world);
      if (actual!=null) {
         armed=actual;
         return new SilentRotationMath.Rotation(yaw,corrected);
      }
      return new SilentRotationMath.Rotation(yaw,pitch);
   }

   public Step update(List<BlockPos> targets, Vec3 eyes, Vec3 nextEyes, double reach, float sentYaw, float sentPitch,
      double sensitivity, SilentRotationMath.Rotation travel, boolean allowAim, PlacementPlanner.World world) {
      return update(targets,eyes,nextEyes,nextEyes.add(nextEyes.subtract(eyes)),reach,sentYaw,sentPitch,sensitivity,travel,allowAim,world);
   }

   public Step update(List<BlockPos> targets, Vec3 eyes, Vec3 nextEyes, Vec3 postEyes, double reach, float sentYaw, float sentPitch,
      double sensitivity, SilentRotationMath.Rotation travel, boolean allowAim, PlacementPlanner.World world) {
      return update(targets,eyes,nextEyes,postEyes,reach,sentYaw,sentPitch,sensitivity,travel,allowAim,world,180);
   }

   public Step update(List<BlockPos> targets, Vec3 eyes, Vec3 nextEyes, Vec3 postEyes, double reach, float sentYaw, float sentPitch,
      double sensitivity, SilentRotationMath.Rotation travel, boolean allowAim, PlacementPlanner.World world, float backwardOffset) {
      return update(targets,eyes,nextEyes,postEyes,reach,sentYaw,sentPitch,sensitivity,travel,allowAim,world,backwardOffset,world);
   }

   public Step update(List<BlockPos> targets, Vec3 eyes, Vec3 nextEyes, Vec3 postEyes, double reach, float sentYaw, float sentPitch,
      double sensitivity, SilentRotationMath.Rotation travel, boolean allowAim, PlacementPlanner.World world, float backwardOffset,
      PlacementPlanner.World aimWorld) {
      return update(targets,eyes,nextEyes,postEyes,reach,sentYaw,sentPitch,sensitivity,travel,allowAim,world,backwardOffset,aimWorld,true);
   }

   public Step update(List<BlockPos> targets, Vec3 eyes, Vec3 nextEyes, Vec3 postEyes, double reach, float sentYaw, float sentPitch,
      double sensitivity, SilentRotationMath.Rotation travel, boolean allowAim, PlacementPlanner.World world, float backwardOffset,
      PlacementPlanner.World aimWorld, boolean requirePostSupport) {
      if (armed != null) {
         var placement = PlacementPlanner.findAlongLook(List.of(armed.target()), eyes, reach, sentYaw, sentPitch, world);
         armed = null;
         // Return forward only for a real click. A miss immediately prepares
         // another aim instead of burning a complete tick looking into air.
         if (placement != null) return new Step(placement, new SilentRotationMath.Rotation(sentYaw, sentPitch), true, true);
      }
      if (allowAim) {
         // A backward pulse preserves the exact travel axis through native
         // WASD correction. Arbitrary face-center yaw rounds diagonal input
         // to a different direction and invalidates the predicted footprint.
         float backwardYaw = SilentRotationMath.quantizeRotation(sentYaw, sentPitch,
            travel.yaw()+backwardOffset, sentPitch, sensitivity).yaw();
         float backwardPitch = PlacementPlanner.backwardPitch(targets, nextEyes, reach,
            backwardYaw, sentPitch, sensitivity, aimWorld, true);
         var backward = PlacementPlanner.findAlongLook(targets,nextEyes,reach,backwardYaw,backwardPitch,aimWorld);
         if (backward != null) {
            float postPitch=PlacementPlanner.postPlacementPitch(backward,postEyes,reach,backwardYaw,backwardPitch,sensitivity);
            if (!requirePostSupport || PlacementPlanner.lookIntersectsClickedSupport(backward,postEyes,reach,backwardYaw,postPitch)) {
               armed=backward;
               return new Step(null,new SilentRotationMath.Rotation(backwardYaw,backwardPitch),true,false);
            }
         }
         // Face centers leave more margin than a ray grazing the top lip when
         // the next native tick adds ground acceleration or begins falling.
         var center = requirePostSupport
            ? PlacementPlanner.centerPulseAim(targets, nextEyes, postEyes, reach, sentYaw, sentPitch, sensitivity, aimWorld, null)
            : PlacementPlanner.centerAim(targets,nextEyes,reach,sentYaw,sentPitch,sensitivity,aimWorld,null,false);
         var next = center == null ? PlacementPlanner.clutchStep(targets, eyes, nextEyes, reach, sentYaw, sentPitch,
            sensitivity, false, aimWorld, null) : new PlacementPlanner.ClutchStep(center.placement(), center.rotation(), false);
         if (next != null) {
            armed = next.placement();
            return new Step(null, next.rotation(), true, false);
         }
      }
      return new Step(null, travel, false, false);
   }
}
