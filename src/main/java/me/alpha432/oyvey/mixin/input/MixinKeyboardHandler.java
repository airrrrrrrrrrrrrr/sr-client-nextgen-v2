package me.alpha432.oyvey.mixin.input;

import me.alpha432.oyvey.event.impl.input.KeyInputEvent;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.movement.InventoryMove;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {
   @Inject(method = "keyPress", at = @At("HEAD"))
   private void oyvey$inventoryMoveScanCode(long window, int action, KeyEvent input, CallbackInfo ci) {
      if (OyVey.moduleManager == null || window != Util.mc.getWindow().handle()) return;
      InventoryMove inventoryMove = OyVey.moduleManager.getModuleByClass(InventoryMove.class);
      if (inventoryMove != null) inventoryMove.recordKeyEvent(action, input.scancode());
   }

   @Inject(method = "keyPress", at = @At("TAIL"), cancellable = true)
   private void keyPress(long window, int action, KeyEvent input, CallbackInfo ci) {
      if (action == 1 && Util.EVENT_BUS.post(new KeyInputEvent(input.key()))) {
         ci.cancel();
      }
   }
}
