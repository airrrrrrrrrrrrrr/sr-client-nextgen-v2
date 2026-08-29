package me.alpha432.oyvey.features.gui;

import java.util.ArrayList;
import java.util.Comparator;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.Feature;
import me.alpha432.oyvey.features.gui.items.buttons.ModuleButton;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.HudModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudEditorScreen extends Screen {
   private static HudEditorScreen INSTANCE;
   private final ArrayList<Widget> components = new ArrayList<>();
   public HudModule currentDragging;
   public boolean anyHover;

   private HudEditorScreen() {
      super(Component.literal("srclient-hudeditor"));
      this.load();
   }

   private void load() {
      Widget hud = new Widget("Hud", 50, 50, true);
      OyVey.moduleManager.stream().filter(m -> m.getCategory() == Module.Category.HUD && !m.hidden).map(ModuleButton::new).forEach(hud::addButton);
      this.components.add(hud);
      this.components.forEach(component -> component.getItems().sort(Comparator.comparing(Feature::getName)));
   }

   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      this.anyHover = false;
      this.components.forEach(component -> component.drawScreen(context, mouseX, mouseY, delta));
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      this.components.forEach(component -> component.mouseClicked((int)click.x(), (int)click.y(), click.button()));
      return super.mouseClicked(click, doubled);
   }

   public boolean mouseReleased(MouseButtonEvent click) {
      this.components.forEach(component -> component.mouseReleased((int)click.x(), (int)click.y(), click.button()));
      return super.mouseReleased(click);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (verticalAmount < 0.0) {
         this.components.forEach(component -> component.setY(component.getY() - 10));
      } else if (verticalAmount > 0.0) {
         this.components.forEach(component -> component.setY(component.getY() + 10));
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean keyPressed(KeyEvent input) {
      this.components.forEach(component -> component.onKeyPressed(input.input()));
      return super.keyPressed(input);
   }

   public boolean charTyped(CharacterEvent input) {
      this.components.forEach(component -> component.onKeyTyped(input.codepointAsString(), 0));
      return super.charTyped(input);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
   }

   public ArrayList<Widget> getComponents() {
      return this.components;
   }

   public static HudEditorScreen getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new HudEditorScreen();
      }

      return INSTANCE;
   }
}
