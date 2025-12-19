package work.lclpnet.playerswitch.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import work.lclpnet.playerswitch.util.PlayerUnifier;

@Mixin(Player.class)
public class PlayerMixin {

    @ModifyArg(
            method = "getDisplayName",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/scores/PlayerTeam;formatNameForTeam(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;"
            )
    )
    public Component useRealNameForDisplayName(Component name) {
        var self = (Player) (Object) this;

        if (self instanceof ServerPlayer player) {
            GameProfile realProfile = PlayerUnifier.getRealProfile(player);

            return Component.literal(realProfile.name());
        }

        return name;
    }
}
