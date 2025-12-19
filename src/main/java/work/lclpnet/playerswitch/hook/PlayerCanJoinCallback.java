package work.lclpnet.playerswitch.hook;

import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

import java.net.SocketAddress;

public interface PlayerCanJoinCallback {

    Hook<PlayerCanJoinCallback> HOOK = HookFactory.createArrayBacked(PlayerCanJoinCallback.class, hooks -> (address, configEntry) -> {
        for (PlayerCanJoinCallback hook : hooks) {
            Component msg = hook.checkCanJoin(address, configEntry);

            if (msg != null) {
                return msg;
            }
        }

        return null;
    });

    @Nullable
    Component checkCanJoin(SocketAddress address, NameAndId configEntry);
}
