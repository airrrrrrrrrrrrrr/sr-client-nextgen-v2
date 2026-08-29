package me.alpha432.oyvey.features.modules.misc;

import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

public class Disabler extends Module {
   public final Setting<Boolean> cancelKeepAlive = this.register(new Setting<>("CancelKeepAlive", false));
   public final Setting<Boolean> cancelSwing = this.register(new Setting<>("CancelSwing", false));
   public final Setting<Boolean> cancelEntityAction = this.register(new Setting<>("CancelEntityAction", false));
   public final Setting<Boolean> cancelPlayerAction = this.register(new Setting<>("CancelPlayerAction", false));
   public final Setting<Boolean> cancelAbilities = this.register(new Setting<>("CancelAbilities", false));
   public final Setting<Boolean> cancelMove = this.register(new Setting<>("CancelMove", false));
   public final Setting<Boolean> onlyWhenStill = this.register(new Setting<>("OnlyWhenStill", true, v -> this.cancelMove.getValue()));
   public final Setting<Boolean> noRotation = this.register(new Setting<>("NoRotation", false));
   public final Setting<Disabler.RotationMode> rotMode = this.register(
      new Setting<>("RotationMode", Disabler.RotationMode.ZERO, v -> this.noRotation.getValue())
   );
   public final Setting<Float> rotOffset = this.register(
      new Setting<>("RotOffset", 6.0F, -180.0F, 180.0F, v -> this.noRotation.getValue() && this.rotMode.getValue() == Disabler.RotationMode.OFFSET_YAW)
   );
   public final Setting<Boolean> watchdogMotion = this.register(new Setting<>("WatchdogMotion", false));
   public final Setting<Boolean> vulcan = this.register(new Setting<>("Vulcan", false));
   public final Setting<Boolean> vulcanSprint = this.register(new Setting<>("VulcanSprint", true, v -> this.vulcan.getValue()));
   public final Setting<Boolean> vulcanMovement = this.register(new Setting<>("VulcanMovement", true, v -> this.vulcan.getValue()));
   public final Setting<Boolean> vulcanAutoClicker = this.register(new Setting<>("VulcanAutoClick", true, v -> this.vulcan.getValue()));
   public final Setting<Boolean> vulcanFastUse = this.register(new Setting<>("VulcanFastUse", true, v -> this.vulcan.getValue()));
   public final Setting<Boolean> vulcanMisc = this.register(new Setting<>("VulcanMisc", true, v -> this.vulcan.getValue()));
   public final Setting<Boolean> vulcanGroundSpoof = this.register(new Setting<>("VulcanGroundSpoof", false, v -> this.vulcan.getValue()));
   public final Setting<Integer> vulcanTick = this.register(
      new Setting<>("VulcanTick", 15, 1, 20, v -> this.vulcan.getValue() && this.vulcanGroundSpoof.getValue())
   );
   public final Setting<Boolean> sneakStop = this.register(new Setting<>("SneakStop", false));
   public final Setting<Integer> sneakDelay = this.register(new Setting<>("SneakDelay", 1, 1, 20, v -> this.sneakStop.getValue()));
   public final Setting<Boolean> sneakStartToo = this.register(new Setting<>("SneakStartToo", false, v -> this.sneakStop.getValue()));
   public final Setting<Boolean> deathzoneTest = this.register(new Setting<>("DeathZoneTest", false));
   public final Setting<Boolean> deathzoneReach = this.register(new Setting<>("DeathzoneReach", false));
   public final Setting<Boolean> cubeCraft = this.register(new Setting<>("CubeCraft", false));
   public final Setting<Float> ccMoveSpeed = this.register(new Setting<>("CcMoveSpeed", 0.2F, 0.0F, 1.0F, v -> this.cubeCraft.getValue()));
   public final Setting<Boolean> startSprint = this.register(new Setting<>("StartSprint", true));
   public final Setting<Boolean> miniblox = this.register(new Setting<>("Miniblox", false));
   public final Setting<Boolean> ghostly = this.register(new Setting<>("Ghostly", false));
   private float spoofYaw = 0.0F;
   private float spoofPitch = 0.0F;
   private int vulcanTimer = 0;
   private int sneakTimer = 0;
   private int dzTestTimer = 0;
   private boolean dzReachAttacked = false;
   private int aTick = 0;
   private boolean ccNotified = false;
   private boolean ccNotified2 = false;
   private boolean ccSending = false;

   public Disabler() {
      super("Disabler", "Bypasses anti-cheat detection.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.spoofYaw = mc.player.getYRot();
         this.spoofPitch = mc.player.getXRot();
         this.vulcanTimer = 0;
         this.aTick = 0;
         this.ccNotified = false;
         this.ccNotified2 = false;
         this.ccSending = false;
         if (this.startSprint.getValue()) {
            mc.player.setSprinting(true);
         }
      }
   }

   @Override
   public void onDisable() {
      this.vulcanTimer = 0;
      this.sneakTimer = 0;
      this.dzTestTimer = 0;
      this.dzReachAttacked = false;
      this.aTick = 0;
      this.ccNotified = false;
      this.ccNotified2 = false;
      this.ccSending = false;
   }

   @Override
   public void onTick() {
      if (mc.player != null) {
         if (this.startSprint.getValue()) {
            mc.player.setSprinting(true);
         }

         if (this.deathzoneTest.getValue()) {
            this.dzTestTimer++;
            if (this.dzTestTimer >= 7) {
               mc.player.connection.send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, true, false)));
               mc.player.connection.send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, false, false)));
               this.dzTestTimer = 0;
            }
         }

         if (this.sneakStop.getValue()) {
            this.sneakTimer++;
            if (this.sneakTimer >= this.sneakDelay.getValue()) {
               if (this.sneakStartToo.getValue()) {
                  mc.player.setShiftKeyDown(true);
               }

               mc.player.setShiftKeyDown(false);
               this.sneakTimer = 0;
            }
         }

         if (this.vulcan.getValue()) {
            this.vulcanTimer++;
            if (this.vulcanMovement.getValue() && this.vulcanTimer % 5 == 0) {
               mc.player.connection.send(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, mc.player.blockPosition(), Direction.UP));
            }

            if (this.vulcanFastUse.getValue() && this.vulcanTimer % 7 == 0) {
               mc.player.connection.send(new ServerboundPlayerActionPacket(Action.DROP_ITEM, BlockPos.ZERO, Direction.DOWN));
            }

            if (this.vulcanGroundSpoof.getValue() && this.vulcanTimer >= this.vulcanTick.getValue()) {
               mc.player
                  .connection
                  .send(
                     new PosRot(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), true, mc.player.horizontalCollision
                     )
                  );
               this.vulcanTimer = 0;
            }
         }

         if (this.miniblox.getValue()) {
            mc.player
               .connection
               .send(
                  new ServerboundPlayerInputPacket(
                     new Input(
                        mc.options.keyUp.isDown(),
                        mc.options.keyDown.isDown(),
                        mc.options.keyLeft.isDown(),
                        mc.options.keyRight.isDown(),
                        mc.options.keyJump.isDown(),
                        mc.options.keyShift.isDown(),
                        mc.options.keySprint.isDown()
                     )
                  )
               );
         }
      }
   }

   @Subscribe
   private void onPacketSend(PacketEvent.Send event) {
      if (mc.player != null) {
         if (this.cubeCraft.getValue() && !this.ccSending) {
            this.handleCubeCraft(event);
            if (event.isCancelled()) {
               return;
            }
         }

         Packet<?> pkt = event.getPacket();
         if (this.deathzoneReach.getValue()) {
            if (pkt instanceof ServerboundAttackPacket) {
               this.dzReachAttacked = true;
            } else if (this.dzReachAttacked && pkt instanceof ServerboundMovePlayerPacket) {
               this.dzReachAttacked = false;
               event.cancel();
               return;
            }
         }

         if (this.ghostly.getValue() && pkt instanceof ServerboundPlayerInputPacket) {
            event.cancel();
            return;
         }

         if (this.cancelKeepAlive.getValue() && pkt instanceof ServerboundKeepAlivePacket) {
            event.cancel();
         } else if (this.cancelSwing.getValue() && pkt instanceof ServerboundSwingPacket) {
            event.cancel();
         } else if (this.cancelEntityAction.getValue() && pkt instanceof ServerboundPlayerCommandPacket) {
            event.cancel();
         } else if (this.cancelPlayerAction.getValue() && pkt instanceof ServerboundPlayerActionPacket) {
            event.cancel();
         } else if (this.cancelAbilities.getValue() && pkt instanceof ServerboundPlayerAbilitiesPacket) {
            event.cancel();
         } else {
            if (this.cancelMove.getValue() && pkt instanceof ServerboundMovePlayerPacket) {
               Vec3 mot = mc.player.getDeltaMovement();
               boolean moving = Math.abs(mot.x) > 0.005 || Math.abs(mot.z) > 0.005;
               if (!this.onlyWhenStill.getValue() || !moving) {
                  event.cancel();
                  return;
               }
            }

            if (this.vulcan.getValue() && this.vulcanMisc.getValue() && pkt instanceof ServerboundCustomPayloadPacket) {
               event.cancel();
            }
         }
      }
   }

   @Subscribe
   private void onMotion(UpdateWalkingPlayerEvent event) {
      if (mc.player != null) {
         if (event.getStage() == Stage.MOTION_PRE) {
            if (this.noRotation.getValue()) {
               this.applyRotationSpoof();
            }

            if (this.watchdogMotion.getValue()) {
               this.applyWatchdogMotion();
            }

            if (this.vulcan.getValue()) {
               if (this.vulcanSprint.getValue()) {
                  mc.player
                     .connection
                     .send(
                        new ServerboundPlayerCommandPacket(mc.player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_SPRINTING)
                     );
                  mc.player
                     .connection
                     .send(
                        new ServerboundPlayerCommandPacket(mc.player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.STOP_SPRINTING)
                     );
               }

               if (this.vulcanAutoClicker.getValue() && mc.screen == null) {
                  mc.player.connection.send(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, mc.player.blockPosition(), Direction.UP));
               }
            }
         }

         if (event.getStage() == Stage.POST && this.noRotation.getValue()) {
            mc.player.setYRot(mc.player.getYRot());
            mc.player.yHeadRot = mc.player.getYRot();
         }
      }
   }

   private void handleCubeCraft(PacketEvent.Send event) {
      if (mc.player != null) {
         if (mc.player.tickCount <= 20) {
            this.aTick = 0;
            this.ccNotified = false;
            this.ccNotified2 = false;
         } else {
            if (this.aTick == 1) {
               this.aTick++;
            }

            if (this.aTick >= 3) {
               if (event.getPacket() instanceof ServerboundPlayerInputPacket) {
                  event.cancel();
                  this.sendDirect(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, false, false)));
               }

               if ((this.aTick == 3 || this.aTick >= 328) && event.getPacket() instanceof ServerboundMovePlayerPacket) {
                  event.cancel();
               }

               if (this.aTick % 55 == 0 && event.getPacket() instanceof ServerboundMovePlayerPacket) {
                  event.cancel();
                  this.sendDirect(new StatusOnly(true, mc.player.horizontalCollision));
                  this.ccNotified2 = true;
               }
            }

            if (event.getPacket() instanceof ServerboundMovePlayerPacket
               && Math.sqrt(mc.player.distanceToSqr(mc.player.xo, mc.player.yo, mc.player.zo)) <= 8.0 - this.ccMoveSpeed.getValue().floatValue() - 0.15
               && mc.player.tickCount % 2 != 0) {
               event.cancel();
            }

            if (event.getPacket() instanceof ServerboundPongPacket pong) {
               int id = pong.getId();
               if (id >= 0 || this.aTick < 3 && this.aTick != 0) {
                  if (id != 0) {
                     event.cancel();
                     if (this.aTick >= 330 || this.aTick == 2) {
                        this.sendDirect(new ServerboundPongPacket(id + 3));
                        this.aTick = 3;
                     }
                  }
               } else {
                  this.aTick++;
                  this.sendDirect(new ServerboundPongPacket(id + 1));
               }
            }
         }
      }
   }

   private void sendDirect(Packet<?> packet) {
      this.ccSending = true;

      try {
         mc.player.connection.send(packet);
      } finally {
         this.ccSending = false;
      }
   }

   private void applyRotationSpoof() {
      if (this.rotMode.getValue() != null) {
         float yaw = mc.player.getYRot();
         float pitch = mc.player.getXRot();
         switch ((Disabler.RotationMode)this.rotMode.getValue()) {
            case ZERO:
               mc.player.setYRot(0.0F);
               mc.player.yHeadRot = 0.0F;
               mc.player.setXRot(0.0F);
               break;
            case SPOOF:
               mc.player.setYRot(this.spoofYaw);
               mc.player.yHeadRot = this.spoofYaw;
               mc.player.setXRot(this.spoofPitch);
               break;
            case SPOOF_ZERO:
               mc.player.setYRot(this.spoofYaw);
               mc.player.yHeadRot = this.spoofYaw;
               mc.player.setXRot(0.0F);
               break;
            case NEGATIVE:
               mc.player.setYRot(-yaw);
               mc.player.yHeadRot = -yaw;
               mc.player.setXRot(-pitch);
               break;
            case OFFSET_YAW:
               mc.player.setYRot(yaw + this.rotOffset.getValue());
               mc.player.yHeadRot = yaw + this.rotOffset.getValue();
               break;
            case INVALID:
               mc.player.setYRot(180.0F);
               mc.player.yHeadRot = 180.0F;
               mc.player.setXRot(90.0F);
         }
      }
   }

   private void applyWatchdogMotion() {
      Vec3 mot = mc.player.getDeltaMovement();
      if (mc.player.onGround()) {
         mc.player.setDeltaMovement(mot.x, 0.42, mot.z);
      } else if (mot.y < 0.1) {
         mc.player.setDeltaMovement(mot.x, -0.3, mot.z);
      }
   }

   @Override
   public String getDisplayInfo() {
      return null;
   }

   public enum RotationMode {
      ZERO,
      SPOOF,
      SPOOF_ZERO,
      NEGATIVE,
      OFFSET_YAW,
      INVALID;
   }
}
