package me.alpha432.oyvey.features.modules.render;

import java.lang.reflect.Field;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.client.OptionInstance;

public class Ambience extends Module {
   public static Ambience INSTANCE;
   public final Setting<Boolean> customTime = this.bool("CustomTime", false);
   public final Setting<Integer> time = this.register(new Setting<>("Time", 6000, 0, 24000, v -> this.customTime.getValue()));
   public final Setting<Float> rainStrength = this.num("RainStrength", 0.0F, 0.0F, 1.0F);
   public final Setting<Float> thunderStrength = this.num("ThunderStrength", 0.0F, 0.0F, 1.0F);
   public final Setting<Float> gamma = this.num("Gamma", 2.0F, 1.0F, 16.0F);
   public final Setting<Boolean> noFog = this.bool("NoFog", false);
   public final Setting<Float> fogStart = this.register(new Setting<>("FogStart", 0.0F, 0.0F, 1.0F, v -> !this.noFog.getValue()));
   public final Setting<Float> fogEnd = this.register(new Setting<>("FogEnd", 1.0F, 0.0F, 1.0F, v -> !this.noFog.getValue()));
   public float currentRain = -1.0F;
   public float currentThunder = -1.0F;
   private double savedGamma = -1.0;
   private double currentGammaI = -1.0;
   private Field gammaField;

   public Ambience() {
      super("Ambience", "Override world atmosphere and environmental rendering.", Module.Category.RENDER);
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      if (mc.level != null) mc.level.environmentAttributes().invalidateTickCache();
      if (mc.options != null) {
         this.savedGamma = (Double)mc.options.gamma().get();
         this.currentGammaI = this.savedGamma;
      }

      if (mc.level != null) {
         this.currentRain = mc.level.getRainLevel(1.0F);
         this.currentThunder = mc.level.getThunderLevel(1.0F);
      } else {
         this.currentRain = 0.0F;
         this.currentThunder = 0.0F;
      }
   }

   @Override
   public void onTick() {
      if (mc.player != null) {
         double gTarget = this.gamma.getValue().floatValue();
         if (this.currentGammaI < 0.0) {
            this.currentGammaI = gTarget;
         }

         this.currentGammaI = lerp(this.currentGammaI, gTarget, 0.08);
         this.forceSetGamma(this.currentGammaI);
         if (this.currentRain < 0.0F) {
            this.currentRain = mc.level != null ? mc.level.getRainLevel(1.0F) : 0.0F;
         }

         if (this.currentThunder < 0.0F) {
            this.currentThunder = mc.level != null ? mc.level.getThunderLevel(1.0F) : 0.0F;
         }

         this.currentRain = lerp(this.currentRain, this.rainStrength.getValue(), 0.05F);
         this.currentThunder = lerp(this.currentThunder, this.thunderStrength.getValue(), 0.05F);
      }
   }

   @Override
   public String getDisplayInfo() {
      return this.customTime.getValue() ? "t:" + this.time.getValue() : null;
   }

   private void forceSetGamma(double value) {
      try {
         if (this.gammaField == null) {
            this.gammaField = OptionInstance.class.getDeclaredField("value");
            this.gammaField.setAccessible(true);
         }

         this.gammaField.set(mc.options.gamma(), value);
      } catch (Exception var4) {
         mc.options.gamma().set(Math.min(1.0, value));
      }
   }

   @Override
   public void onDisable() {
      if (mc.level != null) mc.level.environmentAttributes().invalidateTickCache();
      if (this.savedGamma >= 0.0) {
         this.forceSetGamma(this.savedGamma);
      }

      this.savedGamma = -1.0;
      this.currentGammaI = -1.0;
      this.gammaField = null;
      this.currentRain = -1.0F;
      this.currentThunder = -1.0F;
   }

   private static float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }

   private static double lerp(double a, double b, double t) {
      return a + (b - a) * t;
   }
}
