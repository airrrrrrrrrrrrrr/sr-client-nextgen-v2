package me.alpha432.oyvey.features.modules.combat;

import java.util.Comparator;
import java.util.stream.StreamSupport;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.RotationUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class PvpBot extends Module {
   public final Setting<Double> reach = this.register(new Setting<>("Reach", 3.5, 1.0, 6.0));
   public final Setting<Boolean> mobs = this.register(new Setting<>("Mobs", false));
   public final Setting<Double> stopDist = this.register(new Setting<>("StopDist", 1.8, 0.5, 4.0));
   private static final double SPRINT_SPEED = 0.286;
   private static final long ATTACK_PAUSE = 200L;
   private Entity currentTarget = null;
   private long lastAttackTime = 0L;
   private float sentYaw = 0.0F;
   private float sentPitch = 0.0F;
   private boolean hasSentRot = false;

   public PvpBot() {
      super("PvpBot", "Automatically moves to and attacks the nearest player.", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      this.currentTarget = null;
      this.lastAttackTime = 0L;
      this.hasSentRot = false;
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         mc.player.setSprinting(false);
      }

      this.currentTarget = null;
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         this.currentTarget = this.findNearest();
         if (this.currentTarget == null) {
            mc.player.setSprinting(false);
         } else {
            double dist = mc.player.distanceTo(this.currentTarget);
            if (dist > this.stopDist.getValue()) {
               double dx = this.currentTarget.getX() - mc.player.getX();
               double dz = this.currentTarget.getZ() - mc.player.getZ();
               double len = Math.sqrt(dx * dx + dz * dz);
               mc.player.setSprinting(true);
               mc.player.setDeltaMovement(dx / len * 0.286, mc.player.getDeltaMovement().y, dz / len * 0.286);
            } else {
               mc.player.setSprinting(false);
            }
         }
      }
   }

   @Subscribe
   public void onUpdateWalkingPlayer(UpdateWalkingPlayerEvent event) {
      if (mc.level != null && mc.player != null && this.currentTarget != null && this.currentTarget.isAlive()) {
         if (event.getStage() == Stage.MOTION_PRE) {
            float[] rots = this.calcRotations(this.currentTarget);
            OyVey.rotationManager.setPlayerRotations(rots[0], rots[1]);
         } else if (event.getStage() == Stage.PRE) {
            float[] rots = this.calcRotations(this.currentTarget);
            OyVey.rotationManager.setPlayerRotations(rots[0], rots[1]);
            this.sentYaw = rots[0];
            this.sentPitch = rots[1];
            this.hasSentRot = true;
            long now = System.currentTimeMillis();
            double dist = mc.player.distanceTo(this.currentTarget);
            if (dist <= this.reach.getValue() && now - this.lastAttackTime >= 200L && mc.player.getAttackStrengthScale(0.0F) >= 0.9F) {
               mc.player.setSprinting(false);
               mc.gameMode.attack(mc.player, this.currentTarget);
               mc.player.swing(InteractionHand.MAIN_HAND);
               this.lastAttackTime = now;
            }
         }
      }
   }

   private Entity findNearest() {
      return StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), false)
         .filter(e -> e != mc.player && e.isAlive())
         .filter(e -> e instanceof Player || this.mobs.getValue() && e instanceof Mob)
         .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
         .orElse(null);
   }

   private float[] calcRotations(Entity target) {
      double dx = target.getX() - mc.player.getX();
      double dy = target.getY() + target.getEyeHeight() * 0.6 - (mc.player.getY() + mc.player.getEyeHeight());
      double dz = target.getZ() - mc.player.getZ();
      float[] raw = RotationUtils.fromRotationVec(dx, dy, dz);
      if (!this.hasSentRot) {
         this.sentYaw = mc.player.getYRot();
         this.sentPitch = mc.player.getXRot();
      }

      return RotationUtils.normalizeGcd(raw, this.sentYaw, this.sentPitch);
   }

   @Override
   public String getDisplayInfo() {
      return this.currentTarget instanceof Player p ? p.getName().getString() : "";
   }
}
