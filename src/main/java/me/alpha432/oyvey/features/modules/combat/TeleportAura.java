package me.alpha432.oyvey.features.modules.combat;

import java.util.Comparator;
import java.util.stream.StreamSupport;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class TeleportAura extends Module {
   public final Setting<Double> maxRange = this.register(new Setting<>("MaxRange", 20.0, 1.0, 60.0));
   public final Setting<Float> cps = this.register(new Setting<>("CPS", 1.0F, 0.1F, 20.0F));
   public final Setting<Boolean> mobs = this.register(new Setting<>("Mobs", false));
   public final Setting<Boolean> swing = this.register(new Setting<>("Swing", true));
   private volatile boolean blinking = false;
   private long lastAttackMs = 0L;

   public TeleportAura() {
      super("TeleportAura", "Blink-teleports to nearby players and attacks them at adjustable CPS.", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.blinking = false;
      this.lastAttackMs = 0L;
   }

   @Subscribe
   private void onReceive(PacketEvent.Receive event) {
      if (this.blinking && !(event.getPacket() instanceof ClientboundPlayerPositionPacket)) {
         event.cancel();
      }
   }

   @Subscribe
   private void onMotion(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == Stage.PRE && mc.player != null && mc.level != null) {
         long now = System.currentTimeMillis();
         long delay = (long)(1000.0F / this.cps.getValue());
         if (now - this.lastAttackMs >= delay) {
            Entity target = this.findTarget();
            if (target != null) {
               double px = mc.player.getX();
               double py = mc.player.getY();
               double pz = mc.player.getZ();
               double dx = px - target.getX();
               double dy = py - target.getY();
               double dz = pz - target.getZ();
               double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
               if (dist != 0.0) {
                  double scale = 2.5 / dist;
                  boolean og = mc.player.onGround();
                  boolean hc = mc.player.horizontalCollision;
                  this.blinking = true;
                  mc.player.connection.send(new Pos(target.getX() + dx * scale, target.getY() + dy * scale, target.getZ() + dz * scale, og, hc));
                  mc.gameMode.attack(mc.player, target);
                  if (this.swing.getValue()) {
                     mc.player.swing(InteractionHand.MAIN_HAND);
                  }

                  this.blinking = false;
                  mc.player.connection.send(new Pos(px, py, pz, og, hc));
                  this.lastAttackMs = now;
               }
            }
         }
      }
   }

   private Entity findTarget() {
      double range = this.maxRange.getValue();
      return StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), false)
         .filter(e -> e != mc.player)
         .filter(e -> e instanceof Player ? true : this.mobs.getValue() && e instanceof LivingEntity le && le.getHealth() > 0.0F)
         .filter(e -> mc.player.distanceTo(e) <= range)
         .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
         .orElse(null);
   }
}
