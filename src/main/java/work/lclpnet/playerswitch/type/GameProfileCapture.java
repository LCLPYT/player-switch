package work.lclpnet.playerswitch.type;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayNetworkHandler;

public interface GameProfileCapture {

    GameProfile playerSwitch$getRealGameProfile();

    static GameProfileCapture get(ServerPlayNetworkHandler handler) {
        return (GameProfileCapture) handler;
    }
}
