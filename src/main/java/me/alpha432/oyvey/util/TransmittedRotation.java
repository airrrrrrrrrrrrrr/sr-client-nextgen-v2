package me.alpha432.oyvey.util;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;

/** The last outgoing look and the target it was actually prepared for. */
public final class TransmittedRotation {
   private float yaw, pitch;
   private int targetId = -1;

   public void reset(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
      this.targetId = -1;
   }

   public void record(ServerboundMovePlayerPacket packet, boolean cancelled, int intendedTarget,
      float intendedYaw, float intendedPitch) {
      if (cancelled) return;
      this.yaw = packet.getYRot(this.yaw);
      this.pitch = packet.getXRot(this.pitch);
      this.targetId = intendedTarget >= 0 && Math.abs(Mth.wrapDegrees(this.yaw - intendedYaw)) < 0.001F
         && Math.abs(this.pitch - intendedPitch) < 0.001F ? intendedTarget : -1;
   }

   public float yaw() { return this.yaw; }
   public float pitch() { return this.pitch; }
   public int targetId() { return this.targetId; }
}
