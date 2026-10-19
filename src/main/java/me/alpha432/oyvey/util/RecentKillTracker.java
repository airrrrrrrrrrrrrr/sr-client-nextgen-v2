package me.alpha432.oyvey.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Recent hits are consumed by confirmed deaths, never by entity removal alone. */
public final class RecentKillTracker {
   private record Hit(UUID player, String name, long time) { }
   private final Map<Integer, Hit> hits = new HashMap<>();
   private static final long CREDIT_WINDOW_MS = 10_000;

   public void attack(int entityId, UUID player, String name, long now) {
      expire(now);
      hits.put(entityId, new Hit(player, name, now));
   }

   public String death(int entityId, UUID player, long now) {
      expire(now);
      Hit hit = hits.get(entityId);
      if (hit == null || !hit.player().equals(player)) return null;
      hits.remove(entityId);
      return hit.name();
   }

   public void expire(long now) {
      hits.values().removeIf(hit -> now - hit.time() > CREDIT_WINDOW_MS || now < hit.time());
   }

   public void clear() { hits.clear(); }
}
