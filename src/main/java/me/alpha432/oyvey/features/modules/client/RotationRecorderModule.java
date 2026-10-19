package me.alpha432.oyvey.features.modules.client;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.commands.Command;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Bind;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.PlayerSkin;
import org.lwjgl.glfw.GLFW;

public class RotationRecorderModule extends Module {
   private static final int DUMMY_ENTITY_ID = -4321432;
   private static final UUID STEVE_ID = new UUID(0, 15);
   private RemotePlayer practiceDummy;
   private ClientLevel dummyLevel;

   public RotationRecorderModule() {
      super("RotationRecorder", "Sneak to start recording all mouse samples against a client-only Steve; toggle off to save JSON.", Module.Category.CLIENT);
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
      OyVey.recordedRotationManager.armRecording(this.practiceDummy);
      Command.sendMessage(
         "{yellow} Steve spawned. {white}Sneak{yellow} to start recording, then aim naturally. Press {white}H{yellow} again to save JSON.",
         "rot-record"
      );
   }

   @Override
   public void onDisable() {
      if (!OyVey.recordedRotationManager.isRecording()) {
         OyVey.recordedRotationManager.cancelRecording();
         this.removePracticeDummy();
         return;
      }

      int count = OyVey.recordedRotationManager.stopRecording();
      this.removePracticeDummy();
      if (count < 0) {
         Command.sendMessage(
            "{yellow} No samples were captured. Your previous JSON recording was kept.",
            "rot-record"
         );
         return;
      }

      OyVey.configManager.save();
      Command.sendMessage(
         "{green} Saved {white}%s{green} samples, including zeros, to {white}oyvey/recorded-rotations.json{green}.",
         "rot-record",
         count
      );
   }

   @Override
   public void onTick() {
      if (mc.player == null || mc.level != this.dummyLevel || this.practiceDummy == null || this.practiceDummy.isRemoved()) {
         this.disable();
         return;
      }
      if (OyVey.recordedRotationManager.isArmed()) {
         if (mc.screen == null && mc.options.keyShift.isDown()) {
            OyVey.recordedRotationManager.startRecording(this.practiceDummy);
            Command.sendMessage("{green} Recording started. Zero movement is recorded too. Press {white}H{green} to save.", "rot-record");
         }
      } else if (!OyVey.recordedRotationManager.isRecording() || OyVey.recordedRotationManager.isFull()) this.disable();
   }

   @Override
   public String getDisplayInfo() {
      return OyVey.recordedRotationManager.isArmed() ? "Sneak to start"
         : OyVey.recordedRotationManager.isRecording() ? Integer.toString(OyVey.recordedRotationManager.getPendingSize()) : null;
   }

   @Override public void onUnload() {
      if (this.isEnabled()) this.disable();
      else this.removePracticeDummy();
   }

   private void spawnPracticeDummy() {
      this.removePracticeDummy();
      float spawnYaw = mc.player.getYRot() + 25.0F;
      double radians = Math.toRadians(spawnYaw);
      double x = mc.player.getX() - Math.sin(radians) * 3.0;
      double z = mc.player.getZ() + Math.cos(radians) * 3.0;
      this.dummyLevel = mc.level;
      this.practiceDummy = new RemotePlayer(mc.level, new GameProfile(STEVE_ID, "Steve")) {
         @Override public PlayerSkin getSkin() { return DefaultPlayerSkin.get(STEVE_ID); }
      };
      this.practiceDummy.setPos(x, mc.player.getY(), z);
      this.practiceDummy.setId(DUMMY_ENTITY_ID);
      this.practiceDummy.setYRot(spawnYaw + 180.0F);
      this.practiceDummy.yHeadRot = this.practiceDummy.yBodyRot = spawnYaw + 180.0F;
      this.practiceDummy.setOldPosAndRot();
      this.practiceDummy.setNoGravity(true);
      this.practiceDummy.setInvulnerable(true);
      this.practiceDummy.setCustomName(Component.literal("Steve"));
      this.practiceDummy.setCustomNameVisible(true);
      mc.level.addEntity(this.practiceDummy);
   }

   private void removePracticeDummy() {
      if (this.practiceDummy == null) {
         return;
      }

      if (this.dummyLevel != null && this.dummyLevel.getEntity(this.practiceDummy.getId()) == this.practiceDummy) {
         this.dummyLevel.removeEntity(this.practiceDummy.getId(), Entity.RemovalReason.DISCARDED);
      } else {
         this.practiceDummy.discard();
      }

      this.practiceDummy = null;
      this.dummyLevel = null;
   }
}
