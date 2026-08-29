package me.alpha432.oyvey.features.gui;

import java.awt.Color;
import java.util.ArrayList;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.gui.items.Item;
import me.alpha432.oyvey.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Widget {
   public String name;
   public int x;
   public int y;
   public int width;
   public int height;
   public boolean open;
   public boolean drag;
   private int dragX;
   private int dragY;
   private final ArrayList<Item> items = new ArrayList<>();

   public Widget(String name, int x, int y, boolean open) {
      this.name = name;
      this.x = x;
      this.y = y;
      this.width = 110;
      this.height = 20;
      this.open = open;
   }

   public int getY() {
      return this.y;
   }

   public void setY(int y) {
      this.y = y;
   }

   public int getX() {
      return this.x;
   }

   public void setX(int x) {
      this.x = x;
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (this.isHovering(mouseX, mouseY)) {
         if (mouseButton == 0) {
            this.drag = true;
            this.dragX = mouseX - this.x;
            this.dragY = mouseY - this.y;
         } else if (mouseButton == 1) {
            this.open = !this.open;
         }
      } else if (this.open) {
         this.items.forEach(item -> item.mouseClicked(mouseX, mouseY, mouseButton));
      }
   }

   public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
      this.drag = false;
      if (this.open) {
         this.items.forEach(item -> item.mouseReleased(mouseX, mouseY, releaseButton));
      }
   }

   public void onKeyPressed(int key) {
      if (this.open) {
         this.items.forEach(item -> item.onKeyPressed(key));
      }
   }

   public void onKeyTyped(String character, int modifiers) {
      if (this.open) {
         this.items.forEach(item -> item.onKeyTyped(character, modifiers));
      }
   }

   public boolean isHovering(int mouseX, int mouseY) {
      return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
   }

   public void addButton(Item button) {
      this.items.add(button);
   }

   public ArrayList<Item> getItems() {
      return this.items;
   }

   public void drawScreen(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      if (this.drag) {
         this.x = mouseX - this.dragX;
         this.y = mouseY - this.dragY;
      }

      Minecraft mc = Minecraft.getInstance();
      int accentArgb = OyVey.colorManager.getColorWithAlpha(this.y, 255);
      Color accent = new Color(accentArgb, true);
      int accentDim = new Color(accent.getRed() * 45 / 100, accent.getGreen() * 45 / 100, accent.getBlue() * 45 / 100, 210).getRGB();
      int totalHeight = this.height;
      if (this.open) {
         for (Item item : this.items) {
            if (!item.isHidden()) {
               totalHeight += item.getHeight();
            }
         }
      }

      RenderUtil.dropShadow(context, this.x, this.y, this.width, totalHeight, 7.0F, 10.0F, 115);
      RenderUtil.roundedRectGradient(context, this.x, this.y, this.width, totalHeight, 7.0F, -301726456, -436204280, true);
      RenderUtil.roundedRectGradient(context, this.x + 1, this.y + 1, this.width - 2, this.height - 1, 6.0F, accentArgb, accentDim, true);
      RenderUtil.roundedOutline(context, this.x, this.y, this.width, totalHeight, 7.0F, 0.85F, 603979775);
      if (this.open) {
         int itemY = this.y + this.height;

         for (Item itemx : this.items) {
            if (!itemx.isHidden()) {
               itemx.setLocation(this.x, itemY);
               itemx.setWidth(this.width);
               itemx.drawScreen(context, mouseX, mouseY, delta);
               itemY += itemx.getHeight();
            }
         }
      }

      RenderUtil.roundedRect(context, this.x + 4.0F, this.y + 4.0F, 2.0F, Math.max(4.0F, totalHeight - 8.0F), 1.0F, accentArgb);
      RenderUtil.roundedOutline(context, this.x + 1, this.y + 1, this.width - 2, this.height - 1, 6.0F, 0.65F, 687865855);
      String label = this.name.toUpperCase();
      int tw = mc.font.width(label);
      context.text(mc.font, label, this.x + (this.width - tw) / 2, this.y + 6, -1, true);
   }
}
