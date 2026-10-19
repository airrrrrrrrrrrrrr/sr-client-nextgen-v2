package me.alpha432.oyvey.auth;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import me.alpha432.oyvey.manager.RecordedRotationManager;
import me.alpha432.oyvey.util.CombatAim;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** End-to-end data checks for arming, idle capture, JSON reload, and retargeting. */
final class RotationRecordingChecks {
   private static int checks;
   static void run() {
      var manager = new RecordedRotationManager();
      manager.armRecording(null);
      manager.captureMouseRotation(2, 1);
      require(manager.isArmed() && !manager.isRecording() && manager.getPendingSize() == 0, "arming does not record before sneak");
      manager.startRecording();
      for (int i = 0; i < 25; i++) manager.captureMouseRotation(0, 0);
      manager.captureMouseRotation(0.000001F, -0.000001F);
      manager.captureMouseRotation(2, 1);
      manager.captureMouseRotation(0, 0);
      manager.captureMouseRotation(Float.NaN, 0);
      manager.captureMouseRotation(0, Float.POSITIVE_INFINITY);
      require(manager.stopRecording() == 28, "idle, tiny movements and trailing idle are all retained");
      require(manager.getMinimumSpeed() == 0 && manager.getMaximumSpeed() > 2, "cache speed bounds including zero");
      try {
         Path output = Path.of("build/tmp/rotation-recorder-roundtrip.json");
         Files.createDirectories(output.getParent());
         Files.writeString(output, manager.toJson().toString());
         var loaded = new RecordedRotationManager();
         loaded.fromJson(JsonParser.parseString(Files.readString(output)));
         require(loaded.getPatternSize() == 28, "JSON disk round trip retains every sample");
         for (int i = 0; i < 28; i++) require(manager.getSample(i).equals(loaded.getSample(i)), "JSON preserves sample values and sequence");
      } catch (Exception failure) { throw new AssertionError("recorder JSON round trip", failure); }
      manager.startRecording(); manager.captureMouseRotation(0, 0);
      require(manager.stopRecording() == 1 && manager.hasPattern(), "one zero sample is a valid recording");
      manager.armRecording(null); manager.cancelRecording();
      require(!manager.isArmed() && !manager.isRecording() && manager.getPatternSize() == 1, "cancelling before sneak preserves previous recording");
      manager.fromJson(JsonParser.parseString("{\"samples\":[[0,0],null,[\"bad\",0],[1,2],[0,0,3,4]]}"));
      require(manager.getPatternSize() == 3 && manager.getSample(0).yawDelta() == 0, "legacy JSON and zeros survive malformed neighboring entries");
      require(DefaultPlayerSkin.get(new UUID(0, 15)).body().texturePath().toString().contains("wide/steve"), "practice player uses the actual wide Steve skin");

      for (double distance : new double[]{1.5, 3, 8, 30}) for (float direction : new float[]{0, 45, 90, 179, -179}) {
         Vec3 eyes = new Vec3(0, 1.62, 0);
         Vec3 center = Vec3.directionFromRotation(0, direction).scale(distance);
         AABB box = new AABB(center.x - .3, 0, center.z - .3, center.x + .3, 1.8, center.z + .3);
         for (float yawError : new float[]{-45, -5, 0, 5, 45}) for (float pitchError : new float[]{-30, 0, 30}) {
            float[] aim = CombatAim.recordedOffset(eyes, box, direction, 0, yawError, pitchError);
            require(box.clip(eyes, eyes.add(Vec3.directionFromRotation(aim[1], aim[0]).scale(distance + 3))).isPresent(),
               "large recorded errors stay inside the current player's hitbox");
         }
      }
      float[] ballistic = CombatAim.recordedOffset(new Vec3(0, 1.62, 0), new AABB(-.3, 0, 2.7, .3, 1.8, 3.3), 0, -40, 30, 30);
      require(ballistic[0] == 0 && ballistic[1] == -40, "recorded offsets never spoil an AutoRod ballistic aim");
      System.out.println("Rotation recorder/aim checks passed: " + checks);
   }
   private static void require(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
}
