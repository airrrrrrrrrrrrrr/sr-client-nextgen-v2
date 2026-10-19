package me.alpha432.oyvey.features.modules.combat;

import java.util.concurrent.ThreadLocalRandom;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.movement.Scaffold;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.mixin.render.accessor.LocalPlayerRotationAccessor;
import me.alpha432.oyvey.mixin.render.accessor.ClientInputAccessor;
import me.alpha432.oyvey.util.KeyboardMovement;
import me.alpha432.oyvey.util.KnockbackDisplacement;
import me.alpha432.oyvey.util.TransmittedRotation;
import me.alpha432.oyvey.util.SilentRotationMath;
import me.alpha432.oyvey.util.CombatAim;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class KnockbackDisplace extends Module {
   public final Setting<Integer> chance = this.register(new Setting<>("Chance", 100, 0, 100,
      "Percentage of eligible attacks that displace knockback."));
   public final Setting<KnockbackDisplacement.Direction> direction = this.register(
      new Setting<>("Direction", KnockbackDisplacement.Direction.LEFT));
   public final Setting<Boolean> movementFix = this.register(new Setting<>("MovementFix", true,
      "Preserve movement direction during the temporary attack rotation."));
   public final Setting<Boolean> grim = this.register(new Setting<>("Grim", true,
      "Uses ordinary on-target rotations. Sideways knockback is limited by the target's hitbox and distance."));

   private final TransmittedRotation transmittedLook = new TransmittedRotation();
   private LocalPlayer trackedPlayer;
   private boolean attacking;
   private boolean ownsManualLook;
   private float manualYaw, manualPitch, cameraYaw, cameraPitch;
   private int rollTarget = -1, selectedRoll, rerollAtTick = Integer.MAX_VALUE;
   private volatile int resumeAfterTick;

   public KnockbackDisplace() {
      super("KnockbackDisplace", "Directs sprint or enchanted knockback to the left or right of your attack aim.", Category.COMBAT);
   }

   @Override
   public void onEnable() {
      this.trackedPlayer = null;
      this.trackPlayer();
   }

   @Override
   public void onDisable() {
      this.releaseManualLook();
      this.trackedPlayer = null;
      this.rollTarget = -1;
   }

   private void trackPlayer() {
      if (mc.player == this.trackedPlayer) return;
      this.trackedPlayer = mc.player;
      this.rollTarget = -1;
      this.resumeAfterTick = 0;
      if (this.trackedPlayer != null) {
         var accessor = (LocalPlayerRotationAccessor)this.trackedPlayer;
         this.transmittedLook.reset(accessor.oyvey$getLastSentYaw(), accessor.oyvey$getLastSentPitch());
      }
   }

   @Subscribe(priority = -1100)
   private void onMovementPacket(PacketEvent.Send event) {
      if (event.isCancelled() || !(event.getPacket() instanceof ServerboundMovePlayerPacket movement)) return;
      this.trackPlayer();
      this.transmittedLook.record(movement, false, -1, 0, 0);
   }

   @Subscribe
   private void onCorrection(PacketEvent.Receive event) {
      if (mc.player != null && event.getPacket() instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket)
         this.resumeAfterTick = mc.player.tickCount + 3;
   }

   /** Used by Recode before it builds and transmits its ordinary target look. */
   public Vec3 aimPoint(Entity target, Vec3 eyes, Vec3 aim) {
      return this.aimPoint(target,eyes,aim,true);
   }

   public Vec3 aimPoint(Entity target, Vec3 eyes, Vec3 aim, boolean requireVisible) {
      if (!this.isEnabled() || !this.grim.getValue() || nullCheck() || mc.player.tickCount < this.resumeAfterTick
         || !(target instanceof LivingEntity) || !target.isAlive() || target.isRemoved() || this.chance.getValue()<=0) return aim;
      this.trackPlayer();
      if (this.rollTarget != target.getId() || mc.player.tickCount >= this.rerollAtTick) {
         this.rollTarget = target.getId();
         this.selectedRoll = ThreadLocalRandom.current().nextInt(100);
         this.rerollAtTick = Integer.MAX_VALUE;
      }
      if (!KnockbackDisplacement.shouldDisplace(this.chance.getValue(), this.selectedRoll, true, false)) return aim;
      // Prepare before sprint resets/restarts. The server applies the actual
      // sprint/enchantment bonus at attack time; gating aim on this tick's
      // local sprint state dropped entire attack cycles.
      for (Vec3 point : KnockbackDisplacement.safeAimPoints(target.getBoundingBox(), eyes, aim, this.direction.getValue())) {
         Vec3 delta=point.subtract(eyes);
         var look=SilentRotationMath.quantizeRotation(this.transmittedLook.yaw(),this.transmittedLook.pitch(),
            (float)Math.toDegrees(Math.atan2(delta.z,delta.x))-90,
            (float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))),mc.options.sensitivity().get());
         Vec3 end=eyes.add(Vec3.directionFromRotation(look.pitch(),look.yaw()).scale(eyes.distanceTo(point)+target.getBoundingBox().getSize()+1));
         if (!target.getBoundingBox().contains(eyes) && target.getBoundingBox().clip(eyes,end).isEmpty()) continue;
         if (!requireVisible || mc.level.clip(new ClipContext(eyes,point,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player)).getType()==HitResult.Type.MISS)
            return point;
      }
      return aim;
   }

   public boolean requiresMovementFix() { return this.isEnabled() && this.grim.getValue() && this.movementFix.getValue(); }

   @Subscribe(priority = -1200)
   private void onWalking(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == Stage.POST) { this.releaseManualLook(); return; }
      if (event.getStage() == Stage.PRE && this.ownsManualLook) {
         OyVey.rotationManager.setPlayerRotations(this.manualYaw, this.manualPitch);
         OyVey.rotationManager.setServerRotations(this.manualYaw, this.manualPitch);
         return;
      }
      if (event.getStage() != Stage.MOTION_PRE) return;
      this.releaseManualLook();
      this.trackPlayer();
      if (!this.grim.getValue() || nullCheck() || mc.screen != null || mc.player.isSpectator() || mc.player.isPassenger()
         || mc.player.tickCount < this.resumeAfterTick) return;
      KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      BedNuker nuker = OyVey.moduleManager.getModuleByClass(BedNuker.class);
      if (aura != null && aura.isEnabled() && aura.getTarget() != null
         || scaffold != null && scaffold.ownsRotation() || nuker != null && nuker.isEnabled()) return;
      // A click is handled before MOTION_PRE. Arm while the crosshair is on
      // the target so an ordinary tap can use the preceding native look.
      if (!(mc.crosshairPickEntity instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()) return;
      Vec3 eyes = mc.player.getEyePosition(), center = CombatAim.headCenter(target.getBoundingBox(), target.getEyeY());
      Vec3 aim = this.aimPoint(target, eyes, center);
      if (aim.equals(center)) return;
      Vec3 delta = aim.subtract(eyes);
      var look = SilentRotationMath.quantizeRotation(this.transmittedLook.yaw(), this.transmittedLook.pitch(),
         (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90,
         (float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))), mc.options.sensitivity().get());
      this.cameraYaw = mc.player.getYRot(); this.cameraPitch = mc.player.getXRot();
      this.manualYaw = look.yaw(); this.manualPitch = look.pitch(); this.ownsManualLook = true;
      OyVey.rotationManager.setPlayerRotations(this.manualYaw, this.manualPitch);
      OyVey.rotationManager.setServerRotations(this.manualYaw, this.manualPitch);
   }

   private void releaseManualLook() {
      if (!this.ownsManualLook) return;
      if (mc.player != null && mc.player == this.trackedPlayer) {
         OyVey.rotationManager.setPlayerRotations(this.cameraYaw, this.cameraPitch);
         OyVey.rotationManager.clearServerRotations();
      }
      this.ownsManualLook = false;
   }

   public void applyInput(ClientInput input) {
      if (!this.ownsManualLook || !this.requiresMovementFix()) return;
      Input keys = input.keyPresses;
      var movement = SilentRotationMath.correctMovement(keys, this.cameraYaw, this.manualYaw);
      input.keyPresses = new Input(movement.forward() > 0, movement.forward() < 0, movement.sideways() > 0,
         movement.sideways() < 0, keys.jump(), keys.shift(), keys.sprint() && movement.forward() > 0);
      ((ClientInputAccessor)input).oyvey$setMoveVector(KeyboardMovement.vector(movement));
      if (movement.forward() <= 0) mc.player.setSprinting(false);
   }

   private void nativeAttack(LocalPlayer player, Runnable original) {
      this.trackPlayer();
      float yaw = player.getYRot(), pitch = player.getXRot();
      this.attacking = true;
      try {
         // The last real movement packet determines server-side attack yaw.
         // Local knockback/attack slowdown must simulate that same look.
         player.setYRot(this.transmittedLook.yaw()); player.setXRot(this.transmittedLook.pitch());
         original.run();
      } finally {
         player.setYRot(yaw); player.setXRot(pitch);
         this.attacking = false;
         this.rerollAtTick = player.tickCount + 1;
      }
   }

   /** Shared by manual attacks and Killaura Recode through the game-mode attack hook. */
   public void attack(Player attacker, Entity target, Runnable original) {
      if (!this.isEnabled() || this.attacking || nullCheck() || attacker != mc.player || attacker.isSpectator()
         || !(target instanceof LivingEntity) || !target.isAlive() || target.isRemoved() || target == attacker
         || this.chance.getValue() <= 0) {
         original.run();
         return;
      }
      Scaffold scaffold = OyVey.moduleManager.getModuleByClass(Scaffold.class);
      if (scaffold != null && scaffold.ownsRotation()) {
         original.run();
         return;
      }
      if (this.grim.getValue()) { this.nativeAttack(mc.player, original); return; }
      var knockback = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.KNOCKBACK);
      boolean enchanted = EnchantmentHelper.getItemEnchantmentLevel(knockback, attacker.getMainHandItem()) > 0;
      if (!KnockbackDisplacement.shouldDisplace(this.chance.getValue(), ThreadLocalRandom.current().nextInt(100),
         attacker.isSprinting(), enchanted)) {
         original.run();
         return;
      }

      this.trackPlayer();
      LocalPlayer player = mc.player;
      float cameraYaw = player.getYRot();
      float cameraPitch = player.getXRot();
      float headYaw = player.yHeadRot;
      var input = player.input;
      Input savedKeys = input.keyPresses;
      var savedVector = input.getMoveVector();
      // Recode's last actual outgoing aim can differ from the visible camera
      // and from the aim it has just prepared for the next movement packet.
      var originalLook = new KnockbackDisplacement.Look(this.transmittedLook.yaw(), this.transmittedLook.pitch());
      KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      // Recode can already have corrected this input before restoring the
      // camera. Reuse that input's actual yaw basis rather than correcting twice.
      float inputYaw = aura == null ? cameraYaw : aura.movementInputYaw(input, cameraYaw);
      boolean fixMovement = this.movementFix.getValue();
      this.attacking = true;
      try {
         ((LocalPlayerRotationAccessor)player).oyvey$sendSprintState();
         KnockbackDisplacement.attack(originalLook, this.direction.getValue(), look -> {
            if (fixMovement) {
               if (look.equals(originalLook)) {
                  input.keyPresses = savedKeys;
                  ((ClientInputAccessor)input).oyvey$setMoveVector(savedVector);
               } else {
                  var movement = look.correctMovement(savedKeys, inputYaw);
                  input.keyPresses = new Input(movement.forward() > 0, movement.forward() < 0,
                     movement.sideways() > 0, movement.sideways() < 0, savedKeys.jump(), savedKeys.shift(), savedKeys.sprint());
                  ((ClientInputAccessor)input).oyvey$setMoveVector(KeyboardMovement.vector(movement));
               }
            }
            // Match local attack simulation to the look received by the server.
            player.setYRot(look.yaw());
            player.setXRot(look.pitch());
            player.connection.send(new ServerboundMovePlayerPacket.Rot(look.yaw(), look.pitch(),
               player.onGround(), player.horizontalCollision));
         }, original);
      } finally {
         if (fixMovement) {
            input.keyPresses = savedKeys;
            ((ClientInputAccessor)input).oyvey$setMoveVector(savedVector);
         }
         player.setYRot(cameraYaw);
         player.setXRot(cameraPitch);
         player.yHeadRot = headYaw;
         this.attacking = false;
      }
   }

   @Override
   public String getDisplayInfo() {
      return this.direction.getValue().name();
   }
}
