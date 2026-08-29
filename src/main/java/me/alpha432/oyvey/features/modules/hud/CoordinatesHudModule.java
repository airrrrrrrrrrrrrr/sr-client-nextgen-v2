package me.alpha432.oyvey.features.modules.hud;

import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.client.HudModule;
import me.alpha432.oyvey.features.settings.Setting;

public class CoordinatesHudModule extends HudModule {
   public Setting<Boolean> nether = this.bool("Nether", false);

   public CoordinatesHudModule() {
      super("Coordinates", "Display coordinates", 150.0F, 20.0F);
   }

   @Override
   protected void render(Render2DEvent e) {
      super.render(e);
      if (!nullCheck()) {
         String coordsStr = String.format("X: %d Y: %d Z: %d", mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
         if (this.nether.getValue()) {
            int netherX = mc.player.level().dimension().identifier().getPath().equals("the_nether") ? mc.player.getBlockX() * 8 : mc.player.getBlockX() / 8;
            int netherZ = mc.player.level().dimension().identifier().getPath().equals("the_nether") ? mc.player.getBlockZ() * 8 : mc.player.getBlockZ() / 8;
            coordsStr = coordsStr + String.format(" [%d, %d]", netherX, netherZ);
         }

         e.getContext().text(mc.font, coordsStr, (int)this.getX(), (int)this.getY(), -1);
         this.setWidth(mc.font.width(coordsStr));
         this.setHeight(9.0F);
      }
   }
}
