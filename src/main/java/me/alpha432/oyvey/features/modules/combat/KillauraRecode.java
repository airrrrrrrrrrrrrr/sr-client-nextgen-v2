package me.alpha432.oyvey.features.modules.combat;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import me.alpha432.oyvey.util.TransmittedRotation;
import me.alpha432.oyvey.util.OneTickAttackRotation;
import me.alpha432.oyvey.util.PolarRotation;
import me.alpha432.oyvey.util.RecordedClickPlayback;
import me.alpha432.oyvey.util.CriticalTiming;
import me.alpha432.oyvey.util.CombatAim;
import me.alpha432.oyvey.util.ShieldCombat;
import me.alpha432.oyvey.util.NativeClickPulse;
import me.alpha432.oyvey.util.ViaProtocol;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.manager.RecordedRotationManager;
import me.alpha432.oyvey.mixin.render.accessor.ClientInputAccessor;
import me.alpha432.oyvey.mixin.render.accessor.LocalPlayerRotationAccessor;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class KillauraRecode extends Module {
   public final Setting<Boolean> grim = this.register(new Setting<>("Grim", false, "Use conservative rotations, movement, and attack checks for Grim servers."));
   public final Setting<Double> reach = this.register(new Setting<>("Reach", 3.0, 1.0, 6.0));
   public final Setting<Double> searchRange = this.register(new Setting<>("SearchRange", 3.0, 1.0, 30.0));
   public final Setting<Boolean> mobs = this.register(new Setting<>("Mobs", false));
   public final Setting<Boolean> requireArmor = this.register(new Setting<>("RequireArmor", false));
   public final Setting<Boolean> respectFriends = this.register(new Setting<>("RespectFriends", true));
   public final Setting<Boolean> wallCheck = this.register(new Setting<>("WallCheck", true));
   public final Setting<Boolean> raycast = this.register(new Setting<>("Raycast", true));
   public final Setting<KillauraRecode.TargetMode> targetMode = this.register(new Setting<>("TargetMode", KillauraRecode.TargetMode.CLOSEST));
   public final Setting<ClickMode> clickMode = this.register(new Setting<>("ClickMode", ClickMode.NORMAL,
      "Recorded uses the selected .click recording instead of APS or 1.9 cooldown pacing."));
   public final Setting<Boolean> cooldown = this.register(new Setting<>("1.9Cooldown", true, v -> this.clickMode.getValue() == ClickMode.NORMAL));
   public final Setting<Boolean> onlyCrits = this.register(new Setting<>("OnlyCrits", false));
   public final Setting<Boolean> preferCrit = this.register(new Setting<>("PreferCrit", false,
      "Attack normally on the ground; wait for a falling critical hit while jumping."));
   public final Setting<Boolean> apsRand = this.register(new Setting<>("RandAPS", true, v -> this.clickMode.getValue() == ClickMode.NORMAL));
   public final Setting<Integer> apsMin = this.register(new Setting<>("APSMin", 9, 1, 20, v -> this.apsRand.getValue() && this.clickMode.getValue() == ClickMode.NORMAL));
   public final Setting<Integer> apsMax = this.register(new Setting<>("APSMax", 14, 1, 20, v -> this.apsRand.getValue() && this.clickMode.getValue() == ClickMode.NORMAL));
   public final Setting<Integer> aps = this.register(new Setting<>("APS", 12, 1, 20, v -> !this.apsRand.getValue() && this.clickMode.getValue() == ClickMode.NORMAL));
   public final Setting<KillauraRecode.RotationMode> rotMode = this.register(new Setting<RotationMode>("RotationMode", RotationMode.INSTANT) {
      @Override public String currentEnumName() {
         return getValue() == RotationMode.ONE_TICK ? "One Tick" : super.currentEnumName();
      }
   });
   public final Setting<Float> predictTicks = this.register(
      new Setting<>("PredictTicks", 1.5F, 0.0F, 4.0F, v -> this.rotMode.getValue() == KillauraRecode.RotationMode.PREDICT)
   );
   public final Setting<Float> advRotMin = this.register(
      new Setting<>(
         "MinRotSpeed",
         2.0F,
         0.5F,
         15.0F,
         v -> this.rotMode.getValue() != RotationMode.INSTANT && this.rotMode.getValue() != RotationMode.ONE_TICK
      )
   );
   public final Setting<Float> advRotMax = this.register(
      new Setting<>(
         "MaxRotSpeed",
         6.0F,
         0.5F,
         15.0F,
         v -> this.rotMode.getValue() != RotationMode.INSTANT && this.rotMode.getValue() != RotationMode.ONE_TICK
      )
   );
   public final Setting<Float> jitter = this.register(
      new Setting<>("Jitter", 15.0F, 0.0F, 100.0F, v -> this.rotMode.getValue() == KillauraRecode.RotationMode.ADVANCED)
   );
   public final Setting<Float> noise = this.register(
      new Setting<>("Noise", 20.0F, 0.0F, 100.0F, v -> this.rotMode.getValue() == KillauraRecode.RotationMode.ADVANCED)
   );
   public final Setting<Boolean> style = this.register(new Setting<>("Style", false,
      v -> this.rotMode.getValue() != RotationMode.ONE_TICK && this.rotMode.getValue() != RotationMode.POLAR));
   public final Setting<Float> styleSpeed = this.register(new Setting<>("StyleSpeed", 30.0F, 5.0F, 120.0F,
      v -> this.style.getValue() && this.rotMode.getValue() != RotationMode.ONE_TICK && this.rotMode.getValue() != RotationMode.POLAR));
   public final Setting<Boolean> gcdFix = this.register(new Setting<>("GCDFix", true));
   public final Setting<Boolean> correctMove = this.register(new Setting<>("CorrectMove", true));
   public final Setting<KillauraRecode.MoveFix> moveFix = this.register(new Setting<>("MoveFix", KillauraRecode.MoveFix.GRIM));
   public final Setting<KillauraRecode.BlockMode> blockMode = this.register(new Setting<>("AutoBlock", KillauraRecode.BlockMode.OFF));
   public final Setting<Integer> legitBlockCps = this.register(new Setting<>("LegitBlockCPS", 10, 5, 20,
      v -> this.blockMode.getValue() == BlockMode.LEGIT));
   public final Setting<Boolean> attackWhileShield = this.register(new Setting<>("AttackWhileShield", false,
      v -> this.effectiveBlockMode() == BlockMode.SHIELD));
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
   private final TransmittedRotation transmittedRotation = new TransmittedRotation();
   private final OneTickAttackRotation oneTickRotation = new OneTickAttackRotation();
   private final PolarRotation polarRotation = new PolarRotation();
   private final RecordedClickPlayback recordedClicks = new RecordedClickPlayback();
   private int recordedClicksDue;
   private boolean missingClickPatternWarned;
   private OneTickAttackRotation.Step oneTickStep = OneTickAttackRotation.Step.WAIT;
   private LocalPlayer oneTickOwner;
   private ClientLevel oneTickLevel;
   private boolean hasSentRot = false;
   private int sentTargetId = -1;
   private float[] targetRots = null;
   private float savedYaw = 0.0F;
   private float savedPitch = 0.0F;
   private boolean hasSaved = false;
   private float jitterPhase = 0.0F;
   private float jitterOffYaw = 0.0F;
   private float jitterOffPitch = 0.0F;
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
   private final NativeClickPulse legitClicks = new NativeClickPulse();
   private boolean legitClickQueued;
   private boolean legitClickDue;
   private LocalPlayer legitUseOwner;
   private ClientLevel legitUseLevel;
   private InteractionHand legitUseHand;
   private boolean serverBlockHeld;
   private int serverBlockSlot = -1;
   private final ShieldCombat.AxeSwap shieldSwap = new ShieldCombat.AxeSwap();
   private LocalPlayer shieldSwapOwner, shieldUseOwner;
   private ClientLevel shieldSwapLevel, shieldUseLevel;
   private InteractionHand shieldUseHand;
   private long shieldBreakRetryUntil;
   private int recordedPatternIndex = 0;
   private float recordedReplaySpeed = 0.1F;
   private float recordedYawError = 0.0F;
   private float recordedPitchError = 0.0F;
   private KillauraRecode.RotationMode lastRotationMode = null;
   private boolean grimSprintAllowed = true;
   private ClientInput correctedMovementInput;
   private float correctedMovementYaw;
   private int resumeAfterCorrectionTick;
   private String lastAttackTrace = "none";
   private String lastMovementTrace = "none";
   private String lastInputTrace = "none";

   public KillauraRecode() {
      super("KillauraRecode", "Advanced KillAura with smooth rotations, prediction, and wall checks.", Module.Category.COMBAT);
   }

   @Override
   public void onEnable() {
      this.recordedClicks.reset(); this.recordedClicksDue=0; this.missingClickPatternWarned=false;
      this.polarRotation.reset();
      this.resetOneTickRotation();
      this.legitClicks.reset(); this.legitClickQueued=this.legitClickDue=false;
      this.clearLegitUse();
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
      this.shieldSwap.clear();
      this.shieldSwapOwner = this.shieldUseOwner = null;
      this.shieldSwapLevel = this.shieldUseLevel = null;
      this.shieldUseHand = null;
      this.shieldBreakRetryUntil = 0;
      this.jitterPhase = 0.0F;
      this.jitterOffYaw = 0.0F;
      this.jitterOffPitch = 0.0F;
      this.styleSpin = 0.0F;
      this.styleAtTarget = false;
      this.hsHoldUntil = 0L;
      this.recordedPatternIndex = 0;
      this.recordedReplaySpeed = 0.1F;
      this.recordedYawError = 0.0F;
      this.recordedPitchError = 0.0F;
      this.lastRotationMode = null;
      this.grimSprintAllowed = true;
      this.resumeAfterCorrectionTick = 0;
      this.lastAttackTrace = "none";
      this.lastMovementTrace = "none";
      this.lastInputTrace = "none";
      if (mc.player != null) {
         this.smoothYaw = mc.player.getYRot();
         this.smoothPitch = mc.player.getXRot();
         LocalPlayerRotationAccessor player = (LocalPlayerRotationAccessor)mc.player;
         this.sentYaw = player.oyvey$getLastSentYaw();
         this.sentPitch = player.oyvey$getLastSentPitch();
      }
      this.transmittedRotation.reset(this.sentYaw, this.sentPitch);
   }

   @Override
   public void onDisable() {
      this.recordedClicks.reset(); this.recordedClicksDue=0;
      this.polarRotation.reset();
      this.resetOneTickRotation();
      this.legitClicks.reset(); this.legitClickQueued=this.legitClickDue=false;
      if (this.ownsLegitUse() && mc.gameMode != null) mc.gameMode.releaseUsingItem(mc.player);
      this.clearLegitUse();
      isRotating = false;
      AutoRod rod = OyVey.moduleManager.getModuleByClass(AutoRod.class);
      if (rod != null) rod.abort();
      this.stopServerBlock();
      this.stopShieldBlock();
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

   @Override
   public void onTick() {
      if (nullCheck() || mc.screen != null || !mc.player.isAlive()) {
         this.stopShieldBlock(); this.restoreShieldBreakSwap(); return;
      }
      if ((this.effectiveBlockMode() != BlockMode.SHIELD && this.effectiveBlockMode() != BlockMode.LEGIT)
         || this.currentTarget == null || !this.currentTarget.isAlive()) this.stopShieldBlock();
      if (this.shieldSwap.active() && (!this.shieldBreak.getValue() || this.shieldSwapOwner != mc.player || this.shieldSwapLevel != mc.level))
         this.restoreShieldBreakSwap();
   }

   @Subscribe
   public void onUpdateWalkingPlayer(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == Stage.POST) return;
      if (mc.level != null && mc.player != null) {
         if (OyVey.recordedClickManager != null && OyVey.recordedClickManager.isRecording()) {
            AutoRod rod = OyVey.moduleManager.getModuleByClass(AutoRod.class);
            if (rod != null) rod.abort();
            this.stopServerBlock(); this.stopShieldBlock(); this.restoreShieldBreakSwap();
            if (this.ownsLegitUse() && mc.gameMode != null) mc.gameMode.releaseUsingItem(mc.player);
            this.clearLegitUse();
            this.targetRots=null; this.currentTarget=null; this.hasSaved=false;
            this.hasSentRot=false; this.sentTargetId=-1; isRotating=false;
            this.resetOneTickRotation(); this.recordedClicks.reset(); this.recordedClicksDue=0;
            OyVey.rotationManager.clearServerRotations();
            return;
         }
         if (mc.player.tickCount < this.resumeAfterCorrectionTick) return;
         Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
         if (scaffold != null && scaffold.ownsRotation()) {
            this.recordedClicks.reset(); this.recordedClicksDue=0;
            this.polarRotation.reset();
            this.resetOneTickRotation();
            AutoRod rod = OyVey.moduleManager.getModuleByClass(AutoRod.class);
            if (rod != null) rod.abort();
            // One rotation must drive physics, input and serialization for a tick.
            // Scaffold has priority while lining up a placement.
            isRotating = false;
            this.stopServerBlock();
            this.stopShieldBlock();
            this.restoreShieldBreakSwap();
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
            if (this.isCriticalSprintSuppressed()) {
               mc.player.setSprinting(false);
               // Use vanilla's command and cached sprint state before attacking.
               ((LocalPlayerRotationAccessor)mc.player).oyvey$sendSprintState();
            }
            this.updateCombat();
            if (this.rotMode.getValue() == RotationMode.ONE_TICK && this.oneTickStep == OneTickAttackRotation.Step.ATTACK) {
               // Attack used the preceding aim. This tick's native movement
               // returns to the saved camera; no extra movement packet is sent.
               this.targetRots = null;
               isRotating = false;
               this.grimSprintAllowed = true;
               OyVey.rotationManager.clearServerRotations();
            }
            if (this.targetRots != null && this.isGrimMovementCorrectionConfigured()) {
               OyVey.rotationManager.setPlayerRotations(this.targetRots[0], this.targetRots[1]);
            }
         } else if (event.getStage() == Stage.PRE) {
            // Rotation serialization must not change movement or sprint state
            // after vanilla has already calculated this tick's displacement.
            if (this.targetRots != null && this.currentTarget != null) {
               if ((this.rotMode.getValue() == RotationMode.INSTANT || this.rotMode.getValue() == RotationMode.ONE_TICK)
                  && this.criticalState().normalMovement()) {
                  // Jumping changes eye height after input/physics. Refresh only
                  // pitch from the actual eye position; physics yaw stays fixed.
                  float pitch = this.computeAimPoint(this.currentTarget)[1];
                  this.targetRots[1] = this.grim.getValue() || this.gcdFix.getValue()
                     ? this.quantizeRotation(this.targetRots[0], pitch)[1] : pitch;
                  this.smoothPitch = this.targetRots[1];
               }
               OyVey.rotationManager.setPlayerRotations(this.targetRots[0], this.targetRots[1]);
               OyVey.rotationManager.setServerRotations(this.targetRots[0], this.targetRots[1]);
            }
         }
      }
   }

   @Subscribe(priority = -900)
   public void onWalkingPost(UpdateWalkingPlayerEvent event) {
      if (event.getStage() != Stage.POST) return;
      isRotating = false;
      if (this.hasSaved && mc.player != null) {
         float yaw = this.sentYaw + Mth.wrapDegrees(this.savedYaw - this.sentYaw);
         OyVey.rotationManager.setPlayerRotations(yaw, this.savedPitch);
      }
      this.hasSaved = false;
   }

   @Subscribe(priority = -1000)
   private void onMovementPacket(PacketEvent.Send event) {
      if (event.isCancelled() || !(event.getPacket() instanceof ServerboundMovePlayerPacket movement)) return;
      int intendedTarget = this.hasSaved && this.targetRots != null && this.currentTarget != null
         ? this.currentTarget.getId() : -1;
      this.transmittedRotation.record(movement, false, intendedTarget,
         this.targetRots == null ? this.sentYaw : this.targetRots[0],
         this.targetRots == null ? this.sentPitch : this.targetRots[1]);
      this.sentYaw = this.transmittedRotation.yaw();
      this.sentPitch = this.transmittedRotation.pitch();
      this.sentTargetId = this.transmittedRotation.targetId();
      this.hasSentRot = this.sentTargetId >= 0;
      if (this.rotMode.getValue() == RotationMode.ONE_TICK && mc.player != null) {
         this.oneTickRotation.transmitted(mc.player.tickCount, this.sentTargetId);
      }
      // A teleport acknowledgment is also a movement packet; preserve the
      // preceding combat movement rather than replacing it with the correction.
      if (intendedTarget >= 0 && mc.player != null) {
         this.lastMovementTrace = "tick=" + mc.player.tickCount + " look=" + this.sentYaw + "," + this.sentPitch
            + " ground=" + movement.isOnGround() + " sprint=" + mc.player.isSprinting()
            + " motion=" + mc.player.getDeltaMovement();
      }
   }

   public void recordInputTrace(ClientInput input) {
      if (!this.isEnabled() || mc.player == null) return;
      this.lastInputTrace = "tick=" + mc.player.tickCount + " physicsLook=" + mc.player.getYRot() + "," + mc.player.getXRot()
         + " keys=" + input.keyPresses + " vector=" + input.getMoveVector().x + "," + input.getMoveVector().y
         + " normalized=" + me.alpha432.oyvey.util.KeyboardMovement.normalizesDiagonals();
   }

   public void onServerCorrection() {
      if (!this.isEnabled() || mc.player == null) return;
      this.recordedClicks.reset(); this.recordedClicksDue=0;
      this.polarRotation.reset();
      this.resetOneTickRotation();
      AutoRod rod = OyVey.moduleManager.getModuleByClass(AutoRod.class);
      if (rod != null) rod.abort();
      this.stopShieldBlock();
      this.restoreShieldBreakSwap();
      OyVey.LOGGER.warn("[CombatRollback] attack=[{}] movement=[{}] input=[{}]",
         this.lastAttackTrace, this.lastMovementTrace, this.lastInputTrace);
      // Vanilla has already applied and acknowledged the server's position.
      // Discard old aim rather than restoring the pre-correction camera next tick.
      this.hasSaved = false;
      this.hasSentRot = false;
      this.sentTargetId = -1;
      this.targetRots = null;
      this.currentTarget = null;
      isRotating = false;
      this.sentYaw = this.smoothYaw = mc.player.getYRot();
      this.sentPitch = this.smoothPitch = mc.player.getXRot();
      this.transmittedRotation.reset(this.sentYaw, this.sentPitch);
      this.grimSprintAllowed = true;
      this.resumeAfterCorrectionTick = mc.player.tickCount + 2;
      OyVey.rotationManager.clearServerRotations();
   }

   private void updateCombat() {
      this.recordedClicksDue = this.clickMode.getValue() == ClickMode.RECORDED && mc.screen == null
         && this.canAttackNow(this.currentTarget) ? this.recordedClicks.poll(System.nanoTime()) : 0;
      AutoRod rod = OyVey.moduleManager.getModuleByClass(AutoRod.class);
      if (rod != null && rod.active()) {
         this.stopServerBlock();
         this.stopShieldBlock();
         this.restoreShieldBreakSwap();
         if (this.autoBlockActive && mc.player.isUsingItem()) mc.gameMode.releaseUsingItem(mc.player);
         this.autoBlockActive = false;
         rod.use(this.currentTarget, this.hasSentRot && this.currentTarget != null
            && this.sentTargetId == this.currentTarget.getId(), this.sentYaw, this.sentPitch);
         if (rod.blocksMelee()) return;
      }

      KillauraRecode.BlockMode bm = this.effectiveBlockMode();
      if (bm != BlockMode.SHIELD && bm != BlockMode.LEGIT && this.fakeAutoBlock.getValue() && !isFullBlockMode(bm)) bm = BlockMode.OFF;
      final BlockMode activeBlockMode = bm;
      if (bm != BlockMode.SHIELD && bm != BlockMode.LEGIT) this.stopShieldBlock();
      if (!isFullBlockMode(bm)) {
         this.stopServerBlock();
      }
      if (this.currentTarget != null && (this.targetRots != null || this.rotMode.getValue() == RotationMode.ONE_TICK
         && mc.screen == null && this.isWithinAttackRange(this.currentTarget))) {
         boolean breakingShield = this.prepareShieldBreakSwap();
         if (isFullBlockMode(bm)) {
            this.updateServerBlock(false);
         }
         if (bm == BlockMode.SHIELD) this.updateShieldBlock();
         boolean ownedSwordBlock = this.serverBlockHeld
            && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND
            && mc.player.getMainHandItem().is(ItemTags.SWORDS);
         ownedSwordBlock |= bm == BlockMode.LEGIT && this.ownsLegitUse() && mc.player.getUseItem().is(ItemTags.SWORDS);
         boolean raisedShield = (bm == BlockMode.SHIELD || bm == BlockMode.LEGIT && this.shieldUseOwner == mc.player)
            && this.usingShield();
         boolean attackReady = (!mc.player.isUsingItem() || ownedSwordBlock || raisedShield) && this.shouldAttack(breakingShield);
         if (attackReady) {
            int attempts = this.clickMode.getValue() == ClickMode.RECORDED ? this.recordedClicksDue : 1;
            for (int click = 0; click < attempts; click++) {
               if (click > 0 && (breakingShield || !this.shouldAttack(false))) break;
               if (bm == BlockMode.LEGIT && this.ownsLegitUse() && !raisedShield) {
                  mc.gameMode.releaseUsingItem(mc.player);
                  this.clearLegitUse();
               }
               if (bm == BlockMode.FULL2) {
                  this.updateServerBlock(true);
               }
               // canAttackNow validates the preceding tick's transmitted rotation.
               final boolean axeHit = breakingShield;
               try {
                  ShieldCombat.attack(raisedShield, bm == BlockMode.SHIELD && this.attackWhileShield.getValue(), axeHit,
                     () -> { mc.gameMode.releaseUsingItem(mc.player); this.clearShieldOwnership(); },
                     () -> this.performAttack(this.currentTarget),
                     () -> { if (activeBlockMode == BlockMode.SHIELD) this.updateShieldBlock(); });
               } finally {
                  if (axeHit) {
                     this.restoreShieldBreakSwap();
                     this.shieldBreakRetryUntil = System.currentTimeMillis() + 250;
                     if (bm == BlockMode.SHIELD) this.updateShieldBlock();
                  }
               }
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
            }
         } else {
            this.updateAutoBlock(bm);
         }
      } else {
         this.stopServerBlock();
         this.stopShieldBlock();
         this.restoreShieldBreakSwap();
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
      if (this.rotMode.getValue() == KillauraRecode.RotationMode.RECORDED && !OyVey.recordedRotationManager.hasPattern()) {
         AutoRod rod = OyVey.moduleManager.getModuleByClass(AutoRod.class);
         if (rod != null) rod.abort();
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
         this.recordedClicks.reset(); this.recordedClicksDue=0;
         this.polarRotation.reset();
         this.resetOneTickRotation();
         AutoRod rod = OyVey.moduleManager.getModuleByClass(AutoRod.class);
         if (rod != null) rod.prepare(null, false);
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

         AutoRod rod = OyVey.moduleManager.getModuleByClass(AutoRod.class);
         if (rod != null) rod.prepare(this.currentTarget, this.isWithinAttackRange(this.currentTarget));
         this.prepareRecordedClicks(rod);
         if (this.rotMode.getValue() == RotationMode.ONE_TICK && (rod == null || !rod.active())) {
            if (this.oneTickOwner != mc.player || this.oneTickLevel != mc.level) this.resetOneTickRotation();
            this.oneTickOwner = mc.player;
            this.oneTickLevel = mc.level;
            boolean ready = mc.screen == null && mc.player.isAlive() && this.isWithinAttackRange(this.currentTarget)
               && (!mc.player.isUsingItem() || this.usingShield() || this.ownsMainHandBlock() || this.ownsLegitUse())
               && this.attackTimingReady(this.hasReadyShieldBreak(),true);
            this.oneTickStep = this.oneTickRotation.update(this.currentTarget.getId(), mc.player.tickCount, ready);
            if (this.oneTickStep == OneTickAttackRotation.Step.WAIT) {
               this.targetRots = null;
               OyVey.rotationManager.clearServerRotations();
               return;
            }
         } else {
            this.resetOneTickRotation();
         }
         this.targetRots = this.buildRotations(this.computeAimPoint(this.currentTarget));
         OyVey.rotationManager.setServerRotations(this.targetRots[0], this.targetRots[1]);
      }
   }

   private void resetOneTickRotation() {
      this.oneTickRotation.reset();
      this.oneTickStep = OneTickAttackRotation.Step.WAIT;
      this.oneTickOwner = null;
      this.oneTickLevel = null;
   }

   private void prepareRecordedClicks(AutoRod rod) {
      if (this.clickMode.getValue() != ClickMode.RECORDED || OyVey.recordedClickManager == null) {
         this.recordedClicks.reset(); this.missingClickPatternWarned=false; return;
      }
      var pattern = OyVey.recordedClickManager.pattern();
      if (pattern.isEmpty() && !this.missingClickPatternWarned) {
         Command.sendMessage("{yellow} Recorded clicks need a pattern. Use .click record start, sneak and click, then .click record stop <name>.","click-record");
         this.missingClickPatternWarned=true;
      }
      if (!pattern.isEmpty()) this.missingClickPatternWarned=false;
      boolean eligible = mc.screen == null && mc.player.isAlive() && this.isWithinAttackRange(this.currentTarget)
         && (rod == null || !rod.blocksMelee())
         && (!mc.player.isUsingItem() || this.usingShield() || this.ownsMainHandBlock() || this.ownsLegitUse());
      this.recordedClicks.prepare(pattern,this.currentTarget.getId(),eligible,System.nanoTime());
   }

   private boolean hasReadyShieldBreak() {
      if (!this.shieldBreak.getValue() || System.currentTimeMillis() < this.shieldBreakRetryUntil
         || !(this.currentTarget instanceof LivingEntity living) || !living.isBlocking()
         || !(living.getUseItem().getItem() instanceof ShieldItem)) return false;
      for (int slot = 0; slot < 9; slot++) {
         if (mc.player.getInventory().getItem(slot).getItem() instanceof AxeItem) return true;
      }
      return false;
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
      KillauraRecode.RotationMode current = this.rotMode.getValue();
      if (current == this.lastRotationMode) {
         return;
      }

      this.lastRotationMode = current;
      this.polarRotation.reset();
      this.resetOneTickRotation();
      this.recordedPatternIndex = 0;
      this.resetRecordedPlayback();
      this.smoothYaw = this.sentYaw;
      this.smoothPitch = this.sentPitch;
      if (current == KillauraRecode.RotationMode.RECORDED) {
         if (OyVey.recordedRotationManager.hasPattern()) {
            Command.sendMessage(
               "{yellow} Recorded rotations are using {white}%s{yellow} target-relative samples. Press {white}H{yellow}, then sneak to record a replacement.",
               "rot-record",
               OyVey.recordedRotationManager.getPatternSize()
            );
         } else {
            Command.sendMessage(
               "{yellow} Recorded mode needs a pattern. Press {white}H{yellow}, sneak to start, aim at Steve, then press H again to save JSON.",
               "rot-record"
            );
         }
      }
   }

   private List<Entity> getTargets() {
      double r = this.effectiveSearchRange();
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
         .filter(e -> !this.wallCheck.getValue() || this.hasLineOfSight(e))
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
      return CombatAim.visibleHeadPoint(target.getBoundingBox(), target.getEyeY(), point -> this.rayMiss(eyes, point)).isPresent();
   }

   private boolean rayMiss(Vec3 from, Vec3 to) {
      return mc.level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, mc.player)).getType() == Type.MISS;
   }

   private boolean rotationHitsTarget(Entity target, float yaw, float pitch) {
      Vec3 eyes = mc.player.getEyePosition();
      Vec3 look = Vec3.directionFromRotation(pitch, yaw);
      Vec3 end = eyes.add(look.scale(this.effectiveReach()));
      var hit = ProjectileUtil.getEntityHitResult(mc.player, eyes, end,
         mc.player.getBoundingBox().expandTowards(look.scale(this.effectiveReach())).inflate(1),
         entity -> entity != mc.player && entity.isAlive() && entity.isPickable() && !entity.isSpectator(),
         this.effectiveReach() * this.effectiveReach());
      if (hit == null || hit.getEntity() != target) {
         return false;
      } else {
         return !this.wallCheck.getValue()
            ? true
            : mc.level.clip(new ClipContext(eyes, hit.getLocation(), Block.COLLIDER, Fluid.NONE, mc.player)).getType() == Type.MISS;
      }
   }

   private double effectiveReach() {
      // Reach is the client's attack cutoff in every rotation/Grim mode.
      // Acceptance of the resulting native attack is decided by the server.
      return this.reach.getValue();
   }

   private double effectiveSearchRange() {
      AutoRod rod = OyVey.moduleManager.getModuleByClass(AutoRod.class);
      return Math.max(Math.max(this.searchRange.getValue(), this.effectiveReach()), rod == null ? 0 : rod.searchRange());
   }

   public boolean isWithinSearchRange(Entity target) {
      if (target == null || target.isRemoved()) {
         return false;
      }

      double range = this.effectiveSearchRange();
      return this.distanceToHitboxSqr(target) <= range * range;
   }

   private boolean isWithinAttackRange(Entity target) {
      if (target == null || target.isRemoved()) {
         return false;
      }

      double effectiveReach = this.effectiveReach();
      return this.distanceToHitboxSqr(target) <= effectiveReach * effectiveReach;
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
      AutoRod rod = OyVey.moduleManager.getModuleByClass(AutoRod.class);
      Vec3 rodPoint = rod == null ? null : rod.aimPoint(target);
      if (rodPoint != null) return this.rawRotation(rodPoint.x, rodPoint.y, rodPoint.z);
      AABB box = target.getBoundingBox();
      double headY = target.getEyeY();
      if (!this.grim.getValue() && this.rotMode.getValue() == KillauraRecode.RotationMode.PREDICT) {
         float ticks = this.predictTicks.getValue();
         Vec3 prediction = target.getDeltaMovement().scale(ticks);
         box = box.move(prediction);
         headY += prediction.y;
      }
      Vec3 eyes = mc.player.getEyePosition();
      boolean requireVisible = this.wallCheck.getValue();
      Vec3 fallback = CombatAim.headCenter(box, headY);
      Vec3 aim = CombatAim.visibleHeadPoint(box, headY, point -> !requireVisible || this.rayMiss(eyes, point)).orElse(fallback);
      if (this.rotMode.getValue() == RotationMode.POLAR) {
         Vec3 varied = this.polarRotation.aimPoint(box,headY,target.getId(),mc.player.tickCount);
         if (!requireVisible || this.rayMiss(eyes,varied)) aim = varied;
      }
      KnockbackDisplace displace = OyVey.moduleManager.getModuleByClass(KnockbackDisplace.class);
      if (displace != null) aim = displace.aimPoint(target, eyes, aim, this.wallCheck.getValue());
      return this.rawRotation(aim.x, aim.y, aim.z);
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
      if (this.rotMode.getValue() == RotationMode.POLAR) {
         var varied = this.polarRotation.step(this.sentYaw,this.sentPitch,tYaw,tPitch,
            this.advRotMin.getValue(),this.advRotMax.getValue(),this.currentTarget.getId(),mc.player.tickCount);
         float[] result = this.quantizeRotation(varied.yaw(),varied.pitch());
         this.smoothYaw = result[0]; this.smoothPitch = result[1];
         return result;
      }
      if (this.rotMode.getValue() == RotationMode.INSTANT || this.rotMode.getValue() == RotationMode.ONE_TICK) {
         float yaw = this.sentYaw + Mth.wrapDegrees(tYaw - this.sentYaw);
         float[] rotation = this.grim.getValue() || this.gcdFix.getValue()
            ? this.quantizeRotation(yaw, tPitch) : new float[]{yaw, Mth.clamp(tPitch, -90, 90)};
         this.smoothYaw = rotation[0];
         this.smoothPitch = rotation[1];
         return rotation;
      }
      if (this.grim.getValue() && this.rotMode.getValue() != RotationMode.RECORDED) {
         // Step from the last sent angle so changing modes cannot snap the
         // server rotation to an old smoothing or recorded-pattern state.
         float speed = this.sampledRotationSpeed();
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
         float[] desired = CombatAim.recordedOffset(mc.player.getEyePosition(), this.currentTarget.getBoundingBox(),
            tYaw, tPitch, this.recordedYawError, this.recordedPitchError);
         float desiredYaw = desired[0];
         float desiredPitch = desired[1];
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
         if (this.recordedPatternIndex == 1) this.recordedReplaySpeed = recordedSpeed;
         else this.recordedReplaySpeed += (recordedSpeed - this.recordedReplaySpeed) * 0.55F;
         if (targetDistance > 1.0E-4F) {
            float step = Math.min(targetDistance, this.recordedReplaySpeed);
            this.smoothYaw += deltaYaw / targetDistance * step;
            this.smoothPitch = Mth.clamp(this.smoothPitch + deltaPitch / targetDistance * step, -90.0F, 90.0F);
         }

         tYaw = this.smoothYaw;
         tPitch = this.smoothPitch;
      } else if (this.rotMode.getValue() == KillauraRecode.RotationMode.SMOOTH || this.rotMode.getValue() == RotationMode.PREDICT) {
         float speed = this.sampledRotationSpeed();
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
      return this.grim.getValue() || this.gcdFix.getValue() ? this.quantizeRotation(continuousYaw, tPitch) : new float[]{continuousYaw, tPitch};
   }

   private float sampledRotationSpeed() {
      float lo = Math.min(this.advRotMin.getValue(), this.advRotMax.getValue());
      float hi = Math.max(this.advRotMin.getValue(), this.advRotMax.getValue());
      return lo + ThreadLocalRandom.current().nextFloat() * (hi - lo);
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
      var beforeMotion = mc.player.getDeltaMovement();
      boolean beforeSprint = mc.player.isSprinting();
      float strength = mc.player.getAttackStrengthScale(0.5F);
      boolean emptyHand = mc.player.getMainHandItem().isEmpty();
      boolean sword = mc.player.getMainHandItem().is(ItemTags.SWORDS);
      float cameraYaw = mc.player.getYRot(), cameraPitch = mc.player.getXRot();
      WTap.onAttack();
      // The server processes this attack with the preceding transmitted look
      // and sprint state. Match native local attack simulation to that state
      // before preparing the next physics tick; no extra look packets are sent.
      ((LocalPlayerRotationAccessor)mc.player).oyvey$sendSprintState();
      try {
         mc.player.setYRot(this.sentYaw);
         mc.player.setXRot(this.sentPitch);
         mc.gameMode.attack(mc.player, target);
      } finally {
         mc.player.setYRot(cameraYaw);
         mc.player.setXRot(cameraPitch);
      }
      this.lastAttackTrace = "tick=" + mc.player.tickCount + " empty=" + emptyHand + " sword=" + sword
         + " strength=" + strength + " attackLook=" + this.sentYaw + "," + this.sentPitch
         + " reach=" + this.effectiveReach() + " sprint=" + beforeSprint + "->" + mc.player.isSprinting()
         + " motion=" + beforeMotion + "->" + mc.player.getDeltaMovement();
      if (this.armSwing.getValue()) {
         mc.player.swing(InteractionHand.MAIN_HAND);
      } else {
         mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
      }
   }

   private boolean shouldAttack(boolean breakingShield) {
      if (!this.canAttackNow(this.currentTarget)) {
         return false;
      }

      return this.attackTimingReady(breakingShield);
   }

   private boolean attackTimingReady(boolean breakingShield) {
      return this.attackTimingReady(breakingShield,false);
   }

   private boolean attackTimingReady(boolean breakingShield, boolean preparingAim) {
      if (!this.grim.getValue() && this.rotMode.getValue() != RotationMode.INSTANT && this.rotMode.getValue() != RotationMode.ONE_TICK
         && this.rotMode.getValue() != RotationMode.POLAR
         && this.rotMode.getValue() != RotationMode.RECORDED && this.style.getValue() && !this.styleAtTarget) {
         return false;
      }

      CriticalTiming.State critical = this.criticalState();
      if (preparingAim && CriticalTiming.suppressSprint(this.preferCrit.getValue(),this.onlyCrits.getValue(),critical)) {
         // The native critical sprint suppression runs before the later attack.
         // It cannot depend on a pulse which has not yet been allowed to arm.
         critical = new CriticalTiming.State(critical.grounded(),critical.jumpRequested(),critical.verticalMotion(),
            critical.fallDistance(),critical.normalMovement(),critical.mobilityRestricted(),false,critical.livingTarget());
      }
      if (!CriticalTiming.allowsAttack(this.preferCrit.getValue(), this.onlyCrits.getValue(), critical)) return false;
      boolean requiresCrit = CriticalTiming.requiresCrit(this.preferCrit.getValue(), this.onlyCrits.getValue(), critical);
      boolean baseOk = this.clickMode.getValue() == ClickMode.RECORDED
         ? (preparingAim ? this.recordedClicks.dueWithin(System.nanoTime(),50_000_000L) : this.recordedClicksDue > 0)
            && (!requiresCrit || mc.player.getAttackStrengthScale(0) >= 1)
         : ShieldCombat.timingReady(breakingShield, this.cooldown.getValue(),
         mc.player.getAttackStrengthScale(0), requiresCrit || this.grim.getValue() ? 1 : 0.9F,
         System.currentTimeMillis() - this.lastAttackTime >= this.nextAttackDelay);

      return !baseOk ? false : !this.hitSelect.getValue() || this.hitSelectGate();
   }

   private CriticalTiming.State criticalState() {
      boolean normalMovement = !mc.player.onClimbable() && !mc.player.isInWater() && !mc.player.isInLava()
         && !mc.player.isPassenger() && !mc.player.isFallFlying() && !mc.player.getAbilities().flying;
      return new CriticalTiming.State(mc.player.onGround(), mc.options.keyJump.isDown(), mc.player.getDeltaMovement().y,
         mc.player.fallDistance, normalMovement, mc.player.isMobilityRestricted(), mc.player.isSprinting(),
         this.currentTarget instanceof LivingEntity);
   }

   public boolean isCriticalSprintSuppressed() {
      return this.isEnabled() && !nullCheck() && this.targetRots != null && this.currentTarget != null
         && this.currentTarget.isAlive() && this.isWithinAttackRange(this.currentTarget)
         && CriticalTiming.suppressSprint(this.preferCrit.getValue(), this.onlyCrits.getValue(), this.criticalState());
   }

   public void applyCriticalSprintInput(ClientInput input) {
      if (!this.isCriticalSprintSuppressed()) return;
      Input keys = input.keyPresses;
      input.keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(),
         keys.jump(), keys.shift(), false);
      mc.player.setSprinting(false);
   }

   private boolean canAttackNow(Entity target) {
      return target != null
         && target == this.currentTarget
         && target.isAlive()
         && !target.isRemoved()
         && this.targetRots != null
         && (this.rotMode.getValue() != RotationMode.ONE_TICK || this.oneTickStep == OneTickAttackRotation.Step.ATTACK)
         && this.hasSentRot
         && target.getId() == this.sentTargetId
         && this.isWithinAttackRange(target)
         // Grim always checks the rotation sent on the preceding tick, even
         // when the separate Raycast setting is disabled.
         && (!(this.grim.getValue() || this.raycast.getValue() || this.rotMode.getValue() == RotationMode.ONE_TICK
            || this.rotMode.getValue() == RotationMode.POLAR)
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
      if (mode == BlockMode.LEGIT) return; // Native input, not a use-item packet loop.
      if (mode == BlockMode.SHIELD) { this.updateShieldBlock(); return; }
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

   private BlockMode effectiveBlockMode() {
      BlockMode selected = this.blockMode.getValue();
      if (selected == BlockMode.OFF || selected == BlockMode.FAKE || selected == BlockMode.LEGIT) return selected;
      if (this.fakeAutoBlock.getValue() && !isFullBlockMode(selected) && selected != BlockMode.SHIELD) return BlockMode.FAKE;
      return ViaProtocol.usesSwordBlocking() ? (selected == BlockMode.SHIELD ? BlockMode.FULL2 : selected) : BlockMode.SHIELD;
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
      if (!this.serverBlockHeld || nullCheck() || this.grim.getValue() || !isFullBlockMode(this.effectiveBlockMode())
         || this.currentTarget == null || !this.currentTarget.isAlive()
         || this.targetRots == null && this.rotMode.getValue() != RotationMode.ONE_TICK
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

   private boolean usingShield() {
      return mc.player != null && mc.player.isUsingItem() && mc.player.getUseItem().getItem() instanceof ShieldItem;
   }

   private void clearShieldOwnership() {
      this.shieldUseHand = null; this.shieldUseOwner = null; this.shieldUseLevel = null;
   }

   private void stopShieldBlock() {
      if (this.shieldUseHand != null && this.shieldUseOwner == mc.player && this.shieldUseLevel == mc.level
         && this.usingShield() && mc.player.getUsedItemHand() == this.shieldUseHand && mc.gameMode != null)
         mc.gameMode.releaseUsingItem(mc.player);
      this.clearShieldOwnership();
   }

   private void updateShieldBlock() {
      if (mc.screen != null || !mc.player.isAlive()) { this.stopShieldBlock(); return; }
      InteractionHand hand = mc.player.getOffhandItem().getItem() instanceof ShieldItem ? InteractionHand.OFF_HAND
         : mc.player.getMainHandItem().getItem() instanceof ShieldItem ? InteractionHand.MAIN_HAND : null;
      if (hand == null || mc.player.isSpectator() || mc.player.getCooldowns().isOnCooldown(mc.player.getItemInHand(hand))) {
         this.stopShieldBlock(); return;
      }
      if (mc.player.isUsingItem()) {
         // Never replace food/potions or repeatedly restart a raised shield.
         if (!this.usingShield() || mc.player.getUsedItemHand() != hand) this.clearShieldOwnership();
         return;
      }
      this.clearShieldOwnership();
      float yaw = mc.player.getYRot(), pitch = mc.player.getXRot();
      try {
         if (this.hasSentRot) { mc.player.setYRot(this.sentYaw); mc.player.setXRot(this.sentPitch); }
         mc.gameMode.useItem(mc.player, hand);
      } finally { mc.player.setYRot(yaw); mc.player.setXRot(pitch); }
      if (this.usingShield() && mc.player.getUsedItemHand() == hand) {
         this.shieldUseHand = hand; this.shieldUseOwner = mc.player; this.shieldUseLevel = mc.level;
      }
   }

   public boolean holdsShieldInput() {
      if (!this.isEnabled() || nullCheck() || mc.screen != null || this.effectiveBlockMode() != BlockMode.SHIELD
         || this.shieldUseOwner != mc.player || this.shieldUseLevel != mc.level || this.shieldUseHand == null
         || !this.usingShield() || mc.player.getUsedItemHand() != this.shieldUseHand
         || this.currentTarget == null || !this.currentTarget.isAlive() || !this.isWithinSearchRange(this.currentTarget)) return false;
      AutoRod rod = OyVey.moduleManager.getModuleByClass(AutoRod.class);
      return rod == null || !rod.active();
   }

   private boolean canLegitBlockClick() {
      boolean eligible=this.isEnabled() && this.blockMode.getValue()==BlockMode.LEGIT && !nullCheck()
         && (OyVey.recordedClickManager == null || !OyVey.recordedClickManager.isRecording())
         && mc.gameMode!=null && mc.screen==null && mc.player.isAlive() && !mc.player.isSpectator()
         && this.currentTarget!=null && this.currentTarget.isAlive() && this.isWithinAttackRange(this.currentTarget)
         && (mc.player.getMainHandItem().is(ItemTags.SWORDS)
            || mc.player.getMainHandItem().getItem() instanceof ShieldItem
            || mc.player.getOffhandItem().getItem() instanceof ShieldItem
               && !(mc.player.getMainHandItem().getItem() instanceof net.minecraft.world.item.BlockItem));
      if (eligible) {
         Scaffold scaffold=OyVey.moduleManager.getModuleByClass(Scaffold.class);
         AutoRod rod=OyVey.moduleManager.getModuleByClass(AutoRod.class);
         eligible=(scaffold==null||!scaffold.isEnabled()) && (rod==null||!rod.active())
            && mc.player.tickCount>=this.resumeAfterCorrectionTick;
      }
      return eligible && (!mc.player.isUsingItem() || this.ownsLegitUse());
   }

   /** Consumed by vanilla handleKeybinds, so it follows the real right-click path. */
   public void beginLegitBlockInput() {
      this.legitClickQueued = false;
      this.legitClickDue = this.legitClicks.consume(this.canLegitBlockClick(), this.legitBlockCps.getValue(), System.nanoTime());
      // Vanilla drains use clicks in its already-using-item branch. Release
      // an owned shield before that branch so the next press is actually used.
      if (this.legitClickDue && this.ownsLegitUse()) {
         mc.gameMode.releaseUsingItem(mc.player);
         this.clearLegitUse();
         this.clearShieldOwnership();
      }
   }

   public boolean consumeLegitBlockClick() {
      if (!this.legitClickDue || !this.canLegitBlockClick()) { this.legitClickDue = false; return false; }
      this.legitClickDue=false; this.legitClickQueued=true;
      return true;
   }

   public void afterNativeLegitClick() {
      if (!this.legitClickQueued) return;
      this.legitClickQueued=false;
      if (!this.isEnabled() || nullCheck() || this.blockMode.getValue()!=BlockMode.LEGIT) return;
      if (mc.player.isUsingItem() && (this.usingShield() || mc.player.getUseItem().is(ItemTags.SWORDS))) {
         this.legitUseOwner = mc.player; this.legitUseLevel = mc.level; this.legitUseHand = mc.player.getUsedItemHand();
      }
      if (this.usingShield()) {
         this.shieldUseHand=mc.player.getUsedItemHand(); this.shieldUseOwner=mc.player; this.shieldUseLevel=mc.level;
      }
   }

   private boolean ownsLegitUse() {
      return mc.player != null && this.legitUseOwner == mc.player && this.legitUseLevel == mc.level
         && this.legitUseHand != null && mc.player.isUsingItem() && mc.player.getUsedItemHand() == this.legitUseHand
         && (this.usingShield() || mc.player.getUseItem().is(ItemTags.SWORDS));
   }

   private void clearLegitUse() {
      this.legitUseOwner = null; this.legitUseLevel = null; this.legitUseHand = null;
   }

   private boolean prepareShieldBreakSwap() {
      if (mc.screen != null) { this.restoreShieldBreakSwap(); return false; }
      if (this.shieldSwap.active() && !this.shieldSwap.owns(mc.player.getInventory().getSelectedSlot(),
         mc.player.getMainHandItem().getItem() instanceof AxeItem, this.shieldSwapOwner == mc.player && this.shieldSwapLevel == mc.level)) {
         // A manual hotbar change or a new player/world takes priority.
         this.shieldSwap.clear(); this.shieldSwapOwner = null; this.shieldSwapLevel = null;
         this.shieldBreakRetryUntil = System.currentTimeMillis() + 250;
      }
      boolean blocking = this.shieldBreak.getValue() && this.currentTarget instanceof LivingEntity living
         && living.isBlocking() && living.getUseItem().getItem() instanceof ShieldItem && this.isWithinAttackRange(living);
      if (!blocking || this.shieldSwap.active() && this.shieldSwap.target() != this.currentTarget.getId()) {
         this.restoreShieldBreakSwap(); return false;
      }
      if (System.currentTimeMillis() < this.shieldBreakRetryUntil) return false;
      if (mc.player.getMainHandItem().getItem() instanceof AxeItem) return true;
      if (mc.player.isUsingItem() && !this.usingShield() && !this.serverBlockHeld) return false;
      for (int slot = 0; slot < 9; slot++) {
         if (!(mc.player.getInventory().getItem(slot).getItem() instanceof AxeItem)) continue;
         this.stopServerBlock();
         if (this.usingShield() && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            mc.gameMode.releaseUsingItem(mc.player); this.clearShieldOwnership();
         }
         this.shieldSwap.begin(mc.player.getInventory().getSelectedSlot(), slot, this.currentTarget.getId());
         this.shieldSwapOwner = mc.player; this.shieldSwapLevel = mc.level;
         mc.player.getInventory().setSelectedSlot(slot);
         mc.gameMode.ensureHasSentCarriedItem();
         return true;
      }
      return false;
   }

   private void restoreShieldBreakSwap() {
      int slot = this.shieldSwap.finish(mc.player == null ? -1 : mc.player.getInventory().getSelectedSlot(),
         mc.player != null && mc.player.getMainHandItem().getItem() instanceof AxeItem,
         this.shieldSwapOwner == mc.player && this.shieldSwapLevel == mc.level, this.shieldSwapBack.getValue());
      this.shieldSwapOwner = null; this.shieldSwapLevel = null;
      if (slot >= 0 && mc.gameMode != null) {
         mc.player.getInventory().setSelectedSlot(slot); mc.gameMode.ensureHasSentCarriedItem();
      }
   }

   public void applyGrimMovementCorrection(ClientInput input) {
      this.correctedMovementInput = null;
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

      // Legacy protocols keep diagonals raw until after slowdown. Normalizing
      // here would change acceleration compared with Via's native input path.
      ((ClientInputAccessor)input).oyvey$setMoveVector(
         me.alpha432.oyvey.util.KeyboardMovement.vector(corrected)
      );
      this.correctedMovementInput = input;
      this.correctedMovementYaw = this.targetRots[0];
      if (!this.grimSprintAllowed) {
         mc.player.setSprinting(false);
      }
   }

   public boolean isGrimMovementCorrectionActive() {
      return this.isGrimMovementCorrectionConfigured() && this.targetRots != null && this.hasSaved;
   }

   public float movementInputYaw(ClientInput input, float cameraYaw) {
      // Input survives POST's camera restoration and can still be in use at
      // the next attack, before the next keyboard tick has been calculated.
      return input == this.correctedMovementInput ? this.correctedMovementYaw : cameraYaw;
   }

   public boolean isGrimMovementCorrectionConfigured() {
      KnockbackDisplace displace = OyVey.moduleManager.getModuleByClass(KnockbackDisplace.class);
      return this.isEnabled() && (this.grim.getValue() || this.correctMove.getValue() && this.moveFix.getValue() == KillauraRecode.MoveFix.GRIM
         || displace != null && displace.requiresMovementFix());
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
         && this.currentTarget != null && this.currentTarget.isAlive()
         && (this.targetRots != null || this.rotMode.getValue() == RotationMode.ONE_TICK)
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

   public boolean ownsMainHandBlock() {
      return this.serverBlockHeld && mc.player != null && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND;
   }

   public boolean ownsAutoBlockUse() {
      return this.ownsMainHandBlock() || this.shieldUseHand != null && this.shieldUseOwner == mc.player
         && this.shieldUseLevel == mc.level && this.usingShield() && mc.player.getUsedItemHand() == this.shieldUseHand;
   }

   public enum ClickMode { NORMAL, RECORDED }

   public enum BlockMode {
      OFF,
      LEGIT,
      PACKET,
      FULL,
      FULL2,
      VANILLA,
      SHIELD,
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
      RECORDED,
      ONE_TICK,
      POLAR;
   }

   public enum TargetMode {
      CLOSEST,
      LOWEST_HP,
      HIGHEST_HP,
      ANGLE;
   }
}
