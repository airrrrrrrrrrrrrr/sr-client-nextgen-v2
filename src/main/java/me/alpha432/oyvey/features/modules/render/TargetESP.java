package me.alpha432.oyvey.features.modules.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import java.util.List;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.combat.GrimAura;
import me.alpha432.oyvey.features.modules.combat.KillAura;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.Layers;
import me.alpha432.oyvey.util.render.RenderUtil;
import me.alpha432.oyvey.util.render.WorldRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TargetESP extends Module {
   public final Setting<TargetESP.Shape> shape = this.register(new Setting<>("Mode", TargetESP.Shape.RING));
   public final Setting<Color> ringColor = this.color("RingColor", 0, 180, 255, 200);
   public final Setting<Float> ringSpeed = this.register(new Setting<>("RingSpeed", 1.5F, 0.1F, 10.0F, v -> this.shape.getValue() != TargetESP.Shape.NONE));
   public final Setting<Float> ringAmplitude = this.register(
      new Setting<>("RingAmplitude", 0.6F, 0.0F, 2.0F, v -> this.shape.getValue() != TargetESP.Shape.NONE)
   );
   public final Setting<Float> ringRadius = this.register(new Setting<>("RingRadius", 0.55F, 0.2F, 2.0F, v -> this.shape.getValue() != TargetESP.Shape.NONE));
   public final Setting<Float> ringWidth = this.register(new Setting<>("RingWidth", 1.5F, 0.5F, 4.0F, v -> this.shape.getValue() != TargetESP.Shape.NONE));
   public final Setting<Integer> ringSegments = this.register(new Setting<>("RingSegments", 32, 8, 64, v -> this.shape.getValue() == TargetESP.Shape.RING));
   public final Setting<Integer> starPoints = this.register(new Setting<>("StarPoints", 5, 4, 10, v -> this.shape.getValue() == TargetESP.Shape.STAR));
   public final Setting<Float> starSpin = this.register(new Setting<>("StarSpin", 3.0F, -15.0F, 15.0F, v -> this.shape.getValue() == TargetESP.Shape.STAR));
   public final Setting<Boolean> esp2DEnabled = this.bool("2D ESP", true);
   public final Setting<Color> esp2DColor = this.color("2DColor", 0, 180, 255, 200);
   public final Setting<Float> esp2DWidth = this.register(new Setting<>("2DWidth", 1.0F, 0.5F, 3.0F, v -> this.esp2DEnabled.getValue()));

   public TargetESP() {
      super("TargetESP", "Target marker (ring / rhombus) + 2D box.", Module.Category.RENDER);
   }

   @Subscribe
   @Override
   public void onRender3D(Render3DEvent event) {
      if (this.shape.getValue() != TargetESP.Shape.NONE && mc.player != null && mc.level != null) {
         Entity target = this.rangeTarget();
         if (target != null) {
            double time = System.currentTimeMillis() / 1000.0 * this.ringSpeed.getValue().floatValue();
            TargetESP.Shape sh = this.shape.getValue();
            AABB box = RenderUtil.interpolatedBox(target, event.getDelta());
            Vec3 targetPos = RenderUtil.interpolatedPosition(target, event.getDelta());
            double mid = box.minY + (box.maxY - box.minY) / 2.0;
            double cy = mid + Math.sin(time + target.getId()) * this.ringAmplitude.getValue().floatValue();
            double radius = this.ringRadius.getValue().floatValue();
            if (sh == TargetESP.Shape.STAR) {
               double spin = System.currentTimeMillis() / 1000.0 * this.starSpin.getValue().floatValue();
               this.drawStar(
                  event.getMatrix(),
                  targetPos.x,
                  cy,
                  targetPos.z,
                  radius,
                  this.ringColor.getValue(),
                  this.ringWidth.getValue(),
                  this.starPoints.getValue(),
                  spin
               );
            } else if (sh == TargetESP.Shape.RHOMBUS) {
               this.drawRhombus(event.getMatrix(), targetPos.x, cy, targetPos.z, radius, this.ringColor.getValue(), this.ringWidth.getValue());
            } else {
               this.drawRing(
                  event.getMatrix(),
                  targetPos.x,
                  cy,
                  targetPos.z,
                  radius,
                  this.ringSegments.getValue(),
                  this.ringColor.getValue(),
                  this.ringWidth.getValue()
               );
            }
         }
      }
   }

   @Subscribe
   @Override
   public void onRender2D(Render2DEvent event) {
      if (this.esp2DEnabled.getValue() && mc.player != null && mc.level != null) {
         GuiGraphicsExtractor gfx = event.getContext();
         PlayerESP esp = OyVey.moduleManager.getModuleByClass(PlayerESP.class);

         for (Entity entity : this.getTargets()) {
            float[] r = WorldRenderer.aabbToScreen(RenderUtil.interpolatedBox(entity, event.getDelta()));
            if (r != null) {
               float x1 = r[0];
               float y1 = r[1];
               float x2 = r[2];
               float y2 = r[3];
               if (!(x1 >= x2) && !(y1 >= y2)) {
                  Color c = esp != null && esp.isEnabled() ? esp.color.getValue() : this.esp2DColor.getValue();
                  int ci = c.getRGB();
                  float w = this.esp2DWidth.getValue();
                  gfx.fill((int)(x1 - w), (int)y1, (int)(x1 + w), (int)y2, ci);
                  gfx.fill((int)(x2 - w), (int)y1, (int)(x2 + w), (int)y2, ci);
                  gfx.fill((int)x1, (int)(y1 - w), (int)x2, (int)(y1 + w), ci);
                  gfx.fill((int)x1, (int)(y2 - w), (int)x2, (int)(y2 + w), ci);
               }
            }
         }
      }
   }

   private Entity rangeTarget() {
      KillauraRecode kr = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      if (kr != null && kr.isEnabled()) {
         Entity locked = kr.getTarget();
         if (locked != null && locked.isAlive()) {
            return locked;
         }
      }

      double range = kr != null ? kr.reach.getValue() : 4.0;
      Vec3 eye = mc.player.getEyePosition();
      Entity closest = null;
      double closestDist = range;

      for (Entity e : mc.level.entitiesForRendering()) {
         if (e != mc.player && e instanceof Player && e.isAlive()) {
            double d = eye.distanceTo(e.position());
            if (d <= closestDist) {
               closestDist = d;
               closest = e;
            }
         }
      }

      return closest;
   }

   private List<Entity> getTargets() {
      Vec3 eye = mc.player.getEyePosition();
      double r = this.auraReach();
      List<Entity> out = new java.util.ArrayList<>();

      for (Entity e : mc.level.entitiesForRendering()) {
         if (e != mc.player && e instanceof Player && e.isAlive() && !(eye.distanceTo(e.position()) > r)) {
            out.add(e);
         }
      }

      return out;
   }

   private double auraReach() {
      KillAura ka = OyVey.moduleManager.getModuleByClass(KillAura.class);
      if (ka != null && ka.isEnabled()) {
         return ka.reach.getValue();
      }

      GrimAura ga = OyVey.moduleManager.getModuleByClass(GrimAura.class);
      if (ga != null && ga.isEnabled()) {
         return ga.reach.getValue();
      }

      KillauraRecode kr = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      return kr != null && kr.isEnabled() ? kr.reach.getValue() : 4.0;
   }

   private void drawRing(PoseStack stack, double wx, double wy, double wz, double radius, int segs, Color color, float lineWidth) {
      Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
      float ox = (float)(wx - cam.x());
      float oy = (float)(wy - cam.y());
      float oz = (float)(wz - cam.z());
      BufferBuilder buf = Tesselator.getInstance().begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_LINE_WIDTH);
      Pose pose = stack.last();
      int col = color.getRGB();

      for (int i = 0; i < segs; i++) {
         double a1 = 6.283185307179586 * i / segs;
         double a2 = 6.283185307179586 * (i + 1) / segs;
         buf.addVertex(pose, ox + (float)(Math.cos(a1) * radius), oy, oz + (float)(Math.sin(a1) * radius)).setColor(col).setLineWidth(lineWidth);
         buf.addVertex(pose, ox + (float)(Math.cos(a2) * radius), oy, oz + (float)(Math.sin(a2) * radius)).setColor(col).setLineWidth(lineWidth);
      }

      Layers.lines().draw(buf.buildOrThrow());
   }

   private void drawRhombus(PoseStack stack, double wx, double wy, double wz, double radius, Color color, float lineWidth) {
      Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
      float ox = (float)(wx - cam.x());
      float oy = (float)(wy - cam.y());
      float oz = (float)(wz - cam.z());
      boolean smooth = RenderUtil.highQuality;
      int segs = smooth ? 64 : 4;
      double n = smooth ? 1.5 : 1.0;
      BufferBuilder buf = Tesselator.getInstance().begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_LINE_WIDTH);
      Pose pose = stack.last();
      int col = color.getRGB();

      for (int i = 0; i < segs; i++) {
         float[] p1 = rhombusPoint(6.283185307179586 * i / segs, radius, n);
         float[] p2 = rhombusPoint(6.283185307179586 * (i + 1) / segs, radius, n);
         buf.addVertex(pose, ox + p1[0], oy, oz + p1[1]).setColor(col).setLineWidth(lineWidth);
         buf.addVertex(pose, ox + p2[0], oy, oz + p2[1]).setColor(col).setLineWidth(lineWidth);
      }

      Layers.lines().draw(buf.buildOrThrow());
   }

   private static float[] rhombusPoint(double angle, double radius, double n) {
      double c = Math.cos(angle);
      double s = Math.sin(angle);
      double x = Math.signum(c) * Math.pow(Math.abs(c), 2.0 / n) * radius;
      double z = Math.signum(s) * Math.pow(Math.abs(s), 2.0 / n) * radius;
      return new float[]{(float)x, (float)z};
   }

   private void drawStar(PoseStack stack, double wx, double wy, double wz, double radius, Color color, float lineWidth, int points, double spin) {
      Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
      float ox = (float)(wx - cam.x());
      float oy = (float)(wy - cam.y());
      float oz = (float)(wz - cam.z());
      double inner = radius * 0.45;
      int n = points * 2;
      double[] vx = new double[n];
      double[] vz = new double[n];

      for (int i = 0; i < n; i++) {
         double ang = spin + 3.141592653589793 * i / points - 1.5707963267948966;
         double r = i % 2 == 0 ? radius : inner;
         vx[i] = Math.cos(ang) * r;
         vz[i] = Math.sin(ang) * r;
      }

      List<float[]> outline = new java.util.ArrayList<>();
      if (RenderUtil.highQuality) {
         int cornerSegs = 6;

         for (int i = 0; i < n; i++) {
            double cx = vx[i];
            double cz = vz[i];
            double pxd = vx[(i - 1 + n) % n];
            double pzd = vz[(i - 1 + n) % n];
            double nxd = vx[(i + 1) % n];
            double nzd = vz[(i + 1) % n];
            double d1x = cx - pxd;
            double d1z = cz - pzd;
            double l1 = Math.max(1.0E-5, Math.hypot(d1x, d1z));
            double d2x = nxd - cx;
            double d2z = nzd - cz;
            double l2 = Math.max(1.0E-5, Math.hypot(d2x, d2z));
            double t = 0.38 * Math.min(l1, l2);
            double sx = cx - d1x / l1 * t;
            double sz = cz - d1z / l1 * t;
            double ex = cx + d2x / l2 * t;
            double ez = cz + d2z / l2 * t;

            for (int k = 0; k <= cornerSegs; k++) {
               double tt = (double)k / cornerSegs;
               double omt = 1.0 - tt;
               double bx = omt * omt * sx + 2.0 * omt * tt * cx + tt * tt * ex;
               double bz = omt * omt * sz + 2.0 * omt * tt * cz + tt * tt * ez;
               outline.add(new float[]{(float)bx, (float)bz});
            }
         }
      } else {
         for (int i = 0; i < n; i++) {
            outline.add(new float[]{(float)vx[i], (float)vz[i]});
         }
      }

      BufferBuilder buf = Tesselator.getInstance().begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_LINE_WIDTH);
      Pose pose = stack.last();
      int col = color.getRGB();
      int m = outline.size();

      for (int i = 0; i < m; i++) {
         float[] a = outline.get(i);
         float[] b = outline.get((i + 1) % m);
         buf.addVertex(pose, ox + a[0], oy, oz + a[1]).setColor(col).setLineWidth(lineWidth);
         buf.addVertex(pose, ox + b[0], oy, oz + b[1]).setColor(col).setLineWidth(lineWidth);
      }

      Layers.lines().draw(buf.buildOrThrow());
   }

   public enum Shape {
      NONE,
      RING,
      RHOMBUS,
      STAR;
   }
}
