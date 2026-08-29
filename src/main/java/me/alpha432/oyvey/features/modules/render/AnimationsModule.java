package me.alpha432.oyvey.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class AnimationsModule extends Module {
   public static AnimationsModule INSTANCE;
   public final Setting<AnimationsModule.BlockAnim> blockAnim = this.register(new Setting<>("BlockAnimation", AnimationsModule.BlockAnim.SEVENTEEN));
   public final Setting<AnimationsModule.SwingMode> swingMode = this.register(new Setting<>("SwingMode", AnimationsModule.SwingMode.SEVENTEEN));
   public final Setting<Integer> swingDuration = this.register(new Setting<>("SwingDuration", 6, 1, 20));
   public final Setting<Boolean> mainHandEnabled = this.register(new Setting<>("MainHand", false));
   public final Setting<Float> mainHandScale = this.register(new Setting<>("MH.Scale", 0.0F, -5.0F, 5.0F, v -> this.mainHandEnabled.getValue()));
   public final Setting<Float> mainHandX = this.register(new Setting<>("MH.X", 0.0F, -5.0F, 5.0F, v -> this.mainHandEnabled.getValue()));
   public final Setting<Float> mainHandY = this.register(new Setting<>("MH.Y", 0.0F, -5.0F, 5.0F, v -> this.mainHandEnabled.getValue()));
   public final Setting<Float> mainHandRotX = this.register(new Setting<>("MH.RotX", 0.0F, -50.0F, 50.0F, v -> this.mainHandEnabled.getValue()));
   public final Setting<Float> mainHandRotY = this.register(new Setting<>("MH.RotY", 0.0F, -50.0F, 50.0F, v -> this.mainHandEnabled.getValue()));
   public final Setting<Float> mainHandRotZ = this.register(new Setting<>("MH.RotZ", 0.0F, -50.0F, 50.0F, v -> this.mainHandEnabled.getValue()));
   public final Setting<Boolean> offHandEnabled = this.register(new Setting<>("OffHand", false));
   public final Setting<Float> offHandScale = this.register(new Setting<>("OH.Scale", 0.0F, -5.0F, 5.0F, v -> this.offHandEnabled.getValue()));
   public final Setting<Float> offHandX = this.register(new Setting<>("OH.X", 0.0F, -1.0F, 1.0F, v -> this.offHandEnabled.getValue()));
   public final Setting<Float> offHandY = this.register(new Setting<>("OH.Y", 0.0F, -1.0F, 1.0F, v -> this.offHandEnabled.getValue()));
   public final Setting<Float> offHandRotX = this.register(new Setting<>("OH.RotX", 0.0F, -50.0F, 50.0F, v -> this.offHandEnabled.getValue()));
   public final Setting<Float> offHandRotY = this.register(new Setting<>("OH.RotY", 0.0F, -50.0F, 50.0F, v -> this.offHandEnabled.getValue()));
   public final Setting<Float> offHandRotZ = this.register(new Setting<>("OH.RotZ", 0.0F, -50.0F, 50.0F, v -> this.offHandEnabled.getValue()));
   public final Setting<Boolean> equipIgnoreBlocking = this.register(new Setting<>("EquipIgnoreBlocking", true));
   public final Setting<Boolean> equipIgnorePlace = this.register(new Setting<>("EquipIgnorePlace", true));
   public final Setting<Boolean> equipIgnoreAmount = this.register(new Setting<>("EquipIgnoreAmount", false));

   public AnimationsModule() {
      super("Animations", "LiquidBounce-style view model and block animations.", Module.Category.RENDER);
      INSTANCE = this;
   }

   public static float applySwingMode(float swingProgress) {
      AnimationsModule anim = INSTANCE;
      if (anim != null && anim.isEnabled()) {
         return switch ((AnimationsModule.SwingMode)anim.swingMode.getValue()) {
            case VANILLA -> swingProgress;
            case SEVENTEEN -> (float)Math.sqrt(swingProgress);
            case SMOOTH -> Mth.sin(swingProgress * 3.1415927F / 2.0F);
         };
      } else {
         return swingProgress;
      }
   }

   public static void applySwingOffset(PoseStack matrices, HumanoidArm arm, float swingProgress) {
      int side = arm == HumanoidArm.RIGHT ? 1 : -1;
      float f = Mth.sin(swingProgress * swingProgress * 3.1415927F);
      matrices.mulPose(Axis.YP.rotationDegrees(side * (45.0F + f * -20.0F)));
      float g = Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
      matrices.mulPose(Axis.ZP.rotationDegrees(side * g * -20.0F));
      matrices.mulPose(Axis.XP.rotationDegrees(g * -80.0F));
      matrices.mulPose(Axis.YP.rotationDegrees(side * -45.0F));
   }

   public static void transformBlockAnimation(PoseStack matrices, HumanoidArm arm, float equipProgress, float swingProgress) {
      AnimationsModule anim = INSTANCE;
      if (anim == null) {
         applyOneSevenTransform(matrices, arm, swingProgress);
      } else if (anim.isEnabled()) {
         switch ((AnimationsModule.BlockAnim)anim.blockAnim.getValue()) {
            case NONE:
            default:
               break;
            case SEVENTEEN:
               applyOneSevenTransform(matrices, arm, swingProgress);
               break;
            case PUSHDOWN:
               applyPushdownTransform(matrices, arm, swingProgress);
               break;
            case RISE:
               applyRiseTransform(matrices, arm, swingProgress);
               break;
            case TILT:
               applyTiltTransform(matrices, arm, swingProgress);
               break;
            case SMOOTH:
               applySmoothTransform(matrices, arm, swingProgress);
         }
      } else {
         applyOneSevenTransform(matrices, arm, swingProgress);
      }
   }

   private static void applyOneSevenTransform(PoseStack matrices, HumanoidArm arm, float swingProgress) {
      matrices.translate(arm == HumanoidArm.RIGHT ? -0.1F : 0.1F, 0.1F, 0.0F);
      applySwingOffset(matrices, arm, swingProgress * 0.9F);
   }

   private static void applyPushdownTransform(PoseStack matrices, HumanoidArm arm, float swingProgress) {
      matrices.translate(arm == HumanoidArm.RIGHT ? -0.1F : 0.1F, 0.1F, 0.0F);
      float g = Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
      int side = arm == HumanoidArm.RIGHT ? 1 : -1;
      matrices.mulPose(Axis.ZP.rotationDegrees(side * g * 10.0F));
      matrices.mulPose(Axis.XP.rotationDegrees(g * -35.0F));
   }

   private static void applyRiseTransform(PoseStack matrices, HumanoidArm arm, float swingProgress) {
      matrices.translate(arm == HumanoidArm.RIGHT ? -0.1F : 0.1F, 0.1F, 0.0F);
      float g = Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
      int side = arm == HumanoidArm.RIGHT ? 1 : -1;
      matrices.mulPose(Axis.ZP.rotationDegrees(side * g * -10.0F));
      matrices.mulPose(Axis.XP.rotationDegrees(g * 35.0F));
   }

   private static void applyTiltTransform(PoseStack matrices, HumanoidArm arm, float swingProgress) {
      int side = arm == HumanoidArm.RIGHT ? 1 : -1;
      matrices.translate(arm == HumanoidArm.RIGHT ? -0.15F : 0.15F, 0.05F, 0.0F);
      matrices.mulPose(Axis.ZP.rotationDegrees(side * -40.0F));
      matrices.mulPose(Axis.XP.rotationDegrees(15.0F));
      matrices.mulPose(Axis.YP.rotationDegrees(side * -10.0F));
   }

   private static void applySmoothTransform(PoseStack matrices, HumanoidArm arm, float swingProgress) {
      matrices.translate(arm == HumanoidArm.RIGHT ? -0.1F : 0.1F, 0.1F, 0.0F);
      applySwingOffset(matrices, arm, swingProgress * 0.5F);
   }

   public static void applyHandTransform(PoseStack matrices, float x, float y, float scale, float rotX, float rotY, float rotZ) {
      matrices.translate(x, y, scale);
      matrices.mulPose(Axis.XP.rotationDegrees(rotX));
      matrices.mulPose(Axis.YP.rotationDegrees(rotY));
      matrices.mulPose(Axis.ZP.rotationDegrees(rotZ));
   }

   @Override
   public String getDisplayInfo() {
      return this.blockAnim.getValue().toString();
   }

   public enum BlockAnim {
      NONE("None"),
      SEVENTEEN("1.7"),
      PUSHDOWN("Pushdown"),
      RISE("Rise"),
      TILT("Tilt"),
      SMOOTH("Smooth");

      private final String display;

      BlockAnim(String display) {
         this.display = display;
      }

      @Override
      public String toString() {
         return this.display;
      }
   }

   public enum SwingMode {
      VANILLA("Vanilla"),
      SEVENTEEN("1.7"),
      SMOOTH("Smooth");

      private final String display;

      SwingMode(String display) {
         this.display = display;
      }

      @Override
      public String toString() {
         return this.display;
      }
   }
}
