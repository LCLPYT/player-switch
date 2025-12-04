package work.lclpnet.playerswitch.type;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.jetbrains.annotations.NotNull;

public interface GameProfileCapture {

    @NotNull GameProfile playerSwitch$getRealGameProfile();

    static GameProfileCapture get(ServerPlayNetworkHandler handler) {
        return (GameProfileCapture) handler;
    }
}
