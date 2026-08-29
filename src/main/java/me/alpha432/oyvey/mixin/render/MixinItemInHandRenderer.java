package me.alpha432.oyvey.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.alpha432.oyvey.features.modules.render.AnimationsModule;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {
   @Inject(
      method = "renderArmWithItem",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = Shift.AFTER),
      require = 0
   )
   private void oyvey$applyHandTransforms(
      AbstractClientPlayer player,
      float tickProgress,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      PoseStack matrices,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo ci
   ) {
      AnimationsModule anim = AnimationsModule.INSTANCE;
      if (anim != null && anim.isEnabled()) {
         boolean isMainHand = hand == InteractionHand.MAIN_HAND;
         boolean isBothHands = isMainHand && item != null && item.has(DataComponents.MAP_ID);
         boolean mhOn = anim.mainHandEnabled.getValue();
         boolean ohOn = anim.offHandEnabled.getValue();
         if (isBothHands && mhOn && ohOn) {
            AnimationsModule.applyHandTransform(
               matrices,
               (anim.mainHandX.getValue() + anim.offHandX.getValue()) / 2.0F,
               (anim.mainHandY.getValue() + anim.offHandY.getValue()) / 2.0F,
               (anim.mainHandScale.getValue() + anim.offHandScale.getValue()) / 2.0F,
               (anim.mainHandRotX.getValue() + anim.offHandRotX.getValue()) / 2.0F,
               (anim.mainHandRotY.getValue() + anim.offHandRotY.getValue()) / 2.0F,
               (anim.mainHandRotZ.getValue() + anim.offHandRotZ.getValue()) / 2.0F
            );
         } else if (isMainHand && mhOn) {
            AnimationsModule.applyHandTransform(
               matrices,
               anim.mainHandX.getValue(),
               anim.mainHandY.getValue(),
               anim.mainHandScale.getValue(),
               anim.mainHandRotX.getValue(),
               anim.mainHandRotY.getValue(),
               anim.mainHandRotZ.getValue()
            );
         } else if (!isMainHand && ohOn) {
            AnimationsModule.applyHandTransform(
               matrices,
               anim.offHandX.getValue(),
               anim.offHandY.getValue(),
               anim.offHandScale.getValue(),
               anim.offHandRotX.getValue(),
               anim.offHandRotY.getValue(),
               anim.offHandRotZ.getValue()
            );
         }
      }
   }

   @Inject(
      method = "renderArmWithItem",
      slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/ItemUseAnimation;")),
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
         ordinal = 0,
         shift = Shift.AFTER
      ),
      require = 0
   )
   private void oyvey$applyBlockAnimation(
      AbstractClientPlayer player,
      float tickProgress,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      PoseStack matrices,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo ci
   ) {
      AnimationsModule anim = AnimationsModule.INSTANCE;
      if (item != null && anim != null && anim.isEnabled() && item.is(ItemTags.SWORDS)) {
         HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
         AnimationsModule.transformBlockAnimation(matrices, arm, equipProgress, swingProgress);
      }
   }

   @ModifyArg(
      method = "renderArmWithItem",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
         ordinal = 3
      ),
      index = 2,
      require = 0
   )
   private float oyvey$freezeEquipBlocking(float equipProgress) {
      AnimationsModule anim = AnimationsModule.INSTANCE;
      return anim != null && anim.isEnabled() && anim.equipIgnoreBlocking.getValue() ? 0.0F : equipProgress;
   }

   @Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true, require = 0)
   private void oyvey$cancelEquipPlace(InteractionHand hand, CallbackInfo ci) {
      AnimationsModule anim = AnimationsModule.INSTANCE;
      if (anim != null && anim.isEnabled() && anim.equipIgnorePlace.getValue()) {
         ci.cancel();
      }
   }

   @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("RETURN"), cancellable = true, require = 0)
   private void oyvey$ignoreAmount(ItemStack from, ItemStack to, CallbackInfoReturnable<Boolean> cir) {
      AnimationsModule anim = AnimationsModule.INSTANCE;
      if (anim != null && anim.isEnabled() && anim.equipIgnoreAmount.getValue() && !cir.getReturnValueZ() && ItemStack.isSameItemSameComponents(from, to)) {
         cir.setReturnValue(true);
      }
   }

   @ModifyArg(
      method = "renderHandsWithItems",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
         ordinal = 0
      ),
      index = 4,
      require = 0
   )
   private float oyvey$snapSwing(float swingProgress) {
      AnimationsModule anim = AnimationsModule.INSTANCE;
      return anim != null && anim.isEnabled() ? (float)Math.sqrt(swingProgress) : swingProgress;
   }
}
