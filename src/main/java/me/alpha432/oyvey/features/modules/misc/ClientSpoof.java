package me.alpha432.oyvey.features.modules.misc;

import me.alpha432.oyvey.features.modules.Module;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientSpoof extends Module {
   public static final String SPOOFED_BRAND = "lunarclient";

   public ClientSpoof() {
      super(
         "ClientSpoof",
         "Spoofs your client brand to Lunar Client and registers the lunar:apollo channel. Enable before joining a server.",
         Module.Category.MISC
      );
   }

   @Override
   public void onEnable() {
      ClientPlayNetworking.registerGlobalReceiver(LunarApolloPayload.TYPE, (payload, context) -> {});
   }

   @Override
   public void onDisable() {
      ClientPlayNetworking.unregisterGlobalReceiver(LunarApolloPayload.CHANNEL);
   }

   @Override
   public String getDisplayInfo() {
      return "Lunar";
   }
}
