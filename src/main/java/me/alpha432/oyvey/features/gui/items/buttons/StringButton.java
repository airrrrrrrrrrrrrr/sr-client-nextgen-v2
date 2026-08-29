package me.alpha432.oyvey.features.gui.items.buttons;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.gui.OyVeyGui;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.models.Timer;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class StringButton extends Button {
   private static final Timer idleTimer = new Timer();
   private static boolean idle;
   private final Setting<String> setting;
   public boolean isListening;
   private StringButton.CurrentString currentString = new StringButton.CurrentString("");

   public StringButton(Setting<String> setting) {
      super(setting.getName());
      this.setting = setting;
      this.width = 15;
   }

   public static String removeLastChar(String str) {
      String output = "";
      if (str != null && !str.isEmpty()) {
         output = str.substring(0, str.length() - 1);
      }

      return output;
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
      if (this.isListening) {
         this.drawString(
            this.currentString.string() + getIdleSign(), this.x + 2.3F, this.y - 1.7F - OyVeyGui.getInstance().getTextOffset(), this.getState() ? -1 : -5592406
         );
      } else {
         this.drawString(
            (this.setting.getName().equals("Buttons") ? "Buttons " : (this.setting.getName().equals("Prefix") ? "Prefix  " + ChatFormatting.GRAY : ""))
               + this.setting.getValue(),
            this.x + 2.3F,
            this.y - 1.7F - OyVeyGui.getInstance().getTextOffset(),
            this.getState() ? -1 : -5592406
         );
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (this.isHovering(mouseX, mouseY)) {
         mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      }
   }

   @Override
   public void onKeyTyped(String typedChar, int keyCode) {
      if (this.isListening) {
         this.setString(this.currentString.string() + typedChar);
      }
   }

   @Override
   public void onKeyPressed(int key) {
      if (this.isListening) {
         switch (key) {
            case 257:
               this.enterString();
            case 259:
               this.setString(removeLastChar(this.currentString.string()));
         }
      }
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   private void enterString() {
      if (this.currentString.string().isEmpty()) {
         this.setting.setValue(this.setting.getDefaultValue());
      } else {
         this.setting.setValue(this.currentString.string());
      }

      this.setString("");
      this.onMouseClick();
   }

   @Override
   public void toggle() {
      this.isListening = !this.isListening;
   }

   @Override
   public boolean getState() {
      return !this.isListening;
   }

   public void setString(String newString) {
      this.currentString = new StringButton.CurrentString(newString);
   }

   public static String getIdleSign() {
      if (idleTimer.passedMs(500L)) {
         idle = !idle;
         idleTimer.reset();
      }

      return idle ? "_" : "";
   }

   public record CurrentString(String string) {
   }
}
