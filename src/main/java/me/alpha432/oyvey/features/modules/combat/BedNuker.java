package me.alpha432.oyvey.features.modules.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.movement.Scaffold;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.mixin.render.accessor.ClientInputAccessor;
import me.alpha432.oyvey.util.MathUtil;
import me.alpha432.oyvey.util.KeyboardMovement;
import me.alpha432.oyvey.util.SilentRotationMath;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

public class BedNuker extends Module {
   public static volatile boolean isRotating = false;
   public static volatile float visualYaw = 0.0F;
   public static volatile float visualPitch = 0.0F;
   public final Setting<Integer> range = this.num("Range", 5, 1, 10);
   public final Setting<Boolean> wallCheck = this.bool("WallCheck", false);
   public final Setting<Boolean> headOnly = this.bool("HeadOnly", false);
   private BlockPos lastTarget = null;
   private BlockPos cachedTarget = null;
   private float[] cachedRot = null;
   private float savedYaw = 0.0F;
   private float savedPitch = 0.0F;
   private boolean hasSaved = false;

   public BedNuker() {
      super("BedNuker", "Automatically breaks beds.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      this.lastTarget = null;
      this.cachedTarget = null;
      this.cachedRot = null;
      this.hasSaved = false;
      isRotating = false;
   }

   @Override
   public void onDisable() {
      this.stopMining();
      this.restoreRotation();
      this.cachedTarget = null;
      this.cachedRot = null;
      isRotating = false;
   }

   @Subscribe(priority = -10)
   public void onUpdate(UpdateWalkingPlayerEvent event) {
      if (mc.player != null && mc.level != null && mc.gameMode != null) {
         if (event.getStage() == Stage.MOTION_PRE) {
            Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
            if (mc.screen != null || mc.player.isSpectator() || mc.player.isUsingItem()
               || scaffold != null && scaffold.ownsRotation() || KillauraRecode.isRotating) {
               this.stopMining();
               this.cachedTarget = null;
               return;
            }
            this.cachedTarget = this.validTarget(this.lastTarget) ? this.lastTarget : this.findBest();
            this.cachedRot = null;
            isRotating = false;
            this.hasSaved = false;
            if (this.cachedTarget != null) {
               Vec3 center = new Vec3(this.cachedTarget.getX() + 0.5, this.cachedTarget.getY() + 0.5, this.cachedTarget.getZ() + 0.5);
               this.cachedRot = MathUtil.calcAngle(mc.player.getEyePosition(), center);
               visualYaw = mc.player.getYRot();
               visualPitch = mc.player.getXRot();
               isRotating = true;
               this.savedYaw = visualYaw;
               this.savedPitch = visualPitch;
               this.hasSaved = true;
               OyVey.rotationManager.setPlayerRotations(this.cachedRot[0], this.cachedRot[1]);
               OyVey.rotationManager.setServerRotations(this.cachedRot[0], this.cachedRot[1]);
               this.mineTarget();
            } else {
               this.stopMining();
            }
         } else if (event.getStage() == Stage.PRE) {
            if (this.hasSaved && this.cachedRot != null)
               OyVey.rotationManager.setPlayerRotations(this.cachedRot[0], this.cachedRot[1]);
         }
      }
   }

   @Subscribe(priority = -1100)
   public void onWalkingPost(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == Stage.POST) this.restoreRotation();
   }

   private void mineTarget() {
      Direction face = this.bestFace(this.cachedTarget);
      boolean progress;
      if (!this.cachedTarget.equals(this.lastTarget)) {
         this.stopMining();
         progress = mc.gameMode.startDestroyBlock(this.cachedTarget, face);
         this.lastTarget = progress ? this.cachedTarget.immutable() : null;
      } else {
         progress = mc.gameMode.continueDestroyBlock(this.cachedTarget, face);
      }
      // Use vanilla mining progress, cracks, item durability and swing. Do not
      // spam START/STOP packets or remove an unconfirmed bed from the world.
      if (progress) mc.player.swing(InteractionHand.MAIN_HAND);
   }

   private void stopMining() {
      if (this.lastTarget != null && mc.gameMode != null) mc.gameMode.stopDestroyBlock();
      this.lastTarget = null;
   }

   private void restoreRotation() {
      if (this.hasSaved) {
         if (mc.player != null) OyVey.rotationManager.setPlayerRotations(this.savedYaw, this.savedPitch);
         OyVey.rotationManager.clearServerRotations();
      }
      this.hasSaved = false;
      isRotating = false;
   }

   public boolean isMining() {
      return this.isEnabled() && mc.player != null && mc.level != null && mc.screen == null
         && this.lastTarget != null && this.isBed(this.lastTarget);
   }

   public void applyInput(ClientInput input) {
      if (!this.isEnabled() || !this.hasSaved || this.cachedRot == null) return;
      Input keys = input.keyPresses;
      var movement = SilentRotationMath.correctMovement(keys, visualYaw, this.cachedRot[0]);
      input.keyPresses = new Input(movement.forward() > 0, movement.forward() < 0,
         movement.sideways() > 0, movement.sideways() < 0, keys.jump(), keys.shift(),
         keys.sprint() && movement.forward() > 0);
      ((ClientInputAccessor)input).oyvey$setMoveVector(KeyboardMovement.vector(movement));
      if (movement.forward() <= 0) mc.player.setSprinting(false);
   }

   private boolean validTarget(BlockPos pos) {
      return pos != null && this.isBed(pos) && this.inRange(pos)
         && (!this.wallCheck.getValue() || this.hasLineOfSight(pos));
   }

   private BlockPos findBest() {
      BlockPos eye = mc.player.blockPosition();
      int r = this.range.getValue();
      List<BlockPos> candidates = new ArrayList<>();

      for (int x = -r; x <= r; x++) {
         for (int y = -r; y <= r; y++) {
            for (int z = -r; z <= r; z++) {
               BlockPos pos = eye.offset(x, y, z);
               if (this.validTarget(pos)) {
                  candidates.add(pos);
               }
            }
         }
      }

      if (candidates.isEmpty()) {
         return null;
      }

      candidates.sort(Comparator.comparingDouble(p -> mc.player.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5)));
      return candidates.get(0);
   }

   private boolean isBed(BlockPos pos) {
      BlockState state = mc.level.getBlockState(pos);
      if (!(state.getBlock() instanceof BedBlock)) {
         return false;
      } else {
         return !this.headOnly.getValue() ? true : state.hasProperty(BedBlock.PART) && state.getValue(BedBlock.PART) == BedPart.HEAD;
      }
   }

   private boolean inRange(BlockPos pos) {
      double dist = mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
      double r = this.range.getValue().intValue();
      return dist <= r * r;
   }

   private boolean hasLineOfSight(BlockPos pos) {
      Vec3 eyes = mc.player.getEyePosition();
      Vec3 to = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
      BlockHitResult hit = mc.level.clip(new ClipContext(eyes, to, Block.COLLIDER, Fluid.NONE, mc.player));
      return hit.getType() == Type.MISS || hit.getBlockPos().equals(pos);
   }

   private Direction bestFace(BlockPos pos) {
      Vec3 eyes = mc.player.getEyePosition();
      Direction best = Direction.UP;
      double bestDot = -1.0 / 0.0;

      for (Direction d : Direction.values()) {
         Vec3 center = Vec3.atCenterOf(pos).add(d.getStepX() * 0.5, d.getStepY() * 0.5, d.getStepZ() * 0.5);
         Vec3 dir = eyes.subtract(center).normalize();
         double dot = dir.x * d.getStepX() + dir.y * d.getStepY() + dir.z * d.getStepZ();
         if (dot > bestDot) {
            bestDot = dot;
            best = d;
         }
      }

      return best;
   }
}
