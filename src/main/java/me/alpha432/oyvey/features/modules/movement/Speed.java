package me.alpha432.oyvey.features.modules.movement;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.util.player.Vulcan2Movement;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.world.effect.MobEffects;

public class Speed extends Module {
   public final Setting<Speed.Mode> mode = this.register(new Setting<>("Mode", Speed.Mode.STRAFE));
   public final Setting<Double> speed = this.register(
      new Setting<>(
         "Speed",
         0.28,
         0.1,
         1.0,
         v -> this.mode.getValue() != Speed.Mode.VULCANGROUND && this.mode.getValue() != Speed.Mode.LEGIT && this.mode.getValue() != Speed.Mode.CUBECRAFT
      )
   );
   public final Setting<Boolean> jump = this.register(
      new Setting<>(
         "AutoJump",
         true,
         v -> this.mode.getValue() != Speed.Mode.VULCANGROUND && this.mode.getValue() != Speed.Mode.LEGIT && this.mode.getValue() != Speed.Mode.CUBECRAFT
      )
   );
   public final Setting<Boolean> lowHop = this.register(
      new Setting<>(
         "LowHop",
         true,
         v -> this.mode.getValue() != Speed.Mode.VULCANGROUND && this.mode.getValue() != Speed.Mode.LEGIT && this.mode.getValue() != Speed.Mode.CUBECRAFT
      )
   );
   public final Setting<Double> lowHopHeight = this.register(
      new Setting<>(
         "HopHeight",
         0.401,
         0.1,
         0.42,
         v -> this.mode.getValue() != Speed.Mode.VULCANGROUND && this.mode.getValue() != Speed.Mode.LEGIT && this.mode.getValue() != Speed.Mode.CUBECRAFT
      )
   );
   public final Setting<Boolean> damageBoost = this.register(
      new Setting<>("DamageBoost", false, v -> this.mode.getValue() != Speed.Mode.LEGIT && this.mode.getValue() != Speed.Mode.CUBECRAFT)
   );
   public final Setting<Double> boostFactor = this.register(
      new Setting<>(
         "BoostFactor",
         1.5,
         1.0,
         5.0,
         v -> this.mode.getValue() != Speed.Mode.VULCANGROUND
            && this.mode.getValue() != Speed.Mode.LEGIT
            && this.mode.getValue() != Speed.Mode.CUBECRAFT
            && this.damageBoost.getValue()
      )
   );
   public final Setting<Boolean> potionSync = this.register(
      new Setting<>(
         "PotionSync",
         true,
         v -> this.mode.getValue() != Speed.Mode.VULCANGROUND && this.mode.getValue() != Speed.Mode.LEGIT && this.mode.getValue() != Speed.Mode.CUBECRAFT
      )
   );
   public final Setting<Double> hhHeight = this.register(new Setting<>("HopHeight", 1.25, 1.25, 6.0, v -> this.mode.getValue() == Speed.Mode.HIGHHOP));
   public final Setting<Boolean> vulcanGroundOnly = this.register(new Setting<>("VulcanGroundOnly", true, v -> this.mode.getValue() == Speed.Mode.VULCAN));
   public final Setting<Boolean> fastFall = this.register(
      new Setting<>(
         "FastFall",
         true,
         v -> this.mode.getValue() != Speed.Mode.VULCANGROUND && this.mode.getValue() != Speed.Mode.LEGIT && this.mode.getValue() != Speed.Mode.CUBECRAFT
      )
   );
   public final Setting<Integer> airTicksMin = this.register(
      new Setting<>(
         "AirTicksMin",
         3,
         0,
         20,
         v -> this.mode.getValue() != Speed.Mode.VULCANGROUND
            && this.mode.getValue() != Speed.Mode.LEGIT
            && this.mode.getValue() != Speed.Mode.CUBECRAFT
            && this.fastFall.getValue()
      )
   );
   public final Setting<Speed.StrafeType> strafeType = this.register(
      new Setting<>("StrafeType", Speed.StrafeType.SEMI, v -> this.mode.getValue() == Speed.Mode.VULCAN || this.mode.getValue() == Speed.Mode.GROUND)
   );
   public final Setting<Double> semiAccel = this.register(
      new Setting<>(
         "SemiAccel",
         0.3,
         0.05,
         1.0,
         v -> (this.mode.getValue() == Speed.Mode.VULCAN || this.mode.getValue() == Speed.Mode.GROUND) && this.strafeType.getValue() == Speed.StrafeType.SEMI
      )
   );
   public final Setting<Double> mediumAccel = this.register(
      new Setting<>(
         "MediumAccel",
         0.5,
         0.2,
         0.9,
         v -> (this.mode.getValue() == Speed.Mode.VULCAN || this.mode.getValue() == Speed.Mode.GROUND) && this.strafeType.getValue() == Speed.StrafeType.MEDIUM
      )
   );
   public final Setting<Double> mediumSpeed = this.register(
      new Setting<>(
         "MediumSpeed",
         0.35,
         0.2,
         0.5,
         v -> (this.mode.getValue() == Speed.Mode.VULCAN || this.mode.getValue() == Speed.Mode.GROUND) && this.strafeType.getValue() == Speed.StrafeType.MEDIUM
      )
   );
   public final Setting<Double> adaptiveAccel = this.register(
      new Setting<>(
         "AdaptiveAccel",
         0.3,
         0.05,
         1.0,
         v -> (this.mode.getValue() == Speed.Mode.VULCAN || this.mode.getValue() == Speed.Mode.GROUND)
            && this.strafeType.getValue() == Speed.StrafeType.ADAPTIVE
      )
   );
   public final Setting<Double> adaptiveSpeed = this.register(
      new Setting<>(
         "AdaptiveSpeed",
         0.3,
         0.1,
         0.5,
         v -> (this.mode.getValue() == Speed.Mode.VULCAN || this.mode.getValue() == Speed.Mode.GROUND)
            && this.strafeType.getValue() == Speed.StrafeType.ADAPTIVE
      )
   );
   public final Setting<Double> vgSpeed = this.register(new Setting<>("VGSpeed", 0.31, 0.28, 0.32, v -> this.mode.getValue() == Speed.Mode.VULCANGROUND));
   public final Setting<Double> vgAccel = this.register(new Setting<>("VGAccel", 0.028, 0.02, 0.04, v -> this.mode.getValue() == Speed.Mode.VULCANGROUND));
   public final Setting<Double> testSpeed = this.register(new Setting<>("TestSpeed", 0.4, 0.2, 0.6, v -> this.mode.getValue() == Speed.Mode.TEST));
   public final Setting<Double> testAccel = this.register(new Setting<>("TestAccel", 0.6, 0.3, 0.9, v -> this.mode.getValue() == Speed.Mode.TEST));
   public final Setting<Boolean> testWobble = this.register(new Setting<>("TestWobble", true, v -> this.mode.getValue() == Speed.Mode.TEST));
   public final Setting<Double> testWobbleStr = this.register(
      new Setting<>("TestWobbleStr", 0.1, 0.02, 0.3, v -> this.mode.getValue() == Speed.Mode.TEST && this.testWobble.getValue())
   );
   public final Setting<Boolean> fortyFive = this.register(new Setting<>("45Mode", false, v -> this.mode.getValue() == Speed.Mode.LEGIT));
   private int airTicks = 0;
   private final Vulcan2Movement vulcan2Movement = new Vulcan2Movement();
   private int vulcan2LastTick = Integer.MIN_VALUE;
   private Object vulcan2Player;
   private boolean wasOnGround = true;
   private double wobblePhase = 0.0;
   private double vgPrevSpeed = 0.0;
   private boolean vgGroundToggle = false;
   private boolean vgBypass = false;
   private static final double CC_VANILLA_SPRINT = 0.2873;

   public Speed() {
      super("Speed", "Epic bypass", Module.Category.MOVEMENT);
      this.lowHopHeight.setVisibility(v -> this.mode.getValue() != Mode.VULCAN2 && this.lowHop.getValue());
      this.speed.setVisibility(v -> this.mode.getValue() != Mode.VULCAN2 && this.mode.getValue() != Mode.VULCANGROUND
         && this.mode.getValue() != Mode.LEGIT && this.mode.getValue() != Mode.CUBECRAFT);
      this.jump.setVisibility(v -> this.speed.isVisible());
      this.lowHop.setVisibility(v -> this.speed.isVisible());
      this.damageBoost.setVisibility(v -> this.mode.getValue() != Mode.VULCAN2
         && this.mode.getValue() != Mode.LEGIT && this.mode.getValue() != Mode.CUBECRAFT);
      this.boostFactor.setVisibility(v -> this.speed.isVisible() && this.damageBoost.getValue());
      this.potionSync.setVisibility(v -> this.speed.isVisible());
      this.fastFall.setVisibility(v -> this.speed.isVisible());
      this.airTicksMin.setVisibility(v -> this.speed.isVisible() && this.fastFall.getValue());
   }

   @Override
   public void onEnable() {
      this.resetVulcan2();
      this.airTicks = 0;
      this.wasOnGround = true;
      this.wobblePhase = 0.0;
      this.vgPrevSpeed = 0.0;
      this.vgGroundToggle = false;
      this.vgBypass = false;
   }

   @Override
   public void onTick() {
      // Vulcan2 is applied immediately before physical movement.
      if (this.mode.getValue() == Mode.VULCAN2) {
         return;
      }
      if (!nullCheck()) {
         if (mc.player.onGround()) {
            this.airTicks = 0;
         } else {
            this.airTicks++;
         }

         if (this.mode.getValue() == Speed.Mode.LEGIT) {
            this.handleLegitMode();
         } else if (mc.player.xxa != 0.0F || mc.player.zza != 0.0F) {
            double currentSpeed = this.speed.getValue();
            if (this.mode.getValue() != Speed.Mode.CUBECRAFT) {
               if (this.damageBoost.getValue() && mc.player.hurtTime > 0) {
                  currentSpeed *= this.boostFactor.getValue();
               }

               if (this.potionSync.getValue() && mc.player.hasEffect(MobEffects.SPEED)) {
                  int amp = mc.player.getEffect(MobEffects.SPEED).getAmplifier();
                  currentSpeed *= 1.0 + 0.2 * (amp + 1);
               }
            }

            switch ((Speed.Mode)this.mode.getValue()) {
               case STRAFE:
                  this.handleStrafeMode(currentSpeed);
                  break;
               case VANILLA:
                  this.handleVanillaMode(currentSpeed);
                  break;
               case VULCAN:
                  this.handleVulcanMode(currentSpeed);
                  break;
               case GROUND:
                  this.handleGroundMode(currentSpeed);
                  break;
               case VULCANGROUND:
                  this.handleVulcanGroundMode();
                  break;
               case TEST:
                  this.handleTestMode(currentSpeed);
                  break;
               case NCP:
                  this.handleNcpMode();
                  break;
               case CUBECRAFT:
                  this.handleCubeCraftMode();
               case LEGIT:
               default:
                  break;
               case HIGHHOP:
                  this.handleHighHopMode(currentSpeed);
            }

            Speed.Mode m = this.mode.getValue();
            if (m != Speed.Mode.VULCANGROUND && m != Speed.Mode.LEGIT) {
               boolean ffEnabled = m == Speed.Mode.CUBECRAFT || this.fastFall.getValue();
               if (ffEnabled && !mc.player.onGround() && this.airTicks >= this.airTicksMin.getValue() && mc.player.getDeltaMovement().y <= 0.0) {
                  mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -0.5, mc.player.getDeltaMovement().z);
                  mc.player.fallDistance = 0.0;
               }
            }
         }
      }
   }

   /** Runs once per keyboard tick, after movement correction and before physics. */
   public void applyVulcan2Input(ClientInput input) {
      if (!this.isEnabled() || this.mode.getValue() != Mode.VULCAN2 || nullCheck()) {
         this.resetVulcan2();
         return;
      }
      if (mc.player.isInWater() || mc.player.isInLava() || mc.player.onClimbable()
         || mc.player.isPassenger() || mc.player.isFallFlying() || mc.player.getAbilities().flying) {
         this.resetVulcan2();
         return;
      }

      Input keys = input.keyPresses;
      // The preset supplies its own launch; ordinary jump input stays blocked.
      input.keyPresses = new Input(keys.forward(), keys.backward(), keys.left(), keys.right(),
         false, keys.shift(), keys.sprint());
   }

   @Override
   public void onDisable() {
      this.resetVulcan2();
   }

   private void resetVulcan2() {
      this.vulcan2Movement.reset();
      this.vulcan2LastTick = Integer.MIN_VALUE;
      this.vulcan2Player = null;
   }

   public void applyVulcan2Motion() {
      if (!this.isEnabled() || this.mode.getValue() != Mode.VULCAN2 || nullCheck()) {
         this.resetVulcan2();
         return;
      }
      if (mc.player.isInWater() || mc.player.isInLava() || mc.player.onClimbable()
         || mc.player.isPassenger() || mc.player.isFallFlying() || mc.player.getAbilities().flying) {
         this.resetVulcan2();
         return;
      }
      if (this.vulcan2Player != mc.player) {
         this.resetVulcan2();
         this.vulcan2Player = mc.player;
      }
      if (this.vulcan2LastTick == mc.player.tickCount) {
         return;
      }
      this.vulcan2LastTick = mc.player.tickCount;
      var input = mc.player.input.getMoveVector();
      if (mc.player.onGround() && input.lengthSquared() > 1.0E-6F) {
         OyVey.TIMER = 1.0F;
      }
      mc.player.setDeltaMovement(this.vulcan2Movement.step(mc.player.getDeltaMovement(),
         mc.player.onGround(), mc.player.hurtTime > 0, mc.player.getYRot(), input.x, input.y));
   }

   private void handleStrafeMode(double speed) {
      if (this.jump.getValue() && mc.player.onGround()) {
         this.applyJump();
      }

      if (!mc.player.onGround()) {
         this.doStrafe(speed);
      }
   }

   private void handleVanillaMode(double speed) {
      if (this.jump.getValue() && mc.player.onGround()) {
         this.applyJump();
      }

      this.doStrafe(speed);
   }

   private void handleGroundMode(double speed) {
      boolean nearGround = !mc.player.onGround() && mc.player.getDeltaMovement().y <= 0.0;
      if (this.jump.getValue() && mc.player.onGround()) {
         this.applyJump();
      }

      if (this.strafeType.getValue() == Speed.StrafeType.ADAPTIVE && nearGround) {
         this.doAdaptiveStrafe(speed);
      } else if (this.strafeType.getValue() == Speed.StrafeType.MEDIUM) {
         this.doMediumStrafe(speed);
      } else if (this.strafeType.getValue() == Speed.StrafeType.SEMI) {
         this.doSemiStrafe(speed);
      } else {
         this.doStrafe(speed);
      }
   }

   private void handleNcpMode() {
      boolean moving = mc.player.xxa != 0.0F || mc.player.zza != 0.0F;
      boolean onGround = mc.player.onGround();
      if (onGround) {
         if (moving) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.401, mc.player.getDeltaMovement().z);
            this.doStrafe(0.287);
         }
      } else if (moving) {
         if (mc.player.getDeltaMovement().y < 0.0) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -0.49, mc.player.getDeltaMovement().z);
            mc.player.fallDistance = 0.0;
         }

         this.doStrafe(this.airTicks == 1 ? 0.45 : 0.287);
      }
   }

   private void handleCubeCraftMode() {
      if (mc.player.onGround()) {
         mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.401, mc.player.getDeltaMovement().z);
      } else {
         this.doStrafe(0.2873);
      }
   }

   private void handleTestMode(double speed) {
      double curY = mc.player.getDeltaMovement().y;
      if (!mc.player.onGround() && this.testWobble.getValue() && curY < 0.0) {
         this.wobblePhase += 0.3;
         double wobble = Math.sin(this.wobblePhase) * this.testWobbleStr.getValue();
         mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, curY + wobble, mc.player.getDeltaMovement().z);
      } else if (mc.player.onGround()) {
         this.wobblePhase = 0.0;
      }

      this.doSemiStrafe(speed);
   }

   private void handleVulcanMode(double speed) {
      if (mc.player.onGround()) {
         this.wasOnGround = true;
      } else {
         this.wasOnGround = false;
      }

      if (this.jump.getValue() && mc.player.onGround()) {
         this.applyJump();
      }

      boolean applyStrafe = !this.vulcanGroundOnly.getValue() || mc.player.onGround();
      if (applyStrafe) {
         if (this.strafeType.getValue() == Speed.StrafeType.SEMI) {
            this.doSemiStrafe(speed);
         } else {
            this.doStrafe(speed);
         }
      }
   }

   private void handleVulcanGroundMode() {
      mc.player.setSprinting(true);
      this.doStrafe(this.vgSpeed.getValue());
   }

   @Subscribe
   public void onPacketSend(PacketEvent.Send event) {
      if (this.mode.getValue() == Speed.Mode.VULCANGROUND && mc.player != null && !this.vgBypass && event.getPacket() instanceof ServerboundMovePlayerPacket) {
         event.cancel();
         this.vgGroundToggle = !this.vgGroundToggle;
         this.vgBypass = true;
         mc.player
            .connection
            .send(
               new PosRot(
                  mc.player.getX(),
                  mc.player.getY(),
                  mc.player.getZ(),
                  mc.player.getYRot(),
                  mc.player.getXRot(),
                  this.vgGroundToggle,
                  mc.player.horizontalCollision
               )
            );
         this.vgBypass = false;
      }
   }

   private void handleLegitMode() {
      if (mc.player.onGround()) {
         mc.player.jumpFromGround();
      }

      if (this.fortyFive.getValue()) {
         mc.player.setYRot(mc.player.getYRot() + 45.0F);
         mc.player.yHeadRot = mc.player.getYRot();
      }
   }

   private void handleHighHopMode(double spd) {
      if (mc.player.onGround()) {
         double v0 = hopVelocityForHeight(this.hhHeight.getValue());
         mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, v0, mc.player.getDeltaMovement().z);
      }

      this.doStrafe(spd);
   }

   private static double hopVelocityForHeight(double blocks) {
      double lo = 0.0;
      double hi = 5.0;

      for (int i = 0; i < 64; i++) {
         double mid = (lo + hi) / 2.0;
         if (simulateJumpHeight(mid) < blocks) {
            lo = mid;
         } else {
            hi = mid;
         }
      }

      return (lo + hi) / 2.0;
   }

   private static double simulateJumpHeight(double v0) {
      double vy = v0;
      double height = 0.0;

      while (vy > 0.0) {
         height += vy;
         vy = (vy - 0.08) * 0.98;
      }

      return height;
   }

   private void doStrafe(double speed) {
      float yaw = mc.player.getYRot();
      float forward = mc.player.zza;
      float strafe = mc.player.xxa;
      if (forward == 0.0F && strafe == 0.0F) {
         mc.player.setDeltaMovement(0.0, mc.player.getDeltaMovement().y, 0.0);
      } else {
         if (forward != 0.0F) {
            if (strafe > 0.0F) {
               yaw += forward > 0.0F ? -45 : 45;
            } else if (strafe < 0.0F) {
               yaw += forward > 0.0F ? 45 : -45;
            }

            strafe = 0.0F;
            forward = forward > 0.0F ? 1.0F : -1.0F;
         }

         double cos = Math.cos(Math.toRadians(yaw + 90.0));
         double sin = Math.sin(Math.toRadians(yaw + 90.0));
         mc.player.setDeltaMovement(forward * speed * cos + strafe * speed * sin, mc.player.getDeltaMovement().y, forward * speed * sin - strafe * speed * cos);
      }
   }

   private void doSemiStrafe(double maxSpeed) {
      float yaw = mc.player.getYRot();
      float forward = mc.player.zza;
      float strafe = mc.player.xxa;
      if (forward != 0.0F || strafe != 0.0F) {
         if (forward != 0.0F) {
            if (strafe > 0.0F) {
               yaw += forward > 0.0F ? -45 : 45;
            } else if (strafe < 0.0F) {
               yaw += forward > 0.0F ? 45 : -45;
            }

            strafe = 0.0F;
            forward = forward > 0.0F ? 1.0F : -1.0F;
         }

         double cos = Math.cos(Math.toRadians(yaw + 90.0));
         double sin = Math.sin(Math.toRadians(yaw + 90.0));
         double targetX = forward * maxSpeed * cos + strafe * maxSpeed * sin;
         double targetZ = forward * maxSpeed * sin - strafe * maxSpeed * cos;
         double curX = mc.player.getDeltaMovement().x;
         double curZ = mc.player.getDeltaMovement().z;
         double accel = this.semiAccel.getValue();
         double newX = curX + (targetX - curX) * accel;
         double newZ = curZ + (targetZ - curZ) * accel;
         double hs = Math.sqrt(newX * newX + newZ * newZ);
         if (hs > 0.3) {
            double scale = 0.3 / hs;
            newX *= scale;
            newZ *= scale;
         }

         mc.player.setDeltaMovement(newX, mc.player.getDeltaMovement().y, newZ);
      }
   }

   private void doAdaptiveStrafe(double maxSpeed) {
      float yaw = mc.player.getYRot();
      float forward = mc.player.zza;
      float strafe = mc.player.xxa;
      if (forward != 0.0F || strafe != 0.0F) {
         if (forward != 0.0F) {
            if (strafe > 0.0F) {
               yaw += forward > 0.0F ? -45 : 45;
            } else if (strafe < 0.0F) {
               yaw += forward > 0.0F ? 45 : -45;
            }

            strafe = 0.0F;
            forward = forward > 0.0F ? 1.0F : -1.0F;
         }

         double cos = Math.cos(Math.toRadians(yaw + 90.0));
         double sin = Math.sin(Math.toRadians(yaw + 90.0));
         double targetX = forward * maxSpeed * cos + strafe * maxSpeed * sin;
         double targetZ = forward * maxSpeed * sin - strafe * maxSpeed * cos;
         double curX = mc.player.getDeltaMovement().x;
         double curZ = mc.player.getDeltaMovement().z;
         double accel = this.adaptiveAccel.getValue();
         double newX = curX + (targetX - curX) * accel;
         double newZ = curZ + (targetZ - curZ) * accel;
         double speedCap = this.adaptiveSpeed.getValue();
         double hs = Math.sqrt(newX * newX + newZ * newZ);
         if (hs > speedCap) {
            double scale = speedCap / hs;
            newX *= scale;
            newZ *= scale;
         }

         mc.player.setDeltaMovement(newX, mc.player.getDeltaMovement().y, newZ);
      }
   }

   private void doMediumStrafe(double maxSpeed) {
      float yaw = mc.player.getYRot();
      float forward = mc.player.zza;
      float strafe = mc.player.xxa;
      if (forward != 0.0F || strafe != 0.0F) {
         if (forward != 0.0F) {
            if (strafe > 0.0F) {
               yaw += forward > 0.0F ? -45 : 45;
            } else if (strafe < 0.0F) {
               yaw += forward > 0.0F ? 45 : -45;
            }

            strafe = 0.0F;
            forward = forward > 0.0F ? 1.0F : -1.0F;
         }

         double cos = Math.cos(Math.toRadians(yaw + 90.0));
         double sin = Math.sin(Math.toRadians(yaw + 90.0));
         double targetX = forward * maxSpeed * cos + strafe * maxSpeed * sin;
         double targetZ = forward * maxSpeed * sin - strafe * maxSpeed * cos;
         double curX = mc.player.getDeltaMovement().x;
         double curZ = mc.player.getDeltaMovement().z;
         double accel = this.mediumAccel.getValue();
         double newX = curX + (targetX - curX) * accel;
         double newZ = curZ + (targetZ - curZ) * accel;
         double speedCap = this.mediumSpeed.getValue();
         double hs = Math.sqrt(newX * newX + newZ * newZ);
         if (hs > speedCap) {
            double scale = speedCap / hs;
            newX *= scale;
            newZ *= scale;
         }

         mc.player.setDeltaMovement(newX, mc.player.getDeltaMovement().y, newZ);
      }
   }

   private void applyJump() {
      if (this.lowHop.getValue()) {
         mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, this.lowHopHeight.getValue(), mc.player.getDeltaMovement().z);
      } else {
         mc.player.jumpFromGround();
      }
   }

   public enum Mode {
      STRAFE,
      VANILLA,
      VULCAN,
      VULCAN2,
      GROUND,
      VULCANGROUND,
      TEST,
      NCP,
      CUBECRAFT,
      LEGIT,
      HIGHHOP;
   }

   public enum StrafeType {
      FULL,
      SEMI,
      MEDIUM,
      ADAPTIVE;
   }
}
