package me.alpha432.oyvey.features.modules.movement.scaffold;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Input;

/** A diagonal bridge that steps up once after every seven successful blocks. */
public final class GodBridge {
   private final Set<BlockPos> counted = new HashSet<>();
   private int blocks;
   private boolean requestedJump;
   private Integer layer;

   public void reset() { counted.clear(); blocks = 0; requestedJump = false; layer = null; }

   public void placed(BlockPos pos) {
      if (counted.add(pos.immutable())) blocks++;
   }

   public boolean jump(boolean grounded, boolean moving, boolean aligned, boolean headroom) {
      this.observeTakeoff(grounded);
      boolean jump = grounded && moving && aligned && headroom && blocks >= 7;
      if (jump) requestedJump = true;
      return jump;
   }

   public int placementLayer(int footLayer, boolean grounded) {
      if (layer == null) layer = footLayer;
      this.observeTakeoff(grounded);
      // Wait until the player's feet clear the new block before placing it.
      // Only an automatic seven-block takeoff advances the desired height.
      return Math.min(layer, footLayer);
   }

   private void observeTakeoff(boolean grounded) {
      if (!grounded && requestedJump) {
         blocks = Math.max(0, blocks - 7);
         counted.clear();
         requestedJump = false;
         if (layer != null) layer++;
      }
   }

   public static float heading(Input keys, float cameraYaw) {
      float backward = TellyBridge.forwardYaw(keys, cameraYaw) + 180;
      return 45 + Math.round((backward - 45) / 90) * 90;
   }

   /** A small inset avoids a ray exactly grazing the shared diagonal edge. */
   public static float aimYaw(Input keys, float cameraYaw) {
      return heading(keys, cameraYaw) + .3F;
   }
}
