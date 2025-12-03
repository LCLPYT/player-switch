package work.lclpnet.playerswitch.hook;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

public interface CodeOfConductCallback {

    Hook<CodeOfConductCallback> HOOK = HookFactory.createArrayBacked(CodeOfConductCallback.class, hooks -> (profile, lang) -> {
        for (var hook : hooks) {
            var provider = hook.getCodeOfConductProvider(profile, lang);

            if (provider != null) {
                return provider;
            }
        }

        return null;
    });

    @Nullable String getCodeOfConductProvider(GameProfile profile, String lang);
}
