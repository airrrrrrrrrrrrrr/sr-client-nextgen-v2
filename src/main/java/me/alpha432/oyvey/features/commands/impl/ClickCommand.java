package me.alpha432.oyvey.features.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.commands.Command;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.manager.CommandManager;
import me.alpha432.oyvey.manager.RecordedClickManager;

public final class ClickCommand extends Command {
   public ClickCommand() { super("click"); setDescription("Record and select named Killaura click timing patterns"); }
   @Override public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
      builder.executes(ctx->success("Use .click record start, sneak and click, then .click record stop <name>."));
      builder.then(literal("record")
         .then(literal("start").executes(ctx->{
            if(mc.player==null || mc.level==null) return fail("Join a world before recording clicks.");
            if(!OyVey.recordedClickManager.start()) return fail("Already recording. Use .click record stop <name> or .click record cancel.");
            return success("Click recording armed. Hold Sneak and left-click. Save with .click record stop <name>.");
         }))
         .then(literal("stop").then(argument("name",StringArgumentType.word()).executes(ctx->{
            var manager=OyVey.recordedClickManager;
            if(!manager.isRecording()) return fail("Start with .click record start first.");
            String name;
            try { name=RecordedClickManager.normalizeName(StringArgumentType.getString(ctx,"name")); }
            catch(IllegalArgumentException e) { return fail(e.getMessage()); }
            int count=manager.stop(name);
            if(count<0) return fail("Record at least two clicks while holding Sneak before saving. Recording is still active.");
            selectRecorded(); OyVey.configManager.save();
            return success("Saved {white}%s{reset} clicks as {white}%s{reset} in oyvey/recorded-clicks.json. ClickMode is Recorded.",count,name);
         })))
         .then(literal("cancel").executes(ctx->{ OyVey.recordedClickManager.cancel(); return success("Cancelled click recording; saved patterns were kept."); })));
      builder.then(literal("use").then(argument("name",StringArgumentType.word()).suggests((ctx,b)->{
         OyVey.recordedClickManager.names().forEach(b::suggest); return b.buildFuture();
      }).executes(ctx->{
         try {
            if(!OyVey.recordedClickManager.use(StringArgumentType.getString(ctx,"name"))) return fail("No pattern with that name. Use .click list.");
         } catch(IllegalArgumentException e) { return fail(e.getMessage()); }
         selectRecorded(); OyVey.configManager.save();
         return success("Recorded clicks selected: {white}%s{reset}.",OyVey.recordedClickManager.selectedName());
      })));
      builder.then(literal("list").executes(ctx->success("Saved click patterns: {white}%s{reset}.",
         OyVey.recordedClickManager.names().isEmpty()?"none":String.join(", ",OyVey.recordedClickManager.names()))));
      builder.then(literal("status").executes(ctx->{
         var manager=OyVey.recordedClickManager;
         return success(manager.isRecording()?"Recording: {white}%s{reset} intervals captured."
            :"Selected click pattern: {white}%s{reset}.",manager.isRecording()?manager.capturedIntervals():manager.selectedName());
      }));
   }
   private void selectRecorded() {
      var aura=OyVey.moduleManager.getModuleByClass(KillauraRecode.class);
      if(aura!=null) aura.clickMode.setValue(KillauraRecode.ClickMode.RECORDED);
   }
}
