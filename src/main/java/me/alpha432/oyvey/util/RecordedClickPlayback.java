package me.alpha432.oyvey.util;

import java.util.List;

/** Preserves cumulative nanosecond deadlines; native ticks may drain nearby clicks together. */
public final class RecordedClickPlayback {
   private List<Long> pattern=List.of();
   private int index,target=-1;
   private long next;
   private boolean active;
   public void reset() { active=false; pattern=List.of(); index=0; target=-1; }
   public void prepare(List<Long> intervals, int targetId, boolean eligible, long now) {
      if(!eligible || targetId<0 || intervals.isEmpty()) { reset(); return; }
      if(!active || pattern!=intervals || target!=targetId) {
         pattern=intervals; target=targetId; index=0; next=now+50_000_000L; active=true;
      }
   }
   public boolean dueWithin(long now,long horizon) { return active && next-now<=horizon; }
   public int poll(long now) {
      if(!active || now<next) return 0;
      // A pause/menu/server stall must not dump old clicks into a new frame.
      if(now-next>250_000_000L) next=now;
      int due=0;
      while(now>=next && due<16) {
         due++;
         next+=pattern.get(index);
         index=(index+1)%pattern.size();
      }
      if(now>=next) next=now+Math.max(1,pattern.get(index));
      return due;
   }
}
