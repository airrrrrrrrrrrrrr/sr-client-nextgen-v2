package me.alpha432.oyvey.util.inventory.strategy;

import me.alpha432.oyvey.util.inventory.Result;

public interface SwapStrategy {
   boolean swap(Result var1);

   boolean swapBack(int var1, Result var2);
}
