package me.alpha432.oyvey.features.modules.player;

import java.awt.Color;
import java.util.ArrayDeque;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FakeLagModule extends Module {
   public final Setting<Integer> minDelay = this.register(new Setting<>("MinDelay", 50, 0, 500));
   public final Setting<Integer> maxDelay = this.register(new Setting<>("MaxDelay", 150, 0, 500));
   public final Setting<Boolean> chokeServer = this.register(new Setting<>("ChokeServer", true));
   public final Setting<Boolean> showServerPos = this.register(new Setting<>("ShowServerPos", true));
   private final ArrayDeque<FakeLagModule.Queued> queue = new ArrayDeque<>();
   private volatile Vec3 serverPos = null;
   private volatile boolean releasing = false;

   public FakeLagModule() {
      super("FakeLag", "Delays outbound movement packets to simulate lag.", Module.Category.PLAYER);
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.serverPos = mc.player.position();
      }
   }

   @Override
   public void onDisable() {
      this.flushQueue();
      this.serverPos = null;
   }

   @Override
   public void onTick() {
      if (mc.player != null) {
         int min = this.minDelay.getValue();
         if (min <= 0) {
            this.flushQueue();
         } else {
            int max = this.maxDelay.getValue();
            long delay = max > min ? min + (long)(Math.random() * (max - min)) : min;
            long now = System.currentTimeMillis();
            this.releasing = true;

            try {
               while (!this.queue.isEmpty()) {
                  FakeLagModule.Queued head = this.queue.peekFirst();
                  if (now - head.queuedAtMs() < delay) {
                     break;
                  }

                  this.queue.pollFirst();
                  if (mc.player.connection != null) {
                     this.updateServerPos(head.packet());
                     mc.player.connection.send(head.packet());
                  }
               }
            } finally {
               this.releasing = false;
            }
         }
      }
   }

   @Override
   public void onRender3D(Render3DEvent event) {
      if (this.showServerPos.getValue()) {
         Vec3 sp = this.serverPos;
         if (sp != null && mc.player != null && !this.queue.isEmpty()) {
            AABB box = new AABB(sp.x - 0.3, sp.y, sp.z - 0.3, sp.x + 0.3, sp.y + 1.8, sp.z + 0.3);
            RenderUtil.drawBoxFilled(event.getMatrix(), box, new Color(255, 50, 50, 60));
            RenderUtil.drawBox(event.getMatrix(), box, new Color(255, 80, 80, 220), 1.5F);
         }
      }
   }

   public void queuePacket(Packet<?> packet) {
      if (packet != null) {
         this.queue.addLast(new FakeLagModule.Queued(packet, System.currentTimeMillis()));
      }
   }

   public boolean shouldQueuePacket(Packet<?> packet) {
      if (this.releasing) {
         return false;
      } else if (!this.chokeServer.getValue()) {
         return false;
      } else {
         return packet == null ? false : packet instanceof ServerboundMovePlayerPacket;
      }
   }

   private void updateServerPos(Packet<?> packet) {
      if (packet instanceof ServerboundMovePlayerPacket pkt && pkt.hasPosition()) {
         Vec3 current = this.serverPos;
         double fx = current != null ? current.x : 0.0;
         double fy = current != null ? current.y : 0.0;
         double fz = current != null ? current.z : 0.0;
         this.serverPos = new Vec3(pkt.getX(fx), pkt.getY(fy), pkt.getZ(fz));
      }
   }

   private void flushQueue() {
      if (mc.player != null && mc.player.connection != null) {
         this.releasing = true;

         FakeLagModule.Queued q;
         try {
            while ((q = this.queue.pollFirst()) != null) {
               this.updateServerPos(q.packet());
               mc.player.connection.send(q.packet());
            }
         } finally {
            this.releasing = false;
         }
      } else {
         this.queue.clear();
      }
   }

   private record Queued(Packet<?> packet, long queuedAtMs) {
   }
}
