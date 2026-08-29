package me.alpha432.oyvey.features.commands.impl;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.alpha432.oyvey.features.commands.Command;
import me.alpha432.oyvey.manager.CommandManager;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;

public class VclipCommand extends Command {
   public VclipCommand() {
      super("vclip");
      this.setDescription("Clip through blocks vertically (.vclip <blocks>)");
   }

   @Override
   public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
      builder.then(argument("blocks", FloatArgumentType.floatArg()).executes(ctx -> {
         if (mc.player == null) {
            return this.fail("Not in a world");
         }

         float blocks = FloatArgumentType.getFloat(ctx, "blocks");
         double newY = mc.player.getY() - blocks;
         mc.player.connection.send(new Pos(mc.player.getX(), newY, mc.player.getZ(), false, mc.player.horizontalCollision));
         mc.player.setPos(mc.player.getX(), newY, mc.player.getZ());
         sendMessage(String.format("{green} Clipped {white}%.1f {green}blocks %s", Math.abs(blocks), blocks >= 0.0F ? "down" : "up"), "general");
         return 1;
      }));
   }
}
