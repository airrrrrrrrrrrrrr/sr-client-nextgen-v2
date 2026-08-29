package me.alpha432.oyvey.features.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.awt.Color;
import me.alpha432.oyvey.features.commands.argument.ColorArgumentType;
import me.alpha432.oyvey.features.commands.argument.EnumArgumentType;
import me.alpha432.oyvey.features.commands.argument.NumberArgumentType;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.manager.CommandManager;

public class ModuleCommand extends Command {
   private final Module module;

   public ModuleCommand(Module module) {
      super(module.getName().toLowerCase());
      this.setDescription("Command line configuration implementation for \"" + module.getName() + "\"");
      this.module = module;
   }

   @Override
   public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
      for (Setting<?> setting : this.module.getSettings()) {
         Class<?> type = setting.getDefaultValue().getClass();
         if (Boolean.class.isAssignableFrom(type)) {
            this.registerBooleanArgument(builder, (Setting<Boolean>)setting);
         } else if (Number.class.isAssignableFrom(type)) {
            this.registerNumberArgument(builder, (Setting)setting);
         } else if (Enum.class.isAssignableFrom(type)) {
            this.registerEnumArgument(builder, (Setting<Enum<?>>)setting);
         } else if (String.class.isAssignableFrom(type)) {
            this.registerStringArgument(builder, (Setting<String>)setting);
         } else if (Color.class.isAssignableFrom(type)) {
            this.registerColorArgument(builder, (Setting<Color>)setting);
         }
      }
   }

   private void registerColorArgument(LiteralArgumentBuilder<CommandManager> builder, Setting<Color> setting) {
      builder.then(literal(setting.getName().toLowerCase()).then(argument("value", ColorArgumentType.color()).executes(ctx -> {
         setting.setValue(ColorArgumentType.getColor(ctx, "value"));
         Color value = setting.getValue();
         return this.success("Set %s.%s to RGB(%s, %s, %s)", this.module.getName(), setting.getName(), value.getRed(), value.getGreen(), value.getBlue());
      })));
   }

   private void registerStringArgument(LiteralArgumentBuilder<CommandManager> builder, Setting<String> setting) {
      builder.then(literal(setting.getName().toLowerCase()).then(argument("value", StringArgumentType.greedyString()).executes(ctx -> {
         setting.setValue(StringArgumentType.getString(ctx, "value"));
         return this.settingChangeReturn(setting);
      })));
   }

   private void registerEnumArgument(LiteralArgumentBuilder<CommandManager> builder, Setting<Enum<?>> setting) {
      Class<Enum<?>> type = (Class<Enum<?>>)setting.getDefaultValue().getClass();
      builder.then(literal(setting.getName().toLowerCase()).then(argument("value", EnumArgumentType._enum(type)).executes(ctx -> {
         setting.setValue(EnumArgumentType.getEnum(ctx, "value"));
         return this.settingChangeReturn(setting);
      })));
   }

   private <T extends Number> void registerNumberArgument(LiteralArgumentBuilder<CommandManager> builder, Setting<T> setting) {
      Class<T> type = (Class<T>)setting.getDefaultValue().getClass();
      builder.then(
         literal(setting.getName().toLowerCase())
            .then(argument("value", NumberArgumentType.number(type, NumberArgumentType.minMax(setting.getMin(), setting.getMax()))).executes(ctx -> {
               setting.setValue(NumberArgumentType.get(type, ctx, "value"));
               return this.settingChangeReturn(setting);
            }))
      );
   }

   private void registerBooleanArgument(LiteralArgumentBuilder<CommandManager> builder, Setting<Boolean> setting) {
      builder.then(literal(setting.getName().toLowerCase()).then(argument("value", BoolArgumentType.bool()).executes(ctx -> {
         setting.setValue(BoolArgumentType.getBool(ctx, "value"));
         return this.settingChangeReturn(setting);
      })));
   }

   private int settingChangeReturn(Setting<?> setting) {
      return this.success("Set %s.%s to %s", this.module.getName(), setting.getName(), setting.getValue());
   }

   @Override
   public boolean isShown() {
      return false;
   }
}
