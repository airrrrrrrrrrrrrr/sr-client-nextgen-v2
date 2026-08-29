package me.alpha432.oyvey.features.modules.render;

import java.awt.Color;
import java.util.UUID;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.HudModule;
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

public class TargetHUD extends HudModule {
   public final Setting<Boolean> modernMode = this.register(new Setting<>("ModernMode", true));
   private static final int PANEL_W = 130;
   private static final int PANEL_H = 50;
   private static final int MM_W = 165;
   private static final int MM_H = 52;
   private static final int MM_HEAD = 32;
   private static final int MM_PAD = 6;
   private float displayHealth = -1.0F;
   private double bpsPrevX;
   private double bpsPrevZ;
   private float bps = 0.0F;
   private UUID trackedUUID = null;

   public TargetHUD() {
      super("TargetHUD", "Shows target's head and health", 130.0F, 50.0F, Module.Category.RENDER);
   }

   @Override
   protected void render(Render2DEvent e) {
      super.render(e);
      if (mc.player != null && mc.level != null) {
         LivingEntity target = this.getTarget();
         if (target != null) {
            GuiGraphicsExtractor g = e.getContext();
            int x = (int)this.getX();
            int y = (int)this.getY();
            if (this.modernMode.getValue() && target instanceof AbstractClientPlayer acp) {
               this.drawModernMode(acp, g, x, y);
               this.setWidth(165.0F);
               this.setHeight(52.0F);
            } else {
               this.drawLegacyMode(target, g, x, y);
               this.setWidth(130.0F);
               this.setHeight(50.0F);
            }
         }
      }
   }

   public void drawModernMode(AbstractClientPlayer target, GuiGraphicsExtractor g, int x, int y) {
      UUID uid = target.getUUID();
      if (!uid.equals(this.trackedUUID)) {
         this.trackedUUID = uid;
         this.displayHealth = target.getHealth();
         this.bpsPrevX = target.getX();
         this.bpsPrevZ = target.getZ();
         this.bps = 0.0F;
      }

      double dX = target.getX() - this.bpsPrevX;
      double dZ = target.getZ() - this.bpsPrevZ;
      this.bps = (float)(Math.sqrt(dX * dX + dZ * dZ) * 20.0);
      this.bpsPrevX = target.getX();
      this.bpsPrevZ = target.getZ();
      float realHealth = target.getHealth();
      float maxHealth = Math.max(1.0F, target.getMaxHealth());
      this.displayHealth = this.displayHealth + (realHealth - this.displayHealth) * 0.1F;
      float healthPct = Mth.clamp(this.displayHealth / maxHealth, 0.0F, 1.0F);
      RenderUtil.roundedRect(g, x, y, 165, 52, 6, -871558631);
      RenderUtil.roundedRect(g, x + 1, y + 1, 163, 1, 0, 352321535);
      RenderUtil.roundedRect(g, x, y, 165, 52, 6, 637534207);
      int headX = x + 6;
      int headY = y + 10;
      Identifier skin = target.getSkin().body().texturePath();
      g.pose().pushMatrix();
      g.pose().translate(headX, headY);
      g.pose().scale(4.0F, 4.0F);
      g.blit(RenderPipelines.GUI_TEXTURED, skin, 0, 0, 8.0F, 8.0F, 8, 8, 64, 64);
      g.blit(RenderPipelines.GUI_TEXTURED, skin, 0, 0, 40.0F, 8.0F, 8, 8, 64, 64);
      g.pose().popMatrix();
      this.maskHeadCorners(g, headX, headY, 32, 3, -871558631);
      RenderUtil.roundedRect(g, headX - 1, headY - 1, 34, 34, 4, 1090519039);
      int textX = headX + 32 + 6;
      int nameY = y + 6 + 1;
      int bpsY = nameY + 9 + 2;
      g.text(mc.font, target.getName().getString(), textX, nameY, -1, true);
      g.text(mc.font, String.format("%.2f BPS", this.bps), textX, bpsY, -5592406, false);
      int barY = bpsY + 9 + 3;
      int barW = x + 165 - 6 - textX;
      int barH = 5;
      RenderUtil.roundedRect(g, textX, barY, barW, barH, barH / 2, 1428300322);
      int filled = Math.round(barW * healthPct);
      if (filled > 0) {
         RenderUtil.roundedRect(g, textX, barY, filled, barH, barH / 2, -16722712);
      }

      if (filled > 2 && filled < barW) {
         RenderUtil.roundedRect(g, textX + filled - 2, barY - 1, 4, barH + 2, (barH + 2) / 2, -855638017);
      }
   }

   private void maskHeadCorners(GuiGraphicsExtractor g, int hx, int hy, int size, int radius, int color) {
      RenderUtil.maskCorners(g, hx, hy, size, size, radius, color);
   }

   private void drawLegacyMode(LivingEntity target, GuiGraphicsExtractor g, int x, int y) {
      g.fill(x, y, x + 130, y + 50, new Color(15, 15, 15, 180).getRGB());
      g.fill(x, y, x + 130, y + 2, new Color(200, 50, 50, 220).getRGB());
      int headX = x + 5;
      int headY = y + 9;
      if (target instanceof AbstractClientPlayer acp) {
         Identifier skin = acp.getSkin().body().texturePath();
         g.pose().pushMatrix();
         g.pose().translate(headX, headY);
         g.pose().scale(4.0F, 4.0F);
         g.blit(RenderPipelines.GUI_TEXTURED, skin, 0, 0, 8.0F, 8.0F, 8, 8, 64, 64);
         g.blit(RenderPipelines.GUI_TEXTURED, skin, 0, 0, 40.0F, 8.0F, 8, 8, 64, 64);
         g.pose().popMatrix();
      } else {
         g.fill(headX, headY, headX + 32, headY + 32, new Color(160, 50, 50, 200).getRGB());
      }

      int textX = x + 43;
      g.text(mc.font, target.getName().getString(), textX, y + 9, Color.WHITE.getRGB(), true);
      float health = target.getHealth();
      float maxHealth = target.getMaxHealth();
      float ratio = Mth.clamp(health / maxHealth, 0.0F, 1.0F);
      int barY = y + 24;
      int barW = 82;
      int barH = 6;
      g.fill(textX, barY, textX + barW, barY + barH, new Color(40, 40, 40, 200).getRGB());
      g.fill(textX, barY, textX + Math.round(barW * ratio), barY + barH, this.legacyHealthColor(ratio));
      g.text(mc.font, String.format("%.1f / %.0f HP", health, maxHealth), textX, barY + 9, new Color(180, 180, 180).getRGB(), false);
   }

   private LivingEntity getTarget() {
      KillAura ka = OyVey.moduleManager.getModuleByClass(KillAura.class);
      if (ka != null && ka.isEnabled()) {
         Entity le = ka.getTarget();
         if (le instanceof LivingEntity) {
            return (LivingEntity)le;
         }
      }

      KillauraRecode kr = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      if (kr != null && kr.isEnabled()) {
         Entity var4 = kr.getTarget();
         if (var4 instanceof LivingEntity) {
            return (LivingEntity)var4;
         }
      }

      return null;
   }

   private int legacyHealthColor(float ratio) {
      int r;
      int gr;
      if (ratio > 0.5F) {
         r = (int)(255.0F * (1.0F - ratio) * 2.0F);
         gr = 200;
      } else {
         r = 220;
         gr = (int)(200.0F * ratio * 2.0F);
      }

      return new Color(Mth.clamp(r, 0, 255), Mth.clamp(gr, 0, 255), 0).getRGB();
   }
}
