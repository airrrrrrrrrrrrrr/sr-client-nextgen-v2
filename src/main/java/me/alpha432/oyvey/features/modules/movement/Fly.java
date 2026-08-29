package me.alpha432.oyvey.features.modules.movement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;

public class Fly extends Module {
   public final Setting<Fly.Mode> mode = this.register(new Setting<>("Mode", Fly.Mode.VANILLA));
   public final Setting<Float> speed = this.register(
      new Setting<>(
         "Speed",
         0.2F,
         0.01F,
         5.0F,
         v -> this.mode.getValue() != Fly.Mode.JUMP
            && this.mode.getValue() != Fly.Mode.DEATHZONE
            && this.mode.getValue() != Fly.Mode.TEST
            && this.mode.getValue() != Fly.Mode.GHOSTLY
      )
   );
   public final Setting<Float> boost = this.register(new Setting<>("Boost", 0.42F, 0.1F, 5.0F, v -> this.mode.getValue() == Fly.Mode.JUMP));
   private final Setting<Float> dzSpeed = this.register(new Setting<>("DZ Speed", 0.35F, 0.1F, 5.0F, v -> this.mode.getValue() == Fly.Mode.DEATHZONE));
   private final Setting<Float> dzTimer = this.register(new Setting<>("DZ Timer", 0.05F, 0.01F, 0.15F, v -> this.mode.getValue() == Fly.Mode.DEATHZONE));
   private final Setting<Float> testSpeed = this.register(new Setting<>("Test Speed", 0.35F, 0.1F, 5.0F, v -> this.mode.getValue() == Fly.Mode.TEST));
   private final Setting<Float> ghostlySpeed = this.register(new Setting<>("Ghostly Speed", 0.5F, 0.1F, 5.0F, v -> this.mode.getValue() == Fly.Mode.GHOSTLY));
   private final Setting<Integer> ghostlyDepth = this.register(new Setting<>("Clip Depth", 3, 1, 10, v -> this.mode.getValue() == Fly.Mode.GHOSTLY));
   private final Setting<Float> mospixelSpeed = this.register(
      new Setting<>("Mospixel Speed", 0.287F, 0.1F, 5.0F, v -> this.mode.getValue() == Fly.Mode.MOSPIXEL)
   );
   private final Setting<Boolean> stopMotion = this.register(new Setting<>("StopMotion", false, v -> this.mode.getValue() == Fly.Mode.MOSPIXEL2));
   private final Setting<Fly.Mp3Mode> mp3Mode = this.register(new Setting<>("MP3Mode", Fly.Mp3Mode.FAST, v -> this.mode.getValue() == Fly.Mode.MOSPIXEL3));
   private final Setting<Float> mp3Speed = this.register(
      new Setting<>(
         "MP3Speed",
         0.3F,
         0.0F,
         1.0F,
         v -> this.mode.getValue() == Fly.Mode.MOSPIXEL3 && this.mp3Mode.getValue() != Fly.Mp3Mode.BASIC && this.mp3Mode.getValue() != Fly.Mp3Mode.NODMG
      )
   );
   private final Setting<Boolean> mp3NoCollide = this.register(new Setting<>("MP3NoCollide", true, v -> this.mode.getValue() == Fly.Mode.MOSPIXEL3));
   private final Setting<Boolean> capEnabled = this.register(new Setting<>("SpeedCap", false));
   private final Setting<Float> capSpeed = this.register(new Setting<>("MaxSpeed", 2.0F, 0.05F, 10.0F, v -> this.capEnabled.getValue()));
   private final Setting<Boolean> antiKick = this.register(new Setting<>("AntiKick", false, v -> this.mode.getValue() == Fly.Mode.VANILLA));
   private final Setting<Float> glideSpeed = this.register(
      new Setting<>("GlideSpeed", 0.03F, 0.001F, 0.5F, v -> this.mode.getValue() == Fly.Mode.VANILLA && this.antiKick.getValue())
   );
   private boolean jumpKeyWasDown = false;
   private int ncpPhase = 0;
   private boolean ncpJumped = false;
   private int mospixelTicks = 0;
   private double mospixel2Boost = 0.0;
   private int mospixel2Flew = 100;
   private double mp3FlySpeed = 0.0;
   private boolean mp3Grounded = false;
   private int mp3AirTicks = 0;
   private volatile Fly.Phase phase = Fly.Phase.IDLE;
   private boolean bypass = false;
   private final List<Packet<?>> blinkBuffer = new ArrayList<>();
   private boolean pendingTestMode = false;

   public Fly() {
      super("Fly", "Fly through the air.", Module.Category.MOVEMENT);
   }

   @Override
   public void onEnable() {
      this.ncpPhase = 0;
      this.ncpJumped = false;
      this.jumpKeyWasDown = false;
      this.phase = Fly.Phase.IDLE;
      this.blinkBuffer.clear();
      switch ((Fly.Mode)this.mode.getValue()) {
         case DEATHZONE:
            this.initDamageSequence(false);
            break;
         case TEST:
            this.initDamageSequence(true);
            break;
         case MOSPIXEL:
            this.mospixelTicks = 0;
            break;
         case MOSPIXEL2:
            this.mospixel2Boost = 0.0;
            this.mospixel2Flew = 25;
            if (mc.player != null && mc.player.onGround()) {
               this.flyStrafe(0.3F);
               mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.42, mc.player.getDeltaMovement().z);
               this.mospixel2Boost = 17.8;
               this.mospixel2Flew = 0;
            }
            break;
         case MOSPIXEL3:
            this.mp3FlySpeed = 0.0;
            this.mp3AirTicks = 0;
            this.mp3Grounded = mc.player != null && mc.player.onGround();
            break;
         case GHOSTLY:
            double gx = mc.player.getX();
            double gy = mc.player.getY();
            double gz = mc.player.getZ();
            boolean bl = mc.player.horizontalCollision;
            int depth = this.ghostlyDepth.getValue();
            double step = depth / 6.0;

            for (int i = 1; i <= 6; i++) {
               this.sendNoEvent(new Pos(gx, gy - step * i, gz, i == 6, bl));
            }
      }
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         mc.player.getAbilities().flying = false;
         mc.player.fallDistance = 0.0;
         this.ncpPhase = 0;
         this.ncpJumped = false;
         if (this.mode.getValue() == Fly.Mode.MOSPIXEL2 && this.stopMotion.getValue()) {
            mc.player.setDeltaMovement(0.0, 0.0, 0.0);
            this.flyStrafe(0.2F);
         }

         if (this.mode.getValue() == Fly.Mode.MOSPIXEL3) {
            Vec3 mot = mc.player.getDeltaMovement();
            double spd = Math.sqrt(mot.x * mot.x + mot.z * mot.z);
            this.flyStrafe((float)(spd * 0.7));
            this.mp3FlySpeed = 0.0;
            this.mp3AirTicks = 0;
            this.mp3Grounded = false;
         }

         if (!this.blinkBuffer.isEmpty()) {
            for (Packet<?> pkt : this.blinkBuffer) {
               mc.player.connection.send(pkt);
            }

            this.blinkBuffer.clear();
         }

         this.phase = Fly.Phase.IDLE;
      }
   }

   @Override
   public void onTick() {
      if (mc.player != null) {
         switch ((Fly.Mode)this.mode.getValue()) {
            case JUMP:
               this.handleJump();
               break;
            case VANILLA:
               this.handleVanilla();
               break;
            case NCP:
               this.handleNcp();
               break;
            case VULCAN:
               this.handleVulcan();
               break;
            case VULCAN_OLD:
               this.handleVulcanOld();
               break;
            case VULCAN_GHOST:
               this.handleVulcanGhost();
               break;
            case DEATHZONE:
               this.handleDeathZone();
               break;
            case TEST:
               this.handleTest();
               break;
            case MOSPIXEL:
               this.handleMospixel();
               break;
            case MOSPIXEL2:
               this.handleMospixel2();
               break;
            case MOSPIXEL3:
               this.handleMospixel3();
               break;
            case GHOSTLY:
               this.handleGhostly();
         }

         if (this.capEnabled.getValue()) {
            Vec3 mot = mc.player.getDeltaMovement();
            double hs = Math.sqrt(mot.x * mot.x + mot.z * mot.z);
            if (hs > this.capSpeed.getValue().floatValue()) {
               double scale = this.capSpeed.getValue().floatValue() / hs;
               mc.player.setDeltaMovement(mot.x * scale, mot.y, mot.z * scale);
            }
         }
      }
   }

   private void handleJump() {
      boolean jumpDown = mc.options.keyJump.isDown();
      boolean justPressed = jumpDown && !this.jumpKeyWasDown;
      this.jumpKeyWasDown = jumpDown;
      if (mc.player.onGround() || justPressed) {
         mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, this.boost.getValue().floatValue(), mc.player.getDeltaMovement().z);
         mc.player.fallDistance = 0.0;
      }
   }

   private void handleVanilla() {
      mc.player.getAbilities().flying = true;
      mc.player.getAbilities().setFlyingSpeed(0.0F);
      mc.player.fallDistance = 0.0;
      float spd = this.speed.getValue();
      float yaw = mc.player.getYRot();
      float fwd = mc.player.zza;
      float str = mc.player.xxa;
      double dx = 0.0;
      double dz = 0.0;
      if (fwd != 0.0F || str != 0.0F) {
         double cos = Math.cos(Math.toRadians(yaw + 90.0));
         double sin = Math.sin(Math.toRadians(yaw + 90.0));
         dx = fwd * cos + str * sin;
         dz = fwd * sin - str * cos;
         double len = Math.sqrt(dx * dx + dz * dz);
         if (len > 0.0) {
            dx /= len;
            dz /= len;
         }

         dx *= spd;
         dz *= spd;
      }

      double dy = 0.0;
      if (mc.options.keyJump.isDown()) {
         dy = spd;
      } else if (mc.options.keyShift.isDown()) {
         dy = -spd;
      } else if (this.antiKick.getValue() && !mc.player.onGround()) {
         dy = -this.glideSpeed.getValue();
      }

      mc.player.setDeltaMovement(dx, dy, dz);
   }

   private void handleNcp() {
      Vec3 mot = mc.player.getDeltaMovement();
      if (mc.player.onGround()) {
         mc.player.setDeltaMovement(mot.x, 0.42, mot.z);
         this.ncpPhase = 0;
         this.ncpJumped = true;
         mc.player.fallDistance = 0.0;
      } else if (!this.ncpJumped) {
         mc.player.setDeltaMovement(mot.x, 0.42, mot.z);
         this.ncpJumped = true;
         this.ncpPhase = 0;
      } else {
         this.ncpPhase++;
         double yVel;
         if (this.ncpPhase <= 6) {
            yVel = mot.y - 0.42;
         } else if (this.ncpPhase <= 20) {
            yVel = -0.015;
         } else {
            yVel = mot.y - 0.08;
         }

         mc.player.setDeltaMovement(mot.x, yVel, mot.z);
         mc.player.fallDistance = 0.0;
      }
   }

   private void handleVulcan() {
      if (!mc.player.onGround() && !(mc.player.fallDistance <= 0.0)) {
         Vec3 mot = mc.player.getDeltaMovement();
         double y = mc.player.tickCount % 2 == 0 ? -0.155 : -0.1;
         mc.player.setDeltaMovement(mot.x, y, mot.z);
      }
   }

   private void handleVulcanOld() {
      if (!mc.player.onGround() && !(mc.player.fallDistance <= 0.0)) {
         Vec3 mot = mc.player.getDeltaMovement();
         double y = mc.player.tickCount % 2 == 0 ? -0.1 : -0.16;
         mc.player.setDeltaMovement(mot.x, y, mot.z);
      }
   }

   private void handleVulcanGhost() {
      mc.player.getAbilities().flying = true;
      mc.player.getAbilities().setFlyingSpeed(this.speed.getValue() * 0.1F);
      mc.player.fallDistance = 0.0;
   }

   private void handleDeathZone() {
      if (this.phase == Fly.Phase.FIRST_WAIT || this.phase == Fly.Phase.SECOND_WAIT) {
         mc.player.setDeltaMovement(0.0, 0.0, 0.0);
         mc.player.fallDistance = 0.0;
      } else if (this.phase == Fly.Phase.FLYING) {
         mc.player.getAbilities().flying = true;
         mc.player.getAbilities().setFlyingSpeed(this.dzSpeed.getValue() * 0.1F);
         mc.player.fallDistance = 0.0;
      }
   }

   private void handleTest() {
      if (this.phase == Fly.Phase.FIRST_WAIT || this.phase == Fly.Phase.SECOND_WAIT) {
         mc.player.setDeltaMovement(0.0, 0.0, 0.0);
         mc.player.fallDistance = 0.0;
      } else if (this.phase == Fly.Phase.BLINKING) {
         mc.player.getAbilities().flying = true;
         mc.player.getAbilities().setFlyingSpeed(this.testSpeed.getValue() * 0.1F);
         mc.player.fallDistance = 0.0;
      }
   }

   private void handleGhostly() {
      mc.player.getAbilities().flying = true;
      mc.player.getAbilities().setFlyingSpeed(this.ghostlySpeed.getValue() * 0.1F);
      mc.player.fallDistance = 0.0;
   }

   private void handleMospixel() {
      mc.player.fallDistance = 0.0;
      mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.0, mc.player.getDeltaMovement().z);
      if (++this.mospixelTicks >= 2) {
         mc.player.setPos(mc.player.getX(), mc.player.getY() + 1.0E-5, mc.player.getZ());
         this.mospixelTicks = 0;
      }

      if (mc.player.zza == 0.0F && mc.player.xxa == 0.0F) {
         mc.player.setDeltaMovement(0.0, 0.0, 0.0);
      } else {
         this.flyStrafe(this.mospixelSpeed.getValue());
      }
   }

   private void handleMospixel2() {
      this.mospixel2Flew++;
      if (mc.player.onGround() && mc.options.keyJump.isDown()) {
         if (this.mospixel2Flew >= 25) {
            this.flyStrafe(0.3F);
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.42, mc.player.getDeltaMovement().z);
            this.mospixel2Flew = 0;
            this.mospixel2Boost = 19.0;
         } else {
            mc.player.setDeltaMovement(0.0, mc.player.getDeltaMovement().y, 0.0);
         }
      } else if (!mc.player.onGround()) {
         mc.player.fallDistance = 0.0;
         mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.0, mc.player.getDeltaMovement().z);
         double spd = 0.29;
         if (this.mospixel2Boost > 0.0) {
            boolean moving = mc.player.xxa != 0.0F || mc.player.zza != 0.0F;
            if (moving && !mc.player.horizontalCollision) {
               spd = 0.29 + this.mospixel2Boost / 18.0;
               this.mospixel2Boost -= 0.155;
            } else {
               this.mospixel2Boost = 0.0;
            }
         }

         this.flyStrafe((float)spd);
      }
   }

   private void handleMospixel3() {
      Fly.Mp3Mode m3 = this.mp3Mode.getValue();
      double minSpeed = m3 == Fly.Mp3Mode.BASIC ? 0.281 : 0.273;
      if (this.mp3Grounded) {
         double hBoost = m3 == Fly.Mp3Mode.FUNCRAFT ? 0.6 : 0.64;
         switch (m3) {
            case BASIC:
               this.mp3Grounded = false;
               this.mp3AirTicks = 0;
               break;
            case FAST:
            case FUNCRAFT:
               mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.42, mc.player.getDeltaMovement().z);
               this.flyStrafe((float)hBoost);
               this.mp3FlySpeed = 0.51 + this.mp3Speed.getValue().floatValue();
               this.mp3Grounded = false;
               this.mp3AirTicks = 0;
               break;
            case NODMG:
               mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.42, mc.player.getDeltaMovement().z);
               this.flyStrafe((float)hBoost);
               this.mp3FlySpeed = hBoost * 0.987;
               this.mp3Grounded = false;
               this.mp3AirTicks = 0;
         }
      } else {
         if (m3 == Fly.Mp3Mode.NODMG && this.mp3AirTicks > 20) {
            this.mp3FlySpeed = 0.0;
         }

         this.mp3FlySpeed = this.mp3FlySpeed * (0.99375 - this.mp3AirTicks * 1.0E-13);
         boolean moving = mc.player.xxa != 0.0F || mc.player.zza != 0.0F;
         if (!this.mp3NoCollide.getValue() && mc.player.horizontalCollision) {
            this.mp3FlySpeed = 0.0;
         }

         if (!moving) {
            this.mp3FlySpeed = 0.0;
         } else if (this.mp3FlySpeed < minSpeed) {
            this.mp3FlySpeed = minSpeed;
         }

         this.flyStrafe((float)this.mp3FlySpeed);
         if (!mc.player.onGround()) {
            this.mp3AirTicks++;
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.0, mc.player.getDeltaMovement().z);
            if (this.mp3AirTicks % 5 < 4) {
               mc.player.setPos(mc.player.getX(), mc.player.getY() + 1.0E-14, mc.player.getZ());
            }
         } else {
            this.mp3Grounded = true;
            this.mp3AirTicks = 0;
         }

         if (this.mp3NoCollide.getValue() && mc.player.horizontalCollision && moving) {
            Vec3 mot = mc.player.getDeltaMovement();
            if (Math.abs(mot.x) >= Math.abs(mot.z)) {
               double zSign = Math.signum(mot.z);
               mc.player.setDeltaMovement(0.0, mot.y, zSign != 0.0 ? zSign * this.mp3FlySpeed : this.mp3FlySpeed);
            } else {
               double xSign = Math.signum(mot.x);
               mc.player.setDeltaMovement(xSign != 0.0 ? xSign * this.mp3FlySpeed : this.mp3FlySpeed, mot.y, 0.0);
            }
         }

         if (mc.options.keyJump.isDown()) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.25, mc.player.getDeltaMovement().z);
         }
      }
   }

   private void flyStrafe(float spd) {
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
         mc.player.setDeltaMovement(forward * spd * cos + strafe * spd * sin, mc.player.getDeltaMovement().y, forward * spd * sin - strafe * spd * cos);
      }
   }

   private void initDamageSequence(boolean isTest) {
      double x = mc.player.getX();
      double y = mc.player.getY();
      double z = mc.player.getZ();
      boolean bl = mc.player.horizontalCollision;
      this.sendNoEvent(new Pos(x, y, z, true, bl));
      this.sendNoEvent(new Pos(x, y - 0.0784, z, false, bl));
      this.sendNoEvent(new Pos(x, y, z, true, bl));
      this.sendNoEvent(new Pos(x, y + 0.41999998688697815, z, false, bl));
      this.sendNoEvent(new Pos(x, y + 0.7531999805212, z, false, bl));
      this.sendNoEvent(new Pos(x, y + 1.0, z, true, bl));
      this.sendNoEvent(new Pos(x, y + 1.4199999868869781, z, false, bl));
      this.sendNoEvent(new Pos(x, y + 1.7531999805212, z, false, bl));
      this.sendNoEvent(new Pos(x, y + 2.0, z, true, bl));
      this.sendNoEvent(new Pos(x, y + 2.419999986886978, z, false, bl));
      this.sendNoEvent(new Pos(x, y + 2.7531999805212, z, false, bl));
      this.sendNoEvent(new Pos(x, y + 3.00133597911214, z, false, bl));
      this.pendingTestMode = isTest;
      this.phase = Fly.Phase.FIRST_WAIT;
   }

   private void sendNoEvent(Packet<?> packet) {
      this.bypass = true;
      mc.player.connection.send(packet);
      this.bypass = false;
   }

   @Subscribe
   private void onPacketReceive(PacketEvent.Receive event) {
      if (mc.player != null) {
         Fly.Mode m = this.mode.getValue();
         if (m != Fly.Mode.VULCAN_GHOST && m != Fly.Mode.GHOSTLY) {
            if (m == Fly.Mode.DEATHZONE && this.phase == Fly.Phase.FLYING) {
               if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
                  event.cancel();
               }
            } else if (m == Fly.Mode.TEST && this.phase == Fly.Phase.BLINKING) {
               if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
                  event.cancel();
               }
            } else if (m == Fly.Mode.MOSPIXEL3) {
               if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
                  mc.execute(this::disable);
               }
            } else if ((m == Fly.Mode.DEATHZONE || m == Fly.Mode.TEST)
               && event.getPacket() instanceof ClientboundPlayerPositionPacket pos
               && (this.phase == Fly.Phase.FIRST_WAIT || this.phase == Fly.Phase.SECOND_WAIT)) {
               event.cancel();
               int tid = pos.id();
               PositionMoveRotation change = pos.change();
               Set<Relative> relatives = pos.relatives();
               Fly.Phase p = this.phase;
               boolean toTest = this.pendingTestMode;
               mc.execute(() -> {
                  if (mc.player != null && (this.mode.getValue() == Fly.Mode.DEATHZONE || this.mode.getValue() == Fly.Mode.TEST)) {
                     Vec3 serverPos = PositionMoveRotation.calculateAbsolute(PositionMoveRotation.of(mc.player), change, relatives).position();
                     this.sendNoEvent(new ServerboundAcceptTeleportationPacket(tid));
                     this.sendNoEvent(new PosRot(serverPos.x, serverPos.y, serverPos.z, change.yRot(), change.xRot(), false, mc.player.horizontalCollision));
                     if (p == Fly.Phase.FIRST_WAIT) {
                        this.sendNoEvent(new Pos(serverPos.x, serverPos.y - 0.0784, serverPos.z, false, mc.player.horizontalCollision));
                        this.phase = Fly.Phase.SECOND_WAIT;
                     } else {
                        this.phase = toTest ? Fly.Phase.BLINKING : Fly.Phase.FLYING;
                     }
                  }
               });
            }
         } else if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            event.cancel();
         }
      }
   }

   @Subscribe
   private void onPacketSend(PacketEvent.Send event) {
      if (mc.player != null && !this.bypass) {
         Fly.Mode m = this.mode.getValue();
         if (m == Fly.Mode.DEATHZONE) {
            if ((this.phase == Fly.Phase.FIRST_WAIT || this.phase == Fly.Phase.SECOND_WAIT) && event.getPacket() instanceof ServerboundMovePlayerPacket) {
               event.cancel();
            }
         } else {
            if (m == Fly.Mode.TEST) {
               if (this.phase != Fly.Phase.FIRST_WAIT && this.phase != Fly.Phase.SECOND_WAIT) {
                  if (this.phase == Fly.Phase.BLINKING && event.getPacket() instanceof ServerboundMovePlayerPacket) {
                     this.blinkBuffer.add(event.getPacket());
                     event.cancel();
                  }
               } else if (event.getPacket() instanceof ServerboundMovePlayerPacket) {
                  event.cancel();
               }
            }

            if ((m == Fly.Mode.MOSPIXEL || m == Fly.Mode.MOSPIXEL2 || m == Fly.Mode.MOSPIXEL3)
               && event.getPacket() instanceof ServerboundMovePlayerPacket pkt
               && pkt.isOnGround()) {
               event.cancel();
               this.bypass = true;
               mc.player
                  .connection
                  .send(
                     new PosRot(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), false, mc.player.horizontalCollision
                     )
                  );
               this.bypass = false;
            }
         }
      }
   }

   @Override
   public String getDisplayInfo() {
      return this.mode.getValue().name();
   }

   public enum Mode {
      JUMP,
      VANILLA,
      NCP,
      VULCAN,
      VULCAN_OLD,
      VULCAN_GHOST,
      DEATHZONE,
      TEST,
      MOSPIXEL,
      MOSPIXEL2,
      MOSPIXEL3,
      GHOSTLY;
   }

   public enum Mp3Mode {
      BASIC,
      FAST,
      NODMG,
      FUNCRAFT;
   }

   private enum Phase {
      IDLE,
      FIRST_WAIT,
      SECOND_WAIT,
      FLYING,
      BLINKING;
   }
}
