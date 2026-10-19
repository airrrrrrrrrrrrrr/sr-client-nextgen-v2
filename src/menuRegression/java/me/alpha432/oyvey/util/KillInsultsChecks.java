package me.alpha432.oyvey.util;

import java.util.UUID;

public final class KillInsultsChecks {
   public static void main(String[] args) {
      var kills = new RecentKillTracker();
      UUID first = UUID.randomUUID(), other = UUID.randomUUID();
      kills.attack(7, first, "Steve", 1_000);
      require("Steve".equals(kills.death(7, first, 1_001)), "death packet before the next tick still credits the hit");
      require(kills.death(7, first, 1_002) == null, "death packet plus health polling emits once");
      require(kills.death(8, other, 1_002) == null, "unattacked deaths emit nothing");
      kills.attack(7, first, "Steve", 2_000);
      require(kills.death(7, other, 2_001) == null, "reused entity IDs cannot credit a different player");
      require("Steve".equals(kills.death(7, first, 2_002)), "an ID mismatch cannot consume the original hit");
      kills.attack(7, first, "Steve", 3_000);
      kills.attack(7, first, "Steve", 12_000);
      require("Steve".equals(kills.death(7, first, 21_999)), "continued attacks refresh the ten-second credit window");
      kills.attack(7, first, "Steve", 22_000);
      require(kills.death(7, first, 32_001) == null, "stale hits cannot claim later deaths");
      kills.attack(7, first, "Steve", 33_000);
      kills.clear();
      require(kills.death(7, first, 33_001) == null, "server changes and disabling discard previous hits");
      kills.attack(7, first, "Steve", 34_000);
      require(kills.death(7, first, 1) == null, "clock rollback cannot keep old hits alive");
      System.out.println("Kill Insults checks passed: 9");
   }

   private static void require(boolean value, String message) {
      if (!value) throw new AssertionError(message);
   }
}
