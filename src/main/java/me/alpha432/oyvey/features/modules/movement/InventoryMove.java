package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

public class InventoryMove extends Module {
   public final Setting<InventoryMove.Mode> mode = this.register(new Setting<>("Mode", InventoryMove.Mode.VULCAN));
   public final Setting<Boolean> jump = this.bool("Jump", true);
   private boolean movingThisTick = false;

   public InventoryMove() {
      super("InventoryMove", "Move while inventory is open.", Module.Category.MOVEMENT);
   }

   @Subscribe
   private void onMotion(UpdateWalkingPlayerEvent event) {
      if (mc.player != null && mc.screen instanceof AbstractContainerScreen) {
         if (event.getStage() == Stage.MOTION_PRE) {
            float fwd = (mc.options.keyUp.isDown() ? 1.0F : 0.0F) - (mc.options.keyDown.isDown() ? 1.0F : 0.0F);
            float strafe = (mc.options.keyLeft.isDown() ? 1.0F : 0.0F) - (mc.options.keyRight.isDown() ? 1.0F : 0.0F);
            this.movingThisTick = fwd != 0.0F || strafe != 0.0F;
            mc.player.zza = fwd;
            mc.player.xxa = strafe;
            if (this.jump.getValue() && mc.options.keyJump.isDown() && mc.player.onGround()) {
               mc.player.jumpFromGround();
            }

            mc.player.setSprinting(false);
         } else if (event.getStage() == Stage.PRE && this.mode.getValue() == InventoryMove.Mode.VULCAN) {
            if (!this.movingThisTick) {
               return;
            }

            int cid = mc.player.containerMenu.containerId;
            if (cid != 0) {
               mc.player.connection.send(new ServerboundContainerClosePacket(cid));
            }
         }
      }
   }

   public enum Mode {
      BASIC,
      VULCAN;
   }
}
