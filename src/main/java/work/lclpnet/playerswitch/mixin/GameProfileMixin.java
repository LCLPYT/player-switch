package work.lclpnet.playerswitch.mixin;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import work.lclpnet.playerswitch.type.PlayerSwitchGameProfile;

@Mixin(GameProfile.class)
public class GameProfileMixin implements PlayerSwitchGameProfile {

    @Unique
    private GameProfile realProfile = null;

    @Override
    public void playerSwitch$setRealGameProfile(GameProfile real) {
        this.realProfile = real;
    }

    @Override
    public boolean playerSwitch$isRealProfile() {
        return realProfile == null;
    }

    @Override
    public @NotNull GameProfile playerSwitch$getRealGameProfile() {
        var profile = realProfile;

        return profile != null ? profile : (GameProfile) (Object) this;
    }
}
