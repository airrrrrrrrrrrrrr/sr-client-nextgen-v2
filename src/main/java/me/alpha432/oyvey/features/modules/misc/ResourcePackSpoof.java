package me.alpha432.oyvey.features.modules.misc;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.NotificationsModule;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket.Action;

public class ResourcePackSpoof extends Module {
   public ResourcePackSpoof() {
      super("ResourcePackSpoof", "Spoofs resource pack download responses to the server.", Module.Category.MISC);
   }

   @Subscribe
   private void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof ClientboundResourcePackPushPacket pkt) {
         event.cancel();
         if (mc.player != null) {
            UUID id = pkt.id();
            mc.player.connection.send(new ServerboundResourcePackPacket(id, Action.ACCEPTED));
            NotificationsModule.post("ResourcePackSpoof", "Downloading pack...", -21965, 3000L);
            Thread spoof = new Thread(() -> {
               try {
                  long downloadMs = 1000L + ThreadLocalRandom.current().nextLong(3000L);
                  Thread.sleep(downloadMs);
                  if (mc.player == null) {
                     return;
                  }

                  mc.player.connection.send(new ServerboundResourcePackPacket(id, Action.DOWNLOADED));
                  long reloadMs = 1000L + ThreadLocalRandom.current().nextLong(2000L);
                  Thread.sleep(reloadMs);
                  if (mc.player == null) {
                     return;
                  }

                  mc.player.connection.send(new ServerboundResourcePackPacket(id, Action.SUCCESSFULLY_LOADED));
                  NotificationsModule.post("ResourcePackSpoof", "Pack loaded", -11145558, 3000L);
               } catch (InterruptedException var5x) {
               }
            }, "ResourcePackSpoof-" + id);
            spoof.setDaemon(true);
            spoof.start();
         }
      }
   }
}
