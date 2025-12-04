package work.lclpnet.playerswitch.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.c2s.config.ReadyC2SPacket;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.SendCodeOfConductTask;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerConfigurationTask;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.playerswitch.hook.CodeOfConductCallback;
import work.lclpnet.playerswitch.hook.GameProfileModificationCallback;
import work.lclpnet.playerswitch.hook.PlayerBeforeCheckCanJoinCallback;
import work.lclpnet.playerswitch.util.PlayerUnifier;

import java.util.Locale;
import java.util.Queue;

@Mixin(ServerConfigurationNetworkHandler.class)
public class ServerConfigurationNetworkHandlerMixin {

    @Shadow @Final private GameProfile profile;

    @Shadow
    @Final
    private Queue<ServerPlayerConfigurationTask> tasks;

    @Shadow
    private SyncedClientOptions syncedOptions;

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ConnectedClientData;gameProfile()Lcom/mojang/authlib/GameProfile;"
            )
    )
    public GameProfile modifyGameProfile(ConnectedClientData instance, Operation<GameProfile> original) {
        GameProfile real = original.call(instance);
        return GameProfileModificationCallback.HOOK.invoker().modifyGameProfile(real);
    }

    @Inject(
            method = "onReady",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;checkCanJoin(Ljava/net/SocketAddress;Lnet/minecraft/server/PlayerConfigEntry;)Lnet/minecraft/text/Text;"
            )
    )
    public void beforeCheckCanJoin(ReadyC2SPacket packet, CallbackInfo ci) {
        ServerConfigurationNetworkHandler self = (ServerConfigurationNetworkHandler) (Object) this;
        PlayerBeforeCheckCanJoinCallback.HOOK.invoker().beforeCheckCanJoin(PlayerUnifier.getRealProfile(profile), self);
    }

    @ModifyArg(
            method = "onReady",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;checkCanJoin(Ljava/net/SocketAddress;Lnet/minecraft/server/PlayerConfigEntry;)Lnet/minecraft/text/Text;"
            )
    )
    public PlayerConfigEntry useRealProfileForCheckCanJoin(PlayerConfigEntry configEntry) {
        return new PlayerConfigEntry(PlayerUnifier.getRealProfile(profile));
    }

    @Inject(
            method = "queueSendResourcePackTask",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;getResourcePackProperties()Ljava/util/Optional;"
            )
    )
    public void enqueueCustomCodeOfConduct(CallbackInfo ci) {
        String lang = syncedOptions.language().toLowerCase(Locale.ROOT);
        String codeOfConduct = CodeOfConductCallback.HOOK.invoker().getCodeOfConductProvider(PlayerUnifier.getRealProfile(profile), lang);

        if (codeOfConduct == null) return;

        tasks.add(new SendCodeOfConductTask(() -> codeOfConduct));
    }
}
