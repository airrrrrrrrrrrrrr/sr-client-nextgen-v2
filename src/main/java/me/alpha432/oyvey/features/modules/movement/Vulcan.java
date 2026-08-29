package me.alpha432.oyvey.features.modules.movement;

import java.util.concurrent.ThreadLocalRandom;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.RotationUtils;
import me.alpha432.oyvey.util.inventory.InventoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class Vulcan extends Module {
   public final Setting<Float> pitch = this.register(new Setting<>("Pitch", 80.0F, 79.0F, 81.0F));
   public final Setting<Float> rotSpeed = this.register(new Setting<>("RotSpeed", 0.85F, 0.1F, 1.0F));
   public final Setting<Float> friction = this.register(new Setting<>("Friction", 0.85F, 0.5F, 1.0F));
   public final Setting<Boolean> tower = this.register(new Setting<>("Tower", false));
   public final Setting<Boolean> autoBlock = this.register(new Setting<>("AutoBlock", true));
   public final Setting<Integer> delay = this.register(new Setting<>("Delay", 0, 0, 5));
   private float smoothYaw;
   private float smoothPitch;
   private boolean hasSmooth;
   private boolean savedRot;
   private boolean needSprintRestore;
   private int savedSlot = -1;
   private int placeTimer;

   public Vulcan() {
      super("Vulcan", "Backward-facing scaffold with Vulcan bypass.", Module.Category.MOVEMENT);
      this.drawn.setValue(false);
   }

   @Override
   public void onDisable() {
      if (this.savedRot && mc.player != null) {
         OyVey.rotationManager.restoreRotations();
      }

      if (this.needSprintRestore && mc.player != null) {
         mc.player.setSprinting(true);
         mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.START_SPRINTING));
      }

      this.restoreSlot();
      this.savedRot = false;
      this.hasSmooth = false;
      this.needSprintRestore = false;
      this.placeTimer = 0;
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.level != null) {
         if (this.needSprintRestore) {
            mc.player.setSprinting(true);
            mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.START_SPRINTING));
            this.needSprintRestore = false;
         }

         if (this.tower.getValue() && mc.options.keyJump.isDown()) {
            this.handleTower();
         }
      }
   }

   @Subscribe
   public void onUpdateWalkingPlayer(UpdateWalkingPlayerEvent event) {
      if (mc.player != null && mc.level != null) {
         if (event.getStage() == Stage.POST) {
            if (this.savedRot) {
               OyVey.rotationManager.restoreRotations();
               this.savedRot = false;
            }
         } else if (event.getStage() == Stage.PRE) {
            BlockPos target = this.findBackwardTarget();
            if (target == null) {
               this.hasSmooth = false;
               this.restoreSlot();
            } else {
               Vulcan.PlaceInfo info = this.findNeighbor(target);
               if (info == null) {
                  this.hasSmooth = false;
                  this.restoreSlot();
               } else {
                  float[] rot = this.computeBackwardRot();
                  OyVey.rotationManager.updateRotations();
                  OyVey.rotationManager.setPlayerRotations(rot[0], rot[1]);
                  this.savedRot = true;
                  Vec3 mot = mc.player.getDeltaMovement();
                  mc.player.setDeltaMovement(mot.x * this.friction.getValue().floatValue(), mot.y, mot.z * this.friction.getValue().floatValue());
                  if (this.placeTimer < this.delay.getValue()) {
                     this.placeTimer++;
                     this.restoreSlot();
                  } else {
                     this.placeTimer = 0;
                     if (mc.player.isSprinting()) {
                        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SPRINTING));
                        mc.player.setSprinting(false);
                        this.needSprintRestore = true;
                     }

                     if (!this.rayTraceValidates(info.hitResult().getBlockPos(), info.hitResult().getDirection(), rot[0], rot[1])) {
                        this.restoreSlot();
                     } else {
                        InteractionHand hand = this.getBlockHand();
                        if (hand == null) {
                           if (!this.autoBlock.getValue()) {
                              this.restoreSlot();
                              return;
                           }

                           hand = this.autoSwitchBlock();
                           if (hand == null) {
                              this.restoreSlot();
                              return;
                           }
                        }

                        mc.gameMode.useItemOn(mc.player, hand, info.hitResult());
                        mc.player.swing(hand);
                        this.restoreSlot();
                     }
                  }
               }
            }
         }
      }
   }

   private BlockPos findBackwardTarget() {
      if (!this.anyMovementKey()) {
         return null;
      }

      float backYaw = (float)Math.toRadians(mc.player.getYRot() + 180.0F);
      double dx = -Math.sin(backYaw);
      double dz = Math.cos(backYaw);
      int by = mc.player.blockPosition().getY() - 1;
      BlockPos primary = new BlockPos(Mth.floor(mc.player.getX() + dx * 0.5), by, Mth.floor(mc.player.getZ() + dz * 0.5));
      if (mc.level.getBlockState(primary).isAir()) {
         return primary;
      }

      BlockPos below = mc.player.blockPosition().below();
      return mc.level.getBlockState(below).isAir() ? below : null;
   }

   private boolean anyMovementKey() {
      return mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
   }

   private Vulcan.PlaceInfo findNeighbor(BlockPos target) {
      Vec3 eye = mc.player.getEyePosition();
      Vulcan.PlaceInfo best = null;
      double bestDist = 1.7976931348623157E308;

      for (Direction dir : Direction.values()) {
         BlockPos nb = target.relative(dir);
         BlockState state = mc.level.getBlockState(nb);
         if (!state.isAir() && state.getFluidState().isEmpty()) {
            Direction face = dir.getOpposite();
            Vec3 P = closestPointOnFace(nb, face, eye);
            double distSq = eye.distanceToSqr(P);
            if (!(distSq > 20.25) && !(distSq >= bestDist)) {
               bestDist = distSq;
               best = new Vulcan.PlaceInfo(new BlockHitResult(jitterPoint(P, face, nb), face, nb, false), P);
            }
         }
      }

      return best;
   }

   private static Vec3 closestPointOnFace(BlockPos nb, Direction face, Vec3 eye) {
      double fx = face.getStepX();
      double fy = face.getStepY();
      double fz = face.getStepZ();
      if (face.getAxis() == Axis.X) {
         return new Vec3(nb.getX() + 0.5 + fx * 0.5, Mth.clamp(eye.y, nb.getY(), nb.getY() + 1.0), Mth.clamp(eye.z, nb.getZ(), nb.getZ() + 1.0));
      } else {
         return face.getAxis() == Axis.Y
            ? new Vec3(Mth.clamp(eye.x, nb.getX(), nb.getX() + 1.0), nb.getY() + 0.5 + fy * 0.5, Mth.clamp(eye.z, nb.getZ(), nb.getZ() + 1.0))
            : new Vec3(Mth.clamp(eye.x, nb.getX(), nb.getX() + 1.0), Mth.clamp(eye.y, nb.getY(), nb.getY() + 1.0), nb.getZ() + 0.5 + fz * 0.5);
      }
   }

   private static Vec3 jitterPoint(Vec3 P, Direction face, BlockPos nb) {
      return new Vec3(
         face.getAxis() == Axis.X ? P.x : Mth.clamp(P.x + jitter(), nb.getX() + 0.01, nb.getX() + 0.99),
         face.getAxis() == Axis.Y ? P.y : Mth.clamp(P.y + jitter(), nb.getY() + 0.01, nb.getY() + 0.99),
         face.getAxis() == Axis.Z ? P.z : Mth.clamp(P.z + jitter(), nb.getZ() + 0.01, nb.getZ() + 0.99)
      );
   }

   private static double jitter() {
      return ThreadLocalRandom.current().nextDouble(-0.05, 0.05);
   }

   private float[] computeBackwardRot() {
      float targetYaw = Mth.wrapDegrees(mc.player.getYRot() + 180.0F);
      float targetPitch = this.pitch.getValue();
      if (!this.hasSmooth) {
         this.smoothYaw = mc.player.getYRot();
         this.smoothPitch = mc.player.getXRot();
         this.hasSmooth = true;
      }

      float s = this.rotSpeed.getValue();
      double gcdStep = RotationUtils.getGcdStep();
      float dy = Mth.wrapDegrees(targetYaw - this.smoothYaw) * s;
      float dp = (targetPitch - this.smoothPitch) * s;
      dy = RotationUtils.applyGcd(dy, gcdStep);
      dp = RotationUtils.applyGcd(dp, gcdStep);
      this.smoothYaw = Mth.wrapDegrees(this.smoothYaw + dy);
      this.smoothPitch = Mth.clamp(this.smoothPitch + dp, -90.0F, 90.0F);
      return new float[]{this.smoothYaw, this.smoothPitch};
   }

   private boolean rayTraceValidates(BlockPos nb, Direction face, float yaw, float pitch) {
      Vec3 eye = mc.player.getEyePosition();
      Vec3 look = Vec3.directionFromRotation(pitch, yaw);
      Vec3 end = eye.add(look.scale(6.0));
      BlockHitResult hit = mc.level.clip(new ClipContext(eye, end, Block.OUTLINE, Fluid.NONE, mc.player));
      return hit.getType() == Type.BLOCK && hit.getBlockPos().equals(nb) && hit.getDirection() == face;
   }

   private void handleTower() {
      Vec3 mot = mc.player.getDeltaMovement();
      if (mc.player.onGround()) {
         mc.player.setDeltaMovement(mot.x, 0.42, mot.z);
         mc.player.fallDistance = 0.0;
      } else if (mot.y < 0.1) {
         mc.player.setDeltaMovement(mot.x, -0.3, mot.z);
      }
   }

   private InteractionHand getBlockHand() {
      if (mc.player.getMainHandItem().getItem() instanceof BlockItem) {
         return InteractionHand.MAIN_HAND;
      } else {
         return mc.player.getOffhandItem().getItem() instanceof BlockItem ? InteractionHand.OFF_HAND : null;
      }
   }

   private InteractionHand autoSwitchBlock() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getItem(i).getItem() instanceof BlockItem) {
            if (this.savedSlot == -1) {
               this.savedSlot = InventoryUtil.selected();
            }

            InventoryUtil.swap(i);
            return InteractionHand.MAIN_HAND;
         }
      }

      return null;
   }

   private void restoreSlot() {
      if (this.savedSlot != -1 && mc.player != null) {
         InventoryUtil.swap(this.savedSlot);
         this.savedSlot = -1;
      }
   }

   @Override
   public String getDisplayInfo() {
      return this.tower.getValue() ? "Tower" : null;
   }

   private record PlaceInfo(BlockHitResult hitResult, Vec3 rotTarget) {
   }
}
