package me.alpha432.oyvey.features.modules.movement.scaffold;

/** Rising Telly cycle: forward sprint takeoff, backward catch, forward recovery. */
public final class Telly2Bridge {
   private TellyBridge.Phase phase = TellyBridge.Phase.TURN_FORWARD;
   private boolean initialized, airborne;
   private int takeoffLayer;

   public void reset() { initialized = airborne = false; phase = TellyBridge.Phase.TURN_FORWARD; }

   public TellyBridge.Step update(boolean grounded, double motionY, int layer, boolean canJump,
      boolean landingReady) {
      if (!initialized) {
         initialized = true;
         takeoffLayer = layer;
         phase = grounded ? TellyBridge.Phase.TURN_FORWARD : TellyBridge.Phase.CLUTCH;
      }
      if (grounded) {
         takeoffLayer = layer;
         phase = canJump ? TellyBridge.Phase.TURN_FORWARD : TellyBridge.Phase.CLUTCH;
      } else {
         if (!airborne && phase == TellyBridge.Phase.TURN_FORWARD) phase = TellyBridge.Phase.FORWARD;
         // Use the rising arc for the raised catch immediately after takeoff.
         if (phase == TellyBridge.Phase.FORWARD) phase = TellyBridge.Phase.CLUTCH;
         // Recover only after native collision geometry predicts a landing.
         // A turn or lost support immediately reopens the catch phase.
         if (phase == TellyBridge.Phase.RECOVER_FORWARD && !landingReady) phase = TellyBridge.Phase.CLUTCH;
         else if (phase == TellyBridge.Phase.CLUTCH && landingReady) recoverForward(layer);
      }
      airborne = !grounded;
      boolean forward = phase != TellyBridge.Phase.CLUTCH;
      // Follow the current feet without a takeoff-Y lock or a held Space key.
      return new TellyBridge.Step(phase, forward, grounded && canJump, layer);
   }

   public TellyBridge.Step recoverForward(int layer) {
      // Geometry, rather than a fixed air-tick delay, ends the raised catch.
      if (layer <= takeoffLayer) return null;
      phase = TellyBridge.Phase.RECOVER_FORWARD;
      return new TellyBridge.Step(phase, true, false, layer);
   }
}
