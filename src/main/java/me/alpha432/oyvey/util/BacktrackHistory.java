package me.alpha432.oyvey.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Client-thread position history and deterministic FIFO movement replay. */
public final class BacktrackHistory<T> {
   public static final int CAPACITY = 128;
   public static final long HISTORY_MS = 1000;
   public record Sample(long time, Vec3 position, AABB box) { }
   private record Pending<T>(long time, T packet) { }
   private final Deque<Sample> samples = new ArrayDeque<>();
   private final Deque<Pending<T>> pending = new ArrayDeque<>();

   public void record(long now, Vec3 position, AABB box) {
      samples.addLast(new Sample(now, position, box));
      while (samples.size() > CAPACITY || samples.size() > 1 && samples.peekFirst().time() < now - HISTORY_MS)
         samples.removeFirst();
   }

   /** Returns an observed record, never an extrapolated future position. */
   public Sample atOrBefore(long time) {
      Sample selected = null;
      for (Sample sample : samples) {
         if (sample.time() > time) break;
         selected = sample;
      }
      return selected;
   }

   public void enqueue(long now, T packet, Consumer<T> overflow) {
      // Release the oldest first if a burst fills the buffer. Dropping a
      // relative movement packet would corrupt every following position.
      while (pending.size() >= CAPACITY) overflow.accept(pending.removeFirst().packet());
      pending.addLast(new Pending<>(now, packet));
   }

   public void release(long now, int delay, Consumer<T> consumer) {
      long cutoff = now - Math.max(0, Math.min(400, delay));
      while (!pending.isEmpty() && pending.peekFirst().time() <= cutoff) consumer.accept(pending.removeFirst().packet());
   }

   public void flush(Consumer<T> consumer) {
      while (!pending.isEmpty()) consumer.accept(pending.removeFirst().packet());
   }
   public int size() { return pending.size(); }
   public int age(long now) { return pending.isEmpty() ? 0 : (int)Math.min(400, Math.max(0, now - pending.peekFirst().time())); }
   public void clear() { pending.clear(); samples.clear(); }
}
