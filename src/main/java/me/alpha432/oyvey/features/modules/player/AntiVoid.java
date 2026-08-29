package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.world.phys.Vec3;

public class AntiVoid extends Module {
   public final Setting<AntiVoid.Mode> mode = this.register(new Setting<>("Mode", AntiVoid.Mode.PACKET));
   public final Setting<Integer> safeOffset = this.register(new Setting<>("SafeOffset", 5, 1, 20));

   public AntiVoid() {
      super("AntiVoid", "Prevents dying in the void.", Module.Category.PLAYER);
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.level != null) {
         int minY = mc.level.getMinY();
         double safeY = minY + this.safeOffset.getValue();
         if (!(mc.player.getY() >= minY)) {
            Vec3 mot = mc.player.getDeltaMovement();
            if (this.mode.getValue() == AntiVoid.Mode.PACKET) {
               mc.player
                  .connection
                  .send(new PosRot(mc.player.getX(), safeY, mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), true, mc.player.horizontalCollision));
            }

            mc.player.setPos(mc.player.getX(), safeY, mc.player.getZ());
            mc.player.setDeltaMovement(mot.x, 0.0, mot.z);
            mc.player.fallDistance = 0.0;
         }
      }
   }

   @Override
   public String getDisplayInfo() {
      return this.mode.getValue().name();
   }

   public enum Mode {
      PACKET,
      CLIP;
   }
}
