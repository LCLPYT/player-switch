package work.lclpnet.playerswitch.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.config.ServerCodeOfConductConfigurationTask;
import net.minecraft.server.players.NameAndId;
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

@Mixin(ServerConfigurationPacketListenerImpl.class)
public class ServerConfigurationPacketListenerImplMixin {

    @Shadow @Final private GameProfile gameProfile;

    @Shadow
    @Final
    private Queue<ConfigurationTask> configurationTasks;

    @Shadow
    private ClientInformation clientInformation;

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/CommonListenerCookie;gameProfile()Lcom/mojang/authlib/GameProfile;"
            )
    )
    public GameProfile modifyGameProfile(CommonListenerCookie instance, Operation<GameProfile> original) {
        GameProfile real = original.call(instance);
        return GameProfileModificationCallback.HOOK.invoker().modifyGameProfile(real);
    }

    @Inject(
            method = "handleConfigurationFinished",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lnet/minecraft/server/players/NameAndId;)Lnet/minecraft/network/chat/Component;"
            )
    )
    public void beforeCheckCanJoin(ServerboundFinishConfigurationPacket packet, CallbackInfo ci) {
        ServerConfigurationPacketListenerImpl self = (ServerConfigurationPacketListenerImpl) (Object) this;
        PlayerBeforeCheckCanJoinCallback.HOOK.invoker().beforeCheckCanJoin(PlayerUnifier.getRealProfile(gameProfile), self);
    }

    @ModifyArg(
            method = "handleConfigurationFinished",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lnet/minecraft/server/players/NameAndId;)Lnet/minecraft/network/chat/Component;"
            )
    )
    public NameAndId useRealProfileForCheckCanJoin(NameAndId configEntry) {
        return new NameAndId(PlayerUnifier.getRealProfile(gameProfile));
    }

    @Inject(
            method = "addOptionalTasks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;getServerResourcePack()Ljava/util/Optional;"
            )
    )
    public void enqueueCustomCodeOfConduct(CallbackInfo ci) {
        String lang = clientInformation.language().toLowerCase(Locale.ROOT);
        String codeOfConduct = CodeOfConductCallback.HOOK.invoker().getCodeOfConductProvider(PlayerUnifier.getRealProfile(gameProfile), lang);

        if (codeOfConduct == null) return;

        configurationTasks.add(new ServerCodeOfConductConfigurationTask(() -> codeOfConduct));
    }
}
