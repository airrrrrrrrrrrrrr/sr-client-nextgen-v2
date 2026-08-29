package me.alpha432.oyvey.mixin.world;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.features.modules.movement.Jesus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlock.class)
public class MixinLiquidBlock {
   @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true, require = 0)
   private void oyvey$solidWater(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx, CallbackInfoReturnable<VoxelShape> cir) {
      if (OyVey.moduleManager != null) {
         Jesus jesus = OyVey.moduleManager.getModuleByClass(Jesus.class);
         if (jesus != null && jesus.isEnabled() && jesus.mode.getValue() == Jesus.Mode.SOLID && state.getFluidState().is(Fluids.WATER)) {
            cir.setReturnValue(Shapes.block());
         }
      }
   }
}
