package me.alpha432.oyvey.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public final class AuthClient {
   public static final String BASE = "https://authsr.nxc.tools";
   private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).build();

   private AuthClient() {
   }

   public static AuthClient.Result register(String username, String password) {
      return post("/register", credBody(username, password), null);
   }

   public static AuthClient.Result login(String username, String password) {
      return post("/login", credBody(username, password), null);
   }

   public static AuthClient.Result refresh(String refreshToken) {
      return post("/refresh", "{\"refresh_token\":\"" + esc(refreshToken) + "\"}", null);
   }

   public static AuthClient.Result changePassword(String accessToken, String oldPassword, String newPassword) {
      String body = "{\"old_password\":\"" + esc(oldPassword) + "\",\"new_password\":\"" + esc(newPassword) + "\"}";
      return post("/change-password", body, accessToken);
   }

   public static AuthClient.Result validate(String accessToken) {
      return post("/validate", "{}", accessToken);
   }

   private static AuthClient.Result post(String path, String json, String bearer) {
      try {
         Builder b = HttpRequest.newBuilder(URI.create("https://authsr.nxc.tools" + path))
            .timeout(Duration.ofSeconds(12L))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(json));
         if (bearer != null && !bearer.isEmpty()) {
            b.header("Authorization", "Bearer " + bearer);
         }

         HttpResponse<String> resp = HTTP.send(b.build(), BodyHandlers.ofString());
         JsonObject obj = parse(resp.body());
         boolean ok = resp.statusCode() >= 200 && resp.statusCode() < 300;
         String err = null;
         if (!ok) {
            err = obj != null && obj.has("error") ? obj.get("error").getAsString() : "error " + resp.statusCode();
         }

         return new AuthClient.Result(ok, resp.statusCode(), err, obj);
      } catch (Exception e) {
         return new AuthClient.Result(false, 0, "cannot reach server", null);
      }
   }

   private static String credBody(String username, String password) {
      return "{\"username\":\"" + esc(username) + "\",\"password\":\"" + esc(password) + "\"}";
   }

   private static String esc(String s) {
      return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "").replace("\r", "");
   }

   private static JsonObject parse(String s) {
      try {
         return JsonParser.parseString(s).getAsJsonObject();
      } catch (Exception e) {
         return null;
      }
   }

   public record Result(boolean ok, int status, String error, JsonObject body) {
   }
}
