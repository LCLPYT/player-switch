package work.lclpnet.playerswitch.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.playerswitch.type.GameProfileCapture;
import work.lclpnet.playerswitch.type.PlayerSwitchGameProfile;

import java.util.UUID;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin implements GameProfileCapture {

    @Shadow public ServerPlayerEntity player;

    @Shadow public abstract void sendProfilelessChatMessage(Text message, MessageType.Parameters params);

    @Unique
    private GameProfile realGameProfile;

    @Override
    public GameProfile playerSwitch$getRealGameProfile() {
        return realGameProfile;
    }

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/message/MessageChain$Unpacker;unsigned(Ljava/util/UUID;Ljava/util/function/BooleanSupplier;)Lnet/minecraft/network/message/MessageChain$Unpacker;"
            )
    )
    public void setOriginalGameProfile(MinecraftServer server, ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        realGameProfile = ((PlayerSwitchGameProfile) player.getGameProfile()).playerSwitch$getRealGameProfile();
    }

    @ModifyArg(
            method = "onPlayerSession",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/encryption/PublicPlayerSession$Serialized;toSession(Lcom/mojang/authlib/GameProfile;Lnet/minecraft/network/encryption/SignatureVerifier;)Lnet/minecraft/network/encryption/PublicPlayerSession;"
            )
    )
    public GameProfile useRealGameProfileForSession(GameProfile gameProfile) {
        return realGameProfile != null ? realGameProfile : player.getGameProfile();
    }

    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/message/MessageChain$Unpacker;unsigned(Ljava/util/UUID;Ljava/util/function/BooleanSupplier;)Lnet/minecraft/network/message/MessageChain$Unpacker;"
            )
    )
    public UUID useRealUuidForSigning(UUID sender) {
        return realGameProfile.getId();
    }

    @ModifyArg(
            method = "setSession",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/encryption/PublicPlayerSession;createUnpacker(Ljava/util/UUID;)Lnet/minecraft/network/message/MessageChain$Unpacker;"
            )
    )
    public UUID useRealUuidForUnpacker(UUID sender) {
        return realGameProfile.getId();
    }

    @Inject(
            method = "sendChatMessage",
            at = @At("HEAD"),
            cancellable = true
    )

    public void sendOnlyUnsignedChatMessages(SignedMessage message, MessageType.Parameters params, CallbackInfo ci) {
        ci.cancel();
        sendProfilelessChatMessage(message.getContent(), params);
    }
}
