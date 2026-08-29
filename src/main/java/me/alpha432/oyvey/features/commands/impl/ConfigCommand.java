package me.alpha432.oyvey.features.commands.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.commands.Command;
import me.alpha432.oyvey.manager.CommandManager;
import me.alpha432.oyvey.manager.ConfigManager;

public class ConfigCommand extends Command {
   private static final Path CONFIG_DIR = ConfigManager.getConfigsPath();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

   public ConfigCommand() {
      super("config", "cfg");
      this.setDescription("Save / load named module configs");
   }

   @Override
   public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
      builder.then(literal("save").then(argument("name", StringArgumentType.word()).executes(ctx -> {
         String name = StringArgumentType.getString(ctx, "name");
         if (!this.isValidName(name)) {
            return this.fail("Name may only contain letters, digits, _ or -");
         }

         try {
            Files.createDirectories(CONFIG_DIR);
            JsonElement json = OyVey.moduleManager.toJson();
            Files.writeString(CONFIG_DIR.resolve(name + ".json"), GSON.toJson(json));
            return this.success("Config {green}%s{reset} saved.", name);
         } catch (IOException var4) {
            OyVey.LOGGER.error("Failed to save config '{}'", name, var4);
            return this.fail("Failed to save config \u2014 check logs.");
         }
      })));
      builder.then(literal("load").then(argument("name", StringArgumentType.word()).suggests((ctx, suggestions) -> this.suggestConfigs(suggestions)).executes(ctx -> {
         String name = StringArgumentType.getString(ctx, "name");
         if (!this.isValidName(name)) {
            return this.fail("Name may only contain letters, digits, _ or -");
         }

         Path file = CONFIG_DIR.resolve(name + ".json");
         if (!Files.exists(file)) {
            return this.fail("Config {red}%s{reset} not found.", name);
         }

         try {
            String content = Files.readString(file);
            OyVey.moduleManager.fromJson(JsonParser.parseString(content));
            return this.success("Config {green}%s{reset} loaded.", name);
         } catch (IOException var5) {
            OyVey.LOGGER.error("Failed to load config '{}'", name, var5);
            return this.fail("Failed to load config \u2014 check logs.");
         }
      })));
      builder.then(
         literal("list")
            .executes(
               ctx -> {
                  if (!Files.exists(CONFIG_DIR)) {
                     return this.success("No configs saved yet.");
                  }

                  try (Stream<Path> paths = Files.list(CONFIG_DIR)) {
                     List<String> names = paths.filter(p -> p.toString().endsWith(".json"))
                        .map(p -> p.getFileName().toString().replace(".json", ""))
                        .sorted()
                        .collect(Collectors.toList());
                     return names.isEmpty()
                        ? this.success("No configs saved yet.")
                        : this.success("Configs: {green}%s", String.join("{reset}, {green}", names));
                  } catch (IOException var7) {
                     return this.fail("Could not read config directory.");
                  }
               }
            )
      );
      builder.then(literal("delete").then(argument("name", StringArgumentType.word()).suggests((ctx, suggestions) -> this.suggestConfigs(suggestions)).executes(ctx -> {
         String name = StringArgumentType.getString(ctx, "name");
         if (!this.isValidName(name)) {
            return this.fail("Name may only contain letters, digits, _ or -");
         }

         Path file = CONFIG_DIR.resolve(name + ".json");
         if (!Files.exists(file)) {
            return this.fail("Config {red}%s{reset} not found.", name);
         }

         try {
            Files.delete(file);
            return this.success("Config {green}%s{reset} deleted.", name);
         } catch (IOException var5) {
            return this.fail("Could not delete config.");
         }
      })));
      builder.then(literal("folder").executes(ctx -> {
         try {
            Files.createDirectories(CONFIG_DIR);
            if (!Desktop.isDesktopSupported()) {
               return this.fail("Opening folders is not supported on this system. Folder: %s", CONFIG_DIR.toAbsolutePath());
            }

            Desktop.getDesktop().open(CONFIG_DIR.toFile());
            return this.success("Opened config folder: {green}%s", CONFIG_DIR.toAbsolutePath());
         } catch (IOException exception) {
            OyVey.LOGGER.error("Failed to open config folder", exception);
            return this.fail("Could not open config folder: %s", CONFIG_DIR.toAbsolutePath());
         }
      }));
   }

   private boolean isValidName(String name) {
      return name != null && name.matches("[a-zA-Z0-9_\\-]+");
   }

   private CompletableFuture<Suggestions> suggestConfigs(SuggestionsBuilder builder) {
      String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
      if (Files.exists(CONFIG_DIR)) {
         try (Stream<Path> paths = Files.list(CONFIG_DIR)) {
            paths.filter(Files::isRegularFile)
               .map(path -> path.getFileName().toString())
               .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".json"))
               .map(name -> name.substring(0, name.length() - 5))
               .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(remaining))
               .sorted(String.CASE_INSENSITIVE_ORDER)
               .forEach(builder::suggest);
         } catch (IOException exception) {
            OyVey.LOGGER.debug("Could not list config suggestions", exception);
         }
      }

      return builder.buildFuture();
   }
}
