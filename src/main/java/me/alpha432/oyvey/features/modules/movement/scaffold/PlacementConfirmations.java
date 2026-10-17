package me.alpha432.oyvey.features.modules.movement.scaffold;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;

/** A locally predicted placement is not a support until vanilla reconciles its ack. */
public final class PlacementConfirmations {
   private final Map<BlockPos, Integer> pending = new HashMap<>();

   public void predict(BlockPos target, int sequence) {
      this.pending.merge(target.immutable(), sequence, Math::max);
   }

   public boolean isPending(BlockPos pos) {
      return this.pending.containsKey(pos);
   }

   public void acknowledge(int sequence) {
      this.pending.values().removeIf(value -> value <= sequence);
   }

   public void clear() {
      this.pending.clear();
   }
}
