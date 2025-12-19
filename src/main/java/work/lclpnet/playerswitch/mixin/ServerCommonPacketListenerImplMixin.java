package work.lclpnet.playerswitch.mixin;

import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.playerswitch.hook.PlayerHandlerDisconnectCallback;

@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerImplMixin {

    @Inject(
            method = "disconnect(Lnet/minecraft/network/DisconnectionDetails;)V",
            at = @At("HEAD")
    )
    public void playerSwitch$onDisconnect(DisconnectionDetails disconnectionInfo, CallbackInfo ci) {
        var self = (ServerCommonPacketListenerImpl) (Object) this;

        PlayerHandlerDisconnectCallback.HOOK.invoker().onDisconnect(self);
    }
}
