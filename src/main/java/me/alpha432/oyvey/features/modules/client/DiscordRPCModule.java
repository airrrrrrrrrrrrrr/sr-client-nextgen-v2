package me.alpha432.oyvey.features.modules.client;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import net.minecraft.client.multiplayer.ServerData;

public class DiscordRPCModule extends Module {
   private static DiscordRPCModule INSTANCE;
   private String lastDetails = "";
   private int tickDelay = 0;

   public DiscordRPCModule() {
      super("DiscordRPC", "Shows SR Client status in Discord.", Module.Category.CLIENT);
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      if (OyVey.discordRPCManager != null) {
         OyVey.discordRPCManager.setActive(true);
         this.triggerUpdate();
      }
   }

   @Override
   public void onDisable() {
      if (OyVey.discordRPCManager != null) {
         OyVey.discordRPCManager.setActive(false);
      }
   }

   public void applyOnStartup() {
      if (this.isEnabled() && OyVey.discordRPCManager != null) {
         OyVey.discordRPCManager.setActive(true);
         this.triggerUpdate();
      }
   }

   @Override
   public void onTick() {
      if (++this.tickDelay >= 20) {
         this.tickDelay = 0;
         String details = this.buildDetails();
         if (!details.equals(this.lastDetails)) {
            this.lastDetails = details;
            if (OyVey.discordRPCManager != null) {
               OyVey.discordRPCManager.notifyUpdate(details, "Bypass \u2022 PvP \u2022 Utility");
            }
         }
      }
   }

   private void triggerUpdate() {
      this.lastDetails = this.buildDetails();
      if (OyVey.discordRPCManager != null) {
         OyVey.discordRPCManager.notifyUpdate(this.lastDetails, "Bypass \u2022 PvP \u2022 Utility");
      }
   }

   private String buildDetails() {
      if (mc.player == null) {
         return "In Main Menu";
      }

      if (mc.isSingleplayer()) {
         return "Using SR Client in Singleplayer";
      }

      ServerData srv = mc.getCurrentServer();
      if (srv != null) {
         String name = stripColorCodes(srv.name).trim();
         String ip = srv.ip != null ? srv.ip.trim() : "";
         if (ip.toLowerCase().contains("liquidproxy.net")) {
            return "Using SR Client";
         }

         String label = !name.isEmpty() && !name.equalsIgnoreCase(ip) ? name : ip;
         return "Using SR Client on " + label;
      } else {
         return "Using SR Client";
      }
   }

   private static String stripColorCodes(String text) {
      return text == null ? "" : text.replaceAll("\u00a7[0-9a-fk-orA-FK-OR]", "");
   }

   public static DiscordRPCModule getInstance() {
      return INSTANCE;
   }
}
