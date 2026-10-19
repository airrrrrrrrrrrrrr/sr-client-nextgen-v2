package me.alpha432.oyvey.features.modules.movement.scaffold;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/** Tracks native predictions for reconciliation without delaying dependent placements. */
public final class PlacementConfirmations {
   private final Map<BlockPos, Integer> pending = new HashMap<>();

   public void predict(BlockPos target, int sequence) {
      this.pending.merge(target.immutable(), sequence, Math::max);
   }

   public boolean isPending(BlockPos pos) {
      return this.pending.containsKey(pos);
   }

   public boolean hasPending() {
      return !this.pending.isEmpty();
   }

   public List<BlockPos> acknowledge(int sequence) {
      List<BlockPos> resolved = new ArrayList<>();
      this.pending.entrySet().removeIf(entry -> {
         if (entry.getValue() > sequence) return false;
         resolved.add(entry.getKey());
         return true;
      });
      return resolved;
   }

   /** A real server block update can confirm a legacy placement before a synthetic ack. */
   public void confirmBlock(BlockPos pos) {
      this.pending.remove(pos);
   }

   public void clear() {
      this.pending.clear();
   }
}
