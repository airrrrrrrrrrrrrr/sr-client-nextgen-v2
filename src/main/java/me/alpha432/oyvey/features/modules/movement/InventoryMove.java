package me.alpha432.oyvey.features.modules.movement;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.HashSet;
import java.util.Set;
import me.alpha432.oyvey.features.gui.AccountManagerScreen;
import me.alpha432.oyvey.features.gui.DropdownGui;
import me.alpha432.oyvey.features.gui.FancyGui;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.mixin.input.KeyMappingAccessor;
import me.alpha432.oyvey.util.player.GuiMovementKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import org.lwjgl.glfw.GLFW;

public class InventoryMove extends Module {
   private final Set<Integer> heldScanCodes = new HashSet<>();
   private GuiMovementKeys movementKeys;
   private Options boundOptions;

   public InventoryMove() {
      super("InventoryMove", "Move normally in inventories, containers and other GUIs.", Module.Category.MOVEMENT);
   }

   /** Runs before vanilla builds movement input, preserving normal physics and diagonal normalization. */
   public void updateInputKeys() {
      if (!this.isEnabled() || mc.options == null) return;
      if (this.boundOptions != mc.options) {
         this.boundOptions = mc.options;
         this.movementKeys = new GuiMovementKeys(mc.options.keyUp, mc.options.keyDown,
            mc.options.keyLeft, mc.options.keyRight, mc.options.keyJump, mc.options.keyShift, mc.options.keySprint);
      }
      if (nullCheck()) {
         this.movementKeys.release(false, this::physicalKeyDown, InventoryMove::setKeyDown);
         this.heldScanCodes.clear();
         return;
      }
      boolean active = mc.isWindowActive() && mc.player.isAlive();
      if (!active) this.heldScanCodes.clear();
      this.movementKeys.update(mc.screen != null, active && canMoveInScreen(mc.screen),
         this::physicalKeyDown, InventoryMove::setKeyDown);
   }

   public void recordKeyEvent(int action, int scanCode) {
      if (!this.isEnabled() || scanCode < 0) return;
      if (action == GLFW.GLFW_RELEASE) this.heldScanCodes.remove(scanCode);
      else this.heldScanCodes.add(scanCode);
   }

   private boolean physicalKeyDown(InputConstants.Key key) {
      long window = mc.getWindow().handle();
      return switch (key.getType()) {
         case KEYSYM -> key.getValue() >= 0 && key.getValue() <= GLFW.GLFW_KEY_LAST
            && GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
         case MOUSE -> key.getValue() >= 0 && key.getValue() <= GLFW.GLFW_MOUSE_BUTTON_LAST
            && GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
         case SCANCODE -> this.heldScanCodes.contains(key.getValue());
      };
   }

   private static void setKeyDown(KeyMapping key, boolean down) {
      // Bypass toggle semantics: repeated GUI polls must not flip sprint/sneak on and off.
      ((KeyMappingAccessor)(Object)key).oyvey$setDown(down);
   }

   public static boolean canMoveInScreen(Screen screen) {
      if (screen == null) return true;
      if (screen instanceof ChatScreen || screen instanceof AbstractSignEditScreen || screen instanceof BookEditScreen
         || screen instanceof KeyBindsScreen || screen instanceof AccountManagerScreen) return false;
      if (screen instanceof DropdownGui gui && gui.isCapturingKeyboardInput()) return false;
      if (screen instanceof FancyGui gui && gui.isCapturingKeyboardInput()) return false;
      GuiEventListener focused = screen.getFocused();
      for (int depth = 0; focused != null && depth < 16; depth++) {
         if (focused instanceof EditBox || focused instanceof MultiLineEditBox) return false;
         if (!(focused instanceof ContainerEventHandler container)) break;
         focused = container.getFocused();
      }
      return true;
   }

   @Override
   public void onDisable() {
      if (this.movementKeys != null) {
         this.movementKeys.release(mc.screen == null && mc.isWindowActive(), this::physicalKeyDown, InventoryMove::setKeyDown);
      }
      this.heldScanCodes.clear();
   }

   @Override
   public void onUnload() {
      this.onDisable();
   }
}
