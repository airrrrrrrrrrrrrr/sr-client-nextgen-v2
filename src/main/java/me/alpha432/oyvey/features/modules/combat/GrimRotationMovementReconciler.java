package me.alpha432.oyvey.features.modules.combat;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;

/**
 * Pure math shared by the silent-rotation and movement-input pipelines.
 *
 * <p>The movement result intentionally contains only legal keyboard states.
 * Minecraft 26.1.2 sends those states in {@code ServerboundPlayerInputPacket},
 * and Grim reconstructs movement from the same finite set. Applying movement
 * speed, block friction, or the 0.91 air-drag term here would apply physics
 * twice: vanilla applies those terms later in the movement tick and Grim does
 * the same independently in its prediction engine.</p>
 */
final class GrimRotationMovementReconciler {
   private static final float INPUT_EPSILON = 1.0E-6F;
   private static final float SCORE_EPSILON = 1.0E-6F;

   private GrimRotationMovementReconciler() {
   }

   /**
    * Quantizes a desired look direction to the exact angular increment emitted
    * by vanilla's mouse handler for the configured sensitivity.
    *
    * <p>Yaw is kept continuous around {@code baseYaw}; it is not reduced modulo
    * 360. That avoids the artificial +/-360 jump detected by AimModulo360.</p>
    */
   static Rotation quantizeRotation(float baseYaw, float basePitch, float targetYaw, float targetPitch, double sensitivity) {
      float step = mouseStep(sensitivity);
      if (!Float.isFinite(step) || step <= INPUT_EPSILON) {
         return new Rotation(baseYaw + Mth.wrapDegrees(targetYaw - baseYaw), Mth.clamp(targetPitch, -90.0F, 90.0F));
      }

      float yawDelta = Mth.wrapDegrees(targetYaw - baseYaw);
      float pitchDelta = targetPitch - basePitch;
      float quantizedYawDelta = Math.round(yawDelta / step) * step;
      float quantizedPitchDelta = Math.round(pitchDelta / step) * step;
      return new Rotation(
         baseYaw + quantizedYawDelta,
         Mth.clamp(basePitch + quantizedPitchDelta, -90.0F, 90.0F)
      );
   }

   /** Vanilla mouse rotation increment: {@code (s * 0.6 + 0.2)^3 * 8 * 0.15}. */
   static float mouseStep(double sensitivity) {
      double delta = sensitivity * 0.6 + 0.2;
      return (float)(delta * delta * delta * 8.0 * 0.15);
   }

   /**
    * Finds the legal W/S/A/D tuple whose world-space direction under the
    * silent/server yaw most closely matches the user's camera-space direction.
    *
    * <p>Modern clients send seven booleans, not an arbitrary analog vector.
    * Restricting the search to {-1, 0, 1} for forward and sideways makes the
    * local acceleration exactly reconstructible from the packet. Diagonal
    * inputs are normalized exactly as {@code KeyboardInput.tick()} does.</p>
    */
   static Movement correctMovement(Input keys, float cameraYaw, float serverYaw) {
      int originalForward = bool(keys.forward()) - bool(keys.backward());
      int originalSideways = bool(keys.left()) - bool(keys.right());
      if (originalForward == 0 && originalSideways == 0) {
         return Movement.ZERO;
      }

      WorldDirection desired = toWorld(normalize(originalSideways, originalForward), cameraYaw);
      Movement best = Movement.ZERO;
      float bestError = Float.POSITIVE_INFINITY;
      int bestChanges = Integer.MAX_VALUE;

      for (int forward = -1; forward <= 1; forward++) {
         for (int sideways = -1; sideways <= 1; sideways++) {
            if (forward == 0 && sideways == 0) {
               continue;
            }

            Movement candidate = normalize(sideways, forward);
            WorldDirection worldCandidate = toWorld(candidate, serverYaw);
            float errorX = worldCandidate.x() - desired.x();
            float errorZ = worldCandidate.z() - desired.z();
            float error = errorX * errorX + errorZ * errorZ;
            int changes = Math.abs(forward - originalForward) + Math.abs(sideways - originalSideways);

            // The error selects the nearest world direction. The stable
            // tie-break keeps the original keys when two directions straddle
            // the desired angle by the same amount.
            if (error < bestError - SCORE_EPSILON
               || Math.abs(error - bestError) <= SCORE_EPSILON && changes < bestChanges) {
               best = candidate;
               bestError = error;
               bestChanges = changes;
            }
         }
      }

      return best;
   }

   private static Movement normalize(int sideways, int forward) {
      float length = Mth.sqrt((float)(sideways * sideways + forward * forward));
      return length <= INPUT_EPSILON
         ? Movement.ZERO
         : new Movement(sideways, forward, sideways / length, forward / length);
   }

   /** Matches vanilla/Grim's local-input-to-world-axis rotation. */
   private static WorldDirection toWorld(Movement movement, float yaw) {
      float radians = yaw * ((float)Math.PI / 180.0F);
      float sin = Mth.sin(radians);
      float cos = Mth.cos(radians);
      return new WorldDirection(
         movement.normalizedSideways() * cos - movement.normalizedForward() * sin,
         movement.normalizedForward() * cos + movement.normalizedSideways() * sin
      );
   }

   private static int bool(boolean value) {
      return value ? 1 : 0;
   }

   record Rotation(float yaw, float pitch) {
   }

   record Movement(int sideways, int forward, float normalizedSideways, float normalizedForward) {
      private static final Movement ZERO = new Movement(0, 0, 0.0F, 0.0F);
   }

   private record WorldDirection(float x, float z) {
   }
}
