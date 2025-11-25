package work.lclpnet.playerswitch.mixin;

import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import work.lclpnet.playerswitch.hook.GameProfileModificationCallback;

@Mixin(targets = "net.minecraft.server.network.PrepareSpawnTask$PlayerSpawn")
public class PrepareSpawnTaskMixin {

    @ModifyArg(
            method = "onReady",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayerEntity;<init>(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/world/ServerWorld;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/network/packet/c2s/common/SyncedClientOptions;)V"
            )
    )
    public GameProfile unifyGameProfile(GameProfile gameProfile) {
        return GameProfileModificationCallback.HOOK.invoker().modifyGameProfile(gameProfile);
    }
}
