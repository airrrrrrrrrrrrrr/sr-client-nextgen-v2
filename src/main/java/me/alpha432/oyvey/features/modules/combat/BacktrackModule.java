package me.alpha432.oyvey.features.modules.combat;

import java.awt.Color;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.BacktrackHistory;
import me.alpha432.oyvey.util.CombatLatency;
import java.util.function.BiConsumer;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded client position history. The server remains authoritative for hits. */
public class BacktrackModule extends Module {
   public static BacktrackModule INSTANCE;
   public final Setting<Integer> delayMs = register(new Setting<>("DelayMS", 100, 0, 400));
   public final Setting<Double> maxRange = register(new Setting<>("MaxRange", 6.0, 3.0, 8.0));
   public final Setting<Boolean> smartOnly = register(new Setting<>("SmartMode", false));
   public final Setting<Boolean> ghostESP = register(new Setting<>("GhostESP", true));
   private final BacktrackHistory<ClientboundMoveEntityPacket> history = new BacktrackHistory<>();
   private final VecDeltaCodec serverPosition = new VecDeltaCodec();
   private LivingEntity target;
   private ClientLevel level;
   private ClientPacketListener connection;
   private boolean replaying;
   private long lastAttack;
   private final CombatLatency latency = new CombatLatency();
   private final ThreadLocal<Boolean> sendingLatencyReply = ThreadLocal.withInitial(() -> false);
   private ClientLevel latencyLevel;

   public BacktrackModule() {
      super("Backtrack", "Delays target movement and latency replies by DelayMS during combat", Category.COMBAT);
      INSTANCE = this;
   }

   private static long now() { return System.nanoTime() / 1_000_000L; }

   @Override public void onEnable() { clear(); latency.clear(); latencyLevel = null; }
   @Override public void onDisable() { flushAndReset(); latency.flush(); latency.clear(); latencyLevel = null; }
   @Override public void onUnload() { onDisable(); }

   public boolean deferLatencyReply(Connection owner, Packet<?> packet, Runnable send, BiConsumer<Runnable, Integer> schedule) {
      if (!isEnabled() || sendingLatencyReply.get() || latencyLevel != mc.level
         || !(packet instanceof ServerboundKeepAlivePacket || packet instanceof ServerboundPongPacket)) return false;
      return latency.delay(owner, now(), delayMs.getValue(), () -> {
         if (!owner.isConnected() || mc.getConnection() == null || mc.getConnection().getConnection() != owner
            || latencyLevel != mc.level) return;
         sendingLatencyReply.set(true);
         try { send.run(); }
         finally { sendingLatencyReply.remove(); }
      }, schedule);
   }

   @Subscribe private void onSend(PacketEvent.Send event) {
      if (!(event.getPacket() instanceof ServerboundInteractPacket packet) || packet.hand() != null) return;
      // Some senders can run on Netty. All world access/replay stays on the
      // client thread, using the connection/world captured by this attack.
      ClientLevel attackedLevel = mc.level;
      ClientPacketListener attackedConnection = mc.getConnection();
      Runnable select = () -> {
         if (!isEnabled() || mc.level != attackedLevel || mc.getConnection() != attackedConnection || nullCheck()) return;
         Entity entity = mc.level.getEntity(packet.entityId());
         if (!(entity instanceof LivingEntity living) || living == mc.player || !living.isAlive()) return;
         if (target != living || !sameWorld()) {
            flushAndReset();
            target = living; level = mc.level; connection = mc.getConnection();
            serverPosition.setBase(living.getPositionCodec().getBase());
            history.record(now(), serverPosition.getBase(), boxAt(serverPosition.getBase()));
         }
         lastAttack = now();
         latencyLevel = mc.level;
         latency.attack(attackedConnection.getConnection(), lastAttack);
      };
      if (mc.isSameThread()) select.run(); else mc.execute(select);
   }

   /** Called after vanilla's thread handoff, before it applies movement. */
   public boolean interceptMovement(ClientboundMoveEntityPacket packet) {
      if (!isEnabled() || replaying || target == null) return false;
      if (!sameWorld()) { clear(); return false; }
      if (packet.getEntity(level) != target) return false;
      long time = now();
      if (!validTarget(time) || delayMs.getValue() <= 0) { flushAndReset(); return false; }
      Vec3 position = serverPosition.getBase();
      if (packet.hasPosition()) position = serverPosition.decode(packet.getXa(), packet.getYa(), packet.getZa());
      // Discontinuous movement must be applied immediately and reset history.
      if (position.distanceToSqr(serverPosition.getBase()) > 64 || !Double.isFinite(position.lengthSqr())) {
         flushAndReset(); return false;
      }
      serverPosition.setBase(position);
      history.record(time, position, boxAt(position));
      boolean useful = !smartOnly.getValue() || distanceSquared(boxAt(position))
         >= distanceSquared(target.getBoundingBox()) - 0.0025;
      if (!useful || distanceSquared(boxAt(position)) > maxRange.getValue() * maxRange.getValue()) {
         history.flush(this::dispatch);
         return false;
      }
      history.enqueue(time, packet, this::dispatch);
      history.release(time, delayMs.getValue(), this::dispatch);
      return true;
   }

   /** Critical packets are never delayed; flush earlier movement before them. */
   public void beforeImmediate(Packet<?> packet) {
      if (!isEnabled() || replaying || target == null) return;
      if (!sameWorld()) { clear(); return; }
      boolean reset = packet instanceof ClientboundPlayerPositionPacket || packet instanceof ClientboundRespawnPacket
         || packet instanceof ClientboundExplodePacket
         || packet instanceof ClientboundTeleportEntityPacket p && p.id() == target.getId()
         || packet instanceof ClientboundEntityPositionSyncPacket p && p.id() == target.getId()
         || packet instanceof ClientboundSetEntityMotionPacket p && (p.id() == target.getId() || p.id() == mc.player.getId())
         || packet instanceof ClientboundRemoveEntitiesPacket p && p.getEntityIds().contains(target.getId());
      if (reset) flushAndReset();
   }

   @Override public void onTick() {
      if (mc.level != latencyLevel || mc.getConnection() == null) { latency.clear(); latencyLevel = null; }
      if (delayMs.getValue() == 0) latency.flush();
      if (!sameWorld()) { clear(); return; }
      if (target == null) return;
      if (!validTarget(now())) { flushAndReset(); return; }
      if (smartOnly.getValue() && distanceSquared(boxAt(serverPosition.getBase()))
         < distanceSquared(target.getBoundingBox()) - 0.0025) history.flush(this::dispatch);
      else history.release(now(), delayMs.getValue(), this::dispatch);
   }

   private boolean validTarget(long time) {
      return sameWorld() && target != null && target.isAlive() && !target.isRemoved() && mc.player.isAlive()
         && level.getEntity(target.getId()) == target && time - lastAttack <= 1500
         && distanceSquared(boxAt(serverPosition.getBase())) <= maxRange.getValue() * maxRange.getValue();
   }

   private boolean sameWorld() { return mc.player != null && level != null && mc.level == level && connection == mc.getConnection(); }
   private AABB boxAt(Vec3 position) { return target.getBoundingBox().move(position.subtract(target.position())); }
   private double distanceSquared(AABB box) {
      Vec3 eyes = mc.player.getEyePosition();
      double dx = Math.max(box.minX - eyes.x, Math.max(0, eyes.x - box.maxX));
      double dy = Math.max(box.minY - eyes.y, Math.max(0, eyes.y - box.maxY));
      double dz = Math.max(box.minZ - eyes.z, Math.max(0, eyes.z - box.maxZ));
      return dx * dx + dy * dy + dz * dz;
   }

   private void dispatch(ClientboundMoveEntityPacket packet) {
      if (!sameWorld() || level.getEntity(target.getId()) != target) return;
      replaying = true;
      try { packet.handle(connection); }
      finally { replaying = false; }
   }
   private void flushAndReset() { if (sameWorld()) history.flush(this::dispatch); clear(); }
   private void clear() { history.clear(); target = null; level = null; connection = null; replaying = false; lastAttack = 0; }

   // The rendered entity already follows the replayed history. Do not
   // temporarily teleport it or invent a backdated Minecraft attack packet.
   public AABB getBacktrackedBox(Entity entity) { return entity == null ? null : entity.getBoundingBox(); }
   public boolean applyBacktrack(Entity entity, Runnable attack) { boolean tracking = isTracking(entity); attack.run(); return tracking; }
   public boolean isTracking(Entity entity) { return isEnabled() && entity != null && entity == target && history.size() > 0 && sameWorld(); }
   public int getEffectiveDelayMs() { return history.age(now()); }
   public int getRawDelayMs() { return delayMs.getValue(); }
   public int getCachedLatencyMs() {
      var info = mc.player == null || mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(mc.player.getUUID());
      return info == null ? 0 : info.getLatency();
   }
   @Override public String getDisplayInfo() {
      return mc.getConnection() != null && latency.active(mc.getConnection().getConnection(), now())
         ? delayMs.getValue() + "ms" : target == null ? null : getEffectiveDelayMs() + "ms";
   }

   @Override public void onRender3D(Render3DEvent event) {
      if (!ghostESP.getValue() || !isTracking(target)) return;
      var sample = history.atOrBefore(now() - delayMs.getValue());
      int color = OyVey.colorManager.getColorAsInt();
      if (sample != null) {
         Color fill = new Color((color & 0xFFFFFF) | 0x26000000, true);
         RenderUtil.drawBoxFilled(event.getMatrix(), sample.box(), fill);
         RenderUtil.drawBox(event.getMatrix(), sample.box(), new Color(color | 0xDD000000, true), 1.5F);
      }
      // The faint second outline distinguishes the latest observed server
      // position from the historical position being replayed locally.
      RenderUtil.drawBox(event.getMatrix(), boxAt(serverPosition.getBase()),
         new Color((color & 0xFFFFFF) | 0x60000000, true), 1.0F);
   }
}
