package work.lclpnet.playerswitch.mixin;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import work.lclpnet.playerswitch.type.PlayerSwitchGameProfile;

import java.lang.ref.WeakReference;

@Mixin(GameProfile.class)
public class GameProfileMixin implements PlayerSwitchGameProfile {

    @Unique
    private WeakReference<Object> networkHandlerRef = null;

    @Override
    public void playerSwitch$setNetworkHandler(Object networkHandler) {
        this.networkHandlerRef = new WeakReference<>(networkHandler);
    }

    @Override
    public @Nullable Object playerSwitch$getNetworkHandler() {
        var ref = networkHandlerRef;
        return ref != null ? ref.get() : null;
    }
}
