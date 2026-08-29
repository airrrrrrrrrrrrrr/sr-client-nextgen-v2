package me.alpha432.oyvey.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Color;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import me.alpha432.oyvey.features.modules.render.ArrayList;
import me.alpha432.oyvey.util.traits.Jsonable;

public class PreferencesManager implements Jsonable {
   private boolean showOnLaunch = true;
   private PreferencesManager.Language language = PreferencesManager.Language.ENGLISH;
   private PreferencesManager.ThemeColor themeColor = PreferencesManager.ThemeColor.BLUE;
   private PreferencesManager.Server server = PreferencesManager.Server.NONE;

   public boolean isShowOnLaunch() {
      return this.showOnLaunch;
   }

   public void setShowOnLaunch(boolean v) {
      this.showOnLaunch = v;
   }

   public PreferencesManager.Language getLanguage() {
      return this.language;
   }

   public void setLanguage(PreferencesManager.Language v) {
      this.language = v;
   }

   public PreferencesManager.ThemeColor getThemeColor() {
      return this.themeColor;
   }

   public void setThemeColor(PreferencesManager.ThemeColor v) {
      this.themeColor = v;
   }

   public PreferencesManager.Server getServer() {
      return this.server;
   }

   public void setServer(PreferencesManager.Server v) {
      this.server = v;
   }

   public void applyTheme() {
      ClickGuiModule cfg = ClickGuiModule.getInstance();
      if (cfg != null) {
         Color c = this.themeColor.toColor(200);
         cfg.color.setValue(c);
         cfg.topColor
            .setValue(new Color(Math.max(0, this.themeColor.r() - 40), Math.max(0, this.themeColor.g() - 40), Math.max(0, this.themeColor.b() - 40), 240));
         OyVey.colorManager.setColor(c);
      }

      if (OyVey.moduleManager != null) {
         OyVey.moduleManager.getModules().stream().filter(m -> m instanceof ArrayList).map(m -> (ArrayList)m).findFirst().ifPresent(al -> {
            al.r.setValue(this.themeColor.r());
            al.g.setValue(this.themeColor.g());
            al.b.setValue(this.themeColor.b());
         });
      }
   }

   @Override
   public JsonElement toJson() {
      JsonObject obj = new JsonObject();
      obj.addProperty("showOnLaunch", this.showOnLaunch);
      obj.addProperty("language", this.language.name());
      obj.addProperty("themeColor", this.themeColor.name());
      obj.addProperty("server", this.server.name());
      return obj;
   }

   @Override
   public void fromJson(JsonElement element) {
      if (element != null && element.isJsonObject()) {
         JsonObject obj = element.getAsJsonObject();
         if (obj.has("showOnLaunch")) {
            this.showOnLaunch = obj.get("showOnLaunch").getAsBoolean();
         } else if (obj.has("firstLaunch") && !obj.get("firstLaunch").getAsBoolean()) {
            this.showOnLaunch = true;
         }

         if (obj.has("language")) {
            try {
               this.language = PreferencesManager.Language.valueOf(obj.get("language").getAsString());
            } catch (Exception var6) {
            }
         }

         if (obj.has("themeColor")) {
            try {
               this.themeColor = PreferencesManager.ThemeColor.valueOf(obj.get("themeColor").getAsString());
            } catch (Exception var5) {
            }
         }

         if (obj.has("server")) {
            try {
               this.server = PreferencesManager.Server.valueOf(obj.get("server").getAsString());
            } catch (Exception var4) {
            }
         }
      }
   }

   @Override
   public String getFileName() {
      return "preferences.json";
   }

   public enum Language {
      ENGLISH,
      SPANISH,
      GERMAN;
   }

   public enum Server {
      NONE,
      DEATHZONE,
      HYPIXEL,
      OTHER;
   }

   public enum ThemeColor {
      BLACK(30, 30, 30),
      WHITE(210, 210, 215),
      RED(220, 30, 50),
      ORANGE(255, 120, 0),
      YELLOW(215, 195, 0),
      GREEN(0, 200, 80),
      CYAN(0, 210, 220),
      BLUE(0, 100, 255),
      PURPLE(120, 0, 220),
      PINK(220, 50, 160),
      TEAL(0, 180, 160),
      LIME(120, 220, 0);

      private final int r;
      private final int g;
      private final int b;

      ThemeColor(int r, int g, int b) {
         this.r = r;
         this.g = g;
         this.b = b;
      }

      public int r() {
         return this.r;
      }

      public int g() {
         return this.g;
      }

      public int b() {
         return this.b;
      }

      public Color toColor(int alpha) {
         return new Color(this.r, this.g, this.b, alpha);
      }
   }
}
