package me.alpha432.oyvey.features.modules.player;

import java.security.SecureRandom;
import java.util.Random;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.combat.WTap;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult.Type;
import org.lwjgl.glfw.GLFW;

public class AutoClickerModule extends Module {
   public final Setting<Boolean> mode19 = this.register(new Setting<>("19Mode", false));
   public final Setting<Boolean> mode18 = this.register(new Setting<>("18Mode", true));
   public final Setting<Integer> cps = this.register(new Setting<>("CPS", 12, 1, 20));
   public final Setting<Boolean> humanize = this.register(new Setting<>("Humanize", true));
   public final Setting<Boolean> intave = this.register(new Setting<>("IntaveBypass", true));
   public final Setting<Boolean> entityOnly = this.register(new Setting<>("EntityOnly", true));
   public final Setting<Boolean> hitSelect = this.register(new Setting<>("HitSelect", false));
   private static final double MISS_CHANCE = 0.018;
   private static final double FATIGUE_CHANCE = 0.025;
   private static final double CPS_STDDEV = 0.08;
   private static final double JITTER_RANGE = 0.12;
   private final Random rng = new Random(new SecureRandom().nextLong());
   private long nextClickNs = 0L;
   private boolean prevOnEntity = false;
   private boolean hitSelectFired = false;

   public AutoClickerModule() {
      super("AutoClicker", "Auto clicker", Module.Category.GHOST);
   }

   @Subscribe
   public void onWalk(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == Stage.PRE && mc.player != null && mc.level != null) {
         boolean var10000;
         if (mc.crosshairPickEntity instanceof LivingEntity le && le.isAlive()) {
            var10000 = true;
         } else {
            var10000 = false;
         }

         boolean curOnEntity = var10000;
         if (!this.hitSelect.getValue()) {
            this.prevOnEntity = curOnEntity;
            if (this.isHoldingAttack() && (!this.mode19.getValue() || this.mode18.getValue() || !(mc.player.getAttackStrengthScale(0.0F) < 0.9F))) {
               long nowNs = System.nanoTime();
               if (nowNs >= this.nextClickNs) {
                  if (this.humanize.getValue() && this.rng.nextDouble() < 0.018) {
                     this.nextClickNs = nowNs + this.sampleDelayNs(false);
                  } else if (this.performClick()) {
                     this.nextClickNs = nowNs + this.sampleDelayNs(this.humanize.getValue());
                  }
               }
            }
         } else {
            boolean justArrived = curOnEntity && !this.prevOnEntity;
            if (justArrived && !this.hitSelectFired && this.isHoldingAttack()) {
               this.performClick();
               this.hitSelectFired = true;
            }

            if (!curOnEntity) {
               this.hitSelectFired = false;
            }

            this.prevOnEntity = curOnEntity;
         }
      }
   }

   private boolean performClick() {
      if (mc.hitResult == null) {
         return false;
      }

      Type type = mc.hitResult.getType();
      if (type == Type.MISS) {
         return false;
      }

      Entity crosshair = mc.crosshairPickEntity;
      if (!this.entityOnly.getValue() || crosshair instanceof LivingEntity le && le.isAlive()) {
         if (crosshair instanceof LivingEntity target && target.isAlive() && target != mc.player) {
            boolean wasSprinting = mc.player.isSprinting();
            if (this.intave.getValue() && wasSprinting) {
               mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SPRINTING));
               mc.player.setSprinting(false);
            }

            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);
            WTap.onAttack();
            if (this.intave.getValue() && wasSprinting) {
               mc.player.setSprinting(true);
               mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.START_SPRINTING));
            }

            return true;
         } else if (!this.entityOnly.getValue()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private long sampleDelayNs(boolean applyHumanization) {
      double meanCps = this.cps.getValue().intValue();
      double effectiveCps = meanCps;
      if (applyHumanization) {
         effectiveCps = meanCps + this.rng.nextGaussian() * meanCps * 0.08;
      }

      effectiveCps = Math.max(1.0, Math.min(25.0, effectiveCps));
      long baseNs = (long)(1.0E9 / effectiveCps);
      if (!applyHumanization) {
         return baseNs;
      }

      long jitter = (long)(baseNs * 0.12 * (2.0 * this.rng.nextDouble() - 1.0));
      long totalNs = baseNs + jitter;
      if (this.rng.nextDouble() < 0.025) {
         totalNs += 150000000L + (long)(this.rng.nextDouble() * 2.0E8);
      }

      return Math.max(20000000L, totalNs);
   }

   private boolean isHoldingAttack() {
      return GLFW.glfwGetMouseButton(mc.getWindow().handle(), 0) == 1 || AimAssistModule.simulateClick;
   }
}
