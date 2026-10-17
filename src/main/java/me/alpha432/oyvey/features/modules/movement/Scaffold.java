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
import net.minecraft.world.phys.Vec2;

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
   private int startSlot = -1, lockedLayer, groundedLayer, lastPlaceTick = -100;
   private boolean ownsSlot, injectedRotation;
   private InteractionHand placeHand;
   private final PlacementConfirmations confirmations = new PlacementConfirmations();
   private final SilentHeadPose headPose = new SilentHeadPose();
   private boolean stepUpAttemptedThisJump;
   private ClientLevel trackedLevel;
   private volatile float sentYaw, sentPitch;
   private volatile boolean hasSentRotation;
   private float rotationYaw, rotationPitch;
   private boolean placing, sprintAllowed = true;

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
      this.stepUpAttemptedThisJump = false;
      this.hasSentRotation = false;
      this.sprintAllowed = true;
      this.syncWorld();
      isRotating = false;
      this.captureLastSentRotation();
      this.startSlot = mc.player == null ? -1 : mc.player.getInventory().getSelectedSlot();
      this.lockedLayer = mc.player == null ? 0 : PlacementPlanner.blockLayer(mc.player.getY());
      this.groundedLayer = this.lockedLayer;
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
         // Native prediction and block collision must exist before movement.
         // Placing in POST sends an interaction after the movement tick.
         this.placePending();
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
      this.sprintAllowed = true;
      this.syncWorld();
      if (nullCheck() || mc.gameMode == null || mc.screen != null || mc.player.isSpectator()
         || mc.player.isPassenger() || mc.player.isUsingItem()) return;
      if (this.startSlot < 0) {
         this.startSlot = mc.player.getInventory().getSelectedSlot();
         this.lockedLayer = PlacementPlanner.blockLayer(mc.player.getY());
         this.groundedLayer = this.lockedLayer;
      }
      if (mc.player.onGround()) {
         this.groundedLayer = PlacementPlanner.blockLayer(mc.player.getY());
         this.stepUpAttemptedThisJump = false;
      }
      if (mc.player.tickCount - this.lastPlaceTick < this.placeDelay.getValue()) return;
      boolean airborne = !mc.player.onGround();
      int layer = this.keepY.getValue() ? this.lockedLayer
         : Math.min(this.groundedLayer, PlacementPlanner.blockLayer(mc.player.getY()));
      // A locked bridge layer must never intersect the player after dropping below it.
      if (layer + 1 > mc.player.getY() + 0.001) return;
      Vec3 motion = mc.player.getDeltaMovement();
      this.pending = PlacementPlanner.findStepOrCatch(mc.player.position(), motion, layer,
         airborne, this.keepY.getValue(), mc.player.getAttributeValue(Attributes.GRAVITY),
         mc.player.getEyePosition(), mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.world());
      if (this.pending == null || !this.selectBlocks()) {
         this.pending = null;
         return;
      }
      visualYaw = mc.player.getYRot();
      visualPitch = mc.player.getXRot();
      SilentRotationMath.Rotation rotation = this.placementRotation(this.pending);
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
   }

   private void placePending() {
      if (this.pending == null || nullCheck() || mc.gameMode == null || mc.player.isUsingItem()) return;
      if (!this.validBlock(mc.player.getItemInHand(this.placeHand)) || !this.world().replaceable(this.pending.target())) return;
      BlockHitResult planned = this.pending.hit();
      if (!this.world().attachable(planned.getBlockPos(), planned.getDirection())) return;
      // Like vanilla use-item input, placement precedes this tick's look/move
      // packet. Grim's post-flying placement pass validates that upcoming look
      // against the saved pre-movement position. Requiring last tick's look here
      // would miss fast-moving bridge edges. Always require a real support ray.
      BlockHitResult hit = this.traceLook(mc.player.getYRot(), mc.player.getXRot());
      if (!PlacementPlanner.matchesSupport(this.pending, hit)) return;
      // useItemOn performs native replaceability, collision, prediction and slot synchronization.
      this.placing = true;
      try {
         boolean stepUp = this.pending.target().getY() > this.groundedLayer;
         // Commit at most one raised placement per jump. If it is rejected or
         // the trajectory changes, keep the remaining ticks for flat catching.
         if (stepUp) this.stepUpAttemptedThisJump = true;
         var result = mc.gameMode.useItemOn(mc.player, this.placeHand, hit);
         if (result.consumesAction()) {
            mc.player.swing(this.placeHand);
            this.lastPlaceTick = mc.player.tickCount;
            this.lastPlaced = this.pending.target();
         }
      } finally {
         this.placing = false;
      }
   }

   private BlockHitResult traceLook(float yaw, float pitch) {
      Vec3 eyes = mc.player.getEyePosition();
      double reach = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      return this.world().raycast(eyes, eyes.add(Vec3.directionFromRotation(pitch, yaw).scale(reach)));
   }

   private SilentRotationMath.Rotation placementRotation(PlacementPlanner.Placement placement) {
      Vec3 delta = placement.hit().getLocation().subtract(mc.player.getEyePosition());
      float yaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
      float pitch = (float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
      return SilentRotationMath.quantizeRotation(this.sentYaw, this.sentPitch,
         yaw, pitch, mc.options.sensitivity().get());
   }

   @Subscribe(priority = -1000)
   private void onPacketSend(PacketEvent.Send event) {
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
         this.stepUpAttemptedThisJump = false;
         this.groundedLayer = mc.player == null ? 0 : PlacementPlanner.blockLayer(mc.player.getY());
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
            return !confirmations.isPending(support) && !state.isAir() && state.getFluidState().isEmpty()
               && !state.canBeReplaced() && state.isFaceSturdy(mc.level, support, face)
               && state.getMenuProvider(mc.level, support) == null;
         }
         public BlockHitResult raycast(Vec3 from, Vec3 to) {
            return mc.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
         }
         public boolean rotationValid(PlacementPlanner.Placement placement) {
            if (stepUpAttemptedThisJump && placement.target().getY() > groundedLayer) return false;
            SilentRotationMath.Rotation rotation = placementRotation(placement);
            return PlacementPlanner.matchesSupport(placement, traceLook(rotation.yaw(), rotation.pitch()));
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
      return this.isEnabled() && (!this.sprint.getValue() || this.ownsRotation() && !this.sprintAllowed);
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
         this.sprintAllowed = movement.forward() > 0;
         keys = new Input(movement.forward() > 0, movement.forward() < 0,
            movement.sideways() > 0, movement.sideways() < 0,
            keys.jump(), keys.shift(), keys.sprint());
         ((ClientInputAccessor)input).oyvey$setMoveVector(new Vec2(movement.normalizedSideways(), movement.normalizedForward()));
      }
      boolean allowSprint = !this.isSprintSuppressed();
      input.keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(), keys.jump(), keys.shift(), keys.sprint() && allowSprint);
      if (!allowSprint) mc.player.setSprinting(false);
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
