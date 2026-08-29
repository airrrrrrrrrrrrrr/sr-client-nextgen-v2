package me.alpha432.oyvey.features.gui.items;

import me.alpha432.oyvey.features.Feature;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class Item extends Feature {
   public static GuiGraphicsExtractor context;
   protected float x;
   protected float y;
   protected int width;
   protected int height;
   private boolean hidden;

   public Item(String name) {
      super(name);
   }

   public void setLocation(float x, float y) {
      this.x = x;
      this.y = y;
   }

   public abstract void drawScreen(GuiGraphicsExtractor var1, int var2, int var3, float var4);

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
   }

   public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
   }

   public void onKeyTyped(String character, int modifiers) {
   }

   public void onKeyPressed(int key) {
   }

   public void onMouseClick() {
   }

   public void toggle() {
   }

   public void update() {
   }

   public boolean getState() {
      return false;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public int getWidth() {
      return this.width;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public int getHeight() {
      return this.height;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public boolean isHidden() {
      return this.hidden;
   }

   public boolean setHidden(boolean hidden) {
      this.hidden = hidden;
      return this.hidden;
   }

   protected void drawString(String text, double x, double y, int color) {
      if (context != null) {
         context.text(mc.font, text, (int)x, (int)y, color, true);
      }
   }

   public boolean isHovering(int mouseX, int mouseY) {
      return mouseX >= (int)this.x && mouseX <= (int)this.x + this.width && mouseY >= (int)this.y && mouseY <= (int)this.y + this.height;
   }
}
