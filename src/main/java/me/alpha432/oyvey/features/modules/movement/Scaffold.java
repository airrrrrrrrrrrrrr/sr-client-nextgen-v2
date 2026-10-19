package me.alpha432.oyvey.features.modules.movement;

import java.awt.Color;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.movement.scaffold.PlacementPlanner;
import me.alpha432.oyvey.features.modules.movement.scaffold.PlacementConfirmations;
import me.alpha432.oyvey.features.modules.movement.scaffold.SilentHeadPose;
import me.alpha432.oyvey.features.modules.movement.scaffold.ScaffoldMovement;
import me.alpha432.oyvey.features.modules.movement.scaffold.BackwardBridgeRotation;
import me.alpha432.oyvey.features.modules.movement.scaffold.TellyBridge;
import me.alpha432.oyvey.features.modules.movement.scaffold.Telly2Bridge;
import me.alpha432.oyvey.features.modules.movement.scaffold.OneTickBridgeRotation;
import me.alpha432.oyvey.features.modules.movement.scaffold.GodBridge;
import me.alpha432.oyvey.features.modules.movement.scaffold.PlacementClickRate;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.mixin.render.accessor.ClientInputAccessor;
import me.alpha432.oyvey.mixin.render.accessor.LocalPlayerRotationAccessor;
import me.alpha432.oyvey.util.SilentRotationMath;
import me.alpha432.oyvey.util.ViaProtocol;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.CameraType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends Module {
   public static volatile boolean isRotating;
   public static volatile float visualYaw, visualPitch;

   public final Setting<Mode> mode = this.register(new Setting<>("Mode", Mode.NORMAL));
   public final Setting<RotationMode> rotation = this.register(new Setting<RotationMode>("Rotation", RotationMode.BACKWARDS) {
      @Override public String currentEnumName() {
         return this.getValue() == RotationMode.ONE_TICK ? "One Tick" : super.currentEnumName();
      }
   });
   public final Setting<Boolean> autoSwitch = this.bool("AutoSwitch", true);
   public final Setting<Boolean> restoreSlot = this.bool("RestoreSlot", true);
   public final Setting<Boolean> keepY = this.bool("KeepY", false);
   public final Setting<Boolean> sprint = this.bool("Sprint", false);
   public final Setting<Boolean> thirdPerson = this.bool("ThirdPerson", false);
   public final Setting<Integer> placeDelay = this.num("PlaceDelay", 0, 0, 10);
   public final Setting<Boolean> randomCps = this.bool("RandomCPS", false);
   public final Setting<Integer> cps = this.register(new Setting<>("CPS",40,1,40,v -> !this.randomCps.getValue()));
   public final Setting<Integer> minCps = this.register(new Setting<>("MinCPS",10,1,40,v -> this.randomCps.getValue()));
   public final Setting<Integer> maxCps = this.register(new Setting<>("MaxCPS",14,1,40,v -> this.randomCps.getValue()));
   public final Setting<Boolean> blockCounter = this.bool("BlockCounter", true);
   public final Setting<Boolean> blockESP = this.bool("BlockESP", true);

   private PlacementPlanner.Placement pending;
   private PlacementPlanner.Placement centerTarget;
   private BlockPos lastPlaced;
   private int startSlot = -1, lockedLayer, lastPlaceTick = -100;
   private boolean ownsSlot, injectedRotation;
   private boolean switchedCamera;
   private InteractionHand placeHand;
   private final PlacementConfirmations confirmations = new PlacementConfirmations();
   private final SilentHeadPose headPose = new SilentHeadPose();
   private final BackwardBridgeRotation bridgeRotation = new BackwardBridgeRotation();
   private final TellyBridge telly = new TellyBridge();
   private final Telly2Bridge telly2 = new Telly2Bridge();
   private final OneTickBridgeRotation oneTick = new OneTickBridgeRotation();
   private final GodBridge godbridge = new GodBridge();
   private final PlacementClickRate clickRate = new PlacementClickRate();
   private boolean godbridgeJump;
   private boolean oneTickAiming, oneTickReturnAfterAttempt;
   private TellyBridge.Step tellyStep;
   private Mode lastMode;
   private RotationMode lastRotationMode;
   private RotationMode activeRotationMode = RotationMode.BACKWARDS;
   private ClientLevel trackedLevel;
   private volatile float sentYaw, sentPitch;
   private volatile boolean hasSentRotation;
   private float rotationYaw, rotationPitch;
   private boolean placing, sprintAllowed;
   private boolean rotationPrimed;
   private boolean placedThisTick;
   private boolean diagonalMovement;
   private boolean clutchRecovery;
   private PlacementPlanner.Placement clutchTarget;
   private PlacementPlanner.Placement clickedPlacement;
   private Vec3 clickEyes;
   private int lastTraceTick;
   private int resumeAfterCorrectionTick;
   private String placementStatus = "idle";

   public Scaffold() {
      super("Scaffold", "Places hotbar blocks beneath your feet using visible support faces.", Category.MOVEMENT);
      this.sprint.setVisibility(value -> !this.isTellyMode() && this.mode.getValue() != Mode.GODBRIDGE);
      this.keepY.setVisibility(value -> this.mode.getValue() != Mode.TELLY2 && this.mode.getValue() != Mode.GODBRIDGE);
      this.rotation.setVisibility(value -> this.mode.getValue() != Mode.GODBRIDGE);
   }

   @Override
   public void onEnable() {
      this.clickRate.reset();
      this.godbridge.reset(); this.godbridgeJump = false;
      this.pending = null;
      this.clutchTarget = null;
      this.clutchRecovery = false;
      this.centerTarget = null;
      this.lastPlaced = null;
      this.lastPlaceTick = -100;
      this.ownsSlot = false;
      this.injectedRotation = false;
      this.headPose.clear();
      this.bridgeRotation.reset();
      this.telly.reset(); this.telly2.reset();
      this.oneTick.reset();
      this.tellyStep = null;
      this.lastMode = this.mode.getValue();
      this.lastRotationMode = this.rotation.getValue();
      this.activeRotationMode = this.lastRotationMode;
      this.hasSentRotation = false;
      this.sprintAllowed = false;
      this.rotationPrimed = false;
      this.lastTraceTick = -100;
      this.resumeAfterCorrectionTick = 0;
      this.syncWorld();
      isRotating = false;
      this.captureLastSentRotation();
      this.startSlot = mc.player == null ? -1 : mc.player.getInventory().getSelectedSlot();
      this.lockedLayer = mc.player == null ? 0 : PlacementPlanner.blockLayer(mc.player.getY());
      this.syncThirdPerson();
   }

   @Override
   public void onDisable() {
      this.clickRate.reset();
      this.godbridge.reset(); this.godbridgeJump = false;
      this.oneTick.reset();
      this.clutchTarget = null;
      this.clutchRecovery = false;
      this.restoreCamera();
      this.restoreRotation();
      this.headPose.clear();
      this.pending = null;
      this.centerTarget = null;
      this.telly.reset(); this.telly2.reset();
      this.tellyStep = null;
      if (this.restoreSlot.getValue() && this.ownsSlot && this.startSlot >= 0 && mc.player != null && mc.gameMode != null) {
         mc.player.getInventory().setSelectedSlot(this.startSlot);
         // Vanilla synchronizes the restored slot on its next game-mode tick.
         // Disabling from a key/UI callback must not send an out-of-tick action.
      }
      this.ownsSlot = false;
      this.lastPlaced = null;
   }

   @Subscribe(priority = 100)
   public void onWalking(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == Stage.MOTION_PRE) {
         this.syncThirdPerson();
         this.preparePlacement();
         this.prepareClutchRecovery();
         this.placePending();
         this.completeTellyCorner();
         this.updateGodbridgeJump();
         this.returnOneTickAfterAttempt();
         this.recoverTellyAfterPlacement();
         this.advanceClutchAfterPlacement();
         this.advanceCenterAfterPlacement();
         this.tracePlacement();
      } else if (event.getStage() == Stage.PRE && this.injectedRotation) {
         // Physics has finished. Only pitch changes here; the backward yaw that
         // drove movement stays the same for the outgoing movement packet.
         this.refreshPlacement();
         this.keepClickedSupportPitch();
      }
   }

   @Subscribe(priority = -1000)
   public void onWalkingPost(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == Stage.POST) {
         // EventManager snapshots the physics yaw in PRE and restores it in
         // POST. Camera cleanup must run after that shared restoration.
         this.restoreRotation();
         this.pending = null;
      }
   }

   private void preparePlacement() {
      this.restoreRotation();
      this.headPose.beginTick();
      this.pending = null;
      this.placedThisTick = false;
      this.clutchRecovery = false;
      this.clickedPlacement = null;
      this.placementStatus = "inactive";
      this.sprintAllowed = false;
      this.godbridgeJump = false;
      this.tellyStep = null;
      this.syncWorld();
      if (this.lastMode != this.mode.getValue() || this.lastRotationMode != this.rotation.getValue()) {
         this.lastMode = this.mode.getValue();
         this.lastRotationMode = this.rotation.getValue();
         this.godbridge.reset();
         this.telly.reset(); this.telly2.reset();
         this.oneTick.reset();
         this.bridgeRotation.reset();
         this.rotationPrimed = false;
         this.centerTarget = null;
      }
      if (nullCheck() || mc.gameMode == null || mc.screen != null || mc.player.isSpectator()
         || mc.player.isPassenger() || mc.player.isUsingItem()) { this.clickRate.reset(); this.telly.reset(); this.telly2.reset(); this.oneTick.reset(); this.godbridge.reset(); return; }
      if (mc.player.tickCount < this.resumeAfterCorrectionTick) { this.placementStatus = "reconciling"; return; }
      if (this.startSlot < 0) {
         this.startSlot = mc.player.getInventory().getSelectedSlot();
         this.lockedLayer = PlacementPlanner.blockLayer(mc.player.getY());
      }
      int layer = this.placementLayer();
      // A locked bridge layer must never intersect the player after dropping below it.
      if (layer + 1 > mc.player.getY() + 0.001) { this.telly.reset(); this.telly2.reset(); this.oneTick.reset(); return; }
      if (!this.selectBlocks()) { this.telly.reset(); this.telly2.reset(); this.oneTick.reset(); return; }
      visualYaw = mc.player.getYRot();
      visualPitch = mc.player.getXRot();
      // Read camera-space keys, not the previous tick's corrected WASD state.
      Input keys = new Input(mc.options.keyUp.isDown(), mc.options.keyDown.isDown(),
         mc.options.keyLeft.isDown(), mc.options.keyRight.isDown(), false, false, false);
      this.diagonalMovement = ScaffoldMovement.diagonalBridge(keys, visualYaw);
      RotationMode active = this.mode.getValue() == Mode.GODBRIDGE ? RotationMode.BACKWARDS
         : this.rotation.getValue() == RotationMode.CENTER && this.diagonalMovement
         ? RotationMode.BACKWARDS : this.rotation.getValue();
      if (active != this.activeRotationMode) {
         this.activeRotationMode = active;
         this.bridgeRotation.reset();
         this.rotationPrimed = false;
         this.centerTarget = null;
      }
      if (this.mode.getValue() == Mode.GODBRIDGE) { this.prepareGodbridge(keys,layer); return; }
      if (this.isTellyMode()) {
         this.prepareTelly(keys);
         return;
      }
      if (active == RotationMode.ONE_TICK) { this.prepareOneTick(keys); return; }
      var candidates = this.catchTargets(layer);
      var transmittedPlacement = this.transmittedPlacement(candidates);
      SilentRotationMath.Rotation rotation;
      if (this.activeRotationMode == RotationMode.CENTER) {
         var aim = this.centerAim(candidates);
         rotation = aim == null ? new SilentRotationMath.Rotation(this.sentYaw, this.sentPitch) : aim.rotation();
         this.centerTarget = aim == null ? null : aim.placement();
         this.rotationPrimed = aim != null;
      } else {
         var backwardLook = this.bridgeRotation.next(keys, visualYaw, this.sentYaw, this.sentPitch, 82, mc.options.sensitivity().get());
         float desiredPitch = PlacementPlanner.backwardPitch(candidates, mc.player.getEyePosition(),
            mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), backwardLook.yaw(), this.sentPitch,
            mc.options.sensitivity().get(), this.world());
         rotation = this.bridgeRotation.next(keys, visualYaw,
            this.sentYaw, this.sentPitch, desiredPitch, mc.options.sensitivity().get());
         this.rotationPrimed |= Math.abs(Mth.wrapDegrees(this.bridgeRotation.heading(keys, visualYaw) - rotation.yaw())) < 1
            && rotation.pitch() >= 70;
      }
      if (transmittedPlacement != null && this.hasSentRotation && this.rotationPrimed) {
         // Complete a click with its already transmitted look. Turning toward
         // the next heading resumes when this support has been used.
         rotation = new SilentRotationMath.Rotation(this.sentYaw, this.sentPitch);
         if (this.activeRotationMode == RotationMode.CENTER) this.centerTarget = transmittedPlacement;
      }
      this.rotationYaw = rotation.yaw();
      this.rotationPitch = rotation.pitch();
      this.injectedRotation = true;
      isRotating = true;
      // Keep the model pose available after POST restores the camera. It is
      // consumed only during entity extraction, never during camera setup.
      this.headPose.aim(this.rotationYaw, this.rotationPitch, visualYaw, visualPitch);
      // This same yaw drives movement physics, corrected keys and sendPosition.
      OyVey.rotationManager.setPlayerRotations(this.rotationYaw, this.rotationPitch);
      OyVey.rotationManager.setServerRotations(this.rotationYaw, this.rotationPitch);
      this.pending = transmittedPlacement;
      this.placementStatus = this.pending == null ? "no-target" : "prepared";
   }

   private void syncThirdPerson() {
      if (this.thirdPerson.getValue()) {
         if (!this.switchedCamera) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            this.switchedCamera = true;
         }
      } else {
         this.restoreCamera();
      }
   }

   private void prepareGodbridge(Input keys, int layer) {
      var candidates = this.catchTargets(layer);
      float heading = GodBridge.aimYaw(keys,visualYaw);
      var turn = TellyBridge.turn(this.sentYaw,this.sentPitch,heading,75.5F,mc.options.sensitivity().get());
      float pitch = PlacementPlanner.backwardPitch(candidates,mc.player.getEyePosition(),
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE),turn.yaw(),this.sentPitch,
         mc.options.sensitivity().get(),this.world(),true);
      var look = TellyBridge.turn(this.sentYaw,this.sentPitch,heading,pitch,mc.options.sensitivity().get());
      this.pending = this.hasSentRotation && Math.abs(Mth.wrapDegrees(heading-this.sentYaw))<1
         ? this.transmittedPlacement(candidates) : null;
      this.injectedRotation = this.rotationPrimed = isRotating = true;
      this.applyOneTickLook(look);
      this.placementStatus = this.pending == null ? "godbridge-align" : "godbridge-place";
   }

   private void updateGodbridgeJump() {
      if (this.mode.getValue()!=Mode.GODBRIDGE || !this.injectedRotation || nullCheck()) return;
      Input keys=this.heldTellyMovement();
      boolean aligned=this.hasSentRotation && Math.abs(Mth.wrapDegrees(GodBridge.heading(keys,visualYaw)-this.sentYaw))<1;
      boolean headroom=!mc.options.keyShift.isDown() && !mc.player.isInWater() && !mc.player.isInLava()
         && !mc.player.onClimbable() && !mc.player.isFallFlying() && !mc.player.getAbilities().flying
         && mc.level.noCollision(mc.player,mc.player.getBoundingBox().move(0,.42,0));
      this.godbridgeJump=this.godbridge.jump(mc.player.onGround(),TellyBridge.moving(keys),aligned,headroom);
   }

   private void restoreCamera() {
      if (this.switchedCamera) {
         mc.options.setCameraType(CameraType.FIRST_PERSON);
         this.switchedCamera = false;
      }
   }

   private void refreshPlacement() {
      if (nullCheck()) return;
      if (this.activeRotationMode == RotationMode.ONE_TICK) {
         var actual = this.oneTick.refine(mc.player.getEyePosition(),
            mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.rotationYaw, this.rotationPitch,
            mc.options.sensitivity().get(), this.world());
         this.rotationPitch = actual.pitch();
         OyVey.rotationManager.setPlayerRotations(this.rotationYaw, this.rotationPitch);
         OyVey.rotationManager.setServerRotations(this.rotationYaw, this.rotationPitch);
         this.headPose.aim(this.rotationYaw, this.rotationPitch, visualYaw, visualPitch);
         return;
      }
      if (this.clutchRecovery) {
         // Recovery yaw already drove input/physics. Keep it until its normal
         // movement packet has been sent; never add an out-of-tick look.
         OyVey.rotationManager.setPlayerRotations(this.rotationYaw, this.rotationPitch);
         OyVey.rotationManager.setServerRotations(this.rotationYaw, this.rotationPitch);
         return;
      }
      if (this.isTellyMode() && this.tellyStep != null && this.tellyStep.lookForward()) {
         // Telly's forward travel look is left untouched by placement aim.
         OyVey.rotationManager.setPlayerRotations(this.rotationYaw, this.rotationPitch);
         OyVey.rotationManager.setServerRotations(this.rotationYaw, this.rotationPitch);
         this.pending = null;
         return;
      }
      int layer = this.placementLayer();
      var candidates = this.catchTargets(layer);
      if (this.activeRotationMode == RotationMode.CENTER) {
         var aim = PlacementPlanner.centerAim(candidates, mc.player.getEyePosition(),
            mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.rotationYaw, this.rotationPitch,
            mc.options.sensitivity().get(), this.world(), this.centerTarget, true);
         if (aim != null) {
            this.rotationPitch = aim.rotation().pitch();
            this.centerTarget = aim.placement();
         }
         OyVey.rotationManager.setPlayerRotations(this.rotationYaw, this.rotationPitch);
         OyVey.rotationManager.setServerRotations(this.rotationYaw, this.rotationPitch);
         this.headPose.aim(this.rotationYaw, this.rotationPitch, visualYaw, visualPitch);
         this.pending = null;
         return;
      }
      float pitch = PlacementPlanner.backwardPitch(candidates, mc.player.getEyePosition(),
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.rotationYaw,
         this.sentPitch, mc.options.sensitivity().get(), this.world(), this.cornerCatch());
      var rotation = SilentRotationMath.quantizeRotation(this.sentYaw, this.sentPitch, this.rotationYaw,
         this.rotationPitch + Mth.clamp(pitch - this.rotationPitch,
            this.isTellyMode() || this.mode.getValue()==Mode.GODBRIDGE ? -40 : -10,
            this.isTellyMode() || this.mode.getValue()==Mode.GODBRIDGE ? 40 : 10), mc.options.sensitivity().get());
      // Quantize pitch alone: never change the yaw used for this tick's physics.
      // The upcoming look must still reach the clicked support from the click
      // position, while preparing the next edge from the new player position.
      if (this.climbingTelly() || !this.placedThisTick || PlacementPlanner.lookIntersectsClickedSupport(this.clickedPlacement, this.clickEyes,
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.rotationYaw, rotation.pitch())) {
         this.rotationPitch = rotation.pitch();
      }
      OyVey.rotationManager.setPlayerRotations(this.rotationYaw, this.rotationPitch);
      OyVey.rotationManager.setServerRotations(this.rotationYaw, this.rotationPitch);
      this.headPose.aim(this.rotationYaw, this.rotationPitch, visualYaw, visualPitch);
      this.pending = PlacementPlanner.findAlongLook(candidates, mc.player.getEyePosition(),
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.rotationYaw, this.rotationPitch, this.world());
      this.placementStatus = this.pending == null ? "no-target-after-movement" : "ready";
   }

   private void prepareTelly(Input keys) {
      boolean manualJump = mc.options.keyJump.isDown();
      if ((!TellyBridge.moving(keys) && !manualJump) || mc.options.keyShift.isDown() || mc.player.isInWater() || mc.player.isInLava()
         || mc.player.isFallFlying() || mc.player.getAbilities().flying || mc.player.onClimbable()) {
         this.telly.reset(); this.telly2.reset();
         this.oneTick.reset();
         return;
      }
      float forwardYaw = TellyBridge.forwardYaw(keys, visualYaw);
      float backwardYaw = forwardYaw + TellyBridge.BACKWARD_OFFSET;
      boolean backwardReady = this.activeRotationMode == RotationMode.ONE_TICK || this.hasSentRotation && (this.activeRotationMode == RotationMode.CENTER
         ? this.rotationPrimed : Math.abs(Mth.wrapDegrees(backwardYaw - this.sentYaw)) < 1 && this.sentPitch >= 45);
      boolean forwardReady = this.hasSentRotation && Math.abs(Mth.wrapDegrees(forwardYaw - this.sentYaw)) < 1;
      AABB feet = mc.player.getBoundingBox();
      boolean headroom = mc.level.noCollision(mc.player, feet.move(0, 0.42, 0));
      boolean climb = this.climbingTelly();
      int footLayer = PlacementPlanner.blockLayer(mc.player.getY());
      int landingLayer = climb ? footLayer : Math.min(PlacementPlanner.bridgeLayer(mc.player.getY(), this.lockedLayer, this.keepLayer()),
         this.telly.placementLayer(footLayer, false));
      boolean landingReady = PlacementPlanner.hasForwardLanding(mc.player.position(), mc.player.getDeltaMovement(),
         landingLayer, mc.player.getAttributeValue(Attributes.GRAVITY), forwardYaw, TellyBridge.moving(keys), this.world());
      this.tellyStep = this.mode.getValue() == Mode.TELLY2
         ? this.telly2.update(mc.player.onGround(), mc.player.getDeltaMovement().y, footLayer, headroom, landingReady)
         : this.telly.update(mc.player.onGround(), mc.player.getDeltaMovement().y,
            footLayer, backwardReady, forwardReady, headroom, climb, landingReady);
      if (this.activeRotationMode == RotationMode.ONE_TICK) { this.prepareOneTick(keys); return; }
      int layer = this.placementLayer();
      var candidates = this.catchTargets(layer);
      boolean forward = this.tellyStep.lookForward();
      float desiredYaw = forward ? forwardYaw : backwardYaw;
      var turn = TellyBridge.turn(this.sentYaw, this.sentPitch, desiredYaw, forward ? 15 : 82,
         mc.options.sensitivity().get());
      float pitch = forward ? 15 : PlacementPlanner.backwardPitch(candidates, mc.player.getEyePosition(),
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), turn.yaw(), this.sentPitch,
         mc.options.sensitivity().get(), this.world(), this.cornerCatch());
      var rotation = TellyBridge.turn(this.sentYaw, this.sentPitch, desiredYaw, pitch, mc.options.sensitivity().get());
      if (!forward && this.activeRotationMode == RotationMode.CENTER) {
         var aim = this.centerAim(candidates);
         rotation = aim == null ? new SilentRotationMath.Rotation(this.sentYaw, this.sentPitch) : aim.rotation();
         // Read the last chosen face before replacing it with the next aim.
         this.pending = this.transmittedPlacement(candidates);
         this.centerTarget = aim == null ? null : aim.placement();
      } else {
         this.pending = forward ? null : this.transmittedPlacement(candidates);
         if (forward) this.centerTarget = null;
      }
      if (this.pending != null && backwardReady) {
         rotation = new SilentRotationMath.Rotation(this.sentYaw, this.sentPitch);
         if (this.activeRotationMode == RotationMode.CENTER) this.centerTarget = this.pending;
      }
      this.rotationYaw = rotation.yaw();
      this.rotationPitch = rotation.pitch();
      this.rotationPrimed = this.activeRotationMode == RotationMode.CENTER
         || Math.abs(Mth.wrapDegrees(desiredYaw - this.rotationYaw)) < 1;
      this.injectedRotation = true;
      isRotating = true;
      this.headPose.aim(this.rotationYaw, this.rotationPitch, visualYaw, visualPitch);
      OyVey.rotationManager.setPlayerRotations(this.rotationYaw, this.rotationPitch);
      OyVey.rotationManager.setServerRotations(this.rotationYaw, this.rotationPitch);
      this.placementStatus = "telly-" + this.tellyStep.phase().name().toLowerCase();
   }

   private void prepareClutchRecovery() {
      if (this.mode.getValue()==Mode.GODBRIDGE) return;
      if (this.activeRotationMode == RotationMode.ONE_TICK) return;
      if (nullCheck() || mc.gameMode == null || mc.screen != null
         || mc.player.isSpectator() || mc.player.isPassenger()
         || mc.player.isUsingItem() || mc.player.isInWater() || mc.player.isInLava()
         || mc.player.getAbilities().flying || mc.player.isFallFlying()
         || mc.player.tickCount < this.resumeAfterCorrectionTick || !this.selectBlocks()) {
         this.clutchTarget = null;
         return;
      }
      Vec3 feet = mc.player.position(), motion = mc.player.getDeltaMovement();
      // Preserve the initial native forward takeoff, but prepare the catch
      // before descending. Waiting for negative Y lost a full click/turn tick.
      boolean forwardPhase = this.isTellyMode() && this.tellyStep != null && this.tellyStep.lookForward();
      // A prepared Center face already owns the catch. Replacing it with a
      // generic recovery look discarded its next-tick click during Telly.
      if (this.activeRotationMode == RotationMode.CENTER && !forwardPhase && this.centerTarget != null) return;
      if (forwardPhase && this.hasTellyForwardLanding()) { this.clutchTarget = null; return; }
      if (forwardPhase && !mc.player.onGround() && motion.y > 0.2) return;
      int layer = Math.min(this.placementLayer(), PlacementPlanner.blockLayer(feet.y));
      double gravity = mc.player.getAttributeValue(Attributes.GRAVITY);
      var world = this.world();
      if (PlacementPlanner.hasLanding(feet, motion, layer, gravity, world)) { this.clutchTarget = null; return; }
      // Use the shared complete footprint, including side/backward motion,
      // rather than Center's forward-progress filter or a fixed backward ray.
      var targets = PlacementPlanner.catchTargets(feet, motion, layer, true, true, gravity);
      Vec3 eyes = mc.player.getEyePosition(), nextEyes = eyes.add(motion);
      double reach = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      // A normal next-tick look being valid does not mean that its current
      // pending click is valid. In particular it must not discard the previous
      // recovery look before that look has placed its block.
      var step = PlacementPlanner.clutchStep(targets, eyes, nextEyes, reach, this.sentYaw, this.sentPitch,
         mc.options.sensitivity().get(), this.hasSentRotation, world, this.clutchTarget);
      if (step == null) return;
      if (!this.injectedRotation) { visualYaw = mc.player.getYRot(); visualPitch = mc.player.getXRot(); }
      this.clutchRecovery = true;
      this.pending = step.ready() ? step.placement() : null;
      this.clutchTarget = step.placement();
      this.centerTarget = this.clutchTarget;
      this.rotationYaw = step.rotation().yaw();
      this.rotationPitch = step.rotation().pitch();
      this.rotationPrimed = this.injectedRotation = true;
      isRotating = true;
      this.placementStatus = "clutch-recovery";
      this.headPose.aim(this.rotationYaw, this.rotationPitch, visualYaw, visualPitch);
      OyVey.rotationManager.setPlayerRotations(this.rotationYaw, this.rotationPitch);
      OyVey.rotationManager.setServerRotations(this.rotationYaw, this.rotationPitch);
   }

   private java.util.List<BlockPos> catchTargets(int layer) {
      return this.activeRotationMode == RotationMode.CENTER
         ? PlacementPlanner.centerTargets(mc.player.position(), mc.player.getDeltaMovement(), layer, !mc.player.onGround(),
            !this.keepLayer(), mc.player.getAttributeValue(Attributes.GRAVITY), this.world())
         : PlacementPlanner.catchTargets(mc.player.position(), mc.player.getDeltaMovement(), layer, !mc.player.onGround(),
            !this.keepLayer(), mc.player.getAttributeValue(Attributes.GRAVITY));
   }

   private PlacementPlanner.Aim centerAim(java.util.List<BlockPos> candidates) {
      Vec3 nextStep = this.predictedPlacementStep();
      Vec3 nextEyes = mc.player.getEyePosition().add(nextStep);
      // This look is transmitted after native movement. Prepare it from the
      // next eye position so diagonal motion does not cross to another face
      // between selecting the center and using the look on the next tick.
      if (ViaProtocol.usesSwordBlocking()) {
         return PlacementPlanner.centerPulseAim(candidates,nextEyes,nextEyes.add(this.predictedFollowupStep(nextStep)),
            mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE),this.sentYaw,this.sentPitch,
            mc.options.sensitivity().get(),this.world(),this.centerTarget);
      }
      return PlacementPlanner.centerAim(candidates, nextEyes,
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.sentYaw, this.sentPitch,
         mc.options.sensitivity().get(), this.world(), this.centerTarget, false);
   }

   private PlacementPlanner.Placement transmittedPlacement(java.util.List<BlockPos> candidates) {
      double reach = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      return this.activeRotationMode == RotationMode.CENTER
         ? PlacementPlanner.findAlongCenterLook(this.centerTarget, candidates, mc.player.getEyePosition(), reach,
            this.sentYaw, this.sentPitch, this.world())
         : PlacementPlanner.findAlongLook(candidates, mc.player.getEyePosition(), reach, this.sentYaw, this.sentPitch, this.world());
   }

   private void advanceCenterAfterPlacement() {
      if (!this.placedThisTick || this.activeRotationMode != RotationMode.CENTER || this.clutchRecovery
         || this.isTellyMode() && this.tellyStep != null && this.tellyStep.lookForward()) return;
      var aim = this.centerAim(this.catchTargets(this.placementLayer()));
      if (aim == null) return;
      Vec3 postEyes = mc.player.getEyePosition().add(this.predictedPlacementStep());
      if (ViaProtocol.usesSwordBlocking() && !PlacementPlanner.lookIntersectsClickedSupport(this.clickedPlacement, postEyes,
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.sentYaw, aim.rotation().pitch())) return;
      // The click has already used the transmitted look. Prepare the next
      // center before physics/input, rather than spending an extra tick looking
      // at the consumed face. That click was validated with its actual preceding
      // transmitted look; the next native look belongs to the next placement.
      this.centerTarget = aim.placement();
      this.rotationYaw = aim.rotation().yaw();
      this.rotationPitch = aim.rotation().pitch();
      this.headPose.aim(this.rotationYaw, this.rotationPitch, visualYaw, visualPitch);
      OyVey.rotationManager.setPlayerRotations(this.rotationYaw, this.rotationPitch);
      OyVey.rotationManager.setServerRotations(this.rotationYaw, this.rotationPitch);
   }

   private int placementLayer() {
      if (this.mode.getValue() == Mode.GODBRIDGE) {
         return this.godbridge.placementLayer(PlacementPlanner.blockLayer(mc.player.getY()), mc.player.onGround());
      }
      int layer = PlacementPlanner.bridgeLayer(mc.player.getY(), this.lockedLayer, this.keepLayer());
      if (this.climbingTelly()) return layer;
      return this.isTellyMode() && this.tellyStep != null && !this.keepLayer()
         ? Math.min(layer, this.tellyStep.layer()) : layer;
   }

   private boolean climbingTelly() {
      return this.mode.getValue() == Mode.TELLY2
         || this.mode.getValue() == Mode.TELLY && !this.keepLayer() && mc.options.keyJump.isDown();
   }

   private boolean keepLayer() { return this.mode.getValue()==Mode.GODBRIDGE || this.mode.getValue() != Mode.TELLY2 && this.keepY.getValue(); }

   private SilentRotationMath.Rotation oneTickTravel(Input keys) {
      return TellyBridge.turn(this.sentYaw, this.sentPitch, TellyBridge.forwardYaw(keys, visualYaw),
         this.isTellyMode() ? 15 : visualPitch, mc.options.sensitivity().get());
   }

   private void prepareOneTick(Input keys) {
      Vec3 eyes = mc.player.getEyePosition(), motion = mc.player.getDeltaMovement();
      Vec3 nextStep = this.predictedPlacementStep();
      boolean takeoff = this.isTellyMode() && this.tellyStep != null && this.tellyStep.jump();
      boolean landing = this.isTellyMode() ? this.hasTellyForwardLanding()
         : PlacementPlanner.hasForwardLanding(mc.player.position(), motion, this.placementLayer(),
            mc.player.getAttributeValue(Attributes.GRAVITY), TellyBridge.forwardYaw(keys, visualYaw), TellyBridge.moving(keys), this.world());
      var liveWorld = this.world();
      boolean climb = this.climbingTelly();
      Vec3 aimFeet = climb ? mc.player.position().add(nextStep) : mc.player.position();
      var aimWorld = climb ? PlacementPlanner.withPlayerBox(liveWorld,mc.player.getBoundingBox().move(nextStep)) : liveWorld;
      int aimLayer = climb ? PlacementPlanner.blockLayer(aimFeet.y) : this.placementLayer();
      var candidates = PlacementPlanner.centerTargets(aimFeet,climb ? this.predictedFollowupStep(nextStep) : nextStep,
         aimLayer,!mc.player.onGround(),!this.keepLayer(),mc.player.getAttributeValue(Attributes.GRAVITY),aimWorld);
      if (climb) {
         var connectors = new java.util.LinkedHashSet<>(candidates);
         connectors.addAll(PlacementPlanner.centerTargets(mc.player.position(),nextStep,this.placementLayer(),
            !mc.player.onGround(),true,mc.player.getAttributeValue(Attributes.GRAVITY),aimWorld));
         candidates = new java.util.ArrayList<>(connectors);
         candidates.sort(java.util.Comparator.<BlockPos>comparingInt(BlockPos::getY).reversed());
      }
      var step = this.oneTick.update(candidates, eyes,
         eyes.add(nextStep), eyes.add(nextStep).add(this.predictedFollowupStep(nextStep)),
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.sentYaw, this.sentPitch,
         mc.options.sensitivity().get(), this.oneTickTravel(keys), !takeoff && (!this.isTellyMode() || !landing)
            && this.clickRate.readyWithin(System.nanoTime(),50_000_000L,this.cps.getValue(),this.randomCps.getValue(),this.minCps.getValue(),this.maxCps.getValue())
            && mc.player.tickCount + 1 - this.lastPlaceTick >= this.placeDelay.getValue(), liveWorld,
         this.isTellyMode()?TellyBridge.BACKWARD_OFFSET:180,aimWorld,!climb || ViaProtocol.usesSwordBlocking());
      this.pending = step.placement();
      this.oneTickAiming = step.aiming();
      this.oneTickReturnAfterAttempt = step.returnAfterAttempt();
      this.injectedRotation = this.rotationPrimed = isRotating = true;
      this.applyOneTickLook(step.look());
      this.placementStatus = step.aiming() ? "one-tick-aim" : "one-tick-travel";
   }

   private void applyOneTickLook(SilentRotationMath.Rotation look) {
      this.rotationYaw = look.yaw(); this.rotationPitch = look.pitch();
      this.headPose.aim(this.rotationYaw, this.rotationPitch, visualYaw, visualPitch);
      OyVey.rotationManager.setPlayerRotations(this.rotationYaw, this.rotationPitch);
      OyVey.rotationManager.setServerRotations(this.rotationYaw, this.rotationPitch);
   }

   private Vec3 predictedPlacementStep() {
      Input keys = this.heldTellyMovement();
      Vec3 direction = TellyBridge.moving(keys) ? Vec3.directionFromRotation(0, TellyBridge.forwardYaw(keys, visualYaw)) : Vec3.ZERO;
      double acceleration = .02;
      if (mc.player.onGround()) {
         float friction = mc.level.getBlockState(BlockPos.containing(mc.player.getX(), mc.player.getY()-.5, mc.player.getZ())).getBlock().getFriction();
         double walkSpeed = mc.player.getAttributeValue(Attributes.MOVEMENT_SPEED) / (mc.player.isSprinting() ? 1.3 : 1);
         acceleration = walkSpeed * .21600002 / (friction*friction*friction);
      }
      Vec3 requested = mc.player.getDeltaMovement().add(direction.scale(acceleration));
      boolean jump = mc.player.onGround() && (this.isTellyMode() ? this.tellyStep != null && this.tellyStep.jump() : mc.options.keyJump.isDown());
      if (jump) requested = requested.with(Direction.Axis.Y, mc.player.getAttributeValue(Attributes.JUMP_STRENGTH)+mc.player.getJumpBoostPower());
      var box = mc.player.getBoundingBox();
      return ScaffoldMovement.collisionStep(box, requested, mc.level.getBlockCollisions(mc.player, box.expandTowards(requested)));
   }

   private void keepClickedSupportPitch() {
      if (!ViaProtocol.usesSwordBlocking()) return;
      if (!this.placedThisTick || this.clickedPlacement == null
         || this.activeRotationMode != RotationMode.ONE_TICK && this.activeRotationMode != RotationMode.CENTER) return;
      if (PlacementPlanner.lookIntersectsClickedSupport(this.clickedPlacement, mc.player.getEyePosition(),
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.sentYaw, this.rotationPitch)) return;
      // Physics yaw is fixed here. Only pitch follows the actual post-movement
      // eye position, keeping the preceding placement's support in the ray.
      this.rotationPitch = PlacementPlanner.postPlacementPitch(this.clickedPlacement, mc.player.getEyePosition(),
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.sentYaw, this.sentPitch, mc.options.sensitivity().get());
      OyVey.rotationManager.setPlayerRotations(this.rotationYaw, this.rotationPitch);
      OyVey.rotationManager.setServerRotations(this.rotationYaw, this.rotationPitch);
      this.headPose.aim(this.rotationYaw, this.rotationPitch, visualYaw, visualPitch);
   }

   private Vec3 predictedFollowupStep(Vec3 firstStep) {
      Input keys=this.heldTellyMovement();
      Vec3 direction=TellyBridge.moving(keys)?Vec3.directionFromRotation(0,TellyBridge.forwardYaw(keys,visualYaw)):Vec3.ZERO;
      boolean jumping=mc.player.onGround() && (this.isTellyMode()?this.tellyStep!=null&&this.tellyStep.jump():mc.options.keyJump.isDown());
      boolean grounded=mc.player.onGround()&&!jumping;
      double drag=.91, acceleration=.026;
      if(grounded) {
         float friction=mc.level.getBlockState(BlockPos.containing(mc.player.getX(),mc.player.getY()-.5,mc.player.getZ())).getBlock().getFriction();
         drag*=friction;
         double walkSpeed=mc.player.getAttributeValue(Attributes.MOVEMENT_SPEED)/(mc.player.isSprinting()?1.3:1);
         acceleration=walkSpeed*(this.isTellyMode()||this.sprint.getValue()?1.3:1)*.21600002/(friction*friction*friction);
      }
      return new Vec3(firstStep.x*drag+direction.x*acceleration,
         grounded?0:(firstStep.y-mc.player.getAttributeValue(Attributes.GRAVITY))*.98,
         firstStep.z*drag+direction.z*acceleration);
   }

   private void returnOneTickAfterAttempt() {
      if (this.activeRotationMode != RotationMode.ONE_TICK || !this.injectedRotation || !this.oneTickReturnAfterAttempt) return;
      this.oneTickReturnAfterAttempt = this.oneTickAiming = false;
      this.pending = null;
      Input keys=this.heldTellyMovement();
      if (this.placedThisTick && !PlacementPlanner.hasForwardLanding(mc.player.position(),mc.player.getDeltaMovement(),
         this.placementLayer(),mc.player.getAttributeValue(Attributes.GRAVITY),TellyBridge.forwardYaw(keys,visualYaw),
         TellyBridge.moving(keys),this.world())) {
         // Each prepared look still lasts one native movement tick. A block
         // that does not yet cover the landing must prepare the next catch
         // now, instead of inserting an empty forward tick between blocks.
         this.prepareOneTick(keys);
         this.pending=null;
         if (this.oneTickAiming) { this.placementStatus="one-tick-continue-catch"; return; }
      }
      this.applyOneTickLook(this.oneTickTravel(this.heldTellyMovement()));
      this.placementStatus = this.placedThisTick ? "one-tick-placed" : "one-tick-released";
   }

   private Input heldTellyMovement() {
      return new Input(mc.options.keyUp.isDown(), mc.options.keyDown.isDown(),
         mc.options.keyLeft.isDown(), mc.options.keyRight.isDown(), false, false, false);
   }

   private boolean hasTellyForwardLanding() {
      Input keys = this.heldTellyMovement();
      return PlacementPlanner.hasForwardLanding(mc.player.position(), mc.player.getDeltaMovement(), this.placementLayer(),
         mc.player.getAttributeValue(Attributes.GRAVITY), TellyBridge.forwardYaw(keys, visualYaw), TellyBridge.moving(keys), this.world());
   }

   private void recoverTellyAfterPlacement() {
      if (this.activeRotationMode == RotationMode.ONE_TICK || !this.isTellyMode() || this.tellyStep == null || !this.placedThisTick || nullCheck()
         || mc.player.onGround() || !this.hasTellyForwardLanding()) return;
      int layer = PlacementPlanner.blockLayer(mc.player.getY());
      var recovered = this.mode.getValue() == Mode.TELLY2 ? this.telly2.recoverForward(layer)
         : this.telly.recoverForward(layer, this.climbingTelly());
      if (recovered == null) return;
      this.tellyStep = recovered;
      this.pending = null;
      this.clutchTarget = this.centerTarget = null;
      this.clutchRecovery = false;
      Input keys = this.heldTellyMovement();
      var look = TellyBridge.turn(this.sentYaw, this.sentPitch, TellyBridge.forwardYaw(keys, visualYaw), 15,
         mc.options.sensitivity().get());
      this.rotationYaw = look.yaw(); this.rotationPitch = look.pitch();
      this.rotationPrimed = true;
      this.placementStatus = "telly-recover_forward";
      // The click used the previously transmitted backward look. Turn forward
      // for this tick's native sprint/physics and its ordinary movement packet.
      this.headPose.aim(this.rotationYaw, this.rotationPitch, visualYaw, visualPitch);
      OyVey.rotationManager.setPlayerRotations(this.rotationYaw, this.rotationPitch);
      OyVey.rotationManager.setServerRotations(this.rotationYaw, this.rotationPitch);
   }

   private void advanceClutchAfterPlacement() {
      if (!this.clutchRecovery || !this.placedThisTick || nullCheck()) return;
      Vec3 eyes = mc.player.getEyePosition(), motion = mc.player.getDeltaMovement();
      var targets = PlacementPlanner.catchTargets(mc.player.position(), motion,
         Math.min(this.placementLayer(), PlacementPlanner.blockLayer(mc.player.getY())), true, true,
         mc.player.getAttributeValue(Attributes.GRAVITY));
      // The previous look already clicked. Use this tick's normal movement
      // packet to prepare the next support, rather than wasting a turn-only tick.
      var next = PlacementPlanner.clutchStep(targets, eyes, eyes.add(motion),
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.sentYaw, this.sentPitch,
         mc.options.sensitivity().get(), false, this.world(), null);
      if (next == null) return;
      this.clutchTarget = next.placement();
      this.rotationYaw = next.rotation().yaw(); this.rotationPitch = next.rotation().pitch();
      this.headPose.aim(this.rotationYaw, this.rotationPitch, visualYaw, visualPitch);
      OyVey.rotationManager.setPlayerRotations(this.rotationYaw, this.rotationPitch);
      OyVey.rotationManager.setServerRotations(this.rotationYaw, this.rotationPitch);
   }

   private void completeTellyCorner() {
      if (this.singlePlacementTick()) return;
      if (!this.placedThisTick || !this.cornerCatch() && this.activeRotationMode != RotationMode.ONE_TICK
         || (this.activeRotationMode != RotationMode.BACKWARDS && this.activeRotationMode != RotationMode.ONE_TICK && !this.clutchRecovery)
         || this.placeDelay.getValue() != 0) return;
      var targets = PlacementPlanner.centerTargets(mc.player.position(), mc.player.getDeltaMovement(), this.placementLayer(),
         !mc.player.onGround(), !this.keepLayer(), mc.player.getAttributeValue(Attributes.GRAVITY), this.world());
      if ((this.clutchRecovery || this.activeRotationMode == RotationMode.ONE_TICK) && this.clickedPlacement != null) {
         int catchLayer = this.clickedPlacement.target().getY();
         targets = targets.stream().filter(pos -> pos.getY() == catchLayer).toList();
      }
      // At a diagonal corner the first native prediction exposes the other
      // face needed for the landing. Reuse the exact transmitted look and
      // validate that new face; never add a look packet or an off-path block.
      this.pending = PlacementPlanner.findAlongLook(targets, mc.player.getEyePosition(),
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.sentYaw, this.sentPitch, this.world());
      this.placePending();
   }

   private void placePending() {
      if (this.pending == null || nullCheck() || mc.gameMode == null || mc.player.isUsingItem()) return;
      if (mc.player.tickCount == this.lastPlaceTick && this.singlePlacementTick()) { this.placementStatus = "placed-this-tick"; return; }
      if (mc.player.tickCount - this.lastPlaceTick < this.placeDelay.getValue()) { this.placementStatus = "delay"; return; }
      if (!this.validBlock(mc.player.getItemInHand(this.placeHand)) || !this.world().replaceable(this.pending.target())) return;
      BlockHitResult planned = this.pending.hit();
      if (!this.world().attachable(planned.getBlockPos(), planned.getDirection())) return;
      // Click from the position/look already known to the server, before this
      // tick's native physics and movement packet. The upcoming yaw must still
      // reach the support from this click position.
      if (!this.hasSentRotation || !PlacementPlanner.lookMatchesSupport(this.pending,
         mc.player.getEyePosition(), mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE),
         this.sentYaw, this.sentPitch, this.world())) { this.placementStatus = "look-not-sent"; return; }
      if (!PlacementPlanner.lookMatchesSupport(this.pending, mc.player.getEyePosition(),
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE),
         this.rotationYaw, this.rotationPitch, this.world())) { this.placementStatus = "turning"; return; }
      BlockHitResult hit = this.traceLook(this.sentYaw, this.sentPitch);
      if (!PlacementPlanner.matchesSupport(this.pending, hit)) { this.placementStatus = "look-overwritten"; return; }
      if (!this.clickRate.tryAcquire(System.nanoTime(),mc.player.tickCount,this.cps.getValue(),this.randomCps.getValue(),
         this.minCps.getValue(),this.maxCps.getValue())) { this.placementStatus = "cps-delay"; return; }
      // useItemOn performs native replaceability, collision, prediction and slot synchronization.
      this.placing = true;
      try {
         Vec3 eyes = mc.player.getEyePosition();
         var result = mc.gameMode.useItemOn(mc.player, this.placeHand, hit);
         this.placementStatus = "useItemOn=" + result;
         if (result.consumesAction()) {
            this.placedThisTick = true;
            this.clickedPlacement = this.pending;
            this.clickEyes = eyes;
            mc.player.swing(this.placeHand);
            this.lastPlaceTick = mc.player.tickCount;
            this.lastPlaced = this.pending.target();
            if (this.mode.getValue()==Mode.GODBRIDGE) this.godbridge.placed(this.pending.target());
         }
      } finally {
         this.placing = false;
      }
   }

   private boolean singlePlacementTick() {
      return this.mode.getValue() == Mode.GODBRIDGE
         || this.activeRotationMode == RotationMode.ONE_TICK && this.climbingTelly();
   }

   private void tracePlacement() {
      if (nullCheck() || !this.injectedRotation || mc.player.tickCount - this.lastTraceTick < 20) return;
      this.lastTraceTick = mc.player.tickCount;
      BlockHitResult ray = this.traceLook(this.rotationYaw, this.rotationPitch);
      OyVey.LOGGER.info("[ScaffoldPlacement] status={} feet={} eyeY={} look={},{} sent={},{} ray={} target={} hand={} shift={}",
         this.placementStatus, mc.player.position(), mc.player.getEyeY(), this.rotationYaw, this.rotationPitch,
         this.sentYaw, this.sentPitch, ray.getType() + ":" + ray.getBlockPos() + ":" + ray.getDirection() + ":" + ray.getLocation(),
         this.pending == null ? null : this.pending.target(), this.placeHand, mc.player.input.keyPresses.shift());
   }

   private BlockHitResult traceLook(float yaw, float pitch) {
      Vec3 eyes = mc.player.getEyePosition();
      double reach = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      return this.world().raycast(eyes, eyes.add(Vec3.directionFromRotation(pitch, yaw).scale(reach)));
   }

   @Subscribe(priority = -1000)
   private void onPacketSend(PacketEvent.Send event) {
      if (event.isCancelled()) return;
      if (event.getPacket() instanceof ServerboundMovePlayerPacket move) {
         // A position-only packet preserves the preceding look. Count it too:
         // enabling while already aimed must not wait for mouse movement.
         this.sentYaw = move.getYRot(this.sentYaw);
         this.sentPitch = move.getXRot(this.sentPitch);
         this.hasSentRotation = true;
      } else if (this.placing && this.pending != null
         && event.getPacket() instanceof ServerboundUseItemOnPacket use
         && use.getHand() == this.placeHand && PlacementPlanner.matchesSupport(this.pending, use.getHitResult())) {
         this.confirmations.predict(this.pending.target(), use.getSequence());
      }
   }

   private void syncWorld() {
      if (this.trackedLevel != mc.level) {
         this.trackedLevel = mc.level;
         this.clickRate.reset();
         this.confirmations.clear();
         this.godbridge.reset(); this.godbridgeJump=false;
         this.oneTick.reset();
         this.headPose.clear();
         this.bridgeRotation.reset();
         this.telly.reset(); this.telly2.reset();
         this.tellyStep = null;
         this.rotationPrimed = false;
         this.centerTarget = null;
         this.startSlot = -1;
         this.resumeAfterCorrectionTick = 0;
         this.hasSentRotation = false;
         this.captureLastSentRotation();
      }
   }

   private void captureLastSentRotation() {
      // The camera may be hundreds of degrees away numerically after another
      // silent-rotation module. Start from vanilla's actual transmitted baseline.
      LocalPlayerRotationAccessor player = mc.player == null ? null : (LocalPlayerRotationAccessor)mc.player;
      this.sentYaw = player == null ? 0.0F : player.oyvey$getLastSentYaw();
      this.sentPitch = player == null ? 0.0F : player.oyvey$getLastSentPitch();
   }

   public void onPredictionAck(int sequence) {
      if (this.trackedLevel == mc.level) {
         boolean rejected = false;
         for (BlockPos pos : this.confirmations.acknowledge(sequence)) {
            if (mc.level != null && mc.level.getBlockState(pos).canBeReplaced()) {
               rejected = true;
               OyVey.LOGGER.info("[ScaffoldPrediction] rejected={} sequence={}", pos, sequence);
            }
         }
         if (mc.level != null && this.lastPlaced != null && !this.confirmations.isPending(this.lastPlaced)
            && mc.level.getBlockState(this.lastPlaced).isAir()) {
            this.lastPlaced = null;
         }
         if (rejected) this.onServerCorrection();
      }
   }

   public void onServerBlockUpdate(BlockPos pos, BlockState state) {
      if (mc.level != this.trackedLevel || !this.confirmations.isPending(pos)) return;
      if (!state.isAir() && state.getFluidState().isEmpty() && state.isCollisionShapeFullBlock(mc.level, pos)) {
         this.confirmations.confirmBlock(pos);
      }
      // A negative update stays pending until vanilla reconciles its prediction.
      // It must never authorize a dependent placement or an automatic jump.
   }

   public void onServerCorrection() {
      if (!this.isEnabled() || mc.player == null) return;
      this.clickRate.reset();
      this.godbridge.reset(); this.godbridgeJump=false;
      this.oneTick.reset();
      this.restoreRotation();
      this.headPose.clear();
      this.pending = null;
      this.centerTarget = null;
      this.telly.reset(); this.telly2.reset();
      this.tellyStep = null;
      this.bridgeRotation.reset();
      this.rotationPrimed = false;
      this.hasSentRotation = false;
      this.captureLastSentRotation();
      this.resumeAfterCorrectionTick = mc.player.tickCount + 3;
   }

   private PlacementPlanner.World world() {
      return new PlacementPlanner.World() {
         public boolean replaceable(BlockPos pos) {
            return this.replaceableAt(pos,mc.player.getBoundingBox());
         }
         public boolean replaceableAt(BlockPos pos, AABB playerBox) {
            return mc.level.isInWorldBounds(pos) && mc.level.getWorldBorder().isWithinBounds(pos)
               && mc.level.getBlockState(pos).canBeReplaced()
               && !playerBox.intersects(new AABB(pos));
         }
         public boolean attachable(BlockPos support, Direction face) {
            var state = mc.level.getBlockState(support);
            // Native placements update the local collision world immediately.
            // Ordered useItemOn calls can extend that block before its ack;
            // waiting here throttled bridging to one block per round trip.
            return !state.isAir() && state.getFluidState().isEmpty()
               && !state.canBeReplaced() && state.isFaceSturdy(mc.level, support, face)
               && state.getMenuProvider(mc.level, support) == null;
         }
         public BlockHitResult raycast(Vec3 from, Vec3 to) {
            return mc.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
         }
         public boolean supportsLanding(BlockPos pos) {
            var state = mc.level.getBlockState(pos);
            return !state.isAir() && state.getFluidState().isEmpty()
               && state.isCollisionShapeFullBlock(mc.level, pos);
         }
      };
   }

   private boolean selectBlocks() {
      this.placeHand = InteractionHand.MAIN_HAND;
      if (this.validBlock(mc.player.getMainHandItem())) return true;
      if (this.validBlock(mc.player.getOffhandItem())) {
         this.placeHand = InteractionHand.OFF_HAND;
         return true;
      }
      if (!this.autoSwitch.getValue()) return false;
      int best = -1, count = 0;
      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = mc.player.getInventory().getItem(slot);
         if (this.validBlock(stack) && stack.getCount() > count) {
            best = slot;
            count = stack.getCount();
         }
      }
      if (best < 0) return false;
      mc.player.getInventory().setSelectedSlot(best);
      mc.gameMode.ensureHasSentCarriedItem();
      this.ownsSlot = true;
      return true;
   }

   private boolean validBlock(ItemStack stack) {
      if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
      var block = item.getBlock();
      return !(block instanceof FallingBlock) && !(block instanceof EntityBlock)
         && block.defaultBlockState().isCollisionShapeFullBlock(mc.level, mc.player.blockPosition());
   }

   private void restoreRotation() {
      if (this.injectedRotation) {
         if (mc.player != null) {
            float yaw = this.hasSentRotation ? this.sentYaw + Mth.wrapDegrees(visualYaw - this.sentYaw) : visualYaw;
            OyVey.rotationManager.setPlayerRotations(yaw, visualPitch);
         }
         OyVey.rotationManager.clearServerRotations();
      }
      this.injectedRotation = false;
      isRotating = false;
   }

   public boolean isSprintSuppressed() {
      return this.ownsRotation() && (!this.wantsSprint() || !this.sprintAllowed);
   }

   private boolean wantsSprint() {
      if (this.mode.getValue()==Mode.GODBRIDGE) return false;
      return this.isTellyMode()
         ? this.tellyStep != null && (this.activeRotationMode == RotationMode.ONE_TICK ? !this.oneTickAiming : this.tellyStep.lookForward()) : this.sprint.getValue();
   }

   private boolean allowsSprint(Input keys) {
      return this.wantsSprint() && !nullCheck() && mc.screen == null
         && ScaffoldMovement.forwardSprint(keys) && !mc.player.isUsingItem()
         && !mc.player.hasEffect(MobEffects.BLINDNESS)
         && (mc.player.getFoodData().getFoodLevel() > 6 || mc.player.getAbilities().mayfly)
         && (!mc.player.horizontalCollision || mc.player.minorHorizontalCollision)
         && !mc.player.isFallFlying() && !mc.player.isPassenger()
         && !mc.player.isInWater() && !mc.player.isInLava() && !mc.player.isSpectator();
   }

   public void enforceSprintInput(ClientInput input) {
      if (!this.ownsRotation() || mc.player == null) return;
      // Run again after other input modules: an Omni preset or jump module must
      // not re-enable sprint after correction selected sideways/backward keys.
      Input keys = input.keyPresses;
      this.sprintAllowed = this.allowsSprint(keys);
      boolean controlsJump = this.isTellyMode() && this.tellyStep != null
         && this.ownsRotation() && mc.screen == null;
      // Telly2 rises automatically; Space selects upward building in Telly.
      // Native jump presses use the forward takeoff look, then release in air.
      boolean jump = controlsJump ? this.tellyStep.jump() && mc.player.onGround()
         : keys.jump() || this.mode.getValue()==Mode.GODBRIDGE && this.godbridgeJump;
      // Reassert the module's request after other modules process the keyboard;
      // no physical sprint binding is required for an eligible forward phase.
      input.keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(),
         jump, keys.shift(), this.sprintAllowed);
      if (!this.sprintAllowed) mc.player.setSprinting(false);
   }

   public boolean ownsRotation() {
      return this.isEnabled() && this.injectedRotation;
   }

   public boolean hasSilentHeadPose() {
      return this.isEnabled() && this.trackedLevel == mc.level && !nullCheck() && this.headPose.isActive();
   }

   public SilentHeadPose getSilentHeadPose() {
      return this.headPose;
   }

   public void applyInput(ClientInput input) {
      if (!this.isEnabled() || mc.player == null) return;
      Input keys = input.keyPresses;
      if (this.ownsRotation()) {
         // Both modes preserve held movement. Rotation correction only remaps
         // WASD; it never releases or reverses keys to wait for support.
         SilentRotationMath.Movement movement = SilentRotationMath.correctMovement(keys, visualYaw, this.rotationYaw);
         // Correct the held input from the first turn. Clearing WASD while the
         // look aligns caused the visible pause immediately after enabling.
         keys = new Input(movement.forward() > 0, movement.forward() < 0,
            movement.sideways() > 0, movement.sideways() < 0,
            keys.jump(), keys.shift(), keys.sprint());
         ((ClientInputAccessor)input).oyvey$setMoveVector(me.alpha432.oyvey.util.KeyboardMovement.vector(movement));
      }
      input.keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(),
         keys.jump(), keys.shift(), this.ownsRotation() ? this.wantsSprint() : keys.sprint());
      this.enforceSprintInput(input);
   }

   @Override
   public String getDisplayInfo() {
      if (this.mode.getValue()==Mode.GODBRIDGE) return "Godbridge";
      return this.isTellyMode() ? this.mode.getValue() == Mode.TELLY2 ? "Telly2" : "Telly" : null;
   }

   private boolean isTellyMode() { return this.mode.getValue() == Mode.TELLY || this.mode.getValue() == Mode.TELLY2; }

   private boolean cornerCatch() {
      // Recovery can be driven by momentum/knockback without held diagonal
      // keys. Let its native ray complete a connector in every travel direction.
      return this.mode.getValue()==Mode.GODBRIDGE || this.clutchRecovery
         || this.diagonalMovement && (this.climbingTelly() || this.activeRotationMode == RotationMode.ONE_TICK);
   }

   public enum Mode { NORMAL, TELLY, TELLY2, GODBRIDGE }
   public enum RotationMode { BACKWARDS, CENTER, ONE_TICK }

   @Override
   public void onRender2D(Render2DEvent event) {
      if (nullCheck() || !this.blockCounter.getValue()) return;
      int count = 0;
      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = mc.player.getInventory().getItem(slot);
         if (this.validBlock(stack)) count += stack.getCount();
      }
      if (this.validBlock(mc.player.getOffhandItem())) count += mc.player.getOffhandItem().getCount();
      String text = count + " blocks";
      int x = (mc.getWindow().getGuiScaledWidth() - mc.font.width(text)) / 2;
      event.getContext().text(mc.font, text, x, mc.getWindow().getGuiScaledHeight() / 2 + 18,
         count > 0 ? 0xFFFFFFFF : 0xFFFF5555, true);
   }

   @Override
   public void onRender3D(Render3DEvent event) {
      if (this.blockESP.getValue() && this.lastPlaced != null) {
         RenderUtil.drawBox(event.getMatrix(), new AABB(this.lastPlaced), new Color(90, 190, 255, 180), 1);
      }
   }
}
