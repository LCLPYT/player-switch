package work.lclpnet.playerswitch.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.playerswitch.PlayerSwitchInit;
import work.lclpnet.playerswitch.hook.ServerMaxPlayersCallback;
import work.lclpnet.playerswitch.hook.ServerPausedCallback;
import work.lclpnet.playerswitch.hook.ServerTickPauseCallback;
import work.lclpnet.playerswitch.type.PlayerSwitchMinecraftServer;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements PlayerSwitchMinecraftServer {

    @Shadow protected abstract void runAutosave();

    @Shadow public abstract void tickNetworkIo();

    @Shadow
    private @Nullable ServerMetadata metadata;

    @Shadow
    protected abstract ServerMetadata createMetadata();

    @Unique
    private boolean paused = false;

    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    public void shouldPauseTicking(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        var self = (MinecraftServer) (Object) this;

        boolean pause = ServerTickPauseCallback.HOOK.invoker().shouldPause(self);
        boolean wasPaused = paused;
        paused = pause;

        if (!pause) return;

        if (!wasPaused) {
            PlayerSwitchInit.LOGGER.info("Pausing the server");

            runAutosave();

            ServerPausedCallback.HOOK.invoker().onPause(self);
        }

        tickNetworkIo();
        ci.cancel();
    }

    @Override
    public void playerSwitch$updateMetadata() {
        metadata = createMetadata();
    }

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
