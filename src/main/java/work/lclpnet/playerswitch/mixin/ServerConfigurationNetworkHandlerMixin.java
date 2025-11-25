package work.lclpnet.playerswitch.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.c2s.config.ReadyC2SPacket;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.playerswitch.hook.PlayerBeforeCheckCanJoinCallback;

@Mixin(ServerConfigurationNetworkHandler.class)
public class ServerConfigurationNetworkHandlerMixin {

    @Shadow @Final private GameProfile profile;

    @Inject(
            method = "onReady",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;checkCanJoin(Ljava/net/SocketAddress;Lnet/minecraft/server/PlayerConfigEntry;)Lnet/minecraft/text/Text;"
            )
    )
    public void beforeCheckCanJoin(ReadyC2SPacket packet, CallbackInfo ci) {
        ServerConfigurationNetworkHandler self = (ServerConfigurationNetworkHandler) (Object) this;
        PlayerBeforeCheckCanJoinCallback.HOOK.invoker().beforeCheckCanJoin(profile, self);
    }
}
