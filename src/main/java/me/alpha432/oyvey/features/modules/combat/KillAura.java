package me.alpha432.oyvey.features.modules.combat;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.Stage;
import me.alpha432.oyvey.event.impl.entity.player.UpdateWalkingPlayerEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.movement.SprintModule;
import me.alpha432.oyvey.features.modules.render.RotationVisualizerModule;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.RotationUtils;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class KillAura extends Module {
   public final Setting<Double> reach = this.register(new Setting<>("Reach", 3.8, 1.0, 6.0));
   public final Setting<Boolean> mobs = this.register(new Setting<>("Mobs", true));
   public final Setting<Integer> cps = this.register(new Setting<>("CPS", 12, 1, 20));
   public final Setting<Boolean> cpsRand = this.register(new Setting<>("CpsRand", false));
   public final Setting<Integer> cpsMin = this.register(new Setting<>("CpsMin", 8, 1, 20, v -> this.cpsRand.getValue()));
   public final Setting<Integer> cpsMax = this.register(new Setting<>("CpsMax", 14, 1, 20, v -> this.cpsRand.getValue()));
   public final Setting<Boolean> cooldown = this.register(new Setting<>("1.9Cooldown", true));
   public final Setting<Boolean> onlyCrits = this.register(new Setting<>("OnlyCrits", true));
   public final Setting<Boolean> multi = this.register(new Setting<>("MultiAura", false));
   public final Setting<Boolean> switchMode = this.register(new Setting<>("SwitchAura", false));
   public final Setting<Integer> switchDelay = this.register(new Setting<>("SwitchDelay", 600, 50, 3000, v -> this.switchMode.getValue()));
   public final Setting<KillAura.AutoBlockMode> autoBlock = this.register(new Setting<>("AutoBlock", KillAura.AutoBlockMode.SMART));
   public final Setting<Boolean> correctMove = this.register(new Setting<>("CorrectMove", true));
   public final Setting<Boolean> intaveMode = this.register(new Setting<>("IntaveMode", false));
   public final Setting<Float> intaveJitter = this.register(new Setting<>("IntaveJitter", 0.003F, 0.001F, 0.005F, v -> this.intaveMode.getValue()));
   public final Setting<Boolean> antiBot = this.register(new Setting<>("AntiBot", true));
   public final Setting<Float> botSpinThreshold = this.register(new Setting<>("BotSpinDeg", 60.0F, 20.0F, 180.0F, v -> this.antiBot.getValue()));
   private float[] targetRots = null;
   private float savedYaw = 0.0F;
   private float savedPitch = 0.0F;
   private boolean hasSaved = false;
   private float sentYaw = 0.0F;
   private float sentPitch = 0.0F;
   private boolean hasSentRot = false;
   public static volatile boolean isRotating = false;
   public static volatile float visualYaw = 0.0F;
   public static volatile float visualPitch = 0.0F;
   private final Map<Integer, float[]> botTracker = new HashMap<>();
   private Entity currentTarget = null;
   private Entity searchOnlyTarget = null;
   private long lastAttackTime = 0L;
   private long nextAttackDelay = 0L;
   private long lastSwitchTime = 0L;
   private int switchIndex = 0;
   private boolean autoBlockActive = false;

   public KillAura() {
      super("KillAura", "Attack players for you.", Module.Category.OLD);
   }

   @Override
   public void onEnable() {
      this.botTracker.clear();
      this.targetRots = null;
      this.hasSaved = false;
      this.hasSentRot = false;
      isRotating = false;
      this.currentTarget = null;
      this.searchOnlyTarget = null;
      this.switchIndex = 0;
      this.lastSwitchTime = 0L;
      this.nextAttackDelay = this.rollAttackDelay();
      this.autoBlockActive = false;
   }

   @Override
   public void onDisable() {
      if (this.hasSaved && mc.player != null) {
         OyVey.rotationManager.setPlayerRotations(this.savedYaw, this.savedPitch);
      }

      if (this.autoBlockActive && mc.player != null) {
         mc.player.releaseUsingItem();
         this.autoBlockActive = false;
      }

      isRotating = false;
      this.hasSaved = false;
      this.hasSentRot = false;
      this.targetRots = null;
      this.currentTarget = null;
      this.searchOnlyTarget = null;
      OyVey.rotationManager.clearServerRotations();
   }

   @Subscribe
   public void onUpdateWalkingPlayer(UpdateWalkingPlayerEvent event) {
      if (mc.level != null && mc.player != null) {
         if (event.getStage() == Stage.MOTION_PRE) {
            this.updateTargeting();
            if (this.targetRots != null && this.correctMove.getValue()) {
               this.savedYaw = mc.player.getYRot();
               this.savedPitch = mc.player.getXRot();
               visualYaw = this.savedYaw;
               visualPitch = this.savedPitch;
               this.hasSaved = true;
               isRotating = true;
               OyVey.rotationManager.setPlayerRotations(this.targetRots[0], this.targetRots[1]);
               this.applyMovementCorrection(this.targetRots[0], this.savedYaw);
            } else {
               this.hasSaved = false;
               isRotating = false;
            }

            if (this.targetRots != null && this.currentTarget != null) {
               SprintModule sm0 = OyVey.moduleManager.getModuleByClass(SprintModule.class);
               boolean ks = sm0 != null && sm0.keepSprint.getValue();
               if (!ks && this.shouldAttack() && mc.player.isSprinting()) {
                  mc.player.setSprinting(false);
                  mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SPRINTING));
               }
            }
         } else if (event.getStage() == Stage.POST) {
            isRotating = false;
            if (this.hasSaved && mc.player != null) {
               OyVey.rotationManager.setPlayerRotations(this.savedYaw, this.savedPitch);
               if (this.targetRots != null) {
                  this.sentYaw = this.targetRots[0];
                  this.sentPitch = this.targetRots[1];
                  this.hasSentRot = true;
               }
            } else {
               this.hasSentRot = false;
            }

            this.hasSaved = false;
         } else if (event.getStage() == Stage.PRE) {
            SprintModule sm = OyVey.moduleManager.getModuleByClass(SprintModule.class);
            boolean ks = sm != null && sm.keepSprint.getValue();
            if (!ks && mc.player.isSprinting()) {
               long pauseMs = sm != null ? sm.sprintResetTicks.getValue().intValue() * 50L : 500L;
               if (System.currentTimeMillis() - SprintModule.lastAttackMs < pauseMs) {
                  mc.player.setSprinting(false);
               }
            }

            if (this.targetRots == null) {
               isRotating = false;
               if (this.autoBlockActive) {
                  mc.player.releaseUsingItem();
                  this.autoBlockActive = false;
               }
            } else {
               if (!this.hasSaved) {
                  this.savedYaw = mc.player.getYRot();
                  this.savedPitch = mc.player.getXRot();
                  this.hasSaved = true;
               }

               visualYaw = this.savedYaw;
               visualPitch = this.savedPitch;
               isRotating = true;
               OyVey.rotationManager.setPlayerRotations(this.targetRots[0], this.targetRots[1]);
               if (this.currentTarget != null) {
                  KillAura.AutoBlockMode blockMode = this.autoBlock.getValue();
                  if (blockMode == KillAura.AutoBlockMode.FULL) {
                     mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                     this.autoBlockActive = true;
                  }

                  if ((blockMode == KillAura.AutoBlockMode.SMART || blockMode == KillAura.AutoBlockMode.VANILLA) && !this.autoBlockActive) {
                     mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                     this.autoBlockActive = true;
                  }

                  if (this.shouldAttack()) {
                     SprintModule smKs = OyVey.moduleManager.getModuleByClass(SprintModule.class);
                     boolean ksx = smKs != null && smKs.keepSprint.getValue();
                     if (!ksx && mc.player.isSprinting()) {
                        mc.player.setSprinting(false);
                        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SPRINTING));
                     }

                     SprintModule.lastAttackMs = System.currentTimeMillis();
                     if (blockMode == KillAura.AutoBlockMode.SMART && this.autoBlockActive) {
                        mc.player.releaseUsingItem();
                        this.autoBlockActive = false;
                     }

                     if (this.multi.getValue()) {
                        for (Entity t : this.getTargets()) {
                           this.performAttack(t);
                        }
                     } else if (this.switchMode.getValue()) {
                        List<Entity> targets = this.getTargets();
                        if (targets.isEmpty()) {
                           return;
                        }

                        if (targets.size() > 1 && System.currentTimeMillis() - this.lastSwitchTime >= this.switchDelay.getValue().intValue()) {
                           this.switchIndex = (this.switchIndex + 1) % targets.size();
                           this.lastSwitchTime = System.currentTimeMillis();
                        }

                        this.performAttack(targets.get(Math.min(this.switchIndex, targets.size() - 1)));
                     } else {
                        this.performAttack(this.currentTarget);
                     }

                     this.lastAttackTime = System.currentTimeMillis();
                     this.nextAttackDelay = this.rollAttackDelay();
                     if (blockMode == KillAura.AutoBlockMode.SMART || blockMode == KillAura.AutoBlockMode.VANILLA) {
                        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                        this.autoBlockActive = true;
                     }
                  }
               }
            }
         }
      }
   }

   private void updateTargeting() {
      List<Entity> targets = this.getTargets();
      if (targets.isEmpty()) {
         this.currentTarget = null;
         this.switchIndex = 0;
         this.searchOnlyTarget = this.findSearchTarget();
         if (this.searchOnlyTarget != null) {
            if (!this.hasSentRot) {
               this.sentYaw = mc.player.getYRot();
               this.sentPitch = mc.player.getXRot();
            }

            this.targetRots = this.applyGcd(this.calculateRotations(this.searchOnlyTarget));
            OyVey.rotationManager.setServerRotations(this.targetRots[0], this.targetRots[1]);
         } else {
            this.targetRots = null;
            this.hasSentRot = false;
            OyVey.rotationManager.clearServerRotations();
         }
      } else {
         this.searchOnlyTarget = null;
         this.currentTarget = this.switchMode.getValue() && targets.size() > 1 ? targets.get(Math.min(this.switchIndex, targets.size() - 1)) : targets.get(0);
         float[] raw = this.calculateRotations(this.currentTarget);
         if (!this.hasSentRot) {
            this.sentYaw = mc.player.getYRot();
            this.sentPitch = mc.player.getXRot();
         }

         this.targetRots = this.applyGcd(raw);
         OyVey.rotationManager.setServerRotations(this.targetRots[0], this.targetRots[1]);
      }
   }

   private List<Entity> getTargets() {
      double baseReach = this.reach.getValue();
      double effectiveReach = this.intaveMode.getValue() ? Math.min(baseReach, 2.98) : baseReach;
      this.botTracker.keySet().removeIf(id -> {
         Entity e = mc.level.getEntity(id);
         return e == null || !e.isAlive() || mc.player.distanceTo(e) > effectiveReach + 4.0;
      });
      return StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), false)
         .filter(e -> e != mc.player && e.isAlive())
         .filter(e -> e instanceof Player || this.mobs.getValue() && e instanceof Mob)
         .filter(e -> mc.player.distanceTo(e) <= effectiveReach)
         .filter(e -> !this.isBot(e))
         .sorted(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
         .collect(Collectors.toList());
   }

   private Entity findSearchTarget() {
      RotationVisualizerModule rvm = OyVey.moduleManager.getModuleByClass(RotationVisualizerModule.class);
      if (rvm != null && rvm.isEnabled()) {
         float sr = rvm.searchRange.getValue();
         if (sr <= 0.0F) {
            return null;
         }

         double attackReach = this.reach.getValue();
         return StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), false)
            .filter(e -> e != mc.player && e.isAlive())
            .filter(e -> e instanceof Player || this.mobs.getValue() && e instanceof Mob)
            .filter(e -> {
               double d = mc.player.distanceTo(e);
               return d > attackReach && d <= sr;
            })
            .filter(e -> !this.isBot(e))
            .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
            .orElse(null);
      } else {
         return null;
      }
   }

   private boolean isBot(Entity e) {
      if (!this.antiBot.getValue()) {
         return false;
      }

      float[] data = this.botTracker.computeIfAbsent(e.getId(), id -> new float[]{e.getYRot(), 0.0F, 0.0F});
      float delta = Math.abs(Mth.wrapDegrees(e.getYRot() - data[0]));
      data[0] = e.getYRot();
      data[1] += delta;
      data[2]++;
      if (data[2] < 10.0F) {
         return false;
      }

      float avgSpin = data[1] / data[2];
      return avgSpin > this.botSpinThreshold.getValue();
   }

   private void performAttack(Entity target) {
      WTap.onAttack();
      BacktrackModule bt = OyVey.moduleManager.getModuleByClass(BacktrackModule.class);
      if (bt != null && bt.isEnabled()) {
         bt.applyBacktrack(target, () -> {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);
         });
      } else {
         mc.gameMode.attack(mc.player, target);
         mc.player.swing(InteractionHand.MAIN_HAND);
      }
   }

   private boolean shouldAttack() {
      return this.cooldown.getValue()
         ? mc.player.getAttackStrengthScale(0.0F) >= 0.92F
         : System.currentTimeMillis() - this.lastAttackTime >= this.nextAttackDelay;
   }

   private long rollAttackDelay() {
      if (!this.cpsRand.getValue()) {
         return 1000L / this.cps.getValue().intValue();
      }

      int lo = Math.min(this.cpsMin.getValue(), this.cpsMax.getValue());
      int hi = Math.max(this.cpsMin.getValue(), this.cpsMax.getValue());
      float randomCps = lo + ThreadLocalRandom.current().nextFloat() * (hi - lo);
      return (long)(1000.0F / randomCps);
   }

   private void applyMovementCorrection(float serverYaw, float realYaw) {
      mc.player.zza = mc.player.input.getMoveVector().y;
      mc.player.xxa = mc.player.input.getMoveVector().x;
   }

   private float[] applyGcd(float[] raw) {
      return RotationUtils.normalizeGcd(raw, this.sentYaw, this.sentPitch);
   }

   private float[] calculateRotations(Entity target) {
      double dX = target.getX() - mc.player.getX();
      double dY = target.getY() + target.getEyeHeight() * 0.6 - (mc.player.getY() + mc.player.getEyeHeight());
      double dZ = target.getZ() - mc.player.getZ();
      float[] rot = RotationUtils.fromRotationVec(dX, dY, dZ);
      if (this.intaveMode.getValue()) {
         double j = this.intaveJitter.getValue().floatValue();
         rot[0] += gaussian(j);
         rot[1] = Mth.clamp(rot[1] + gaussian(j), -90.0F, 90.0F);
      }

      return rot;
   }

   private static float gaussian(double stdDev) {
      double u1 = ThreadLocalRandom.current().nextDouble();
      double u2 = ThreadLocalRandom.current().nextDouble();
      if (u1 == 0.0) {
         u1 = 5.0E-324;
      }

      return (float)(Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(6.283185307179586 * u2) * stdDev);
   }

   public Entity getTarget() {
      return this.currentTarget;
   }

   public enum AutoBlockMode {
      OFF,
      FULL,
      SMART,
      VANILLA;
   }
}
