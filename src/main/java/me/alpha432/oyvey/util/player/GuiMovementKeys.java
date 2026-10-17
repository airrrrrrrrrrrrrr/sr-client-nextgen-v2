package me.alpha432.oyvey.util.player;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.client.KeyMapping;

/** Owns movement key overrides and resynchronizes them when a GUI closes or the module stops. */
public final class GuiMovementKeys {
   private final KeyMapping[] bindings;
   private boolean overriding;

   public GuiMovementKeys(KeyMapping... bindings) {
      this.bindings = bindings.clone();
   }

   public void update(boolean screenOpen, boolean allowed, Predicate<InputConstants.Key> physicalKeys,
                      BiConsumer<KeyMapping, Boolean> setDown) {
      if (screenOpen) {
         this.sync(allowed, physicalKeys, setDown);
         this.overriding = true;
      } else {
         this.release(allowed, physicalKeys, setDown);
      }
   }

   public void release(boolean allowed, Predicate<InputConstants.Key> physicalKeys,
                       BiConsumer<KeyMapping, Boolean> setDown) {
      if (this.overriding) {
         this.sync(allowed, physicalKeys, setDown);
         this.overriding = false;
      }
   }

   private void sync(boolean allowed, Predicate<InputConstants.Key> physicalKeys,
                     BiConsumer<KeyMapping, Boolean> setDown) {
      for (KeyMapping binding : this.bindings) {
         InputConstants.Key key = InputConstants.getKey(binding.saveString());
         setDown.accept(binding, allowed && physicalKeys.test(key));
      }
   }
}
