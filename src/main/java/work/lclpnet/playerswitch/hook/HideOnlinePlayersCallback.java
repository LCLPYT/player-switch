package work.lclpnet.playerswitch.hook;

import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

public interface HideOnlinePlayersCallback {

    Hook<HideOnlinePlayersCallback> HOOK = HookFactory.createArrayBacked(HideOnlinePlayersCallback.class, hooks -> () -> {
        for (HideOnlinePlayersCallback hook : hooks) {
            if (hook.shouldHideOnlinePlayers()) {
                return true;
            }
        }

        return false;
    });

    boolean shouldHideOnlinePlayers();
}
