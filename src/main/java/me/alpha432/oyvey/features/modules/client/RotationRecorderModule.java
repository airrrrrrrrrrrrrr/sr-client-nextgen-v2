package me.alpha432.oyvey.features.modules.client;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.commands.Command;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Bind;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class RotationRecorderModule extends Module {
   private static final int DUMMY_ENTITY_ID = -4321432;
   private ArmorStand practiceDummy;

   public RotationRecorderModule() {
      super("RotationRecorder", "Records every mouse rotation and jitter for KillauraRecode Recorded mode.", Module.Category.CLIENT);
      this.bind.setValueNoEvent(new Bind(GLFW.GLFW_KEY_H));
   }

   @Override
   public void onEnable() {
      if (mc.player == null || mc.level == null) {
         Command.sendMessage("{red} Join a world before starting RotationRecorder.", "rot-record");
         this.disable();
         return;
      }

      this.spawnPracticeDummy();
      OyVey.recordedRotationManager.startRecording(this.practiceDummy);
      Command.sendMessage(
         "{yellow} Client-only practice dummy spawned. Aim at and attack it naturally, then press {white}H{yellow} again to save.",
         "rot-record"
      );
   }

   @Override
   public void onDisable() {
      if (!OyVey.recordedRotationManager.isRecording()) {
         this.removePracticeDummy();
         return;
      }

      int count = OyVey.recordedRotationManager.stopRecording();
      this.removePracticeDummy();
      if (count < 0) {
         Command.sendMessage(
            "{red} Recording had too little movement. Your previous pattern was kept; press H and record a longer mouse movement.",
            "rot-record"
         );
         return;
      }

      OyVey.configManager.save();
      Command.sendMessage(
         "{green} Saved {white}%s{green} non-zero rotation and jitter samples for KillauraRecode.",
         "rot-record",
         count
      );
   }

   @Override
   public void onTick() {
      if (!OyVey.recordedRotationManager.isRecording()) {
         this.disable();
      }
   }

   @Override
   public String getDisplayInfo() {
      return OyVey.recordedRotationManager.isRecording() ? Integer.toString(OyVey.recordedRotationManager.getPendingSize()) : null;
   }

   private void spawnPracticeDummy() {
      this.removePracticeDummy();
      float spawnYaw = mc.player.getYRot() + 25.0F;
      double radians = Math.toRadians(spawnYaw);
      double x = mc.player.getX() - Math.sin(radians) * 3.0;
      double z = mc.player.getZ() + Math.cos(radians) * 3.0;
      this.practiceDummy = new ArmorStand(mc.level, x, mc.player.getY(), z);
      this.practiceDummy.setId(DUMMY_ENTITY_ID);
      this.practiceDummy.setYRot(spawnYaw + 180.0F);
      this.practiceDummy.setNoGravity(true);
      this.practiceDummy.setInvulnerable(true);
      this.practiceDummy.setCustomName(Component.literal("Rotation Recorder Dummy"));
      this.practiceDummy.setCustomNameVisible(true);
      mc.level.addEntity(this.practiceDummy);
   }

   private void removePracticeDummy() {
      if (this.practiceDummy == null) {
         return;
      }

      if (mc.level != null && mc.level.getEntity(this.practiceDummy.getId()) == this.practiceDummy) {
         mc.level.removeEntity(this.practiceDummy.getId(), Entity.RemovalReason.DISCARDED);
      } else {
         this.practiceDummy.discard();
      }

      this.practiceDummy = null;
   }
}
