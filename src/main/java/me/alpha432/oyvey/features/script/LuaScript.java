package me.alpha432.oyvey.features.script;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.util.MathUtil;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

public class LuaScript implements Util {
   private final File file;
   private String name;
   private String description;
   private boolean enabled = false;
   private boolean loaded = false;
   private Globals globals;
   private final List<LuaScriptSetting> scriptSettings = new ArrayList<>();

   public LuaScript(File file) {
      this.file = file;
      this.name = file.getName().replace(".lua", "");
      this.description = "";
   }

   public void load() {
      this.scriptSettings.clear();
      this.globals = JsePlatform.standardGlobals();
      this.registerAPI();

      try {
         LuaValue chunk = this.globals.loadfile(this.file.getAbsolutePath());
         chunk.call();
         LuaValue n = this.globals.get("NAME");
         LuaValue d = this.globals.get("DESCRIPTION");
         if (n.isstring()) {
            this.name = n.tojstring();
         }

         if (d.isstring()) {
            this.description = d.tojstring();
         }

         this.loaded = true;
      } catch (Exception var4) {
         OyVey.LOGGER.error("[Script] Failed to load {}: {}", this.file.getName(), var4.getMessage());
         this.loaded = false;
      }
   }

   public void enable() {
      if (this.loaded) {
         this.enabled = true;
         this.callFn("onEnable");
      }
   }

   public void disable() {
      this.enabled = false;
      this.callFn("onDisable");
   }

   public void onTick() {
      if (this.enabled) {
         this.callFn("onTick");
      }
   }

   public void onMotionPre() {
      if (this.enabled) {
         this.callFn("onMotionPre");
      }
   }

   public void onMotionPost() {
      if (this.enabled) {
         this.callFn("onMotionPost");
      }
   }

   public void onDeath() {
      if (this.enabled) {
         this.callFn("onDeath");
      }
   }

   public void onPacketSend(String type) {
      if (this.enabled) {
         this.callFnArg("onPacketSend", LuaValue.valueOf(type));
      }
   }

   public void onPacketReceive(String type) {
      if (this.enabled) {
         this.callFnArg("onPacketReceive", LuaValue.valueOf(type));
      }
   }

   public void onKey(int key) {
      if (this.enabled) {
         this.callFnArg("onKey", LuaValue.valueOf(key));
      }
   }

   private void callFn(String fn) {
      try {
         LuaValue f = this.globals.get(fn);
         if (f.isfunction()) {
            f.call();
         }
      } catch (LuaError var3) {
         OyVey.LOGGER.error("[Script:{}] {}: {}", this.name, fn, var3.getMessage());
      }
   }

   private void callFnArg(String fn, LuaValue arg) {
      try {
         LuaValue f = this.globals.get(fn);
         if (f.isfunction()) {
            f.call(arg);
         }
      } catch (LuaError var4) {
         OyVey.LOGGER.error("[Script:{}] {}: {}", this.name, fn, var4.getMessage());
      }
   }

   private void registerAPI() {
      this.globals.set("player", this.buildPlayerTable());
      this.globals.set("world", this.buildWorldTable());
      this.globals.set("module", this.buildModuleTable());
      this.globals.set("rotation", this.buildRotationTable());
      this.globals.set("util", this.buildUtilTable());
      this.globals.set("script", this.buildScriptTable());
   }

   private LuaTable buildPlayerTable() {
      LuaTable t = new LuaTable();
      t.set("getX", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.getX()) : LuaValue.ZERO;
         }
      });
      t.set("getY", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.getY()) : LuaValue.ZERO;
         }
      });
      t.set("getZ", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.getZ()) : LuaValue.ZERO;
         }
      });
      t.set("getMotionX", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.getDeltaMovement().x) : LuaValue.ZERO;
         }
      });
      t.set("getMotionY", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.getDeltaMovement().y) : LuaValue.ZERO;
         }
      });
      t.set("getMotionZ", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.getDeltaMovement().z) : LuaValue.ZERO;
         }
      });
      t.set("setMotion", new ThreeArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
            if (Util.mc.player != null) {
               Util.mc.player.setDeltaMovement(x.todouble(), y.todouble(), z.todouble());
            }

            return LuaValue.NIL;
         }
      });
      t.set("getYaw", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.getYRot()) : LuaValue.ZERO;
         }
      });
      t.set("getPitch", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.getXRot()) : LuaValue.ZERO;
         }
      });
      t.set("getHealth", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.getHealth()) : LuaValue.ZERO;
         }
      });
      t.set("getMaxHealth", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.getMaxHealth()) : LuaValue.ZERO;
         }
      });
      t.set("getFoodLevel", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.getFoodData().getFoodLevel()) : LuaValue.ZERO;
         }
      });
      t.set("isOnGround", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.onGround()) : LuaValue.FALSE;
         }
      });
      t.set("isSprinting", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.isSprinting()) : LuaValue.FALSE;
         }
      });
      t.set("setSprinting", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue v) {
            if (Util.mc.player != null) {
               Util.mc.player.setSprinting(v.toboolean());
            }

            return LuaValue.NIL;
         }
      });
      t.set("isSneaking", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.isCrouching()) : LuaValue.FALSE;
         }
      });
      t.set("isFlying", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.getAbilities().flying) : LuaValue.FALSE;
         }
      });
      t.set("setFlying", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue v) {
            if (Util.mc.player != null) {
               Util.mc.player.getAbilities().flying = v.toboolean();
            }

            return LuaValue.NIL;
         }
      });
      t.set("getName", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.getName().getString()) : LuaValue.valueOf("");
         }
      });
      t.set("getSelectedSlot", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.player != null ? LuaValue.valueOf(Util.mc.player.getInventory().getSelectedSlot()) : LuaValue.ZERO;
         }
      });
      t.set("setSelectedSlot", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue v) {
            if (Util.mc.player != null) {
               Util.mc.player.getInventory().setSelectedSlot(Math.max(0, Math.min(8, v.toint())));
            }

            return LuaValue.NIL;
         }
      });
      t.set(
         "getHeldItemName",
         new ZeroArgFunction() {
            {
               Objects.requireNonNull(LuaScript.this);
            }

            @Override
            public LuaValue call() {
               return Util.mc.player == null
                  ? LuaValue.valueOf("minecraft:air")
                  : LuaValue.valueOf(BuiltInRegistries.ITEM.getKey(Util.mc.player.getMainHandItem().getItem()).toString());
            }
         }
      );
      t.set("getInventoryItem", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue slot) {
            if (Util.mc.player == null) {
               return LuaValue.valueOf("minecraft:air");
            }

            ItemStack stack = Util.mc.player.getInventory().getItem(slot.toint());
            return LuaValue.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
         }
      });
      t.set("sendChat", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue v) {
            if (Util.mc.player != null) {
               Util.mc.player.connection.sendChat(v.tojstring());
            }

            return LuaValue.NIL;
         }
      });
      t.set("printChat", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue v) {
            if (Util.mc.player != null) {
               Util.mc.player.sendSystemMessage(Component.literal(v.tojstring()));
            }

            return LuaValue.NIL;
         }
      });
      t.set("swing", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            if (Util.mc.player != null) {
               Util.mc.player.swing(InteractionHand.MAIN_HAND);
            }

            return LuaValue.NIL;
         }
      });
      t.set("jump", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            if (Util.mc.player != null) {
               Util.mc.player.setDeltaMovement(Util.mc.player.getDeltaMovement().x, 0.42, Util.mc.player.getDeltaMovement().z);
            }

            return LuaValue.NIL;
         }
      });
      t.set("attack", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue entityId) {
            if (Util.mc.player != null && Util.mc.level != null) {
               Entity entity = Util.mc.level.getEntity(entityId.toint());
               if (entity != null) {
                  Util.mc.gameMode.attack(Util.mc.player, entity);
               }

               return LuaValue.NIL;
            } else {
               return LuaValue.NIL;
            }
         }
      });
      return t;
   }

   private LuaTable buildWorldTable() {
      LuaTable t = new LuaTable();
      t.set("getBlock", new ThreeArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
            if (Util.mc.level == null) {
               return LuaValue.valueOf("minecraft:air");
            }

            Block block = Util.mc.level.getBlockState(new BlockPos(x.toint(), y.toint(), z.toint())).getBlock();
            return LuaValue.valueOf(BuiltInRegistries.BLOCK.getKey(block).toString());
         }
      });
      t.set("isAir", new ThreeArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
            return Util.mc.level == null ? LuaValue.FALSE : LuaValue.valueOf(Util.mc.level.isEmptyBlock(new BlockPos(x.toint(), y.toint(), z.toint())));
         }
      });
      t.set(
         "isSolid",
         new ThreeArgFunction() {
            {
               Objects.requireNonNull(LuaScript.this);
            }

            @Override
            public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
               return Util.mc.level == null
                  ? LuaValue.FALSE
                  : LuaValue.valueOf(Util.mc.level.getBlockState(new BlockPos(x.toint(), y.toint(), z.toint())).isSolid());
            }
         }
      );
      t.set("getTime", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.level != null ? LuaValue.valueOf(Util.mc.level.getGameTime()) : LuaValue.ZERO;
         }
      });
      t.set("isDay", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return Util.mc.level == null ? LuaValue.FALSE : LuaValue.valueOf(Util.mc.level.getGameTime() % 24000L < 13000L);
         }
      });
      t.set("getDimension", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            if (Util.mc.level == null) {
               return LuaValue.valueOf("unknown");
            }

            try {
               return LuaValue.valueOf(Util.mc.level.dimension().identifier().toString());
            } catch (Exception var2) {
               return LuaValue.valueOf(Util.mc.level.dimension().toString());
            }
         }
      });
      t.set("getPlayers", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            LuaTable result = new LuaTable();
            if (Util.mc.level != null && Util.mc.player != null) {
               int i = 1;

               for (AbstractClientPlayer p : Util.mc.level.players()) {
                  if (p != Util.mc.player) {
                     LuaTable row = new LuaTable();
                     row.set("name", LuaValue.valueOf(p.getName().getString()));
                     row.set("x", LuaValue.valueOf(p.getX()));
                     row.set("y", LuaValue.valueOf(p.getY()));
                     row.set("z", LuaValue.valueOf(p.getZ()));
                     row.set("health", LuaValue.valueOf(p.getHealth()));
                     row.set("id", LuaValue.valueOf(p.getId()));
                     result.set(i++, row);
                  }
               }

               return result;
            } else {
               return result;
            }
         }
      });
      t.set("getClosestPlayer", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            if (Util.mc.level != null && Util.mc.player != null) {
               Player best = null;
               double bestDist = 1.7976931348623157E308;

               for (AbstractClientPlayer p : Util.mc.level.players()) {
                  if (p != Util.mc.player) {
                     double d = Util.mc.player.distanceTo(p);
                     if (d < bestDist) {
                        bestDist = d;
                        best = p;
                     }
                  }
               }

               if (best == null) {
                  return LuaValue.NIL;
               }

               LuaTable row = new LuaTable();
               row.set("name", LuaValue.valueOf(best.getName().getString()));
               row.set("x", LuaValue.valueOf(best.getX()));
               row.set("y", LuaValue.valueOf(best.getY()));
               row.set("z", LuaValue.valueOf(best.getZ()));
               row.set("health", LuaValue.valueOf(best.getHealth()));
               row.set("dist", LuaValue.valueOf(bestDist));
               row.set("id", LuaValue.valueOf(best.getId()));
               return row;
            } else {
               return LuaValue.NIL;
            }
         }
      });
      t.set("getEntities", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            LuaTable result = new LuaTable();
            if (Util.mc.level != null && Util.mc.player != null) {
               int i = 1;

               for (Entity entity : Util.mc.level.entitiesForRendering()) {
                  if (entity != Util.mc.player) {
                     LuaTable row = new LuaTable();
                     row.set("name", LuaValue.valueOf(entity.getName().getString()));
                     row.set("x", LuaValue.valueOf(entity.getX()));
                     row.set("y", LuaValue.valueOf(entity.getY()));
                     row.set("z", LuaValue.valueOf(entity.getZ()));
                     row.set("id", LuaValue.valueOf(entity.getId()));
                     row.set("type", LuaValue.valueOf(entity.getType().toShortString()));
                     row.set("health", entity instanceof LivingEntity le ? LuaValue.valueOf(le.getHealth()) : LuaValue.ZERO);
                     result.set(i++, row);
                  }
               }

               return result;
            } else {
               return result;
            }
         }
      });
      return t;
   }

   private LuaTable buildModuleTable() {
      LuaTable t = new LuaTable();
      t.set("isEnabled", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue name) {
            Module mod = OyVey.moduleManager.getModuleByName(name.tojstring());
            return LuaValue.valueOf(mod != null && mod.isEnabled());
         }
      });
      t.set("enable", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue name) {
            Module mod = OyVey.moduleManager.getModuleByName(name.tojstring());
            if (mod != null && !mod.isEnabled()) {
               mod.enable();
            }

            return LuaValue.NIL;
         }
      });
      t.set("disable", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue name) {
            Module mod = OyVey.moduleManager.getModuleByName(name.tojstring());
            if (mod != null && mod.isEnabled()) {
               mod.disable();
            }

            return LuaValue.NIL;
         }
      });
      t.set("toggle", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue name) {
            Module mod = OyVey.moduleManager.getModuleByName(name.tojstring());
            if (mod != null) {
               mod.toggle();
            }

            return LuaValue.NIL;
         }
      });
      return t;
   }

   private LuaTable buildRotationTable() {
      LuaTable t = new LuaTable();
      t.set("setServer", new TwoArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue yaw, LuaValue pitch) {
            OyVey.rotationManager.setServerRotations((float)yaw.todouble(), (float)pitch.todouble());
            return LuaValue.NIL;
         }
      });
      t.set("clear", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            OyVey.rotationManager.clearServerRotations();
            return LuaValue.NIL;
         }
      });
      t.set("getServerYaw", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return LuaValue.valueOf(OyVey.rotationManager.getServerYaw());
         }
      });
      t.set("getServerPitch", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return LuaValue.valueOf(OyVey.rotationManager.getServerPitch());
         }
      });
      t.set("lookAt", new ThreeArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
            if (Util.mc.player == null) {
               return LuaValue.NIL;
            }

            float[] angles = MathUtil.calcAngle(Util.mc.player.getEyePosition(), new Vec3(x.todouble(), y.todouble(), z.todouble()));
            OyVey.rotationManager.setServerRotations(angles[0], angles[1]);
            return LuaValue.NIL;
         }
      });
      return t;
   }

   private LuaTable buildUtilTable() {
      LuaTable t = new LuaTable();
      t.set("log", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue v) {
            OyVey.LOGGER.info("[Script:{}] {}", this.name, v.tojstring());
            return LuaValue.NIL;
         }
      });
      t.set("time", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return LuaValue.valueOf(System.currentTimeMillis());
         }
      });
      t.set("random", new TwoArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue min, LuaValue max) {
            double lo = min.todouble();
            double hi = max.todouble();
            return LuaValue.valueOf(lo + Math.random() * (hi - lo));
         }
      });
      t.set("distance", new VarArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public Varargs invoke(Varargs args) {
            double dx = args.todouble(4) - args.todouble(1);
            double dy = args.todouble(5) - args.todouble(2);
            double dz = args.todouble(6) - args.todouble(3);
            return LuaValue.valueOf(Math.sqrt(dx * dx + dy * dy + dz * dz));
         }
      });
      t.set("clamp", new ThreeArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue v, LuaValue mn, LuaValue mx) {
            return LuaValue.valueOf(Math.max(mn.todouble(), Math.min(mx.todouble(), v.todouble())));
         }
      });
      t.set("floor", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue v) {
            return LuaValue.valueOf((int)Math.floor(v.todouble()));
         }
      });
      t.set("ceil", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue v) {
            return LuaValue.valueOf((int)Math.ceil(v.todouble()));
         }
      });
      t.set("round", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue v) {
            return LuaValue.valueOf((int)Math.round(v.todouble()));
         }
      });
      t.set(
         "calcAngle",
         new VarArgFunction() {
            {
               Objects.requireNonNull(LuaScript.this);
            }

            @Override
            public Varargs invoke(Varargs args) {
               float[] angles = MathUtil.calcAngle(
                  new Vec3(args.todouble(1), args.todouble(2), args.todouble(3)), new Vec3(args.todouble(4), args.todouble(5), args.todouble(6))
               );
               LuaTable r = new LuaTable();
               r.set("yaw", LuaValue.valueOf(angles[0]));
               r.set("pitch", LuaValue.valueOf(angles[1]));
               return r;
            }
         }
      );
      return t;
   }

   private LuaTable buildScriptTable() {
      LuaTable t = new LuaTable();
      t.set("getName", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return LuaValue.valueOf(this.name);
         }
      });
      t.set("getDescription", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return LuaValue.valueOf(LuaScript.this.description);
         }
      });
      t.set("isEnabled", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return LuaValue.valueOf(LuaScript.this.enabled);
         }
      });
      t.set("addBoolean", new TwoArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue n, LuaValue def) {
            LuaScriptSetting s = new LuaScriptSetting(n.tojstring(), def.toboolean());
            LuaScript.this.scriptSettings.add(s);
            return LuaScript.this.settingHandle(s);
         }
      });
      t.set("addSlider", new VarArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public Varargs invoke(Varargs args) {
            LuaScriptSetting s = new LuaScriptSetting(args.arg(1).tojstring(), args.arg(2).todouble(), args.arg(3).todouble(), args.arg(4).todouble());
            LuaScript.this.scriptSettings.add(s);
            return LuaScript.this.settingHandle(s);
         }
      });
      t.set("addEnum", new VarArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public Varargs invoke(Varargs args) {
            LuaTable opts = (LuaTable)args.arg(2);
            List<String> list = new ArrayList<>();

            for (int i = 1; i <= opts.length(); i++) {
               list.add(opts.get(i).tojstring());
            }

            LuaScriptSetting s = new LuaScriptSetting(args.arg(1).tojstring(), list.toArray(new String[0]), args.arg(3).toint() - 1);
            LuaScript.this.scriptSettings.add(s);
            return LuaScript.this.settingHandle(s);
         }
      });
      t.set("getSetting", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue n) {
            for (LuaScriptSetting s : LuaScript.this.scriptSettings) {
               if (s.getName().equals(n.tojstring())) {
                  return switch (s.getType()) {
                     case BOOLEAN -> LuaValue.valueOf(s.getBoolean());
                     case SLIDER -> LuaValue.valueOf(s.getDouble());
                     case ENUM -> LuaValue.valueOf(s.getString());
                  };
               }
            }

            return LuaValue.NIL;
         }
      });
      return t;
   }

   private LuaTable settingHandle(final LuaScriptSetting s) {
      LuaTable h = new LuaTable();
      h.set("get", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            return switch (s.getType()) {
               case BOOLEAN -> LuaValue.valueOf(s.getBoolean());
               case SLIDER -> LuaValue.valueOf(s.getDouble());
               case ENUM -> LuaValue.valueOf(s.getString());
            };
         }
      });
      h.set("set", new OneArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call(LuaValue v) {
            switch (s.getType()) {
               case BOOLEAN:
                  s.setBoolValue(v.toboolean());
                  break;
               case SLIDER:
                  s.setDoubleValue(v.todouble());
                  break;
               case ENUM:
                  s.setStringValue(v.tojstring());
            }

            return LuaValue.NIL;
         }
      });
      h.set("cycle", new ZeroArgFunction() {
         {
            Objects.requireNonNull(LuaScript.this);
         }

         @Override
         public LuaValue call() {
            s.cycleEnum();
            return LuaValue.NIL;
         }
      });
      return h;
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public boolean isLoaded() {
      return this.loaded;
   }

   public File getFile() {
      return this.file;
   }

   public List<LuaScriptSetting> getScriptSettings() {
      return this.scriptSettings;
   }
}
