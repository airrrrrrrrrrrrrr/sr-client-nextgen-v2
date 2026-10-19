package me.alpha432.oyvey.auth;

import com.google.gson.JsonObject;
import java.util.UUID;
import me.alpha432.oyvey.manager.AccountManager;

/** Credential validation and persistence checks without a real account or network. */
public final class AccountLoginChecks {
   private static int checks;

   public static void main(String[] args) {
      RotationRecordingChecks.run();
      require(MinecraftTokenLogin.normalize("  abc.def-ghi_123  ").equals("abc.def-ghi_123"), "trim pasted token");
      require(MinecraftTokenLogin.normalize("Bearer abc.def").equals("abc.def"), "accept pasted authorization value");
      require(MinecraftTokenLogin.normalize("bearer abc.def").equals("abc.def"), "case-insensitive bearer prefix");
      for (String value : new String[]{null, "", "   ", "Bearer ", "abc\ndef", "abc\rdef", "abc\tdef", "abc def", "é", "x".repeat(16385)}) {
         boolean rejected = false;
         try { MinecraftTokenLogin.normalize(value); } catch (IllegalArgumentException expected) { rejected = true; }
         require(rejected, "reject malformed/oversized credentials");
      }
      String id = "abcdef0123456789abcdef0123456789";
      var profile = MinecraftTokenLogin.parseProfile("{\"name\":\"Example_1\",\"id\":\"" + id + "\"}");
      require(profile.name().equals("Example_1"), "use service-validated profile name");
      require(profile.id().equals(UUID.fromString("abcdef01-2345-6789-abcd-ef0123456789")), "preserve signed UUID bits");
      for (String json : new String[]{"{}", "[]", "not json", "{\"name\":\"Bad name\",\"id\":\"" + id + "\"}",
         "{\"name\":\"Example\",\"id\":\"bad\"}", "{\"name\":\"Example\",\"id\":\"00000000000000000000000000000000\"}"}) {
         boolean rejected = false;
         try { MinecraftTokenLogin.parseProfile(json); } catch (RuntimeException expected) { rejected = true; }
         require(rejected, "reject incomplete/untrusted profile bodies before session swap");
      }
      require(MinecraftTokenLogin.rejection(401).contains("expired"), "invalid/expired feedback");
      require(MinecraftTokenLogin.rejection(404).contains("Java profile"), "missing Java profile feedback");
      require(MinecraftTokenLogin.rejection(429).contains("Too many"), "rate limit feedback");
      require(MinecraftTokenLogin.rejection(503).contains("503"), "service error feedback");

      AccountManager accounts = new AccountManager();
      accounts.addAccount("Example_1");
      accounts.saveToken("Example_1", "test-secret-not-a-real-token");
      require(accounts.isToken("Example_1") && !accounts.isMicrosoft("Example_1"), "direct token is distinct from refreshable Microsoft session");
      require(accounts.getToken("Example_1").equals("test-secret-not-a-real-token"), "allow switching within current session");
      JsonObject saved = accounts.toJson().getAsJsonObject();
      require(!saved.toString().contains("test-secret"), "never persist pasted bearer token");
      require(saved.has("tokenAccounts") && !saved.has("tokens"), "save account row without credential");
      AccountManager reloaded = new AccountManager();
      reloaded.fromJson(saved);
      require(reloaded.isToken("Example_1") && reloaded.getToken("Example_1") == null, "restart requires a fresh direct token");
      accounts.saveMicrosoftSession("Example_1", "refreshable-session");
      require(accounts.isMicrosoft("Example_1") && !accounts.isToken("Example_1") && accounts.getToken("Example_1") == null,
         "Microsoft login replaces direct token session");
      accounts.saveToken("Example_1", "new-test-secret");
      require(!accounts.isMicrosoft("Example_1") && accounts.getMicrosoftSession("Example_1") == null,
         "token login replaces stale Microsoft session type");
      saved.addProperty("unused", "value");
      JsonObject legacy = new JsonObject(); legacy.addProperty("Example_1", "legacy-test-secret"); saved.add("tokens", legacy);
      reloaded.fromJson(saved);
      require("legacy-test-secret".equals(reloaded.getToken("Example_1")), "load legacy saved token into memory for migration");
      require(!reloaded.toJson().toString().contains("legacy-test-secret"), "remove legacy plaintext on next save");
      reloaded.removeAccount("Example_1");
      require(reloaded.getToken("Example_1") == null && !reloaded.isToken("Example_1"), "remove account credential and type");
      System.out.println("Account login checks passed: " + checks + " (no live credentials used)");
   }

   private static void require(boolean condition, String message) {
      checks++;
      if (!condition) throw new AssertionError(message);
   }
}
