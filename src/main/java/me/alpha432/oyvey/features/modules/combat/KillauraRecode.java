package me.alpha432.oyvey.features.modules.combat;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.commands.Command;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.movement.SprintModule;
import me.alpha432.oyvey.features.modules.movement.Scaffold;
import me.alpha432.oyvey.util.SilentRotationMath;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.manager.RecordedRotationManager;
import me.alpha432.oyvey.mixin.render.accessor.ClientInputAccessor;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.HitResult.Type;

public class KillauraRecode extends Module {
   public final Setting<Boolean> grim = this.register(new Setting<>("Grim", false, "Use conservative rotations, movement, reach, and attack checks for Grim servers."));
   public final Setting<Double> reach = this.register(new Setting<>("Reach", 3.0, 1.0, 6.0));
   public final Setting<Double> searchRange = this.register(new Setting<>("SearchRange", 3.0, 1.0, 30.0));
   public final Setting<Boolean> mobs = this.register(new Setting<>("Mobs", false));
   public final Setting<Boolean> requireArmor = this.register(new Setting<>("RequireArmor", false));
   public final Setting<Boolean> respectFriends = this.register(new Setting<>("RespectFriends", true));
   public final Setting<Boolean> wallCheck = this.register(new Setting<>("WallCheck", true));
   public final Setting<Boolean> raycast = this.register(new Setting<>("Raycast", true));
   public final Setting<KillauraRecode.TargetMode> targetMode = this.register(new Setting<>("TargetMode", KillauraRecode.TargetMode.CLOSEST));
   public final Setting<Boolean> cooldown = this.register(new Setting<>("1.9Cooldown", true));
   public final Setting<Boolean> onlyCrits = this.register(new Setting<>("OnlyCrits", false));
   public final Setting<Boolean> apsRand = this.register(new Setting<>("RandAPS", true));
   public final Setting<Integer> apsMin = this.register(new Setting<>("APSMin", 9, 1, 20, v -> this.apsRand.getValue()));
   public final Setting<Integer> apsMax = this.register(new Setting<>("APSMax", 14, 1, 20, v -> this.apsRand.getValue()));
   public final Setting<Integer> aps = this.register(new Setting<>("APS", 12, 1, 20, v -> !this.apsRand.getValue()));
   public final Setting<KillauraRecode.RotationMode> rotMode = this.register(new Setting<>("RotationMode", KillauraRecode.RotationMode.SMOOTH));
   public final Setting<Float> rotSpeed = this.register(
      new Setting<>("RotSpeed", 3.5F, 0.5F, 15.0F, v -> this.grim.getValue() || this.rotMode.getValue() == KillauraRecode.RotationMode.SMOOTH)
   );
   public final Setting<Float> predictTicks = this.register(
      new Setting<>("PredictTicks", 1.5F, 0.0F, 4.0F, v -> this.rotMode.getValue() == KillauraRecode.RotationMode.PREDICT)
   );
   public final Setting<Float> advRotMin = this.register(
      new Setting<>(
         "MinRotSpeed",
         2.0F,
         0.5F,
         15.0F,
         v -> this.rotMode.getValue() == KillauraRecode.RotationMode.ADVANCED || this.rotMode.getValue() == KillauraRecode.RotationMode.RECORDED
      )
   );
   public final Setting<Float> advRotMax = this.register(
      new Setting<>(
         "MaxRotSpeed",
         6.0F,
         0.5F,
         15.0F,
         v -> this.rotMode.getValue() == KillauraRecode.RotationMode.ADVANCED || this.rotMode.getValue() == KillauraRecode.RotationMode.RECORDED
      )
   );
   public final Setting<Float> jitter = this.register(
      new Setting<>("Jitter", 15.0F, 0.0F, 100.0F, v -> this.rotMode.getValue() == KillauraRecode.RotationMode.ADVANCED)
   );
   public final Setting<Float> noise = this.register(
      new Setting<>("Noise", 20.0F, 0.0F, 100.0F, v -> this.rotMode.getValue() == KillauraRecode.RotationMode.ADVANCED)
   );
   public final Setting<Float> chance = this.register(
      new Setting<>("Chance", 20.0F, 1.0F, 100.0F, v -> this.rotMode.getValue() == KillauraRecode.RotationMode.ADVANCED)
   );
   public final Setting<Boolean> style = this.register(new Setting<>("Style", false));
   public final Setting<Float> styleSpeed = this.register(new Setting<>("StyleSpeed", 30.0F, 5.0F, 120.0F, v -> this.style.getValue()));
   public final Setting<Boolean> gcdFix = this.register(new Setting<>("GCDFix", true));
   public final Setting<Boolean> correctMove = this.register(new Setting<>("CorrectMove", true));
   public final Setting<KillauraRecode.MoveFix> moveFix = this.register(new Setting<>("MoveFix", KillauraRecode.MoveFix.GRIM));
   public final Setting<KillauraRecode.BlockMode> blockMode = this.register(new Setting<>("AutoBlock", KillauraRecode.BlockMode.OFF));
   public final Setting<Boolean> fakeAutoBlock = this.register(new Setting<>("FakeAutoBlock", false));
   public final Setting<Boolean> showBlockCooldown = this.register(new Setting<>("ShowCooldown", true,
      v -> this.fakeAutoBlock.getValue() || this.blockMode.getValue() == BlockMode.FAKE));
   public final Setting<Boolean> shieldBreak = this.register(new Setting<>("AutoShieldBreak", false));
   public final Setting<Boolean> shieldSwapBack = this.register(new Setting<>("ShieldSwapBack", true, v -> this.shieldBreak.getValue()));
   public final Setting<Boolean> armSwing = this.register(new Setting<>("ArmSwing", true));
   public final Setting<Boolean> multi = this.register(new Setting<>("MultiAura", false));
   public final Setting<Boolean> switchMode = this.register(new Setting<>("SwitchAura", false));
   public final Setting<Integer> switchDelay = this.register(new Setting<>("SwitchDelay", 600, 50, 3000, v -> this.switchMode.getValue()));
   public final Setting<Boolean> hitSelect = this.register(new Setting<>("HitSelect", false));
   public final Setting<Integer> hitSelectDelayMin = this.register(new Setting<>("HitSelectDelayMin", 0, 0, 500, v -> this.hitSelect.getValue()));
   public final Setting<Integer> hitSelectDelayMax = this.register(new Setting<>("HitSelectDelayMax", 60, 0, 500, v -> this.hitSelect.getValue()));
   public final Setting<Double> hitSelectRange = this.register(new Setting<>("HitSelectRange", 6.0, 1.0, 20.0, v -> this.hitSelect.getValue()));
   public final Setting<Boolean> requireTargetHurtTime = this.register(new Setting<>("RequireHurtTime", true, v -> this.hitSelect.getValue()));
   public final Setting<Integer> targetHurtTimeMin = this.register(
      new Setting<>("TargetHurtTimeMin", 0, 0, 20, v -> this.hitSelect.getValue() && this.requireTargetHurtTime.getValue())
   );
   public final Setting<Integer> targetHurtTimeMax = this.register(
      new Setting<>("TargetHurtTimeMax", 3, 0, 20, v -> this.hitSelect.getValue() && this.requireTargetHurtTime.getValue())
   );
   public final Setting<Boolean> antiBot = this.register(new Setting<>("AntiBot", true));
   public final Setting<Float> botSpinThresh = this.register(new Setting<>("BotSpinDeg", 60.0F, 20.0F, 180.0F, v -> this.antiBot.getValue()));
   public static volatile boolean isRotating = false;
   public static volatile float visualYaw = 0.0F;
   public static volatile float visualPitch = 0.0F;
   private float smoothYaw = 0.0F;
   private float smoothPitch = 0.0F;
   private float sentYaw = 0.0F;
   private float sentPitch = 0.0F;
   private boolean hasSentRot = false;
   private int sentTargetId = -1;
   private float[] targetRots = null;
   private float savedYaw = 0.0F;
   private float savedPitch = 0.0F;
   private boolean hasSaved = false;
   private float jitterPhase = 0.0F;
   private float jitterOffYaw = 0.0F;
   private float jitterOffPitch = 0.0F;
   private float aimVertBias = -0.08F;
   private float aimVertTarget = -0.08F;
   private float styleSpin = 0.0F;
   private boolean styleAtTarget = false;
   private final Map<Integer, float[]> botTracker = new HashMap<>();
   private Entity currentTarget = null;
   private long lastAttackTime = 0L;
   private long nextAttackDelay = 0L;
   private long lastSwitchTime = 0L;
   private int switchIndex = 0;
   private long hsHoldUntil = 0L;
   private boolean autoBlockActive = false;
   private boolean serverBlockHeld;
   private int serverBlockSlot = -1;
   private int shieldSwapPrev = -1;
   private int recordedPatternIndex = 0;
   private float recordedReplaySpeed = 0.1F;
   private float recordedYawError = 0.0F;
   private float recordedPitchError = 0.0F;
   private KillauraRecode.RotationMode lastRotationMode = null;
   private boolean grimSprintAllowed = true;

   public KillauraRecode() {
      super("KillauraRecode", "Advanced KillAura with smooth rotations, prediction, and wall checks.", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      this.botTracker.clear();
      this.targetRots = null;
      this.hasSaved = false;
      this.hasSentRot = false;
      this.sentTargetId = -1;
      this.currentTarget = null;
      this.switchIndex = 0;
      this.lastSwitchTime = 0L;
      this.nextAttackDelay = this.rollDelay();
      this.autoBlockActive = false;
      this.serverBlockHeld = false;
      this.serverBlockSlot = -1;
      this.jitterPhase = 0.0F;
      this.jitterOffYaw = 0.0F;
      this.jitterOffPitch = 0.0F;
      this.aimVertBias = -0.08F;
      this.aimVertTarget = -0.08F;
      this.styleSpin = 0.0F;
      this.styleAtTarget = false;
      this.hsHoldUntil = 0L;
      this.recordedPatternIndex = 0;
      this.recordedReplaySpeed = 0.1F;
      this.recordedYawError = 0.0F;
      this.recordedPitchError = 0.0F;
      this.lastRotationMode = null;
      this.grimSprintAllowed = true;
      if (mc.player != null) {
         this.smoothYaw = mc.player.getYRot();
         this.smoothPitch = mc.player.getXRot();
         this.sentYaw = this.smoothYaw;
         this.sentPitch = this.smoothPitch;
      }
   }

   @Override
   public void onDisable() {
      isRotating = false;
      this.stopServerBlock();
      if (this.hasSaved && mc.player != null) {
         OyVey.rotationManager.setPlayerRotations(this.savedYaw, this.savedPitch);
      }
      if (mc.player != null) {
         this.finishSilentRotation();
      }

      if (mc.player != null && mc.gameMode != null && this.autoBlockActive && mc.player.isUsingItem()) {
         mc.gameMode.releaseUsingItem(mc.player);
      }

      this.autoBlockActive = false;
      this.hasSaved = false;
      this.hasSentRot = false;
      this.sentTargetId = -1;
      this.targetRots = null;
      this.currentTarget = null;
      this.grimSprintAllowed = true;
      this.restoreShieldBreakSwap();
      OyVey.rotationManager.clearServerRotations();
   }

   @Subscribe
   public void onUpdateWalkingPlayer(UpdateWalkingPlayerEvent event) {
      if (mc.level != null && mc.player != null) {
         Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
         if (scaffold != null && scaffold.ownsRotation()) {
            // One rotation must drive physics, input and serialization for a tick.
            // Scaffold has priority while lining up a placement.
            isRotating = false;
            this.stopServerBlock();
            this.hasSaved = false;
            this.hasSentRot = false;
            this.sentTargetId = -1;
            this.targetRots = null;
            this.currentTarget = null;
            return;
         }
         if (event.getStage() == Stage.MOTION_PRE) {
            this.updateTargeting();
            if (this.targetRots != null) {
               visualYaw = mc.player.getYRot();
               visualPitch = mc.player.getXRot();
               isRotating = true;
               this.savedYaw = visualYaw;
               this.savedPitch = visualPitch;
               this.hasSaved = true;
            } else {
               isRotating = false;
               this.hasSaved = false;
            }

            // Attacks can slow velocity and stop sprinting. Apply those effects
            // before vanilla moves, matching the attack packet's place before
            // this tick's movement packet in the server's simulation.
            this.updateCombat();
            if (this.targetRots != null && this.isGrimMovementCorrectionConfigured()) {
               OyVey.rotationManager.setPlayerRotations(this.targetRots[0], this.targetRots[1]);
            }
         } else if (event.getStage() == Stage.POST) {
            isRotating = false;
            if (this.hasSaved && mc.player != null) {
               OyVey.rotationManager.setPlayerRotations(this.savedYaw, this.savedPitch);
            }

            if (this.targetRots != null) {
               this.sentYaw = this.targetRots[0];
               this.sentPitch = this.targetRots[1];
               this.hasSentRot = true;
               this.sentTargetId = this.currentTarget == null ? -1 : this.currentTarget.getId();
            } else {
               this.hasSentRot = false;
               this.sentTargetId = -1;
            }

            this.hasSaved = false;
         } else if (event.getStage() == Stage.PRE) {
            // Rotation serialization must not change movement or sprint state
            // after vanilla has already calculated this tick's displacement.
            if (this.targetRots != null && this.currentTarget != null) {
               OyVey.rotationManager.setPlayerRotations(this.targetRots[0], this.targetRots[1]);
            }
         }
      }
   }

   private void updateCombat() {
      // Restore carried items and update use-item state before physics too.
      this.restoreShieldBreakSwap();

      KillauraRecode.BlockMode bm = this.grim.getValue()
         || this.fakeAutoBlock.getValue() && !isFullBlockMode(this.blockMode.getValue())
         ? KillauraRecode.BlockMode.OFF : this.blockMode.getValue();
      if (!isFullBlockMode(bm)) {
         this.stopServerBlock();
      }
      if (this.targetRots != null && this.currentTarget != null) {
         if (isFullBlockMode(bm)) {
            this.updateServerBlock(false);
         }
         boolean ownedSwordBlock = this.serverBlockHeld
            && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND
            && mc.player.getMainHandItem().is(ItemTags.SWORDS);
         boolean attackReady = (!mc.player.isUsingItem() || ownedSwordBlock) && this.shouldAttack();
         if (attackReady) {
            if (!this.grim.getValue() && !isFullBlockMode(bm)) {
               this.tryShieldBreakSwap();
            }
            if (bm == BlockMode.FULL2) {
               this.updateServerBlock(true);
            }
            // canAttackNow validates the preceding tick's transmitted rotation.
            this.performAttack(this.currentTarget);
            if (bm == BlockMode.FULL2) {
               // Keep use/attack/use together before physics.
               this.updateServerBlock(true);
            }
            SprintModule.lastAttackMs = System.currentTimeMillis();
            this.lastAttackTime = System.currentTimeMillis();
            this.nextAttackDelay = this.rollDelay();
            if (this.hitSelect.getValue()) {
               this.hsHoldUntil = System.currentTimeMillis() + this.rollHsDelay();
            }

            if (!this.grim.getValue() && this.multi.getValue()) {
               this.switchIndex++;
            }
         } else {
            this.updateAutoBlock(bm);
         }
      } else {
         this.stopServerBlock();
         if (this.autoBlockActive && mc.player.isUsingItem()) {
            mc.gameMode.releaseUsingItem(mc.player);
            this.autoBlockActive = false;
         }
      }
   }

   private void updateTargeting() {
      if (this.grim.getValue()) {
         this.switchIndex = 0;
      }
      this.notifyRecordedMode();
      if (!this.grim.getValue() && this.rotMode.getValue() == KillauraRecode.RotationMode.RECORDED && !OyVey.recordedRotationManager.hasPattern()) {
         this.currentTarget = null;
         this.targetRots = null;
         this.finishSilentRotation();
         this.recordedPatternIndex = 0;
         this.resetRecordedPlayback();
         this.smoothYaw = mc.player.getYRot();
         this.smoothPitch = mc.player.getXRot();
         OyVey.rotationManager.clearServerRotations();
         return;
      }

      List<Entity> targets = this.getTargets();
      if (targets.isEmpty()) {
         this.currentTarget = null;
         this.targetRots = null;
         this.finishSilentRotation();
         this.switchIndex = 0;
         this.recordedPatternIndex = 0;
         this.resetRecordedPlayback();
         OyVey.rotationManager.clearServerRotations();
      } else {
         int previousTargetId = this.currentTarget == null ? -1 : this.currentTarget.getId();
         if (this.switchIndex >= targets.size()) {
            this.switchIndex = 0;
         }

         if (!this.grim.getValue()
            && this.switchMode.getValue()
            && !this.multi.getValue()
            && targets.size() > 1
            && System.currentTimeMillis() - this.lastSwitchTime >= this.switchDelay.getValue().intValue()) {
            this.switchIndex = (this.switchIndex + 1) % targets.size();
            this.lastSwitchTime = System.currentTimeMillis();
         }

         this.currentTarget = !this.grim.getValue() && (this.switchMode.getValue() || this.multi.getValue()) && targets.size() > 1
            ? targets.get(this.switchIndex)
            : targets.get(0);
         if (this.currentTarget.getId() != previousTargetId) {
            this.recordedPatternIndex = 0;
            this.resetRecordedPlayback();
         }

         if (!this.hasSentRot) {
            this.sentYaw = mc.player.getYRot();
            this.sentPitch = mc.player.getXRot();
         }

         this.targetRots = this.buildRotations(this.computeAimPoint(this.currentTarget));
         OyVey.rotationManager.setServerRotations(this.targetRots[0], this.targetRots[1]);
      }
   }

   private void finishSilentRotation() {
      if (this.hasSentRot) {
         // Preserve the continuous yaw representation used by the last movement
         // packet. Returning directly to an equivalent +/-180 visual angle can
         // otherwise create a 320+ degree delta and trigger AimModulo360.
         float alignedYaw = this.sentYaw + Mth.wrapDegrees(mc.player.getYRot() - this.sentYaw);
         mc.player.setYRot(alignedYaw);
         mc.player.yHeadRot = alignedYaw;
         this.sentYaw = alignedYaw;
      }

      this.smoothYaw = mc.player.getYRot();
      this.smoothPitch = mc.player.getXRot();
      this.sentPitch = this.smoothPitch;
      this.hasSentRot = false;
      this.sentTargetId = -1;
   }

   private void notifyRecordedMode() {
      if (this.grim.getValue()) {
         this.lastRotationMode = null;
         return;
      }
      KillauraRecode.RotationMode current = this.rotMode.getValue();
      if (current == this.lastRotationMode) {
         return;
      }

      this.lastRotationMode = current;
      if (current == KillauraRecode.RotationMode.RECORDED) {
         if (OyVey.recordedRotationManager.hasPattern()) {
            Command.sendMessage(
               "{yellow} Recorded rotations are using {white}%s{yellow} target-relative samples. Press {white}H{yellow} to replace them.",
               "rot-record",
               OyVey.recordedRotationManager.getPatternSize()
            );
         } else {
            Command.sendMessage(
               "{yellow} Recorded mode needs a target-relative pattern. Press {white}H{yellow}, aim at the client-only dummy, then press H again.",
               "rot-record"
            );
         }
      }
   }

   private List<Entity> getTargets() {
      double r = this.searchRange.getValue();
      this.botTracker.keySet().removeIf(id -> {
         Entity e = mc.level.getEntity(id);
         return e == null || !e.isAlive() || mc.player.distanceTo(e) > r + 4.0;
      });
      List<Entity> list = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), false)
         .filter(e -> e != mc.player && e.isAlive())
         .filter(e -> e instanceof Player || this.mobs.getValue() && e instanceof Mob)
         .filter(e -> !this.isFriend(e))
         .filter(this::isWithinSearchRange)
         .filter(e -> !this.isBot(e))
         .filter(e -> !this.requireArmor.getValue() ? true : e instanceof LivingEntity le && le.getArmorValue() > 0)
         .filter(e -> !(this.grim.getValue() || this.wallCheck.getValue()) || this.hasLineOfSight(e))
         .collect(Collectors.toList());
      list.sort(this.buildComparator());
      return list;
   }

   private Comparator<Entity> buildComparator() {
      return switch ((KillauraRecode.TargetMode)this.targetMode.getValue()) {
         case LOWEST_HP -> Comparator.comparingDouble(e -> e instanceof LivingEntity le ? le.getHealth() : 3.4028234663852886E38);
         case HIGHEST_HP -> Comparator.comparingDouble(e -> e instanceof LivingEntity le ? -le.getHealth() : -3.4028234663852886E38);
         case ANGLE -> Comparator.comparingDouble(this::crosshairAngle);
         default -> Comparator.comparingDouble(e -> mc.player.distanceTo(e));
      };
   }

   private double crosshairAngle(Entity e) {
      float[] r = this.rawRotation(e.getX(), e.getY() + e.getEyeHeight() * 0.6, e.getZ());
      float dYaw = Math.abs(Mth.wrapDegrees(r[0] - mc.player.getYRot()));
      float dPitch = Math.abs(r[1] - mc.player.getXRot());
      return Math.sqrt(dYaw * dYaw + dPitch * dPitch);
   }

   private boolean isFriend(Entity e) {
      if (!this.respectFriends.getValue()) {
         return false;
      } else {
         return e instanceof Player p ? OyVey.friendManager.isFriend(p) : false;
      }
   }

   private boolean isBot(Entity e) {
      if (!this.antiBot.getValue()) {
         return false;
      }

      float[] data = this.botTracker.computeIfAbsent(e.getId(), id -> new float[]{e.getYRot(), 0.0F, 0.0F});
      float delta = Math.abs(Mth.wrapDegrees(e.getYRot() - data[0]));
      data[0] = e.getYRot();
      data[1] += delta;
      data[2]++;
      return data[2] < 10.0F ? false : data[1] / data[2] > this.botSpinThresh.getValue();
   }

   private boolean hasLineOfSight(Entity target) {
      Vec3 eyes = mc.player.getEyePosition();
      AABB box = target.getBoundingBox();
      if (this.rayMiss(eyes, box.getCenter())) {
         return true;
      }

      double[] xs = new double[]{box.minX, box.maxX};
      double[] ys = new double[]{box.minY + 0.1, box.maxY - 0.1};
      double[] zs = new double[]{box.minZ, box.maxZ};

      for (double x : xs) {
         for (double y : ys) {
            for (double z : zs) {
               if (this.rayMiss(eyes, new Vec3(x, y, z))) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean rayMiss(Vec3 from, Vec3 to) {
      return mc.level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, mc.player)).getType() == Type.MISS;
   }

   private boolean rotationHitsTarget(Entity target, float yaw, float pitch) {
      Vec3 eyes = mc.player.getEyePosition();
      Vec3 look = Vec3.directionFromRotation(pitch, yaw);
      Vec3 end = eyes.add(look.scale(this.effectiveReach()));
      Optional<Vec3> hit = target.getBoundingBox().clip(eyes, end);
      if (hit.isEmpty()) {
         return false;
      } else {
         return !(this.grim.getValue() || this.wallCheck.getValue())
            ? true
            : mc.level.clip(new ClipContext(eyes, hit.get(), Block.COLLIDER, Fluid.NONE, mc.player)).getType() == Type.MISS;
      }
   }

   private double effectiveReach() {
      double serverRange = mc.player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
      double configuredReach = this.grim.getValue() ? Math.min(this.reach.getValue(), 3.0) : this.reach.getValue();
      return Math.max(0.0, Math.min(configuredReach, serverRange));
   }

   private boolean isWithinSearchRange(Entity target) {
      if (target == null || target.isRemoved()) {
         return false;
      }

      double range = this.searchRange.getValue();
      return this.distanceToHitboxSqr(target) <= range * range;
   }

   private boolean isWithinAttackRange(Entity target) {
      if (target == null || target.isRemoved()) {
         return false;
      }

      double effectiveReach = this.effectiveReach();
      return this.distanceToHitboxSqr(target) <= effectiveReach * effectiveReach
         && mc.player.isWithinAttackRange(mc.player.getMainHandItem(), target.getBoundingBox(), 0.0);
   }

   private double distanceToHitboxSqr(Entity target) {
      AABB box = target.getBoundingBox();
      Vec3 eyes = mc.player.getEyePosition();
      double closestX = Mth.clamp(eyes.x, box.minX, box.maxX);
      double closestY = Mth.clamp(eyes.y, box.minY, box.maxY);
      double closestZ = Mth.clamp(eyes.z, box.minZ, box.maxZ);
      return eyes.distanceToSqr(closestX, closestY, closestZ);
   }

   private float[] computeAimPoint(Entity target) {
      double tx = target.getX();
      double ty = target.getY() + target.getEyeHeight() * 0.6;
      double tz = target.getZ();
      if (this.grim.getValue()) {
         return this.rawRotation(tx, ty, tz);
      }
      if (this.rotMode.getValue() == KillauraRecode.RotationMode.PREDICT) {
         float ticks = this.predictTicks.getValue();
         tx += target.getDeltaMovement().x * ticks;
         ty += target.getDeltaMovement().y * ticks;
         tz += target.getDeltaMovement().z * ticks;
      }

      if (this.rotMode.getValue() == KillauraRecode.RotationMode.ADVANCED) {
         float chanceF = this.chance.getValue();
         float updateRate = chanceF / 100.0F * 0.25F;
         if (ThreadLocalRandom.current().nextFloat() < updateRate) {
            float amplitude = chanceF / 100.0F * 0.5F;
            this.aimVertTarget = -0.08F + (ThreadLocalRandom.current().nextFloat() * 2.0F - 1.0F) * amplitude;
            this.aimVertTarget = Mth.clamp(this.aimVertTarget, -0.5F, 0.3F);
         }

         float smoothRate = chanceF / 100.0F * 0.15F + 0.02F;
         this.aimVertBias = this.aimVertBias + (this.aimVertTarget - this.aimVertBias) * smoothRate;
         ty += target.getEyeHeight() * this.aimVertBias;
      }

      return this.rawRotation(tx, ty, tz);
   }

   private float[] rawRotation(double tx, double ty, double tz) {
      double dX = tx - mc.player.getX();
      double dY = ty - (mc.player.getY() + mc.player.getEyeHeight());
      double dZ = tz - mc.player.getZ();
      double dist = Math.sqrt(dX * dX + dZ * dZ);
      float yaw = (float)(Math.atan2(dZ, dX) * 180.0 / 3.141592653589793) - 90.0F;
      float pitch = (float)(-(Math.atan2(dY, dist) * 180.0 / 3.141592653589793));
      return new float[]{yaw, Mth.clamp(pitch, -90.0F, 90.0F)};
   }

   private float[] buildRotations(float[] raw) {
      float tYaw = raw[0];
      float tPitch = raw[1];
      if (this.grim.getValue()) {
         // Step from the last sent angle so changing modes cannot snap the
         // server rotation to an old smoothing or recorded-pattern state.
         float speed = Math.min(this.rotSpeed.getValue(), 6.0F);
         float yaw = this.sentYaw + Mth.clamp(Mth.wrapDegrees(tYaw - this.sentYaw), -speed * 4.0F, speed * 4.0F);
         float pitch = Mth.clamp(this.sentPitch + Mth.clamp(tPitch - this.sentPitch, -speed * 2.0F, speed * 2.0F), -90.0F, 90.0F);
         float[] rotation = this.quantizeRotation(yaw, pitch);
         this.smoothYaw = rotation[0];
         this.smoothPitch = rotation[1];
         return rotation;
      }
      if (this.style.getValue() && this.rotMode.getValue() != KillauraRecode.RotationMode.RECORDED) {
         return this.buildStyleRotations(tYaw, tPitch);
      }

      if (this.rotMode.getValue() == KillauraRecode.RotationMode.RECORDED) {
         RecordedRotationManager.RotationSample sample = OyVey.recordedRotationManager.getSample(this.recordedPatternIndex++);
         float sampleYawError = Mth.clamp(sample.yawError(), -45.0F, 45.0F);
         float samplePitchError = Mth.clamp(sample.pitchError(), -30.0F, 30.0F);
         this.recordedYawError += Mth.wrapDegrees(sampleYawError - this.recordedYawError) * 0.55F;
         this.recordedPitchError += (samplePitchError - this.recordedPitchError) * 0.55F;
         float desiredYaw = tYaw + this.recordedYawError;
         float desiredPitch = Mth.clamp(tPitch + this.recordedPitchError, -90.0F, 90.0F);
         float deltaYaw = Mth.wrapDegrees(desiredYaw - this.smoothYaw);
         float deltaPitch = desiredPitch - this.smoothPitch;
         float targetDistance = (float)Math.hypot(deltaYaw, deltaPitch);
         float sampleSpeed = (float)Math.hypot(sample.yawDelta(), sample.pitchDelta());
         float patternMin = OyVey.recordedRotationManager.getMinimumSpeed();
         float patternMax = OyVey.recordedRotationManager.getMaximumSpeed();
         float speedPosition = patternMax - patternMin > 1.0E-4F ? Mth.clamp((sampleSpeed - patternMin) / (patternMax - patternMin), 0.0F, 1.0F) : 0.5F;
         float configuredMin = Math.min(this.advRotMin.getValue(), this.advRotMax.getValue());
         float configuredMax = Math.max(this.advRotMin.getValue(), this.advRotMax.getValue());
         float recordedSpeed = (configuredMin + (configuredMax - configuredMin) * speedPosition) * 4.0F;
         this.recordedReplaySpeed += (recordedSpeed - this.recordedReplaySpeed) * 0.55F;
         if (targetDistance > 1.0E-4F) {
            float step = Math.min(targetDistance, this.recordedReplaySpeed);
            this.smoothYaw += deltaYaw / targetDistance * step;
            this.smoothPitch = Mth.clamp(this.smoothPitch + deltaPitch / targetDistance * step, -90.0F, 90.0F);
         }

         tYaw = this.smoothYaw;
         tPitch = this.smoothPitch;
      } else if (this.rotMode.getValue() == KillauraRecode.RotationMode.SMOOTH) {
         float speed = this.rotSpeed.getValue();
         float dYaw = Mth.wrapDegrees(tYaw - this.smoothYaw);
         float dPitch = tPitch - this.smoothPitch;
         this.smoothYaw = this.smoothYaw + Mth.clamp(dYaw, -speed * 4.0F, speed * 4.0F);
         this.smoothPitch = Mth.clamp(this.smoothPitch + Mth.clamp(dPitch, -speed * 2.0F, speed * 2.0F), -90.0F, 90.0F);
         tYaw = this.smoothYaw;
         tPitch = this.smoothPitch;
      } else if (this.rotMode.getValue() == KillauraRecode.RotationMode.ADVANCED) {
         float lo = Math.min(this.advRotMin.getValue(), this.advRotMax.getValue());
         float hi = Math.max(this.advRotMin.getValue(), this.advRotMax.getValue());
         float speed = lo + ThreadLocalRandom.current().nextFloat() * (hi - lo);
         float noiseF = this.noise.getValue() / 100.0F;
         if (noiseF > 0.0F) {
            speed *= 1.0F - ThreadLocalRandom.current().nextFloat() * noiseF * 0.85F;
            if (ThreadLocalRandom.current().nextFloat() < noiseF * 0.04F) {
               speed *= 0.05F;
            }
         }

         speed = Math.max(0.2F, speed);
         float dYaw = Mth.wrapDegrees(tYaw - this.smoothYaw);
         float dPitch = tPitch - this.smoothPitch;
         this.smoothYaw = this.smoothYaw + Mth.clamp(dYaw, -speed * 4.0F, speed * 4.0F);
         this.smoothPitch = Mth.clamp(this.smoothPitch + Mth.clamp(dPitch, -speed * 2.0F, speed * 2.0F), -90.0F, 90.0F);
         tYaw = this.smoothYaw;
         tPitch = this.smoothPitch;
         float jAmt = this.jitter.getValue() / 100.0F;
         if (jAmt > 0.0F) {
            this.jitterPhase = this.jitterPhase + 0.35F + ThreadLocalRandom.current().nextFloat() * 0.5F;
            float rawJitYaw = (float)Math.sin(this.jitterPhase) * jAmt * 2.0F + (float)Math.sin(this.jitterPhase * 2.3F) * jAmt * 0.8F;
            float rawJitPitch = (float)Math.sin(this.jitterPhase * 0.6F + 0.8F) * jAmt * 0.5F;
            rawJitYaw += (ThreadLocalRandom.current().nextFloat() * 2.0F - 1.0F) * jAmt * 0.7F;
            rawJitPitch += (ThreadLocalRandom.current().nextFloat() * 2.0F - 1.0F) * jAmt * 0.25F;
            this.jitterOffYaw = this.jitterOffYaw + (rawJitYaw - this.jitterOffYaw) * 0.55F;
            this.jitterOffPitch = this.jitterOffPitch + (rawJitPitch - this.jitterOffPitch) * 0.55F;
            tYaw += this.jitterOffYaw;
            tPitch += this.jitterOffPitch;
         }

         tPitch = Mth.clamp(tPitch, -90.0F, 90.0F);
      } else {
         this.smoothYaw = tYaw;
         this.smoothPitch = tPitch;
      }

      float continuousYaw = this.sentYaw + Mth.wrapDegrees(tYaw - this.sentYaw);
      return this.gcdFix.getValue() ? this.quantizeRotation(continuousYaw, tPitch) : new float[]{continuousYaw, tPitch};
   }

   private void resetRecordedPlayback() {
      this.recordedReplaySpeed = 0.1F;
      this.recordedYawError = 0.0F;
      this.recordedPitchError = 0.0F;
   }

   private float[] buildStyleRotations(float targetYaw, float targetPitch) {
      this.styleSpin = this.styleSpin + this.styleSpeed.getValue();
      float yaw;
      if (this.styleSpin >= 360.0F) {
         this.styleSpin = 0.0F;
         this.styleAtTarget = true;
         yaw = targetYaw;
      } else {
         this.styleAtTarget = false;
         yaw = targetYaw + this.styleSpin;
      }

      yaw = this.sentYaw + Mth.wrapDegrees(yaw - this.sentYaw);
      float pitch = Mth.clamp(targetPitch, -90.0F, 90.0F);
      this.smoothYaw = yaw;
      this.smoothPitch = pitch;
      return this.gcdFix.getValue() ? this.quantizeRotation(yaw, pitch) : new float[]{yaw, pitch};
   }

   private float[] quantizeRotation(float targetYaw, float targetPitch) {
      SilentRotationMath.Rotation rotation = SilentRotationMath.quantizeRotation(
         this.sentYaw,
         this.sentPitch,
         targetYaw,
         targetPitch,
         (Double)mc.options.sensitivity().get()
      );
      return new float[]{rotation.yaw(), rotation.pitch()};
   }

   private void performAttack(Entity target) {
      WTap.onAttack();
      mc.gameMode.attack(mc.player, target);
      if (this.armSwing.getValue()) {
         mc.player.swing(InteractionHand.MAIN_HAND);
      } else {
         mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
      }
   }

   private boolean shouldAttack() {
      if (!this.canAttackNow(this.currentTarget)) {
         return false;
      }

      BacktrackModule backtrack = OyVey.moduleManager.getModuleByClass(BacktrackModule.class);
      if (backtrack != null && backtrack.isEnabled() && backtrack.isTracking(this.currentTarget)) {
         return false;
      }

      if (!this.grim.getValue() && this.style.getValue() && !this.styleAtTarget) {
         return false;
      }

      boolean falling = !mc.player.onGround()
         && mc.player.getDeltaMovement().y < 0.0
         && !mc.player.isInWater()
         && !mc.player.isInLava()
         && !mc.player.isFallFlying();
      boolean baseOk;
      if (this.cooldown.getValue()) {
         float strength = mc.player.getAttackStrengthScale(0.0F);
         baseOk = this.onlyCrits.getValue()
            ? strength >= 1.0F && falling
            : strength >= (this.grim.getValue() ? 1.0F : 0.9F);
      } else {
         boolean delayOk = System.currentTimeMillis() - this.lastAttackTime >= this.nextAttackDelay;
         baseOk = this.onlyCrits.getValue() ? falling && delayOk : delayOk;
      }

      return !baseOk ? false : !this.hitSelect.getValue() || this.hitSelectGate();
   }

   private boolean canAttackNow(Entity target) {
      return target != null
         && target == this.currentTarget
         && target.isAlive()
         && !target.isRemoved()
         && this.targetRots != null
         && this.hasSentRot
         && target.getId() == this.sentTargetId
         && this.isWithinAttackRange(target)
         // Grim always checks the rotation sent on the preceding tick, even
         // when the separate Raycast setting is disabled.
         && (!(this.grim.getValue() || this.raycast.getValue())
            || this.rotationHitsTarget(target, this.sentYaw, this.sentPitch));
   }

   private boolean hitSelectGate() {
      if (this.currentTarget == null) {
         return true;
      }

      long now = System.currentTimeMillis();
      double dist = mc.player.distanceTo(this.currentTarget);
      if (dist > this.hitSelectRange.getValue()) {
         return true;
      }

      if (now < this.hsHoldUntil) {
         return false;
      }

      if (mc.player.hurtTime > 8) {
         this.hsHoldUntil = now + this.rollHsDelay();
         return false;
      }

      if (this.requireTargetHurtTime.getValue() && this.currentTarget instanceof LivingEntity le) {
         int ht = le.hurtTime;
         int min = this.targetHurtTimeMin.getValue();
         int max = this.targetHurtTimeMax.getValue();
         if (ht < min || ht > max) {
            return false;
         }
      }

      return true;
   }

   private long rollHsDelay() {
      int lo = Math.min(this.hitSelectDelayMin.getValue(), this.hitSelectDelayMax.getValue());
      int hi = Math.max(this.hitSelectDelayMin.getValue(), this.hitSelectDelayMax.getValue());
      return lo >= hi ? lo : lo + (long)(ThreadLocalRandom.current().nextDouble() * (hi - lo));
   }

   private long rollDelay() {
      if (!this.apsRand.getValue()) {
         return 1000L / Math.max(1, this.aps.getValue());
      }

      int lo = Math.min(this.apsMin.getValue(), this.apsMax.getValue());
      int hi = Math.max(this.apsMin.getValue(), this.apsMax.getValue());
      double mean = lo + (hi - lo) * 0.5;
      double stdDev = (hi - lo) / 6.0;
      double sampled = mean + gaussianSample(stdDev);
      sampled = Math.max(lo, Math.min(hi, sampled));
      return (long)(1000.0 / Math.max(1.0, sampled));
   }

   private static double gaussianSample(double stdDev) {
      double u1 = ThreadLocalRandom.current().nextDouble();
      double u2 = ThreadLocalRandom.current().nextDouble();
      if (u1 == 0.0) {
         u1 = 5.0E-324;
      }

      return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(6.283185307179586 * u2) * stdDev;
   }

   private void updateAutoBlock(KillauraRecode.BlockMode mode) {
      if (isFullBlockMode(mode)) {
         this.updateServerBlock(false);
         return;
      }
      if (mode == KillauraRecode.BlockMode.OFF || mode == KillauraRecode.BlockMode.FAKE) {
         if (this.autoBlockActive && mc.player.isUsingItem()) {
            mc.gameMode.releaseUsingItem(mc.player);
         }

         this.autoBlockActive = false;
         return;
      }

      if (mc.player.isUsingItem()) {
         this.autoBlockActive = true;
         return;
      }

      InteractionHand hand = mc.player.getOffhandItem().getItem() instanceof ShieldItem
         ? InteractionHand.OFF_HAND
         : InteractionHand.MAIN_HAND;
      mc.gameMode.useItem(mc.player, hand);

      this.autoBlockActive = mc.player.isUsingItem();
   }

   private static boolean isFullBlockMode(BlockMode mode) {
      return mode == BlockMode.FULL || mode == BlockMode.FULL2;
   }

   private void updateServerBlock(boolean refresh) {
      boolean sword = mc.player.getMainHandItem().is(ItemTags.SWORDS);
      boolean usingOtherItem = mc.player.isUsingItem() && mc.player.getUsedItemHand() != InteractionHand.MAIN_HAND;
      if (!sword || usingOtherItem || mc.player.isSpectator()) {
         this.stopServerBlock();
         return;
      }
      int slot = mc.player.getInventory().getSelectedSlot();
      if (this.serverBlockHeld && this.serverBlockSlot == slot) {
         if (refresh) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
         }
         return;
      }
      this.stopServerBlock();
      mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
      // Modern clients may return PASS for a sword even though a translated 1.8
      // server receives the use-item packet and starts blocking.
      this.serverBlockHeld = true;
      this.serverBlockSlot = slot;
   }

   private void stopServerBlock() {
      if (!this.serverBlockHeld) {
         return;
      }
      this.serverBlockHeld = false;
      this.serverBlockSlot = -1;
      if (mc.player != null && mc.player.connection != null) {
         mc.player.connection.send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
         if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            mc.player.releaseUsingItem();
         }
      }
   }

   @Subscribe
   private void onServerBlockPacket(PacketEvent.Send event) {
      if (!this.serverBlockHeld || nullCheck() || this.grim.getValue() || !isFullBlockMode(this.blockMode.getValue())
         || this.currentTarget == null || !this.currentTarget.isAlive() || this.targetRots == null
         || mc.player.getInventory().getSelectedSlot() != this.serverBlockSlot
         || !mc.player.getMainHandItem().is(ItemTags.SWORDS)
         || mc.player.isUsingItem() && mc.player.getUsedItemHand() != InteractionHand.MAIN_HAND) {
         return;
      }
      if (event.getPacket() instanceof ServerboundPlayerActionPacket action
         && action.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
         event.cancel();
      }
   }

   private void tryShieldBreakSwap() {
      this.shieldSwapPrev = -1;
      if (this.shieldBreak.getValue()
         && this.currentTarget instanceof LivingEntity le
         && le.isBlocking()
         && !(mc.player.getMainHandItem().getItem() instanceof AxeItem)) {
         for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() instanceof AxeItem) {
               this.shieldSwapPrev = mc.player.getInventory().getSelectedSlot();
               mc.player.getInventory().setSelectedSlot(i);
               mc.player.connection.send(new ServerboundSetCarriedItemPacket(i));
               return;
            }
         }
      }
   }

   private void restoreShieldBreakSwap() {
      if (this.shieldSwapPrev != -1) {
         int restoreSlot = this.shieldSwapPrev;
         this.shieldSwapPrev = -1;
         if (this.shieldSwapBack.getValue() && mc.player != null && mc.player.connection != null) {
            mc.player.getInventory().setSelectedSlot(restoreSlot);
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(restoreSlot));
         }
      }
   }

   public void applyGrimMovementCorrection(ClientInput input) {
      if (!this.isGrimMovementCorrectionConfigured()
         || this.targetRots == null
         || !this.hasSaved
         || mc.player == null) {
         this.grimSprintAllowed = true;
         return;
      }

      Input originalKeys = input.keyPresses;
      SilentRotationMath.Movement corrected = SilentRotationMath.correctMovement(
         originalKeys,
         this.savedYaw,
         this.targetRots[0]
      );

      // Sprinting is valid for forward diagonals as well as straight forward.
      // Sideways-only/backward corrected input cannot sustain vanilla sprint.
      this.grimSprintAllowed = corrected.forward() > 0;
      input.keyPresses = new Input(
         corrected.forward() > 0,
         corrected.forward() < 0,
         corrected.sideways() > 0,
         corrected.sideways() < 0,
         originalKeys.jump(),
         originalKeys.shift(),
         originalKeys.sprint() && this.grimSprintAllowed
      );

      // This is the same normalized pre-physics vector produced by
      // KeyboardInput.tick(). Vanilla subsequently applies its modern input
      // transform, acceleration, collisions, block friction, and 0.91 drag.
      ((ClientInputAccessor)input).oyvey$setMoveVector(
         new Vec2(corrected.normalizedSideways(), corrected.normalizedForward())
      );
      if (!this.grimSprintAllowed) {
         mc.player.setSprinting(false);
      }
   }

   public boolean isGrimMovementCorrectionActive() {
      return this.isGrimMovementCorrectionConfigured() && this.targetRots != null && this.hasSaved;
   }

   public boolean isGrimMovementCorrectionConfigured() {
      return this.isEnabled() && (this.grim.getValue() || this.correctMove.getValue() && this.moveFix.getValue() == KillauraRecode.MoveFix.GRIM);
   }

   public boolean isGrimSprintAllowed() {
      return this.grimSprintAllowed;
   }

   public boolean isFakeBlocking() {
      return this.isEnabled() && !nullCheck()
         && (this.fakeAutoBlock.getValue() || this.blockMode.getValue() == BlockMode.FAKE || this.serverBlockHeld)
         && (!mc.player.isUsingItem() || this.serverBlockHeld && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND)
         && !mc.player.isSpectator() && mc.player.isAlive()
         && mc.player.getMainHandItem().is(ItemTags.SWORDS)
         && this.currentTarget != null && this.currentTarget.isAlive() && this.targetRots != null
         && this.isWithinSearchRange(this.currentTarget);
   }

   @Override
   public void onRender2D(Render2DEvent event) {
      if (!this.showBlockCooldown.getValue() || !this.isFakeBlocking()
         || mc.options.hideGui || !mc.options.getCameraType().isFirstPerson()
         || mc.options.attackIndicator().get() != AttackIndicatorStatus.OFF) {
         return;
      }

      // Respect an existing crosshair/hotbar indicator; provide one when it is off.
      float strength = Mth.clamp(mc.player.getAttackStrengthScale(event.getDelta()), 0.0F, 1.0F);
      int x = mc.getWindow().getGuiScaledWidth() / 2 - 8;
      int y = mc.getWindow().getGuiScaledHeight() / 2 + 12;
      event.getContext().fill(x, y, x + 16, y + 3, 0xA0000000);
      event.getContext().fill(x, y, x + Math.round(16 * strength), y + 3, 0xFFE8F6FC);
   }

   public Entity getTarget() {
      return this.currentTarget;
   }

   public enum BlockMode {
      OFF,
      LEGIT,
      PACKET,
      FULL,
      FULL2,
      VANILLA,
      FAKE;
   }

   public enum MoveFix {
      OFF,
      GRIM;
   }

   public enum RotationMode {
      INSTANT,
      SMOOTH,
      PREDICT,
      ADVANCED,
      RECORDED;
   }

   public enum TargetMode {
      CLOSEST,
      LOWEST_HP,
      HIGHEST_HP,
      ANGLE;
   }
}
