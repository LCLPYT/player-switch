package work.lclpnet.playerswitch.type;

import com.mojang.authlib.GameProfile;

public interface PlayerSwitchGameProfile extends GameProfileCapture {

    void playerSwitch$setRealGameProfile(GameProfile real);

    boolean playerSwitch$isRealProfile();

    static PlayerSwitchGameProfile get(GameProfile profile) {
        return (PlayerSwitchGameProfile) (Object) profile;
    }
}
