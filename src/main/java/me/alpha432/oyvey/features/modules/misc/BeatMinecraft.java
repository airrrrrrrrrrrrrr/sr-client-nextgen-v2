package me.alpha432.oyvey.features.modules.misc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.util.RotationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BeatMinecraft extends Module {
   private BeatMinecraft.Phase phase = BeatMinecraft.Phase.GATHER_WOOD;
   private final Deque<Runnable> craftActions = new ArrayDeque<>();
   private int craftTickDelay = 0;
   private BlockPos craftTablePos = null;
   private BlockPos seekTarget = null;
   private BlockPos breakTarget = null;
   private boolean miningActive = false;
   private List<BlockPos> path = null;
   private int pathIndex = 0;
   private BlockPos lastGoal = null;
   private long lastPathCalc = 0L;
   private long lastSwing = 0L;
   private int wanderTick = 0;
   private int wanderDir = 0;
   private int furnaceTick = 0;
   private boolean smelting = false;
   private BlockPos furnacePos = null;
   private final int smeltTarget = 5;
   private BlockPos portalAnchor = null;
   private int strongholdEyeCooldown = 0;
   private float strongholdHeading = 0.0F / 0.0F;
   private boolean furnaceReady = false;
   private static final int[][] PORTAL_FRAME = new int[][]{{0, -1}, {1, -1}, {-1, 0}, {-1, 1}, {-1, 2}, {2, 0}, {2, 1}, {2, 2}, {0, 3}, {1, 3}};

   public BeatMinecraft() {
      super("BeatMinecraft", "Beats Minecraft automatically using speedrun strategies.", Module.Category.MISC);
   }

   @Override
   public void onEnable() {
      this.phase = this.detectPhase();
      this.craftActions.clear();
      this.seekTarget = null;
      this.breakTarget = null;
      this.miningActive = false;
      this.path = null;
      this.pathIndex = 0;
      this.lastGoal = null;
      this.smelting = false;
      this.furnacePos = null;
      this.furnaceReady = false;
      this.portalAnchor = null;
      this.strongholdHeading = 0.0F / 0.0F;
      this.strongholdEyeCooldown = 0;
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         mc.player.setSprinting(false);
         this.stopMining();
      }
   }

   @Override
   public void onTick() {
      if (!nullCheck()) {
         if (!this.craftActions.isEmpty()) {
            if (this.craftTickDelay-- <= 0) {
               this.craftActions.poll().run();
               this.craftTickDelay = 3;
            }
         } else if (this.smelting) {
            this.tickSmeltIron();
         } else {
            this.phase = this.detectPhase();
            switch (this.phase) {
               case GATHER_WOOD:
                  this.tickGatherWood();
                  break;
               case CRAFT_BASICS:
                  this.tickCraftBasics();
                  break;
               case GATHER_STONE:
                  this.tickGatherStone();
                  break;
               case CRAFT_TOOLS:
                  this.tickCraftTools();
                  break;
               case GATHER_IRON:
                  this.tickGatherIron();
                  break;
               case SMELT_IRON:
                  this.tickSmeltIron();
                  break;
               case GATHER_FLINT:
                  this.tickGatherFlint();
                  break;
               case CRAFT_IRON_GEAR:
                  this.tickCraftIronGear();
                  break;
               case GATHER_DIAMOND:
                  this.tickGatherDiamond();
                  break;
               case CRAFT_DIAMOND:
                  this.tickCraftDiamond();
                  break;
               case GATHER_OBSIDIAN:
                  this.tickGatherObsidian();
                  break;
               case BUILD_PORTAL:
                  this.tickBuildPortal();
                  break;
               case ENTER_NETHER:
                  this.tickEnterNether();
                  break;
               case FIND_FORTRESS:
                  this.tickFindFortress();
                  break;
               case KILL_BLAZES:
                  this.tickKillBlazes();
                  break;
               case GATHER_PEARLS:
                  this.tickGatherPearls();
                  break;
               case EXIT_NETHER:
                  this.tickExitNether();
                  break;
               case CRAFT_EYES:
                  this.tickCraftEyes();
                  break;
               case FIND_STRONGHOLD:
                  this.tickFindStronghold();
                  break;
               case KILL_DRAGON:
                  this.tickKillDragon();
                  break;
               case DONE:
                  mc.player.setSprinting(false);
                  this.disable();
            }
         }
      }
   }

   private BeatMinecraft.Phase detectPhase() {
      if (mc.level.dimension() == Level.END) {
         return BeatMinecraft.Phase.KILL_DRAGON;
      }

      if (mc.level.dimension() == Level.NETHER) {
         if (this.countItem(Items.BLAZE_ROD) >= 7 && this.countItem(Items.ENDER_PEARL) >= 12) {
            return BeatMinecraft.Phase.EXIT_NETHER;
         } else if (this.countItem(Items.BLAZE_ROD) >= 7) {
            return BeatMinecraft.Phase.GATHER_PEARLS;
         } else {
            return this.countItem(Items.BLAZE_ROD) >= 1 ? BeatMinecraft.Phase.KILL_BLAZES : BeatMinecraft.Phase.FIND_FORTRESS;
         }
      } else {
         if (this.countItem(Items.ENDER_EYE) >= 12) {
            return BeatMinecraft.Phase.FIND_STRONGHOLD;
         }

         if ((this.countItem(Items.BLAZE_POWDER) >= 1 || this.countItem(Items.BLAZE_ROD) >= 1)
            && this.countItem(Items.ENDER_PEARL) >= 1
            && this.countItem(Items.ENDER_EYE) < 12) {
            return BeatMinecraft.Phase.CRAFT_EYES;
         }

         if (!this.hasItem(Items.WOODEN_PICKAXE)) {
            return this.logsNeeded() > 0 ? BeatMinecraft.Phase.GATHER_WOOD : BeatMinecraft.Phase.CRAFT_BASICS;
         }

         boolean toolsDone = this.hasItem(Items.STONE_PICKAXE) && this.hasItem(Items.STONE_SWORD) && (this.hasItem(Items.FURNACE) || this.furnaceReady);
         if (!toolsDone) {
            return this.countCobble() >= 13 ? BeatMinecraft.Phase.CRAFT_TOOLS : BeatMinecraft.Phase.GATHER_STONE;
         }

         boolean needIronPick = !this.hasItem(Items.IRON_PICKAXE);
         boolean needFlintSteel = !this.hasItem(Items.FLINT_AND_STEEL);
         int ironNeeded = (needIronPick ? 3 : 0) + (needFlintSteel ? 1 : 0);
         if (ironNeeded > 0) {
            int ingots = this.countItem(Items.IRON_INGOT);
            int raw = this.countItem(Items.RAW_IRON);
            if (ingots >= ironNeeded) {
               return needFlintSteel && !this.hasItem(Items.FLINT) ? BeatMinecraft.Phase.GATHER_FLINT : BeatMinecraft.Phase.CRAFT_IRON_GEAR;
            } else {
               return raw + ingots < ironNeeded && raw < 1 ? BeatMinecraft.Phase.GATHER_IRON : BeatMinecraft.Phase.SMELT_IRON;
            }
         } else if (!this.hasItem(Items.DIAMOND_PICKAXE)) {
            return this.countItem(Items.DIAMOND) >= 3 ? BeatMinecraft.Phase.CRAFT_DIAMOND : BeatMinecraft.Phase.GATHER_DIAMOND;
         } else if (this.countItem(Items.OBSIDIAN) < 10) {
            return BeatMinecraft.Phase.GATHER_OBSIDIAN;
         } else {
            return this.portalExists() ? BeatMinecraft.Phase.ENTER_NETHER : BeatMinecraft.Phase.BUILD_PORTAL;
         }
      }
   }

   private int planksAvailable() {
      return this.countPlanks() + this.countLogs() * 4;
   }

   private int planksRequired() {
      int need = 0;
      if (!this.hasItem(Items.CRAFTING_TABLE)) {
         need += 4;
      }

      if (!this.hasItem(Items.WOODEN_PICKAXE)) {
         need += 4;
      }

      return need + 6;
   }

   private int logsNeeded() {
      int deficit = this.planksRequired() - this.planksAvailable();
      return deficit > 0 ? (int)Math.ceil(deficit / 4.0) : 0;
   }

   private void tickGatherWood() {
      if (this.logsNeeded() <= 0) {
         this.seekTarget = null;
      } else {
         if (this.seekTarget == null || mc.level.getBlockState(this.seekTarget).isAir()) {
            this.seekTarget = this.findBlock(48, b -> b.is(BlockTags.LOGS));
         }

         if (this.seekTarget == null) {
            this.wander();
         } else {
            this.navigateAndBreak(this.seekTarget);
         }
      }
   }

   private void tickCraftBasics() {
      if (!this.hasEnoughPlanks()) {
         this.schedulePlanksFromLogs();
      } else if (!this.hasItem(Items.CRAFTING_TABLE)) {
         this.scheduleCraftTable();
      } else if (!this.hasItem(Items.WOODEN_PICKAXE)) {
         if (this.craftTablePos == null || !mc.level.getBlockState(this.craftTablePos).is(Blocks.CRAFTING_TABLE)) {
            this.craftTablePos = this.findBlock(8, b -> b.is(Blocks.CRAFTING_TABLE));
            if (this.craftTablePos == null) {
               this.craftTablePos = mc.player.blockPosition().offset(1, 0, 0);
               this.placeBlock(Items.CRAFTING_TABLE, this.craftTablePos);
               return;
            }
         }

         this.pathTo(this.craftTablePos);
         if (!(this.distSq(this.craftTablePos) > 9.0)) {
            if (mc.player.containerMenu instanceof CraftingMenu) {
               if (this.countItem(Items.STICK) < 2) {
                  this.scheduleSticks3x3();
                  return;
               }

               this.scheduleWoodenPickaxe();
            } else {
               this.useBlock(this.craftTablePos);
            }
         }
      }
   }

   private boolean hasEnoughPlanks() {
      int need = 0;
      if (!this.hasItem(Items.CRAFTING_TABLE)) {
         need += 4;
      }

      if (!this.hasItem(Items.WOODEN_PICKAXE)) {
         need += 3;
      }

      need += 2;
      return this.countPlanks() >= need;
   }

   private void tickGatherStone() {
      this.equipBestPickaxe();
      if (this.seekTarget == null || mc.level.getBlockState(this.seekTarget).isAir()) {
         this.seekTarget = this.findBlock(32, b -> b.is(Blocks.STONE) || b.is(Blocks.COBBLESTONE));
      }

      if (this.seekTarget == null) {
         this.wander();
      } else {
         this.navigateAndBreak(this.seekTarget);
      }
   }

   private void tickCraftTools() {
      if (this.craftTablePos == null || !mc.level.getBlockState(this.craftTablePos).is(Blocks.CRAFTING_TABLE)) {
         this.craftTablePos = this.findBlock(8, b -> b.is(Blocks.CRAFTING_TABLE));
         if (this.craftTablePos == null) {
            this.craftTablePos = mc.player.blockPosition().offset(1, 0, 0);
            this.placeBlock(Items.CRAFTING_TABLE, this.craftTablePos);
            return;
         }
      }

      this.pathTo(this.craftTablePos);
      if (!(this.distSq(this.craftTablePos) > 9.0)) {
         if (mc.player.containerMenu instanceof CraftingMenu) {
            if (!this.hasItem(Items.STONE_PICKAXE)) {
               this.scheduleStonePickaxe();
            }

            if (!this.hasItem(Items.STONE_SWORD)) {
               this.scheduleStoneSword();
            }

            if (!this.hasItem(Items.FURNACE) && !this.furnaceReady) {
               this.scheduleFurnace();
            }

            this.enq(() -> mc.player.closeContainer());
         } else {
            this.useBlock(this.craftTablePos);
         }
      }
   }

   private void tickGatherIron() {
      this.equipBestPickaxe();
      if (this.seekTarget == null || mc.level.getBlockState(this.seekTarget).isAir()) {
         this.seekTarget = this.findBlock(48, b -> b.is(Blocks.IRON_ORE) || b.is(Blocks.DEEPSLATE_IRON_ORE));
      }

      if (this.seekTarget == null) {
         this.wander();
      } else {
         this.navigateAndBreak(this.seekTarget);
      }
   }

   private void tickSmeltIron() {
      if (this.furnacePos == null || !mc.level.getBlockState(this.furnacePos).is(Blocks.FURNACE)) {
         this.furnacePos = this.findBlock(16, b -> b.is(Blocks.FURNACE));
      }

      if (this.furnacePos != null) {
         this.furnaceReady = true;
      }

      if (this.furnacePos == null) {
         if (this.hasItem(Items.FURNACE)) {
            this.placeBlock(Items.FURNACE, mc.player.blockPosition().relative(mc.player.getDirection()));
         } else {
            this.smelting = false;
            this.wander();
         }
      } else if (this.distSq(this.furnacePos) > 9.0) {
         this.pathTo(this.furnacePos);
      } else {
         mc.player.setSprinting(false);
         if (mc.player.containerMenu instanceof AbstractFurnaceMenu menu) {
            this.smelting = true;
            if (this.furnaceTick-- <= 0) {
               this.furnaceTick = 6;
               if (!menu.getSlot(2).getItem().isEmpty()) {
                  this.clickMenu(2, 0, ContainerInput.QUICK_MOVE);
               } else {
                  boolean moreRaw = this.findInvIdx(s -> s.is(Items.RAW_IRON)) != -1;
                  if (menu.getSlot(0).getItem().isEmpty() && moreRaw) {
                     this.clickMenu(this.furnaceSlot(this.findInvIdx(s -> s.is(Items.RAW_IRON))), 0, ContainerInput.QUICK_MOVE);
                  } else {
                     boolean inputBusy = !menu.getSlot(0).getItem().isEmpty();
                     if (menu.getSlot(1).getItem().isEmpty() && inputBusy) {
                        int fidx = this.findInvIdx(this::isFuel);
                        if (fidx != -1) {
                           this.clickMenu(this.furnaceSlot(fidx), 0, ContainerInput.QUICK_MOVE);
                           return;
                        }
                     }

                     boolean enough = this.countItem(Items.IRON_INGOT) >= 5;
                     boolean idle = !inputBusy && !moreRaw && menu.getSlot(2).getItem().isEmpty();
                     if (enough && !inputBusy && !moreRaw || idle) {
                        mc.player.closeContainer();
                        this.furnacePos = null;
                        this.smelting = false;
                     }
                  }
               }
            }
         } else {
            if (this.furnaceTick-- <= 0) {
               this.useBlock(this.furnacePos);
               this.furnaceTick = 8;
            }
         }
      }
   }

   private int furnaceSlot(int invIdx) {
      return invIdx < 9 ? 30 + invIdx : invIdx - 6;
   }

   private boolean isFuel(ItemStack s) {
      return s.is(Items.COAL) || s.is(Items.CHARCOAL) || s.is(ItemTags.PLANKS) || s.is(ItemTags.LOGS) || s.is(Items.STICK);
   }

   private void tickBuildPortal() {
      if (this.portalAnchor == null) {
         this.portalAnchor = mc.player.blockPosition().relative(mc.player.getDirection(), 2);
      }

      for (int[] o : PORTAL_FRAME) {
         BlockPos t = this.portalAnchor.offset(o[0], o[1], 0);
         if (!mc.level.getBlockState(t).is(Blocks.OBSIDIAN)) {
            if (this.distSq(t) > 16.0) {
               this.pathTo(t);
            } else {
               mc.player.setSprinting(false);
               this.placeAt(Items.OBSIDIAN, t);
            }

            return;
         }
      }

      this.selectItem(Items.FLINT_AND_STEEL);
      BlockPos floor = this.portalAnchor.offset(0, -1, 0);
      Vec3 hit = Vec3.atCenterOf(floor).add(0.0, 0.5, 0.0);
      float[] rot = RotationUtils.fromRotationVec(hit.x - mc.player.getX(), hit.y - (mc.player.getY() + mc.player.getEyeHeight()), hit.z - mc.player.getZ());
      OyVey.rotationManager.setPlayerRotations(rot[0], rot[1]);
      mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, floor, false));
      mc.player.swing(InteractionHand.MAIN_HAND);
   }

   private boolean portalExists() {
      return this.findBlock(8, b -> b.is(Blocks.NETHER_PORTAL)) != null;
   }

   private void tickGatherFlint() {
      this.equipBestPickaxe();
      if (this.seekTarget == null || !mc.level.getBlockState(this.seekTarget).is(Blocks.GRAVEL)) {
         this.seekTarget = this.findBlock(48, b -> b.is(Blocks.GRAVEL));
      }

      if (this.seekTarget == null) {
         this.wander();
      } else {
         this.navigateAndBreak(this.seekTarget);
      }
   }

   private void tickCraftIronGear() {
      if (this.atCraftingTable()) {
         if (mc.player.containerMenu instanceof CraftingMenu) {
            if (!this.hasItem(Items.IRON_PICKAXE)) {
               this.scheduleIronPickaxe();
            }

            if (!this.hasItem(Items.FLINT_AND_STEEL) && this.hasItem(Items.FLINT)) {
               this.scheduleFlintAndSteel();
            }

            this.enq(() -> mc.player.closeContainer());
         } else {
            this.useBlock(this.craftTablePos);
         }
      }
   }

   private void tickGatherDiamond() {
      this.equipBestPickaxe();
      if (this.seekTarget == null || mc.level.getBlockState(this.seekTarget).isAir()) {
         this.seekTarget = this.findBlock(32, b -> b.is(Blocks.DIAMOND_ORE) || b.is(Blocks.DEEPSLATE_DIAMOND_ORE));
      }

      if (this.seekTarget != null) {
         this.navigateAndBreak(this.seekTarget);
      } else if (mc.player.blockPosition().getY() > -54) {
         this.digTowardsY();
      } else {
         this.stripMine();
      }
   }

   private void tickCraftDiamond() {
      if (this.atCraftingTable()) {
         if (mc.player.containerMenu instanceof CraftingMenu) {
            if (!this.hasItem(Items.DIAMOND_PICKAXE)) {
               this.scheduleDiamondPickaxe();
            }

            this.enq(() -> mc.player.closeContainer());
         } else {
            this.useBlock(this.craftTablePos);
         }
      }
   }

   private void tickGatherObsidian() {
      this.equipBestPickaxe();
      if (this.seekTarget == null || !mc.level.getBlockState(this.seekTarget).is(Blocks.OBSIDIAN)) {
         this.seekTarget = this.findBlock(64, b -> b.is(Blocks.OBSIDIAN));
      }

      if (this.seekTarget != null) {
         this.navigateAndBreak(this.seekTarget);
      } else {
         this.wander();
      }
   }

   private void tickCraftEyes() {
      int eyes = this.countItem(Items.ENDER_EYE);
      if (eyes < 12) {
         if (this.countItem(Items.BLAZE_POWDER) + eyes < 12 && this.hasItem(Items.BLAZE_ROD)) {
            this.scheduleBlazePowder();
         } else if (this.hasItem(Items.BLAZE_POWDER) && this.hasItem(Items.ENDER_PEARL)) {
            this.scheduleEnderEye();
         }
      }
   }

   private boolean atCraftingTable() {
      if (this.craftTablePos == null || !mc.level.getBlockState(this.craftTablePos).is(Blocks.CRAFTING_TABLE)) {
         this.craftTablePos = this.findBlock(8, b -> b.is(Blocks.CRAFTING_TABLE));
         if (this.craftTablePos == null) {
            if (this.hasItem(Items.CRAFTING_TABLE)) {
               this.craftTablePos = mc.player.blockPosition().relative(mc.player.getDirection());
               this.placeBlock(Items.CRAFTING_TABLE, this.craftTablePos);
            } else if (this.countPlanks() >= 4) {
               this.scheduleCraftTable();
            } else {
               this.wander();
            }

            return false;
         }
      }

      this.pathTo(this.craftTablePos);
      return this.distSq(this.craftTablePos) <= 9.0;
   }

   private void digTowardsY() {
      this.equipBestPickaxe();
      BlockPos step = mc.player.blockPosition().relative(mc.player.getDirection()).below();
      BlockPos head = step.above();
      if (!mc.level.getBlockState(head).isAir()) {
         this.navigateAndBreak(head);
      } else if (!mc.level.getBlockState(step).isAir()) {
         this.navigateAndBreak(step);
      } else {
         this.directMove(step);
      }
   }

   private void stripMine() {
      this.equipBestPickaxe();
      BlockPos ahead = mc.player.blockPosition().relative(mc.player.getDirection());
      BlockPos aheadHead = ahead.above();
      if (!mc.level.getBlockState(aheadHead).isAir()) {
         this.navigateAndBreak(aheadHead);
      } else if (!mc.level.getBlockState(ahead).isAir()) {
         this.navigateAndBreak(ahead);
      } else {
         this.directMove(ahead);
      }
   }

   private void placeAt(Item item, BlockPos target) {
      if (mc.level.getBlockState(target).isAir()) {
         this.selectItem(item);

         for (Direction d : Direction.values()) {
            BlockPos nb = target.relative(d);
            if (this.solid(nb)) {
               Direction face = d.getOpposite();
               Vec3 hit = Vec3.atCenterOf(nb).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
               float[] rot = RotationUtils.fromRotationVec(
                  hit.x - mc.player.getX(), hit.y - (mc.player.getY() + mc.player.getEyeHeight()), hit.z - mc.player.getZ()
               );
               OyVey.rotationManager.setPlayerRotations(rot[0], rot[1]);
               mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, nb, false));
               mc.player.swing(InteractionHand.MAIN_HAND);
               return;
            }
         }
      }
   }

   private void tickEnterNether() {
      BlockPos portal = this.findBlock(32, b -> b.is(Blocks.NETHER_PORTAL));
      if (portal == null) {
         this.wander();
      } else {
         this.pathTo(portal);
      }
   }

   private void tickFindFortress() {
      BlockPos brick = this.findBlock(64, b -> b.is(Blocks.NETHER_BRICKS) || b.is(Blocks.NETHER_BRICK_FENCE) || b.is(Blocks.NETHER_BRICK_STAIRS));
      if (brick != null) {
         this.pathTo(brick);
      } else {
         this.pathTo(mc.player.blockPosition().offset(200, 0, 0));
      }
   }

   private void tickKillBlazes() {
      Entity blaze = this.nearest(Blaze.class);
      if (blaze != null) {
         this.attackEntity(blaze);
      } else {
         BlockPos spawner = this.findBlock(48, b -> b.is(Blocks.SPAWNER));
         if (spawner != null) {
            this.pathTo(spawner);
         } else {
            this.wander();
         }
      }
   }

   private void tickGatherPearls() {
      Entity enderman = this.nearest(EnderMan.class);
      if (enderman != null) {
         this.attackEntity(enderman);
      } else {
         Entity piglin = this.nearest(Piglin.class);
         if (piglin != null) {
            if (this.hasItem(Items.GOLD_INGOT)) {
               this.selectItem(Items.GOLD_INGOT);
               mc.player.drop(false);
            }

            this.pathTo(piglin.blockPosition());
         } else {
            this.wander();
         }
      }
   }

   private void tickExitNether() {
      BlockPos portal = this.findBlock(64, b -> b.is(Blocks.NETHER_PORTAL));
      if (portal != null) {
         this.pathTo(portal);
      } else {
         this.wander();
      }
   }

   private void tickFindStronghold() {
      BlockPos frame = this.findBlock(24, b -> b.is(Blocks.END_PORTAL_FRAME));
      if (frame != null) {
         this.fillAndEnterEndPortal();
      } else {
         Entity flying = this.nearest(EyeOfEnder.class);
         if (flying != null) {
            Vec3 v = flying.getDeltaMovement();
            if (Math.hypot(v.x, v.z) > 0.02) {
               this.strongholdHeading = RotationUtils.fromRotationVec(v.x, 0.0, v.z)[0];
            }

            this.strongholdEyeCooldown = 30;
         }

         if (this.strongholdEyeCooldown > 0) {
            this.strongholdEyeCooldown--;
         } else if (this.hasItem(Items.ENDER_EYE)) {
            this.selectItem(Items.ENDER_EYE);
            OyVey.rotationManager.setPlayerRotations(Float.isNaN(this.strongholdHeading) ? mc.player.getYRot() : this.strongholdHeading, -45.0F);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            this.strongholdEyeCooldown = 80;
         }

         if (!Float.isNaN(this.strongholdHeading)) {
            this.walkHeading(this.strongholdHeading);
         } else {
            this.wander();
         }
      }
   }

   private void fillAndEnterEndPortal() {
      BlockPos portal = this.findBlock(6, b -> b.is(Blocks.END_PORTAL));
      if (portal != null) {
         this.pathTo(portal);
      } else {
         BlockPos frame = this.findBlock(8, b -> b.is(Blocks.END_PORTAL_FRAME));
         if (frame != null) {
            this.pathTo(frame);
            if (this.distSq(frame) < 9.0 && this.hasItem(Items.ENDER_EYE)) {
               this.selectItem(Items.ENDER_EYE);
               this.useBlock(frame);
            }
         }
      }
   }

   private void walkHeading(float yaw) {
      OyVey.rotationManager.setPlayerRotations(yaw, mc.player.getXRot());
      double rad = Math.toRadians(yaw);
      mc.player.setSprinting(true);
      mc.player.setDeltaMovement(-Math.sin(rad) * 0.28, mc.player.getDeltaMovement().y, Math.cos(rad) * 0.28);
      if (mc.player.horizontalCollision && mc.player.onGround()) {
         mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.42, mc.player.getDeltaMovement().z);
      }
   }

   private void tickKillDragon() {
      Entity crystal = this.nearest(EndCrystal.class);
      if (crystal == null) {
         Entity dragon = this.nearest(EnderDragon.class);
         if (dragon != null) {
            this.attackEntity(dragon);
         }
      } else {
         double dy = crystal.getY() - mc.player.getY();
         if (dy > 3.0 && this.distHoriz(crystal) < 3.5 && this.firstBlockItem() != null) {
            this.towerUp();
         } else {
            this.attackEntity(crystal);
         }
      }
   }

   private void towerUp() {
      Item block = this.firstBlockItem();
      if (block != null) {
         this.selectItem(block);
         mc.player.setSprinting(false);
         mc.player.setDeltaMovement(0.0, mc.player.getDeltaMovement().y, 0.0);
         double vy = mc.player.getDeltaMovement().y;
         if (mc.player.onGround()) {
            mc.player.setDeltaMovement(0.0, 0.42, 0.0);
         } else if (vy > 0.0 && vy < 0.24) {
            OyVey.rotationManager.setPlayerRotations(mc.player.getYRot(), 90.0F);
            this.placeAt(block, mc.player.blockPosition().below());
         }
      }
   }

   private Item firstBlockItem() {
      for (Item it : new Item[]{Items.COBBLESTONE, Items.END_STONE, Items.OBSIDIAN, Items.NETHERRACK, Items.DIRT}) {
         if (this.hasItem(it)) {
            return it;
         }
      }

      return null;
   }

   private double distHoriz(Entity e) {
      return Math.hypot(e.getX() - mc.player.getX(), e.getZ() - mc.player.getZ());
   }

   private int invSlot(int i) {
      if (mc.player.containerMenu instanceof CraftingMenu) {
         return i < 9 ? 37 + i : i + 1;
      } else {
         return i < 9 ? 36 + i : i;
      }
   }

   private void enq(Runnable r) {
      this.craftActions.addLast(r);
   }

   private void clickMenu(int slot, int btn, ContainerInput type) {
      mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slot, btn, type, mc.player);
   }

   private int findInvIdx(Predicate<ItemStack> pred) {
      for (int i = 9; i <= 35; i++) {
         if (pred.test(mc.player.getInventory().getItem(i))) {
            return i;
         }
      }

      for (int ix = 0; ix < 9; ix++) {
         if (pred.test(mc.player.getInventory().getItem(ix))) {
            return ix;
         }
      }

      return -1;
   }

   private int emptyInvIdx() {
      for (int i = 9; i <= 35; i++) {
         if (mc.player.getInventory().getItem(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   private void enqReturnCursor() {
      this.enq(() -> {
         if (!mc.player.containerMenu.getCarried().isEmpty()) {
            int e = this.emptyInvIdx();
            if (e != -1) {
               this.clickMenu(this.invSlot(e), 0, ContainerInput.PICKUP);
            }
         }
      });
   }

   private void enqClearGrid(int... gridSlots) {
      for (int s : gridSlots) {
         this.enq(() -> {
            if (!mc.player.containerMenu.getSlot(s).getItem().isEmpty()) {
               this.clickMenu(s, 0, ContainerInput.QUICK_MOVE);
            }
         });
      }
   }

   private void schedulePlanksFromLogs() {
      this.enq(() -> {
         int idx = this.findInvIdx(s -> s.is(ItemTags.LOGS));
         if (idx != -1) {
            this.clickMenu(this.invSlot(idx), 0, ContainerInput.PICKUP);
         }
      });
      this.enq(() -> this.clickMenu(1, 0, ContainerInput.PICKUP));
      this.enq(() -> this.clickMenu(0, 0, ContainerInput.QUICK_MOVE));
      this.enqClearGrid(1);
      this.enqReturnCursor();
   }

   private void scheduleCraftTable() {
      this.enq(() -> {
         int idx = this.findInvIdx(sx -> sx.is(ItemTags.PLANKS));
         if (idx != -1) {
            this.clickMenu(this.invSlot(idx), 0, ContainerInput.PICKUP);
         }
      });

      for (int s = 1; s <= 4; s++) {
         int slot = s;
         this.enq(() -> this.clickMenu(slot, 1, ContainerInput.PICKUP));
      }

      this.enqReturnCursor();
      this.enq(() -> this.clickMenu(0, 0, ContainerInput.QUICK_MOVE));
      this.enqClearGrid(1, 2, 3, 4);
   }

   private void scheduleSticks3x3() {
      this.enq(() -> {
         int idx = this.findInvIdx(s -> s.is(ItemTags.PLANKS));
         if (idx != -1) {
            this.clickMenu(this.invSlot(idx), 0, ContainerInput.PICKUP);
         }
      });
      this.enq(() -> this.clickMenu(1, 1, ContainerInput.PICKUP));
      this.enq(() -> this.clickMenu(4, 1, ContainerInput.PICKUP));
      this.enqReturnCursor();
      this.enq(() -> this.clickMenu(0, 0, ContainerInput.QUICK_MOVE));
      this.enqClearGrid(1, 4);
   }

   private void scheduleWoodenPickaxe() {
      this.enq(() -> {
         int idx = this.findInvIdx(s -> s.is(ItemTags.PLANKS));
         if (idx != -1) {
            this.clickMenu(this.invSlot(idx), 0, ContainerInput.PICKUP);
         }
      });
      this.enq(() -> this.clickMenu(1, 1, ContainerInput.PICKUP));
      this.enq(() -> this.clickMenu(2, 1, ContainerInput.PICKUP));
      this.enq(() -> this.clickMenu(3, 1, ContainerInput.PICKUP));
      this.enqReturnCursor();
      this.enq(() -> {
         int idx = this.findInvIdx(s -> s.is(Items.STICK));
         if (idx != -1) {
            this.clickMenu(this.invSlot(idx), 0, ContainerInput.PICKUP);
         }
      });
      this.enq(() -> this.clickMenu(5, 1, ContainerInput.PICKUP));
      this.enq(() -> this.clickMenu(8, 1, ContainerInput.PICKUP));
      this.enqReturnCursor();
      this.enq(() -> this.clickMenu(0, 0, ContainerInput.QUICK_MOVE));
      this.enqClearGrid(1, 2, 3, 4, 5, 6, 7, 8, 9);
      this.enq(() -> mc.player.closeContainer());
   }

   private void scheduleStonePickaxe() {
      this.enq(() -> {
         int idx = this.findInvIdx(s -> s.is(Items.COBBLESTONE) || s.is(Items.STONE));
         if (idx != -1) {
            this.clickMenu(this.invSlot(idx), 0, ContainerInput.PICKUP);
         }
      });
      this.enq(() -> this.clickMenu(1, 1, ContainerInput.PICKUP));
      this.enq(() -> this.clickMenu(2, 1, ContainerInput.PICKUP));
      this.enq(() -> this.clickMenu(3, 1, ContainerInput.PICKUP));
      this.enqReturnCursor();
      this.enqEnsureSticks();
      this.enq(() -> this.clickMenu(5, 1, ContainerInput.PICKUP));
      this.enq(() -> this.clickMenu(8, 1, ContainerInput.PICKUP));
      this.enqReturnCursor();
      this.enq(() -> this.clickMenu(0, 0, ContainerInput.QUICK_MOVE));
      this.enqClearGrid(1, 2, 3, 4, 5, 6, 7, 8, 9);
   }

   private void scheduleStoneSword() {
      this.enq(() -> {
         int idx = this.findInvIdx(s -> s.is(Items.COBBLESTONE) || s.is(Items.STONE));
         if (idx != -1) {
            this.clickMenu(this.invSlot(idx), 0, ContainerInput.PICKUP);
         }
      });
      this.enq(() -> this.clickMenu(2, 1, ContainerInput.PICKUP));
      this.enq(() -> this.clickMenu(5, 1, ContainerInput.PICKUP));
      this.enqReturnCursor();
      this.enqEnsureSticks();
      this.enq(() -> this.clickMenu(8, 1, ContainerInput.PICKUP));
      this.enqReturnCursor();
      this.enq(() -> this.clickMenu(0, 0, ContainerInput.QUICK_MOVE));
      this.enqClearGrid(1, 2, 3, 4, 5, 6, 7, 8, 9);
   }

   private void scheduleFurnace() {
      this.enq(() -> {
         int idx = this.findInvIdx(sx -> sx.is(Items.COBBLESTONE) || sx.is(Items.STONE));
         if (idx != -1) {
            this.clickMenu(this.invSlot(idx), 0, ContainerInput.PICKUP);
         }
      });

      for (int s : new int[]{1, 2, 3, 4, 6, 7, 8, 9}) {
         int slot = s;
         this.enq(() -> this.clickMenu(slot, 1, ContainerInput.PICKUP));
      }

      this.enqReturnCursor();
      this.enq(() -> this.clickMenu(0, 0, ContainerInput.QUICK_MOVE));
      this.enqClearGrid(1, 2, 3, 4, 6, 7, 8, 9);
   }

   private void enqEnsureSticks() {
      this.enq(() -> {
         if (this.countItem(Items.STICK) < 2) {
            int idx = this.findInvIdx(s -> s.is(ItemTags.PLANKS));
            if (idx == -1) {
               return;
            }

            this.clickMenu(this.invSlot(idx), 0, ContainerInput.PICKUP);
            this.clickMenu(2, 1, ContainerInput.PICKUP);
            this.clickMenu(5, 1, ContainerInput.PICKUP);
         }
      });
      this.enq(() -> {
         if (!mc.player.containerMenu.getSlot(0).getItem().isEmpty()) {
            this.clickMenu(0, 0, ContainerInput.QUICK_MOVE);
         }

         this.enqReturnCursor();
      });
      this.enq(() -> {
         int idx = this.findInvIdx(s -> s.is(Items.STICK));
         if (idx != -1) {
            this.clickMenu(this.invSlot(idx), 0, ContainerInput.PICKUP);
         }
      });
   }

   private void scheduleIronPickaxe() {
      this.scheduleHeadPickaxe(s -> s.is(Items.IRON_INGOT));
   }

   private void scheduleDiamondPickaxe() {
      this.scheduleHeadPickaxe(s -> s.is(Items.DIAMOND));
   }

   private void scheduleHeadPickaxe(Predicate<ItemStack> head) {
      this.enq(() -> {
         int idx = this.findInvIdx(head);
         if (idx != -1) {
            this.clickMenu(this.invSlot(idx), 0, ContainerInput.PICKUP);
         }
      });
      this.enq(() -> this.clickMenu(1, 1, ContainerInput.PICKUP));
      this.enq(() -> this.clickMenu(2, 1, ContainerInput.PICKUP));
      this.enq(() -> this.clickMenu(3, 1, ContainerInput.PICKUP));
      this.enqReturnCursor();
      this.enqEnsureSticks();
      this.enq(() -> this.clickMenu(5, 1, ContainerInput.PICKUP));
      this.enq(() -> this.clickMenu(8, 1, ContainerInput.PICKUP));
      this.enqReturnCursor();
      this.enq(() -> this.clickMenu(0, 0, ContainerInput.QUICK_MOVE));
      this.enqClearGrid(1, 2, 3, 4, 5, 6, 7, 8, 9);
   }

   private void scheduleFlintAndSteel() {
      this.enq(() -> {
         int idx = this.findInvIdx(s -> s.is(Items.IRON_INGOT));
         if (idx != -1) {
            this.clickMenu(this.invSlot(idx), 0, ContainerInput.PICKUP);
         }
      });
      this.enq(() -> this.clickMenu(1, 1, ContainerInput.PICKUP));
      this.enqReturnCursor();
      this.enq(() -> {
         int idx = this.findInvIdx(s -> s.is(Items.FLINT));
         if (idx != -1) {
            this.clickMenu(this.invSlot(idx), 0, ContainerInput.PICKUP);
         }
      });
      this.enq(() -> this.clickMenu(5, 1, ContainerInput.PICKUP));
      this.enqReturnCursor();
      this.enq(() -> this.clickMenu(0, 0, ContainerInput.QUICK_MOVE));
      this.enqClearGrid(1, 2, 3, 4, 5, 6, 7, 8, 9);
   }

   private void scheduleBlazePowder() {
      this.enq(() -> {
         int idx = this.findInvIdx(s -> s.is(Items.BLAZE_ROD));
         if (idx != -1) {
            this.clickMenu(this.invSlot(idx), 0, ContainerInput.PICKUP);
         }
      });
      this.enq(() -> this.clickMenu(1, 1, ContainerInput.PICKUP));
      this.enqReturnCursor();
      this.enq(() -> this.clickMenu(0, 0, ContainerInput.QUICK_MOVE));
      this.enqClearGrid(1);
   }

   private void scheduleEnderEye() {
      this.enq(() -> {
         int idx = this.findInvIdx(s -> s.is(Items.BLAZE_POWDER));
         if (idx != -1) {
            this.clickMenu(this.invSlot(idx), 0, ContainerInput.PICKUP);
         }
      });
      this.enq(() -> this.clickMenu(1, 1, ContainerInput.PICKUP));
      this.enqReturnCursor();
      this.enq(() -> {
         int idx = this.findInvIdx(s -> s.is(Items.ENDER_PEARL));
         if (idx != -1) {
            this.clickMenu(this.invSlot(idx), 0, ContainerInput.PICKUP);
         }
      });
      this.enq(() -> this.clickMenu(2, 1, ContainerInput.PICKUP));
      this.enqReturnCursor();
      this.enq(() -> this.clickMenu(0, 0, ContainerInput.QUICK_MOVE));
      this.enqClearGrid(1, 2);
   }

   private void pathTo(BlockPos goal) {
      long now = System.currentTimeMillis();
      boolean stale = now - this.lastPathCalc > 3000L;
      boolean done = this.path == null || this.pathIndex >= this.path.size();
      boolean moved = !goal.equals(this.lastGoal);
      if (moved || stale || done) {
         this.path = this.computePath(mc.player.blockPosition(), goal);
         this.pathIndex = 0;
         this.lastGoal = goal;
         this.lastPathCalc = now;
      }

      if (this.path != null && this.pathIndex < this.path.size()) {
         this.followPath();
      } else {
         this.directMove(goal);
      }
   }

   private List<BlockPos> computePath(BlockPos start, BlockPos goal) {
      HashMap<Long, Integer> gScore = new HashMap<>();
      HashMap<Long, Long> cameFrom = new HashMap<>();
      PriorityQueue<long[]> open = new PriorityQueue<>(Comparator.comparingInt(a -> (int)a[1]));
      long startKey = start.asLong();
      gScore.put(startKey, 0);
      open.offer(new long[]{startKey, this.heuristic(start, goal)});
      int explored = 0;

      while (!open.isEmpty() && explored++ < 2000) {
         long[] entry = open.poll();
         long curKey = entry[0];
         BlockPos cur = BlockPos.of(curKey);
         if (cur.distSqr(goal) <= 4.0) {
            return this.reconstruct(cameFrom, curKey, startKey);
         }

         int curG = gScore.getOrDefault(curKey, 2147483647);

         for (BlockPos nb : this.neighbors(cur)) {
            long nbKey = nb.asLong();
            int ng = curG + 1;
            if (ng < gScore.getOrDefault(nbKey, 2147483647)) {
               gScore.put(nbKey, ng);
               cameFrom.put(nbKey, curKey);
               open.offer(new long[]{nbKey, ng + this.heuristic(nb, goal)});
            }
         }
      }

      return null;
   }

   private List<BlockPos> reconstruct(HashMap<Long, Long> came, long end, long start) {
      LinkedList<BlockPos> out = new LinkedList<>();
      long cur = end;

      while (cur != start && came.containsKey(cur)) {
         out.addFirst(BlockPos.of(cur));
         cur = came.get(cur);
      }

      out.addFirst(BlockPos.of(start));
      return out;
   }

   private int heuristic(BlockPos a, BlockPos b) {
      return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
   }

   private List<BlockPos> neighbors(BlockPos p) {
      List<BlockPos> out = new ArrayList<>(6);
      int[] dx = new int[]{1, -1, 0, 0};
      int[] dz = new int[]{0, 0, 1, -1};

      for (int i = 0; i < 4; i++) {
         BlockPos flat = p.offset(dx[i], 0, dz[i]);
         BlockPos up = p.offset(dx[i], 1, dz[i]);
         BlockPos down = p.offset(dx[i], -1, dz[i]);
         if (this.passable(flat) && this.passable(flat.above()) && this.solid(flat.below())) {
            out.add(flat);
         } else if (this.solid(flat) && this.passable(up) && this.passable(up.above()) && this.passable(p.above())) {
            out.add(up);
         }

         if (this.passable(down) && this.passable(down.above()) && this.solid(down.below())) {
            out.add(down);
         }
      }

      if (this.passable(p.below()) && this.passable(p.below().below())) {
         out.add(p.below());
      }

      return out;
   }

   private void followPath() {
      while (this.pathIndex < this.path.size() - 1) {
         BlockPos wp = this.path.get(this.pathIndex);
         double hDist = Math.hypot(wp.getX() + 0.5 - mc.player.getX(), wp.getZ() + 0.5 - mc.player.getZ());
         if (hDist < 0.5 && Math.abs(wp.getY() - mc.player.blockPosition().getY()) <= 1) {
            this.pathIndex++;
            continue;
         }
         break;
      }

      BlockPos wp = this.path.get(this.pathIndex);
      double dx = wp.getX() + 0.5 - mc.player.getX();
      double dz = wp.getZ() + 0.5 - mc.player.getZ();
      double hDist = Math.hypot(dx, dz);
      int yDiff = wp.getY() - mc.player.blockPosition().getY();
      if (hDist > 0.1) {
         OyVey.rotationManager.setPlayerRotations(RotationUtils.fromRotationVec(dx, 0.0, dz)[0], mc.player.getXRot());
         mc.player.setSprinting(hDist > 1.5);
         mc.player.setDeltaMovement(dx / hDist * 0.28, mc.player.getDeltaMovement().y, dz / hDist * 0.28);
      }

      if (yDiff > 0 && mc.player.onGround()) {
         mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.42, mc.player.getDeltaMovement().z);
      }
   }

   private void directMove(BlockPos goal) {
      double dx = goal.getX() + 0.5 - mc.player.getX();
      double dz = goal.getZ() + 0.5 - mc.player.getZ();
      double len = Math.hypot(dx, dz);
      if (len < 0.6) {
         mc.player.setSprinting(false);
      } else {
         OyVey.rotationManager.setPlayerRotations(RotationUtils.fromRotationVec(dx, 0.0, dz)[0], mc.player.getXRot());
         mc.player.setSprinting(true);
         mc.player.setDeltaMovement(dx / len * 0.28, mc.player.getDeltaMovement().y, dz / len * 0.28);
         if (mc.player.horizontalCollision && mc.player.onGround()) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.42, mc.player.getDeltaMovement().z);
         }
      }
   }

   private void navigateAndBreak(BlockPos target) {
      if (mc.level.getBlockState(target).isAir()) {
         this.stopMining();
         this.seekTarget = null;
      } else {
         BlockPos feet = mc.player.blockPosition();
         BlockPos below = feet.below();
         if (target.equals(below) || target.equals(feet)) {
            boolean safeBelow = this.solid(target.below());
            if (!safeBelow) {
               this.seekTarget = null;
               return;
            }
         }

         if (this.distSq(target) > 18.0) {
            this.stopMining();
            this.pathTo(target);
         } else {
            mc.player.setSprinting(false);
            this.path = null;
            double dx = target.getX() + 0.5 - mc.player.getX();
            double dy = target.getY() + 0.5 - (mc.player.getY() + mc.player.getEyeHeight());
            double dz = target.getZ() + 0.5 - mc.player.getZ();
            float[] rot = RotationUtils.fromRotationVec(dx, dy, dz);
            OyVey.rotationManager.setPlayerRotations(rot[0], rot[1]);
            Direction face = this.nearestFace(target);
            if (!target.equals(this.breakTarget)) {
               this.stopMining();
               mc.gameMode.startDestroyBlock(target, face);
               this.breakTarget = target;
               this.miningActive = true;
            } else {
               mc.gameMode.continueDestroyBlock(target, face);
            }
         }
      }
   }

   private void stopMining() {
      if (this.miningActive) {
         mc.gameMode.stopDestroyBlock();
         this.miningActive = false;
      }

      this.breakTarget = null;
   }

   private void attackEntity(Entity e) {
      double dx = e.getX() - mc.player.getX();
      double dy = e.getY() + e.getEyeHeight() * 0.5 - (mc.player.getY() + mc.player.getEyeHeight());
      double dz = e.getZ() - mc.player.getZ();
      float[] rot = RotationUtils.fromRotationVec(dx, dy, dz);
      OyVey.rotationManager.setPlayerRotations(rot[0], rot[1]);
      if (mc.player.distanceTo(e) > 3.5) {
         this.pathTo(e.blockPosition());
      } else {
         mc.player.setSprinting(false);
         long now = System.currentTimeMillis();
         if (now - this.lastSwing >= 200L && mc.player.getAttackStrengthScale(0.0F) >= 0.9F) {
            mc.gameMode.attack(mc.player, e);
            mc.player.swing(InteractionHand.MAIN_HAND);
            this.lastSwing = now;
         }
      }
   }

   private void wander() {
      if (++this.wanderTick > 100) {
         this.wanderTick = 0;
         this.wanderDir = (this.wanderDir + 1) % 4;
      }
      BlockPos dest = switch (this.wanderDir) {
         case 0 -> mc.player.blockPosition().offset(100, 0, 0);
         case 1 -> mc.player.blockPosition().offset(0, 0, 100);
         case 2 -> mc.player.blockPosition().offset(-100, 0, 0);
         default -> mc.player.blockPosition().offset(0, 0, -100);
      };
      this.pathTo(dest);
   }

   private boolean passable(BlockPos p) {
      return !mc.level.isLoaded(p) ? false : mc.level.getBlockState(p).getCollisionShape(mc.level, p).isEmpty();
   }

   private boolean solid(BlockPos p) {
      return !mc.level.isLoaded(p) ? false : !mc.level.getBlockState(p).getCollisionShape(mc.level, p).isEmpty();
   }

   private BlockPos findBlock(int radius, Predicate<BlockState> pred) {
      BlockPos origin = mc.player.blockPosition();
      BlockPos best = null;
      double bestD = 1.7976931348623157E308;
      int halfY = Math.max(4, radius / 4);

      for (int x = -radius; x <= radius; x++) {
         for (int y = -halfY; y <= halfY; y++) {
            for (int z = -radius; z <= radius; z++) {
               BlockPos p = origin.offset(x, y, z);
               if (mc.level.isLoaded(p) && pred.test(mc.level.getBlockState(p))) {
                  double d = mc.player.distanceToSqr(Vec3.atCenterOf(p));
                  if (d < bestD) {
                     bestD = d;
                     best = p;
                  }
               }
            }
         }
      }

      return best;
   }

   private Direction nearestFace(BlockPos pos) {
      Vec3 e = mc.player.getEyePosition();
      Vec3 c = Vec3.atCenterOf(pos);
      double ax = Math.abs(e.x - c.x);
      double ay = Math.abs(e.y - c.y);
      double az = Math.abs(e.z - c.z);
      if (ay > ax && ay > az) {
         return e.y > c.y ? Direction.UP : Direction.DOWN;
      } else if (ax > az) {
         return e.x > c.x ? Direction.WEST : Direction.EAST;
      } else {
         return e.z > c.z ? Direction.NORTH : Direction.SOUTH;
      }
   }

   private void useBlock(BlockPos pos) {
      Direction face = this.nearestFace(pos);
      mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
   }

   private void placeBlock(Item item, BlockPos target) {
      this.selectItem(item);
      this.useBlock(target.below());
   }

   private double distSq(BlockPos pos) {
      return mc.player.distanceToSqr(Vec3.atCenterOf(pos));
   }

   private int countItem(Item item) {
      int n = 0;

      for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
         ItemStack s = mc.player.getInventory().getItem(i);
         if (s.getItem() == item) {
            n += s.getCount();
         }
      }

      return n;
   }

   private boolean hasItem(Item item) {
      return this.countItem(item) > 0;
   }

   private int countPlanks() {
      int n = 0;

      for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
         ItemStack s = mc.player.getInventory().getItem(i);
         if (s.is(ItemTags.PLANKS)) {
            n += s.getCount();
         }
      }

      return n;
   }

   private int countLogs() {
      int n = 0;

      for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
         ItemStack s = mc.player.getInventory().getItem(i);
         if (s.is(ItemTags.LOGS)) {
            n += s.getCount();
         }
      }

      return n;
   }

   private int countCobble() {
      return this.countItem(Items.COBBLESTONE) + this.countItem(Items.STONE);
   }

   private void equipBestPickaxe() {
      for (Item p : new Item[]{Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE, Items.IRON_PICKAXE, Items.STONE_PICKAXE, Items.WOODEN_PICKAXE}) {
         int s = this.hotbarSlot(p);
         if (s != -1) {
            mc.player.getInventory().setSelectedSlot(s);
            return;
         }
      }
   }

   private void selectItem(Item item) {
      int s = this.hotbarSlot(item);
      if (s != -1) {
         mc.player.getInventory().setSelectedSlot(s);
      }
   }

   private int hotbarSlot(Item item) {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getItem(i).getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private <T extends Entity> Entity nearest(Class<T> type) {
      return StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), false)
         .filter(e -> type.isInstance(e) && e.isAlive() && e != mc.player)
         .min(Comparator.comparingDouble(e -> mc.player.distanceTo(e)))
         .orElse(null);
   }

   @Override
   public String getDisplayInfo() {
      if (this.phase == BeatMinecraft.Phase.GATHER_WOOD) {
         int need = this.logsNeeded();
         return need > 0 ? "Need " + need + " logs" : this.phase.label;
      } else {
         return this.phase.label;
      }
   }

   private enum Phase {
      GATHER_WOOD("Getting wood"),
      CRAFT_BASICS("Crafting basics"),
      GATHER_STONE("Mining stone"),
      CRAFT_TOOLS("Crafting tools"),
      GATHER_IRON("Mining iron"),
      SMELT_IRON("Smelting iron"),
      GATHER_FLINT("Digging flint"),
      CRAFT_IRON_GEAR("Crafting iron gear"),
      GATHER_DIAMOND("Mining diamonds"),
      CRAFT_DIAMOND("Crafting diamond pick"),
      GATHER_OBSIDIAN("Mining obsidian"),
      BUILD_PORTAL("Building portal"),
      ENTER_NETHER("Entering nether"),
      FIND_FORTRESS("Finding fortress"),
      KILL_BLAZES("Killing blazes"),
      GATHER_PEARLS("Getting pearls"),
      EXIT_NETHER("Leaving nether"),
      CRAFT_EYES("Crafting eyes"),
      FIND_STRONGHOLD("Finding stronghold"),
      KILL_DRAGON("Killing dragon"),
      DONE("GG!");

      final String label;

      Phase(String l) {
         this.label = l;
      }
   }
}
