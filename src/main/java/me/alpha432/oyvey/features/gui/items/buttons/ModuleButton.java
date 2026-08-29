package me.alpha432.oyvey.features.gui.items.buttons;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import me.alpha432.oyvey.features.gui.items.Item;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Bind;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public class ModuleButton extends Button {
   private final Module module;
   private List<Item> items = new ArrayList<>();
   private boolean subOpen;

   public ModuleButton(Module module) {
      super(module.getName());
      this.module = module;
      this.initSettings();
   }

   public void initSettings() {
      ArrayList<Item> newItems = new ArrayList<>();
      if (!this.module.getSettings().isEmpty()) {
         for (Setting<?> setting : this.module.getSettings()) {
            if (setting.getValue() instanceof Boolean && !setting.getName().equals("Enabled")) {
               newItems.add(new BooleanButton((Setting<Boolean>)setting));
            }

            if (setting.getValue() instanceof Bind && !setting.getName().equalsIgnoreCase("Keybind") && !this.module.getName().equalsIgnoreCase("Hud")) {
               newItems.add(new BindButton((Setting<Bind>)setting));
            }

            if ((setting.getValue() instanceof String || setting.getValue() instanceof Character) && !setting.getName().equalsIgnoreCase("displayName")) {
               newItems.add(new StringButton((Setting<String>)setting));
            }

            if (setting.isColorSetting()) {
               newItems.add(new ColorButton((Setting<Color>)setting));
            } else if (setting.isNumberSetting() && setting.hasRestriction()) {
               newItems.add(new Slider((Setting<Number>)setting));
            } else if (setting.isEnumSetting()) {
               newItems.add(new EnumButton((Setting<Enum<?>>)setting));
            }
         }
      }

      newItems.add(new BindButton((Setting<Bind>)this.module.getSettingByName("Keybind")));
      this.items = newItems;
   }

   @Override
   public void drawScreen(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTicks) {
      super.drawScreen(context, mouseX, mouseY, partialTicks);
      if (!this.items.isEmpty() && this.subOpen) {
         float height = 16.0F;

         for (Item item : this.items) {
            if (!item.isHidden()) {
               item.setLocation(this.x + 1.0F, this.y + height);
               item.setWidth(this.width - 9);
               item.drawScreen(context, mouseX, mouseY, partialTicks);
               height += item.getHeight() + 1.0F;
            }

            item.update();
         }
      }
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (!this.items.isEmpty()) {
         if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            this.subOpen = !this.subOpen;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         }

         if (this.subOpen) {
            for (Item item : this.items) {
               if (!item.isHidden()) {
                  item.mouseClicked(mouseX, mouseY, mouseButton);
               }
            }
         }
      }
   }

   @Override
   public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
      super.mouseReleased(mouseX, mouseY, releaseButton);
      if (!this.items.isEmpty() && this.subOpen) {
         for (Item item : this.items) {
            if (!item.isHidden()) {
               item.mouseReleased(mouseX, mouseY, releaseButton);
            }
         }
      }
   }

   @Override
   public void onKeyTyped(String typedChar, int keyCode) {
      super.onKeyTyped(typedChar, keyCode);
      if (!this.items.isEmpty() && this.subOpen) {
         for (Item item : this.items) {
            if (!item.isHidden()) {
               item.onKeyTyped(typedChar, keyCode);
            }
         }
      }
   }

   @Override
   public void onKeyPressed(int key) {
      super.onKeyPressed(key);
      if (!this.items.isEmpty() && this.subOpen) {
         for (Item item : this.items) {
            if (!item.isHidden()) {
               item.onKeyPressed(key);
            }
         }
      }
   }

   @Override
   public int getHeight() {
      if (this.subOpen) {
         int height = 16;

         for (Item item : this.items) {
            if (!item.isHidden()) {
               height += item.getHeight() + 1;
            }
         }

         return height;
      } else {
         return 14;
      }
   }

   public Module getModule() {
      return this.module;
   }

   @Override
   public void toggle() {
      this.module.toggle();
   }

   @Override
   public boolean getState() {
      return this.module.isEnabled();
   }
}
