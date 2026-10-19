package me.alpha432.oyvey.features.modules.movement.scaffold;

import me.alpha432.oyvey.util.SilentRotationMath;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;

/** Jump/turn decisions only. Vanilla still performs movement and jumping. */
public final class TellyBridge {
   // A diagonal ray aimed exactly through a grid corner exposes almost none
   // of the next support face. A small, stable bias keeps that face aimable
   // across mouse sensitivities while retaining the backward catch look.
   public static final float BACKWARD_OFFSET = 185.0F;
   public static final int FORWARD_AIR_TICKS = 2;
   public enum Phase { ALIGN_BACKWARD, TURN_FORWARD, FORWARD, CLUTCH, RECOVER_FORWARD }

   public record Step(Phase phase, boolean lookForward, boolean jump, int layer) { }

   private Phase phase = Phase.ALIGN_BACKWARD;
   private boolean initialized, wasAirborne;
   private int airTicks, takeoffLayer;

   public void reset() {
      this.phase = Phase.ALIGN_BACKWARD;
      this.initialized = false;
      this.wasAirborne = false;
      this.airTicks = 0;
   }

   public Step update(boolean grounded, double motionY, int layer, boolean backwardReady, boolean forwardReady,
      boolean canJump, boolean climb) {
      return update(grounded, motionY, layer, backwardReady, forwardReady, canJump, climb, false);
   }

   public Step update(boolean grounded, double motionY, int layer, boolean backwardReady, boolean forwardReady,
      boolean canJump, boolean climb, boolean landingReady) {
      if (!this.initialized) {
         this.initialized = true;
         this.takeoffLayer = layer;
         this.phase = grounded ? Phase.ALIGN_BACKWARD : Phase.CLUTCH;
      }
      if (grounded) {
         if (this.wasAirborne) {
            this.phase = forwardReady && canJump ? Phase.FORWARD : Phase.ALIGN_BACKWARD;
            this.airTicks = 0;
            this.takeoffLayer = layer;
         }
         if (!canJump) this.phase = Phase.ALIGN_BACKWARD;
         if (this.phase == Phase.ALIGN_BACKWARD && backwardReady && canJump) this.phase = Phase.TURN_FORWARD;
         if (this.phase == Phase.TURN_FORWARD && forwardReady) this.phase = Phase.FORWARD;
      } else {
         this.airTicks++;
         if (this.phase == Phase.TURN_FORWARD) this.phase = Phase.FORWARD;
         // Keep the original forward sprint part of the jump before turning
         // back to place. Walking off an edge still catches immediately.
         // Upward bridging needs the full rising arc to build the next step.
         if (this.phase == Phase.FORWARD && (climb || this.airTicks > FORWARD_AIR_TICKS || motionY <= 0)) this.phase = Phase.CLUTCH;
         if (this.phase == Phase.ALIGN_BACKWARD) this.phase = Phase.CLUTCH;
         if (this.phase == Phase.RECOVER_FORWARD && !landingReady) this.phase = Phase.CLUTCH;
         if (this.phase == Phase.CLUTCH && landingReady) this.recoverForward(layer, climb);
      }
      this.wasAirborne = !grounded;
      boolean forward = this.phase == Phase.TURN_FORWARD || this.phase == Phase.FORWARD || this.phase == Phase.RECOVER_FORWARD;
      // Finish the turn before native physics, retaining the held movement.
      return new Step(this.phase, forward, grounded && forward && canJump,
         this.placementLayer(layer, climb));
   }

   /** Called before physics once the catch supplies the forward landing. */
   public Step recoverForward(int layer, boolean climb) {
      // A lower platform must not cancel an upward catch before it gains a step.
      if (climb && layer <= this.takeoffLayer) return null;
      this.phase = Phase.RECOVER_FORWARD;
      return new Step(this.phase, true, false, this.placementLayer(layer, climb));
   }

   public int placementLayer(int currentLayer, boolean climb) {
      return climb || !this.initialized ? currentLayer : Math.min(this.takeoffLayer, currentLayer);
   }

   public static boolean moving(Input keys) {
      return keys.forward() != keys.backward() || keys.left() != keys.right();
   }

   public static float forwardYaw(Input keys, float cameraYaw) {
      int forward = (keys.forward() ? 1 : 0) - (keys.backward() ? 1 : 0);
      int sideways = (keys.left() ? 1 : 0) - (keys.right() ? 1 : 0);
      return cameraYaw - (float)Math.toDegrees(Math.atan2(sideways, forward));
   }

   public static SilentRotationMath.Rotation turn(float sentYaw, float sentPitch, float desiredYaw, float desiredPitch,
      double sensitivity) {
      return SilentRotationMath.quantizeRotation(sentYaw, sentPitch,
         // Finish the yaw before native input and jumping run. A half-turn
         // spread over two physics ticks jumps sideways and loses sprint.
         sentYaw + Mth.wrapDegrees(desiredYaw - sentYaw),
         // The catch must have its placement pitch on its first transmitted
         // look. A 15 -> 55 -> 82 ramp consumed the usable support window.
         desiredPitch, sensitivity);
   }
}
