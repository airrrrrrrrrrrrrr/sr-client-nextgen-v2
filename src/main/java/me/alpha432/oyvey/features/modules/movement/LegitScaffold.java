package me.alpha432.oyvey.features.modules.movement;

import java.util.concurrent.ThreadLocalRandom;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class LegitScaffold extends Module {
   public final Setting<Integer> minDelay = this.register(new Setting<>("MinDelay", 50, 0, 500));
   public final Setting<Integer> maxDelay = this.register(new Setting<>("MaxDelay", 100, 0, 500));
   public final Setting<Boolean> onlyBlocks = this.register(new Setting<>("OnlyBlocks", true));
   public final Setting<Boolean> holdShift = this.register(new Setting<>("HoldShift", false));
   private long currentDelay;
   private long lastSolidTime;
   private boolean wasOnSolid;

   public LegitScaffold() {
      super("LegitScaffold", "Auto-sneaks when bridging to prevent walking off edges.", Module.Category.OLD);
   }

   @Override
   public void onEnable() {
      this.wasOnSolid = true;
      this.lastSolidTime = System.currentTimeMillis();
      this.randomiseDelay();
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         mc.player.setShiftKeyDown(false);
      }
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.level != null) {
         if (this.onlyBlocks.getValue() && !this.isHoldingBlock()) {
            mc.player.setShiftKeyDown(false);
         } else {
            boolean active = this.holdShift.getValue() ? mc.options.keyShift.isDown() : mc.options.keyDown.isDown();
            if (!active) {
               if (!this.wasOnSolid) {
                  this.wasOnSolid = true;
                  mc.player.setShiftKeyDown(false);
               }
            } else {
               BlockPos below = new BlockPos((int)Math.floor(mc.player.getX()), mc.player.blockPosition().getY() - 1, (int)Math.floor(mc.player.getZ()));
               boolean aboveAir = mc.level.getBlockState(below).isAir();
               if (aboveAir) {
                  if (this.wasOnSolid) {
                     this.wasOnSolid = false;
                     if (System.currentTimeMillis() - this.lastSolidTime >= this.currentDelay) {
                        mc.player.setShiftKeyDown(true);
                     }
                  } else {
                     mc.player.setShiftKeyDown(true);
                  }
               } else {
                  if (!this.wasOnSolid) {
                     this.wasOnSolid = true;
                     this.lastSolidTime = System.currentTimeMillis();
                     this.randomiseDelay();
                  }

                  mc.player.setShiftKeyDown(false);
               }
            }
         }
      }
   }

   private void randomiseDelay() {
      long lo = this.minDelay.getValue().intValue();
      long hi = Math.max(lo, this.maxDelay.getValue().intValue());
      this.currentDelay = lo >= hi ? lo : ThreadLocalRandom.current().nextLong(lo, hi);
   }

   private boolean isHoldingBlock() {
      ItemStack stack = mc.player.getMainHandItem();
      return !stack.isEmpty() && stack.getItem() instanceof BlockItem;
   }
}
