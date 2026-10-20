package me.alpha432.oyvey.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

/** Vanilla swords remain recognizable when an old server supplies incomplete item tags. */
public final class CombatItems {
   private CombatItems() { }

   public static boolean isSword(ItemStack stack) {
      if (stack.isEmpty()) return false;
      if (stack.is(ItemTags.SWORDS)) return true;
      var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
      return vanillaSwordId(id.getNamespace(),id.getPath());
   }

   public static boolean vanillaSwordId(String namespace,String path) {
      return namespace.equals("minecraft") && switch(path) {
         case "wooden_sword", "stone_sword", "copper_sword", "iron_sword", "golden_sword", "diamond_sword", "netherite_sword" -> true;
         default -> false;
      };
   }
}
