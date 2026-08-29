package me.alpha432.oyvey.manager;

import java.util.Arrays;
import me.alpha432.oyvey.features.Feature;
import me.alpha432.oyvey.util.models.Timer;

public class ServerManager extends Feature {
   private final float[] tpsCounts = new float[10];
   private final Timer timer = new Timer();
   private float tps = 20.0F;
   private long lastUpdate = -1L;
   private String serverBrand = "";

   public void onPacketReceived() {
      this.timer.reset();
   }

   public void update() {
      long currentTime = System.currentTimeMillis();
      if (this.lastUpdate == -1L) {
         this.lastUpdate = currentTime;
      } else {
         long timeDiff = currentTime - this.lastUpdate;
         float tickTime = (float)timeDiff / 20.0F;
         if (tickTime == 0.0F) {
            tickTime = 50.0F;
         }

         float tps;
         if ((tps = 1000.0F / tickTime) > 20.0F) {
            tps = 20.0F;
         }

         System.arraycopy(this.tpsCounts, 0, this.tpsCounts, 1, this.tpsCounts.length - 1);
         this.tpsCounts[0] = tps;
         double total = 0.0;

         for (float f : this.tpsCounts) {
            total += f;
         }

         if ((total = total / this.tpsCounts.length) > 20.0) {
            total = 20.0;
         }

         this.tps = (float)total;
         this.lastUpdate = currentTime;
      }
   }

   @Override
   public void reset() {
      Arrays.fill(this.tpsCounts, 20.0F);
      this.tps = 20.0F;
   }

   public boolean isServerNotResponding() {
      return this.timer.passedMs(2000L);
   }

   public long serverRespondingTime() {
      return this.timer.getPassedTimeMs();
   }

   public float getTpsFactor() {
      return 20.0F / this.tps;
   }

   public float getTps() {
      return this.tps;
   }

   public String getServerBrand() {
      return this.serverBrand;
   }

   public void setServerBrand(String brand) {
      this.serverBrand = brand;
   }

   public int getPing() {
      if (nullCheck()) {
         return 0;
      }

      try {
         return mc.getConnection().getPlayerInfo(mc.player.getGameProfile().name()).getLatency();
      } catch (Throwable var2) {
         return 0;
      }
   }
}
