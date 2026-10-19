package me.alpha432.oyvey.features.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.commands.Command;
import me.alpha432.oyvey.features.modules.client.RotationRecorderModule;
import me.alpha432.oyvey.manager.CommandManager;
import me.alpha432.oyvey.manager.RecordedRotationManager;

public class RotCommand extends Command {
   public RotCommand() {
      super("rot", "rotation");
      this.setDescription("Records your mouse rotation pattern for KillauraRecode");
   }

   @Override
   public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
      builder.then(literal("record").executes(ctx -> this.toggleRecording()));
      builder.then(literal("stop").executes(ctx -> this.stopRecording()));
      builder.then(literal("status").executes(ctx -> this.showStatus()));
      builder.then(literal("clear").executes(ctx -> {
         RotationRecorderModule recorder = OyVey.moduleManager.getModuleByClass(RotationRecorderModule.class);
         if (recorder != null && recorder.isEnabled()) {
            recorder.disable();
         }

         OyVey.recordedRotationManager.clearPattern();
         OyVey.configManager.save();
         return this.success("Cleared the recorded rotation pattern.");
      }));
   }

   private int toggleRecording() {
      if (mc.player == null || mc.level == null) {
         return this.fail("Join a world before recording rotations.");
      }

      RotationRecorderModule recorder = OyVey.moduleManager.getModuleByClass(RotationRecorderModule.class);
      if (recorder == null) {
         return this.fail("RotationRecorder module is unavailable.");
      }

      if (recorder.isEnabled()) {
         recorder.disable();
      } else {
         recorder.enable();
      }

      return -1;
   }

   private int stopRecording() {
      RecordedRotationManager rotations = OyVey.recordedRotationManager;
      RotationRecorderModule recorder = OyVey.moduleManager.getModuleByClass(RotationRecorderModule.class);
      if (recorder != null && recorder.isEnabled()) {
         recorder.disable();
         return -1;
      }

      if (!rotations.isRecording()) return this.fail("No recording is active. Type .rot record, then sneak to start.");

      int count = rotations.stopRecording();
      if (count >= 0) OyVey.configManager.save();
      return count < 0
         ? this.fail("No samples were captured. Your previous pattern was kept.")
         : this.success("Saved {white}%s{reset} samples, including zeros, to oyvey/recorded-rotations.json.", count);
   }

   private int showStatus() {
      RecordedRotationManager rotations = OyVey.recordedRotationManager;
      if (rotations.isArmed()) return this.success("Recorder armed. {white}Sneak{reset} to start, then press H again to save JSON.");
      if (rotations.isRecording()) {
         return this.success("Recording rotations now ({white}%s{reset} samples captured).", rotations.getPendingSize());
      }

      if (rotations.hasPattern()) {
         return this.success("A recorded rotation pattern is ready ({white}%s{reset} samples).", rotations.getPatternSize());
      }

      return this.success("No rotation pattern is saved. Type {white}.rot record{reset} to make one.");
   }
}
