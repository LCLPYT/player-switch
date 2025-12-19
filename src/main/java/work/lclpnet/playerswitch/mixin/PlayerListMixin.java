package work.lclpnet.playerswitch.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.playerswitch.hook.PlayerCanJoinCallback;
import work.lclpnet.playerswitch.util.PlayerUnifier;

import java.net.SocketAddress;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Inject(
            method = "canPlayerLogin",
            at = @At("RETURN"),
            cancellable = true
    )
    public void checkCanJoin(SocketAddress address, NameAndId configEntry, CallbackInfoReturnable<Component> cir) {
        if (cir.getReturnValue() != null) return;

        Component msg = PlayerCanJoinCallback.HOOK.invoker().checkCanJoin(address, configEntry);

        cir.setReturnValue(msg);
    }

    @ModifyArg(
            method = "sendPlayerPermissionLevel(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;getProfilePermissions(Lnet/minecraft/server/players/NameAndId;)I"
            )
    )
    public NameAndId useRealGameProfileForPermissions(NameAndId playerEntry, @Local(argsOnly = true) ServerPlayer player) {
        return new NameAndId(PlayerUnifier.getRealProfile(player.connection));
    }
}
