package me.alpha432.oyvey.features.modules.misc;

import me.alpha432.oyvey.event.impl.entity.DeathEvent;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.RecentKillTracker;
import me.alpha432.oyvey.util.KillFeed;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class KillInsults extends Module {
   public final Setting<String> message = this.register(new Setting<>("Message", "cooked by sr client {name} get now!"));
   private final RecentKillTracker kills = new RecentKillTracker();
   private ClientLevel trackedLevel;

   public KillInsults() {
      super("KillInsults", "Taunts players you kill in chat.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      this.kills.clear();
      this.trackedLevel = mc.level;
   }

   @Override
   public void onDisable() { this.kills.clear(); this.trackedLevel = null; }

   private void checkWorld() {
      if (this.trackedLevel != mc.level) {
         this.kills.clear();
         this.trackedLevel = mc.level;
      }
   }

   @Override
   public void onTick() {
      this.checkWorld();
      this.kills.expire(System.currentTimeMillis());
   }

   @Subscribe(priority = -1000)
   private void onPacketSend(PacketEvent.Send event) {
      if (!event.isCancelled() && mc.player != null && mc.level != null && event.getPacket() instanceof ServerboundAttackPacket pkt) {
         ClientLevel level = mc.level;
         // Connection.sendPacket may run on Netty's event loop. Keep hit/death
         // state and all world access on the same client thread.
         mc.execute(() -> {
            if (!this.isEnabled() || mc.level != level) return;
            Entity target = level.getEntity(pkt.entityId());
            if (target instanceof Player player && target != mc.player && player.isAlive()) {
               this.checkWorld();
               this.kills.attack(pkt.entityId(), player.getUUID(), player.getGameProfile().name(), System.currentTimeMillis());
            }
         });
      }
   }

   @Subscribe
   private void onDeath(DeathEvent event) {
      this.onConfirmedDeath(event.getEntity());
   }

   @Subscribe(priority = -1000)
   private void onServerChat(PacketEvent.Receive event) {
      if (event.isCancelled() || mc.player == null || mc.level == null
         || !(event.getPacket() instanceof ClientboundSystemChatPacket chat) || chat.overlay()) return;
      var connection = mc.getConnection();
      String text = chat.content().getString();
      mc.execute(() -> {
         if (!this.isEnabled() || mc.getConnection() != connection || mc.player == null) return;
         this.checkWorld();
         KillFeed.victim(text, mc.player.getGameProfile().name()).ifPresent(victim ->
            this.sendKillMessage(this.kills.serverKill(victim, System.currentTimeMillis())));
      });
   }

   /** Called on the client thread after vanilla applies the server death event. */
   public void onConfirmedDeath(Entity entity) {
      if (!this.isEnabled() || mc.player == null || !(entity instanceof Player) || entity == mc.player) return;
      this.checkWorld();
      String name = this.kills.death(entity.getId(), entity.getUUID(), System.currentTimeMillis());
      this.sendKillMessage(name);
   }

   private void sendKillMessage(String name) {
      if (name == null) return;
      String msg = this.message.getValue().replace("{name}", name).replaceAll("[\\r\\n]", " ").strip();
      if (!msg.isEmpty()) mc.player.connection.sendChat(msg.substring(0, Math.min(256, msg.length())));
   }
}
