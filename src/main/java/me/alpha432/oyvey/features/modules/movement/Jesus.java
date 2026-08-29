package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.world.phys.Vec3;

public class Jesus extends Module {
   public final Setting<Jesus.Mode> mode = this.register(new Setting<>("Mode", Jesus.Mode.SOLID));
   public final Setting<Float> speed = this.num("Speed", 1.0F, 0.1F, 5.0F);
   private int ticker = 0;

   public Jesus() {
      super("Jesus", "Walk on water.", Module.Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      this.ticker = 0;
   }

   @Override
   public void onDisable() {
      this.ticker = 0;
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.level != null && (mc.player.isInWater() || mc.player.isInLava())) {
         switch ((Jesus.Mode)this.mode.getValue()) {
            case SOLID:
               this.handleSolid();
               break;
            case NCP:
               this.handleNcp();
         }
      }
   }

   private void handleSolid() {
      Vec3 mot = mc.player.getDeltaMovement();
      if (mot.y < 0.0) {
         mc.player.setDeltaMovement(mot.x * this.speed.getValue().floatValue(), 0.0, mot.z * this.speed.getValue().floatValue());
      } else {
         mc.player.setDeltaMovement(mot.x * this.speed.getValue().floatValue(), mot.y, mot.z * this.speed.getValue().floatValue());
      }
   }

   private void handleNcp() {
      this.ticker++;
      Vec3 mot = mc.player.getDeltaMovement();
      if (this.ticker % 20 != 0) {
         double targetY = mot.y < 0.1 ? 0.1 : mot.y;
         mc.player.setDeltaMovement(mot.x * this.speed.getValue().floatValue(), targetY, mot.z * this.speed.getValue().floatValue());
         mc.player.fallDistance = 0.0;
      }
   }

   public enum Mode {
      SOLID,
      NCP;
   }
}
