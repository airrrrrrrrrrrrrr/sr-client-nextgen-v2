package me.alpha432.oyvey.features.modules.movement;

import java.awt.Color;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.movement.scaffold.PlacementPlanner;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.player.ClientInput;
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

public class Scaffold extends Module {
   public static volatile boolean isRotating;
   public static volatile float visualYaw, visualPitch;

   public final Setting<Boolean> autoSwitch = this.bool("AutoSwitch", true);
   public final Setting<Boolean> restoreSlot = this.bool("RestoreSlot", true);
   public final Setting<Boolean> rotate = this.bool("Rotate", true);
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
      isRotating = false;
      this.startSlot = mc.player == null ? -1 : mc.player.getInventory().getSelectedSlot();
      this.lockedLayer = mc.player == null ? 0 : PlacementPlanner.blockLayer(mc.player.getY());
   }

   @Override
   public void onDisable() {
      this.restoreRotation();
      this.pending = null;
      if (this.restoreSlot.getValue() && this.ownsSlot && this.startSlot >= 0 && mc.player != null && mc.gameMode != null) {
         mc.player.getInventory().setSelectedSlot(this.startSlot);
         mc.gameMode.ensureHasSentCarriedItem();
      }
      this.ownsSlot = false;
      this.lastPlaced = null;
   }

   @Subscribe
   public void onWalking(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == Stage.PRE) {
         this.preparePlacement();
      } else if (event.getStage() == Stage.POST) {
         // Rotation has now been sent by vanilla; place once and restore the camera.
         try {
            this.placePending();
         } finally {
            this.restoreRotation();
            this.pending = null;
         }
      }
   }

   private void preparePlacement() {
      this.restoreRotation();
      this.pending = null;
      if (nullCheck() || mc.gameMode == null || mc.screen != null || mc.player.isSpectator()
         || mc.player.isPassenger() || mc.player.isUsingItem()) return;
      if (this.startSlot < 0) {
         this.startSlot = mc.player.getInventory().getSelectedSlot();
         this.lockedLayer = PlacementPlanner.blockLayer(mc.player.getY());
      }
      if (mc.player.tickCount - this.lastPlaceTick < this.placeDelay.getValue()) return;
      int layer = this.keepY.getValue() ? this.lockedLayer : PlacementPlanner.blockLayer(mc.player.getY());
      // A locked bridge layer must never intersect the player after dropping below it.
      if (layer + 1 > mc.player.getY() + 0.001) return;
      Vec3 motion = mc.player.getDeltaMovement();
      this.pending = PlacementPlanner.find(PlacementPlanner.targets(mc.player.getX(), mc.player.getZ(), layer, motion.x, motion.z),
         mc.player.getEyePosition(), mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), this.world());
      if (this.pending == null || !this.selectBlocks()) {
         this.pending = null;
         return;
      }
      if (this.rotate.getValue()) {
         Vec3 delta = this.pending.hit().getLocation().subtract(mc.player.getEyePosition());
         float yaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
         float pitch = (float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
         visualYaw = mc.player.getYRot();
         visualPitch = mc.player.getXRot();
         float continuousYaw = visualYaw + Mth.wrapDegrees(yaw - visualYaw);
         this.injectedRotation = true;
         isRotating = true;
         OyVey.rotationManager.setPlayerRotations(continuousYaw, pitch);
         OyVey.rotationManager.setServerRotations(continuousYaw, pitch);
      }
   }

   private void placePending() {
      if (this.pending == null || nullCheck() || mc.gameMode == null || mc.player.isUsingItem()) return;
      if (!this.validBlock(mc.player.getItemInHand(this.placeHand)) || !this.world().replaceable(this.pending.target())) return;
      // useItemOn performs native replaceability, collision, prediction and slot synchronization.
      var result = mc.gameMode.useItemOn(mc.player, this.placeHand, this.pending.hit());
      if (result.consumesAction()) {
         mc.player.swing(this.placeHand);
         this.lastPlaceTick = mc.player.tickCount;
         this.lastPlaced = this.pending.target();
      }
   }

   private PlacementPlanner.World world() {
      return new PlacementPlanner.World() {
         public boolean replaceable(BlockPos pos) {
            return mc.level.isInWorldBounds(pos) && mc.level.getWorldBorder().isWithinBounds(pos)
               && mc.level.getBlockState(pos).canBeReplaced();
         }
         public boolean attachable(BlockPos support, Direction face) {
            var state = mc.level.getBlockState(support);
            return !state.canBeReplaced() && state.isFaceSturdy(mc.level, support, face)
               && state.getMenuProvider(mc.level, support) == null;
         }
         public BlockHitResult raycast(Vec3 from, Vec3 to) {
            return mc.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
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
         if (mc.player != null) OyVey.rotationManager.setPlayerRotations(visualYaw, visualPitch);
         OyVey.rotationManager.clearServerRotations();
      }
      this.injectedRotation = false;
      isRotating = false;
   }

   public boolean isSprintSuppressed() {
      return this.isEnabled() && !this.sprint.getValue();
   }

   public void applyInput(ClientInput input) {
      if (!this.isSprintSuppressed() || mc.player == null) return;
      Input keys = input.keyPresses;
      input.keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(), keys.jump(), keys.shift(), false);
      mc.player.setSprinting(false);
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
