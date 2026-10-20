package me.alpha432.oyvey.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;

/** Combines recent-hit deaths with explicit server kill announcements; removal alone is not a kill. */
public final class RecentKillTracker {
   private record Hit(UUID player, String name, long time) { }
   private final Map<Integer, Hit> hits = new HashMap<>();
   private final Map<String, Long> announced = new HashMap<>();
   private static final long CREDIT_WINDOW_MS = 10_000;
   private static final long DUPLICATE_WINDOW_MS = 2_500;

   public void attack(int entityId, UUID player, String name, long now) {
      expire(now);
      hits.put(entityId, new Hit(player, name, now));
   }

   public String death(int entityId, UUID player, long now) {
      expire(now);
      Hit hit = hits.get(entityId);
      if (hit == null || !hit.player().equals(player)) return null;
      hits.remove(entityId);
      return claim(hit.name(), now) ? hit.name() : null;
   }

   /** The server explicitly named us as the killer; the victim may already be despawned. */
   public String serverKill(String victim, long now) {
      expire(now);
      if (victim == null || !victim.matches("[A-Za-z0-9_]{1,16}")) return null;
      hits.values().removeIf(hit -> hit.name().equalsIgnoreCase(victim));
      return claim(victim, now) ? victim : null;
   }

   private boolean claim(String name, long now) {
      String key = name.toLowerCase(Locale.ROOT);
      Long previous = announced.putIfAbsent(key, now);
      return previous == null;
   }

   public void expire(long now) {
      hits.values().removeIf(hit -> now - hit.time() > CREDIT_WINDOW_MS || now < hit.time());
      announced.values().removeIf(time -> now - time > DUPLICATE_WINDOW_MS || now < time);
   }

   public void clear() { hits.clear(); announced.clear(); }
}
