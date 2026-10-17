package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.features.modules.combat.WTap;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;

public class SprintModule extends Module {
   public final Setting<SprintModule.Mode> mode = this.register(new Setting<>("Mode", SprintModule.Mode.LEGIT));
   public final Setting<Boolean> keepSprint = this.bool("KeepSprint", false);
   public final Setting<Boolean> ignoreHunger = this.bool("IgnoreHunger", false);
   public final Setting<Boolean> stopOnSneaking = this.bool("StopOnSneaking", true);
   public final Setting<Boolean> stopOnUsingItem = this.bool("StopOnUsingItem", false);
   public final Setting<Integer> sprintResetTicks = this.num("SprintResetTicks", 10, 1, 40);
   public static volatile long lastAttackMs = 0L;

   public SprintModule() {
      super("Sprint", "Automatically sprints.", Module.Category.MOVEMENT);
   }

   @Override
   public void onDisable() {
      this.releaseSprint();
   }

   private boolean attackPaused() {
      KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      boolean grimAura = aura != null && aura.isGrimMovementCorrectionConfigured();
      boolean canKeepSprint = this.keepSprint.getValue() && !grimAura;
      int resetTicks = grimAura ? Math.min(2, this.sprintResetTicks.getValue()) : this.sprintResetTicks.getValue();
      return !canKeepSprint && System.currentTimeMillis() - lastAttackMs < resetTicks * 50L;
   }

   private boolean shouldSprint() {
      KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      WTap wTap = OyVey.moduleManager.getModuleByClass(WTap.class);
      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      boolean grimAura = aura != null && aura.isGrimMovementCorrectionConfigured();
      if (scaffold != null && scaffold.isSprintSuppressed()) {
         return false;
      } else if (wTap != null && wTap.isResettingSprint()) {
         return false;
      } else if (aura != null && aura.isGrimMovementCorrectionActive() && !aura.isGrimSprintAllowed()) {
         return false;
      } else if (!this.ignoreHunger.getValue() && mc.player.getFoodData().getFoodLevel() <= 6 && !mc.player.getAbilities().mayfly) {
         return false;
      } else if (this.stopOnSneaking.getValue() && mc.player.isCrouching()) {
         return false;
      } else if (this.stopOnUsingItem.getValue() && mc.player.isUsingItem()) {
         return false;
      } else if (!mc.player.isFallFlying() && !mc.player.isPassenger() && !mc.player.horizontalCollision) {
         float strafe = mc.player.input.getMoveVector().x;
         float forward = mc.player.input.getMoveVector().y;
         if (grimAura || this.mode.getValue() == SprintModule.Mode.GRIM) {
            return forward >= 0.8F;
         }

         return this.mode.getValue() == SprintModule.Mode.OMNI ? strafe != 0.0F || forward != 0.0F : forward >= 0.8F;
      } else {
         return false;
      }
   }

   private void releaseSprint() {
      if (mc.player != null && mc.player.isSprinting()) {
         mc.player.setSprinting(false);
      }
   }

   public void applySprintInput(ClientInput input) {
      if (!this.isEnabled() || mc.player == null) {
         return;
      }

      boolean sprintPressed = !this.attackPaused() && this.shouldSprint();
      Input keys = input.keyPresses;
      input.keyPresses = new Input(
         keys.forward(),
         keys.backward(),
         keys.left(),
         keys.right(),
         keys.jump(),
         keys.shift(),
         sprintPressed
      );
      if (!sprintPressed) {
         mc.player.setSprinting(false);
      }
   }

   public boolean canKeepSprintForAura() {
      KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      return this.keepSprint.getValue() && (aura == null || !aura.isGrimMovementCorrectionConfigured());
   }

   @Override
   public String getDisplayInfo() {
      return this.mode.getValue().name().toLowerCase();
   }

   public enum Mode {
      LEGIT,
      GRIM,
      OMNI;
   }
}
