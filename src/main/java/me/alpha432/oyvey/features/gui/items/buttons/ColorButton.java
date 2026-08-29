package me.alpha432.oyvey.features.gui.items.buttons;

import java.awt.Color;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.gui.OyVeyGui;
import me.alpha432.oyvey.features.gui.Widget;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

public class ColorButton extends Button {
   private static final int PICKER_SIZE = 84;
   private final Setting<Color> setting;
   private boolean open = false;
   private boolean hoveringHue = false;
   private boolean hoveringColor = false;
   private boolean hoveringAlpha = false;
   private boolean hoveringCopy = false;
   private boolean hoveringPaste = false;
   private boolean draggingHue = false;
   private boolean draggingColor = false;
   private boolean draggingAlpha = false;
   private float[] hsb;

   public ColorButton(Setting<Color> setting) {
      super(setting.getName());
      this.setting = setting;
      this.width = 15;
      this.hsb = Color.RGBtoHSB(setting.getValue().getRed(), setting.getValue().getGreen(), setting.getValue().getBlue(), null);
   }

   @Override
   public void drawScreen(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTicks) {
      Color currentColor = this.setting.getValue();
      Color outlineColor = Color.BLACK;
      Color realColor = Color.getHSBColor(this.hsb[0], 1.0F, 1.0F);
      RenderUtil.roundedRect(
         context, this.x, this.y, this.width + 7.4F, this.height - 0.5F, 3.0F, !this.isHovering(mouseX, mouseY) ? 290805077 : -2007673515
      );
      RenderUtil.roundedRect(
         context,
         this.x + this.width - 4.0F,
         this.y + 3.0F,
         9.0F,
         this.height - 5.5F,
         2.5F,
         new Color(currentColor.getRGB(), false).getRGB()
      );
      RenderUtil.roundedOutline(context, this.x + this.width - 4.0F, this.y + 3.0F, 9.0F, this.height - 5.5F, 2.5F, 0.65F, 855638015);
      this.drawString(this.getName(), this.x + 2.3F, this.y - 1.7F - OyVeyGui.getInstance().getTextOffset(), -1);
      if (this.open) {
         float yOffset = this.height + 1;
         int availableWidth = this.width + 3;
         int pickerWidth = Math.min(84, availableWidth);
         float pickerX = this.x + 2.0F;
         int dragX = Mth.clamp(mouseX - (int)pickerX, 0, pickerWidth);
         int dragY = Mth.clamp(mouseY - (int)(this.getY() + yOffset), 0, pickerWidth);
         float dragHue = Math.max(pickerWidth * this.hsb[0] - 0.5F, 1.0F);
         float dragSaturation = Math.max(pickerWidth * this.hsb[1] - 1.0F, 2.0F);
         float dragBrightness = Math.max(pickerWidth * (1.0F - this.hsb[2]) - 1.0F, 2.0F);
         float dragAlpha = Math.max(pickerWidth * (currentColor.getAlpha() / 255.0F) - 0.5F, 1.0F);
         RenderUtil.horizontalGradient(context, pickerX, this.y + yOffset, pickerX + pickerWidth, this.y + yOffset + pickerWidth, Color.WHITE, realColor);
         RenderUtil.verticalGradient(
            context, pickerX, this.y + yOffset, pickerX + pickerWidth, this.y + yOffset + pickerWidth, new Color(0, 0, 0, 0), Color.BLACK
         );
         RenderUtil.roundedOutline(context, pickerX, this.y + yOffset, pickerWidth, pickerWidth, 4.0F, 1.0F, outlineColor.getRGB());
         this.hoveringColor = this.isHoveringArea(mouseX, mouseY, pickerX, this.y + yOffset, pickerX + pickerWidth, this.y + yOffset + pickerWidth);
         if (dragSaturation < pickerWidth && dragBrightness < pickerWidth) {
            RenderUtil.rect(
               context,
               pickerX + dragSaturation - 2.5F,
               this.y + yOffset + dragBrightness - 2.5F,
               pickerX + dragSaturation + 0.5F,
               this.y + yOffset + dragBrightness + 0.5F,
               outlineColor.getRGB()
            );
            RenderUtil.rect(
               context,
               pickerX + dragSaturation - 1.5F,
               this.y + yOffset + dragBrightness - 1.5F,
               pickerX + dragSaturation - 0.5F,
               this.y + yOffset + dragBrightness - 0.5F,
               Color.WHITE.getRGB()
            );
         }

         if (this.draggingColor) {
            this.hsb[1] = (float)dragX / pickerWidth;
            this.hsb[2] = 1.0F - (float)dragY / pickerWidth;
            this.setColor(this.hsb);
         }

         yOffset += pickerWidth + 2;
         RenderUtil.horizontalGradient(
            context,
            pickerX,
            this.y + yOffset,
            pickerX + pickerWidth,
            this.y + yOffset + 8.0F,
            new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), 0),
            new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), 255)
         );
         RenderUtil.roundedOutline(context, pickerX, this.y + yOffset, pickerWidth, 8.0F, 3.0F, 1.0F, outlineColor.getRGB());
         this.hoveringAlpha = this.isHoveringArea(mouseX, mouseY, pickerX, this.y + yOffset, pickerX + pickerWidth, this.y + yOffset + 8.0F);
         RenderUtil.rect(
            context, pickerX + dragAlpha - 1.5F, this.y + yOffset - 1.0F, pickerX + dragAlpha + 1.5F, this.y + yOffset + 9.0F, outlineColor.getRGB()
         );
         RenderUtil.rect(context, pickerX + dragAlpha - 0.5F, this.y + yOffset, pickerX + dragAlpha + 0.5F, this.y + yOffset + 8.0F, Color.WHITE.getRGB());
         if (this.draggingAlpha) {
            this.setColor(this.hsb, (int)(255.0F * dragX / pickerWidth));
         }

         yOffset += 10.0F;

         for (float i = 0.0F; i < pickerWidth; i += 0.5F) {
            RenderUtil.rect(
               context, pickerX + i, this.y + yOffset, pickerX + i + 0.5F, this.y + yOffset + 8.0F, Color.getHSBColor(i / pickerWidth, 1.0F, 1.0F).getRGB()
            );
         }

         RenderUtil.roundedOutline(context, pickerX, this.y + yOffset, pickerWidth, 8.0F, 3.0F, 1.0F, outlineColor.getRGB());
         this.hoveringHue = this.isHoveringArea(mouseX, mouseY, pickerX, this.y + yOffset, pickerX + pickerWidth, this.y + yOffset + 8.0F);
         if (dragHue < pickerWidth) {
            RenderUtil.rect(
               context, pickerX + dragHue - 1.5F, this.y + yOffset - 1.0F, pickerX + dragHue + 1.5F, this.y + yOffset + 9.0F, outlineColor.getRGB()
            );
            RenderUtil.rect(context, pickerX + dragHue - 0.5F, this.y + yOffset, pickerX + dragHue + 0.5F, this.y + yOffset + 8.0F, Color.WHITE.getRGB());
         }

         if (this.draggingHue) {
            this.hsb[0] = (float)dragX / pickerWidth;
            this.setColor(this.hsb);
         }

         yOffset += 10.0F;
         int buttonWidth = availableWidth / 2;
         RenderUtil.roundedRect(
            context,
            pickerX,
            this.y + yOffset,
            buttonWidth,
            14.0F,
            3.0F,
            this.hoveringCopy ? OyVey.colorManager.getColorWithAlpha(this.y, ClickGuiModule.getInstance().topColor.getValue().getAlpha()) : 290805077
         );
         this.drawString("Copy", pickerX + buttonWidth / 2.0 - mc.font.width("Copy") / 2.0, this.y + yOffset + 3.0F, -1);
         this.hoveringCopy = this.isHoveringArea(mouseX, mouseY, pickerX, this.y + yOffset, pickerX + buttonWidth, this.y + yOffset + 14.0F);
         RenderUtil.roundedRect(
            context,
            pickerX + buttonWidth + 1.0F,
            this.y + yOffset,
            buttonWidth,
            14.0F,
            3.0F,
            this.hoveringPaste ? OyVey.colorManager.getColorWithAlpha(this.y, ClickGuiModule.getInstance().topColor.getValue().getAlpha()) : 290805077
         );
         this.drawString("Paste", pickerX + buttonWidth + buttonWidth / 2.0 - mc.font.width("Paste") / 2.0 + 1.0, this.y + yOffset + 3.0F, -1);
         this.hoveringPaste = this.isHoveringArea(
            mouseX, mouseY, pickerX + buttonWidth + 1.0F, this.y + yOffset, pickerX + buttonWidth * 2 + 1.0F, this.y + yOffset + 14.0F
         );
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (this.isHovering(mouseX, mouseY) && mouseButton == 1) {
         this.open = !this.open;
         mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      }

      if (mouseButton == 0) {
         if (this.hoveringHue) {
            this.draggingHue = true;
         }

         if (this.hoveringColor) {
            this.draggingColor = true;
         }

         if (this.hoveringAlpha) {
            this.draggingAlpha = true;
         }

         if (this.hoveringCopy) {
            OyVeyGui.setColorClipboard(this.setting.getValue());
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         }

         if (this.hoveringPaste && OyVeyGui.getColorClipboard() != null) {
            this.setting.setValue(OyVeyGui.getColorClipboard());
            this.hsb = Color.RGBtoHSB(this.setting.getValue().getRed(), this.setting.getValue().getGreen(), this.setting.getValue().getBlue(), null);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         }
      }
   }

   @Override
   public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
      if (releaseButton == 0) {
         this.draggingHue = false;
         this.draggingColor = false;
         this.draggingAlpha = false;
      }
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   @Override
   public int getHeight() {
      if (!this.open) {
         return 14;
      }

      int pickerWidth = Math.min(84, this.width + 3);
      return 14 + pickerWidth + 8 + 8 + 14 + 8;
   }

   @Override
   public boolean isHovering(int mouseX, int mouseY) {
      for (Widget widget : OyVeyGui.getInstance().getComponents()) {
         if (widget.drag) {
            return false;
         }
      }

      return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() + 8.0F && mouseY >= this.getY() && mouseY < this.getY() + this.height;
   }

   private boolean isHoveringArea(int mouseX, int mouseY, float left, float top, float right, float bottom) {
      for (Widget widget : OyVeyGui.getInstance().getComponents()) {
         if (widget.drag) {
            return false;
         }
      }

      return left <= mouseX && top <= mouseY && right > mouseX && bottom > mouseY;
   }

   private void setColor(float[] hsb) {
      this.setColor(hsb, this.setting.getValue().getAlpha());
   }

   private void setColor(float[] hsb, int alpha) {
      Color color = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
      color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
      this.setting.setValue(color);
   }
}
