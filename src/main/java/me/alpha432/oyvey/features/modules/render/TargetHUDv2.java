package me.alpha432.oyvey.features.modules.render;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.HudModule;
import me.alpha432.oyvey.features.modules.combat.GrimAura;
import me.alpha432.oyvey.features.modules.combat.KillAura;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class TargetHUDv2 extends HudModule {
   public final Setting<Integer> sparkCount = this.num("SparkCount", 18, 0, 60);
   public final Setting<Float> sparkSpeed = this.num("SparkSpeed", 1.8F, 0.3F, 5.0F);
   public final Setting<Float> sparkLife = this.num("SparkLife", 1.0F, 0.2F, 3.0F);
   public final Setting<Boolean> sparkGravity = this.bool("SparkGravity", true);
   public final Setting<Boolean> rainbowSpark = this.bool("RainbowSpark", false);
   public final Setting<Boolean> showBps = this.bool("ShowBPS", true);
   private static final int W = 200;
   private static final int H = 62;
   private static final int RADIUS = 12;
   private static final int HEAD = 36;
   private static final int PAD = 8;
   private float displayHealth = -1.0F;
   private float prevHealth = -1.0F;
   private double bpsPrevX;
   private double bpsPrevZ;
   private float bps = 0.0F;
   private UUID trackedUUID = null;
   private long lastHitMs = 0L;
   private final List<TargetHUDv2.Spark> sparks = new java.util.ArrayList<>();

   public TargetHUDv2() {
      super("TargetHUDv2", "Premium rounded target display with hit sparks.", 200.0F, 62.0F, Module.Category.RENDER);
   }

   @Override
   protected void render(Render2DEvent e) {
      super.render(e);
      if (mc.player != null && mc.level != null) {
         LivingEntity target = this.getTarget();
         GuiGraphicsExtractor g = e.getContext();
         int x = (int)this.getX();
         int y = (int)this.getY();
         this.tickAndDrawSparks(g);
         if (target != null) {
            this.drawPanel(g, x, y, target);
         }
      }
   }

   private void drawPanel(GuiGraphicsExtractor g, int x, int y, LivingEntity target) {
      int accent = OyVey.colorManager.getColorAsInt();
      int aR = accent >> 16 & 0xFF;
      int aG = accent >> 8 & 0xFF;
      int aB = accent & 0xFF;
      UUID uid = target.getUUID();
      if (!uid.equals(this.trackedUUID)) {
         this.trackedUUID = uid;
         this.displayHealth = target.getHealth();
         this.prevHealth = target.getHealth();
         this.bpsPrevX = target.getX();
         this.bpsPrevZ = target.getZ();
         this.bps = 0.0F;
      }

      float realHealth = target.getHealth();
      float maxHealth = Math.max(1.0F, target.getMaxHealth());
      if (this.displayHealth < 0.0F) {
         this.displayHealth = realHealth;
      }

      this.displayHealth = this.displayHealth + (realHealth - this.displayHealth) * 0.12F;
      float healthPct = Mth.clamp(this.displayHealth / maxHealth, 0.0F, 1.0F);
      if (this.showBps.getValue()) {
         double dX = target.getX() - this.bpsPrevX;
         double dZ = target.getZ() - this.bpsPrevZ;
         this.bps = (float)(Math.sqrt(dX * dX + dZ * dZ) * 20.0);
         this.bpsPrevX = target.getX();
         this.bpsPrevZ = target.getZ();
      }

      if (this.prevHealth > 0.0F && realHealth < this.prevHealth - 0.05F) {
         this.lastHitMs = System.currentTimeMillis();
         int barX = x + 8 + 36 + 8;
         int barW = 200 - barX - 8 - x;
         int barY = y + 31 + 4;
         int barH = 5;
         int sparkX = barX + Math.round(barW * healthPct);
         int sparkY = barY + barH / 2;
         this.spawnSparks(sparkX, sparkY, accent);
      }

      this.prevHealth = realHealth;
      long timeSinceHit = System.currentTimeMillis() - this.lastHitMs;
      if (timeSinceHit < 200L) {
         float glow = 1.0F - (float)timeSinceHit / 200.0F;
         int glowAlpha = (int)(glow * 40.0F);
         RenderUtil.roundedRect(g, x - 2, y - 2, 204, 66, 14, glowAlpha << 24 | aR << 16 | aG << 8 | aB);
      }

      RenderUtil.roundedRect(g, x, y, 200, 62, 12, -402191080);
      RenderUtil.roundedRect(g, x + 1, y + 1, 198, 1, 0, 419430399);
      RenderUtil.roundedRect(g, x, y, 200, 62, 12, 553648127);
      RenderUtil.roundedRect(g, x + 12, y, 176, 2, 0, 0xFF000000 | accent);
      int headX = x + 8;
      int headY = y + 13;
      if (target instanceof AbstractClientPlayer acp) {
         Identifier skin = acp.getSkin().body().texturePath();
         g.pose().pushMatrix();
         g.pose().translate(headX, headY);
         float scale = 4.5F;
         g.pose().scale(scale, scale);
         g.blit(RenderPipelines.GUI_TEXTURED, skin, 0, 0, 8.0F, 8.0F, 8, 8, 64, 64);
         g.blit(RenderPipelines.GUI_TEXTURED, skin, 0, 0, 40.0F, 8.0F, 8, 8, 64, 64);
         g.pose().popMatrix();
         this.maskCorners(g, headX, headY, 36, 5, -402191080);
      } else {
         RenderUtil.roundedRect(g, headX, headY, 36, 36, 5, -13417387);
      }

      RenderUtil.roundedRect(g, headX - 1, headY - 1, 38, 38, 6, 0xFF000000 | accent);
      RenderUtil.roundedRect(g, headX - 1, headY - 1, 38, 38, 6, 822083583);
      int textX = headX + 36 + 8;
      int nameY = y + 8;
      int hpY = nameY + 9 + 2;
      g.text(mc.font, target.getName().getString(), textX, nameY, -1, true);
      String hpStr = String.format("%.1f / %.0f", realHealth, maxHealth);
      int hpW = mc.font.width(hpStr);
      g.text(mc.font, hpStr, x + 200 - 8 - hpW, nameY, this.healthColor(healthPct), false);
      int barY = hpY + 9 + 2;
      int barW = 200 - textX - 8 - x;
      int barH = 5;
      RenderUtil.roundedRect(g, textX, barY, barW, barH, barH / 2, 1143087667);
      int fillW = Math.round(barW * healthPct);
      if (fillW > 0) {
         int fillColor = this.healthGradient(healthPct, aR, aG, aB);
         RenderUtil.roundedRect(g, textX, barY, fillW, barH, barH / 2, fillColor);
         if (fillW > 3 && fillW < barW) {
            RenderUtil.roundedRect(g, textX + fillW - 2, barY - 1, 4, barH + 2, (barH + 2) / 2, -520093697);
         }
      }

      if (this.showBps.getValue()) {
         String bpsStr = String.format("%.1f BPS", this.bps);
         int bpsW = mc.font.width(bpsStr);
         g.text(mc.font, bpsStr, x + 200 - 8 - bpsW, barY - 9 - 1, -11180425, false);
      }
   }

   private void spawnSparks(int cx, int cy, int accent) {
      int count = this.sparkCount.getValue();
      if (count > 0) {
         float speedMul = this.sparkSpeed.getValue();
         float lifeMul = this.sparkLife.getValue();
         ThreadLocalRandom rng = ThreadLocalRandom.current();

         for (int i = 0; i < count; i++) {
            double angle = rng.nextDouble() * 3.141592653589793 * 2.0;
            float spd = (0.4F + rng.nextFloat() * 1.4F) * speedMul;
            float vx = (float)(Math.cos(angle) * spd);
            float vy = (float)(Math.sin(angle) * spd) - rng.nextFloat() * 0.8F * speedMul;
            float decay = (0.018F + rng.nextFloat() * 0.025F) / lifeMul;
            float size = 1.0F + rng.nextFloat() * 1.5F;
            int color;
            if (this.rainbowSpark.getValue()) {
               float hue = rng.nextFloat();
               color = Color.getHSBColor(hue, 1.0F, 1.0F).getRGB();
            } else {
               float shift = rng.nextFloat() * 0.15F;
               float hue = ((accent >> 16 & 0xFF) > 100 ? 0.06F : 0.55F) + shift;
               float sat = 0.7F + rng.nextFloat() * 0.3F;
               color = Color.getHSBColor(hue, sat, 1.0F).getRGB();
            }

            this.sparks.add(new TargetHUDv2.Spark(cx, cy, vx, vy, decay, color, size));
         }
      }
   }

   private void tickAndDrawSparks(GuiGraphicsExtractor g) {
      Iterator<TargetHUDv2.Spark> it = this.sparks.iterator();

      while (it.hasNext()) {
         TargetHUDv2.Spark s = it.next();
         s.update(this.sparkGravity.getValue());
         if (s.dead()) {
            it.remove();
         } else {
            int c = s.color();
            int sz = Math.max(1, (int)(s.size * s.life));
            int px = (int)s.x;
            int py = (int)s.y;
            g.fill(px, py, px + sz, py + sz, c);
            if (s.life > 0.5F && sz > 1) {
               int coreAlpha = (int)((s.life - 0.5F) * 2.0F * 200.0F);
               g.fill(px, py, px + 1, py + 1, coreAlpha << 24 | 16777215);
            }
         }
      }
   }

   private void maskCorners(GuiGraphicsExtractor g, int hx, int hy, int size, int r, int bg) {
      RenderUtil.maskCorners(g, hx, hy, size, size, r, bg);
   }

   private int healthGradient(float pct, int aR, int aG, int aB) {
      if (pct > 0.5F) {
         float t = (pct - 0.5F) * 2.0F;
         int r = (int)(255.0F * (1.0F - t) + aR * t);
         int gr = (int)(200.0F * (1.0F - t) + aG * t);
         int b = (int)(0.0F * (1.0F - t) + aB * t);
         return 0xFF000000 | Mth.clamp(r, 0, 255) << 16 | Mth.clamp(gr, 0, 255) << 8 | Mth.clamp(b, 0, 255);
      } else {
         float t = pct * 2.0F;
         int r = 220;
         int gr = (int)(160.0F * t);
         return 0xFF000000 | r << 16 | Mth.clamp(gr, 0, 255) << 8;
      }
   }

   private int healthColor(float pct) {
      if (pct > 0.6F) {
         return -11149995;
      } else {
         return pct > 0.3F ? -2241468 : -2276284;
      }
   }

   private LivingEntity getTarget() {
      KillAura ka = OyVey.moduleManager.getModuleByClass(KillAura.class);
      if (ka != null && ka.isEnabled()) {
         Entity ga = ka.getTarget();
         if (ga instanceof LivingEntity) {
            return (LivingEntity)ga;
         }
      }

      KillauraRecode kr = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      if (kr != null && kr.isEnabled()) {
         Entity le = kr.getTarget();
         if (le instanceof LivingEntity) {
            return (LivingEntity)le;
         }
      }

      GrimAura ga = OyVey.moduleManager.getModuleByClass(GrimAura.class);
      if (ga != null && ga.isEnabled()) {
         Entity var5 = ga.getTarget();
         if (var5 instanceof LivingEntity) {
            return (LivingEntity)var5;
         }
      }

      return null;
   }

   private static final class Spark {
      float x;
      float y;
      float vx;
      float vy;
      float life;
      float decay;
      int baseColor;
      float size;

      Spark(float x, float y, float vx, float vy, float decay, int color, float size) {
         this.x = x;
         this.y = y;
         this.vx = vx;
         this.vy = vy;
         this.life = 1.0F;
         this.decay = decay;
         this.baseColor = color;
         this.size = size;
      }

      void update(boolean gravity) {
         this.x = this.x + this.vx;
         this.y = this.y + this.vy;
         if (gravity) {
            this.vy += 0.12F;
         }

         this.vx *= 0.9F;
         this.vy *= 0.96F;
         this.life = this.life - this.decay;
      }

      boolean dead() {
         return this.life <= 0.0F;
      }

      int color() {
         int a = (int)(this.life * 255.0F);
         return a << 24 | this.baseColor & 16777215;
      }
   }
}
