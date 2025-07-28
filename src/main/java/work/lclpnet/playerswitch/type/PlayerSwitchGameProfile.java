package work.lclpnet.playerswitch.type;

import org.jetbrains.annotations.Nullable;

public interface PlayerSwitchGameProfile {

    void playerSwitch$setNetworkHandler(Object networkHandler);

    @Nullable
    Object playerSwitch$getNetworkHandler();
}
