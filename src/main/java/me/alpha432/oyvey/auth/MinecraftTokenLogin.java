package me.alpha432.oyvey.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

/** Validates a Minecraft access token only against Minecraft's profile service. */
public final class MinecraftTokenLogin {
   private static final URI PROFILE = URI.create("https://api.minecraftservices.com/minecraft/profile");
   private MinecraftTokenLogin() { }

   public static void login(String token, Consumer<Profile> done, Consumer<String> failed) {
      Thread worker = new Thread(() -> {
         try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER).build()) {
            HttpRequest request = HttpRequest.newBuilder(PROFILE).timeout(Duration.ofSeconds(15))
               .header("Authorization", "Bearer " + normalize(token)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) { failed.accept(rejection(response.statusCode())); return; }
            done.accept(parseProfile(response.body()));
         } catch (Exception ignored) {
            // Never expose an HTTP exception, request header or response body in the UI/log.
            failed.accept("Token login failed. Check the token and your connection.");
         }
      }, "sr-minecraft-token-login");
      worker.setDaemon(true);
      worker.start();
   }

   public static String normalize(String token) {
      if (token == null) throw new IllegalArgumentException("Missing access token");
      String value = token.trim();
      if (value.regionMatches(true, 0, "Bearer ", 0, 7)) value = value.substring(7).trim();
      if (value.isEmpty() || value.equalsIgnoreCase("Bearer") || value.length() > 16384
         || value.chars().anyMatch(c -> c <= 32 || c >= 127))
         throw new IllegalArgumentException("Invalid access token");
      return value;
   }

   public static Profile parseProfile(String json) {
      JsonObject profile = JsonParser.parseString(json).getAsJsonObject();
      String name = profile.get("name").getAsString();
      String id = profile.get("id").getAsString();
      if (!name.matches("[A-Za-z0-9_]{1,16}") || !id.matches("[0-9a-fA-F]{32}"))
         throw new IllegalArgumentException("Invalid Minecraft profile");
      UUID uuid = new UUID(Long.parseUnsignedLong(id.substring(0, 16), 16), Long.parseUnsignedLong(id.substring(16), 16));
      if (uuid.equals(new UUID(0, 0))) throw new IllegalArgumentException("Missing Minecraft UUID");
      return new Profile(name, uuid);
   }

   public static String rejection(int status) {
      return switch (status) {
         case 401, 403 -> "Token is invalid or expired. Paste a fresh Minecraft access token.";
         case 404 -> "This token has no Minecraft Java profile.";
         case 429 -> "Too many sign-in attempts. Try again shortly.";
         default -> "Minecraft profile service is unavailable (HTTP " + status + ").";
      };
   }

   public record Profile(String name, UUID id) { }
}
