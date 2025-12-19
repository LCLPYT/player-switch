package work.lclpnet.playerswitch.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.NameAndId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import work.lclpnet.playerswitch.util.PlayerUnifier;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Shadow public ServerGamePacketListenerImpl connection;

    @ModifyArg(
            method = "permissions",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;getProfilePermissions(Lnet/minecraft/server/players/NameAndId;)Lnet/minecraft/server/permissions/LevelBasedPermissionSet;"
            )
    )
    public NameAndId useRealGameProfileForPermissions(NameAndId player) {
        return new NameAndId(PlayerUnifier.getRealProfile(connection));
    }
}
