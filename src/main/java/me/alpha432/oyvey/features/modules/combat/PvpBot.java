package me.alpha432.oyvey.features.modules.combat;

import java.util.concurrent.ThreadLocalRandom;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.movement.Scaffold;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.mixin.render.accessor.ClientInputAccessor;
import me.alpha432.oyvey.mixin.render.accessor.LocalPlayerRotationAccessor;
import me.alpha432.oyvey.util.CombatHitWindow;
import me.alpha432.oyvey.util.CombatPreSwing;
import me.alpha432.oyvey.util.KeyboardMovement;
import me.alpha432.oyvey.util.PvpCombo;
import me.alpha432.oyvey.util.PvpRotation;
import me.alpha432.oyvey.util.SilentRotationMath;
import me.alpha432.oyvey.util.TransmittedRotation;
import me.alpha432.oyvey.util.ViaProtocol;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Visible aim and real keyboard movement, with spacing and confirmed-hit sprint resets. */
public class PvpBot extends Module {
   public final Setting<Double> reach = this.register(new Setting<>("Reach", 3.0, 1.0, 6.0));
   public final Setting<Double> searchRange = this.register(new Setting<>("SearchRange", 16.0, 3.0, 32.0));
   public final Setting<Double> preHit = this.register(new Setting<>("PreHit", 4.0, 3.0, 10.0,
      "Begin air swings at this distance. Actual hits still require Reach."));
   public final Setting<Boolean> mobs = this.bool("Mobs", false);
   public final Setting<Double> stopDist = this.register(new Setting<>("StopDist", 2.6, 0.5, 4.0));
   public final Setting<Integer> minCps = this.num("MinCPS", 10, 1, 20);
   public final Setting<Integer> maxCps = this.num("MaxCPS", 14, 1, 20);
   public final Setting<Boolean> strafe = this.bool("Strafe", true);
   public final Setting<Boolean> sprintReset = this.bool("SprintReset", true);
   public final Setting<Boolean> edgeCheck = this.bool("EdgeCheck", true);
   public final Setting<Boolean> obstacleJump = this.bool("ObstacleJump", true);

   private final CombatHitWindow hitWindow = new CombatHitWindow();
   private final PvpCombo combo = new PvpCombo();
   private final CombatPreSwing preSwings = new CombatPreSwing();
   private final TransmittedRotation transmitted = new TransmittedRotation();
   private LivingEntity target;
   private LocalPlayer owner;
   private ClientLevel level;
   private PvpCombo.Move movement;
   private float aimYaw, aimPitch;
   private long nextAttack;
   private int resumeAt;
   private boolean ownsInput;
   private boolean turnedThisTick;
   private float jitterYaw, jitterPitch;

   public PvpBot() {
      super("PvpBot", "Tracks head aim, controls spacing, strafes and resets sprint after confirmed hits.", Category.COMBAT);
   }

   @Override public void onEnable() { resumeAt = 0; resetState(); }
   @Override public void onDisable() { releaseControl(); }

   private void resetState() {
      target = null; movement = null; owner = mc.player; level = mc.level;
      hitWindow.reset(); combo.reset(); nextAttack = 0; ownsInput = false;
      preSwings.reset(); turnedThisTick = false;
      transmitted.reset(mc.player == null ? 0 : mc.player.getYRot(), mc.player == null ? 0 : mc.player.getXRot());
   }

   private boolean available() {
      if (!isEnabled() || nullCheck() || mc.gameMode == null || mc.screen != null || !mc.player.isAlive()
         || mc.player.isSpectator() || mc.player.isPassenger() || mc.player.isFallFlying()
         || mc.player.getAbilities().flying || mc.player.tickCount < resumeAt
         || OyVey.recordedClickManager != null && OyVey.recordedClickManager.isRecording()) return false;
      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      BedNuker nuker = OyVey.moduleManager.getModuleByClass(BedNuker.class);
      return (scaffold == null || !scaffold.isEnabled()) && (aura == null || !aura.isEnabled())
         && (nuker == null || !nuker.isEnabled());
   }

   @Subscribe
   public void onWalking(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == Stage.PRE) {
         // Pitch can follow the actual post-movement eye height without changing physics yaw.
         if (turnedThisTick && available() && target != null && !mc.player.isInWater() && !mc.player.isInLava()) {
            Vec3 delta = aimPoint().subtract(mc.player.getEyePosition());
            float pitch = (float)-Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance()));
            aimPitch = SilentRotationMath.quantizeRotation(mc.player.getYRot(), mc.player.getXRot(), mc.player.getYRot(),
               Math.clamp(pitch+jitterPitch,-90,90), mc.options.sensitivity().get()).pitch();
            mc.player.setXRot(aimPitch);
         }
         return;
      }
      if (event.getStage() != Stage.MOTION_PRE) return;
      turnedThisTick = false;
      if (owner != mc.player || level != mc.level) { resumeAt = 0; resetState(); }
      if (!available()) { releaseControl(); return; }
      LivingEntity selected = valid(target) ? target : nearest();
      if (selected != target) {
         hitWindow.reset(); combo.reset(); transmitted.reset(mc.player.getYRot(), mc.player.getXRot());
         preSwings.reset();
         nextAttack = 0; target = selected;
      }
      if (target == null) { releaseControl(); return; }
      int tick = mc.player.tickCount;
      double distance = Math.sqrt(mc.player.distanceToSqr(target));
      Vec3 direction = target.position().subtract(mc.player.position()).normalize();
      double closing = mc.player.getDeltaMovement().subtract(target.getDeltaMovement()).dot(new Vec3(direction.x, 0, direction.z));
      double preferred = Math.min(stopDist.getValue(), Math.max(0.5, effectiveReach() - 0.35));
      movement = combo.decide(tick, target.getId(), distance, preferred, closing, mc.player.hurtTime,
         strafe.getValue(), sprintReset.getValue(), safeMove(0, 1), safeMove(0, -1));
      if (hitWindow.observe(tick, target.getId(), target.hurtTime)) {
         combo.confirmedHit(tick, sprintReset.getValue());
         movement = combo.decide(tick, target.getId(), distance, preferred, closing, mc.player.hurtTime,
            strafe.getValue(), sprintReset.getValue(), safeMove(0, 1), safeMove(0, -1));
      }
      // Attack before physics, using the look delivered on the preceding tick.
      attackIfReady();
      preSwing();
      Vec3 aim = aimPoint();
      Vec3 delta = aim.subtract(mc.player.getEyePosition());
      float wantedYaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
      float wantedPitch = (float)-Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance()));
      float yaw = mc.player.getYRot(), pitch = mc.player.getXRot();
      jitterYaw=ThreadLocalRandom.current().nextFloat(-1.25F,1.25F);
      jitterPitch=ThreadLocalRandom.current().nextFloat(-.65F,.65F);
      SilentRotationMath.Rotation look = PvpRotation.instant(yaw,pitch,wantedYaw,wantedPitch,jitterYaw,jitterPitch,mc.options.sensitivity().get());
      aimYaw = look.yaw(); aimPitch = look.pitch();
      mc.player.setYRot(aimYaw); mc.player.setXRot(aimPitch); mc.player.yHeadRot = aimYaw;
      turnedThisTick = true;
   }

   @Subscribe(priority = -1200)
   private void finishInstantCamera(UpdateWalkingPlayerEvent event) {
      if (event.getStage() != Stage.POST || !turnedThisTick || owner != mc.player || level != mc.level || mc.player == null) return;
      // Keep the visible camera snapped too; restoring the preceding render
      // angle would add interpolation after the instantaneous native turn.
      mc.player.setXRot(aimPitch);
      mc.player.yRotO = mc.player.getYRot();
      mc.player.xRotO = mc.player.getXRot();
      mc.player.yHeadRotO = mc.player.yRotO;
      turnedThisTick = false;
   }

   private Vec3 aimPoint() {
      return PvpRotation.trackingPoint(target.getBoundingBox(),target.getEyeY(),target.getDeltaMovement(),mc.player.getDeltaMovement());
   }

   private long clickDelayNanos() {
      int low = Math.min(minCps.getValue(), maxCps.getValue()), high = Math.max(minCps.getValue(), maxCps.getValue());
      return 1_000_000_000L / ThreadLocalRandom.current().nextInt(low, high + 1);
   }

   private void preSwing() {
      Vec3 eyes = mc.player.getEyePosition();
      AABB box = target.getBoundingBox();
      Vec3 nearest = new Vec3(Math.clamp(eyes.x, box.minX, box.maxX), Math.clamp(eyes.y, box.minY, box.maxY), Math.clamp(eyes.z, box.minZ, box.maxZ));
      boolean eligible = !mc.player.isUsingItem() && !mc.options.keyUse.isDown()
         && CombatPreSwing.inRange(eyes.distanceToSqr(nearest), effectiveReach(), preHit.getValue());
      int swings = preSwings.poll(eligible, target.getId(), clickDelayNanos(), java.util.List.of(), false, System.nanoTime());
      if (swings > 0) mc.player.swing(InteractionHand.MAIN_HAND);
   }

   private void attackIfReady() {
      long now = System.nanoTime();
      double range = effectiveReach();
      if (now < nextAttack || mc.player.isUsingItem() || mc.options.keyUse.isDown()
         || transmitted.targetId() != target.getId() || !ViaProtocol.usesSwordBlocking() && mc.player.getAttackStrengthScale(0) < 0.95F) return;
      Vec3 eyes = mc.player.getEyePosition();
      AABB box=target.getBoundingBox();
      Vec3 nearest=new Vec3(Math.clamp(eyes.x,box.minX,box.maxX),Math.clamp(eyes.y,box.minY,box.maxY),Math.clamp(eyes.z,box.minZ,box.maxZ));
      if (eyes.distanceToSqr(nearest)>range*range) return;
      float yaw = mc.player.getYRot(), pitch = mc.player.getXRot();
      try {
         mc.player.setYRot(transmitted.yaw()); mc.player.setXRot(transmitted.pitch());
         ((LocalPlayerRotationAccessor)mc.player).oyvey$sendSprintState();
         hitWindow.attack(mc.player.tickCount, target.getId(), target.hurtTime);
         mc.gameMode.attack(mc.player, target); mc.player.swing(InteractionHand.MAIN_HAND);
      } finally { mc.player.setYRot(yaw); mc.player.setXRot(pitch); }
      if (nextAttack == 0 || now - nextAttack > 250_000_000L) nextAttack = now;
      nextAttack += clickDelayNanos();
   }

   @Subscribe(priority = -1100)
   private void onMovePacket(PacketEvent.Send event) {
      if (event.getPacket() instanceof ServerboundMovePlayerPacket packet && mc.player != null) {
         transmitted.record(packet, event.isCancelled(), target != null && movement != null ? target.getId() : -1, aimYaw, aimPitch);
      }
   }

   public void applyInput(ClientInput input) {
      if (!available() || target == null || movement == null || !target.isAlive()) return;
      int forward = movement.forward(), sideways = movement.sideways();
      Vec3 aim = aimPoint().subtract(mc.player.getEyePosition());
      float wantedYaw = (float)Math.toDegrees(Math.atan2(aim.z, aim.x)) - 90;
      if (Math.abs(Mth.wrapDegrees(wantedYaw - mc.player.getYRot())) > 45) { forward = 0; sideways = 0; }
      if (!safeMove(forward, sideways)) {
         if (safeMove(forward, 0)) sideways = 0;
         else if (safeMove(0, sideways)) forward = 0;
         else { forward = 0; sideways = 0; }
      }
      boolean jump = input.keyPresses.jump() || obstacleJump.getValue() && mc.player.onGround()
         && mc.player.horizontalCollision && forward > 0 && canStepJump(forward, sideways);
      boolean sprint = movement.sprint() && forward > 0 && !input.keyPresses.shift() && !mc.player.isUsingItem();
      input.keyPresses = new Input(forward > 0, forward < 0, sideways > 0, sideways < 0, jump, input.keyPresses.shift(), sprint);
      ((ClientInputAccessor)input).oyvey$setMoveVector(KeyboardMovement.vector(SilentRotationMath.correctMovement(input.keyPresses, 0, 0)));
      if (!sprint) mc.player.setSprinting(false);
      ownsInput = true;
   }

   public boolean suppressesSprint() { return available() && movement != null && !movement.sprint(); }
   public boolean isControlling() { return available() && target != null && movement != null; }

   private Vec3 moveOffset(int forward, int sideways) {
      double length = Math.sqrt(forward * forward + sideways * sideways);
      if (length == 0) return Vec3.ZERO;
      double angle = Math.toRadians(mc.player.getYRot());
      return new Vec3((sideways * Math.cos(angle) - forward * Math.sin(angle)) / length * 0.45, 0,
         (forward * Math.cos(angle) + sideways * Math.sin(angle)) / length * 0.45);
   }

   private boolean safeMove(int forward, int sideways) {
      if (!edgeCheck.getValue() || mc.player == null || !mc.player.onGround() || forward == 0 && sideways == 0) return true;
      AABB destination = mc.player.getBoundingBox().move(moveOffset(forward, sideways));
      return !mc.level.noCollision(mc.player, destination.move(0, -0.65, 0));
   }

   private boolean canStepJump(int forward, int sideways) {
      AABB destination = mc.player.getBoundingBox().move(moveOffset(forward, sideways));
      return mc.level.noCollision(mc.player, destination.move(0, 1, 0))
         && !mc.level.noCollision(mc.player, destination.move(0, 0.8, 0));
   }

   private double effectiveReach() { return Math.min(reach.getValue(), mc.player.entityInteractionRange()); }

   private boolean valid(LivingEntity entity) {
      return entity != null && entity != mc.player && !entity.isRemoved() && entity.isAlive()
         && !entity.isInvisible() && (entity instanceof Player p && !p.isSpectator()
            && !OyVey.friendManager.isFriend(p) || mobs.getValue() && entity instanceof Mob)
         && mc.player.distanceToSqr(entity) <= Math.pow(Math.max(searchRange.getValue(), preHit.getValue()), 2)
         && (OyVey.recordedRotationManager == null || !OyVey.recordedRotationManager.isRecordingTarget(entity));
   }

   private LivingEntity nearest() {
      LivingEntity closest = null;
      double best = Double.POSITIVE_INFINITY;
      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof LivingEntity living && valid(living)) {
            double distance = mc.player.distanceToSqr(entity);
            if (distance < best) { best = distance; closest = living; }
         }
      }
      return closest;
   }

   private void releaseControl() {
      if (ownsInput && owner == mc.player && mc.player != null) mc.player.setSprinting(false);
      resetState();
   }

   public void onServerCorrection() {
      if (!isEnabled()) return;
      resetState(); resumeAt = mc.player == null ? 0 : mc.player.tickCount + 2;
   }

   @Override public String getDisplayInfo() {
      return target == null ? "Idle" : target.getName().getString() + " " + (movement == null ? "" : movement.phase().name().toLowerCase());
   }
}
