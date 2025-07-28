package work.lclpnet.playerswitch.type;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;

public interface PlayerSwitchGameProfile extends GameProfileCapture {

    void playerSwitch$setNetworkHandler(Object networkHandler);

    @Nullable
    Object playerSwitch$getNetworkHandler();

    void playerSwitch$setRealGameProfile(GameProfile real);
}
