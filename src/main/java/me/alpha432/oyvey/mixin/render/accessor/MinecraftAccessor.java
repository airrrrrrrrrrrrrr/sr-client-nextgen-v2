package me.alpha432.oyvey.mixin.render.accessor;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
   @Mutable
   @Accessor("user")
   void oyvey$setUser(User user);

   @Mutable
   @Accessor("userApiService")
   void oyvey$setUserApiService(UserApiService userApiService);

   @Mutable
   @Accessor("profileKeyPairManager")
   void oyvey$setProfileKeyPairManager(ProfileKeyPairManager profileKeyPairManager);

   @Mutable
   @Accessor("profileFuture")
   void oyvey$setProfileFuture(CompletableFuture<ProfileResult> profileFuture);

   @Invoker("startUseItem")
   void oyvey$startUseItem();
}
