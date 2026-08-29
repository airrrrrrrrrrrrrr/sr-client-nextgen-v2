package me.alpha432.oyvey.features.modules.combat;

import java.awt.Color;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class BacktrackModule extends Module {
   public static BacktrackModule INSTANCE;
   public final Setting<Integer> delayMs = this.register(new Setting<>("DelayMS", 100, 0, 400));
   public final Setting<Double> maxRange = this.register(new Setting<>("MaxRange", 6.0, 3.0, 8.0));
   public final Setting<Boolean> smartOnly = this.register(new Setting<>("SmartMode", true));
   public final Setting<Boolean> ghostESP = this.register(new Setting<>("GhostESP", true));
   private final ConcurrentLinkedQueue<BacktrackModule.TimedPacket> packetQueue = new ConcurrentLinkedQueue<>();
   private volatile LivingEntity target;
   private volatile Vec3 realServerPos;
   private long lastWorldLoad;
   private boolean isFlushing;
   private double renderX;
   private double renderY;
   private double renderZ;
   private double prevRX;
   private double prevRY;
   private double prevRZ;
   private boolean renderInit;
   private float renderAlpha;

   public BacktrackModule() {
      super("Backtrack", "Delays enemy position packets to extend reach.", Module.Category.COMBAT);
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.resetState();
   }

   @Override
   public void onDisable() {
      this.flushAll();
   }

   @Subscribe
   private void onReceive(PacketEvent.Receive event) {
      if (mc.player != null && mc.level != null && System.currentTimeMillis() - this.lastWorldLoad >= 1000L) {
         Packet<?> pkt = event.getPacket();
         if (pkt instanceof ClientboundPlayerPositionPacket || pkt instanceof ClientboundRespawnPacket) {
            this.flushAll();
            if (pkt instanceof ClientboundRespawnPacket) {
               this.lastWorldLoad = System.currentTimeMillis();
            }
         } else if (pkt instanceof ClientboundExplodePacket) {
            this.flushAll();
         } else {
            if (pkt instanceof ClientboundSetEntityMotionPacket p && p.id() == mc.player.getId()) {
               this.flushAll();
               return;
            }

            LivingEntity t = this.target;
            if (t != null && this.isTargetPacket(pkt, t)) {
               this.updateRealServerPos(pkt);
               event.cancel();
               long jitter = ThreadLocalRandom.current().nextInt(-5, 6);
               this.packetQueue.add(new BacktrackModule.TimedPacket(pkt, System.currentTimeMillis() + this.delayMs.getValue().intValue() + jitter));
            }
         }
      }
   }

   @Subscribe
   private void onSend(PacketEvent.Send event) {
      if (mc.player != null && mc.level != null && event.getPacket() instanceof ServerboundInteractPacket use && use.hand() == null) {
         Entity hit = mc.level.getEntity(use.entityId());
         if (hit instanceof LivingEntity le && hit != mc.player) {
            if (this.target != le) {
               this.flushAll();
               this.target = le;
               this.realServerPos = le.position();
               this.isFlushing = false;
               this.renderX = this.prevRX = le.getX();
               this.renderY = this.prevRY = le.getY();
               this.renderZ = this.prevRZ = le.getZ();
               this.renderInit = true;
            }

            return;
         }
      }
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.level != null) {
         LivingEntity t = this.target;
         Vec3 sp = this.realServerPos;
         if (t == null) {
            if (!this.packetQueue.isEmpty()) {
               this.isFlushing = true;
            }
         } else {
            if (this.smartOnly.getValue() && !(mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() == t)) {
               this.isFlushing = true;
            }

            if (!t.isAlive() || t.isRemoved()) {
               this.isFlushing = true;
               this.target = null;
            } else if (sp != null && mc.player.position().distanceTo(sp) > this.maxRange.getValue()) {
               this.isFlushing = true;
            }
         }

         this.processQueue();
         t = this.target;
         sp = this.realServerPos;
         if (t != null && sp != null) {
            double dx = !this.packetQueue.isEmpty() ? sp.x : t.getX();
            double dy = !this.packetQueue.isEmpty() ? sp.y : t.getY();
            double dz = !this.packetQueue.isEmpty() ? sp.z : t.getZ();
            if (!this.renderInit) {
               this.renderX = this.prevRX = dx;
               this.renderY = this.prevRY = dy;
               this.renderZ = this.prevRZ = dz;
               this.renderInit = true;
            } else {
               this.prevRX = this.renderX;
               this.prevRY = this.renderY;
               this.prevRZ = this.renderZ;
               double lerp = 0.15 + 0.35 * (1.0 - Math.min(1.0, this.packetQueue.size() / 25.0));
               this.renderX = this.renderX + (dx - this.renderX) * lerp;
               this.renderY = this.renderY + (dy - this.renderY) * lerp;
               this.renderZ = this.renderZ + (dz - this.renderZ) * lerp;
            }
         }

         this.renderAlpha = this.target != null ? Math.min(1.0F, this.renderAlpha + 0.12F) : Math.max(0.0F, this.renderAlpha - 0.08F);
      }
   }

   @Override
   public void onRender3D(Render3DEvent event) {
      if (this.ghostESP.getValue() && !(this.renderAlpha < 0.01F) && this.renderInit) {
         LivingEntity t = this.target;
         float pt = event.getDelta();
         double ix = this.prevRX + (this.renderX - this.prevRX) * pt;
         double iy = this.prevRY + (this.renderY - this.prevRY) * pt;
         double iz = this.prevRZ + (this.renderZ - this.prevRZ) * pt;
         double w = (t != null ? t.getBbWidth() : 0.6) / 2.0 + 0.05;
         double h = (t != null ? t.getBbHeight() : 1.8) + 0.1;
         AABB bb = new AABB(ix - w, iy, iz - w, ix + w, iy + h, iz + w);
         int base = OyVey.colorManager.getColorAsInt();
         int cR = base >> 16 & 0xFF;
         int cG = base >> 8 & 0xFF;
         int cB = base & 0xFF;
         Color fill = new Color(cR, cG, cB, (int)(this.renderAlpha * 38.0F));
         Color line = new Color(cR, cG, cB, (int)(this.renderAlpha * 230.0F));
         RenderUtil.drawBoxFilled(event.getMatrix(), bb, fill);
         RenderUtil.drawBox(event.getMatrix(), bb, line, 1.5F);
      }
   }

   public AABB getBacktrackedBox(Entity e) {
      return e != null ? e.getBoundingBox() : null;
   }

   public boolean applyBacktrack(Entity e, Runnable attack) {
      attack.run();
      return this.target != null && e == this.target;
   }

   public boolean isTracking(Entity e) {
      return e != null && e == this.target && !this.packetQueue.isEmpty();
   }

   public int getEffectiveDelayMs() {
      return this.delayMs.getValue();
   }

   public int getRawDelayMs() {
      return this.delayMs.getValue();
   }

   public int getCachedLatencyMs() {
      return 0;
   }

   @Override
   public String getDisplayInfo() {
      return this.target == null ? null : this.packetQueue.size() + "pk";
   }

   private void processQueue() {
      if (this.packetQueue.isEmpty()) {
         if (this.isFlushing && this.target == null) {
            this.isFlushing = false;
         }
      } else {
         long now = System.currentTimeMillis();
         int maxPerTick = this.isFlushing ? 50 : Math.max(8, this.packetQueue.size() / 2);

         for (int done = 0; !this.packetQueue.isEmpty() && done < maxPerTick; done++) {
            BacktrackModule.TimedPacket tp = this.packetQueue.peek();
            if (!this.isFlushing && now < tp.releaseTime) {
               break;
            }

            this.packetQueue.poll();
            this.dispatch(tp.packet);
         }
      }
   }

   private void flushAll() {
      BacktrackModule.TimedPacket tp;
      while ((tp = this.packetQueue.poll()) != null) {
         this.dispatch(tp.packet);
      }

      this.resetState();
   }

   private void dispatch(Packet<?> pkt) {
      if (mc.player != null && mc.player.connection != null) {
         try {
            ((Packet)pkt).handle(mc.player.connection);
         } catch (Exception var3) {
         }
      }
   }

   private void resetState() {
      this.target = null;
      this.realServerPos = null;
      this.isFlushing = false;
   }

   private void updateRealServerPos(Packet<?> pkt) {
      Vec3 pos = this.realServerPos;
      if (pos != null) {
         double x = pos.x;
         double y = pos.y;
         double z = pos.z;
         if (pkt instanceof ClientboundMoveEntityPacket p && p.hasPosition()) {
            x += p.getXa() / 4096.0;
            y += p.getYa() / 4096.0;
            z += p.getZa() / 4096.0;
         } else if (pkt instanceof ClientboundTeleportEntityPacket p) {
            Vec3 np = p.change().position();
            x = np.x;
            y = np.y;
            z = np.z;
         }

         this.realServerPos = new Vec3(x, y, z);
      }
   }

   private boolean isTargetPacket(Packet<?> pkt, LivingEntity t) {
      int id = t.getId();
      if (pkt instanceof ClientboundMoveEntityPacket p) {
         return p.getEntity(mc.level) == t;
      } else if (pkt instanceof ClientboundTeleportEntityPacket p) {
         return p.id() == id;
      } else if (pkt instanceof ClientboundEntityEventPacket p) {
         return p.getEntity(mc.level) == t;
      } else if (pkt instanceof ClientboundSetEntityMotionPacket p) {
         return p.id() == id;
      } else if (pkt instanceof ClientboundAnimatePacket p) {
         return p.getId() == id;
      } else {
         return pkt instanceof ClientboundSetEntityDataPacket p ? p.id() == id : false;
      }
   }

   private static final class TimedPacket {
      final Packet<?> packet;
      final long releaseTime;

      TimedPacket(Packet<?> p, long t) {
         this.packet = p;
         this.releaseTime = t;
      }
   }
}
