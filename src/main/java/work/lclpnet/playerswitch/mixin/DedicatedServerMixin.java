package work.lclpnet.playerswitch.mixin;

import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.playerswitch.hook.HideOnlinePlayersCallback;
import work.lclpnet.playerswitch.hook.ServerMaxPlayersCallback;

@Mixin(DedicatedServer.class)
public class DedicatedServerMixin {

    @Inject(
            method = "getMaxPlayers",
            at = @At("RETURN"),
            cancellable = true
    )
    public void modifyMaxPlayerCount(CallbackInfoReturnable<Integer> cir) {
        int maxPlayers = cir.getReturnValueI();
        int modified = ServerMaxPlayersCallback.HOOK.invoker().modifyMaxPlayers(maxPlayers);

        if (maxPlayers != modified) {
            cir.setReturnValue(modified);
        }
    }

    @Inject(
            method = "hidesOnlinePlayers",
            at = @At("RETURN"),
            cancellable = true
    )
    public void modifyHideOnlinePlayers(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && HideOnlinePlayersCallback.HOOK.invoker().shouldHideOnlinePlayers()) {
            cir.setReturnValue(true);
        }
    }
}
