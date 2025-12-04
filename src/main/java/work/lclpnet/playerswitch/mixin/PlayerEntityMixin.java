package work.lclpnet.playerswitch.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import work.lclpnet.playerswitch.util.PlayerUnifier;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @ModifyArg(
            method = "getDisplayName",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/scoreboard/Team;decorateName(Lnet/minecraft/scoreboard/AbstractTeam;Lnet/minecraft/text/Text;)Lnet/minecraft/text/MutableText;"
            )
    )
    public Text useRealNameForDisplayName(Text name) {
        var self = (PlayerEntity) (Object) this;

        if (self instanceof ServerPlayerEntity player) {
            GameProfile realProfile = PlayerUnifier.getRealProfile(player);

            return Text.literal(realProfile.name());
        }

        return name;
    }
}
