package me.alpha432.oyvey.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import me.alpha432.oyvey.util.traits.Jsonable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RecordedRotationManager implements Jsonable {
   private static final int MAX_SAMPLES = 200000;
   private final List<RecordedRotationManager.RotationSample> pattern = new ArrayList<>();
   private final List<RecordedRotationManager.RotationSample> pending = new ArrayList<>();
   private boolean recording;
   private boolean armed;
   private Entity recordingTarget;
   private float minimumSpeed, maximumSpeed;

   public synchronized void armRecording(Entity target) {
      this.pending.clear();
      this.recordingTarget = target;
      this.recording = false;
      this.armed = true;
   }

   public synchronized boolean isArmed() { return this.armed; }

   public synchronized void cancelRecording() {
      this.pending.clear();
      this.recording = this.armed = false;
      this.recordingTarget = null;
   }

   public synchronized void startRecording() {
      this.startRecording(null);
   }

   public synchronized void startRecording(Entity target) {
      this.pending.clear();
      this.recordingTarget = target;
      this.recording = true;
      this.armed = false;
   }

   public synchronized int stopRecording() {
      if (!this.recording) {
         return -1;
      }

      this.recording = false;
      int result = this.commitPending();
      this.recordingTarget = null;
      return result;
   }

   public synchronized void captureMouseRotation(float deltaYaw, float deltaPitch) {
      if (!this.recording || this.pending.size() >= MAX_SAMPLES) {
         return;
      }

      if (!Float.isFinite(deltaYaw) || !Float.isFinite(deltaPitch)) return;
      deltaYaw = Mth.wrapDegrees(deltaYaw);
      deltaPitch = Mth.clamp(deltaPitch, -180.0F, 180.0F);
         float yawError = 0.0F;
         float pitchError = 0.0F;
         Minecraft mc = this.recordingTarget == null ? null : Minecraft.getInstance();
         if (this.recordingTarget != null && !this.recordingTarget.isRemoved() && mc != null && mc.player != null) {
            Vec3 eyes = mc.player.getEyePosition();
            Vec3 target = me.alpha432.oyvey.util.CombatAim.headCenter(this.recordingTarget.getBoundingBox(), this.recordingTarget.getEyeY());
            Vec3 delta = target.subtract(eyes);
            double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            float targetYaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
            float targetPitch = (float)(-Math.toDegrees(Math.atan2(delta.y, horizontal)));
            yawError = Mth.wrapDegrees(mc.player.getYRot() - targetYaw);
            pitchError = Mth.clamp(mc.player.getXRot() - targetPitch, -90.0F, 90.0F);
         }

         this.pending.add(new RecordedRotationManager.RotationSample(deltaYaw, deltaPitch, yawError, pitchError));
   }

   private int commitPending() {
      if (this.pending.isEmpty()) {
         this.pending.clear();
         return -1;
      }

      this.pattern.clear();
      this.pattern.addAll(this.pending);
      this.pending.clear();
      this.updateSpeedBounds();
      return this.pattern.size();
   }

   public synchronized boolean isRecording() {
      return this.recording;
   }

   public synchronized boolean hasPattern() {
      return !this.pattern.isEmpty();
   }

   public synchronized int getPatternSize() {
      return this.pattern.size();
   }

   public synchronized float getMinimumSpeed() {
      return this.minimumSpeed;
   }

   public synchronized float getMaximumSpeed() {
      return this.maximumSpeed;
   }

   private void updateSpeedBounds() {
      this.minimumSpeed = Float.POSITIVE_INFINITY;
      this.maximumSpeed = 0;
      for (RotationSample sample : this.pattern) {
         float speed = (float)Math.hypot(sample.yawDelta(), sample.pitchDelta());
         this.minimumSpeed = Math.min(this.minimumSpeed, speed);
         this.maximumSpeed = Math.max(this.maximumSpeed, speed);
      }
      if (this.pattern.isEmpty()) this.minimumSpeed = 0;
   }

   public synchronized boolean isFull() { return this.pending.size() >= MAX_SAMPLES; }

   public synchronized int getPendingSize() {
      return this.pending.size();
   }

   public synchronized RecordedRotationManager.RotationSample getSample(int index) {
      if (this.pattern.isEmpty()) {
         return RecordedRotationManager.RotationSample.ZERO;
      }

      if (index < this.pattern.size()) {
         return this.pattern.get(Math.max(0, index));
      }

      int tailStart = this.pattern.size() * 3 / 4;
      int tailSize = this.pattern.size() - tailStart;
      return this.pattern.get(tailStart + Math.floorMod(index - this.pattern.size(), tailSize));
   }

   public synchronized boolean isRecordingTarget(Entity entity) {
      return entity != null && entity == this.recordingTarget;
   }

   public synchronized void clearPattern() {
      this.pattern.clear();
      this.pending.clear();
      this.recording = false;
      this.armed = false;
      this.recordingTarget = null;
      this.minimumSpeed = this.maximumSpeed = 0;
   }

   @Override
   public synchronized JsonElement toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("formatVersion", 2);
      root.addProperty("sampling", "mouse-turn-including-idle");
      root.addProperty("sampleCount", this.pattern.size());
      JsonArray columns = new JsonArray();
      for (String name : new String[]{"yawDelta", "pitchDelta", "yawError", "pitchError"}) columns.add(name);
      root.add("columns", columns);
      JsonArray samples = new JsonArray();
      for (RecordedRotationManager.RotationSample sample : this.pattern) {
         JsonArray entry = new JsonArray();
         entry.add(sample.yawDelta());
         entry.add(sample.pitchDelta());
         entry.add(sample.yawError());
         entry.add(sample.pitchError());
         samples.add(entry);
      }

      root.add("samples", samples);
      return root;
   }

   @Override
   public synchronized void fromJson(JsonElement element) {
      this.pattern.clear();
      this.pending.clear();
      this.recording = false;
      this.armed = false;
      this.recordingTarget = null;
      this.minimumSpeed = this.maximumSpeed = 0;
      if (element == null || !element.isJsonObject()) {
         return;
      }

      JsonElement samplesElement = element.getAsJsonObject().get("samples");
      if (samplesElement == null || !samplesElement.isJsonArray()) {
         return;
      }

      for (JsonElement sampleElement : samplesElement.getAsJsonArray()) {
         if (this.pattern.size() >= MAX_SAMPLES) break;
         if (!sampleElement.isJsonArray()) continue;

         JsonArray entry = sampleElement.getAsJsonArray();
         if (entry.size() < 2) {
            continue;
         }

         try {
         float yawDelta = entry.get(0).getAsFloat();
         float pitchDelta = entry.get(1).getAsFloat();
         float yawError = entry.size() >= 3 ? entry.get(2).getAsFloat() : 0.0F;
         float pitchError = entry.size() >= 4 ? entry.get(3).getAsFloat() : 0.0F;
         if (Float.isFinite(yawDelta)
            && Float.isFinite(pitchDelta)
            && Float.isFinite(yawError)
            && Float.isFinite(pitchError)) {
            this.pattern.add(
               new RecordedRotationManager.RotationSample(
                  Mth.wrapDegrees(yawDelta),
                  Mth.clamp(pitchDelta, -180.0F, 180.0F),
                  Mth.clamp(yawError, -90.0F, 90.0F),
                  Mth.clamp(pitchError, -90.0F, 90.0F)
               )
            );
         }
         } catch (RuntimeException invalidSample) {
            // A malformed entry must not discard later valid samples.
         }
      }
      this.updateSpeedBounds();
   }

   @Override
   public String getFileName() {
      return "recorded-rotations.json";
   }

   public record RotationSample(float yawDelta, float pitchDelta, float yawError, float pitchError) {
      private static final RecordedRotationManager.RotationSample ZERO = new RecordedRotationManager.RotationSample(0.0F, 0.0F, 0.0F, 0.0F);
   }
}
