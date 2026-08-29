package me.alpha432.oyvey.util.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Optional;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.mixin.render.accessor.EntityRenderDispatcherAccessor;
import me.alpha432.oyvey.mixin.render.accessor.RenderSetupAccessor;
import me.alpha432.oyvey.mixin.render.accessor.RenderTypeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector.ParticleGroupRenderer;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState.LeashState;
import net.minecraft.client.renderer.entity.state.EntityRenderState.ShadowPiece;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.ClientAsset.Texture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

final class SyntheticHiddenPlayerRenderer {
   private static final int FULL_BRIGHT_LIGHT = 15728880;
   private static final float PLAYER_RENDER_SCALE = 0.9375F;
   private static final float PLAYER_Y_OFFSET = -1.501F;
   private static final int DEBUG_BODY_TINT = -49088;
   private static final int DEBUG_CAPE_TINT = -12558081;
   private static final SyntheticHiddenPlayerRenderer.DebugOptions DEBUG = SyntheticHiddenPlayerRenderer.DebugOptions.defaults();
   private static long lastDebugFrameBucket = -9223372036854775808L;

   void renderHidden(
      AvatarRenderState state, CameraRenderState cameraRenderState, double x, double y, double z, PoseStack poseStack, SubmitNodeCollector collector
   ) {
      SyntheticHiddenPlayerRenderer.SyntheticResources resources = SyntheticHiddenPlayerRenderer.SyntheticResources.create();
      SyntheticHiddenPlayerRenderer.SyntheticModels models = SyntheticHiddenPlayerRenderer.SyntheticModels.create(
         resources.context(), state.skin.model() == PlayerModelType.SLIM
      );
      int overlayCoords = OverlayTexture.pack(0.0F, state.hasRedOverlay);
      int tintedColor = -1;
      ChamsCompositeTarget.markUsed();
      poseStack.pushPose();
      this.applyEntityTransform(state, poseStack, x, y, z);
      SyntheticHiddenPlayerRenderer.DebugTrace trace = SyntheticHiddenPlayerRenderer.DebugTrace.begin(state);
      this.renderPass(
         state,
         models,
         resources.equipmentRenderer(),
         poseStack,
         new SyntheticHiddenPlayerRenderer.HiddenSubmitCollector(collector, SyntheticHiddenPlayerRenderer.Pass.DEPTH, 0),
         SyntheticHiddenPlayerRenderer.Pass.DEPTH,
         overlayCoords,
         tintedColor,
         trace
      );
      this.renderPass(
         state,
         models,
         resources.equipmentRenderer(),
         poseStack,
         new SyntheticHiddenPlayerRenderer.HiddenSubmitCollector(collector, SyntheticHiddenPlayerRenderer.Pass.COLOR, 1),
         SyntheticHiddenPlayerRenderer.Pass.COLOR,
         overlayCoords,
         tintedColor,
         trace
      );
      trace.finish();
      poseStack.popPose();
   }

   private void renderPass(
      AvatarRenderState state,
      SyntheticHiddenPlayerRenderer.SyntheticModels models,
      EquipmentLayerRenderer equipmentRenderer,
      PoseStack basePose,
      SyntheticHiddenPlayerRenderer.HiddenSubmitCollector collector,
      SyntheticHiddenPlayerRenderer.Pass pass,
      int overlayCoords,
      int tintedColor,
      SyntheticHiddenPlayerRenderer.DebugTrace trace
   ) {
      this.submitBody(state, models.bodyModel(), basePose, collector, pass, overlayCoords, tintedColor, trace);
      this.submitOuterSkin(state, models.outerModel(), basePose, collector, pass, overlayCoords, tintedColor, trace);
      this.submitArmor(state, models.armorModels(), equipmentRenderer, basePose, collector, pass, trace);
      this.submitCape(state, models.capeModel(), basePose, collector, pass, overlayCoords, tintedColor, trace);
   }

   private void submitBody(
      AvatarRenderState state,
      SyntheticHiddenPlayerRenderer.BodyPlayerModel model,
      PoseStack basePose,
      SyntheticHiddenPlayerRenderer.HiddenSubmitCollector collector,
      SyntheticHiddenPlayerRenderer.Pass pass,
      int overlayCoords,
      int tintedColor,
      SyntheticHiddenPlayerRenderer.DebugTrace trace
   ) {
      if (!DEBUG.shouldRenderBody(pass)) {
         trace.logSkipped("BODY", pass, "toggle_disabled");
      } else {
         RenderType renderType = model.renderType(texturePath(state.skin.body()));
         trace.logSubmit("BODY", pass, renderType, mapRenderType(pass, model, renderType));
         collector.submitModel(
            model, state, copyPoseStack(basePose), renderType, 15728880, overlayCoords, DEBUG.bodyTint(tintedColor), null, state.outlineColor, null
         );
      }
   }

   private void submitOuterSkin(
      AvatarRenderState state,
      PlayerModel outerModel,
      PoseStack basePose,
      SyntheticHiddenPlayerRenderer.HiddenSubmitCollector collector,
      SyntheticHiddenPlayerRenderer.Pass pass,
      int overlayCoords,
      int tintedColor,
      SyntheticHiddenPlayerRenderer.DebugTrace trace
   ) {
      if (!DEBUG.renderOuterSkin) {
         trace.logSkipped("OUTER", pass, "toggle_disabled");
      } else {
         RenderType renderType = outerModel.renderType(texturePath(state.skin.body()));
         outerModel.setupAnim(state);
         trace.logSubmit("OUTER", pass, renderType, mapRenderType(pass, renderType));
         this.submitAttachedPart(collector, basePose, outerModel, outerModel.head, outerModel.hat, renderType, overlayCoords, tintedColor, state.outlineColor);
         this.submitAttachedPart(
            collector, basePose, outerModel, outerModel.body, outerModel.jacket, renderType, overlayCoords, tintedColor, state.outlineColor
         );
         this.submitAttachedPart(
            collector, basePose, outerModel, outerModel.leftArm, outerModel.leftSleeve, renderType, overlayCoords, tintedColor, state.outlineColor
         );
         this.submitAttachedPart(
            collector, basePose, outerModel, outerModel.rightArm, outerModel.rightSleeve, renderType, overlayCoords, tintedColor, state.outlineColor
         );
         this.submitAttachedPart(
            collector, basePose, outerModel, outerModel.leftLeg, outerModel.leftPants, renderType, overlayCoords, tintedColor, state.outlineColor
         );
         this.submitAttachedPart(
            collector, basePose, outerModel, outerModel.rightLeg, outerModel.rightPants, renderType, overlayCoords, tintedColor, state.outlineColor
         );
      }
   }

   private void submitArmor(
      AvatarRenderState state,
      ArmorModelSet<SyntheticHiddenPlayerRenderer.SyntheticArmorModel> armorModels,
      EquipmentLayerRenderer equipmentRenderer,
      PoseStack basePose,
      SyntheticHiddenPlayerRenderer.HiddenSubmitCollector collector,
      SyntheticHiddenPlayerRenderer.Pass pass,
      SyntheticHiddenPlayerRenderer.DebugTrace trace
   ) {
      if (!DEBUG.renderArmor) {
         trace.logSkipped("ARMOR", pass, "toggle_disabled");
      } else {
         this.submitArmorPiece(state, armorModels, equipmentRenderer, basePose, collector, state.chestEquipment, EquipmentSlot.CHEST);
         this.submitArmorPiece(state, armorModels, equipmentRenderer, basePose, collector, state.legsEquipment, EquipmentSlot.LEGS);
         this.submitArmorPiece(state, armorModels, equipmentRenderer, basePose, collector, state.feetEquipment, EquipmentSlot.FEET);
         this.submitArmorPiece(state, armorModels, equipmentRenderer, basePose, collector, state.headEquipment, EquipmentSlot.HEAD);
         trace.logArmor(pass);
      }
   }

   private void submitArmorPiece(
      AvatarRenderState state,
      ArmorModelSet<SyntheticHiddenPlayerRenderer.SyntheticArmorModel> armorModels,
      EquipmentLayerRenderer equipmentRenderer,
      PoseStack basePose,
      SyntheticHiddenPlayerRenderer.HiddenSubmitCollector collector,
      ItemStack itemStack,
      EquipmentSlot slot
   ) {
      if (HumanoidArmorLayer.shouldRender(itemStack, slot)) {
         Equippable equippable = (Equippable)itemStack.get(DataComponents.EQUIPPABLE);
         if (equippable != null && equippable.slot() == slot) {
            Optional<ResourceKey<EquipmentAsset>> assetId = equippable.assetId();
            if (!assetId.isEmpty()) {
               SyntheticHiddenPlayerRenderer.SyntheticArmorModel armorModel = (SyntheticHiddenPlayerRenderer.SyntheticArmorModel)armorModels.get(slot);
               armorModel.setArmorSlot(slot);
               equipmentRenderer.renderLayers(
                  slot == EquipmentSlot.LEGS ? LayerType.HUMANOID_LEGGINGS : LayerType.HUMANOID,
                  assetId.get(),
                  armorModel,
                  state,
                  itemStack,
                  copyPoseStack(basePose),
                  collector,
                  15728880,
                  state.outlineColor
               );
            }
         }
      }
   }

   private void submitCape(
      AvatarRenderState state,
      PlayerCapeModel capeModel,
      PoseStack basePose,
      SyntheticHiddenPlayerRenderer.HiddenSubmitCollector collector,
      SyntheticHiddenPlayerRenderer.Pass pass,
      int overlayCoords,
      int tintedColor,
      SyntheticHiddenPlayerRenderer.DebugTrace trace
   ) {
      Texture capeTexture = state.skin.cape();
      if (state.isInvisible || !state.showCape || capeTexture == null || this.isWingsChestEquipment(state.chestEquipment)) {
         trace.logSkipped("CAPE", pass, "vanilla_cape_not_rendered");
      } else if (!DEBUG.shouldRenderCape(pass)) {
         trace.logSkipped("CAPE", pass, "toggle_disabled");
      } else {
         PoseStack capePose = copyPoseStack(basePose);
         if (HumanoidArmorLayer.shouldRender(state.chestEquipment, EquipmentSlot.CHEST)) {
            capePose.translate(0.0F, -0.053125F, 0.06875F);
         }

         RenderType renderType = RenderTypes.entitySolid(texturePath(capeTexture));
         trace.logSubmit("CAPE", pass, renderType, mapRenderType(pass, capeModel, renderType));
         collector.submitModel(capeModel, state, capePose, renderType, 15728880, overlayCoords, DEBUG.capeTint(tintedColor), null, state.outlineColor, null);
      }
   }

   private void submitAttachedPart(
      SyntheticHiddenPlayerRenderer.HiddenSubmitCollector collector,
      PoseStack basePose,
      PlayerModel model,
      ModelPart parentPart,
      ModelPart childPart,
      RenderType renderType,
      int overlayCoords,
      int tintedColor,
      int outlineColor
   ) {
      if (childPart.visible) {
         PoseStack poseStack = copyPoseStack(basePose);
         model.root().translateAndRotate(poseStack);
         parentPart.translateAndRotate(poseStack);
         collector.submitModelPart(childPart, poseStack, renderType, 15728880, overlayCoords, null, false, false, tintedColor, null, outlineColor);
      }
   }

   private void applyEntityTransform(AvatarRenderState state, PoseStack poseStack, double x, double y, double z) {
      y += this.getRenderOffsetY(state);
      poseStack.translate(x, y, z);
      if (state.hasPose(Pose.SLEEPING) && state.bedOrientation != null) {
         float eyeHeight = state.eyeHeight - 0.1F;
         poseStack.translate(-state.bedOrientation.getStepX() * eyeHeight, 0.0F, -state.bedOrientation.getStepZ() * eyeHeight);
      }

      float scale = state.scale;
      poseStack.scale(scale, scale, scale);
      this.setupRotations(state, poseStack);
      poseStack.scale(-1.0F, -1.0F, 1.0F);
      poseStack.scale(0.9375F, 0.9375F, 0.9375F);
      poseStack.translate(0.0F, -1.501F, 0.0F);
   }

   private float getRenderOffsetY(AvatarRenderState state) {
      return state.isCrouching ? state.scale * -2.0F / 16.0F : 0.0F;
   }

   private void setupRotations(AvatarRenderState state, PoseStack poseStack) {
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
      if (state.isFallFlying) {
         if (!state.isAutoSpinAttack) {
            poseStack.mulPose(Axis.XP.rotationDegrees(state.fallFlyingScale() * (-90.0F - state.xRot)));
         }

         if (state.shouldApplyFlyingYRot) {
            poseStack.mulPose(Axis.YP.rotation(state.flyingYRot));
         }
      } else if (state.swimAmount > 0.0F) {
         float swimRot = state.isInWater ? Mth.lerp(state.swimAmount, 0.0F, -90.0F - state.xRot) : Mth.lerp(state.swimAmount, 0.0F, -90.0F);
         poseStack.mulPose(Axis.XP.rotationDegrees(swimRot));
         if (state.isVisuallySwimming) {
            poseStack.translate(0.0F, -1.0F, 0.3F);
         }
      } else if (state.isUpsideDown) {
         poseStack.translate(0.0F, state.scale + 0.1F, 0.0F);
         poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
      }
   }

   private boolean isWingsChestEquipment(ItemStack itemStack) {
      return itemStack.is(Items.ELYTRA);
   }

   private static Identifier texturePath(Texture texture) {
      return texture.texturePath();
   }

   private static PoseStack copyPoseStack(PoseStack source) {
      PoseStack copy = new PoseStack();
      copy.last().set(source.last().copy());
      return copy;
   }

   private static RenderType mapRenderType(SyntheticHiddenPlayerRenderer.Pass pass, RenderType renderType) {
      return switch (pass) {
         case DEPTH -> Layers.hiddenPlayerDepth(renderType);
         case COLOR -> Layers.hiddenPlayerColor(renderType);
      };
   }

   private static RenderType mapRenderType(SyntheticHiddenPlayerRenderer.Pass pass, Model<?> model, RenderType renderType) {
      if (pass == SyntheticHiddenPlayerRenderer.Pass.DEPTH) {
         return Layers.hiddenPlayerDepth(renderType);
      } else {
         return model instanceof PlayerCapeModel ? Layers.hiddenPlayerColorClassicCape(renderType) : Layers.hiddenPlayerColor(renderType);
      }
   }

   private static final class BodyPlayerModel extends PlayerModel {
      private BodyPlayerModel(ModelPart modelPart, boolean slim) {
         super(modelPart, slim);
      }

      public void setupAnim(AvatarRenderState state) {
         super.setupAnim(state);
         this.hat.visible = false;
         this.jacket.visible = false;
         this.leftSleeve.visible = false;
         this.rightSleeve.visible = false;
         this.leftPants.visible = false;
         this.rightPants.visible = false;
      }
   }

   private record DebugOptions(
      boolean logEnabled,
      boolean renderBodyDepth,
      boolean renderCapeDepth,
      boolean renderBodyColor,
      boolean renderCapeColor,
      boolean renderOuterSkin,
      boolean renderArmor,
      boolean debugBodyColor,
      boolean debugCapeColor
   ) {
      private static SyntheticHiddenPlayerRenderer.DebugOptions defaults() {
         return new SyntheticHiddenPlayerRenderer.DebugOptions(false, true, true, true, true, true, true, false, false);
      }

      private boolean shouldRenderBody(SyntheticHiddenPlayerRenderer.Pass pass) {
         return pass == SyntheticHiddenPlayerRenderer.Pass.DEPTH ? this.renderBodyDepth : this.renderBodyColor;
      }

      private boolean shouldRenderCape(SyntheticHiddenPlayerRenderer.Pass pass) {
         return pass == SyntheticHiddenPlayerRenderer.Pass.DEPTH ? this.renderCapeDepth : this.renderCapeColor;
      }

      private int bodyTint(int defaultTint) {
         return this.debugBodyColor ? -49088 : defaultTint;
      }

      private int capeTint(int defaultTint) {
         return this.debugCapeColor ? -12558081 : defaultTint;
      }
   }

   private static final class DebugTrace {
      private final boolean active;
      private final String avatarId;
      private boolean bodyDepthLogged;
      private boolean bodyColorLogged;
      private boolean capeDepthLogged;
      private boolean capeColorLogged;
      private boolean outerDepthLogged;
      private boolean outerColorLogged;
      private boolean armorDepthLogged;
      private boolean armorColorLogged;

      private DebugTrace(boolean active, String avatarId) {
         this.active = active;
         this.avatarId = avatarId;
      }

      private static SyntheticHiddenPlayerRenderer.DebugTrace begin(AvatarRenderState state) {
         if (!SyntheticHiddenPlayerRenderer.DEBUG.logEnabled) {
            return new SyntheticHiddenPlayerRenderer.DebugTrace(false, "disabled");
         }

         long frameBucket = System.nanoTime() / 16000000L;
         synchronized (SyntheticHiddenPlayerRenderer.class) {
            if (SyntheticHiddenPlayerRenderer.lastDebugFrameBucket == frameBucket) {
               return new SyntheticHiddenPlayerRenderer.DebugTrace(false, "suppressed");
            }

            SyntheticHiddenPlayerRenderer.lastDebugFrameBucket = frameBucket;
         }

         String avatarId = "avatar@" + Integer.toHexString(System.identityHashCode(state));
         OyVey.LOGGER
            .info(
               "[chams synthetic] {} order=BODY->OUTER->ARMOR->CAPE toggles bodyDepth={} capeDepth={} bodyColor={} capeColor={} outer={} armor={} debugBodyColor={} debugCapeColor={}",
               avatarId,
               SyntheticHiddenPlayerRenderer.DEBUG.renderBodyDepth,
               SyntheticHiddenPlayerRenderer.DEBUG.renderCapeDepth,
               SyntheticHiddenPlayerRenderer.DEBUG.renderBodyColor,
               SyntheticHiddenPlayerRenderer.DEBUG.renderCapeColor,
               SyntheticHiddenPlayerRenderer.DEBUG.renderOuterSkin,
               SyntheticHiddenPlayerRenderer.DEBUG.renderArmor,
               SyntheticHiddenPlayerRenderer.DEBUG.debugBodyColor,
               SyntheticHiddenPlayerRenderer.DEBUG.debugCapeColor
            );
         return new SyntheticHiddenPlayerRenderer.DebugTrace(true, avatarId);
      }

      private void logSubmit(String piece, SyntheticHiddenPlayerRenderer.Pass pass, RenderType originalType, RenderType mappedType) {
         if (this.active && !this.alreadyLogged(piece, pass)) {
            OyVey.LOGGER
               .info(
                  "[chams synthetic] {} {} {} submitted originalType={} originalPipeline={} mappedType={} mappedPipeline={} outputTarget={}",
                  this.avatarId,
                  piece,
                  pass,
                  originalType,
                  originalType.pipeline().getLocation(),
                  mappedType,
                  mappedType.pipeline().getLocation(),
                  describeOutputTarget(mappedType)
               );
         }
      }

      private void logArmor(SyntheticHiddenPlayerRenderer.Pass pass) {
         if (this.active && !this.alreadyLogged("ARMOR", pass)) {
            OyVey.LOGGER
               .info(
                  "[chams synthetic] {} ARMOR {} submitted via EquipmentLayerRenderer outputTarget={} pipelineFamily=collector-mapped-hidden-player",
                  this.avatarId,
                  pass,
                  ChamsCompositeTarget.OUTPUT_TARGET
               );
         }
      }

      private void logSkipped(String piece, SyntheticHiddenPlayerRenderer.Pass pass, String reason) {
         if (this.active && !this.alreadyLogged(piece, pass)) {
            OyVey.LOGGER.info("[chams synthetic] {} {} {} skipped reason={}", this.avatarId, piece, pass, reason);
         }
      }

      private void finish() {
         if (this.active) {
            OyVey.LOGGER.info("[chams synthetic] {} finalCompositeTarget={}", this.avatarId, ChamsCompositeTarget.OUTPUT_TARGET);
         }
      }

      private boolean alreadyLogged(String piece, SyntheticHiddenPlayerRenderer.Pass pass) {
         return switch (piece) {
            case "BODY" -> this.markBody(pass);
            case "CAPE" -> this.markCape(pass);
            case "OUTER" -> this.markOuter(pass);
            case "ARMOR" -> this.markArmor(pass);
            default -> false;
         };
      }

      private boolean markBody(SyntheticHiddenPlayerRenderer.Pass pass) {
         if (pass == SyntheticHiddenPlayerRenderer.Pass.DEPTH) {
            boolean old = this.bodyDepthLogged;
            this.bodyDepthLogged = true;
            return old;
         } else {
            boolean old = this.bodyColorLogged;
            this.bodyColorLogged = true;
            return old;
         }
      }

      private boolean markCape(SyntheticHiddenPlayerRenderer.Pass pass) {
         if (pass == SyntheticHiddenPlayerRenderer.Pass.DEPTH) {
            boolean old = this.capeDepthLogged;
            this.capeDepthLogged = true;
            return old;
         } else {
            boolean old = this.capeColorLogged;
            this.capeColorLogged = true;
            return old;
         }
      }

      private boolean markOuter(SyntheticHiddenPlayerRenderer.Pass pass) {
         if (pass == SyntheticHiddenPlayerRenderer.Pass.DEPTH) {
            boolean old = this.outerDepthLogged;
            this.outerDepthLogged = true;
            return old;
         } else {
            boolean old = this.outerColorLogged;
            this.outerColorLogged = true;
            return old;
         }
      }

      private boolean markArmor(SyntheticHiddenPlayerRenderer.Pass pass) {
         if (pass == SyntheticHiddenPlayerRenderer.Pass.DEPTH) {
            boolean old = this.armorDepthLogged;
            this.armorDepthLogged = true;
            return old;
         } else {
            boolean old = this.armorColorLogged;
            this.armorColorLogged = true;
            return old;
         }
      }

      private static Object describeOutputTarget(RenderType renderType) {
         return ((RenderSetupAccessor)(Object)((RenderTypeAccessor)(Object)renderType).oyvey$getState()).oyvey$getOutputTarget();
      }
   }

   private static final class HiddenOrderedCollector implements OrderedSubmitNodeCollector {
      private final OrderedSubmitNodeCollector delegate;
      private final SyntheticHiddenPlayerRenderer.Pass pass;

      private HiddenOrderedCollector(OrderedSubmitNodeCollector delegate, SyntheticHiddenPlayerRenderer.Pass pass) {
         this.delegate = delegate;
         this.pass = pass;
      }

      public void submitShadow(PoseStack poseStack, float shadowRadius, List<ShadowPiece> shadowPieces) {
      }

      public void submitNameTag(
         PoseStack poseStack,
         Vec3 attachment,
         int verticalOffset,
         Component component,
         boolean visible,
         int lightCoords,
         double distanceToCameraSq,
         CameraRenderState cameraRenderState
      ) {
      }

      public void submitText(
         PoseStack poseStack,
         float x,
         float y,
         FormattedCharSequence text,
         boolean shadow,
         DisplayMode displayMode,
         int backgroundColor,
         int color,
         int lightCoords,
         int outlineColor
      ) {
      }

      public void submitFlame(PoseStack poseStack, EntityRenderState state, Quaternionf quaternionf) {
      }

      public void submitLeash(PoseStack poseStack, LeashState leashState) {
      }

      public <S> void submitModel(
         Model<? super S> model,
         S state,
         PoseStack poseStack,
         RenderType renderType,
         int lightCoords,
         int overlayCoords,
         int tintedColor,
         @Nullable TextureAtlasSprite sprite,
         int outlineColor,
         @Nullable CrumblingOverlay crumblingOverlay
      ) {
         this.delegate
            .submitModel(
               model,
               state,
               poseStack,
               SyntheticHiddenPlayerRenderer.mapRenderType(this.pass, model, renderType),
               15728880,
               overlayCoords,
               tintedColor,
               sprite,
               outlineColor,
               crumblingOverlay
            );
      }

      public void submitModelPart(
         ModelPart modelPart,
         PoseStack poseStack,
         RenderType renderType,
         int lightCoords,
         int overlayCoords,
         @Nullable TextureAtlasSprite sprite,
         boolean sheeted,
         boolean hasFoil,
         int tintedColor,
         @Nullable CrumblingOverlay crumblingOverlay,
         int outlineColor
      ) {
         this.delegate
            .submitModelPart(
               modelPart,
               poseStack,
               SyntheticHiddenPlayerRenderer.mapRenderType(this.pass, renderType),
               15728880,
               overlayCoords,
               sprite,
               sheeted,
               hasFoil,
               tintedColor,
               crumblingOverlay,
               outlineColor
            );
      }

      public void submitBlock(PoseStack poseStack, BlockState blockState, int packedLight, int overlayCoords, int outlineColor) {
      }

      public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
      }

      public void submitBlockModel(
         PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tints, int packedLight, int overlayCoords, int outlineColor
      ) {
      }

      public void submitBreakingBlockModel(PoseStack poseStack, BlockStateModel blockStateModel, long seed, int outlineColor) {
      }

      public void submitItem(
         PoseStack poseStack,
         ItemDisplayContext itemDisplayContext,
         int packedLight,
         int overlayCoords,
         int outlineColor,
         int[] tints,
         List<BakedQuad> bakedQuads,
         FoilType foilType
      ) {
      }

      public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {
      }

      public void submitParticleGroup(ParticleGroupRenderer particleGroupRenderer) {
      }
   }

   private static final class HiddenSubmitCollector implements SubmitNodeCollector {
      private final SubmitNodeCollector delegate;
      private final SyntheticHiddenPlayerRenderer.Pass pass;
      private final int baseOrder;

      private HiddenSubmitCollector(SubmitNodeCollector delegate, SyntheticHiddenPlayerRenderer.Pass pass, int baseOrder) {
         this.delegate = delegate;
         this.pass = pass;
         this.baseOrder = baseOrder;
      }

      public OrderedSubmitNodeCollector order(int order) {
         return new SyntheticHiddenPlayerRenderer.HiddenOrderedCollector(this.delegate.order(this.baseOrder + order), this.pass);
      }

      public void submitShadow(PoseStack poseStack, float shadowRadius, List<ShadowPiece> shadowPieces) {
      }

      public void submitNameTag(
         PoseStack poseStack,
         Vec3 attachment,
         int verticalOffset,
         Component component,
         boolean visible,
         int lightCoords,
         double distanceToCameraSq,
         CameraRenderState cameraRenderState
      ) {
      }

      public void submitText(
         PoseStack poseStack,
         float x,
         float y,
         FormattedCharSequence text,
         boolean shadow,
         DisplayMode displayMode,
         int backgroundColor,
         int color,
         int lightCoords,
         int outlineColor
      ) {
      }

      public void submitFlame(PoseStack poseStack, EntityRenderState state, Quaternionf quaternionf) {
      }

      public void submitLeash(PoseStack poseStack, LeashState leashState) {
      }

      public <S> void submitModel(
         Model<? super S> model,
         S state,
         PoseStack poseStack,
         RenderType renderType,
         int lightCoords,
         int overlayCoords,
         int tintedColor,
         @Nullable TextureAtlasSprite sprite,
         int outlineColor,
         @Nullable CrumblingOverlay crumblingOverlay
      ) {
         this.delegate
            .order(this.baseOrder)
            .submitModel(
               model,
               state,
               poseStack,
               SyntheticHiddenPlayerRenderer.mapRenderType(this.pass, model, renderType),
               15728880,
               overlayCoords,
               tintedColor,
               sprite,
               outlineColor,
               crumblingOverlay
            );
      }

      public void submitModelPart(
         ModelPart modelPart,
         PoseStack poseStack,
         RenderType renderType,
         int lightCoords,
         int overlayCoords,
         @Nullable TextureAtlasSprite sprite,
         boolean sheeted,
         boolean hasFoil,
         int tintedColor,
         @Nullable CrumblingOverlay crumblingOverlay,
         int outlineColor
      ) {
         this.delegate
            .order(this.baseOrder)
            .submitModelPart(
               modelPart,
               poseStack,
               SyntheticHiddenPlayerRenderer.mapRenderType(this.pass, renderType),
               15728880,
               overlayCoords,
               sprite,
               sheeted,
               hasFoil,
               tintedColor,
               crumblingOverlay,
               outlineColor
            );
      }

      public void submitBlock(PoseStack poseStack, BlockState blockState, int packedLight, int overlayCoords, int outlineColor) {
      }

      public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
      }

      public void submitBlockModel(
         PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tints, int packedLight, int overlayCoords, int outlineColor
      ) {
      }

      public void submitBreakingBlockModel(PoseStack poseStack, BlockStateModel blockStateModel, long seed, int outlineColor) {
      }

      public void submitItem(
         PoseStack poseStack,
         ItemDisplayContext itemDisplayContext,
         int packedLight,
         int overlayCoords,
         int outlineColor,
         int[] tints,
         List<BakedQuad> bakedQuads,
         FoilType foilType
      ) {
      }

      public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {
      }

      public void submitParticleGroup(ParticleGroupRenderer particleGroupRenderer) {
      }
   }

   private enum Pass {
      DEPTH,
      COLOR;
   }

   private static final class SyntheticArmorModel extends HumanoidModel<AvatarRenderState> {
      private EquipmentSlot armorSlot;

      private SyntheticArmorModel(ModelPart modelPart, EquipmentSlot armorSlot) {
         super(modelPart);
         this.armorSlot = armorSlot;
      }

      private void setArmorSlot(EquipmentSlot armorSlot) {
         this.armorSlot = armorSlot;
      }

      public void setupAnim(AvatarRenderState state) {
         super.setupAnim(state);
         this.head.visible = false;
         this.hat.visible = false;
         this.body.visible = false;
         this.rightArm.visible = false;
         this.leftArm.visible = false;
         this.rightLeg.visible = false;
         this.leftLeg.visible = false;
         switch (this.armorSlot) {
            case HEAD:
               this.head.visible = true;
               break;
            case CHEST:
               this.body.visible = true;
               this.leftArm.visible = true;
               this.rightArm.visible = true;
               break;
            case LEGS:
               this.body.visible = true;
               this.leftLeg.visible = true;
               this.rightLeg.visible = true;
               break;
            case FEET:
               this.leftLeg.visible = true;
               this.rightLeg.visible = true;
         }
      }
   }

   private record SyntheticModels(
      SyntheticHiddenPlayerRenderer.BodyPlayerModel bodyModel,
      PlayerModel outerModel,
      ArmorModelSet<SyntheticHiddenPlayerRenderer.SyntheticArmorModel> armorModels,
      PlayerCapeModel capeModel
   ) {
      private static SyntheticHiddenPlayerRenderer.SyntheticModels create(Context context, boolean slim) {
         EntityModelSet modelSet = context.getModelSet();
         return new SyntheticHiddenPlayerRenderer.SyntheticModels(
            new SyntheticHiddenPlayerRenderer.BodyPlayerModel(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim),
            new PlayerModel(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim),
            ArmorModelSet.bake(
               slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR,
               modelSet,
               part -> new SyntheticHiddenPlayerRenderer.SyntheticArmorModel(part, EquipmentSlot.CHEST)
            ),
            new PlayerCapeModel(context.bakeLayer(ModelLayers.PLAYER_CAPE))
         );
      }
   }

   private record SyntheticResources(Context context, EquipmentLayerRenderer equipmentRenderer) {
      private static SyntheticHiddenPlayerRenderer.SyntheticResources create() {
         Minecraft minecraft = Minecraft.getInstance();
         EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
         Context context = new Context(
            dispatcher,
            new BlockModelResolver(minecraft.getModelManager()),
            minecraft.getItemModelResolver(),
            minecraft.getMapRenderer(),
            minecraft.getResourceManager(),
            minecraft.getEntityModels(),
            ((EntityRenderDispatcherAccessor)dispatcher).oyvey$getEquipmentAssets(),
            minecraft.getAtlasManager(),
            minecraft.font,
            minecraft.playerSkinRenderCache()
         );
         return new SyntheticHiddenPlayerRenderer.SyntheticResources(context, context.getEquipmentRenderer());
      }
   }
}
