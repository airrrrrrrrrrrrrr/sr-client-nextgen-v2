package me.alpha432.oyvey.features.gui.items.buttons;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.gui.items.Item;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class Button extends Item {
   public Button(String name) {
      super(name);
      this.height = 15;
   }

   @Override
   public void drawScreen(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTicks) {
      boolean enabled = this.getState();
      boolean hovering = this.isHovering(mouseX, mouseY);
      int accentFull = OyVey.colorManager.getColorWithAlpha((int)this.y, 255);
      int top = enabled ? -1339545322 : (hovering ? -1877737438 : -2012607982);
      int bottom = enabled ? -1611925226 : (hovering ? -1995177961 : -2130702300);
      RenderUtil.roundedRectGradient(context, this.x + 1.0F, this.y + 0.5F, this.width - 2.0F, this.height - 1.0F, 3.5F, top, bottom, true);
      RenderUtil.roundedOutline(context, this.x + 1.0F, this.y + 0.5F, this.width - 2.0F, this.height - 1.0F, 3.5F, 0.65F, hovering ? 855638015 : 419430399);

      if (enabled) {
         RenderUtil.roundedRect(context, this.x + 3.0F, this.y + 3.0F, 2.0F, this.height - 6.0F, 1.0F, accentFull);
      }

      int textX = enabled ? (int)(this.x + 7.0F) : (int)(this.x + 5.0F);
      int textColor = enabled ? -1 : -5592406;
      this.drawString(this.getName(), textX, this.y + 4.0F, textColor);
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         this.toggle();
         mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      }
   }

   @Override
   public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
   }

   @Override
   public void onKeyTyped(String character, int modifiers) {
   }

   @Override
   public void onKeyPressed(int key) {
   }

   @Override
   public void onMouseClick() {
   }

   @Override
   public void toggle() {
   }

   @Override
   public boolean getState() {
      return false;
   }
}
