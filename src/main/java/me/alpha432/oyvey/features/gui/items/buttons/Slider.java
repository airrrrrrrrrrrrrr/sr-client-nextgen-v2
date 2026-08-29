package me.alpha432.oyvey.features.gui.items.buttons;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.gui.OyVeyGui;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

public class Slider extends Button {
   private final Number min;
   private final Number max;
   private final int difference;
   public Setting<Number> setting;

   public Slider(Setting<Number> setting) {
      super(setting.getName());
      this.setting = setting;
      this.min = setting.getMin();
      this.max = setting.getMax();
      this.difference = this.max.intValue() - this.min.intValue();
      this.width = 15;
   }

   @Override
   public void drawScreen(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTicks) {
      this.dragSetting(mouseX, mouseY);
      RenderUtil.roundedRect(
         context, this.x, this.y, this.width + 7.4F, this.height - 0.5F, 3.0F, !this.isHovering(mouseX, mouseY) ? 290805077 : -2007673515
      );
      float fillWidth = this.setting.getValue().floatValue() <= this.min.floatValue() ? 0.0F : (this.width + 7.4F) * this.partialMultiplier();
      if (fillWidth > 0.0F) {
         RenderUtil.roundedRect(
            context,
            this.x,
            this.y,
            fillWidth,
            this.height - 0.5F,
            3.0F,
            !this.isHovering(mouseX, mouseY)
               ? OyVey.colorManager.getColorWithAlpha(this.y, ClickGuiModule.getInstance().color.getValue().getAlpha())
               : OyVey.colorManager.getColorWithAlpha(this.y, ClickGuiModule.getInstance().topColor.getValue().getAlpha())
         );
      }
      this.drawString(
         this.getName()
            + " "
            + ChatFormatting.GRAY
            + (this.setting.getValue() instanceof Float ? this.setting.getValue() : this.setting.getValue().doubleValue()),
         this.x + 2.3F,
         this.y - 1.7F - OyVeyGui.getInstance().getTextOffset(),
         -1
      );
   }

   @Override
   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   private void dragSetting(int mouseX, int mouseY) {
      if (this.isHovering(mouseX, mouseY) && GLFW.glfwGetMouseButton(mc.getWindow().handle(), 0) == 1) {
         this.setSettingFromX(mouseX);
      }
   }

   private void setSettingFromX(int mouseX) {
      float percent = (mouseX - this.x) / (this.width + 7.4F);
      if (this.setting.getValue() instanceof Double) {
         double result = (Double)this.setting.getMin() + this.difference * percent;
         this.setting.setValue(Math.round(10.0 * result) / 10.0);
      } else if (this.setting.getValue() instanceof Float) {
         float result = this.setting.getMin().floatValue() + this.difference * percent;
         this.setting.setValue(Math.round(10.0F * result) / 10.0F);
      } else if (this.setting.getValue() instanceof Integer) {
         this.setting.setValue((Integer)this.setting.getMin() + (int)(this.difference * percent));
      }
   }

   private float middle() {
      return this.max.floatValue() - this.min.floatValue();
   }

   private float part() {
      return this.setting.getValue().floatValue() - this.min.floatValue();
   }

   private float partialMultiplier() {
      return this.part() / this.middle();
   }
}
