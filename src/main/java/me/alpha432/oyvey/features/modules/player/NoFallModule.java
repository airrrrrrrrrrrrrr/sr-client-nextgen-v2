package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;

public class NoFallModule extends Module {
   private final Setting<NoFallModule.Mode> mode = this.register(new Setting<>("Mode", NoFallModule.Mode.PACKET));

   public NoFallModule() {
      super("NoFall", "Removes fall damage", Module.Category.PLAYER);
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         switch ((NoFallModule.Mode)this.mode.getValue()) {
            case PACKET:
               if (!mc.player.onGround() && mc.player.fallDistance > 2.5) {
                  mc.player
                     .connection
                     .send(
                        new PosRot(
                           mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), true, mc.player.horizontalCollision
                        )
                     );
                  mc.player.fallDistance = 0.0;
               }
               break;
            case CANCEL:
               mc.player.fallDistance = 0.0;
               break;
            case VULCAN:
               double motY = mc.player.getDeltaMovement().y;
               if (!mc.player.onGround()) {
                  mc.player.fallDistance = 0.0;
                  if (motY < -0.05 && motY > -0.19) {
                     mc.player
                        .connection
                        .send(
                           new PosRot(
                              mc.player.getX(),
                              mc.player.getY(),
                              mc.player.getZ(),
                              mc.player.getYRot(),
                              mc.player.getXRot(),
                              true,
                              mc.player.horizontalCollision
                           )
                        );
                  }
               }
         }
      }
   }

   @Override
   public String getDisplayInfo() {
      return this.mode.getValue().name();
   }

   public enum Mode {
      PACKET,
      CANCEL,
      VULCAN;
   }
}
