package me.alpha432.oyvey.features.modules.movement.scaffold;

import java.util.function.BooleanSupplier;

/** Drain suppressed use clicks so disabling Scaffold cannot replay them. */
public final class ScaffoldUseInput {
   private ScaffoldUseInput() { }

   public static boolean consumeClick(boolean blocked, BooleanSupplier original) {
      if (!blocked) return original.getAsBoolean();
      while (original.getAsBoolean()) { }
      return false;
   }
}
