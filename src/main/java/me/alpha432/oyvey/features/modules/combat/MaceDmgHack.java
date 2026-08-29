package me.alpha432.oyvey.features.modules.combat;

import me.alpha432.oyvey.event.impl.network.PacketEvent;
import me.alpha432.oyvey.event.system.Subscribe;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

public class MaceDmgHack extends Module {
   private final Setting<Double> fallHeight = this.register(new Setting<>("FallHeight", 10.0, 1.0, 50.0));
   private final Setting<Boolean> requireMace = this.register(new Setting<>("RequireMace", true));

   public MaceDmgHack() {
      super("MaceDmgHack", "Fakes fall distance to boost mace damage", Module.Category.COMBAT);
   }

   @Subscribe
   private void onPacketSend(PacketEvent.Send event) {
      if (event.getPacket() instanceof ServerboundAttackPacket packet && (!this.requireMace.getValue() || mc.player.getMainHandItem().is(Items.MACE))) {
         Entity target = mc.level.getEntity(packet.entityId());
         if (target instanceof LivingEntity) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            boolean hc = mc.player.horizontalCollision;
            double fh = this.fallHeight.getValue();
            mc.player.connection.send(new Pos(x, y + fh, z, false, hc));
            mc.player.connection.send(new Pos(x, y, z, false, hc));
         }
      }
   }

   @Override
   public String getDisplayInfo() {
      return (int)this.fallHeight.getValue().doubleValue() + "b";
   }
}
