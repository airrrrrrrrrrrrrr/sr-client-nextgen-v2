package me.alpha432.oyvey.features.modules.movement.scaffold;

import java.util.random.RandomGenerator;
import java.util.concurrent.ThreadLocalRandom;

/** Fixed or randomized placement deadlines, bounded to the native 40 CPS budget. */
public final class PlacementClickRate {
   private final RandomGenerator random;
   private int configured=-1, tick=Integer.MIN_VALUE, used;
   private long next;
   private int low,high;
   public PlacementClickRate() { this(ThreadLocalRandom.current()); }
   public PlacementClickRate(RandomGenerator random) { this.random=random; }
   public void reset() { configured=-1; tick=Integer.MIN_VALUE; used=0; }
   private void configure(long now,int cps,boolean randomized,int min,int max) {
      cps=Math.clamp(cps,1,40); min=Math.clamp(min,1,40); max=Math.clamp(max,1,40);
      int key=cps|(min<<6)|(max<<12)|(randomized?1<<18:0);
      if(configured!=key) {
         configured=key; next=now; used=0; tick=Integer.MIN_VALUE;
      }
      low=randomized?Math.min(min,max):cps;
      high=randomized?Math.max(min,max):cps;
      if(now-next>100_000_000L) next=now;
   }
   public boolean readyWithin(long now,long horizon,int cps,boolean randomized,int min,int max) {
      configure(now,cps,randomized,min,max);
      return next-now<=horizon;
   }
   public boolean tryAcquire(long now,int currentTick,int cps,boolean randomized,int min,int max) {
      configure(now,cps,randomized,min,max);
      if(currentTick!=tick) { tick=currentTick; used=0; }
      if(used>=2 || now<next) return false;
      int rate=low==high?low:random.nextInt(low,high+1);
      next+=1_000_000_000L/rate;
      used++;
      return true;
   }
}
