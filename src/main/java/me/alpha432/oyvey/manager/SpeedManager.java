package me.alpha432.oyvey.manager;

import java.util.ArrayList;
import java.util.List;
import me.alpha432.oyvey.features.Feature;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class SpeedManager extends Feature {
   private static final int SPEED_NORMALIZATION = 20;
   private final List<Float> localSpeed = new ArrayList<>();
   private float localSpeedNormal = 0.0F;

   public void update() {
      this.localSpeed.add(this.getCurrentSpeed(mc.player));

      while (this.localSpeed.size() > 20) {
         this.localSpeed.removeFirst();
      }

      this.localSpeedNormal = this.localSpeed.stream().reduce(0.0F, Float::sum) / this.localSpeed.size();
   }

   public double getSpeed(Player player) {
      return mc.player == player ? this.localSpeedNormal : this.getCurrentSpeed(player);
   }

   public double getSpeedBpS(Player player) {
      return this.getSpeed(player) * 20.0;
   }

   public double getSpeedKmH(Player player) {
      return this.getSpeedBpS(player) * 3.6;
   }

   public float getCurrentSpeed(Entity entity) {
      Entity vehicle = entity.getVehicle();
      double distTraveledX = entity.getX() - entity.xo;
      double distTraveledZ = entity.getZ() - entity.zo;
      if (vehicle != null) {
         distTraveledX = vehicle.getX() - vehicle.xo;
         distTraveledZ = vehicle.getZ() - vehicle.zo;
      }

      return (float)Math.hypot(distTraveledX, distTraveledZ);
   }
}
