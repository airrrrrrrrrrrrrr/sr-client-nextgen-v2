package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.world.phys.Vec3;

public class FastFall extends Module {
   public final Setting<Float> speed = this.num("Speed", 2.0F, 1.0F, 5.0F);

   public FastFall() {
      super("FastFall", "Fall faster than vanilla.", Module.Category.MOVEMENT);
   }

   @Override
   public void onTick() {
      if (mc.player != null && !mc.player.onGround()) {
         Vec3 mot = mc.player.getDeltaMovement();
         if (!(mot.y >= 0.0)) {
            double extra = 0.08 * (this.speed.getValue().floatValue() - 1.0);
            mc.player.setDeltaMovement(mot.x, mot.y - extra, mot.z);
         }
      }
   }

   @Override
   public String getDisplayInfo() {
      return String.format("%.1fx", this.speed.getValue());
   }
}
