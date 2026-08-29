package me.alpha432.oyvey.features.modules.client;

import java.awt.Color;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.ClientEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.commands.Command;
import me.alpha432.oyvey.features.gui.DropdownGui;
import me.alpha432.oyvey.features.gui.FancyGui;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;

public class ClickGuiModule extends Module {
   private static ClickGuiModule INSTANCE;
   public Setting<Boolean> customMenu = this.bool("CustomMenu", true);
   public Setting<String> prefix = this.str("Prefix", ".");
   public Setting<Color> color = this.color("Color", 0, 0, 255, 180);
   public Setting<Color> topColor = this.color("TopColor", 0, 0, 150, 240);
   public Setting<Boolean> rainbow = this.bool("Rainbow", false);
   public Setting<Integer> rainbowHue = this.num("Delay", 240, 0, 600);
   public Setting<Float> rainbowBrightness = this.num("Brightness", 150.0F, 1.0F, 255.0F);
   public Setting<Float> rainbowSaturation = this.num("Saturation", 150.0F, 1.0F, 255.0F);
   public Setting<ClickGuiModule.GuiMode> mode = this.register(new Setting<>("Mode", ClickGuiModule.GuiMode.TAB));
   public Setting<Float> guiScale = this.num("GuiScale", 1.0F, 0.5F, 2.0F);
   public Setting<ClickGuiModule.Language> language = this.register(new Setting<>("Language", ClickGuiModule.Language.ENGLISH));

   public ClickGuiModule() {
      super("ClickGui", "Opens the ClickGui", Module.Category.CLIENT);
      this.setBind(344);
      this.rainbowHue.setVisibility(v -> this.rainbow.getValue());
      this.rainbowBrightness.setVisibility(v -> this.rainbow.getValue());
      this.rainbowSaturation.setVisibility(v -> this.rainbow.getValue());
      INSTANCE = this;
   }

   @Subscribe
   public void onSettingChange(ClientEvent event) {
      if (event.getType() == ClientEvent.Type.SETTING_UPDATE && event.getSetting().getFeature().equals(this)) {
         if (event.getSetting().equals(this.prefix)) {
            OyVey.commandManager.setCommandPrefix(this.prefix.getPlannedValue());
            Command.sendMessage("Prefix set to {global} %s", OyVey.commandManager.getCommandPrefix());
         }

         if (event.getSetting().equals(this.color)) {
            OyVey.colorManager.setColor(this.color.getPlannedValue());
         }
      }
   }

   @Override
   public void onEnable() {
      if (!nullCheck()) {
         switch ((ClickGuiModule.GuiMode)this.mode.getValue()) {
            case DROPDOWN:
               mc.setScreen(DropdownGui.getInstance());
               break;
            default:
               mc.setScreen(new FancyGui());
         }
      }
   }

   @Override
   public void onLoad() {
      this.customMenu.setValue(true);
      OyVey.colorManager.setColor(this.color.getValue());
      OyVey.commandManager.setCommandPrefix(this.prefix.getValue());
   }

   @Override
   public void onTick() {
      boolean guiOpen = mc.screen instanceof FancyGui || mc.screen instanceof DropdownGui;
      if (!guiOpen) {
         this.disable();
      }
   }

   public static ClickGuiModule getInstance() {
      return INSTANCE;
   }

   public static String t(String key) {
      ClickGuiModule cgm = getInstance();
      ClickGuiModule.Language lang = cgm != null ? cgm.language.getValue() : ClickGuiModule.Language.ENGLISH;

      return switch (lang) {
         case SPANISH -> {
            switch (key) {
               case "Bind":
                  yield "Tecla";
               case "Press key...":
                  yield "Presiona tecla...";
               case "None":
                  yield "Ninguna";
               case "Scripts":
                  yield "Scripts";
               case "[ Reload Scripts ]":
                  yield "[ Recargar ]";
               case "[ Reload ]":
                  yield "[ Recargar ]";
               case "Configs":
                  yield "Configs";
               case "CONFIGS":
                  yield "CONFIGS";
               case "No configs saved yet":
                  yield "Sin configs guardados";
               case "No configs yet":
                  yield "Sin configs";
               case "Load":
                  yield "Cargar";
               case "Save":
                  yield "Guardar";
               case "name...":
                  yield "nombre...";
               case "Combat":
                  yield "Combate";
               case "Misc":
                  yield "Varios";
               case "Render":
                  yield "Render";
               case "Movement":
                  yield "Movimiento";
               case "Player":
                  yield "Jugador";
               case "Client":
                  yield "Cliente";
               default:
                  yield key;
            }
         }
         case GERMAN -> {
            switch (key) {
               case "Bind":
                  yield "Taste";
               case "Press key...":
                  yield "Taste druecken...";
               case "None":
                  yield "Keine";
               case "Scripts":
                  yield "Skripte";
               case "[ Reload Scripts ]":
                  yield "[ Neu laden ]";
               case "[ Reload ]":
                  yield "[ Neu laden ]";
               case "Configs":
                  yield "Konfigs";
               case "CONFIGS":
                  yield "KONFIGS";
               case "No configs saved yet":
                  yield "Keine Konfigs gespeichert";
               case "No configs yet":
                  yield "Keine Konfigs";
               case "Load":
                  yield "Laden";
               case "Save":
                  yield "Speichern";
               case "name...":
                  yield "Name...";
               case "Combat":
                  yield "Kampf";
               case "Misc":
                  yield "Sonstiges";
               case "Render":
                  yield "Grafik";
               case "Movement":
                  yield "Bewegung";
               case "Player":
                  yield "Spieler";
               case "Client":
                  yield "Client";
               default:
                  yield key;
            }
         }
         default -> key;
      };
   }

   public enum GuiMode {
      DROPDOWN,
      TAB;
   }

   public enum Language {
      ENGLISH,
      SPANISH,
      GERMAN;
   }
}
