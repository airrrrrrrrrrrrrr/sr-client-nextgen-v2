package me.alpha432.oyvey.features.modules.render;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import me.alpha432.oyvey.features.modules.Module;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class Chams extends Module {
   public static final Set<EntityRenderState> CHAMS_STATES = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
   public static boolean renderingChamsEntity = false;
   public static boolean renderingClassicCape = false;
   public static boolean renderingOwnCape = false;

   public Chams() {
      super("Chams", "Renders player bodies through walls", Module.Category.RENDER);
   }
}
