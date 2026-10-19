package me.alpha432.oyvey.util;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.Connection;

/** Optional ViaFabricPlus integration; no Via classes are bundled into release JARs. */
public final class ViaProtocol {
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
         var listener = Minecraft.getInstance().getConnection();
         Object version = listener == null ? api.getClass().getMethod("getTargetVersion").invoke(api)
            : api.getClass().getMethod("getTargetVersion", Connection.class).invoke(api, listener.getConnection());
         Class<?> versions = Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion");
         Object oneEight = versions.getField("v1_8").get(null);
         Method olderOrEqual = versions.getMethod("olderThanOrEqualTo", versions);
         return (boolean)olderOrEqual.invoke(version, oneEight);
      } catch (ReflectiveOperationException | LinkageError unavailable) {
         return false; // Native 26.x uses shields.
      }
   }
}
