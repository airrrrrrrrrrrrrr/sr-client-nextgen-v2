package me.alpha432.oyvey.features.modules.client;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.input.MouseInputEvent;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.gui.HudEditorScreen;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.gui.screens.ChatScreen;
import org.joml.Vector2f;

public abstract class HudModule extends Module {
   public final Setting<Vector2f> pos = this.vec2f("Position", 0.5F, 0.5F);
   private float dragX;
   private float dragY;
   private float width;
   private float height;
   private boolean dragging;
   private boolean button;

   public HudModule(String name, String description, float width, float height) {
      super(name, description, Module.Category.HUD);
      this.width = width;
      this.height = height;
   }

   public HudModule(String name, String description, float width, float height, Module.Category category) {
      super(name, description, category);
      this.width = width;
      this.height = height;
   }

   public float getX() {
      return mc.getWindow().getGuiScaledWidth() * this.pos.getValue().x();
   }

   public float getY() {
      float heightWithChat = mc.getWindow().getGuiScaledHeight() - 14;
      float baseY = mc.getWindow().getGuiScaledHeight() * this.pos.getValue().y();
      float combined = baseY + this.getHeight();
      if (mc.screen instanceof ChatScreen) {
         baseY = Math.min(combined, heightWithChat) - this.getHeight();
      }

      return baseY;
   }

   @Subscribe
   public void onRender2DHud(Render2DEvent e) {
      this.render(e);
   }

   @Subscribe
   public void onMouse(MouseInputEvent e) {
      if (mc.screen instanceof HudEditorScreen && !nullCheck()) {
         if (e.getAction() == 0) {
            this.button = false;
            this.dragging = false;
            HudEditorScreen.getInstance().currentDragging = null;
         }

         if (e.getAction() == 1 && this.isHovering()) {
            this.button = true;
         }
      }
   }

   protected void render(Render2DEvent e) {
      if (mc.screen instanceof HudEditorScreen && !nullCheck()) {
         float x = this.getX();
         float y = this.getY();
         if (this.button) {
            if (!this.dragging && this.isHovering() && HudEditorScreen.getInstance().currentDragging == null) {
               this.dragX = this.getMouseX() - x;
               this.dragY = this.getMouseY() - y;
               this.dragging = true;
               HudEditorScreen.getInstance().currentDragging = this;
            }

            if (this.dragging) {
               float finalX = Math.min(Math.max(this.getMouseX() - this.dragX, 0.0F), mc.getWindow().getGuiScaledWidth() - this.width);
               float finalY = Math.min(Math.max(this.getMouseY() - this.dragY, 0.0F), mc.getWindow().getGuiScaledHeight() - this.height);
               this.pos.getValue().x = finalX / mc.getWindow().getGuiScaledWidth();
               this.pos.getValue().y = finalY / mc.getWindow().getGuiScaledHeight();
            }
         } else {
            this.dragging = false;
         }

         boolean shouldDrawDescription = this.isHovering() && !HudEditorScreen.getInstance().anyHover;
         if (HudEditorScreen.getInstance().currentDragging != null) {
            shouldDrawDescription = HudEditorScreen.getInstance().currentDragging == this;
         }

         if (shouldDrawDescription) {
            String displayName = "sr client nextgen";
            int textWidth = mc.font.width(displayName);
            int textHeight = 9;
            float textX = x + this.width + 5.0F;
            if (textX + textWidth > mc.getWindow().getGuiScaledWidth()) {
               textX = x - 5.0F - textWidth;
            }

            e.getContext().text(mc.font, displayName, (int)textX, (int)(y + this.height / 2.0F - textHeight / 2.0F), -1);
            HudEditorScreen.getInstance().anyHover = true;
         }

         RenderUtil.roundedOutline(e.getContext(), x - 1.0F, y - 1.0F, this.width + 2.0F, this.height + 2.0F, 4.0F, 1.0F, OyVey.colorManager.getColor().getRGB());
      }
   }

   public int getMouseX() {
      return (int)(mc.mouseHandler.xpos() / mc.getWindow().getGuiScale());
   }

   public int getMouseY() {
      return (int)(mc.mouseHandler.ypos() / mc.getWindow().getGuiScale());
   }

   public void setBounds(float x, float y, float width, float height) {
      this.width = width;
      this.height = height;
      this.pos.getValue().x = x / mc.getWindow().getGuiScaledWidth();
      this.pos.getValue().y = y / mc.getWindow().getGuiScaledHeight();
   }

   public boolean isHovering() {
      float x = this.getX();
      float y = this.getY();
      int mouseX = this.getMouseX();
      int mouseY = this.getMouseY();
      return mouseX >= x - 1.0F && mouseX <= x + this.width + 1.0F && mouseY >= y - 1.0F && mouseY <= y + this.height + 1.0F;
   }

   public float getWidth() {
      return this.width;
   }

   public float getHeight() {
      return this.height;
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public void setHeight(float height) {
      this.height = height;
   }
}
