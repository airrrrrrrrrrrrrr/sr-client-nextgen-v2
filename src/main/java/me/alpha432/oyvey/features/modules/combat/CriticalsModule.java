package me.alpha432.oyvey.features.modules.combat;

import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

public class CriticalsModule extends Module {
   private final Setting<CriticalsModule.Mode> mode = this.register(new Setting<>("Mode", CriticalsModule.Mode.VULCAN));
   private final Setting<Integer> minDelay = this.register(new Setting<>("MinDelay", 0, 0, 500));
   private final Setting<Integer> maxDelay = this.register(new Setting<>("MaxDelay", 0, 0, 500));
   private final Setting<Double> hopHeight = this.register(
      new Setting<>(
         "HopHeight",
         0.0625,
         0.001,
         0.5,
         v -> this.mode.getValue() == CriticalsModule.Mode.DEATHZONE || this.mode.getValue() == CriticalsModule.Mode.DEATHZONE2
      )
   );
   private long nextCritTime = 0L;
   private int deathzoneHitCount = 0;

   public CriticalsModule() {
      super("Criticals", "Makes you do critical hits", Module.Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.deathzoneHitCount = 0;
      this.nextCritTime = 0L;
   }

   @Subscribe
   private void onPacketSend(PacketEvent.Send event) {
      if (event.getPacket() instanceof ServerboundAttackPacket packet) {
         long currentTime = System.currentTimeMillis();
         if (currentTime >= this.nextCritTime) {
            int min = this.minDelay.getValue();
            int max = this.maxDelay.getValue();
            if (max > 0) {
               int delay = max > min ? min + (int)(Math.random() * (max - min + 1)) : min;
               this.nextCritTime = currentTime + delay;
            } else {
               this.nextCritTime = 0L;
            }

            Entity entity = mc.level.getEntity(packet.entityId());
            if (entity != null && !(entity instanceof EndCrystal) && entity instanceof LivingEntity && !mc.player.isInWater() && !mc.player.isInLava()) {
               boolean onGround = mc.player.onGround();
               boolean bl = mc.player.horizontalCollision;
               double x = mc.player.getX();
               double y = mc.player.getY();
               double z = mc.player.getZ();
               switch ((CriticalsModule.Mode)this.mode.getValue()) {
                  case PACKET:
                     if (!onGround) {
                        return;
                     }

                     mc.player.connection.send(new Pos(x, y + 0.0625, z, false, bl));
                     mc.player.connection.send(new Pos(x, y, z, false, bl));
                     break;
                  case JUMP:
                     if (!onGround) {
                        return;
                     }

                     mc.player.connection.send(new Pos(x, y + 0.0625, z, false, bl));
                     mc.player.connection.send(new Pos(x, y, z, false, bl));
                     mc.player.connection.send(new Pos(x, y + 1.1E-5, z, false, bl));
                     mc.player.connection.send(new Pos(x, y, z, false, bl));
                     break;
                  case NCP:
                     if (!onGround) {
                        return;
                     }

                     mc.player.connection.send(new Pos(x, y + 0.41, z, false, bl));
                     mc.player.connection.send(new Pos(x, y + 0.1100013, z, false, bl));
                     mc.player.connection.send(new Pos(x, y + 1.3E-6, z, false, bl));
                     mc.player.connection.send(new Pos(x, y, z, true, bl));
                     break;
                  case VULCAN:
                     if (!onGround) {
                        return;
                     }

                     mc.player.connection.send(new Pos(x, y + 0.0784, z, false, bl));
                     mc.player.connection.send(new Pos(x, y, z, false, bl));
                     break;
                  case DEATHZONE:
                     if (!onGround) {
                        return;
                     }

                     this.deathzoneHitCount++;
                     if (this.deathzoneHitCount < 3) {
                        return;
                     }

                     this.deathzoneHitCount = 0;
                     mc.player.connection.send(new Pos(x, y + this.hopHeight.getValue(), z, false, bl));
                     mc.player.connection.send(new Pos(x, y, z, false, bl));
                     break;
                  case DEATHZONE2:
                     if (!onGround) {
                        return;
                     }

                     mc.player.connection.send(new Pos(x, y + this.hopHeight.getValue(), z, false, bl));
                     mc.player.connection.send(new Pos(x, y, z, false, bl));
               }

               mc.player.crit(entity);
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
      JUMP,
      NCP,
      VULCAN,
      DEATHZONE,
      DEATHZONE2;
   }
}
