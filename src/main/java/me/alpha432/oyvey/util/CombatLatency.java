package me.alpha432.oyvey.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.function.BiConsumer;

/** Combat-scoped latency replies, with one-shot delivery and connection ownership. */
public final class CombatLatency {
   private static final int CAPACITY = 128;
   private static final long COMBAT_MS = 1500;
   private final Deque<Reply> replies = new ArrayDeque<>();
   private Object owner;
   private long combatUntil;

   private static final class Reply {
      final Object owner;
      final Runnable send;
      boolean done;
      boolean ready;
      Reply(Object owner, Runnable send) { this.owner = owner; this.send = send; }
   }

   public synchronized void attack(Object connection, long now) {
      if (this.owner != connection) clear();
      this.owner = connection;
      this.combatUntil = now + COMBAT_MS;
   }

   public synchronized boolean active(Object connection, long now) {
      return connection != null && connection == this.owner && now < this.combatUntil;
   }

   public boolean delay(Object connection, long now, int milliseconds, Runnable send, BiConsumer<Runnable, Integer> schedule) {
      Reply reply, overflow;
      int delay = Math.clamp(milliseconds, 0, 400);
      synchronized (this) {
         if (delay == 0 || !active(connection, now)) return false;
         overflow = replies.size() >= CAPACITY ? replies.peekFirst() : null;
         reply = new Reply(connection, send);
         replies.addLast(reply);
      }
      if (overflow != null) release(overflow);
      try { schedule.accept(() -> release(reply), delay); }
      catch (RuntimeException unavailable) { release(reply); }
      return true;
   }

   private void release(Reply reply) {
      ArrayList<Reply> ready = new ArrayList<>();
      synchronized (this) {
         if (reply.done) return;
         reply.ready = true;
         // Lowering MS must not reorder protocol transaction replies.
         while (!replies.isEmpty() && replies.peekFirst().ready) {
            Reply next = replies.removeFirst();
            next.done = true;
            if (next.owner == this.owner) ready.add(next);
         }
      }
      for (Reply next : ready) next.send.run();
   }

   public void flush() {
      ArrayList<Reply> pending;
      synchronized (this) { combatUntil = 0; pending = new ArrayList<>(replies); }
      for (Reply reply : pending) release(reply);
   }

   public synchronized void clear() {
      for (Reply reply : replies) reply.done = true;
      replies.clear(); owner = null; combatUntil = 0;
   }

   public synchronized int size() { return replies.size(); }
}
