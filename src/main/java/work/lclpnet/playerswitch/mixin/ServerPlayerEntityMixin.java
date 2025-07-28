package work.lclpnet.playerswitch.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import work.lclpnet.playerswitch.util.PlayerUnifier;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Shadow public ServerPlayNetworkHandler networkHandler;

    @ModifyArg(
            method = "getPermissionLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;getPermissionLevel(Lcom/mojang/authlib/GameProfile;)I"
            )
    )
    public GameProfile useRealGameProfileForPermissions(GameProfile profile) {
        return PlayerUnifier.getRealProfile(networkHandler);
    }
}
