package me.alpha432.oyvey.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Client-thread FIFO: changing delay never lets a newer velocity overtake an older one. */
public final class KnockbackDelay<T> {
   private final ArrayDeque<Entry<T>> queue = new ArrayDeque<>();
   public static final class Entry<T> {
      public final T packet;
      public final long deadline;
      private final int receivedTick;
      private boolean damaged;
      private Entry(T packet,long deadline,int tick,boolean damaged) {
         this.packet=packet; this.deadline=deadline; this.receivedTick=tick; this.damaged=damaged;
      }
      public boolean damaged() { return damaged; }
   }
   public Entry<T> offer(T packet,long now,int tick,int milliseconds,boolean damaged) {
      int delay=Math.clamp(milliseconds,0,1000);
      if (queue.size()>=128 || delay==0 && queue.isEmpty()) return null;
      long deadline=now+delay*1_000_000L;
      if (!queue.isEmpty()) deadline=Math.max(deadline,queue.peekLast().deadline);
      Entry<T> entry=new Entry<>(packet,deadline,tick,damaged);
      queue.addLast(entry);
      return entry;
   }
   public void observeDamage(int tick) {
      for (Entry<T> entry:queue)
         if (tick>=entry.receivedTick && tick-entry.receivedTick<=2) entry.damaged=true;
   }
   public boolean contains(T packet) {
      for (Entry<T> entry:queue) if (entry.packet==packet) return true;
      return false;
   }
   public List<Entry<T>> drain(long now,boolean flush) {
      List<Entry<T>> ready=new ArrayList<>();
      while (!queue.isEmpty() && (flush || queue.peekFirst().deadline<=now)) ready.add(queue.removeFirst());
      return ready;
   }
   public int size() { return queue.size(); }
   public void clear() { queue.clear(); }
}
