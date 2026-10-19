package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.combat.GrimAura;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class TargetStrafe extends Module {
   public final Setting<Double> range = this.register(new Setting<>("Range", 3.0, 0.1, 6.0));
   public final Setting<Boolean> behind = this.register(new Setting<>("Behind", false));
   public final Setting<Boolean> fightOnly = this.register(new Setting<>("FightOnly", true));
   public final Setting<Boolean> speedOnly = this.register(new Setting<>("SpeedOnly", false));
   private int direction = 1;
   private int dirTickTimer = 0;
   private int directionTick = Integer.MIN_VALUE;

   public TargetStrafe() {
      super("TargetStrafe", "Circles around a combat target or nearby player.", Module.Category.MOVEMENT);
   }

   @Override
   public void onDisable() {
      this.direction = 1;
      this.dirTickTimer = 0;
      this.directionTick = Integer.MIN_VALUE;
   }

   @Override
   public void onTick() {
      this.applyMotion();
   }

   public void applyMotion() {
      if (!this.isEnabled()) return;
      if (!nullCheck()) {
         Speed speedModule = OyVey.moduleManager.getModuleByClass(Speed.class);
         if (this.speedOnly.getValue() && (speedModule == null || !speedModule.isEnabled())) {
            this.dirTickTimer = 0;
            return;
         }
         Entity target = this.getAuraTarget();
         if (target == null && !this.fightOnly.getValue()) {
            target = this.getNearbyTarget();
         }
         if (this.validTarget(target)) {
            if (this.directionTick != mc.player.tickCount) {
               this.directionTick = mc.player.tickCount;
               this.dirTickTimer++;
               if (mc.player.horizontalCollision && this.dirTickTimer >= 8) {
                  this.direction = -this.direction;
                  this.dirTickTimer = 0;
               }
            }

            double speed = mc.player.getDeltaMovement().horizontalDistance();
            if (!(speed < 0.01)) {
               this.doStrafe(target, speed);
            }
         }
      }
   }

   private Entity getAuraTarget() {
      GrimAura ga = OyVey.moduleManager.getModuleByClass(GrimAura.class);
      if (ga != null && ga.isEnabled() && this.validTarget(ga.getTarget())) {
         return ga.getTarget();
      }

      KillauraRecode kr = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      if (kr != null && kr.isEnabled() && this.validTarget(kr.getTarget())) {
         return kr.getTarget();
      }
      return null;
   }

   private boolean validTarget(Entity target) {
      return target != null && target != mc.player && target.isAlive() && !target.isRemoved()
         && mc.level.getEntity(target.getId()) == target
         && (!(target instanceof Player player) || !player.isSpectator() && !OyVey.friendManager.isFriend(player));
   }

   private Entity getNearbyTarget() {
      Player nearest = null;
      double bestDistance = 6.0 * 6.0;
      for (Player player : mc.level.players()) {
         if (!this.validTarget(player) || !mc.player.hasLineOfSight(player)) continue;
         double distance = mc.player.distanceToSqr(player);
         if (distance < bestDistance) {
            nearest = player;
            bestDistance = distance;
         }
      }
      return nearest;
   }

   private void doStrafe(Entity target, double speed) {
      double dx = mc.player.getX() - target.getX();
      double dz = mc.player.getZ() - target.getZ();
      double dist = Math.sqrt(dx * dx + dz * dz);
      if (!(dist < 0.01)) {
         double currentAngle = Math.atan2(dz, dx);
         if (this.behind.getValue()) {
            double yawRad = Math.toRadians(target.getYRot());
            double behindAngle = Math.atan2(-Math.cos(yawRad), Math.sin(yawRad));
            double diff = behindAngle - currentAngle;

            while (diff > 3.141592653589793) {
               diff -= 6.283185307179586;
            }

            while (diff < -3.141592653589793) {
               diff += 6.283185307179586;
            }

            this.direction = diff >= 0.0 ? 1 : -1;
         }

         double newAngle = currentAngle + this.direction * speed / Math.max(dist, 0.5);
         double orbitX = target.getX() + this.range.getValue() * Math.cos(newAngle);
         double orbitZ = target.getZ() + this.range.getValue() * Math.sin(newAngle);
         double vx = orbitX - mc.player.getX();
         double vz = orbitZ - mc.player.getZ();
         double vmag = Math.sqrt(vx * vx + vz * vz);
         if (vmag > 0.0) {
            vx = vx / vmag * speed;
            vz = vz / vmag * speed;
         }

         mc.player.setDeltaMovement(vx, mc.player.getDeltaMovement().y, vz);
      }
   }

   @Override
   public String getDisplayInfo() {
      return String.format("%.1fm%s", this.range.getValue(), this.behind.getValue() ? " Behind" : "");
   }
}
