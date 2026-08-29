package me.alpha432.oyvey.features.modules.ghost;

import java.awt.Color;
import java.util.concurrent.ThreadLocalRandom;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.SrMusicModule;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public class BannabellPepper extends Module {
   public final Setting<Float> speed = this.register(new Setting<>("Speed", 2.0F, 0.5F, 10.0F));
   public final Setting<Integer> heartCount = this.register(new Setting<>("Hearts", 20, 3, 50));
   public final Setting<Color> heartColor = this.register(new Setting<>("Color", new Color(255, 80, 130, 220)));
   private float[][] heartDefs;
   private long startMs;
   private int lastCount = -1;

   public BannabellPepper() {
      super("BannabellPepper", "Flashes your screen with hearts.", Module.Category.GHOST);
   }

   @Override
   public void onEnable() {
      this.startMs = System.currentTimeMillis();
      this.heartDefs = null;
      this.lastCount = -1;
      SrMusicModule srMusic = OyVey.moduleManager.getModuleByClass(SrMusicModule.class);
      if (srMusic != null) {
         if (srMusic.getCheckStatus() == SrMusicModule.CheckStatus.UNCHECKED) {
            srMusic.runCheck();
         }

         srMusic.stopPlayback();
         srMusic.addToQueue("George Michael Careless Whisper");
      }
   }

   @Override
   public void onDisable() {
      SrMusicModule srMusic = OyVey.moduleManager.getModuleByClass(SrMusicModule.class);
      if (srMusic != null) {
         srMusic.stopPlayback();
      }
   }

   @Override
   public void onRender2D(Render2DEvent event) {
      if (mc.player != null) {
         GuiGraphicsExtractor gfx = event.getContext();
         int sw = mc.getWindow().getGuiScaledWidth();
         int sh = mc.getWindow().getGuiScaledHeight();
         if (this.heartDefs == null || this.lastCount != this.heartCount.getValue()) {
            this.rebuildHearts();
         }

         float t = (float)(System.currentTimeMillis() - this.startMs) / 1000.0F;
         float spd = this.speed.getValue();
         float pulse = (Mth.sin(t * spd * 3.14159F) + 1.0F) * 0.5F;
         Color base = this.heartColor.getValue();
         int r = base.getRed();
         int g = base.getGreen();
         int b = base.getBlue();
         int flashAlpha = (int)(pulse * 60.0F);
         gfx.fill(0, 0, sw, sh, flashAlpha << 24 | r << 16 | g << 8 | b);
         String heart = "\u2665";

         for (float[] def : this.heartDefs) {
            float xFrac = def[0];
            float phase = def[1];
            float driftMul = def[2];
            float drift = (t * spd * driftMul * 25.0F + phase * sh) % sh;
            int hx = (int)(xFrac * (sw - 8));
            int hy = (int)(sh - drift);
            float lifeFrac = drift / sh;
            float fadeAlpha = (1.0F - lifeFrac) * 0.85F + 0.15F;
            float beatPulse = (Mth.sin(t * spd * 6.28F + phase) + 1.0F) * 0.5F * 0.4F + 0.6F;
            int alpha = Mth.clamp((int)(fadeAlpha * beatPulse * 230.0F), 0, 255);
            gfx.text(mc.font, heart, hx, hy, alpha << 24 | r << 16 | g << 8 | b, true);
         }
      }
   }

   private void rebuildHearts() {
      int count = this.heartCount.getValue();
      this.lastCount = count;
      this.heartDefs = new float[count][3];
      ThreadLocalRandom rng = ThreadLocalRandom.current();

      for (int i = 0; i < count; i++) {
         this.heartDefs[i][0] = rng.nextFloat();
         this.heartDefs[i][1] = rng.nextFloat() * 100.0F;
         this.heartDefs[i][2] = 0.5F + rng.nextFloat() * 1.5F;
      }
   }
}
