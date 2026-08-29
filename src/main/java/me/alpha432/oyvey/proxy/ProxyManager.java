package me.alpha432.oyvey.proxy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import me.alpha432.oyvey.util.traits.Jsonable;

public class ProxyManager implements Jsonable {
   private final List<ProxyEntry> proxies = new ArrayList<>();
   private int activeIndex = -1;

   public List<ProxyEntry> getProxies() {
      return this.proxies;
   }

   public ProxyEntry getActive() {
      return this.activeIndex >= 0 && this.activeIndex < this.proxies.size() ? this.proxies.get(this.activeIndex) : null;
   }

   public int getActiveIndex() {
      return this.activeIndex;
   }

   public void setActiveIndex(int index) {
      this.activeIndex = index >= 0 && index < this.proxies.size() ? index : -1;
   }

   public void clearActive() {
      this.activeIndex = -1;
   }

   public void add(ProxyEntry entry) {
      this.proxies.add(entry);
   }

   public void remove(int index) {
      if (index >= 0 && index < this.proxies.size()) {
         this.proxies.remove(index);
         if (this.activeIndex == index) {
            this.activeIndex = -1;
         } else if (this.activeIndex > index) {
            this.activeIndex--;
         }
      }
   }

   @Override
   public JsonElement toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("activeIndex", this.activeIndex);
      JsonArray arr = new JsonArray();

      for (ProxyEntry p : this.proxies) {
         JsonObject o = new JsonObject();
         o.addProperty("type", p.getType().name());
         o.addProperty("host", p.getHost());
         o.addProperty("port", p.getPort());
         o.addProperty("username", p.getUsername());
         o.addProperty("password", p.getPassword());
         arr.add(o);
      }

      root.add("proxies", arr);
      return root;
   }

   @Override
   public void fromJson(JsonElement element) {
      this.proxies.clear();
      this.activeIndex = -1;
      if (element != null && element.isJsonObject()) {
         JsonObject root = element.getAsJsonObject();
         if (root.has("proxies") && root.get("proxies").isJsonArray()) {
            for (JsonElement e : root.getAsJsonArray("proxies")) {
               if (e.isJsonObject()) {
                  JsonObject o = e.getAsJsonObject();

                  try {
                     ProxyEntry.Type type = ProxyEntry.Type.valueOf(o.get("type").getAsString());
                     String host = o.has("host") ? o.get("host").getAsString() : "";
                     int port = o.has("port") ? o.get("port").getAsInt() : 0;
                     String user = o.has("username") ? o.get("username").getAsString() : "";
                     String pass = o.has("password") ? o.get("password").getAsString() : "";
                     this.proxies.add(new ProxyEntry(type, host, port, user, pass));
                  } catch (Exception var11) {
                  }
               }
            }
         }

         if (root.has("activeIndex")) {
            this.setActiveIndex(root.get("activeIndex").getAsInt());
         }
      }
   }

   @Override
   public String getFileName() {
      return "proxies.json";
   }
}
