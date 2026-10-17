package me.alpha432.oyvey.util;

import java.lang.reflect.Method;
import me.alpha432.oyvey.OyVey;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.phys.Vec2;

/** Matches KeyboardInput's vector format, including optional ViaFabricPlus translation. */
public final class KeyboardMovement {
   private static final LegacyInputBridge VIA = findVia();

   private KeyboardMovement() {
   }

   public static Vec2 vector(SilentRotationMath.Movement movement) {
      return movement.inputVector(normalizesDiagonals());
   }

   public static boolean normalizesDiagonals() {
      return VIA == null || !VIA.usesLegacyInput();
   }

   private static LegacyInputBridge findVia() {
      if (!FabricLoader.getInstance().isModLoaded("viafabricplus")) return null;
      try {
         // Optional integration only: no Via classes or dependencies are bundled.
         Class<?> translator = Class.forName("com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator");
         Method target = translator.getMethod("getTargetVersion");
         Class<?> version = target.getReturnType();
         return new LegacyInputBridge(target, version.getMethod("olderThanOrEqualTo", version),
            version.getField("v1_21_4").get(null));
      } catch (ReflectiveOperationException | LinkageError exception) {
         OyVey.LOGGER.warn("Unable to read ViaFabricPlus keyboard input policy", exception);
         return null;
      }
   }

   private static final class LegacyInputBridge {
      private final Method target;
      private final Method atMost;
      private final Object lastLegacyVersion;
      private boolean failed;

      private LegacyInputBridge(Method target, Method atMost, Object lastLegacyVersion) {
         this.target = target;
         this.atMost = atMost;
         this.lastLegacyVersion = lastLegacyVersion;
      }

      private boolean usesLegacyInput() {
         if (this.failed) return false;
         try {
            // Read each time: Via's target can change between server connections.
            return (Boolean)this.atMost.invoke(this.target.invoke(null), this.lastLegacyVersion);
         } catch (ReflectiveOperationException | LinkageError exception) {
            this.failed = true;
            OyVey.LOGGER.warn("Unable to read current ViaFabricPlus keyboard input policy", exception);
            return false;
         }
      }
   }
}
