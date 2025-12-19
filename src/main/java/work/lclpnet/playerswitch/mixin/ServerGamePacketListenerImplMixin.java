package work.lclpnet.playerswitch.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.NotNull;
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

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin implements GameProfileCapture {

    @Shadow public ServerPlayer player;

    @Shadow public abstract void sendDisguisedChatMessage(Component message, ChatType.Bound params);

    @Unique
    private GameProfile realGameProfile;

    @Override
    public @NotNull GameProfile playerSwitch$getRealGameProfile() {
        return realProfile();
    }

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/SignedMessageChain$Decoder;unsigned(Ljava/util/UUID;Ljava/util/function/BooleanSupplier;)Lnet/minecraft/network/chat/SignedMessageChain$Decoder;"
            )
    )
    public void setOriginalGameProfile(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci) {
        realGameProfile = PlayerSwitchGameProfile.get(player.getGameProfile()).playerSwitch$getRealGameProfile();
    }

    @Unique
    private GameProfile realProfile() {
        return realGameProfile != null ? realGameProfile : player.getGameProfile();
    }

    @ModifyArg(
            method = "handleChatSessionUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/RemoteChatSession$Data;validate(Lcom/mojang/authlib/GameProfile;Lnet/minecraft/util/SignatureValidator;)Lnet/minecraft/network/chat/RemoteChatSession;"
            )
    )
    public GameProfile useRealGameProfileForSession(GameProfile gameProfile) {
        return realProfile();
    }

    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/SignedMessageChain$Decoder;unsigned(Ljava/util/UUID;Ljava/util/function/BooleanSupplier;)Lnet/minecraft/network/chat/SignedMessageChain$Decoder;"
            )
    )
    public UUID useRealUuidForSigning(UUID sender) {
        return realProfile().id();
    }

    @ModifyArg(
            method = "resetPlayerChatState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/RemoteChatSession;createMessageDecoder(Ljava/util/UUID;)Lnet/minecraft/network/chat/SignedMessageChain$Decoder;"
            )
    )
    public UUID useRealUuidForUnpacker(UUID sender) {
        return realProfile().id();
    }

    @Inject(
            method = "sendPlayerChatMessage",
            at = @At("HEAD"),
            cancellable = true
    )

    public void sendOnlyUnsignedChatMessages(PlayerChatMessage message, ChatType.Bound params, CallbackInfo ci) {
        ci.cancel();
        sendDisguisedChatMessage(message.decoratedContent(), params);
    }
}
