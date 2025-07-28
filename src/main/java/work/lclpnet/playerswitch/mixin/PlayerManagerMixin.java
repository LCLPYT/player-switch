package work.lclpnet.playerswitch.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.lclpnet.playerswitch.hook.PlayerCanJoinCallback;

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
}
