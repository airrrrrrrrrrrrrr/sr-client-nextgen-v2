package me.alpha432.oyvey.features.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.input.KeyInputEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.commands.Command;
import me.alpha432.oyvey.features.commands.argument.ModuleArgumentType;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Bind;
import me.alpha432.oyvey.manager.CommandManager;
import me.alpha432.oyvey.util.KeyboardUtil;

public class BindCommand extends Command {
   private Module module;

   public BindCommand() {
      super("bind", "setbind");
      this.setDescription("Sets a key bind for a module");
      EVENT_BUS.register(this);
   }

   @Override
   public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
      builder.then(argument("module", ModuleArgumentType.module(true)).executes(ctx -> {
         this.module = ModuleArgumentType.getModule(ctx, "module");
         return this.success("Press any key...");
      }));
   }

   @Subscribe
   public void onKey(KeyInputEvent event) {
      if (this.module != null) {
         int key = event.getKey();
         if (key != -1 && key != 257 && key != 335) {
            if (key == 256) {
               this.module = null;
               sendMessage("Operation canceled.", "fail");
            } else {
               sendMessage("Bind for {green} %s {} set to {green} %s", this.module.getName(), KeyboardUtil.getKeyName(key));
               this.module.bind.setValue(new Bind(key));
               this.module = null;
               OyVey.configManager.save();
            }
         }
      }
   }
}
