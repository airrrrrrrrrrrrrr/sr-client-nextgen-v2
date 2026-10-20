package me.alpha432.oyvey.util;

/** Keeps a modern silent click/close sequence distinct from the user's actual inventory screen. */
public final class InventorySwapSession {
   private InventorySwapSession() { }

   public static boolean run(boolean spoof, boolean inventoryScreen, Runnable click, Runnable close) {
      if (!spoof && !inventoryScreen) return false;
      if (inventoryScreen) { click.run(); return true; }
      try { click.run(); }
      finally { close.run(); }
      return true;
   }

   public static int menuSlot(int inventorySlot) {
      if (inventorySlot < 0 || inventorySlot >= 36) throw new IllegalArgumentException("Not a player inventory slot");
      return inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
   }
}
