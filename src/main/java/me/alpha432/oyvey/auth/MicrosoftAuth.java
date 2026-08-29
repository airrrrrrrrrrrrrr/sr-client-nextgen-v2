package me.alpha432.oyvey.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.UUID;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.StepMCProfile;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;

public final class MicrosoftAuth {
   private MicrosoftAuth() {
   }

   public static void deviceCodeLogin(MicrosoftAuth.CodeCallback onCode, MicrosoftAuth.Done onDone, MicrosoftAuth.Fail onFail) {
      Thread t = new Thread(
         () -> {
            try {
               HttpClient http = MinecraftAuth.createHttpClient();
               StepFullJavaSession.FullJavaSession session = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN
                  .getFromInput(
                     http,
                     new StepMsaDeviceCode.MsaDeviceCodeCallback(
                        code -> onCode.show(code.getUserCode(), code.getVerificationUri(), code.getDirectVerificationUri())
                     )
                  );
               StepMCProfile.MCProfile profile = session.getMcProfile();
               String sessionJson = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(session).toString();
               onDone.ok(profile.getName(), profile.getId(), profile.getMcToken().getAccessToken(), sessionJson);
            } catch (Throwable e) {
               onFail.error(e.getMessage() == null ? e.toString() : e.getMessage());
            }
         },
         "msa-login"
      );
      t.setDaemon(true);
      t.start();
   }

   public static void refreshAndLogin(String sessionJson, MicrosoftAuth.Done onDone, MicrosoftAuth.Fail onFail) {
      Thread t = new Thread(() -> {
         try {
            HttpClient http = MinecraftAuth.createHttpClient();
            JsonObject obj = JsonParser.parseString(sessionJson).getAsJsonObject();
            StepFullJavaSession.FullJavaSession session = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.fromJson(obj);
            session = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.refresh(http, session);
            StepMCProfile.MCProfile profile = session.getMcProfile();
            String newJson = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(session).toString();
            onDone.ok(profile.getName(), profile.getId(), profile.getMcToken().getAccessToken(), newJson);
         } catch (Throwable e) {
            onFail.error(e.getMessage() == null ? e.toString() : e.getMessage());
         }
      }, "msa-refresh");
      t.setDaemon(true);
      t.start();
   }

   public interface CodeCallback {
      void show(String var1, String var2, String var3);
   }

   public interface Done {
      void ok(String var1, UUID var2, String var3, String var4);
   }

   public interface Fail {
      void error(String var1);
   }
}
