package me.alpha432.oyvey.util.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public final class ChamsRenderSystem {
   private static final Set<EntityRenderState> TRACKED_STATES = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
   private static final ThreadLocal<ChamsRenderSystem.FrameState> FRAME_STATE = ThreadLocal.withInitial(ChamsRenderSystem.FrameState::new);

   private ChamsRenderSystem() {
   }

   public static void trackState(EntityRenderState state) {
      TRACKED_STATES.add(state);
   }

   public static void clearTrackedState(EntityRenderState state) {
      TRACKED_STATES.remove(state);
   }

   public static void beginEntity(EntityRenderState state) {
      ChamsRenderSystem.FrameState frameState = FRAME_STATE.get();
      frameState.entityActive = TRACKED_STATES.contains(state);
      frameState.classicCapeDepth = 0;
      frameState.replayDepth = 0;
      frameState.unifiedPlayerModel.clear();
      frameState.avatarState = frameState.entityActive && state instanceof AvatarRenderState avatarRenderState ? avatarRenderState : null;
   }

   public static void endEntity() {
      ChamsRenderSystem.FrameState frameState = FRAME_STATE.get();
      frameState.entityActive = false;
      frameState.classicCapeDepth = 0;
      frameState.replayDepth = 0;
      frameState.unifiedPlayerModel.clear();
      frameState.avatarState = null;
   }

   public static void flushEntity(CameraRenderState cameraRenderState, double x, double y, double z, PoseStack poseStack, SubmitNodeCollector collector) {
      ChamsRenderSystem.FrameState frameState = FRAME_STATE.get();
      if (frameState.entityActive && frameState.avatarState != null) {
         frameState.syntheticHiddenPlayerRenderer.renderHidden(frameState.avatarState, cameraRenderState, x, y, z, poseStack, collector);
      }
   }

   public static void pushClassicCape() {
      FRAME_STATE.get().classicCapeDepth++;
   }

   public static void popClassicCape() {
      ChamsRenderSystem.FrameState frameState = FRAME_STATE.get();
      if (frameState.classicCapeDepth > 0) {
         frameState.classicCapeDepth--;
      }
   }

   public static boolean shouldCaptureHiddenPass() {
      return false;
   }

   public static void replayStorageModel(
      Model<?> model,
      Object state,
      PoseStack poseStack,
      RenderType renderType,
      int overlayCoords,
      int tintedColor,
      TextureAtlasSprite sprite,
      int outlineColor,
      CrumblingOverlay crumblingOverlay
   ) {
      ChamsRenderSystem.FrameState frameState = FRAME_STATE.get();
      if (frameState.classicCapeDepth > 0) {
         frameState.unifiedPlayerModel.captureCape(model, state, poseStack, renderType, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
      } else {
         frameState.unifiedPlayerModel.captureBody(model, state, poseStack, renderType, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
      }
   }

   public static void replayLayerModel(
      Model<?> model,
      Object state,
      PoseStack poseStack,
      RenderType renderType,
      int overlayCoords,
      int tintedColor,
      TextureAtlasSprite sprite,
      int outlineColor,
      CrumblingOverlay crumblingOverlay
   ) {
      FRAME_STATE.get()
         .unifiedPlayerModel
         .captureAttachment(model, state, poseStack, renderType, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
   }

   public static void replayOuterSkinPart(
      ModelPart modelPart,
      PoseStack poseStack,
      RenderType renderType,
      int overlayCoords,
      TextureAtlasSprite sprite,
      boolean sheeted,
      boolean hasFoil,
      int tintedColor,
      CrumblingOverlay crumblingOverlay,
      int outlineColor
   ) {
      FRAME_STATE.get()
         .unifiedPlayerModel
         .captureOuterSkin(modelPart, poseStack, renderType, overlayCoords, sprite, sheeted, hasFoil, tintedColor, crumblingOverlay, outlineColor);
   }

   private static void replay(Runnable runnable) {
      ChamsRenderSystem.FrameState frameState = FRAME_STATE.get();
      frameState.replayDepth++;

      try {
         runnable.run();
      } finally {
         frameState.replayDepth--;
      }
   }

   private static final class FrameState {
      private boolean entityActive;
      private int classicCapeDepth;
      private int replayDepth;
      private AvatarRenderState avatarState;
      private final UnifiedPlayerModel unifiedPlayerModel = new UnifiedPlayerModel();
      private final SyntheticHiddenPlayerRenderer syntheticHiddenPlayerRenderer = new SyntheticHiddenPlayerRenderer();
   }
}
