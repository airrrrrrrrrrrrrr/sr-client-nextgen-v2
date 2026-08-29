package me.alpha432.oyvey.features.modules.misc;

import java.util.HashSet;
import java.util.Set;
import me.alpha432.oyvey.features.commands.Command;
import me.alpha432.oyvey.features.modules.Module;
import net.minecraft.client.multiplayer.PlayerInfo;

public class StaffDetector extends Module {
   private static final Set<String> STAFF = new HashSet<>();
   private final Set<String> previouslyOnline = new HashSet<>();

   public StaffDetector() {
      super("StaffDetector", "Alerts when staff accounts are detected on the server.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      this.previouslyOnline.clear();
      if (mc.getConnection() != null) {
         for (PlayerInfo info : mc.getConnection().getListedOnlinePlayers()) {
            this.previouslyOnline.add(info.getProfile().name());
         }
      }
   }

   @Override
   public void onDisable() {
      this.previouslyOnline.clear();
   }

   @Override
   public void onTick() {
      if (!nullCheck() && mc.getConnection() != null) {
         Set<String> currentlyOnline = new HashSet<>();

         for (PlayerInfo info : mc.getConnection().getListedOnlinePlayers()) {
            currentlyOnline.add(info.getProfile().name());
         }

         for (String name : STAFF) {
            boolean wasOnline = this.previouslyOnline.contains(name);
            boolean isOnline = currentlyOnline.contains(name);
            if (!wasOnline && isOnline) {
               Command.sendMessage("staff online : " + name, "staffdetector");
            } else if (wasOnline && !isOnline) {
               Command.sendMessage("staff offline : " + name, "staffdetector");
            }
         }

         this.previouslyOnline.clear();
         this.previouslyOnline.addAll(currentlyOnline);
      }
   }

   static {
      STAFF.add("_MrDeathMatch_");
      STAFF.add("Busiest");
      STAFF.add("leoguidi44_");
      STAFF.add("Setwok");
      STAFF.add("Weeds4e");
      STAFF.add("Tokyo_idk");
      STAFF.add("dannymagnolia");
   }
}
