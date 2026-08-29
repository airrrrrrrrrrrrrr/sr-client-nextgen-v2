package me.alpha432.oyvey.util.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;

final class UnifiedPlayerModel {
   private static final int FULL_BRIGHT_LIGHT = 15728880;
   private final EnumMap<UnifiedPlayerModel.PieceType, List<UnifiedPlayerModel.HiddenCompositeEntry>> entries = new EnumMap<>(
      UnifiedPlayerModel.PieceType.class
   );

   UnifiedPlayerModel() {
      for (UnifiedPlayerModel.PieceType pieceType : UnifiedPlayerModel.PieceType.values()) {
         this.entries.put(pieceType, new ArrayList<>());
      }
   }

   void captureBody(
      Model<?> model,
      Object state,
      PoseStack poseStack,
      RenderType renderType,
      int overlayCoords,
      int tintedColor,
      @Nullable TextureAtlasSprite sprite,
      int outlineColor,
      CrumblingOverlay crumblingOverlay
   ) {
      this.entries
         .get(UnifiedPlayerModel.PieceType.BODY)
         .add(
            UnifiedPlayerModel.ModelEntry.capture(
               UnifiedPlayerModel.PieceType.BODY,
               model,
               state,
               poseStack,
               UnifiedPlayerModel.HiddenMaterial.resolve(UnifiedPlayerModel.PieceType.BODY, renderType),
               overlayCoords,
               tintedColor,
               sprite,
               outlineColor,
               crumblingOverlay
            )
         );
   }

   void captureOuterSkin(
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
      this.entries
         .get(UnifiedPlayerModel.PieceType.OUTER_SKIN)
         .add(
            UnifiedPlayerModel.ModelPartEntry.capture(
               UnifiedPlayerModel.PieceType.OUTER_SKIN,
               modelPart,
               poseStack,
               UnifiedPlayerModel.HiddenMaterial.resolve(UnifiedPlayerModel.PieceType.OUTER_SKIN, renderType),
               overlayCoords,
               sprite,
               sheeted,
               hasFoil,
               tintedColor,
               crumblingOverlay,
               outlineColor
            )
         );
   }

   void captureAttachment(
      Model<?> model,
      Object state,
      PoseStack poseStack,
      RenderType renderType,
      int overlayCoords,
      int tintedColor,
      @Nullable TextureAtlasSprite sprite,
      int outlineColor,
      CrumblingOverlay crumblingOverlay
   ) {
      this.entries
         .get(UnifiedPlayerModel.PieceType.ATTACHMENT)
         .add(
            UnifiedPlayerModel.ModelEntry.capture(
               UnifiedPlayerModel.PieceType.ATTACHMENT,
               model,
               state,
               poseStack,
               UnifiedPlayerModel.HiddenMaterial.resolve(UnifiedPlayerModel.PieceType.ATTACHMENT, renderType),
               overlayCoords,
               tintedColor,
               sprite,
               outlineColor,
               crumblingOverlay
            )
         );
   }

   void captureCape(
      Model<?> model,
      Object state,
      PoseStack poseStack,
      RenderType renderType,
      int overlayCoords,
      int tintedColor,
      @Nullable TextureAtlasSprite sprite,
      int outlineColor,
      CrumblingOverlay crumblingOverlay
   ) {
      this.entries
         .get(UnifiedPlayerModel.PieceType.CAPE)
         .add(
            UnifiedPlayerModel.ModelEntry.capture(
               UnifiedPlayerModel.PieceType.CAPE,
               model,
               state,
               poseStack,
               UnifiedPlayerModel.HiddenMaterial.resolve(UnifiedPlayerModel.PieceType.CAPE, renderType),
               overlayCoords,
               tintedColor,
               sprite,
               outlineColor,
               crumblingOverlay
            )
         );
   }

   boolean isEmpty() {
      for (List<UnifiedPlayerModel.HiddenCompositeEntry> pieceEntries : this.entries.values()) {
         if (!pieceEntries.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   void clear() {
      for (List<UnifiedPlayerModel.HiddenCompositeEntry> pieceEntries : this.entries.values()) {
         pieceEntries.clear();
      }
   }

   void renderHidden(SubmitNodeStorage storage) {
      ChamsCompositeTarget.markUsed();
      this.renderDepthPass(storage);
      this.renderColorPass(storage);
   }

   private void renderDepthPass(SubmitNodeStorage storage) {
      this.renderCategoryDepth(storage, UnifiedPlayerModel.PieceType.BODY);
      this.renderCategoryDepth(storage, UnifiedPlayerModel.PieceType.OUTER_SKIN);
      this.renderCategoryDepth(storage, UnifiedPlayerModel.PieceType.ATTACHMENT);
      this.renderCategoryDepth(storage, UnifiedPlayerModel.PieceType.CAPE);
   }

   private void renderColorPass(SubmitNodeStorage storage) {
      this.renderCategoryColor(storage, UnifiedPlayerModel.PieceType.BODY);
      this.renderCategoryColor(storage, UnifiedPlayerModel.PieceType.OUTER_SKIN);
      this.renderCategoryColor(storage, UnifiedPlayerModel.PieceType.ATTACHMENT);
      this.renderCategoryColor(storage, UnifiedPlayerModel.PieceType.CAPE);
   }

   private void renderCategoryDepth(SubmitNodeStorage storage, UnifiedPlayerModel.PieceType pieceType) {
      for (UnifiedPlayerModel.HiddenCompositeEntry entry : this.entries.get(pieceType)) {
         entry.renderDepth(storage);
      }
   }

   private void renderCategoryColor(SubmitNodeStorage storage, UnifiedPlayerModel.PieceType pieceType) {
      for (UnifiedPlayerModel.HiddenCompositeEntry entry : this.entries.get(pieceType)) {
         entry.renderColor(storage);
      }
   }

   private record CapturedPose(Pose pose) {
      private static UnifiedPlayerModel.CapturedPose from(PoseStack poseStack) {
         return new UnifiedPlayerModel.CapturedPose(poseStack.last().copy());
      }

      private PoseStack toPoseStack() {
         PoseStack poseStack = new PoseStack();
         poseStack.last().set(this.pose);
         return poseStack;
      }
   }

   private interface HiddenCompositeEntry {
      void renderDepth(SubmitNodeStorage var1);

      void renderColor(SubmitNodeStorage var1);
   }

   private record HiddenMaterial(RenderType originalType, RenderType hiddenDepthType, RenderType hiddenColorType) {
      private static UnifiedPlayerModel.HiddenMaterial resolve(UnifiedPlayerModel.PieceType pieceType, RenderType originalType) {
         return switch (pieceType) {
            case BODY -> new UnifiedPlayerModel.HiddenMaterial(
               originalType, Layers.hiddenCompositeDepth(originalType), Layers.hiddenCompositeColor(originalType)
            );
            case OUTER_SKIN, ATTACHMENT -> new UnifiedPlayerModel.HiddenMaterial(
               originalType, Layers.hiddenCompositeDepth(originalType), Layers.hiddenCompositeColor(originalType)
            );
            case CAPE -> new UnifiedPlayerModel.HiddenMaterial(
               originalType, Layers.hiddenCompositeDepth(originalType), Layers.classicCapeHiddenCompositeColor(originalType)
            );
         };
      }
   }

   private record ModelEntry(
      UnifiedPlayerModel.PieceType pieceType,
      UnifiedPlayerModel.HiddenMaterial hiddenMaterial,
      Model<?> model,
      Object state,
      UnifiedPlayerModel.CapturedPose pose,
      int overlayCoords,
      int tintedColor,
      @Nullable TextureAtlasSprite sprite,
      int outlineColor,
      CrumblingOverlay crumblingOverlay
   ) implements UnifiedPlayerModel.HiddenCompositeEntry {
      private static UnifiedPlayerModel.ModelEntry capture(
         UnifiedPlayerModel.PieceType pieceType,
         Model<?> model,
         Object state,
         PoseStack poseStack,
         UnifiedPlayerModel.HiddenMaterial hiddenMaterial,
         int overlayCoords,
         int tintedColor,
         @Nullable TextureAtlasSprite sprite,
         int outlineColor,
         CrumblingOverlay crumblingOverlay
      ) {
         return new UnifiedPlayerModel.ModelEntry(
            pieceType,
            hiddenMaterial,
            model,
            state,
            UnifiedPlayerModel.CapturedPose.from(poseStack),
            overlayCoords,
            tintedColor,
            sprite,
            outlineColor,
            crumblingOverlay
         );
      }

      @Override
      public void renderDepth(SubmitNodeStorage storage) {
         this.submit(storage, this.hiddenMaterial.hiddenDepthType());
      }

      @Override
      public void renderColor(SubmitNodeStorage storage) {
         this.submit(storage, this.hiddenMaterial.hiddenColorType());
      }

      private void submit(SubmitNodeStorage storage, RenderType hiddenType) {
         storage.submitModel(
            (Model)this.model,
            this.state,
            this.pose.toPoseStack(),
            hiddenType,
            15728880,
            this.overlayCoords,
            this.tintedColor,
            this.sprite,
            this.outlineColor,
            this.crumblingOverlay
         );
      }
   }

   private record ModelPartEntry(
      UnifiedPlayerModel.PieceType pieceType,
      UnifiedPlayerModel.HiddenMaterial hiddenMaterial,
      ModelPart modelPart,
      UnifiedPlayerModel.CapturedPose pose,
      int overlayCoords,
      TextureAtlasSprite sprite,
      boolean sheeted,
      boolean hasFoil,
      int tintedColor,
      CrumblingOverlay crumblingOverlay,
      int outlineColor
   ) implements UnifiedPlayerModel.HiddenCompositeEntry {
      private static UnifiedPlayerModel.ModelPartEntry capture(
         UnifiedPlayerModel.PieceType pieceType,
         ModelPart modelPart,
         PoseStack poseStack,
         UnifiedPlayerModel.HiddenMaterial hiddenMaterial,
         int overlayCoords,
         TextureAtlasSprite sprite,
         boolean sheeted,
         boolean hasFoil,
         int tintedColor,
         CrumblingOverlay crumblingOverlay,
         int outlineColor
      ) {
         return new UnifiedPlayerModel.ModelPartEntry(
            pieceType,
            hiddenMaterial,
            modelPart,
            UnifiedPlayerModel.CapturedPose.from(poseStack),
            overlayCoords,
            sprite,
            sheeted,
            hasFoil,
            tintedColor,
            crumblingOverlay,
            outlineColor
         );
      }

      @Override
      public void renderDepth(SubmitNodeStorage storage) {
         this.submit(storage, this.hiddenMaterial.hiddenDepthType());
      }

      @Override
      public void renderColor(SubmitNodeStorage storage) {
         this.submit(storage, this.hiddenMaterial.hiddenColorType());
      }

      private void submit(SubmitNodeStorage storage, RenderType hiddenType) {
         storage.submitModelPart(
            this.modelPart,
            this.pose.toPoseStack(),
            hiddenType,
            15728880,
            this.overlayCoords,
            this.sprite,
            this.sheeted,
            this.hasFoil,
            this.tintedColor,
            this.crumblingOverlay,
            this.outlineColor
         );
      }
   }

   private enum PieceType {
      BODY,
      OUTER_SKIN,
      ATTACHMENT,
      CAPE;
   }
}
