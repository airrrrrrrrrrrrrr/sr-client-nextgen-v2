package me.alpha432.oyvey.util;

import java.util.Optional;
import java.util.regex.Pattern;

/** Parses server announcements with an explicit victim and killer, never ordinary player chat. */
public final class KillFeed {
   private static final String NAME = "([A-Za-z0-9_]{1,16})";
   private static final String PREFIX = "^(?:\\[[^\\]\\r\\n]{1,48}\\]\\s*)*";
   private static final String END = "(?:\\s+(?:using|with)\\s+.{1,160})?[.!]?\\s*$";
   private static final Pattern PASSIVE = Pattern.compile(PREFIX + NAME
      + "\\s+was\\s+(?:slain|killed|shot|eliminated)\\s+by\\s+" + NAME + END, Pattern.CASE_INSENSITIVE);
   private static final Pattern ACTIVE = Pattern.compile(PREFIX + NAME
      + "\\s+(?:killed|eliminated)\\s+" + NAME + END, Pattern.CASE_INSENSITIVE);
   private static final Pattern YOU = Pattern.compile(PREFIX + "You\\s+(?:killed|eliminated)\\s+" + NAME + "[.!]?\\s*$", Pattern.CASE_INSENSITIVE);

   private KillFeed() { }

   public static Optional<String> victim(String message, String localName) {
      if (message == null || localName == null || !localName.matches("[A-Za-z0-9_]{1,16}") || message.length() > 512) return Optional.empty();
      String plain = message.replaceAll("(?i)\u00a7[0-9a-fk-orx]", "").strip();
      if (plain.indexOf('\n') >= 0 || plain.indexOf('\r') >= 0) return Optional.empty();
      var passive = PASSIVE.matcher(plain);
      if (passive.matches()) return credited(passive.group(1), passive.group(2), localName);
      var you = YOU.matcher(plain);
      if (you.matches()) return credited(you.group(1), localName, localName);
      var active = ACTIVE.matcher(plain);
      if (active.matches()) return credited(active.group(2), active.group(1), localName);
      return Optional.empty();
   }

   private static Optional<String> credited(String victim, String killer, String localName) {
      return killer.equalsIgnoreCase(localName) && !victim.equalsIgnoreCase(localName) ? Optional.of(victim) : Optional.empty();
   }
}
