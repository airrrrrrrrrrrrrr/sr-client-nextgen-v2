package me.alpha432.oyvey.features.modules.combat;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.movement.Scaffold;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.RodLogic;
import me.alpha432.oyvey.util.ViaProtocol;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A combo opener driven by Recode's one shared targeting/rotation pipeline. */
public final class AutoRod extends Module {
   public final Setting<Double> range = register(new Setting<>("Range", 6.0, 3.0, 10.0));
   public final Setting<Double> minRange = register(new Setting<>("MinRange", 3.0, 0.0, 6.0));
   public final Setting<Integer> delay = register(new Setting<>("DelayMS", 350, 150, 2000));
   public final Setting<Boolean> prediction = register(new Setting<>("Prediction", true));
   private final RodLogic.Cycle cycle = new RodLogic.Cycle();
   private LocalPlayer owner;
   private ClientLevel level;
   private int rodSlot = -1, restoreSlot = -1, motionTarget = -1, motionTick = -1, castHurtTime;
   private Vec3 filteredMotion = Vec3.ZERO;
   private RodLogic.Shot shot;
   private RodLogic.Flight flight = RodLogic.Flight.MODERN;

   public AutoRod() { super("AutoRod", "Uses a predicted rod opener and returns to melee as the target closes", Category.COMBAT); }
   @Override public void onEnable() { abort(); }
   @Override public void onDisable() { abort(); }

   @Override public void onTick() {
      if (!sessionValid()) { abort(); return; }
      checkManualSlot();
   }

   private boolean sessionValid() {
      if (nullCheck() || mc.gameMode == null || mc.screen != null || !mc.player.isAlive() || mc.player.isSpectator()) return false;
      KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      return aura != null && aura.isEnabled() && (scaffold == null || !scaffold.ownsRotation())
         && (owner == null || owner == mc.player && level == mc.level);
   }

   /** Runs before Recode chooses aim, so nearby sword targets never inherit rod aim. */
   public void prepare(Entity target, boolean meleePriority) {
      if (!isEnabled()) return;
      if (!sessionValid()) { abort(); return; }
      if (owner == null) { owner = mc.player; level = mc.level; }
      flight = ViaProtocol.usesSwordBlocking() ? RodLogic.Flight.LEGACY : RodLogic.Flight.MODERN;
      checkManualSlot();
      int tick = mc.player.tickCount;
      if (!(target instanceof LivingEntity living) || target == mc.player || !target.isAlive() || target.isRemoved()
         || mc.level.getEntity(target.getId()) != target || mc.player.distanceTo(target) > range.getValue() + 1) {
         cycle.requestReturn(tick); shot = null; return;
      }
      if (cycle.active() && cycle.target() != target.getId()) {
         cycle.requestReturn(tick); shot = null; return;
      }
      updateMotion(target);
      var hook = mc.player.fishing;
      boolean contact = hook != null && (hook.getHookedIn() != null || hook.onGround() || hook.isInWater()
         || (cycle.age(tick) >= 2 && living.hurtTime > castHurtTime));
      int latencyTicks = Math.min(6, (latency() + 49) / 50 + 2);
      cycle.update(tick, hook != null, contact, meleePriority, Math.min(8, latencyTicks + 2));
      if (cycle.ready(tick)) {
         KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
         double distance = mc.player.distanceTo(target);
         if (meleePriority || hook != null || distance > range.getValue() || distance < Math.min(minRange.getValue(), range.getValue())
            || living.hurtTime > 2 || (mc.player.isUsingItem() && !aura.ownsAutoBlockUse()) || findRod() < 0) { shot = null; return; }
         cycle.begin(target.getId(), tick);
      }
      shot = cycle.phase() == RodLogic.Phase.AIMING
         ? RodLogic.solve(mc.player.getEyePosition(), target.getBoundingBox(), filteredMotion, prediction.getValue(), flight) : null;
   }

   private void updateMotion(Entity target) {
      if (motionTick == mc.player.tickCount && motionTarget == target.getId()) return;
      Vec3 measured = RodLogic.boundedMotion(new Vec3(target.getX() - target.xo, target.getY() - target.yo, target.getZ() - target.zo));
      filteredMotion = motionTarget != target.getId() ? measured : measured.scale(0.65).add(filteredMotion.scale(0.35));
      motionTarget = target.getId(); motionTick = mc.player.tickCount;
   }

   public Vec3 aimPoint(Entity target) {
      if (!aimingAt(target) || nullCheck()) return null;
      // Recalculate from the post-movement eye position as well as at targeting.
      shot = RodLogic.solve(mc.player.getEyePosition(), target.getBoundingBox(), filteredMotion, prediction.getValue(), flight);
      return shot.point();
   }
   public boolean aimingAt(Entity target) {
      return isEnabled() && cycle.phase() == RodLogic.Phase.AIMING && shot != null
         && target != null && target.getId() == cycle.target();
   }
   public boolean active() { return isEnabled() && cycle.active(); }
   public boolean blocksMelee() { return isEnabled() && cycle.blocksMelee(); }
   public double searchRange() { return isEnabled() && !nullCheck() && findRod() >= 0 ? range.getValue() : 0; }

   /** May retrieve and restore before Recode attacks in this same tick. */
   public void use(Entity target, boolean sentForTarget, float sentYaw, float sentPitch) {
      if (!active() || !sessionValid()) return;
      if (cycle.phase() == RodLogic.Phase.RETRIEVING) {
         try {
            if (ownsRod() && mc.player.fishing != null) useNative(sentYaw, sentPitch);
         } finally { restoreOwnedSlot(); cycle.finish(mc.player.tickCount, delayTicks()); shot = null; }
         return;
      }
      if (cycle.phase() != RodLogic.Phase.AIMING || !sentForTarget || target == null || target.getId() != cycle.target() || shot == null) return;
      if (mc.player.fishing != null) { restoreOwnedSlot(); shot = null; cycle.finish(mc.player.tickCount, delayTicks()); return; }
      Vec3 motion = prediction.getValue() ? filteredMotion : Vec3.ZERO;
      int intercept = RodLogic.intercept(mc.player.getEyePosition(), sentYaw, sentPitch, target.getBoundingBox(), motion, flight, this::clearPath);
      if (intercept < 0) return;
      int slot = findRod();
      if (slot < 0 || mc.player.getCooldowns().isOnCooldown(mc.player.getInventory().getItem(slot)) || mc.player.isUsingItem()) return;
      restoreSlot = mc.player.getInventory().getSelectedSlot(); rodSlot = slot;
      mc.player.getInventory().setSelectedSlot(slot);
      mc.gameMode.ensureHasSentCarriedItem();
      try {
         if (useNative(sentYaw, sentPitch)) {
            castHurtTime = target instanceof LivingEntity living ? living.hurtTime : 0;
            cycle.cast(mc.player.tickCount, intercept, (latency() + 49) / 50 + 2);
         } else { restoreOwnedSlot(); cycle.finish(mc.player.tickCount, 3); }
      } catch (RuntimeException failure) { abort(); throw failure; }
   }

   private boolean clearPath(Vec3 from, Vec3 to) {
      return mc.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)).getType() == HitResult.Type.MISS;
   }

   private boolean useNative(float yaw, float pitch) {
      float oldYaw = mc.player.getYRot(), oldPitch = mc.player.getXRot();
      try {
         mc.player.setYRot(yaw); mc.player.setXRot(pitch);
         var result = mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
         if (result.consumesAction()) mc.player.swing(InteractionHand.MAIN_HAND);
         return result.consumesAction();
      } finally { mc.player.setYRot(oldYaw); mc.player.setXRot(oldPitch); }
   }

   private boolean ownsRod() {
      return restoreSlot >= 0 && mc.player.getInventory().getSelectedSlot() == rodSlot
         && mc.player.getMainHandItem().getItem() instanceof FishingRodItem;
   }
   private void checkManualSlot() {
      if (restoreSlot >= 0 && !ownsRod()) {
         rodSlot = restoreSlot = -1; shot = null;
         cycle.finish(mc.player.tickCount, delayTicks());
      }
   }
   private int findRod() {
      int selected = mc.player.getInventory().getSelectedSlot();
      if (mc.player.getInventory().getItem(selected).getItem() instanceof FishingRodItem) return selected;
      for (int slot = 0; slot < 9; slot++) if (mc.player.getInventory().getItem(slot).getItem() instanceof FishingRodItem) return slot;
      return -1;
   }
   private int delayTicks() { return Math.max(1, (delay.getValue() + 49) / 50); }
   private int latency() {
      var info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(mc.player.getUUID());
      return info == null ? 0 : Math.max(0, info.getLatency());
   }
   private void restoreOwnedSlot() {
      int slot = mc.player == null ? -1 : RodLogic.restoreSlot(mc.player.getInventory().getSelectedSlot(), rodSlot, restoreSlot,
         mc.player.getMainHandItem().getItem() instanceof FishingRodItem, mc.player == owner && mc.level == level);
      if (slot >= 0) {
         mc.player.getInventory().setSelectedSlot(slot);
         if (mc.gameMode != null) mc.gameMode.ensureHasSentCarriedItem();
      }
      rodSlot = restoreSlot = -1;
   }
   public void abort() {
      restoreOwnedSlot(); cycle.reset(); owner = null; level = null; shot = null;
      filteredMotion = Vec3.ZERO; motionTarget = motionTick = -1;
   }
   @Override public String getDisplayInfo() { return cycle.active() ? cycle.phase().name() : null; }
}
