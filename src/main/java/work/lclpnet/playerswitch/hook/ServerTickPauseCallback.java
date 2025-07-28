package work.lclpnet.playerswitch.hook;

import net.minecraft.server.MinecraftServer;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

public interface ServerTickPauseCallback {

    Hook<ServerTickPauseCallback> HOOK = HookFactory.createArrayBacked(ServerTickPauseCallback.class, hooks -> (server) -> {
        for (ServerTickPauseCallback hook : hooks) {
            if (hook.shouldPause(server)) {
                return true;
            }
        }

        return false;
    });

    boolean shouldPause(MinecraftServer server);
}
