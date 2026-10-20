package me.alpha432.oyvey.util;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.Connection;

/** Optional ViaFabricPlus integration; no Via classes are bundled into release JARs. */
public final class ViaProtocol {
   private static boolean protocolWarning;
   private ViaProtocol() { }

   private static Object api() throws ReflectiveOperationException {
      return Class.forName("com.viaversion.viafabricplus.ViaFabricPlusImpl").getField("INSTANCE").get(null);
   }

   public static boolean openSelector(Screen parent) {
      try {
         Object api = api();
         api.getClass().getMethod("openProtocolSelectionScreen", Screen.class).invoke(api, parent);
         return true;
      } catch (ReflectiveOperationException | LinkageError unavailable) { return false; }
   }

   public static boolean usesSwordBlocking() {
      try {
         Object api = api();
         Object version = targetVersion(api);
         Class<?> versions = Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion");
         Object oneEight = versions.getField("v1_8").get(null);
         Method olderOrEqual = versions.getMethod("olderThanOrEqualTo", versions);
         return (boolean)olderOrEqual.invoke(version, oneEight);
      } catch (ReflectiveOperationException | LinkageError unavailable) {
         if (!protocolWarning && net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("viafabricplus")) {
            protocolWarning=true;
            me.alpha432.oyvey.OyVey.LOGGER.warn("Unable to resolve Via blocking protocol",unavailable);
         }
         return false; // Native 26.x uses shields.
      }
   }

   private static Object targetVersion(Object api) throws ReflectiveOperationException {
      var listener=Minecraft.getInstance().getConnection();
      if (listener!=null) {
         try {
            Object connected=api.getClass().getMethod("getTargetVersion",Connection.class).invoke(api,listener.getConnection());
            if (connected!=null) return connected;
         } catch (NoSuchMethodException olderApi) { /* Use the global API on older installations. */ }
      }
      Object selected=api.getClass().getMethod("getTargetVersion").invoke(api);
      if (selected==null) throw new ReflectiveOperationException("Via target protocol is not initialized");
      return selected;
   }

   public static String targetName() {
      try {
         Object version=targetVersion(api());
         return String.valueOf(version.getClass().getMethod("getName").invoke(version));
      } catch (ReflectiveOperationException | LinkageError unavailable) { return "native/unavailable"; }
   }

   /** Modern inventory swaps use window 0, with no removed legacy inventory-open action. */
   public static boolean supportsModernInventorySpoof() {
      try {
         Object api = api();
         var listener = Minecraft.getInstance().getConnection();
         if (listener == null) return false;
         Object version = api.getClass().getMethod("getTargetVersion", Connection.class).invoke(api, listener.getConnection());
         Class<?> versions = Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion");
         return (boolean)versions.getMethod("newerThanOrEqualTo", versions).invoke(version, versions.getField("v1_12").get(null));
      } catch (ClassNotFoundException noVia) {
         return !net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("viafabricplus");
      } catch (ReflectiveOperationException | LinkageError failure) {
         return false;
      }
   }
}
