package work.lclpnet.playerswitch.hook;

import net.minecraft.server.network.ServerCommonNetworkHandler;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

public interface PlayerHandlerDisconnectCallback {

    Hook<PlayerHandlerDisconnectCallback> HOOK = HookFactory.createArrayBacked(PlayerHandlerDisconnectCallback.class, hooks -> handler -> {
        for (PlayerHandlerDisconnectCallback hook : hooks) {
            hook.onDisconnect(handler);
        }
    });

    void onDisconnect(ServerCommonNetworkHandler handler);
}
