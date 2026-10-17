package me.alpha432.oyvey.features.modules.combat;

import java.util.concurrent.ThreadLocalRandom;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.mixin.render.accessor.ClientInputAccessor;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import me.alpha432.oyvey.util.KeyboardMovement;
import me.alpha432.oyvey.util.SilentRotationMath;

public class WTap extends Module {
   public final Setting<Integer> duration = this.num("Duration", 1, 1, 5);
   public final Setting<Integer> chance = this.num("Chance", 100, 1, 100);
   private boolean pendingTap = false;
   private int countdown = 0;
   private boolean stopApplied = false;

   public WTap() {
      super("WTap", "Briefly stops W and cuts sprint after each hit to maximise knockback.", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      this.pendingTap = false;
      this.countdown = 0;
      this.stopApplied = false;
   }

   @Override
   public void onDisable() {
      this.pendingTap = false;
      this.countdown = 0;
      this.stopApplied = false;
   }

   public static void onAttack() {
      WTap inst = OyVey.moduleManager.getModuleByClass(WTap.class);
      if (inst != null && inst.isEnabled() && inst.countdown <= 0 && !inst.stopApplied) {
         inst.pendingTap = true;
      }
   }

   public boolean isResettingSprint() {
      return this.isEnabled() && (this.pendingTap || this.countdown > 0 || this.stopApplied);
   }

   public void applyInputReset(ClientInput input) {
      if (!this.isEnabled() || mc.player == null) {
         return;
      }

      // Advance the reset only when fresh keyboard input is available, before
      // physics. The chance gate also applies before any keys are suppressed.
      this.stopApplied = false;
      if (this.pendingTap) {
         this.pendingTap = false;
         if (mc.options.keyUp.isDown() && ThreadLocalRandom.current().nextInt(100) < this.chance.getValue()) {
            this.countdown = this.duration.getValue();
         }
      }
      if (this.countdown <= 0) {
         return;
      }

      this.countdown--;
      this.stopApplied = true;
      Input keys = input.keyPresses;
      input.keyPresses = new Input(false, keys.backward(), keys.left(), keys.right(), keys.jump(), keys.shift(), false);
      ((ClientInputAccessor)input).oyvey$setMoveVector(
         KeyboardMovement.vector(SilentRotationMath.correctMovement(input.keyPresses, 0, 0)));
      mc.player.setSprinting(false);
   }
}
