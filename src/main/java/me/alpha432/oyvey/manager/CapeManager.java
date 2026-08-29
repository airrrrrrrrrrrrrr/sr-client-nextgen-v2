package me.alpha432.oyvey.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import me.alpha432.oyvey.features.cape.CapeEntry;
import me.alpha432.oyvey.features.cape.CapeTextureLoader;
import me.alpha432.oyvey.util.traits.Jsonable;

public class CapeManager implements Jsonable {
   private final List<CapeEntry> capes = new ArrayList<>();
   private int activeIndex = -1;

   public List<CapeEntry> getCapes() {
      return this.capes;
   }

   public int getActiveIndex() {
      return this.activeIndex;
   }

   public CapeEntry getActiveCape() {
      return this.activeIndex >= 0 && this.activeIndex < this.capes.size() ? this.capes.get(this.activeIndex) : null;
   }

   public boolean hasActiveCape() {
      return this.getActiveCape() != null;
   }

   public void setActiveIndex(int idx) {
      this.activeIndex = idx >= 0 && idx < this.capes.size() ? idx : -1;
   }

   public void addCape(String name, String source) {
      this.capes.add(new CapeEntry(name.trim(), source.trim()));
   }

   public void removeCape(int idx) {
      if (idx >= 0 && idx < this.capes.size()) {
         CapeEntry removed = this.capes.remove(idx);
         CapeTextureLoader.invalidate(removed.source());
         if (this.activeIndex == idx) {
            this.activeIndex = -1;
         } else if (this.activeIndex > idx) {
            this.activeIndex--;
         }
      }
   }

   @Override
   public JsonElement toJson() {
      JsonObject obj = new JsonObject();
      obj.addProperty("active", this.activeIndex);
      JsonArray arr = new JsonArray();

      for (CapeEntry entry : this.capes) {
         JsonObject e = new JsonObject();
         e.addProperty("name", entry.name());
         e.addProperty("source", entry.source());
         arr.add(e);
      }

      obj.add("capes", arr);
      return obj;
   }

   @Override
   public void fromJson(JsonElement element) {
      this.capes.clear();
      if (element.isJsonObject()) {
         JsonObject obj = element.getAsJsonObject();
         this.activeIndex = obj.has("active") ? obj.get("active").getAsInt() : -1;
         if (obj.has("capes")) {
            for (JsonElement e : obj.getAsJsonArray("capes")) {
               if (e.isJsonObject()) {
                  JsonObject entry = e.getAsJsonObject();
                  String name = entry.has("name") ? entry.get("name").getAsString() : "";
                  String source = entry.has("source") ? entry.get("source").getAsString() : "";
                  if (!name.isEmpty() && !source.isEmpty()) {
                     this.capes.add(new CapeEntry(name, source));
                  }
               }
            }
         }

         if (this.activeIndex >= this.capes.size()) {
            this.activeIndex = -1;
         }
      }
   }

   @Override
   public String getFileName() {
      return "capes.json";
   }
}
