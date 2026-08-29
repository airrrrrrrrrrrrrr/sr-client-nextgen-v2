package me.alpha432.oyvey.features.modules.render;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;

public class FreeLookModule extends Module {
   public static FreeLookModule INSTANCE;
   public final Setting<Float> sensitivity = this.num("Sensitivity", 1.0F, 0.1F, 3.0F);
   float freeYaw = 0.0F;
   float freePitch = 0.0F;

   public FreeLookModule() {
      super("FreeLook", "Pan the camera freely without turning your character.", Module.Category.RENDER);
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.freeYaw = 0.0F;
      this.freePitch = 0.0F;
   }

   @Override
   public void onDisable() {
      this.freeYaw = 0.0F;
      this.freePitch = 0.0F;
   }

   public boolean isFreelooking() {
      return this.isEnabled() && mc.player != null;
   }

   public float getFreeYaw() {
      return this.freeYaw;
   }

   public float getFreePitch() {
      return this.freePitch;
   }

   public float getSensitivity() {
      return this.sensitivity.getValue();
   }

   public void addDelta(float dYaw, float dPitch) {
      this.freeYaw = this.freeYaw + dYaw * this.sensitivity.getValue();
      this.freePitch = Math.max(-90.0F, Math.min(90.0F, this.freePitch + dPitch * this.sensitivity.getValue()));
   }

   @Override
   public String getDisplayInfo() {
      return !this.isEnabled() ? null : String.format("%.0f\u00b0 %.0f\u00b0", this.freeYaw % 360.0F, this.freePitch);
   }
}
