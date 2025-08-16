package work.lclpnet.playerswitch.hook;

import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

public interface ServerMaxPlayersCallback {

    Hook<ServerMaxPlayersCallback> HOOK = HookFactory.createArrayBacked(ServerMaxPlayersCallback.class, hooks -> (maxPlayers) -> {
        for (ServerMaxPlayersCallback hook : hooks) {
            maxPlayers = hook.modifyMaxPlayers(maxPlayers);
        }

        return maxPlayers;
    });

    int modifyMaxPlayers(int maxPlayers);
}
