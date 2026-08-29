package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

public class NoWebModule extends Module {
   public static NoWebModule INSTANCE;
   public final Setting<Double> speed = this.register(new Setting<>("Speed", 0.15, 0.0, 1.0));

   public NoWebModule() {
      super("NoWeb", "Move at a configurable speed through cobwebs.", Module.Category.MOVEMENT);
      INSTANCE = this;
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         BlockPos pos = mc.player.blockPosition();
         if (mc.player.level().getBlockState(pos).is(Blocks.COBWEB) && (mc.player.xxa != 0.0F || mc.player.zza != 0.0F)) {
            float yaw = mc.player.getYRot();
            float forward = mc.player.zza;
            float strafe = mc.player.xxa;
            if (forward != 0.0F) {
               if (strafe > 0.0F) {
                  yaw += forward > 0.0F ? -45 : 45;
               } else if (strafe < 0.0F) {
                  yaw += forward > 0.0F ? 45 : -45;
               }

               strafe = 0.0F;
               forward = forward > 0.0F ? 1.0F : -1.0F;
            }

            double sp = this.speed.getValue();
            double cos = Math.cos(Math.toRadians(yaw + 90.0));
            double sin = Math.sin(Math.toRadians(yaw + 90.0));
            mc.player.setDeltaMovement(forward * sp * cos + strafe * sp * sin, mc.player.getDeltaMovement().y, forward * sp * sin - strafe * sp * cos);
         }
      }
   }

   @Override
   public String getDisplayInfo() {
      return !this.isEnabled() ? null : String.format("%.2f", this.speed.getValue());
   }
}
