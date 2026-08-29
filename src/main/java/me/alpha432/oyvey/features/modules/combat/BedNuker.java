package me.alpha432.oyvey.features.modules.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.MathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
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
      if (this.lastTarget != null && mc.gameMode != null) {
         mc.gameMode.stopDestroyBlock();
      }

      this.lastTarget = null;
      this.cachedTarget = null;
      this.cachedRot = null;
      isRotating = false;
      this.hasSaved = false;
      OyVey.rotationManager.clearServerRotations();
   }

   @Subscribe
   public void onUpdate(UpdateWalkingPlayerEvent event) {
      if (mc.player != null && mc.level != null) {
         if (event.getStage() == Stage.MOTION_PRE) {
            this.cachedTarget = this.findBest();
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
            } else {
               OyVey.rotationManager.clearServerRotations();
               if (this.lastTarget != null) {
                  mc.gameMode.stopDestroyBlock();
                  this.lastTarget = null;
               }
            }
         } else if (event.getStage() == Stage.POST) {
            isRotating = false;
            if (this.hasSaved) {
               OyVey.rotationManager.setPlayerRotations(this.savedYaw, this.savedPitch);
               this.hasSaved = false;
            }
         } else if (event.getStage() == Stage.PRE) {
            if (this.cachedTarget == null) {
               if (this.lastTarget != null) {
                  mc.gameMode.stopDestroyBlock();
                  this.lastTarget = null;
               }
            } else {
               if (this.cachedRot != null) {
                  OyVey.rotationManager.setPlayerRotations(this.cachedRot[0], this.cachedRot[1]);
               }

               Direction face = this.bestFace(this.cachedTarget);
               if (!this.cachedTarget.equals(this.lastTarget)) {
                  if (this.lastTarget != null) {
                     mc.gameMode.stopDestroyBlock();
                  }

                  mc.gameMode.startDestroyBlock(this.cachedTarget, face);
                  this.lastTarget = this.cachedTarget;
               } else {
                  mc.gameMode.continueDestroyBlock(this.cachedTarget, face);
               }
            }
         }
      }
   }

   private BlockPos findBest() {
      BlockPos eye = mc.player.blockPosition();
      int r = this.range.getValue();
      List<BlockPos> candidates = new ArrayList<>();

      for (int x = -r; x <= r; x++) {
         for (int y = -r; y <= r; y++) {
            for (int z = -r; z <= r; z++) {
               BlockPos pos = eye.offset(x, y, z);
               if (this.isBed(pos) && this.inRange(pos) && (!this.wallCheck.getValue() || this.hasLineOfSight(pos))) {
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
      return mc.level.clip(new ClipContext(eyes, to, Block.COLLIDER, Fluid.NONE, mc.player)).getType() == Type.MISS;
   }

   private Direction bestFace(BlockPos pos) {
      Vec3 eyes = mc.player.getEyePosition();
      Direction best = Direction.UP;
      double bestDot = -1.0 / 0.0;

      for (Direction d : Direction.values()) {
         Vec3 center = Vec3.atCenterOf(pos).add(d.getStepX() * 0.5, d.getStepY() * 0.5, d.getStepZ() * 0.5);
         Vec3 dir = center.subtract(eyes).normalize();
         double dot = dir.x * d.getStepX() + dir.y * d.getStepY() + dir.z * d.getStepZ();
         if (dot > bestDot) {
            bestDot = dot;
            best = d;
         }
      }

      return best;
   }
}
