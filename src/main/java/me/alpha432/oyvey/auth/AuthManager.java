package me.alpha432.oyvey.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.alpha432.oyvey.util.traits.Jsonable;

public class AuthManager implements Jsonable {
   private volatile String accessToken;
   private volatile long accessExpiresAt;
   private volatile String roles;
   private volatile String username;
   private volatile String refreshToken;
   private volatile boolean rememberMe = true;

   public boolean isAuthenticated() {
      return this.accessToken != null && !this.accessToken.isEmpty();
   }

   public boolean hasRememberedSession() {
      return this.refreshToken != null && !this.refreshToken.isEmpty();
   }

   public String getAccessToken() {
      return this.accessToken;
   }

   public String getRefreshToken() {
      return this.refreshToken;
   }

   public String getUsername() {
      return this.username;
   }

   public String getRoles() {
      return this.roles;
   }

   public boolean isRememberMe() {
      return this.rememberMe;
   }

   public void setRememberMe(boolean v) {
      this.rememberMe = v;
   }

   public void applySession(JsonObject body, boolean remember) {
      this.accessToken = str(body, "access_token", this.accessToken);
      this.refreshToken = str(body, "refresh_token", this.refreshToken);
      this.username = str(body, "username", this.username);
      this.roles = str(body, "roles", "user");
      long expiresIn = body != null && body.has("expires_in") ? body.get("expires_in").getAsLong() : 900L;
      this.accessExpiresAt = System.currentTimeMillis() + expiresIn * 1000L;
      this.rememberMe = remember;
   }

   public void logout() {
      this.accessToken = null;
      this.refreshToken = null;
      this.roles = null;
      this.accessExpiresAt = 0L;
   }

   @Override
   public JsonElement toJson() {
      JsonObject o = new JsonObject();
      o.addProperty("rememberMe", this.rememberMe);
      if (this.rememberMe && this.refreshToken != null && !this.refreshToken.isEmpty()) {
         o.addProperty("refreshToken", this.refreshToken);
         o.addProperty("username", this.username);
      }

      return o;
   }

   @Override
   public void fromJson(JsonElement element) {
      if (element != null && element.isJsonObject()) {
         JsonObject o = element.getAsJsonObject();
         if (o.has("rememberMe")) {
            this.rememberMe = o.get("rememberMe").getAsBoolean();
         }

         if (o.has("refreshToken")) {
            this.refreshToken = o.get("refreshToken").getAsString();
         }

         if (o.has("username")) {
            this.username = o.get("username").getAsString();
         }
      }
   }

   @Override
   public String getFileName() {
      return "auth.json";
   }

   private static String str(JsonObject o, String key, String def) {
      return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : def;
   }
}
