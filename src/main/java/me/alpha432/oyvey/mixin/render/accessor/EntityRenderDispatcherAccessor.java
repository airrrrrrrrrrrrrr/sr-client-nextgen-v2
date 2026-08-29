package me.alpha432.oyvey.mixin.render.accessor;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderDispatcher.class)
public interface EntityRenderDispatcherAccessor {
   @Accessor("equipmentAssets")
   EquipmentAssetManager oyvey$getEquipmentAssets();
}
