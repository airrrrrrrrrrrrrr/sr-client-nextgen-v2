import java.nio.file.*;

/** Installs native TrueType resources from the local Windows font library. */
public class GenerateHudFonts {
   public static void main(String[] args) throws Exception {
      String[][] fonts = {
         {"arial", "arial.ttf"}, {"verdana", "verdana.ttf"}, {"tahoma", "tahoma.ttf"},
         {"consolas", "consola.ttf"}, {"segoe", "segoeui.ttf"}, {"comic", "comic.ttf"},
         {"georgia", "georgia.ttf"}, {"trebuchet", "trebuc.ttf"}, {"courier", "cour.ttf"}
      };
      Path assets = Path.of(args.length == 0 ? "src/main/resources/assets/oyvey" : args[0]);
      Path definitions = Files.createDirectories(assets.resolve("font/hud"));
      Path windowsFonts = Path.of(System.getenv("SystemRoot"), "Fonts");
      for (String[] entry : fonts) {
         Files.copy(windowsFonts.resolve(entry[1]), definitions.resolve(entry[0] + ".ttf"), StandardCopyOption.REPLACE_EXISTING);
         String json = "{\"providers\":[{\"type\":\"ttf\",\"file\":\"oyvey:hud/" + entry[0] + ".ttf\","
            + "\"size\":11,\"oversample\":2,\"shift\":[0,0]},"
            + "{\"type\":\"reference\",\"id\":\"minecraft:default\"}]}";
         Files.writeString(definitions.resolve(entry[0] + ".json"), json);
         Path obsoleteAtlas = assets.resolve("textures/font/hud/" + entry[0] + ".png").toAbsolutePath().normalize();
         if (!obsoleteAtlas.startsWith(assets.toAbsolutePath().normalize())) {
            throw new IllegalStateException("Invalid generated atlas path");
         }
         Files.deleteIfExists(obsoleteAtlas);
         System.out.println("Installed native font " + entry[0]);
      }
   }
}
