package work.lclpnet.playerswitch.hook;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

public interface PlayerBeforeCheckCanJoinCallback {

    Hook<PlayerBeforeCheckCanJoinCallback> HOOK = HookFactory.createArrayBacked(PlayerBeforeCheckCanJoinCallback.class, hooks -> (profile, handler) -> {
        for (PlayerBeforeCheckCanJoinCallback hook : hooks) {
            hook.beforeCheckCanJoin(profile, handler);
        }
    });

    void beforeCheckCanJoin(GameProfile profile, ServerConfigurationPacketListenerImpl handler);
}
