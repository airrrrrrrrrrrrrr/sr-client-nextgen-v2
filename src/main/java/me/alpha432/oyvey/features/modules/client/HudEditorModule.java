package me.alpha432.oyvey.features.modules.client;

import me.alpha432.oyvey.features.gui.HudEditorScreen;
import me.alpha432.oyvey.features.modules.Module;

public class HudEditorModule extends Module {
   public HudEditorModule() {
      super("HudEditor", "Edit HUD element positions", Module.Category.CLIENT);
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         mc.setScreen(HudEditorScreen.getInstance());
         this.disable();
      }
   }
}
