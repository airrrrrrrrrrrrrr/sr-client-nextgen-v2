package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public class SpeedOld extends Module {
   public final Setting<SpeedOld.Mode> mode = this.register(new Setting<>("Mode", SpeedOld.Mode.VANILLA));
   private int friction = 0;

   public SpeedOld() {
      super("SpeedOld", "Allows you to move faster", Module.Category.OLD);
   }

   @Override
   public void onEnable() {
      this.friction = 0;
   }

   @Subscribe
   public void onMotion(UpdateWalkingPlayerEvent event) {
      if (mc.player != null && mc.level != null && event.getStage() == Stage.PRE) {
         switch ((SpeedOld.Mode)this.mode.getValue()) {
            case VANILLA:
               this.setMotion(this.getPlayerSpeed());
               if (mc.player.onGround() && this.isMoving()) {
                  mc.player.jumpFromGround();
               }
               break;
            case WATCHDOG:
               if (mc.player.onGround()) {
                  this.friction = 0;
               }

               if (mc.player.onGround() && this.isMoving()) {
                  this.setMotionY(0.42);
                  this.setMotion(this.getBaseMoveSpeed() * 1.6414 - this.friction * 0.1252345);
                  this.friction++;
               }
               break;
            case WATCHDOG_EXPERIMENTAL:
               if (mc.player.onGround()) {
                  this.friction = 0;
               }

               if (this.killauraHasTarget()) {
                  this.strafe();
               }

               if (mc.player.onGround() && this.isMoving()) {
                  this.setMotionY(0.42);
                  this.setMotion(this.getBaseMoveSpeed() * 1.6414 - this.friction * 0.1252345);
                  this.friction++;
               }
               break;
            case STRAFE:
               this.strafe();
               if (mc.player.onGround() && this.isMoving()) {
                  mc.player.jumpFromGround();
               }
               break;
            case VERUS:
               if (mc.player.onGround() && this.isMoving()) {
                  mc.player.jumpFromGround();
                  this.setMotion(0.445);
               } else {
                  this.setMotion(0.32);
               }

               if (!(mc.player.zza > 0.0F)) {
                  this.setMotion(0.3225);
               }
         }
      }
   }

   @Override
   public String getDisplayInfo() {
      return this.mode.getValue().getName();
   }

   private boolean isMoving() {
      return mc.player.xxa != 0.0F || mc.player.zza != 0.0F;
   }

   private boolean killauraHasTarget() {
      KillauraRecode ka = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      return ka != null && ka.isEnabled() && ka.getTarget() != null;
   }

   private double getPlayerSpeed() {
      Vec3 m = mc.player.getDeltaMovement();
      return Math.sqrt(m.x * m.x + m.z * m.z);
   }

   private double getBaseMoveSpeed() {
      double base = 0.2873;
      if (mc.player.hasEffect(MobEffects.SPEED)) {
         base *= 1.0 + 0.2 * (mc.player.getEffect(MobEffects.SPEED).getAmplifier() + 1);
      }

      return base;
   }

   private void setMotionY(double y) {
      Vec3 m = mc.player.getDeltaMovement();
      mc.player.setDeltaMovement(m.x, y, m.z);
   }

   private void strafe() {
      this.setMotion(this.getPlayerSpeed());
   }

   private void setMotion(double speed) {
      float yaw = mc.player.getYRot();
      float forward = mc.player.zza;
      float strafe = mc.player.xxa;
      Vec3 m = mc.player.getDeltaMovement();
      if (forward == 0.0F && strafe == 0.0F) {
         mc.player.setDeltaMovement(0.0, m.y, 0.0);
      } else {
         if (forward != 0.0F) {
            if (strafe > 0.0F) {
               yaw += forward > 0.0F ? -45 : 45;
            } else if (strafe < 0.0F) {
               yaw += forward > 0.0F ? 45 : -45;
            }

            strafe = 0.0F;
            forward = forward > 0.0F ? 1.0F : -1.0F;
         }

         double cos = Math.cos(Math.toRadians(yaw + 90.0));
         double sin = Math.sin(Math.toRadians(yaw + 90.0));
         mc.player.setDeltaMovement(forward * speed * cos + strafe * speed * sin, m.y, forward * speed * sin - strafe * speed * cos);
      }
   }

   public enum Mode {
      VANILLA("Vanilla"),
      WATCHDOG("Watchdog"),
      WATCHDOG_EXPERIMENTAL("Watchdog Experimental"),
      STRAFE("Strafe"),
      VERUS("Verus");

      private final String name;

      Mode(String name) {
         this.name = name;
      }

      public String getName() {
         return this.name;
      }
   }
}
