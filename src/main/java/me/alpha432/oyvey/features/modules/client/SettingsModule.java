package me.alpha432.oyvey.features.modules.client;

import java.util.EnumMap;
import java.util.Map;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;

/** Persistent ArrayList preferences; hiding a category does not toggle its modules. */
public class SettingsModule extends Module {
   private final Map<Category, Setting<Boolean>> hiddenCategories = new EnumMap<>(Category.class);

   public SettingsModule() {
      super("Settings", "Hide categories from the ArrayList.", Category.CLIENT);
      this.drawn.setValueNoEvent(false);
      for (Category category : Category.values()) {
         this.hiddenCategories.put(category, this.register(new Setting<>("Hide" + category.getName(), false)));
      }
   }

   private static boolean hidden(Category category) {
      if (OyVey.moduleManager == null) {
         return false;
      }
      SettingsModule settings = OyVey.moduleManager.getModuleByClass(SettingsModule.class);
      return settings != null && settings.hiddenCategories.get(category).getValue();
   }

   public static boolean isArrayListVisible(Module module) {
      return !hidden(module.getCategory());
   }
}
