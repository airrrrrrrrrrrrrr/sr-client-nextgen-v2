package me.alpha432.oyvey.util;

import java.util.UUID;

public final class KillInsultsChecks {
   private static int checks;
   public static void main(String[] args) {
      var kills = new RecentKillTracker();
      UUID first = UUID.randomUUID(), other = UUID.randomUUID();
      kills.attack(7, first, "Steve", 1_000);
      require("Steve".equals(kills.death(7, first, 1_001)), "death packet before the next tick still credits the hit");
      require(kills.death(7, first, 1_002) == null, "death packet plus health polling emits once");
      require(kills.death(8, other, 1_002) == null, "unattacked deaths emit nothing");
      kills.clear();
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
      require(KillFeed.victim("THENERD10 was slain by arsoncauser", "arsoncauser").orElseThrow().equals("THENERD10"),
         "parse the reported PvPLegacy death announcement");
      require(KillFeed.victim("\u00a7cTHENERD10\u00a77 was slain by \u00a7barsoncauser", "arsoncauser").isPresent(), "color formatting does not hide the kill");
      require(KillFeed.victim("[Game] THENERD10 was slain by arsoncauser!", "arsoncauser").isPresent(), "server prefix and punctuation are accepted");
      require(KillFeed.victim("THENERD10 was shot by ARSONCAUSER using a bow", "arsoncauser").isPresent(), "ranged kills and case variations work");
      require(KillFeed.victim("arsoncauser killed THENERD10.", "arsoncauser").isPresent(), "active kill announcements work");
      require(KillFeed.victim("You killed THENERD10!", "arsoncauser").isPresent(), "personal server kill announcements work");
      require(KillFeed.victim("THENERD10 was slain by someoneelse", "arsoncauser").isEmpty(), "other players' kills do not taunt");
      require(KillFeed.victim("<someoneelse> THENERD10 was slain by arsoncauser", "arsoncauser").isEmpty(), "ordinary chat cannot impersonate a server feed");
      require(KillFeed.victim("[VIP] someoneelse: THENERD10 was slain by arsoncauser", "arsoncauser").isEmpty(), "ranked player chat cannot impersonate a server feed");
      require(KillFeed.victim("arsoncauser has won the game!", "arsoncauser").isEmpty(), "victory messages do not add duplicate kill credit");
      require(KillFeed.victim("arsoncauser had 20.0 health left.", "arsoncauser").isEmpty(), "health summaries are not kills");
      require(KillFeed.victim("arsoncauser was slain by arsoncauser", "arsoncauser").isEmpty(), "never announce our own death");
      kills.clear();
      require("THENERD10".equals(kills.serverKill("THENERD10", 40_000)), "explicit server credit works even without a corpse or a saved attack");
      require(kills.serverKill("thenerd10", 40_001) == null, "repeated feed lines are deduplicated case-insensitively");
      require("THENERD10".equals(kills.serverKill("THENERD10", 43_000)), "a later round can credit the same opponent again");
      kills.clear();
      kills.attack(7, first, "Steve", 50_000);
      require("Steve".equals(kills.death(7, first, 50_001)), "death packet can announce first");
      require(kills.serverKill("Steve", 50_002) == null, "the later kill feed cannot double announce");
      kills.clear();
      kills.attack(7, first, "Steve", 60_000);
      require("Steve".equals(kills.serverKill("Steve", 60_001)), "kill feed can announce before entity death");
      require(kills.death(7, first, 60_002) == null, "the later entity death cannot double announce");
      System.out.println("Kill Insults checks passed: " + checks);
   }

   private static void require(boolean value, String message) {
      checks++;
      if (!value) throw new AssertionError(message);
   }
}
