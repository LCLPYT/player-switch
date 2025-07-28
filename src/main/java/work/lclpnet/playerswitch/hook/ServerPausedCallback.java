package work.lclpnet.playerswitch.hook;

import net.minecraft.server.MinecraftServer;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

public interface ServerPausedCallback {

    Hook<ServerPausedCallback> HOOK = HookFactory.createArrayBacked(ServerPausedCallback.class, hooks -> (server) -> {
        for (ServerPausedCallback hook : hooks) {
            hook.onPause(server);
        }
    });

    void onPause(MinecraftServer server);
}
