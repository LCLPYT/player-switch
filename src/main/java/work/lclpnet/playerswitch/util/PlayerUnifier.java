package work.lclpnet.playerswitch.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import work.lclpnet.kibu.hook.HookRegistrar;
import work.lclpnet.playerswitch.config.Config;
import work.lclpnet.playerswitch.hook.GameProfileModificationCallback;
import work.lclpnet.playerswitch.type.GameProfileCapture;
import work.lclpnet.playerswitch.type.PlayerSwitchGameProfile;

import java.util.UUID;

public class PlayerUnifier {

    private final Config config;

    public PlayerUnifier(Config config) {
        this.config = config;
    }

    public void setup(HookRegistrar hooks) {
        hooks.registerHook(GameProfileModificationCallback.HOOK, profile -> {
            var unified = new GameProfile(config.getFixedUuid(), config.getFixedUsername());

            ((PlayerSwitchGameProfile) unified).playerSwitch$setRealGameProfile(profile);

            return unified;
        });
    }

    public static UUID getRealUuid(ServerPlayerEntity player) {
        return getRealProfile(player.networkHandler).getId();
    }

    public static GameProfile getRealProfile(ServerPlayNetworkHandler networkHandler) {
        return ((GameProfileCapture) networkHandler).playerSwitch$getRealGameProfile();
    }
}
