package me.alpha432.oyvey.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.event.impl.render.Render3DEvent;
import me.alpha432.oyvey.features.Feature;
import me.alpha432.oyvey.features.commands.ModuleCommand;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.ClickGuiModule;
import me.alpha432.oyvey.features.modules.client.DiscordRPCModule;
import me.alpha432.oyvey.features.modules.client.HudEditorModule;
import me.alpha432.oyvey.features.modules.client.NotificationsModule;
import me.alpha432.oyvey.features.modules.client.RotationRecorderModule;
import me.alpha432.oyvey.features.modules.client.SrMusicModule;
import me.alpha432.oyvey.features.modules.combat.BacktrackModule;
import me.alpha432.oyvey.features.modules.combat.BedNuker;
import me.alpha432.oyvey.features.modules.combat.CriticalsModule;
import me.alpha432.oyvey.features.modules.combat.FollowPearl;
import me.alpha432.oyvey.features.modules.combat.GrimAura;
import me.alpha432.oyvey.features.modules.combat.HitboxModule;
import me.alpha432.oyvey.features.modules.combat.KeyPearlModule;
import me.alpha432.oyvey.features.modules.combat.KillAura;
import me.alpha432.oyvey.features.modules.combat.KillauraNew;
import me.alpha432.oyvey.features.modules.combat.KillauraRecode;
import me.alpha432.oyvey.features.modules.combat.MaceDmgHack;
import me.alpha432.oyvey.features.modules.combat.PvpBot;
import me.alpha432.oyvey.features.modules.combat.ReachModule;
import me.alpha432.oyvey.features.modules.combat.TeleportAura;
import me.alpha432.oyvey.features.modules.combat.TestAura;
import me.alpha432.oyvey.features.modules.combat.WTap;
import me.alpha432.oyvey.features.modules.ghost.BannabellPepper;
import me.alpha432.oyvey.features.modules.hud.CoordinatesHudModule;
import me.alpha432.oyvey.features.modules.hud.WatermarkHudModule;
import me.alpha432.oyvey.features.modules.misc.BeatMinecraft;
import me.alpha432.oyvey.features.modules.misc.ClientSpoof;
import me.alpha432.oyvey.features.modules.misc.Disabler;
import me.alpha432.oyvey.features.modules.misc.KillInsults;
import me.alpha432.oyvey.features.modules.misc.MCFModule;
import me.alpha432.oyvey.features.modules.misc.ResourcePackSpoof;
import me.alpha432.oyvey.features.modules.misc.SkyWarsAutoPlay;
import me.alpha432.oyvey.features.modules.misc.StaffDetector;
import me.alpha432.oyvey.features.modules.movement.AutoGapple;
import me.alpha432.oyvey.features.modules.movement.FastFall;
import me.alpha432.oyvey.features.modules.movement.Fly;
import me.alpha432.oyvey.features.modules.movement.InventoryMove;
import me.alpha432.oyvey.features.modules.movement.Jesus;
import me.alpha432.oyvey.features.modules.movement.LegitScaffold;
import me.alpha432.oyvey.features.modules.movement.NoSlow;
import me.alpha432.oyvey.features.modules.movement.NoWebModule;
import me.alpha432.oyvey.features.modules.movement.ReverseStepModule;
import me.alpha432.oyvey.features.modules.movement.SafeWalk;
import me.alpha432.oyvey.features.modules.movement.Scaffold;
import me.alpha432.oyvey.features.modules.movement.Speed;
import me.alpha432.oyvey.features.modules.movement.SpeedOld;
import me.alpha432.oyvey.features.modules.movement.SprintModule;
import me.alpha432.oyvey.features.modules.movement.StepModule;
import me.alpha432.oyvey.features.modules.movement.TargetStrafe;
import me.alpha432.oyvey.features.modules.movement.TimerModule;
import me.alpha432.oyvey.features.modules.movement.Vulcan;
import me.alpha432.oyvey.features.modules.movement.WindChargeBoost;
import me.alpha432.oyvey.features.modules.player.AimAssistModule;
import me.alpha432.oyvey.features.modules.player.AntiVoid;
import me.alpha432.oyvey.features.modules.player.AutoClickerModule;
import me.alpha432.oyvey.features.modules.player.AutoEat;
import me.alpha432.oyvey.features.modules.player.AutoExtinguish;
import me.alpha432.oyvey.features.modules.player.AutoPot;
import me.alpha432.oyvey.features.modules.player.AutoSoupModule;
import me.alpha432.oyvey.features.modules.player.AutoTotem;
import me.alpha432.oyvey.features.modules.player.ChestStealer;
import me.alpha432.oyvey.features.modules.player.FakeLagModule;
import me.alpha432.oyvey.features.modules.player.FastPlaceModule;
import me.alpha432.oyvey.features.modules.player.FastUse;
import me.alpha432.oyvey.features.modules.player.InvManager;
import me.alpha432.oyvey.features.modules.player.NoFallModule;
import me.alpha432.oyvey.features.modules.player.RegenModule;
import me.alpha432.oyvey.features.modules.player.VelocityModule;
import me.alpha432.oyvey.features.modules.render.Ambience;
import me.alpha432.oyvey.features.modules.render.AnimationsModule;
import me.alpha432.oyvey.features.modules.render.BlockHighlightModule;
import me.alpha432.oyvey.features.modules.render.Chams;
import me.alpha432.oyvey.features.modules.render.ChestESP;
import me.alpha432.oyvey.features.modules.render.FpsCounterModule;
import me.alpha432.oyvey.features.modules.render.FreeLookModule;
import me.alpha432.oyvey.features.modules.render.GlowESP;
import me.alpha432.oyvey.features.modules.render.MinimapModule;
import me.alpha432.oyvey.features.modules.render.MusicPlayerModule;
import me.alpha432.oyvey.features.modules.render.PlayerESP;
import me.alpha432.oyvey.features.modules.render.RotationVisualizerModule;
import me.alpha432.oyvey.features.modules.render.TargetESP;
import me.alpha432.oyvey.features.modules.render.TargetHUD;
import me.alpha432.oyvey.features.modules.render.TargetHUDv2;
import me.alpha432.oyvey.util.traits.Jsonable;
import me.alpha432.oyvey.util.traits.Toggleable;
import me.alpha432.oyvey.util.traits.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleManager implements Jsonable, Util {
   private static final Logger LOGGER = LoggerFactory.getLogger("ModuleManager");
   private final Map<Class<? extends Module>, Module> fastRegistry = new HashMap<>();
   private final List<Module> modules = new ArrayList<>();

   public void init() {
      this.register(new SrMusicModule());
      this.register(new WatermarkHudModule());
      this.register(new CoordinatesHudModule());
      this.register(new HudEditorModule());
      this.register(new ClickGuiModule());
      this.register(new NotificationsModule());
      this.register(new RotationRecorderModule());
      this.register(new DiscordRPCModule());
      this.register(new CriticalsModule());
      this.register(new KeyPearlModule());
      this.register(new FollowPearl());
      this.register(new KillAura());
      this.register(new KillauraRecode());
      this.register(new KillauraNew());
      this.register(new AutoGapple());
      this.register(new ReachModule());
      this.register(new HitboxModule());
      this.register(new BacktrackModule());
      this.register(new WTap());
      this.register(new GrimAura());
      this.register(new TeleportAura());
      this.register(new BedNuker());
      this.register(new MaceDmgHack());
      this.register(new TestAura());
      this.register(new PvpBot());
      this.register(new Fly());
      this.register(new FastFall());
      this.register(new StepModule());
      this.register(new ReverseStepModule());
      this.register(new Scaffold());
      this.register(new LegitScaffold());
      this.register(new Vulcan());
      this.register(new Speed());
      this.register(new SpeedOld());
      this.register(new TargetStrafe());
      this.register(new TimerModule());
      this.register(new NoSlow());
      this.register(new Jesus());
      this.register(new SafeWalk());
      this.register(new InventoryMove());
      this.register(new NoWebModule());
      this.register(new SprintModule());
      this.register(new WindChargeBoost());
      this.register(new AutoExtinguish());
      this.register(new AutoTotem());
      this.register(new AutoPot());
      this.register(new AutoEat());
      this.register(new AutoSoupModule());
      this.register(new RegenModule());
      this.register(new FastPlaceModule());
      this.register(new VelocityModule());
      this.register(new NoFallModule());
      this.register(new AntiVoid());
      this.register(new ChestStealer());
      this.register(new InvManager());
      this.register(new BeatMinecraft());
      this.register(new MCFModule());
      this.register(new KillInsults());
      this.register(new Disabler());
      this.register(new StaffDetector());
      this.register(new ResourcePackSpoof());
      this.register(new ClientSpoof());
      this.register(new SkyWarsAutoPlay());
      this.register(new FastUse());
      this.register(new FakeLagModule());
      this.register(new AutoClickerModule());
      this.register(new AimAssistModule());
      this.register(new FpsCounterModule());
      this.register(new MinimapModule());
      this.register(new MusicPlayerModule());
      this.register(new TargetESP());
      this.register(new BlockHighlightModule());
      this.register(new ChestESP());
      this.register(new GlowESP());
      this.register(new me.alpha432.oyvey.features.modules.render.ArrayList());
      this.register(new TargetHUD());
      this.register(new TargetHUDv2());
      this.register(new Chams());
      this.register(new PlayerESP());
      this.register(new AnimationsModule());
      this.register(new FreeLookModule());
      this.register(new RotationVisualizerModule());
      this.register(new Ambience());
      this.register(new BannabellPepper());
      LOGGER.info("Registered {} modules", this.modules.size());

      for (Module module : this.modules) {
         OyVey.commandManager.register(new ModuleCommand(module));
      }

      OyVey.configManager.addConfig(this);
   }

   public void register(Module module) {
      this.getModules().add(module);
      this.fastRegistry.put((Class<? extends Module>)module.getClass(), module);
   }

   public List<Module> getModules() {
      return this.modules;
   }

   public Stream<Module> stream() {
      return this.getModules().stream();
   }

   public <T extends Module> T getModuleByClass(Class<T> clazz) {
      return (T)this.fastRegistry.get(clazz);
   }

   public Module getModuleByName(String name) {
      return this.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   public Module getModuleByDisplayName(String display) {
      return this.stream().filter(m -> m.getDisplayName().equalsIgnoreCase(display)).findFirst().orElse(null);
   }

   public List<Module> getModulesByCategory(Module.Category category) {
      return this.stream().filter(m -> m.getCategory() == category).toList();
   }

   public List<Module.Category> getCategories() {
      return Arrays.asList(Module.Category.values());
   }

   public void onLoad() {
      this.getModules().forEach(Module::onLoad);
   }

   public void onTick() {
      this.stream().filter(Feature::isEnabled).forEach(Module::onTick);
   }

   public void onRender2D(Render2DEvent event) {
      this.stream().filter(Feature::isEnabled).forEach(module -> module.onRender2D(event));
   }

   public void onRender3D(Render3DEvent event) {
      this.stream().filter(Feature::isEnabled).forEach(module -> module.onRender3D(event));
   }

   public void onUnload() {
      this.getModules().forEach(EVENT_BUS::unregister);
      this.getModules().forEach(Module::onUnload);
   }

   public void onKeyPressed(int key) {
      if (key > 0 && mc.screen == null) {
         this.stream().filter(module -> module.getBind().getKey() == key).forEach(Toggleable::toggle);
      }
   }

   public void onMouseClicked(int button) {
      if (mc.screen == null) {
         int key = -button - 2;
         this.stream().filter(module -> module.getBind().getKey() == key).forEach(Toggleable::toggle);
      }
   }

   @Override
   public JsonElement toJson() {
      JsonObject object = new JsonObject();

      for (Module module : this.getModules()) {
         object.add(module.getName(), module.toJson());
      }

      return object;
   }

   @Override
   public void fromJson(JsonElement element) {
      for (Module module : this.getModules()) {
         JsonElement moduleElement = element.getAsJsonObject().get(module.getName());
         if (moduleElement != null) {
            module.fromJson(moduleElement);
         }
      }
   }

   @Override
   public String getFileName() {
      return "modules.json";
   }
}
