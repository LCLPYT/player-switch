package work.lclpnet.playerswitch.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import work.lclpnet.playerswitch.util.PlayerUnifier;

@Mixin(PlayerListS2CPacket.Entry.class)
public class PlayerListS2CPacketEntryMixin {

    @WrapOperation(
            method = "<init>(Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayerEntity;getGameProfile()Lcom/mojang/authlib/GameProfile;"
            )
    )
    private static GameProfile useRealGameProfile(ServerPlayerEntity instance, Operation<GameProfile> original) {
        GameProfile profile = original.call(instance);
        return PlayerUnifier.getRealProfile(profile);
    }
}
