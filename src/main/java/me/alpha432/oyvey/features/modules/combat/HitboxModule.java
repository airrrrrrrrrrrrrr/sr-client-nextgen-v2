package me.alpha432.oyvey.features.modules.combat;

import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;

public class HitboxModule extends Module {
   public static HitboxModule INSTANCE;
   public final Setting<Double> horizontal = this.register(new Setting<>("Horizontal", 0.15, 0.0, 1.0));
   public final Setting<Double> vertical = this.register(new Setting<>("Vertical", 0.1, 0.0, 1.0));

   public HitboxModule() {
      super("Hitbox", "Expands entity hitboxes so off-aim attacks still connect.", Module.Category.COMBAT);
      INSTANCE = this;
   }

   @Override
   public String getDisplayInfo() {
      return !this.isEnabled() ? null : String.format("H:%.2f V:%.2f", this.horizontal.getValue(), this.vertical.getValue());
   }
}
