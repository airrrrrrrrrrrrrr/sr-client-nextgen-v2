package me.alpha432.oyvey.features.gui.items.buttons;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class BooleanButton extends Button {
   private final Setting<Boolean> setting;

   public BooleanButton(Setting<Boolean> setting) {
      super(setting.getName());
      this.setting = setting;
   }

   @Override
   public void drawScreen(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTicks) {
      boolean enabled = this.getState();
      boolean hovering = this.isHovering(mouseX, mouseY);
      int accentFull = OyVey.colorManager.getColorWithAlpha((int)this.y, 255);
      int top = enabled ? -1339545322 : (hovering ? -1877737438 : -2012607982);
      int bottom = enabled ? -1611925226 : (hovering ? -1995177961 : -2130702300);
      RenderUtil.roundedRectGradient(context, this.x + 1.0F, this.y + 0.5F, this.width - 2.0F, this.height - 1.0F, 3.5F, top, bottom, true);

      int boxSize = 6;
      int bx = (int)(this.x + this.width - boxSize - 3.0F);
      int by = (int)(this.y + (this.height - boxSize) / 2.0F);
      RenderUtil.roundedRect(context, bx, by, boxSize, boxSize, 2, 1157627903);
      if (enabled) {
         RenderUtil.roundedRect(context, bx + 1, by + 1, boxSize - 2, boxSize - 2, 1, accentFull);
      }

      RenderUtil.roundedOutline(context, this.x + 1.0F, this.y + 0.5F, this.width - 2.0F, this.height - 1.0F, 3.5F, 0.6F, hovering ? 687865855 : 301989887);
      this.drawString(this.getName(), this.x + 5.0F, this.y + 4.0F, enabled ? -1 : -5592406);
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (this.isHovering(mouseX, mouseY)) {
         mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      }
   }

   @Override
   public void toggle() {
      this.setting.setValue(!this.setting.getValue());
   }

   @Override
   public boolean getState() {
      return this.setting.getValue();
   }
}
