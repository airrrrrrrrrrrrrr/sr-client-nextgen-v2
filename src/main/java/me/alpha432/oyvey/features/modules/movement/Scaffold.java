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
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends Module {
   public static volatile boolean isRotating = false;
   public static volatile float visualYaw = 0.0F;
   public static volatile float visualPitch = 0.0F;
   public final Setting<Scaffold.Technique> technique = this.register(new Setting<>("Technique", Scaffold.Technique.NORMAL));
   public final Setting<Scaffold.GodBridgeLedge> gbLedge = this.register(
      new Setting<>("GBLedge", Scaffold.GodBridgeLedge.SNEAK, v -> this.technique.getValue() == Scaffold.Technique.GOD_BRIDGE)
   );
   public final Setting<Scaffold.TowerMode> towerMode = this.register(new Setting<>("Tower", Scaffold.TowerMode.MOTION));
   public final Setting<Integer> towerDelay = this.register(new Setting<>("TowerDelay", 0, 0, 10, v -> this.towerMode.getValue() == Scaffold.TowerMode.JUMP));
   public final Setting<Boolean> rotate = this.register(new Setting<>("Rotate", true));
   public final Setting<Boolean> lookback = this.register(
      new Setting<>(
         "Lookback",
         true,
         v -> this.rotate.getValue() && this.technique.getValue() != Scaffold.Technique.GOD_BRIDGE && this.technique.getValue() != Scaffold.Technique.TELLY
      )
   );
   public final Setting<Float> rotSpeed = this.register(new Setting<>("RotSpeed", 0.85F, 0.1F, 1.0F, v -> this.rotate.getValue()));
   public final Setting<Integer> delay = this.register(new Setting<>("PlaceDelay", 0, 0, 5));
   public final Setting<Boolean> safeWalk = this.register(new Setting<>("SafeWalk", true));
   public final Setting<Boolean> eagle = this.register(new Setting<>("Eagle", false));
   public final Setting<Boolean> expand = this.register(new Setting<>("Expand", false));
   public final Setting<Boolean> autoBlock = this.register(new Setting<>("AutoBlock", true));
   public final Setting<Boolean> sprint = this.register(new Setting<>("Sprint", false));
   public final Setting<Boolean> vulcanBypass = this.register(new Setting<>("VulcanBypass", false));
   public final Setting<Boolean> debug = this.register(new Setting<>("Debug", false));
   private int savedSlot = -1;
   private Scaffold.PlaceInfo pendingPlace;
   private int placeTimer;
   private int placeCooldown;
   private int towerTimer;
   private int eagleTicks;
   private int gbSneakTicks;
   private int airTicksHypixel;
   private boolean pulldownArmed;
   private int tellyGroundY;

   public Scaffold() {
      super("Scaffold", "Place blocks under you automatically.", Module.Category.MOVEMENT);
      this.drawn.setValue(false);
   }

   @Override
   public void onDisable() {
      isRotating = false;
      this.pendingPlace = null;
      OyVey.rotationManager.clearServerRotations();
      this.restoreSlot();
      this.placeTimer = 0;
      this.placeCooldown = 0;
      this.towerTimer = 0;
      this.eagleTicks = 0;
      this.gbSneakTicks = 0;
      this.airTicksHypixel = 0;
      this.pulldownArmed = false;
      this.tellyGroundY = 0;
      if (mc.player != null) {
         mc.player.setShiftKeyDown(false);
      }
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.level != null) {
          if (this.isSprintSuppressed()) {
             mc.player.setSprinting(false);
          }

         if (mc.player.onGround()) {
            this.airTicksHypixel = 0;
         } else {
            this.airTicksHypixel++;
         }

         if (this.towerMode.getValue() != Scaffold.TowerMode.NONE && mc.options.keyJump.isDown()) {
            this.handleTower();
         }

         if (this.technique.getValue() == Scaffold.Technique.TELLY) {
            if (mc.player.onGround()) {
               this.tellyGroundY = mc.player.blockPosition().getY() - 1;
            }

            if (mc.player.onGround() && this.anyMovementKey() && this.isNearForwardEdge()) {
               Vec3 mot = mc.player.getDeltaMovement();
               mc.player.setDeltaMovement(mot.x, 0.42, mot.z);
               mc.player.fallDistance = 0.0;
            }
         }

         boolean gbSneak = false;
         if (this.technique.getValue() == Scaffold.Technique.GOD_BRIDGE
            && this.gbLedge.getValue() == Scaffold.GodBridgeLedge.SNEAK
            && mc.player.onGround()
            && this.isNearForwardEdge()) {
            this.gbSneakTicks = Math.max(this.gbSneakTicks, 3);
         }

         if (this.gbSneakTicks > 0) {
            gbSneak = true;
            this.gbSneakTicks--;
         }

         if (!this.eagle.getValue()) {
            mc.player.setShiftKeyDown(gbSneak);
         } else {
            mc.player.setShiftKeyDown(this.eagleTicks > 0 || gbSneak);
            if (this.eagleTicks > 0) {
               this.eagleTicks--;
            }
         }

         if (this.safeWalk.getValue() && mc.player.onGround()) {
            Vec3 mot = mc.player.getDeltaMovement();
            BlockPos next = new BlockPos(Mth.floor(mc.player.getX() + mot.x), mc.player.blockPosition().getY() - 1, Mth.floor(mc.player.getZ() + mot.z));
            if (mc.level.getBlockState(next).isAir()) {
               mc.player.setDeltaMovement(mot.x * 0.4, mot.y, mot.z * 0.4);
            }
         }
      }
   }

   @Subscribe
   public void onUpdateWalkingPlayer(UpdateWalkingPlayerEvent event) {
      if (this.isEnabled() && mc.player != null && mc.level != null) {
          if (event.getStage() == Stage.POST) {
             if (this.pendingPlace != null) {
                Scaffold.PlaceInfo info = this.pendingPlace;
                this.pendingPlace = null;
                this.doPlace(info);
             }

             if (isRotating && mc.player != null) {
                OyVey.rotationManager.setPlayerRotations(visualYaw, visualPitch);
                isRotating = false;
             }

             OyVey.rotationManager.clearServerRotations();
          } else if (event.getStage() == Stage.PRE) {
             if (this.isSprintSuppressed()) {
                mc.player.setSprinting(false);
             }

             this.pendingPlace = null;
             if (this.technique.getValue() == Scaffold.Technique.TELLY) {
               this.handleTellyPlace();
            } else if (this.placeCooldown > 0) {
               this.placeCooldown--;
               this.restoreSlot();
            } else {
               BlockPos target = this.findTarget();
               if (target == null) {
                  if (this.debug.getValue()) {
                     OyVey.LOGGER.info("[Scaffold] no target");
                  }

                  this.restoreSlot();
               } else {
                  boolean doLookback = this.rotate.getValue() && this.lookback.getValue() && this.technique.getValue() != Scaffold.Technique.GOD_BRIDGE;
                  if (doLookback) {
                     this.placeLookback(target);
                  } else {
                     this.placeNormal(target);
                  }
               }
            }
         }
      }
   }

   private void placeLookback(BlockPos target) {
      Scaffold.PlaceInfo info = this.findNeighbor(target);
      if (info == null) {
         this.restoreSlot();
      } else if (this.placeTimer < this.delay.getValue()) {
         this.placeTimer++;
         this.restoreSlot();
      } else {
         this.placeTimer = 0;
         this.applyPlacementRotation(info);
         this.pendingPlace = info;
      }
   }

   private void placeNormal(BlockPos target) {
      Scaffold.PlaceInfo info = this.findNeighbor(target);
      if (info == null) {
         this.restoreSlot();
      } else {
         if (this.rotate.getValue()) {
            float[] raw;
            if (this.technique.getValue() == Scaffold.Technique.GOD_BRIDGE && this.anyMovementKey()) {
               float movYaw = this.getMovementYaw();
               float gbYaw = Mth.wrapDegrees(movYaw + (this.isGodBridgeRightSide(movYaw) ? 45.0F : -45.0F));
               raw = new float[]{gbYaw, 75.7F};
            } else {
               raw = RotationUtils.fromRotationVec(info.rotTarget().subtract(mc.player.getEyePosition()));
            }

            this.applyRotation(raw);
         }

         if (this.placeTimer < this.delay.getValue()) {
            this.placeTimer++;
            this.restoreSlot();
         } else {
            this.placeTimer = 0;
            this.pendingPlace = info;
         }
      }
   }

   private void handleTellyPlace() {
      if (this.placeCooldown > 0) {
         this.placeCooldown--;
         this.restoreSlot();
      } else if (!mc.player.onGround() && this.tellyGroundY != 0) {
         float movYaw = this.anyMovementKey() ? this.getMovementYaw() : mc.player.getYRot();
         float backYaw = Mth.wrapDegrees(movYaw + 180.0F);
         float[] sent = RotationUtils.normalizeGcd(new float[]{backYaw, 75.0F}, mc.player.getYRot(), mc.player.getXRot());
         visualYaw = mc.player.getYRot();
         visualPitch = mc.player.getXRot();
         isRotating = true;
         OyVey.rotationManager.setPlayerRotations(sent[0], sent[1]);
         double rad = Math.toRadians(movYaw);
         int bx = (int)Math.floor(mc.player.getX() + Math.sin(rad));
         int bz = (int)Math.floor(mc.player.getZ() - Math.cos(rad));
         BlockPos target = new BlockPos(bx, this.tellyGroundY, bz);
         if (!mc.level.getBlockState(target).canBeReplaced()) {
            this.restoreSlot();
         } else {
            Scaffold.PlaceInfo info = this.findNeighbor(target);
            if (info == null) {
               this.restoreSlot();
            } else if (this.placeTimer < this.delay.getValue()) {
               this.placeTimer++;
               this.restoreSlot();
            } else {
               this.placeTimer = 0;
             this.pendingPlace = info;
            }
         }
      } else {
         this.restoreSlot();
      }
   }

   private void doPlace(Scaffold.PlaceInfo info) {
      if (this.vulcanBypass.getValue() && mc.player.isSprinting()) {
         mc.player.setSprinting(false);
      }

      InteractionHand hand = this.getBlockHand();
      if (hand == null) {
         if (!this.autoBlock.getValue()) {
            this.restoreSlot();
            return;
         }

         hand = this.autoSwitchBlock();
         if (hand == null) {
            if (this.debug.getValue()) {
               OyVey.LOGGER.info("[Scaffold] no block in hotbar");
            }

            this.restoreSlot();
            return;
         }
      }

      if (this.debug.getValue()) {
         OyVey.LOGGER.info("[Scaffold] placing hand={}", hand);
      }

      InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, info.hitResult());
      if (result instanceof InteractionResult.Success success && success.swingSource() != InteractionResult.SwingSource.NONE) {
         mc.player.swing(hand);
      }

      this.placeCooldown = 0;
      if (this.eagle.getValue()) {
         this.eagleTicks = 2;
      }

      this.restoreSlot();
   }

   private BlockPos findTarget() {
      int y = mc.player.blockPosition().getY() - 1;
      int px = mc.player.blockPosition().getX();
      int pz = mc.player.blockPosition().getZ();
      BlockPos below = new BlockPos(px, y, pz);
      if (mc.level.getBlockState(below).canBeReplaced()) {
         return below;
      }

      if (this.anyMovementKey()) {
         float yaw = this.getMovementYaw();
         double radians = Math.toRadians(yaw);
         double distance = this.expand.getValue() ? 1.25 : 0.8;
         BlockPos ahead = new BlockPos(
            Mth.floor(mc.player.getX() - Math.sin(radians) * distance),
            y,
            Mth.floor(mc.player.getZ() + Math.cos(radians) * distance)
         );
         if (mc.level.getBlockState(ahead).canBeReplaced()) {
            return ahead;
         }
      }

      if (mc.player.onGround()) {
         return null;
      }

      BlockPos feet = mc.player.blockPosition();
      return mc.level.getBlockState(feet).canBeReplaced() ? feet : null;
   }

   private boolean anyMovementKey() {
      return mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
   }

   private Scaffold.PlaceInfo findNeighbor(BlockPos target) {
      Vec3 eye = mc.player.getEyePosition();
      Scaffold.PlaceInfo best = null;
      double bestDist = 1.7976931348623157E308;

      for (Direction dir : Direction.values()) {
         BlockPos nb = target.relative(dir);
         if (!mc.level.getBlockState(nb).canBeReplaced()) {
            Direction face = dir.getOpposite();
            Vec3 P = getFacePoint(nb, face, eye);
            double distSq = eye.distanceToSqr(P);
            if (!(distSq > 20.25) && !(distSq >= bestDist)) {
               bestDist = distSq;
               Vec3 hitVec = jitterPoint(P, face, nb);
               best = new Scaffold.PlaceInfo(new BlockHitResult(hitVec, face, nb, false), P);
            }
         }
      }

      return best;
   }

   private void applyPlacementRotation(Scaffold.PlaceInfo info) {
      this.applyRotation(RotationUtils.fromRotationVec(info.rotTarget().subtract(mc.player.getEyePosition())));
   }

   private void applyRotation(float[] raw) {
      float[] sent = RotationUtils.normalizeGcd(raw, mc.player.getYRot(), mc.player.getXRot());
      visualYaw = mc.player.getYRot();
      visualPitch = mc.player.getXRot();
      isRotating = true;
      OyVey.rotationManager.setPlayerRotations(sent[0], sent[1]);
      OyVey.rotationManager.setServerRotations(sent[0], sent[1]);
   }

   public boolean isSprintSuppressed() {
      return this.isEnabled() && !this.sprint.getValue();
   }

   private static Vec3 getFacePoint(BlockPos nb, Direction face, Vec3 eye) {
      double lo = 0.15;
      double hi = 0.85;
      double fx = face.getStepX();
      double fy = face.getStepY();
      double fz = face.getStepZ();
      double px;
      double py;
      double pz;
      if (face.getAxis() == Axis.X) {
         px = nb.getX() + 0.5 + fx * 0.5;
         py = Mth.clamp(eye.y, nb.getY() + lo, nb.getY() + hi);
         pz = Mth.clamp(eye.z, nb.getZ() + lo, nb.getZ() + hi);
      } else if (face.getAxis() == Axis.Y) {
         px = Mth.clamp(eye.x, nb.getX() + lo, nb.getX() + hi);
         py = nb.getY() + 0.5 + fy * 0.5;
         pz = Mth.clamp(eye.z, nb.getZ() + lo, nb.getZ() + hi);
      } else {
         px = Mth.clamp(eye.x, nb.getX() + lo, nb.getX() + hi);
         py = Mth.clamp(eye.y, nb.getY() + lo, nb.getY() + hi);
         pz = nb.getZ() + 0.5 + fz * 0.5;
      }

      return new Vec3(px, py, pz);
   }

   private static Vec3 jitterPoint(Vec3 P, Direction face, BlockPos nb) {
      return new Vec3(
         face.getAxis() == Axis.X ? P.x : Mth.clamp(P.x + jitter(), nb.getX() + 0.02, nb.getX() + 0.98),
         face.getAxis() == Axis.Y ? P.y : Mth.clamp(P.y + jitter(), nb.getY() + 0.02, nb.getY() + 0.98),
         face.getAxis() == Axis.Z ? P.z : Mth.clamp(P.z + jitter(), nb.getZ() + 0.02, nb.getZ() + 0.98)
      );
   }

   private static double jitter() {
      return ThreadLocalRandom.current().nextDouble(-0.04, 0.04);
   }

   private float getMovementYaw() {
      boolean w = mc.options.keyUp.isDown();
      boolean s = mc.options.keyDown.isDown();
      boolean a = mc.options.keyLeft.isDown();
      boolean d = mc.options.keyRight.isDown();
      float yaw = mc.player.getYRot();
      if (w && !s && a && !d) {
         yaw -= 45.0F;
      } else if (w && !s && d && !a) {
         yaw += 45.0F;
      } else if (s && !w) {
         yaw += 180.0F;
         if (a && !d) {
            yaw += 45.0F;
         } else if (d && !a) {
            yaw -= 45.0F;
         }
      } else if (a && !d) {
         yaw -= 90.0F;
      } else if (d && !a) {
         yaw += 90.0F;
      }

      return Mth.wrapDegrees(yaw);
   }

   private boolean isGodBridgeRightSide(float movYaw) {
      float yr = (float)Math.toRadians(movYaw);
      int rx = (int)Math.round(Math.cos(yr));
      int rz = (int)Math.round(Math.sin(yr));
      BlockPos rightBelow = mc.player.blockPosition().below().offset(rx, 0, rz);
      return mc.level.getBlockState(rightBelow).isAir();
   }

   private boolean isNearForwardEdge() {
      float yr = (float)Math.toRadians(mc.player.getYRot());
      int dx = -((int)Math.round(Math.sin(yr)));
      int dz = (int)Math.round(Math.cos(yr));
      BlockPos fwd = mc.player.blockPosition().offset(dx, -1, dz);
      return mc.level.getBlockState(fwd).isAir();
   }

   private void handleTower() {
      Vec3 mot = mc.player.getDeltaMovement();
      switch ((Scaffold.TowerMode)this.towerMode.getValue()) {
         case JUMP:
            if (mc.player.onGround() && this.towerTimer >= this.towerDelay.getValue()) {
               mc.player.setDeltaMovement(mot.x, 0.42, mot.z);
               mc.player.fallDistance = 0.0;
               this.towerTimer = 0;
            } else if (mc.player.onGround()) {
               this.towerTimer++;
            }
            break;
         case MOTION:
            if (mc.player.onGround()) {
               mc.player.setDeltaMovement(mot.x, 0.42, mot.z);
               mc.player.fallDistance = 0.0;
            } else if (mot.y < 0.1) {
               mc.player.setDeltaMovement(mot.x, -0.3, mot.z);
            }
            break;
         case TELEPORT:
            if (mc.player.onGround()) {
               double ny = mc.player.getY() + 1.1;
               mc.player.connection.send(new Pos(mc.player.getX(), ny, mc.player.getZ(), false, mc.player.horizontalCollision));
               mc.player.setPos(mc.player.getX(), ny, mc.player.getZ());
               mc.player.setDeltaMovement(mot.x, 0.0, mot.z);
               mc.player.fallDistance = 0.0;
            } else {
               mc.player.setDeltaMovement(mot.x, 0.0, mot.z);
            }
            break;
         case HYPIXEL:
            if (!mc.player.onGround()) {
               if (this.airTicksHypixel > 14) {
                  mc.player.setDeltaMovement(mot.x * 0.6, mot.y - 0.09, mot.z * 0.6);
               } else {
                  switch (this.airTicksHypixel % 3) {
                     case 0:
                        mc.player.setDeltaMovement(mot.x, 0.42, mot.z);
                        break;
                     case 2:
                        mc.player.setDeltaMovement(mot.x, 1.0 - mc.player.getY() % 1.0, mot.z);
                  }
               }
            }
            break;
         case VULCAN:
            if (mc.player.tickCount % 2 == 0) {
               mc.player.setDeltaMovement(mot.x, 0.7, mot.z);
            } else {
               boolean moving = Math.abs(mot.x) > 0.02 || Math.abs(mot.z) > 0.02;
               mc.player.setDeltaMovement(mot.x, moving ? 0.42 : 0.6, mot.z);
               mc.player.fallDistance = 0.0;
            }
            break;
         case PULLDOWN:
            if (mc.player.onGround()) {
               this.pulldownArmed = true;
               mc.player.setDeltaMovement(mot.x, 0.42, mot.z);
               mc.player.fallDistance = 0.0;
            } else if (this.pulldownArmed && mot.y < 0.1) {
               mc.player.setDeltaMovement(mot.x, -1.0, mot.z);
               this.pulldownArmed = false;
            }
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
      if (this.technique.getValue() == Scaffold.Technique.GOD_BRIDGE) {
         return "GodBridge";
      }

      if (this.technique.getValue() == Scaffold.Technique.TELLY) {
         return "Telly";
      }

      Scaffold.TowerMode t = this.towerMode.getValue();
      return t != Scaffold.TowerMode.NONE ? t.name() : null;
   }

   public enum GodBridgeLedge {
      NONE,
      SNEAK,
      JUMP,
      STOP;
   }

   private record PlaceInfo(BlockHitResult hitResult, Vec3 rotTarget) {
   }

   public enum Technique {
      NORMAL,
      GOD_BRIDGE,
      TELLY;
   }

   public enum TowerMode {
      NONE,
      JUMP,
      MOTION,
      TELEPORT,
      HYPIXEL,
      VULCAN,
      PULLDOWN;
   }
}
