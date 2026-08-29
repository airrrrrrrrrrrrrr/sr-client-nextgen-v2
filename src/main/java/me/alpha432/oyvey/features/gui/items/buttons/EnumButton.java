package me.alpha432.oyvey.features.gui.items.buttons;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.gui.OyVeyGui;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class EnumButton extends Button {
   public Setting<Enum<?>> setting;

   public EnumButton(Setting<Enum<?>> setting) {
      super(setting.getName());
      this.setting = setting;
      this.width = 15;
   }

   @Override
   public void drawScreen(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTicks) {
      RenderUtil.roundedRect(
         context,
         this.x,
         this.y,
         this.width + 7.4F,
         this.height - 0.5F,
         3.0F,
         this.getState()
            ? (
               !this.isHovering(mouseX, mouseY)
                  ? OyVey.colorManager.getColorWithAlpha(this.y, ClickGuiModule.getInstance().color.getValue().getAlpha())
                  : OyVey.colorManager.getColorWithAlpha(this.y, ClickGuiModule.getInstance().topColor.getValue().getAlpha())
            )
            : (!this.isHovering(mouseX, mouseY) ? 290805077 : -2007673515)
      );
      this.drawString(
         this.setting.getName() + " " + ChatFormatting.GRAY + (this.setting.currentEnumName().equalsIgnoreCase("ABC") ? "ABC" : this.setting.currentEnumName()),
         this.x + 2.3F,
         this.y - 1.7F - OyVeyGui.getInstance().getTextOffset(),
         this.getState() ? -1 : -5592406
      );
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
      this.setting.increaseEnum();
   }

   @Override
   public boolean getState() {
      return true;
   }
}
