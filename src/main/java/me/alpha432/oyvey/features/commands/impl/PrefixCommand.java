package me.alpha432.oyvey.features.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.alpha432.oyvey.features.commands.Command;
import me.alpha432.oyvey.manager.CommandManager;

public class PrefixCommand extends Command {
   public PrefixCommand() {
      super("prefix", "setprefix");
      this.setDescription("Sets the command prefix");
   }

   @Override
   public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
      builder.then(argument("prefix", StringArgumentType.word()).executes(ctx -> {
         String prefix = StringArgumentType.getString(ctx, "prefix");
         if (prefix != null && !prefix.isEmpty()) {
            ((CommandManager)ctx.getSource()).setCommandPrefix(prefix);
            return this.success("Prefix changed to {green} %s", prefix);
         } else {
            return this.fail("Prefix must contain more than one character");
         }
      }));
   }
}
