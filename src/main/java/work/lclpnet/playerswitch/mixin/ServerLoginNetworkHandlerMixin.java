package work.lclpnet.playerswitch.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.playerswitch.type.PlayerSwitchGameProfile;

@Mixin(ServerLoginNetworkHandler.class)
public class ServerLoginNetworkHandlerMixin {

    @Inject(
            method = "tickVerify",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;checkCanJoin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/text/Text;"
            )
    )
    public void beforeCheckCanJoin(GameProfile profile, CallbackInfo ci) {
        ((PlayerSwitchGameProfile) profile).playerSwitch$setNetworkHandler(this);
    }
}
