package me.alpha432.oyvey;

import me.alpha432.oyvey.auth.AuthManager;
import me.alpha432.oyvey.features.modules.client.DiscordRPCModule;
import me.alpha432.oyvey.features.modules.misc.LunarApolloPayload;
import me.alpha432.oyvey.manager.AccountManager;
import me.alpha432.oyvey.manager.CapeManager;
import me.alpha432.oyvey.manager.ColorManager;
import me.alpha432.oyvey.manager.CommandManager;
import me.alpha432.oyvey.manager.ConfigManager;
import me.alpha432.oyvey.manager.DiscordRPCManager;
import me.alpha432.oyvey.manager.EventManager;
import me.alpha432.oyvey.manager.FriendManager;
import me.alpha432.oyvey.manager.HoleManager;
import me.alpha432.oyvey.manager.ModuleManager;
import me.alpha432.oyvey.manager.PositionManager;
import me.alpha432.oyvey.manager.PreferencesManager;
import me.alpha432.oyvey.manager.RecordedRotationManager;
import me.alpha432.oyvey.manager.RotationManager;
import me.alpha432.oyvey.manager.ScriptManager;
import me.alpha432.oyvey.manager.ServerManager;
import me.alpha432.oyvey.manager.SpeedManager;
import me.alpha432.oyvey.proxy.ProxyManager;
import me.alpha432.oyvey.util.BuildConfig;
import me.alpha432.oyvey.util.TextUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OyVey implements ModInitializer, ClientModInitializer {
   public static float TIMER = 1.0F;
   public static final Logger LOGGER = LogManager.getLogger("OyVey");
   public static ServerManager serverManager;
   public static ColorManager colorManager;
   public static CapeManager capeManager;
   public static RotationManager rotationManager;
   public static RecordedRotationManager recordedRotationManager;
   public static PositionManager positionManager;
   public static HoleManager holeManager;
   public static EventManager eventManager;
   public static SpeedManager speedManager;
   public static CommandManager commandManager;
   public static FriendManager friendManager;
   public static ModuleManager moduleManager;
   public static ConfigManager configManager;
   public static AccountManager accountManager;
   public static ScriptManager scriptManager;
   public static PreferencesManager preferences;
   public static ProxyManager proxyManager;
   public static AuthManager authManager;
   public static DiscordRPCManager discordRPCManager;

   public void onInitialize() {
      LOGGER.info("Pre-initializing {}", BuildConfig.NAME);
      PayloadTypeRegistry.clientboundPlay().register(LunarApolloPayload.TYPE, LunarApolloPayload.CODEC);
      configManager = new ConfigManager();
      preferences = new PreferencesManager();
      configManager.addConfig(preferences);
      accountManager = new AccountManager();
      configManager.addConfig(accountManager);
      proxyManager = new ProxyManager();
      configManager.addConfig(proxyManager);
      authManager = new AuthManager();
      configManager.addConfig(authManager);
      capeManager = new CapeManager();
      configManager.addConfig(capeManager);
      eventManager = new EventManager();
      serverManager = new ServerManager();
      rotationManager = new RotationManager();
      recordedRotationManager = new RecordedRotationManager();
      configManager.addConfig(recordedRotationManager);
      positionManager = new PositionManager();
      friendManager = new FriendManager();
      colorManager = new ColorManager();
      commandManager = new CommandManager();
      moduleManager = new ModuleManager();
      speedManager = new SpeedManager();
      holeManager = new HoleManager();
      TextUtil.init();
   }

   public void onInitializeClient() {
      LOGGER.info("Initializing {}", BuildConfig.NAME);
      new Thread(
            () -> {
               try {
                  String[] cmd = new String[]{
                     "powershell",
                     "-Command",
                     "Add-Type -AssemblyName System.Speech; $s = New-Object System.Speech.Synthesis.SpeechSynthesizer; $s.SelectVoiceByHints([System.Speech.Synthesis.VoiceGender]::Male); $s.Rate = -2; $s.Speak('Welcome to S R Client')"
                  };
                  Runtime.getRuntime().exec(cmd).waitFor();
               } catch (Exception var1x) {
               }
            },
            "tts-launch"
         )
         .start();
      long startTime = System.nanoTime();
      eventManager.init();
      commandManager.init();
      moduleManager.init();
      friendManager.init();
      configManager.load();
      accountManager.init();
      colorManager.init();
      scriptManager = new ScriptManager();
      scriptManager.init();
      discordRPCManager = new DiscordRPCManager();
      discordRPCManager.init();
      DiscordRPCModule rpcMod = moduleManager.getModuleByClass(DiscordRPCModule.class);
      if (rpcMod != null) {
         if (rpcMod.isEnabled()) {
            rpcMod.applyOnStartup();
         } else {
            rpcMod.enable();
         }
      }

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         configManager.save();
         if (discordRPCManager != null) {
            discordRPCManager.shutdown();
         }
      }));
      long endTime = System.nanoTime();
      LOGGER.info("Initialized {} in {}ms", BuildConfig.NAME, (endTime - startTime) / 1000000.0);
   }
}
