package me.alpha432.oyvey.features.modules.misc;

import java.util.HashSet;
import java.util.Set;
import me.alpha432.oyvey.event.impl.entity.DeathEvent;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class KillInsults extends Module {
   public final Setting<String> message = this.register(new Setting<>("Message", "cooked by sr client {name} get now!"));
   private final Set<Integer> recentlyAttacked = new HashSet<>();
   private final Set<Integer> announced = new HashSet<>();

   public KillInsults() {
      super("KillInsults", "Taunts players you kill in chat.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      this.recentlyAttacked.clear();
      this.announced.clear();
   }

   @Override
   public void onTick() {
      if (mc.level != null) {
         this.recentlyAttacked.removeIf(id -> mc.level.getEntity(id) == null);
         this.announced.removeIf(id -> {
            Entity e = mc.level.getEntity(id);
            return e == null ? true : e instanceof LivingEntity le && le.getHealth() > 0.0F;
         });
      }
   }

   @Subscribe
   private void onPacketSend(PacketEvent.Send event) {
      if (mc.player != null && mc.level != null && event.getPacket() instanceof ServerboundAttackPacket pkt) {
         Entity target = mc.level.getEntity(pkt.entityId());
         if (target instanceof Player && target != mc.player) {
            this.recentlyAttacked.add(pkt.entityId());
         }
      }
   }

   @Subscribe
   private void onDeath(DeathEvent event) {
      if (mc.player != null) {
         LivingEntity entity = event.getEntity();
         if (entity instanceof Player && entity != mc.player && this.recentlyAttacked.contains(entity.getId()) && !this.announced.contains(entity.getId())) {
            this.announced.add(entity.getId());
            this.recentlyAttacked.remove(entity.getId());
            String name = ((Player)entity).getGameProfile().name();
            String msg = this.message.getValue().replace("{name}", name);
            mc.player.connection.sendChat(msg);
         }
      }
   }
}
