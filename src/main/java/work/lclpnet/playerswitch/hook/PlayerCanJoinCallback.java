package work.lclpnet.playerswitch.hook;

import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

import java.net.SocketAddress;

public interface PlayerCanJoinCallback {

    Hook<PlayerCanJoinCallback> HOOK = HookFactory.createArrayBacked(PlayerCanJoinCallback.class, hooks -> (address, configEntry) -> {
        for (PlayerCanJoinCallback hook : hooks) {
            Text msg = hook.checkCanJoin(address, configEntry);

            if (msg != null) {
                return msg;
            }
        }

        return null;
    });

    @Nullable
    Text checkCanJoin(SocketAddress address, PlayerConfigEntry configEntry);
}
