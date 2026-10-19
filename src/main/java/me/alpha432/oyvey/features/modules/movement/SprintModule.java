package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.features.modules.combat.WTap;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.mixin.render.accessor.LocalPlayerRotationAccessor;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;

public class SprintModule extends Module {
   public final Setting<SprintModule.Mode> mode = this.register(new Setting<>("Mode", SprintModule.Mode.GRIM));
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
      // Like holding the native sprint key: attacks still apply their normal
      // slowdown, and vanilla decides when sprint can start again. Repeated
      // attacks must not keep extending a separate 100-500 ms sprint lockout.
      if (grimAura || this.mode.getValue() == Mode.GRIM) return false;
      return !this.keepSprint.getValue()
         && System.currentTimeMillis() - lastAttackMs < this.sprintResetTicks.getValue() * 50L;
   }

   private boolean shouldSprint(ClientInput input) {
      KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      WTap wTap = OyVey.moduleManager.getModuleByClass(WTap.class);
      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      boolean grimAura = aura != null && aura.isGrimMovementCorrectionConfigured();
      boolean conservative = grimAura || this.mode.getValue() == Mode.GRIM;
      Input keys = input.keyPresses;
      if (aura != null && aura.isCriticalSprintSuppressed()) {
         return false;
      } else if (scaffold != null && scaffold.isSprintSuppressed()) {
         return false;
      } else if (wTap != null && wTap.isResettingSprint()) {
         return false;
      } else if (aura != null && aura.isGrimMovementCorrectionActive() && !aura.isGrimSprintAllowed()) {
         return false;
      } else if (conservative) {
         if (keys.shift() || !keys.forward() || keys.backward() || !input.hasForwardImpulse()
            || this.stopOnUsingItem.getValue() && mc.player.isUsingItem()) return false;
         // Ask vanilla about the final corrected input. Its start/continue
         // rules differ (including minor collisions and swimming), so a
         // second set of approximated checks can unnecessarily drop sprint.
         LocalPlayerRotationAccessor nativeSprint = (LocalPlayerRotationAccessor)mc.player;
         return mc.player.isSprinting()
            ? !(mc.player.isSwimming() ? nativeSprint.oyvey$shouldStopSwimSprinting() : nativeSprint.oyvey$shouldStopRunSprinting())
            : nativeSprint.oyvey$canStartSprinting();
      } else if ((conservative || !this.ignoreHunger.getValue()) && mc.player.getFoodData().getFoodLevel() <= 6 && !mc.player.getAbilities().mayfly) {
         return false;
      } else if ((conservative || this.stopOnSneaking.getValue()) && keys.shift()) {
         return false;
      } else if ((conservative || this.stopOnUsingItem.getValue()) && mc.player.isUsingItem()) {
         return false;
      } else if (conservative && mc.player.isMobilityRestricted()) {
         return false;
      } else if (!mc.player.isFallFlying() && !mc.player.isPassenger()
         && (!mc.player.horizontalCollision || mc.player.minorHorizontalCollision)) {
         if (scaffold != null && scaffold.isEnabled()) {
            return keys.forward() && !keys.backward() && !keys.shift() && input.hasForwardImpulse();
         }
         if (!conservative && this.mode.getValue() == Mode.OMNI) {
            return input.getMoveVector().lengthSquared() > 0;
         }
         // Modern diagonals are normalized to ~0.707 forward. Use vanilla's
         // forward-impulse check on the final corrected input, not a raw 0.8
         // threshold or the physical W binding.
         return keys.forward() && !keys.backward() && input.hasForwardImpulse();
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

      boolean sprintPressed = !this.attackPaused() && this.shouldSprint(input);
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
