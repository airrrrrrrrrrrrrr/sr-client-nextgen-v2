package me.alpha432.oyvey.features.modules.player;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class RegenModule extends Module {
   public final Setting<RegenModule.Mode> mode = this.register(new Setting<>("Mode", RegenModule.Mode.TIMER));
   public final Setting<Double> speed = this.register(new Setting<>("Speed", 1.0, 0.5, 20.0, v -> this.mode.getValue() == RegenModule.Mode.DEATHZONE));
   public final Setting<Double> tickBoost = this.register(new Setting<>("TickBoost", 3.0, 1.0, 10.0, v -> this.mode.getValue() == RegenModule.Mode.TIMER));
   public final Setting<Boolean> cancelC0C = this.register(new Setting<>("CancelC0C", true));
   private boolean boosting = false;

   public RegenModule() {
      super("Regen", "Regenerates health without food.", Module.Category.PLAYER);
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         if (this.mode.getValue() == RegenModule.Mode.DEATHZONE) {
            mc.player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, (int)Math.round(this.speed.getValue()) - 1, false, false));
            float max = mc.player.getMaxHealth();
            if (mc.player.getHealth() < max) {
               mc.player.setHealth(Math.min(mc.player.getHealth() + this.speed.getValue().floatValue() * 0.1F, max));
            }
         } else if (this.mode.getValue() == RegenModule.Mode.TIMER) {
            if (mc.player.getHealth() < mc.player.getMaxHealth()) {
               OyVey.TIMER = this.tickBoost.getValue().floatValue();
               this.boosting = true;
            } else if (this.boosting) {
               OyVey.TIMER = 1.0F;
               this.boosting = false;
            }
         }
      }
   }

   @Override
   public void onDisable() {
      if (this.boosting) {
         OyVey.TIMER = 1.0F;
         this.boosting = false;
      }
   }

   @Subscribe
   public void onReceive(PacketEvent.Receive event) {
      if (this.mode.getValue() == RegenModule.Mode.DEATHZONE
         && mc.player != null
         && event.getPacket() instanceof ClientboundSetHealthPacket pkt
         && pkt.getHealth() < mc.player.getHealth()) {
         event.cancel();
         mc.player.hurtTo(mc.player.getHealth());
         mc.player.getFoodData().setFoodLevel(20);
         mc.player.getFoodData().setSaturation(5.0F);
      }
   }

   @Subscribe
   public void onSend(PacketEvent.Send event) {
      if (mc.player != null && this.cancelC0C.getValue() && event.getPacket() instanceof ServerboundPlayerInputPacket) {
         event.cancel();
      } else {
         if (this.mode.getValue() == RegenModule.Mode.DEATHZONE && mc.player != null) {
            if (event.getPacket() instanceof ServerboundPlayerCommandPacket c
               && (c.getAction() == Action.STOP_SPRINTING || c.getAction() == Action.START_SPRINTING)) {
               event.cancel();
            }

            if (event.getPacket() instanceof ServerboundPlayerAbilitiesPacket) {
               event.cancel();
            }
         }
      }
   }

   @Override
   public String getDisplayInfo() {
      return this.mode.getValue() == RegenModule.Mode.TIMER ? "timer " + this.tickBoost.getValue() + "x" : this.mode.getValue().name().toLowerCase();
   }

   public enum Mode {
      TIMER,
      DEATHZONE;
   }
}
