package me.alpha432.oyvey.features.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.alpha432.oyvey.features.commands.Command;
import me.alpha432.oyvey.features.commands.argument.ModuleArgumentType;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.manager.CommandManager;

public class DrawnCommand extends Command {
   public DrawnCommand() {
      super("drawn");
      this.setDescription("Sets a module to be drawn to the arraylist or not");
   }

   @Override
   public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
      builder.then(argument("module", ModuleArgumentType.module()).executes(ctx -> {
         Module module = ModuleArgumentType.getModule(ctx, "module");
         module.setDrawn(!module.isDrawn());
         boolean drawn = module.isDrawn();
         return this.success("{gray} %s {reset} is now %s %s", module.getDisplayName(), drawn ? "{green}" : "{red}", drawn ? "drawn" : "hidden");
      }));
   }
}
