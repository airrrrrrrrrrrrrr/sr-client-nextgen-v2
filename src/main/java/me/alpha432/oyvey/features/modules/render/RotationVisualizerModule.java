package me.alpha432.oyvey.features.modules.render;

import java.awt.Color;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.manager.RotationManager;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class RotationVisualizerModule extends Module {
   public final Setting<Float> smoothFactor = this.num("SmoothFactor", 0.3F, 0.02F, 1.0F);
   public final Setting<Float> maxBodyDesync = this.num("BodyDesync", 60.0F, 10.0F, 90.0F);
   public final Setting<Boolean> useEasing = this.bool("Easing", true);
   public final Setting<Float> easingPower = this.register(new Setting<>("EasingPower", 2.0F, 1.0F, 4.0F, v -> this.useEasing.getValue()));
   public final Setting<Float> searchRange = this.num("SearchRange", 0.0F, 0.0F, 10.0F);
   public final Setting<Boolean> showDebug = this.bool("ShowDebug", true);
   public final Setting<Integer> overlayX = this.register(new Setting<>("OverlayX", 4, 0, 1920, v -> this.showDebug.getValue()));
   public final Setting<Integer> overlayY = this.register(new Setting<>("OverlayY", 4, 0, 1080, v -> this.showDebug.getValue()));
   public final Setting<Color> activeColor = this.color("ActiveColor", 100, 220, 255, 255);

   public RotationVisualizerModule() {
      super("RotVisualizer", "Debug overlay and config for server-rotation rendering.", Module.Category.RENDER);
   }

   @Override
   public void onDisable() {
      if (OyVey.rotationManager != null) {
         OyVey.rotationManager.setSmoothFactor(0.3F);
         OyVey.rotationManager.setMaxBodyDesync(60.0F);
         OyVey.rotationManager.setUseEasing(true);
         OyVey.rotationManager.setEasingPower(2.0F);
      }
   }

   @Override
   public void onTick() {
      RotationManager rm = OyVey.rotationManager;
      if (rm != null) {
         rm.setSmoothFactor(this.smoothFactor.getValue());
         rm.setMaxBodyDesync(this.maxBodyDesync.getValue());
         rm.setUseEasing(this.useEasing.getValue());
         rm.setEasingPower(this.easingPower.getValue());
      }
   }

   @Override
   public void onRender2D(Render2DEvent event) {
      if (this.showDebug.getValue() && !nullCheck()) {
         RotationManager rm = OyVey.rotationManager;
         if (rm != null) {
            GuiGraphicsExtractor gfx = event.getContext();
            float clientYaw = mc.player.getYRot();
            float clientPitch = mc.player.getXRot();
            float serverYaw = rm.getServerYaw();
            float serverPitch = rm.getServerPitch();
            float renderYaw = rm.getRenderYaw();
            float renderPitch = rm.getRenderPitch();
            boolean active = rm.hasServerRotation();
            int px = this.overlayX.getValue();
            int py = this.overlayY.getValue();
            int lh = 10;
            int pad = 3;
            int accentW = 2;
            String header = "RotVis";
            String clientLine = String.format("Client  Y:%7.1f  P:%6.1f", clientYaw, clientPitch);
            String serverLine = String.format("Server  Y:%7.1f  P:%6.1f%s", serverYaw, serverPitch, active ? "  [ACTIVE]" : "");
            String renderLine = String.format("Render  Y:%7.1f  P:%6.1f", renderYaw, renderPitch);
            int lineW = Math.max(Math.max(mc.font.width(header), mc.font.width(clientLine)), Math.max(mc.font.width(serverLine), mc.font.width(renderLine)));
            int totalW = accentW + pad + lineW + pad;
            int totalH = pad + lh + lh + lh + lh + pad;
            RenderUtil.roundedRect(gfx, px, py, totalW, totalH, 3, -1727724272);
            int accent = active ? this.activeColor.getValue().getRGB() : -5592406;
            gfx.fill(px, py + 3, px + accentW, py + totalH - 3, accent);
            int tx = px + accentW + pad;
            int ty = py + pad;
            gfx.text(mc.font, header, tx, ty, -1, true);
            ty += lh;
            gfx.text(mc.font, clientLine, tx, ty, -3355444, false);
            ty += lh;
            gfx.text(mc.font, serverLine, tx, ty, active ? this.activeColor.getValue().getRGB() | 0xFF000000 : -7829368, false);
            ty += lh;
            gfx.text(mc.font, renderLine, tx, ty, -2232577, false);
         }
      }
   }

   @Override
   public String getDisplayInfo() {
      RotationManager rm = OyVey.rotationManager;
      return rm != null && rm.hasServerRotation() ? String.format("%.0f\u00b0\u2192%.0f\u00b0", rm.getServerYaw(), rm.getRenderYaw()) : "idle";
   }
}
