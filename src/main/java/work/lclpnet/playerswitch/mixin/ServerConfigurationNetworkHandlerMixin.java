package work.lclpnet.playerswitch.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.c2s.config.ReadyC2SPacket;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.playerswitch.hook.GameProfileModificationCallback;
import work.lclpnet.playerswitch.type.PlayerSwitchGameProfile;

@Mixin(ServerConfigurationNetworkHandler.class)
public class ServerConfigurationNetworkHandlerMixin {

    @Shadow @Final private GameProfile profile;

    @Inject(
            method = "onReady",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;checkCanJoin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/text/Text;"
            )
    )
    public void beforeCheckCanJoin(ReadyC2SPacket packet, CallbackInfo ci) {
        ((PlayerSwitchGameProfile) profile).playerSwitch$setNetworkHandler(this);
    }

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
