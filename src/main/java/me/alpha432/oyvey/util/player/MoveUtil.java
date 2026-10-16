package me.alpha432.oyvey.util.player;

import me.alpha432.oyvey.util.MathUtil;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class MoveUtil implements Util {
   public static boolean isMoving() {
      if (mc.player == null) {
         return false;
      }

      var move = mc.player.input.getMoveVector();
      return move.x != 0.0F || move.y != 0.0F;
   }

   public static boolean isGoingDiagonally() {
      if (mc.player == null) {
         return false;
      }

      var move = mc.player.input.getMoveVector();
      return move.x != 0.0F && move.y != 0.0F;
   }

   public static boolean isPressingForwardAndStrafe() {
      if (mc.player == null) {
         return false;
      }

      boolean forward = mc.options.keyUp.isDown() || mc.options.keyDown.isDown();
      boolean strafe = mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
      return forward && strafe;
   }

   public static float getMovementYaw(Entity entity) {
      if (mc.player == null || entity == null) {
         return 0.0F;
      }

      var move = mc.player.input.getMoveVector();
      float forward = move.y;
      float side = move.x;
      float yaw = entity.getYRot();
      if (forward != 0.0F) {
         if (side > 0.0F) {
            yaw += forward > 0.0F ? -45.0F : 45.0F;
         } else if (side < 0.0F) {
            yaw += forward > 0.0F ? 45.0F : -45.0F;
         }
      } else if (side != 0.0F) {
         yaw += side > 0.0F ? -90.0F : 90.0F;
      }

      return Mth.wrapDegrees(yaw);
   }

   public static void strafe(float speed) {
      strafe((double) speed);
   }

   public static void strafe(double speed) {
      if (mc.player == null || speed == 0.0) {
         return;
      }

      double[] dir = MathUtil.directionSpeed(speed);
      mc.player.setDeltaMovement(dir[0], mc.player.getDeltaMovement().y, dir[1]);
   }

   public static void stop() {
      if (mc.player == null) {
         return;
      }

      mc.player.setDeltaMovement(0.0, mc.player.getDeltaMovement().y, 0.0);
   }

   public static float getPerfectValue(float highSpeed, float lowSpeed, float speed) {
      return Mth.clamp(speed, Math.min(highSpeed, lowSpeed), Math.max(highSpeed, lowSpeed));
   }
}
