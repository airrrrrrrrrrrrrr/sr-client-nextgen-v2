package me.alpha432.oyvey.features.modules.render;

import me.alpha432.oyvey.features.modules.Module;

public final class Cape extends Module {
   public static Cape INSTANCE;

   public Cape() {
      super("Cape", "Shows the SR Client cape on your player.", Category.RENDER);
      INSTANCE = this;
   }
}
