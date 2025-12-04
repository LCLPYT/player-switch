package work.lclpnet.playerswitch.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import work.lclpnet.playerswitch.util.PlayerUnifier;

import java.util.UUID;

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

    @ModifyArg(
            method = "<init>(Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/packet/s2c/play/PlayerListS2CPacket$Entry;<init>(Ljava/util/UUID;Lcom/mojang/authlib/GameProfile;ZILnet/minecraft/world/GameMode;Lnet/minecraft/text/Text;ZILnet/minecraft/network/encryption/PublicPlayerSession$Serialized;)V"
            )
    )
    private static UUID useRealUuid(UUID uuid, @Local(argsOnly = true) ServerPlayerEntity player) {
        return PlayerUnifier.getRealProfile(player.getGameProfile()).id();
    }
}
