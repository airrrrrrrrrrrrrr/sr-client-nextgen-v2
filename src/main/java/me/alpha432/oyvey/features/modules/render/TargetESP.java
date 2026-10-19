package me.alpha432.oyvey.features.modules.render;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.Layers;
import me.alpha432.oyvey.util.render.Pipelines;
import me.alpha432.oyvey.util.render.RenderUtil;
import me.alpha432.oyvey.util.render.WorldRenderer;
import me.alpha432.oyvey.util.render.TargetEspAnimation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Target-only diamond, scanning ring and three orbiting ghost trails. */
public class TargetESP extends Module {
   public enum Shape { DIAMOND, RING, GHOSTS, NONE }
   private static final Identifier RETICLE = Identifier.fromNamespaceAndPath("oyvey", "textures/esp/target.png");
   private static final Identifier GLOW = Identifier.fromNamespaceAndPath("oyvey", "textures/esp/glow.png");
   public final Setting<Shape> shape = register(new Setting<>("Mode", Shape.DIAMOND));
   public final Setting<Float> diamondSpinSpeed = register(new Setting<>("DiamondSpinSpeed", 1F, 0F, 5F, v -> shape.getValue() == Shape.DIAMOND));
   public final Setting<Float> ghostSpinSpeed = register(new Setting<>("GhostSpinSpeed", 1F, 0F, 5F, v -> shape.getValue() == Shape.GHOSTS));
   public final Setting<Boolean> themeColors = bool("ThemeColors", true);
   public final Setting<Color> ringColor = color("RingColor", 0, 180, 255, 200);
   public final Setting<Float> ringSpeed = register(new Setting<>("RingSpeed", 1F, .1F, 5F, v -> shape.getValue() == Shape.RING));
   public final Setting<Float> ringRadius = register(new Setting<>("RingRadius", .55F, .2F, 2F, v -> shape.getValue() == Shape.RING));
   public final Setting<Float> ringWidth = register(new Setting<>("RingWidth", 1.5F, .5F, 4F, v -> shape.getValue() == Shape.RING));
   public final Setting<Integer> ringSegments = register(new Setting<>("RingSegments", 64, 16, 128, v -> shape.getValue() == Shape.RING));
   private Entity currentTarget;
   private ClientLevel targetLevel;
   private final TargetEspAnimation animation = new TargetEspAnimation();

   public TargetESP() { super("TargetESP", "Glowing attack-reactive effects around your combat target.", Category.RENDER); }

   @Override public void fromJson(JsonElement element) {
      // Retain old configurations while replacing the old flat rhombus/star.
      JsonElement migrated = element.deepCopy();
      if (migrated.isJsonObject() && migrated.getAsJsonObject().has("Mode")) {
         String old = migrated.getAsJsonObject().get("Mode").getAsString();
         if (old.equalsIgnoreCase("RHOMBUS") || old.equalsIgnoreCase("STAR")) migrated.getAsJsonObject().addProperty("Mode", "DIAMOND");
      }
      super.fromJson(migrated);
   }

   @Override public void onEnable() { clear(); }
   @Override public void onDisable() { clear(); }
   private void clear() { currentTarget = null; targetLevel = null; animation.reset(); }

   private boolean valid(Entity target) {
      KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      return mc.player != null && target instanceof LivingEntity && target != mc.player && target.isAlive() && !target.isRemoved()
         && mc.level != null && mc.level.getEntity(target.getId()) == target
         && aura != null && aura.isEnabled() && aura.isWithinSearchRange(target);
   }

   private void lock(Entity target) {
      if (currentTarget != target) { currentTarget = target; animation.reset(); }
   }

   /** Native manual and aura attacks both feed the same visual-only animation. */
   public void onAttack(Entity target) {
      if (mc.level == null || !valid(target)) return;
      if (targetLevel != mc.level) { clear(); targetLevel = mc.level; }
      lock(target);
      animation.attack(System.nanoTime());
   }

   private boolean updateTarget() {
      if (mc.player == null || mc.level == null || shape.getValue() == Shape.NONE) { clear(); return false; }
      if (targetLevel != mc.level) { clear(); targetLevel = mc.level; }
      KillauraRecode aura = OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      Entity locked = aura != null && aura.isEnabled() ? aura.getTarget() : null;
      if (valid(locked)) lock(locked);
      // Keep the visual through attack gaps, but only while Recode is enabled
      // and the target remains inside its actual hitbox-based search range.
      if (!valid(currentTarget)) currentTarget = null;
      animation.update(System.nanoTime(), diamondSpinSpeed.getValue(), ghostSpinSpeed.getValue());
      return currentTarget != null;
   }

   private int color(int offset, float alpha) {
      int rgb = themeColors.getValue() ? OyVey.colorManager.getColorWithAlpha(offset, 255) : ringColor.getValue().getRGB();
      int a = Math.round(Mth.clamp(alpha, 0, 1) * (themeColors.getValue() ? 255 : ringColor.getValue().getAlpha()));
      return a << 24 | rgb & 0xFFFFFF;
   }

   @Override public void onRender2D(Render2DEvent event) {
      if (!updateTarget()) return;
      if (shape.getValue() == Shape.DIAMOND) diamond(event);
      else if (shape.getValue() == Shape.GHOSTS) ghosts(event);
   }

   private void diamond(Render2DEvent event) {
      Vec3 position = RenderUtil.interpolatedPosition(currentTarget, event.getDelta());
      float[] center = WorldRenderer.toScreenUnclipped(position.x, position.y + currentTarget.getBbHeight() / 1.8, position.z);
      // The reference reticle is 9 / 30 of a 1000/distance billboard.
      var camera = mc.gameRenderer.getMainCamera();
      double distance = Math.max(.1, camera.position().distanceTo(position));
      float size = (float)(Math.max(10, 1000 / distance) * .3 / (camera.getFov() / 70.0))
         * (1 + animation.pulse() * .1F);
      if (center == null || size <= 0) return;
      var g = event.getContext();
      g.pose().pushMatrix();
      g.pose().translate(center[0], center[1]);
      g.pose().rotate((float)(Math.sin(animation.diamondTime()) * Math.PI * 2) + animation.rotation());
      g.pose().scale(size / 128F);
      // Each disconnected corner receives one of the reference's four tints.
      for (int quadrant = 0; quadrant < 4; quadrant++) {
         int u = (quadrant & 1) * 64, v = (quadrant >> 1) * 64;
         g.blit(Pipelines.GUI_TARGET_HALO, RETICLE, u - 64, v - 64, (float)u, (float)v, 64, 64, 128, 128, color(quadrant * 90, 1));
      }
      g.pose().popMatrix();
   }

   private float projectedSize(Vec3 position, float worldSize) {
      float[] bottom = WorldRenderer.toScreenUnclipped(position.x, position.y, position.z);
      float[] top = WorldRenderer.toScreenUnclipped(position.x, position.y + currentTarget.getBbHeight(), position.z);
      if (bottom == null || top == null) return 0;
      float pixels = (float)Math.hypot(top[0] - bottom[0], top[1] - bottom[1]);
      return Mth.clamp(pixels * worldSize / Math.max(.1F, currentTarget.getBbHeight()), 3, 160);
   }

   private record Ghost(float x, float y, float size, int color, double distance) { }
   private void ghosts(Render2DEvent event) {
      Vec3 base = RenderUtil.interpolatedPosition(currentTarget, event.getDelta()).add(.2, Math.min(1.3, currentTarget.getBbHeight() * .7), 0);
      Vec3 camera = mc.gameRenderer.getMainCamera().position();
      ArrayList<Ghost> sprites = new ArrayList<>();
      double millis = animation.ghostTimeMillis();
      for (int trail = 0; trail < 3; trail++) for (int i = 0; i < 25; i++) {
         float alpha = Math.max(0, 255 - i * 15) / 255F;
         if (alpha <= 0) continue;
         double angle = .15 * (millis - i * 15) / 27 + animation.rotation();
         double s = Math.sin(angle) * .6, c = Math.cos(angle) * .6;
         Vec3 point = base.add(trail == 0 ? s : -s, trail == 0 ? c : trail == 1 ? s : -s, trail == 2 ? c : -c);
         float[] screen = WorldRenderer.toScreenUnclipped(point.x, point.y, point.z);
         if (screen == null) continue;
         float size = projectedSize(point, .32F);
         sprites.add(new Ghost(screen[0], screen[1], size, color(i * 12 + trail * 90, alpha), camera.distanceToSqr(point)));
      }
      sprites.sort(Comparator.comparingDouble(Ghost::distance).reversed());
      var g = event.getContext();
      for (Ghost sprite : sprites) {
         g.pose().pushMatrix();
         g.pose().translate(sprite.x, sprite.y);
         g.pose().scale(sprite.size / 64F);
         g.pose().pushMatrix();
         g.pose().scale(1.8F);
         g.blit(Pipelines.GUI_TARGET_GLOW, GLOW, -32, -32, 0F, 0F, 64, 64, 64, 64, sprite.color);
         g.pose().popMatrix();
         g.blit(Pipelines.GUI_TARGET_GLOW, GLOW, -32, -32, 0F, 0F, 64, 64, 64, 64, sprite.color);
         g.pose().popMatrix();
      }
   }

   @Override public void onRender3D(Render3DEvent event) {
      if (!updateTarget() || shape.getValue() != Shape.RING) return;
      Vec3 position = RenderUtil.interpolatedPosition(currentTarget, event.getDelta());
      Vec3 camera = mc.getEntityRenderDispatcher().camera.position();
      double height = currentTarget.getBbHeight();
      double cycle = (System.nanoTime() / 1_000_000_000.0 * ringSpeed.getValue()) % 2;
      boolean side = cycle > 1;
      double progress = side ? cycle - 1 : 1 - cycle;
      progress = progress < .5 ? 2 * progress * progress : 1 - Math.pow(-2 * progress + 2, 2) / 2;
      double y = position.y + height * progress - camera.y;
      double tail = height / 2 * Math.min(progress, 1 - progress) * (side ? -1 : 1) * 1.5;
      float x = (float)(position.x - camera.x), z = (float)(position.z - camera.z);
      float radius = currentTarget.getBbWidth() * .85F * ringRadius.getValue() / .55F;
      int segments = ringSegments.getValue();
      var matrix = event.getMatrix().last().pose();
      BufferBuilder band = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      for (int i = 0; i < segments; i++) {
         double a = Math.PI * 2 * i / segments + animation.rotation(), b = Math.PI * 2 * (i + 1) / segments + animation.rotation();
         float ax = x + (float)Math.cos(a) * radius, az = z + (float)Math.sin(a) * radius;
         float bx = x + (float)Math.cos(b) * radius, bz = z + (float)Math.sin(b) * radius;
         band.addVertex(matrix, ax, (float)y, az).setColor(color(i * 360 / segments, .7F));
         band.addVertex(matrix, ax, (float)(y + tail), az).setColor(color(i * 360 / segments, 0));
         band.addVertex(matrix, bx, (float)(y + tail), bz).setColor(color((i + 1) * 360 / segments, 0));
         band.addVertex(matrix, bx, (float)y, bz).setColor(color((i + 1) * 360 / segments, .7F));
      }
      Layers.targetGlowQuads().draw(band.buildOrThrow());
      // Soft additive fringe around the scanning edge, not just a flat line.
      BufferBuilder halo = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      for (int sideIndex : new int[]{-1, 1}) for (int i = 0; i < segments; i++) {
         double a = Math.PI * 2 * i / segments + animation.rotation();
         double b = Math.PI * 2 * (i + 1) / segments + animation.rotation();
         float ax = x + (float)Math.cos(a) * radius, az = z + (float)Math.sin(a) * radius;
         float bx = x + (float)Math.cos(b) * radius, bz = z + (float)Math.sin(b) * radius;
         halo.addVertex(matrix, ax, (float)y, az).setColor(color(i * 360 / segments, .8F));
         halo.addVertex(matrix, ax, (float)y + sideIndex * .065F, az).setColor(color(i * 360 / segments, 0));
         halo.addVertex(matrix, bx, (float)y + sideIndex * .065F, bz).setColor(color((i + 1) * 360 / segments, 0));
         halo.addVertex(matrix, bx, (float)y, bz).setColor(color((i + 1) * 360 / segments, .8F));
      }
      Layers.targetGlowQuads().draw(halo.buildOrThrow());
      BufferBuilder edge = Tesselator.getInstance().begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_LINE_WIDTH);
      for (int i = 0; i < segments; i++) {
         double a = Math.PI * 2 * i / segments + animation.rotation(), b = Math.PI * 2 * (i + 1) / segments + animation.rotation();
         edge.addVertex(matrix, x + (float)Math.cos(a) * radius, (float)y, z + (float)Math.sin(a) * radius).setColor(color(i * 360 / segments, 1)).setLineWidth(ringWidth.getValue());
         edge.addVertex(matrix, x + (float)Math.cos(b) * radius, (float)y, z + (float)Math.sin(b) * radius).setColor(color((i + 1) * 360 / segments, 1)).setLineWidth(ringWidth.getValue());
      }
      Layers.lines().draw(edge.buildOrThrow());
   }
}
