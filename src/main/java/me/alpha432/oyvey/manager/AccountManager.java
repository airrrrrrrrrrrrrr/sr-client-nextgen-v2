package me.alpha432.oyvey.manager;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.alpha432.oyvey.mixin.render.accessor.MinecraftAccessor;
import me.alpha432.oyvey.util.traits.Jsonable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;

public class AccountManager implements Jsonable {
   private final List<String> accounts = new ArrayList<>();
   private final Set<String> microsoftAccounts = new HashSet<>();
   private final Map<String, String> microsoftSessions = new HashMap<>();

   public void init() {
      String current = this.getCurrentName();
      if (!current.isEmpty() && !this.accounts.contains(current)) {
         this.accounts.add(0, current);
      }
   }

   public List<String> getAccounts() {
      return this.accounts;
   }

   public String getCurrentName() {
      Minecraft mc = Minecraft.getInstance();
      return mc != null ? mc.getUser().getName() : "";
   }

   public void addAccount(String username) {
      username = username.trim();
      if (!username.isEmpty() && !this.accounts.contains(username)) {
         this.accounts.add(username);
      }
   }

   public void removeAccount(String username) {
      this.accounts.remove(username);
      this.microsoftAccounts.remove(username);
      this.microsoftSessions.remove(username);
   }

   public boolean isMicrosoft(String username) {
      return this.microsoftAccounts.contains(username);
   }

   public void saveMicrosoftSession(String username, String sessionJson) {
      this.microsoftAccounts.add(username);
      if (sessionJson != null) {
         this.microsoftSessions.put(username, sessionJson);
      }
   }

   public String getMicrosoftSession(String username) {
      return this.microsoftSessions.get(username);
   }

   public boolean setOnlineSession(String name, UUID uuid, String accessToken) {
      try {
         User newUser = new User(name, uuid, accessToken, Optional.empty(), Optional.empty());
         Minecraft mc = Minecraft.getInstance();
         YggdrasilAuthenticationService authenticationService = new YggdrasilAuthenticationService(mc.getProxy());
         UserApiService userApiService = authenticationService.createUserApiService(accessToken);
         ProfileKeyPairManager profileKeyPairManager = ProfileKeyPairManager.create(userApiService, newUser, mc.gameDirectory.toPath());
         MinecraftAccessor accessor = (MinecraftAccessor)mc;
         accessor.oyvey$setUser(newUser);
         accessor.oyvey$setUserApiService(userApiService);
         accessor.oyvey$setProfileKeyPairManager(profileKeyPairManager);
         accessor.oyvey$setProfileFuture(
            CompletableFuture.completedFuture(new ProfileResult(new GameProfile(uuid, name)))
         );
         if (!this.accounts.contains(name)) {
            this.accounts.add(name);
         }

         this.microsoftAccounts.add(name);
         return true;
      } catch (Exception var10) {
      }

      return false;
   }

   public boolean switchAccount(String username) {
      try {
         UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
         User newUser = new User(username, uuid, "", Optional.empty(), Optional.empty());
         Minecraft mc = Minecraft.getInstance();

         for (Field f : Minecraft.class.getDeclaredFields()) {
            if (f.getType() == User.class) {
               f.setAccessible(true);
               f.set(mc, newUser);
               return true;
            }
         }
      } catch (Exception var9) {
      }

      return false;
   }

   @Override
   public JsonElement toJson() {
      JsonObject obj = new JsonObject();
      JsonArray arr = new JsonArray();
      this.accounts.forEach(arr::add);
      obj.add("accounts", arr);
      JsonArray ms = new JsonArray();
      this.microsoftAccounts.forEach(ms::add);
      obj.add("microsoft", ms);
      JsonObject sessions = new JsonObject();
      this.microsoftSessions.forEach(sessions::addProperty);
      obj.add("sessions", sessions);
      return obj;
   }

   @Override
   public void fromJson(JsonElement element) {
      this.accounts.clear();
      this.microsoftAccounts.clear();
      this.microsoftSessions.clear();
      if (element.isJsonObject()) {
         JsonObject obj = element.getAsJsonObject();
         JsonArray arr = obj.getAsJsonArray("accounts");
         if (arr != null) {
            arr.forEach(e -> this.accounts.add(e.getAsString()));
         }

         JsonArray ms = obj.getAsJsonArray("microsoft");
         if (ms != null) {
            ms.forEach(e -> this.microsoftAccounts.add(e.getAsString()));
         }

         if (obj.has("sessions") && obj.get("sessions").isJsonObject()) {
            obj.getAsJsonObject("sessions").entrySet().forEach(en -> this.microsoftSessions.put(en.getKey(), en.getValue().getAsString()));
         }
      }
   }

   @Override
   public String getFileName() {
      return "accounts.json";
   }
}
