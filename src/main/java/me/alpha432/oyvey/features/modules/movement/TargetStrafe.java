package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.combat.GrimAura;
import me.alpha432.oyvey.features.modules.combat.KillAura;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.world.entity.Entity;

public class TargetStrafe extends Module {
   public final Setting<Double> range = this.register(new Setting<>("Range", 3.0, 0.1, 6.0));
   public final Setting<Boolean> behind = this.register(new Setting<>("Behind", false));
   public final Setting<Boolean> onGround = this.register(new Setting<>("OnGround", false));
   private int direction = 1;
   private int dirTickTimer = 0;

   public TargetStrafe() {
      super("TargetStrafe", "Circles around your KillAura target.", Module.Category.MOVEMENT);
   }

   @Override
   public void onDisable() {
      this.direction = 1;
      this.dirTickTimer = 0;
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         Entity target = this.getAuraTarget();
         if (target != null && (!this.onGround.getValue() || mc.player.onGround())) {
            this.dirTickTimer++;
            if (mc.player.horizontalCollision && this.dirTickTimer >= 8) {
               this.direction = -this.direction;
               this.dirTickTimer = 0;
            }

            double speed = mc.player.getDeltaMovement().horizontalDistance();
            if (!(speed < 0.01)) {
               this.doStrafe(target, speed);
            }
         }
      }
   }

   private Entity getAuraTarget() {
      KillAura ka = OyVey.moduleManager.getModuleByClass(KillAura.class);
      if (ka != null && ka.isEnabled() && ka.getTarget() != null) {
         return ka.getTarget();
      }

      GrimAura ga = OyVey.moduleManager.getModuleByClass(GrimAura.class);
      if (ga != null && ga.isEnabled() && ga.getTarget() != null) {
         return ga.getTarget();
      }

      KillauraRecode kr = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      return kr != null && kr.isEnabled() && kr.getTarget() != null ? kr.getTarget() : null;
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
