package work.lclpnet.playerswitch.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import work.lclpnet.playerswitch.util.PlayerUnifier;

import java.util.UUID;

@Mixin(ClientboundPlayerInfoUpdatePacket.Entry.class)
public class PlayerListS2CPacketEntryMixin {

    @WrapOperation(
            method = "<init>(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;getGameProfile()Lcom/mojang/authlib/GameProfile;"
            )
    )
    private static GameProfile useRealGameProfile(ServerPlayer instance, Operation<GameProfile> original) {
        GameProfile profile = original.call(instance);
        return PlayerUnifier.getRealProfile(profile);
    }

    @ModifyArg(
            method = "<init>(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket$Entry;<init>(Ljava/util/UUID;Lcom/mojang/authlib/GameProfile;ZILnet/minecraft/world/level/GameType;Lnet/minecraft/network/chat/Component;ZILnet/minecraft/network/chat/RemoteChatSession$Data;)V"
            )
    )
    private static UUID useRealUuid(UUID uuid, @Local(argsOnly = true) ServerPlayer player) {
        return PlayerUnifier.getRealProfile(player.getGameProfile()).id();
    }
}
