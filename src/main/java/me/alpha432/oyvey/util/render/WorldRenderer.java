package me.alpha432.oyvey.util.render;

import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public final class WorldRenderer {
   private WorldRenderer() { }

   @Nullable
   public static float[] toScreen(double worldX, double worldY, double worldZ) {
      Camera camera = Util.mc.gameRenderer.getMainCamera();
      if (!camera.isInitialized()) return null;
      Vec3 position = camera.position();
      return ScreenProjection.point(camera.getViewRotationProjectionMatrix(new Matrix4f()),
         worldX - position.x, worldY - position.y, worldZ - position.z,
         Util.mc.getWindow().getGuiScaledWidth(), Util.mc.getWindow().getGuiScaledHeight());
   }

   @Nullable
   public static float[] aabbToScreen(AABB box) {
      Camera camera = Util.mc.gameRenderer.getMainCamera();
      if (!camera.isInitialized()) return null;
      Vec3 position = camera.position();
      return ScreenProjection.bounds(camera.getViewRotationProjectionMatrix(new Matrix4f()),
         box.minX - position.x, box.minY - position.y, box.minZ - position.z,
         box.maxX - position.x, box.maxY - position.y, box.maxZ - position.z,
         Util.mc.getWindow().getGuiScaledWidth(), Util.mc.getWindow().getGuiScaledHeight());
   }
}
