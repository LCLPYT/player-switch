package work.lclpnet.playerswitch.mixin;

import net.minecraft.network.DisconnectionInfo;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.playerswitch.hook.PlayerHandlerDisconnectCallback;

@Mixin(ServerCommonNetworkHandler.class)
public class ServerCommonNetworkHandlerMixin {

    @Inject(
            method = "disconnect(Lnet/minecraft/network/DisconnectionInfo;)V",
            at = @At("HEAD")
    )
    public void playerSwitch$onDisconnect(DisconnectionInfo disconnectionInfo, CallbackInfo ci) {
        var self = (ServerCommonNetworkHandler) (Object) this;

        PlayerHandlerDisconnectCallback.HOOK.invoker().onDisconnect(self);
    }
}
