package work.lclpnet.playerswitch.type;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.NotNull;

public interface GameProfileCapture {

    @NotNull GameProfile playerSwitch$getRealGameProfile();

    static GameProfileCapture get(ServerGamePacketListenerImpl handler) {
        return (GameProfileCapture) handler;
    }
}
