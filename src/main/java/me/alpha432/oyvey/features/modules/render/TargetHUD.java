package me.alpha432.oyvey.features.modules.render;

import java.awt.Color;
import java.util.UUID;
import java.util.Locale;
import com.google.gson.JsonElement;
import me.alpha432.oyvey.features.gui.HudEditorScreen;
import me.alpha432.oyvey.util.render.HudFont;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EquipmentSlot;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.HudModule;
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
   public enum Mode { CLASSIC, MODERN, ADJUST }
   public final Setting<Mode> mode = register(new Setting<>("Mode", Mode.MODERN));
   public final Setting<Float> scale = register(new Setting<>("Scale", 1.0F, 0.5F, 3.0F));
   private static final EquipmentSlot[] EQUIPMENT = { EquipmentSlot.MAINHAND, EquipmentSlot.HEAD,
      EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
   private LivingEntity shown;
   private Object world;
   private final Tween ghost = new Tween(), fade = new Tween();
   private float lastRatio;
   private long ghostAfter;
   private float displayHealth = -1.0F;
   private float bps = 0.0F;
   private UUID trackedUUID = null;

   public TargetHUD() {
      super("TargetHUD", "Shows target's head and health", 130.0F, 50.0F, Module.Category.RENDER);
   }

   @Override public void fromJson(JsonElement element) {
      super.fromJson(element);
      if (element != null && element.isJsonObject() && !element.getAsJsonObject().has("Mode")
         && element.getAsJsonObject().has("ModernMode")) {
         mode.setValue(element.getAsJsonObject().get("ModernMode").getAsBoolean() ? Mode.MODERN : Mode.CLASSIC);
      }
   }

   @Override public void onDisable() { shown = null; trackedUUID = null; ghostAfter = 0; fade.snap(0); }

   @Override
   protected void render(Render2DEvent e) {
      if (world != mc.level) { world = mc.level; onDisable(); }
      if (mc.player == null || mc.level == null) return;
      long now = System.nanoTime() / 1_000_000L;
      LivingEntity target = getTarget();
      if (mc.screen instanceof HudEditorScreen || mc.screen instanceof ChatScreen) target = mc.player;
      if (target != null && (!target.isAlive() || target.isRemoved())) target = null;
      if (target != null && target != shown) {
         shown = target; lastRatio = ratio(target);
         ghost.snap(lastRatio); ghostAfter = 0;
      }
      fade.to(target == null ? 0 : 1, now, 250);
      float alpha = fade.value(now);
      if (target == null && alpha < 0.01F) { shown = null; ghostAfter = 0; }
      boolean modern = mode.getValue() == Mode.MODERN;
      int width = mode.getValue() == Mode.ADJUST
         ? shown == null ? 150 : Math.max(150, mc.font.width(HudFont.SEGOE.text("Name: " + cleanName(shown))) + 80)
         : modern ? 165 : 130;
      int height = mode.getValue() == Mode.ADJUST ? 35 : modern ? 52 : 50;
      float size = Mth.clamp(scale.getValue(), 0.5F, 3);
      setWidth(width * size); setHeight(height * size);
      super.render(e);
      if (shown == null || (mode.getValue() != Mode.ADJUST && target == null)) return;
      GuiGraphicsExtractor g = e.getContext();
      g.pose().pushMatrix();
      try {
         g.pose().translate(getX(), getY()); g.pose().scale(size, size);
         if (mode.getValue() == Mode.ADJUST) drawAdjust(g, width, alpha, now);
         else if (modern) drawModernMode(shown, g, 0, 0);
         else drawLegacyMode(shown, g, 0, 0);
      } finally { g.pose().popMatrix(); }
   }

   public void drawModernMode(LivingEntity target, GuiGraphicsExtractor g, int x, int y) {
      UUID uid = target.getUUID();
      if (!uid.equals(this.trackedUUID)) {
         this.trackedUUID = uid;
         this.displayHealth = target.getHealth();
         this.bps = 0.0F;
      }

      double dX = target.getX() - target.xo;
      double dZ = target.getZ() - target.zo;
      this.bps = (float)(Math.sqrt(dX * dX + dZ * dZ) * 20.0);
      float realHealth = target.getHealth();
      float maxHealth = Math.max(1.0F, target.getMaxHealth());
      this.displayHealth = this.displayHealth + (realHealth - this.displayHealth) * 0.1F;
      float healthPct = Mth.clamp(this.displayHealth / maxHealth, 0.0F, 1.0F);
      RenderUtil.roundedRect(g, x, y, 165, 52, 6, -871558631);
      RenderUtil.roundedRect(g, x + 1, y + 1, 163, 1, 0, 352321535);
      RenderUtil.roundedRect(g, x, y, 165, 52, 6, 637534207);
      int headX = x + 6;
      int headY = y + 10;
      this.portrait(target, g, headX, headY, 32, 1);
      this.maskHeadCorners(g, headX, headY, 32, 3, -871558631);
      RenderUtil.roundedRect(g, headX - 1, headY - 1, 34, 34, 4, 1090519039);
      int textX = headX + 32 + 6;
      int nameY = y + 6 + 1;
      int bpsY = nameY + 9 + 2;
      g.text(mc.font, mc.font.plainSubstrByWidth(target.getName().getString(), 115), textX, nameY, -1, true);
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
      g.text(mc.font, mc.font.plainSubstrByWidth(target.getName().getString(), 82), textX, y + 9, Color.WHITE.getRGB(), true);
      float health = target.getHealth();
      float maxHealth = Math.max(1, target.getMaxHealth());
      float ratio = Mth.clamp(health / maxHealth, 0.0F, 1.0F);
      int barY = y + 24;
      int barW = 82;
      int barH = 6;
      g.fill(textX, barY, textX + barW, barY + barH, new Color(40, 40, 40, 200).getRGB());
      g.fill(textX, barY, textX + Math.round(barW * ratio), barY + barH, this.legacyHealthColor(ratio));
      g.text(mc.font, String.format("%.1f / %.0f HP", health, maxHealth), textX, barY + 9, new Color(180, 180, 180).getRGB(), false);
   }

   private LivingEntity getTarget() {
      KillauraRecode kr = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      if (kr != null && kr.isEnabled()) {
         Entity var4 = kr.getTarget();
         if (var4 instanceof LivingEntity) {
            return (LivingEntity)var4;
         }
      }

      return null;
   }

   private void drawAdjust(GuiGraphicsExtractor g, int width, float alpha, long now) {
      float next = ratio(shown);
      if (next != lastRatio) {
         if (next < lastRatio) ghostAfter = now + 300;
         else { ghostAfter = 0; ghost.snap(next); }
         lastRatio = next;
      }
      if (ghostAfter != 0 && now >= ghostAfter) { ghost.to(lastRatio, now, 200); ghostAfter = 0; }
      g.fill(0, 0, width, 35, tint(0x801A1A1A, alpha));
      portrait(shown, g, 3, 3, 25, alpha);
      g.text(mc.font, HudFont.SEGOE.text(cleanName(shown)), 30, 3, tint(-1, alpha), true);
      var healthLabel = HudFont.SEGOE.text("♥ ").withStyle(style -> style.withColor(0xF47A8B))
         .append(HudFont.SEGOE.text(String.format(Locale.ROOT, "%.1f", Math.max(0, shown.getHealth())))
            .withStyle(style -> style.withColor(0xFFFFFF)));
      int diffX = width - 3 - mc.font.width(healthLabel);
      g.text(mc.font, healthLabel, diffX, 18, tint(-1, alpha), true);
      int iconX = 30;
      for (EquipmentSlot slot : EQUIPMENT) {
         var stack = shown.getItemBySlot(slot);
         if (stack.isEmpty()) continue;
         if (iconX + 14 > diffX - 3) break;
         if (alpha > 0.1F) {
            g.pose().pushMatrix();
            g.pose().translate(iconX, 15); g.pose().scale(0.875F, 0.875F);
            g.item(shown, stack, 0, 0, slot.ordinal());
            g.pose().popMatrix();
         }
         iconX += 16;
      }
      int barWidth = width - 5;
      int base = OyVey.colorManager.getColorAsInt() | 0xFF000000;
      g.fill(3, 30, width - 2, 33, tint(0xFF262626, alpha));
      g.fill(3, 30, 3 + Math.round(barWidth * ghost.value(now)), 33, tint(0xFF555555, alpha));
      // The bright line always shows the latest remaining HP/max HP; the
      // subdued trailing segment alone animates recently lost health.
      int filled = Math.round(barWidth * next);
      for (int px = 0; px < filled; px++) {
         float wave = (float)(0.75 + 0.25 * Math.sin(now / 500.0 + px / 50.0));
         g.fill(3 + px, 30, 4 + px, 33, tint(darken(base, wave), alpha));
      }
   }

   private void portrait(LivingEntity target, GuiGraphicsExtractor g, int x, int y, int size, float alpha) {
      if (target instanceof AbstractClientPlayer player) {
         var skin = player.getSkin().body().texturePath();
         g.pose().pushMatrix();
         g.pose().translate(x, y); g.pose().scale(size / 8F, size / 8F);
         g.blit(RenderPipelines.GUI_TEXTURED, skin, 0, 0, 8F, 8F, 8, 8, 64, 64, tint(-1, alpha));
         g.blit(RenderPipelines.GUI_TEXTURED, skin, 0, 0, 40F, 8F, 8, 8, 64, 64, tint(-1, alpha));
         g.pose().popMatrix();
      } else {
         InventoryScreen.extractEntityInInventoryFollowsMouse(g, x, y, x + size, y + size,
            Math.max(8, size / 2), 0.0625F, x + size / 2F, y + size / 2F, target);
      }
   }

   private static String cleanName(LivingEntity target) { return target.getName().getString().replaceAll("§[0-9a-fk-or]", ""); }
   private static float ratio(LivingEntity target) { return Mth.clamp(target.getHealth() / Math.max(1, target.getMaxHealth()), 0, 1); }
   private static int tint(int color, float alpha) { return (color & 0xFFFFFF) | Math.round((color >>> 24) * alpha) << 24; }
   private static int darken(int color, float factor) {
      return 0xFF000000 | Math.round((color >> 16 & 255) * factor) << 16
         | Math.round((color >> 8 & 255) * factor) << 8 | Math.round((color & 255) * factor);
   }

   private static final class Tween {
      float from, end;
      long start, duration;
      float value(long now) {
         float t = duration == 0 ? 1 : Mth.clamp((now - start) / (float)duration, 0, 1);
         return from + (end - from) * (1 - (1 - t) * (1 - t) * (1 - t));
      }
      void snap(float value) { from = end = value; duration = 0; }
      void to(float value, long now, long time) {
         if (value == end) return;
         from = value(now); end = value; start = now; duration = time;
      }
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
