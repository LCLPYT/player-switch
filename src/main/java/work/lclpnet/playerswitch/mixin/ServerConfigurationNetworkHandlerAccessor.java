package work.lclpnet.playerswitch.mixin;

import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerConfigurationNetworkHandler.class)
public interface ServerConfigurationNetworkHandlerAccessor {

    @Accessor
    SyncedClientOptions getSyncedOptions();
}
