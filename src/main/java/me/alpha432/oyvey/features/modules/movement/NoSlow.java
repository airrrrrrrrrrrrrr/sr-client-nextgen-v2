package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.item.ItemStack;

public class NoSlow extends Module {
   public final Setting<NoSlow.Mode> mode = this.register(new Setting<>("Mode", NoSlow.Mode.VANILLA));

   public NoSlow() {
      super("NoSlow", "Prevents slowdown when using items.", Module.Category.MOVEMENT);
   }

   @Subscribe
   public void onWalk(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == Stage.PRE && this.mode.getValue() == NoSlow.Mode.GRIM && mc.player != null && mc.player.isUsingItem()) {
         ItemStack inUse = mc.player.getUseItem();
         if (!inUse.has(DataComponents.FOOD) && (mc.player.xxa != 0.0F || mc.player.zza != 0.0F)) {
            mc.player.connection.send(new ServerboundPlayerActionPacket(Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
         }
      }
   }

   public enum Mode {
      VANILLA,
      GRIM;
   }
}
