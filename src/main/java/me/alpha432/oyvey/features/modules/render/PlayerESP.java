package me.alpha432.oyvey.features.modules.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.Layers;
import me.alpha432.oyvey.util.render.RenderUtil;
import me.alpha432.oyvey.util.render.WorldRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PlayerESP extends Module {
   public final Setting<Boolean> modeBox = this.bool("Box", true);
   public final Setting<Boolean> modeCorner = this.bool("Corner", false);
   public final Setting<Boolean> mode2D = this.bool("2D", false);
   public final Setting<Boolean> modeFilled = this.bool("Filled", false);
   public final Setting<Boolean> modeHealthbar = this.bool("Healthbar", false);
   public final Setting<Boolean> modeNametag = this.bool("Nametag", false);
   public final Setting<Boolean> modePenis = this.bool("PenisESP", false);
   public final Setting<Float> penisSize = this.register(new Setting<>("PenisSize", 1.0F, 0.1F, 5.0F, v -> this.modePenis.getValue()));
   public final Setting<Color> penisColor = this.register(new Setting<>("PenisColor", new Color(255, 200, 150, 230), v -> this.modePenis.getValue()));
   public final Setting<Boolean> modeTrack = this.bool("Track", false);
   public final Setting<Integer> trackLength = this.register(new Setting<>("TrackLength", 50, 5, 200, v -> this.modeTrack.getValue()));
   public final Setting<Float> trackWidth = this.register(new Setting<>("TrackWidth", 2.0F, 0.5F, 5.0F, v -> this.modeTrack.getValue()));
   public final Setting<Boolean> trackFade = this.register(new Setting<>("TrackFade", true, v -> this.modeTrack.getValue()));
   public final Setting<Color> trackColor = this.color("TrackColor", 0, 180, 255, 200);
   private final Map<UUID, ArrayDeque<Vec3>> trackHistory = new HashMap<>();
   public final Setting<Boolean> showPlayers = this.bool("Players", true);
   public final Setting<Boolean> showMobs = this.bool("Mobs", false);
   public final Setting<Boolean> showAnimals = this.bool("Animals", false);
   public final Setting<Boolean> showInvisibles = this.bool("Invisibles", true);
   public final Setting<Boolean> showFriends = this.bool("Friends", false);
   public final Setting<Boolean> hideSelf = this.bool("HideSelf", true);
   public final Setting<Color> color = this.color("Color", 0, 150, 255, 200);
   public final Setting<Color> friendColor = this.color("FriendColor", 0, 255, 100, 200);
   public final Setting<Float> lineWidth = this.num("LineWidth", 1.5F, 0.5F, 4.0F);
   public final Setting<Integer> filledAlpha = this.register(new Setting<>("FilledAlpha", 30, 0, 80, v -> this.modeFilled.getValue()));
   public final Setting<Boolean> rainbow = this.bool("Rainbow", false);
   public final Setting<Boolean> scaleWithDist = this.bool("DistScale", false);
   public final Setting<Integer> range = this.num("Range", 64, 4, 256);

   public PlayerESP() {
      super("PlayerESP", "Highlights nearby entities.", Module.Category.RENDER);
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.level != null && this.modeTrack.getValue()) {
         this.trackHistory.keySet().removeIf(uuid -> mc.level.getEntity(uuid) == null);

         for (Entity entity : this.getTargets()) {
            ArrayDeque<Vec3> trail = this.trackHistory.computeIfAbsent(entity.getUUID(), k -> new ArrayDeque<>());
            Vec3 pos = entity.position();
            if (trail.isEmpty() || trail.peekLast().distanceToSqr(pos) > 0.0025) {
               trail.addLast(pos);

               while (trail.size() > this.trackLength.getValue()) {
                  trail.pollFirst();
               }
            }
         }
      }
   }

   @Override
   public void onDisable() {
      this.trackHistory.clear();
   }

   @Subscribe
   @Override
   public void onRender3D(Render3DEvent event) {
      if (mc.player != null && mc.level != null) {
         for (Entity entity : this.getTargets()) {
            AABB box = RenderUtil.interpolatedBox(entity, event.getDelta());
            Color c = this.getColor(entity);
            if (this.modeBox.getValue()) {
               float lw = this.scaledWidth(entity);
               RenderUtil.drawBox(event.getMatrix(), box, c, lw);
            }

            if (this.modeCorner.getValue()) {
               this.drawCorners3D(event.getMatrix(), box, c, this.scaledWidth(entity));
            }

            if (this.modeFilled.getValue()) {
               Color fill = new Color(c.getRed(), c.getGreen(), c.getBlue(), this.filledAlpha.getValue());
               RenderUtil.drawBoxFilled(event.getMatrix(), box, fill);
            }

            if (this.modePenis.getValue()) {
               this.drawPenis3D(event.getMatrix(), entity, event.getDelta());
            }

            if (this.modeTrack.getValue()) {
               this.drawTrack3D(event.getMatrix(), entity);
            }
         }
      }
   }

   @Subscribe
   @Override
   public void onRender2D(Render2DEvent event) {
      if (mc.player != null
         && mc.level != null
         && (this.mode2D.getValue() || this.modeHealthbar.getValue() || this.modeNametag.getValue() || this.modePenis.getValue())) {
         GuiGraphicsExtractor gfx = event.getContext();

         for (Entity entity : this.getTargets()) {
            if (entity instanceof LivingEntity le) {
               AABB box = RenderUtil.interpolatedBox(entity, event.getDelta());
               Color c = this.getColor(entity);
               float[] r = WorldRenderer.aabbToScreen(box);
               if (r != null) {
                  float x1 = r[0];
                  float y1 = r[1];
                  float x2 = r[2];
                  float y2 = r[3];
                  if (!(x1 >= x2) && !(y1 >= y2)) {
                     int ci = c.getRGB();
                     if (this.mode2D.getValue()) {
                        this.draw2DBox(gfx, x1, y1, x2, y2, ci);
                     }

                     if (this.modeHealthbar.getValue()) {
                        this.drawHealthbar(gfx, le, x1, y1, x2, y2);
                     }

                     if (this.modeNametag.getValue()) {
                        this.drawNametag(gfx, entity, x1, x2, y1, ci);
                     }

                     if (this.modePenis.getValue()) {
                        this.drawPenis(gfx, x1, y1, x2, y2);
                     }
                  }
               }
            }
         }
      }
   }

   private List<Entity> getTargets() {
      Vec3 eye = mc.player.getEyePosition();
      double r = this.range.getValue().intValue();
      List<Entity> out = new java.util.ArrayList<>();

      for (Entity e : mc.level.entitiesForRendering()) {
         if ((e != mc.player || !this.hideSelf.getValue()) && e.isAlive() && !(eye.distanceTo(e.position()) > r) && this.passesFilter(e)) {
            out.add(e);
         }
      }

      return out;
   }

   private boolean passesFilter(Entity e) {
      boolean var10000;
      if (e instanceof LivingEntity le && le.hasEffect(MobEffects.INVISIBILITY)) {
         var10000 = true;
      } else {
         var10000 = false;
      }

      boolean invisible = var10000;
      if (invisible && !this.showInvisibles.getValue()) {
         return false;
      }

      if (e instanceof Player p) {
         if (p == mc.player && this.hideSelf.getValue()) {
            return false;
         }

         boolean isFriend = OyVey.friendManager.isFriend(p);
         return isFriend ? this.showFriends.getValue() : this.showPlayers.getValue();
      } else if (e instanceof Animal) {
         return this.showAnimals.getValue();
      } else {
         return e instanceof Mob ? this.showMobs.getValue() : false;
      }
   }

   private Color getColor(Entity e) {
      if (e instanceof Player p && OyVey.friendManager.isFriend(p) && this.showFriends.getValue()) {
         return this.friendColor.getValue();
      } else if (this.rainbow.getValue()) {
         float hue = (float)((System.currentTimeMillis() / 15L + e.getId() * 40L) % 3600L) / 3600.0F;
         Color hc = Color.getHSBColor(hue, 1.0F, 1.0F);
         return new Color(hc.getRed(), hc.getGreen(), hc.getBlue(), this.color.getValue().getAlpha());
      } else {
         return this.color.getValue();
      }
   }

   private float scaledWidth(Entity e) {
      if (!this.scaleWithDist.getValue()) {
         return this.lineWidth.getValue();
      }

      float dist = mc.player.distanceTo(e);
      float scale = Mth.clamp(1.0F - dist / this.range.getValue().intValue(), 0.2F, 1.0F);
      return this.lineWidth.getValue() * scale;
   }

   private void draw2DBox(GuiGraphicsExtractor g, float x1, float y1, float x2, float y2, int c) {
      int x1i = (int)x1;
      int y1i = (int)y1;
      int x2i = (int)x2;
      int y2i = (int)y2;
      g.fill(x1i, y1i, x2i, y1i + 1, c);
      g.fill(x1i, y2i - 1, x2i, y2i, c);
      g.fill(x1i, y1i, x1i + 1, y2i, c);
      g.fill(x2i - 1, y1i, x2i, y2i, c);
   }

   private void drawCorners3D(PoseStack stack, AABB box, Color c, float lw) {
      double lenX = (box.maxX - box.minX) * 0.25;
      double lenY = (box.maxY - box.minY) * 0.25;
      double lenZ = (box.maxZ - box.minZ) * 0.25;
      double[][] corners = new double[][]{
         {box.minX, box.minY, box.minZ},
         {box.maxX, box.minY, box.minZ},
         {box.minX, box.maxY, box.minZ},
         {box.maxX, box.maxY, box.minZ},
         {box.minX, box.minY, box.maxZ},
         {box.maxX, box.minY, box.maxZ},
         {box.minX, box.maxY, box.maxZ},
         {box.maxX, box.maxY, box.maxZ}
      };
      int[][] signs = new int[][]{{1, 1, 1}, {-1, 1, 1}, {1, -1, 1}, {-1, -1, 1}, {1, 1, -1}, {-1, 1, -1}, {1, -1, -1}, {-1, -1, -1}};

      for (int i = 0; i < 8; i++) {
         double cx = corners[i][0];
         double cy = corners[i][1];
         double cz = corners[i][2];
         int sx = signs[i][0];
         int sy = signs[i][1];
         int sz = signs[i][2];
         AABB edgeX = new AABB(cx, cy, cz, cx + sx * lenX, cy, cz);
         AABB edgeY = new AABB(cx, cy, cz, cx, cy + sy * lenY, cz);
         AABB edgeZ = new AABB(cx, cy, cz, cx, cy, cz + sz * lenZ);
         RenderUtil.drawBox(stack, edgeX, c, lw);
         RenderUtil.drawBox(stack, edgeY, c, lw);
         RenderUtil.drawBox(stack, edgeZ, c, lw);
      }
   }

   private void drawHealthbar(GuiGraphicsExtractor g, LivingEntity le, float x1, float y1, float x2, float y2) {
      float health = le.getHealth();
      float maxHealth = Math.max(1.0F, le.getMaxHealth());
      float ratio = Mth.clamp(health / maxHealth, 0.0F, 1.0F);
      int barX = (int)x2 + 1;
      int barY = (int)y1;
      int barH = (int)(y2 - y1);
      int barW = 3;
      g.fill(barX, barY, barX + barW, barY + barH, -2013265920);
      int filled = Math.round(barH * ratio);
      int gr = Mth.clamp((int)(255.0F * ratio * 2.0F), 0, 255);
      int r = Mth.clamp((int)(255.0F * (1.0F - ratio) * 2.0F), 0, 255);
      int healthColor = new Color(r, gr, 0, 220).getRGB();
      g.fill(barX, barY + barH - filled, barX + barW, barY + barH, healthColor);
      g.fill(barX - 1, barY - 1, barX + barW + 1, barY, -16777216);
      g.fill(barX - 1, barY + barH, barX + barW + 1, barY + barH + 1, -16777216);
      g.fill(barX - 1, barY, barX, barY + barH, -16777216);
      g.fill(barX + barW, barY, barX + barW + 1, barY + barH, -16777216);
   }

   private void drawPenis3D(PoseStack stack, Entity entity, float partialTick) {
      float s = this.penisSize.getValue();
      Color pc = this.penisColor.getValue();
      Vec3 interpolated = RenderUtil.interpolatedPosition(entity, partialTick);
      double ex = interpolated.x;
      double ey = interpolated.y;
      double ez = interpolated.z;
      double eh = entity.getBbHeight();
      double crotchY = ey + eh * 0.35;
      double yaw = Math.toRadians(-entity.getYRot());
      double fwdX = Math.sin(yaw);
      double fwdZ = -Math.cos(yaw);
      double rgtX = Math.cos(yaw);
      double rgtZ = Math.sin(yaw);
      double shaftR = 0.065 * s;
      double shaftLen = 0.42 * s;
      double tipX = ex + fwdX * shaftLen;
      double tipZ = ez + fwdZ * shaftLen;
      double headR = 0.1 * s;
      double ballR = 0.075 * s;
      double ballOff = shaftR * 1.9;
      Color dark = new Color(Math.max(0, pc.getRed() - 35), Math.max(0, pc.getGreen() - 35), Math.max(0, pc.getBlue() - 35), pc.getAlpha());
      RenderUtil.drawCylinderOriented(stack, ex, crotchY, ez, tipX, crotchY, tipZ, shaftR, 16, pc);
      RenderUtil.drawSphere(stack, tipX, crotchY, tipZ, headR, 14, dark);
      RenderUtil.drawSphere(stack, ex - rgtX * ballOff, crotchY - ballR * 0.3, ez - rgtZ * ballOff, ballR, 12, pc);
      RenderUtil.drawSphere(stack, ex + rgtX * ballOff, crotchY - ballR * 0.3, ez + rgtZ * ballOff, ballR, 12, pc);
   }

   private void drawPenis(GuiGraphicsExtractor g, float x1, float y1, float x2, float y2) {
      float s = this.penisSize.getValue();
      int c = this.penisColor.getValue().getRGB();
      float cx = (x1 + x2) / 2.0F;
      float eh = y2 - y1;
      float shaftW = Math.max(2.0F, eh * 0.1F * s);
      float shaftH = Math.max(4.0F, eh * 0.55F * s);
      float headW = shaftW * 1.8F;
      float headH = shaftW * 1.4F;
      float ballD = shaftW * 1.1F;
      float rootY = y1 + eh * 0.7F;
      float tipY = rootY - shaftH;
      int sx1 = (int)(cx - shaftW / 2.0F);
      int sx2 = (int)(cx + shaftW / 2.0F);
      g.fill(sx1, (int)tipY, sx2, (int)rootY, c);
      int hx1 = (int)(cx - headW / 2.0F);
      int hx2 = (int)(cx + headW / 2.0F);
      int hy2 = (int)tipY;
      int hy1 = (int)(tipY - headH);
      RenderUtil.roundedRect(g, hx1, hy1, (int)headW, (int)headH, (int)(headW / 2.0F), c);
      int ridgeC = new Color(
            Math.max(0, this.penisColor.getValue().getRed() - 40),
            Math.max(0, this.penisColor.getValue().getGreen() - 40),
            Math.max(0, this.penisColor.getValue().getBlue() - 40),
            this.penisColor.getValue().getAlpha()
         )
         .getRGB();
      g.fill(hx1, hy2, hx2, hy2 + 1, ridgeC);
      int ballY = (int)rootY;
      int ballH2 = (int)ballD;
      int ballW2 = (int)ballD;
      float gap = shaftW * 0.25F;
      int lbx = (int)(cx - shaftW / 2.0F - ballW2 - gap);
      RenderUtil.roundedRect(g, lbx, ballY, ballW2, ballH2, ballW2 / 2, c);
      int rbx = (int)(cx + shaftW / 2.0F + gap);
      RenderUtil.roundedRect(g, rbx, ballY, ballW2, ballH2, ballW2 / 2, c);
   }

   private void drawNametag(GuiGraphicsExtractor g, Entity entity, float x1, float x2, float y1, int c) {
      String name = entity instanceof LivingEntity le
         ? String.format("%s \u00a77%.0f%%", entity.getName().getString(), le.getHealth() / le.getMaxHealth() * 100.0F)
         : entity.getName().getString();
      int textW = mc.font.width(name);
      int tx = (int)((x1 + x2) / 2.0F) - textW / 2;
      int ty = (int)y1 - 9 - 2;
      RenderUtil.roundedRect(g, tx - 2, ty - 1, textW + 4, 11, 2, -2013265920);
      g.text(mc.font, name, tx, ty, c, false);
   }

   private void drawTrack3D(PoseStack stack, Entity entity) {
      ArrayDeque<Vec3> trail = this.trackHistory.get(entity.getUUID());
      if (trail != null && trail.size() >= 2) {
         Vec3[] points = trail.toArray(new Vec3[0]);
         int size = points.length;
         Color baseColor = this.trackColor.getValue();
         float lw = this.trackWidth.getValue();
         boolean fade = this.trackFade.getValue();
         Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
         BufferBuilder buf = Tesselator.getInstance().begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_LINE_WIDTH);
         Pose pose = stack.last();

         for (int i = 1; i < size; i++) {
            Vec3 p0 = points[i - 1];
            Vec3 p1 = points[i];
            float alpha = fade ? (float)i / size : 1.0F;
            int a = (int)(baseColor.getAlpha() * alpha);
            int col = baseColor.getRGB() & 16777215 | a << 24;
            buf.addVertex(pose, (float)(p0.x - cam.x()), (float)(p0.y - cam.y()), (float)(p0.z - cam.z())).setColor(col).setLineWidth(lw);
            buf.addVertex(pose, (float)(p1.x - cam.x()), (float)(p1.y - cam.y()), (float)(p1.z - cam.z())).setColor(col).setLineWidth(lw);
         }

         Layers.lines().draw(buf.buildOrThrow());
      }
   }

   public enum RenderMode {
      BOX,
      CORNER,
      TWO_D,
      FILLED,
      OUTLINE,
      HEALTHBAR,
      NAMETAG;
   }
}
