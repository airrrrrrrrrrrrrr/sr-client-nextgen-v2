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
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.mixin.render.accessor.ClientInputAccessor;
import me.alpha432.oyvey.mixin.render.accessor.LocalPlayerRotationAccessor;
import me.alpha432.oyvey.util.SilentRotationMath;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.player.ClientInput;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends Module {
   public static volatile boolean isRotating;
   public static volatile float visualYaw, visualPitch;

   public final Setting<Boolean> autoSwitch = this.bool("AutoSwitch", true);
   public final Setting<Boolean> restoreSlot = this.bool("RestoreSlot", true);
   public final Setting<Boolean> keepY = this.bool("KeepY", false);
   public final Setting<Boolean> sprint = this.bool("Sprint", false);
   public final Setting<Integer> placeDelay = this.num("PlaceDelay", 0, 0, 10);
   public final Setting<Boolean> blockCounter = this.bool("BlockCounter", true);
   public final Setting<Boolean> blockESP = this.bool("BlockESP", true);

   private PlacementPlanner.Placement pending;
   private BlockPos lastPlaced;
   private int startSlot = -1, lockedLayer, lastPlaceTick = -100;
   private boolean ownsSlot, injectedRotation;
   private InteractionHand placeHand;
   private final PlacementConfirmations confirmations = new PlacementConfirmations();
   private final SilentHeadPose headPose = new SilentHeadPose();
   private final BackwardBridgeRotation bridgeRotation = new BackwardBridgeRotation();
   private ClientLevel trackedLevel;
   private volatile float sentYaw, sentPitch;
   private volatile boolean hasSentRotation;
   private float rotationYaw, rotationPitch;
   private boolean placing, sprintAllowed;
   private boolean rotationPrimed;
   private boolean placedThisTick;
   private PlacementPlanner.Placement clickedPlacement;
   private Vec3 clickEyes;
   private int lastTraceTick;
   private String placementStatus = "idle";

   public Scaffold() {
      super("Scaffold", "Places hotbar blocks beneath your feet using visible support faces.", Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      this.pending = null;
      this.lastPlaced = null;
      this.lastPlaceTick = -100;
      this.ownsSlot = false;
      this.injectedRotation = false;
      this.headPose.clear();
      this.bridgeRotation.reset();
      this.hasSentRotation = false;
      this.sprintAllowed = false;
      this.rotationPrimed = false;
      this.lastTraceTick = -100;
      this.syncWorld();
      isRotating = false;
      this.captureLastSentRotation();
      this.startSlot = mc.player == null ? -1 : mc.player.getInventory().getSelectedSlot();
      this.lockedLayer = mc.player == null ? 0 : PlacementPlanner.blockLayer(mc.player.getY());
   }

   @Override
   public void onDisable() {
      this.restoreRotation();
      this.headPose.clear();
      this.pending = null;
      if (this.restoreSlot.getValue() && this.ownsSlot && this.startSlot >= 0 && mc.player != null && mc.gameMode != null) {
         mc.player.getInventory().setSelectedSlot(this.startSlot);
         mc.gameMode.ensureHasSentCarriedItem();
      }
      this.ownsSlot = false;
      this.lastPlaced = null;
   }

   @Subscribe(priority = 100)
   public void onWalking(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == Stage.MOTION_PRE) {
         this.preparePlacement();
         this.placePending();
         this.tracePlacement();
      } else if (event.getStage() == Stage.PRE && this.injectedRotation) {
         // Physics has finished. Only pitch changes here; the backward yaw that
         // drove movement stays the same for the outgoing movement packet.
         this.refreshPlacement();
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
      this.clickedPlacement = null;
      this.placementStatus = "inactive";
      this.sprintAllowed = false;
      this.syncWorld();
      if (nullCheck() || mc.gameMode == null || mc.screen != null || mc.player.isSpectator()
         || mc.player.isPassenger() || mc.player.isUsingItem()) return;
      if (this.startSlot < 0) {
         this.startSlot = mc.player.getInventory().getSelectedSlot();
         this.lockedLayer = PlacementPlanner.blockLayer(mc.player.getY());
      }
      boolean airborne = !mc.player.onGround();
      int layer = PlacementPlanner.bridgeLayer(mc.player.getY(), this.lockedLayer, this.keepY.getValue());
      // A locked bridge layer must never intersect the player after dropping below it.
      if (layer + 1 > mc.player.getY() + 0.001) return;
      Vec3 motion = mc.player.getDeltaMovement();
      if (!this.selectBlocks()) return;
      visualYaw = mc.player.getYRot();
      visualPitch = mc.player.getXRot();
      // Read camera-space keys, not the previous tick's corrected WASD state.
      Input keys = new Input(mc.options.keyUp.isDown(), mc.options.keyDown.isDown(),
         mc.options.keyLeft.isDown(), mc.options.keyRight.isDown(), false, false, false);
      var candidates = PlacementPlanner.catchTargets(mc.player.position(), motion, layer, airborne, !this.keepY.getValue(),
         mc.player.getAttributeValue(Attributes.GRAVITY));
      var transmittedPlacement = PlacementPlanner.findAlongLook(candidates, mc.player.getEyePosition(),
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.sentYaw, this.sentPitch, this.world());
      var backwardLook = this.bridgeRotation.next(keys, visualYaw, this.sentYaw, this.sentPitch, 82, mc.options.sensitivity().get());
      float desiredPitch = PlacementPlanner.backwardPitch(candidates, mc.player.getEyePosition(),
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), backwardLook.yaw(), this.sentPitch,
         mc.options.sensitivity().get(), this.world());
      SilentRotationMath.Rotation rotation = this.bridgeRotation.next(keys, visualYaw,
         this.sentYaw, this.sentPitch, desiredPitch, mc.options.sensitivity().get());
      if (transmittedPlacement != null && this.hasSentRotation && this.rotationPrimed) {
         // Complete a click with its already transmitted look. Turning toward
         // the next heading resumes when this support has been used.
         rotation = new SilentRotationMath.Rotation(this.sentYaw, this.sentPitch);
      }
      this.rotationYaw = rotation.yaw();
      this.rotationPitch = rotation.pitch();
      this.rotationPrimed |= Math.abs(Mth.wrapDegrees(this.bridgeRotation.heading(keys, visualYaw) - this.rotationYaw)) < 1
         && this.rotationPitch >= 70;
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

   private void refreshPlacement() {
      if (nullCheck()) return;
      int layer = PlacementPlanner.bridgeLayer(mc.player.getY(), this.lockedLayer, this.keepY.getValue());
      var candidates = PlacementPlanner.catchTargets(mc.player.position(), mc.player.getDeltaMovement(), layer,
         !mc.player.onGround(), !this.keepY.getValue(), mc.player.getAttributeValue(Attributes.GRAVITY));
      float pitch = PlacementPlanner.backwardPitch(candidates, mc.player.getEyePosition(),
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.rotationYaw,
         this.sentPitch, mc.options.sensitivity().get(), this.world());
      var rotation = SilentRotationMath.quantizeRotation(this.sentYaw, this.sentPitch, this.rotationYaw,
         this.rotationPitch + Mth.clamp(pitch - this.rotationPitch, -10, 10), mc.options.sensitivity().get());
      // Quantize pitch alone: never change the yaw used for this tick's physics.
      // The upcoming look must still reach the clicked support from the click
      // position, while preparing the next edge from the new player position.
      if (!this.placedThisTick || PlacementPlanner.lookMatchesSupport(this.clickedPlacement, this.clickEyes,
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.rotationYaw, rotation.pitch(), this.world())) {
         this.rotationPitch = rotation.pitch();
      }
      OyVey.rotationManager.setPlayerRotations(this.rotationYaw, this.rotationPitch);
      OyVey.rotationManager.setServerRotations(this.rotationYaw, this.rotationPitch);
      this.headPose.aim(this.rotationYaw, this.rotationPitch, visualYaw, visualPitch);
      this.pending = PlacementPlanner.findAlongLook(candidates, mc.player.getEyePosition(),
         mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.rotationYaw, this.rotationPitch, this.world());
      this.placementStatus = this.pending == null ? "no-target-after-movement" : "ready";
   }

   private void placePending() {
      if (this.pending == null || nullCheck() || mc.gameMode == null || mc.player.isUsingItem()) return;
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
         }
      } finally {
         this.placing = false;
      }
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
         this.confirmations.clear();
         this.headPose.clear();
         this.bridgeRotation.reset();
         this.rotationPrimed = false;
         this.startSlot = -1;
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
         this.confirmations.acknowledge(sequence);
         if (mc.level != null && this.lastPlaced != null && !this.confirmations.isPending(this.lastPlaced)
            && mc.level.getBlockState(this.lastPlaced).isAir()) {
            this.lastPlaced = null;
         }
      }
   }

   private PlacementPlanner.World world() {
      return new PlacementPlanner.World() {
         public boolean replaceable(BlockPos pos) {
            return mc.level.isInWorldBounds(pos) && mc.level.getWorldBorder().isWithinBounds(pos)
               && mc.level.getBlockState(pos).canBeReplaced()
               && !mc.player.getBoundingBox().intersects(new AABB(pos));
         }
         public boolean attachable(BlockPos support, Direction face) {
            var state = mc.level.getBlockState(support);
            // Use vanilla's predicted world just as a normal right click does.
            // Waiting for every ack stalls bridging on high-latency servers.
            return !state.isAir() && state.getFluidState().isEmpty()
               && !state.canBeReplaced() && state.isFaceSturdy(mc.level, support, face)
               && state.getMenuProvider(mc.level, support) == null;
         }
         public BlockHitResult raycast(Vec3 from, Vec3 to) {
            return mc.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
         }
         public boolean supportsLanding(BlockPos pos) {
            // A predicted block already participates in local collision. Await
            // its reconciliation instead of surrounding it with more blocks.
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
      return this.isEnabled() && (!this.sprint.getValue() || !this.sprintAllowed);
   }

   private boolean allowsSprint(Input keys) {
      return this.sprint.getValue() && !nullCheck() && mc.screen == null
         && ScaffoldMovement.forwardSprint(keys) && !mc.player.isUsingItem()
         && !mc.player.hasEffect(MobEffects.BLINDNESS)
         && (mc.player.getFoodData().getFoodLevel() > 6 || mc.player.getAbilities().mayfly)
         && !mc.player.horizontalCollision && !mc.player.isFallFlying() && !mc.player.isPassenger()
         && !mc.player.isInWater() && !mc.player.isInLava() && !mc.player.isSpectator();
   }

   public void enforceSprintInput(ClientInput input) {
      if (!this.isEnabled() || mc.player == null) return;
      // Run again after other input modules: an Omni preset or jump module must
      // not re-enable sprint after correction selected sideways/backward keys.
      Input keys = input.keyPresses;
      this.sprintAllowed = this.allowsSprint(keys);
      input.keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(),
         keys.jump(), keys.shift(), keys.sprint() && this.sprintAllowed);
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
         SilentRotationMath.Movement movement = SilentRotationMath.correctMovement(keys, visualYaw, this.rotationYaw);
         if (!this.rotationPrimed) {
            // Let native friction settle while the initial backward look aligns.
            // This changes actual keyboard input, never velocity or collision.
            movement = SilentRotationMath.correctMovement(new Input(false, false, false, false,
               keys.jump(), keys.shift(), false), visualYaw, this.rotationYaw);
         }
         keys = new Input(movement.forward() > 0, movement.forward() < 0,
            movement.sideways() > 0, movement.sideways() < 0,
            keys.jump(), keys.shift(), keys.sprint());
         ((ClientInputAccessor)input).oyvey$setMoveVector(me.alpha432.oyvey.util.KeyboardMovement.vector(movement));
      }
      input.keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(),
         keys.jump(), keys.shift(), this.sprint.getValue());
      this.enforceSprintInput(input);
   }

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
