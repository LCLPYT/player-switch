package work.lclpnet.playerswitch.hook;

import com.mojang.authlib.GameProfile;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

public interface GameProfileModificationCallback {

    Hook<GameProfileModificationCallback> HOOK = HookFactory.createArrayBacked(GameProfileModificationCallback.class, hooks -> (gameProfile) -> {
        for (GameProfileModificationCallback hook : hooks) {
            gameProfile = hook.modifyGameProfile(gameProfile);
        }

        return gameProfile;
    });

    GameProfile modifyGameProfile(GameProfile gameProfile);
}
