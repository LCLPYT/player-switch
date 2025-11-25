package work.lclpnet.playerswitch.mixin;

import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.playerswitch.hook.ServerMaxPlayersCallback;

@Mixin(MinecraftDedicatedServer.class)
public class MinecraftDedicatedServerMixin {

    @Inject(
            method = "getMaxPlayerCount",
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
}
