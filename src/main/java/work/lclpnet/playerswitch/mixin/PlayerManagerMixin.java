package work.lclpnet.playerswitch.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.playerswitch.hook.PlayerCanJoinCallback;
import work.lclpnet.playerswitch.util.PlayerUnifier;

import java.net.SocketAddress;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(
            method = "checkCanJoin",
            at = @At("RETURN"),
            cancellable = true
    )
    public void checkCanJoin(SocketAddress address, GameProfile profile, CallbackInfoReturnable<Text> cir) {
        if (cir.getReturnValue() != null) return;

        Text msg = PlayerCanJoinCallback.HOOK.invoker().checkCanJoin(address, profile);

        cir.setReturnValue(msg);
    }

    @ModifyArg(
            method = "sendCommandTree(Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;getPermissionLevel(Lcom/mojang/authlib/GameProfile;)I"
            )
    )
    public GameProfile useRealGameProfileForPermissions(GameProfile profile, @Local(argsOnly = true) ServerPlayerEntity player) {
        return PlayerUnifier.getRealProfile(player.networkHandler);
    }
}
