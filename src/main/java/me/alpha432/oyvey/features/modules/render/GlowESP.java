package me.alpha432.oyvey.features.modules.render;

import java.awt.Color;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.ColorUtil;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class GlowESP extends Module {
   public final Setting<Boolean> players = this.register(new Setting<>("Players", true));
   public final Setting<Boolean> targetOnly = this.register(new Setting<>("TargetOnly", false));
   public final Setting<Boolean> rainbow = this.register(new Setting<>("Rainbow", true));
   public final Setting<Color> color = this.color("Color", 90, 160, 255, 255);
   public final Setting<Integer> layers = this.register(new Setting<>("Layers", 6, 1, 12));
   public final Setting<Float> spread = this.num("Spread", 0.35F, 0.05F, 1.0F);
   public final Setting<Integer> alpha = this.register(new Setting<>("Alpha", 55, 1, 150));

   public GlowESP() {
      super("GlowESP", "Soft glowing halo around players (shader-free).", Module.Category.RENDER);
   }

   @Subscribe
   @Override
   public void onRender3D(Render3DEvent event) {
      if (mc.player != null && mc.level != null) {
         Entity target = this.currentTarget();

         for (Player p : mc.level.players()) {
            if (p != mc.player && p.isAlive() && this.players.getValue() && (!this.targetOnly.getValue() || p == target)) {
               this.glow(event, p, this.colorFor(p));
            }
         }
      }
   }

   private void glow(Render3DEvent event, Entity e, Color base) {
      int n = this.layers.getValue();

      for (int i = 0; i < n; i++) {
         float t = (float)i / n;
         double inflate = t * this.spread.getValue();
         int a = (int)(this.alpha.getValue().intValue() * (1.0F - t));
         if (a > 0) {
            RenderUtil.drawBoxFilled(event.getMatrix(), RenderUtil.interpolatedBox(e, event.getDelta()).inflate(inflate), ColorUtil.withAlpha(base, a));
         }
      }
   }

   private Color colorFor(Entity e) {
      return this.rainbow.getValue() ? ColorUtil.rainbow(e.getId() * 40) : this.color.getValue();
   }

   private Entity currentTarget() {
      KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      return aura != null && aura.isEnabled() ? aura.getTarget() : null;
   }
}
