package me.alpha432.oyvey.features.gui.items.buttons;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.settings.Bind;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class BindButton extends Button {
   private final Setting<Bind> setting;
   public boolean isListening;

   public BindButton(Setting<Bind> setting) {
      super(setting.getName());
      this.setting = setting;
   }

   @Override
   public void drawScreen(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTicks) {
      boolean hovering = this.isHovering(mouseX, mouseY);
      int accentFull = OyVey.colorManager.getColorWithAlpha((int)this.y, 255);
      int top = this.isListening ? -854585576 : (hovering ? -1877737438 : -2012607982);
      int bottom = this.isListening ? -1127377128 : (hovering ? -1995177961 : -2130702300);
      RenderUtil.roundedRectGradient(context, this.x + 1.0F, this.y + 0.5F, this.width - 2.0F, this.height - 1.0F, 3.5F, top, bottom, true);
      RenderUtil.roundedOutline(context, this.x + 1.0F, this.y + 0.5F, this.width - 2.0F, this.height - 1.0F, 3.5F, 0.65F, hovering ? 855638015 : 419430399);

      if (this.isListening) {
         RenderUtil.roundedRect(context, this.x + 3.0F, this.y + 3.0F, 2.0F, this.height - 6.0F, 1.0F, accentFull);
      }

      if (this.isListening) {
         this.drawString("Press a Key...", this.x + 5.0F, this.y + 4.0F, accentFull);
      } else {
         String str = this.setting.getValue().toString().toUpperCase().replace("KEY.KEYBOARD", "").replace(".", " ").trim();
         String label = this.setting.getName()
            + " "
            + ChatFormatting.DARK_GRAY
            + "["
            + ChatFormatting.RESET
            + ChatFormatting.GRAY
            + str
            + ChatFormatting.DARK_GRAY
            + "]";
         this.drawString(label, this.x + 5.0F, this.y + 4.0F, -5592406);
      }
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (this.isListening) {
         if (mouseButton != 0 && mouseButton != 1) {
            this.setting.setValue(new Bind(-mouseButton - 2));
            this.toggle();
         }
      } else if (this.isHovering(mouseX, mouseY)) {
         mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         this.toggle();
      }
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.isListening) {
         Bind bind = new Bind(key);
         if (key == 261 || key == 259 || key == 256) {
            bind = new Bind(-1);
         }

         this.setting.setValue(bind);
         this.toggle();
      }
   }

   @Override
   public void toggle() {
      this.isListening = !this.isListening;
   }

   @Override
   public boolean getState() {
      return !this.isListening;
   }
}
