package work.lclpnet.playerswitch.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.kibu.hook.HookRegistrar;
import work.lclpnet.kibu.scheduler.api.TaskScheduler;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.playerswitch.PlayerSwitchInit;
import work.lclpnet.playerswitch.config.Config;
import work.lclpnet.playerswitch.hook.PlayerCanJoinCallback;
import work.lclpnet.playerswitch.mixin.ServerConfigurationNetworkHandlerAccessor;
import work.lclpnet.playerswitch.type.PlayerSwitchGameProfile;

import java.net.SocketAddress;
import java.util.Optional;
import java.util.UUID;

import static net.minecraft.util.Formatting.*;
import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class SwitchManager {

    private final ConfigManager<Config> configManager;
    private final Config config;
    private final PlayerUtil playerUtil;
    private final Translations translations;
    private final MinecraftServer server;
    private final Logger logger;

    public SwitchManager(ConfigManager<Config> configManager, PlayerUtil playerUtil, Translations translations, MinecraftServer server, Logger logger) {
        this.configManager = configManager;
        this.config = configManager.config();
        this.playerUtil = playerUtil;
        this.translations = translations;
        this.server = server;
        this.logger = logger;
    }

    public boolean setup(TaskScheduler scheduler, HookRegistrar hooks) {
        if (config.getParticipants().isEmpty()) {
            logger.error("No participants are configured. Please modify {}", PlayerSwitchInit.configPath());
            return false;
        }

        hooks.registerHook(PlayerCanJoinCallback.HOOK, this::checkCanJoin);

        scheduler.interval(this::tick, 1);

        preloadUsername();

        return true;
    }

    private void preloadUsername() {
        config.getCurrentPlayerUuid().ifPresent(playerUtil::getUsername);
    }

    private @Nullable Text checkCanJoin(SocketAddress socketAddress, GameProfile gameProfile) {
        Object networkHandler = ((PlayerSwitchGameProfile) gameProfile).playerSwitch$getNetworkHandler();

        if (!(networkHandler instanceof ServerConfigurationNetworkHandler handler)) return null;

        SyncedClientOptions syncedOptions = ((ServerConfigurationNetworkHandlerAccessor) handler).getSyncedOptions();
        String language = syncedOptions != null ? syncedOptions.language() : "en_us";

        UUID uuid = config.getCurrentPlayerUuid().orElse(null);

        if (uuid == null) {
            return translations.translateText(language, "player-switch.not_configured").formatted(RED);
        }

        if (uuid.equals(gameProfile.getId())) {
            return null;
        }

        return playerUtil.getUsername(uuid).join()
                .map(name -> translations.translateText(language, "player-switch.other_user_turn", styled(name, YELLOW)).formatted(RED))
                .orElseGet(() -> translations.translateText(language, "player-switch.not_your_turn").formatted(RED));
    }

    private void tick() {
        int ticks = config.getCurrentTicks();
        int ticksLeft = config.getSwitchTicks() - ticks;

        if (ticksLeft <= 0) {
            switchPlayer();
            return;
        }

        config.setCurrentTicks(ticks + 1);
    }

    public Optional<ServerPlayerEntity> currentPlayer() {
        return config.getCurrentPlayerUuid().map(uuid -> server.getPlayerManager().getPlayer(uuid));
    }

    private void switchPlayer() {
        int currentPlayer = config.getCurrentPlayer();
        int count = config.getParticipants().size();
        int nextPlayer = (currentPlayer + 1) % count;

        if (nextPlayer == currentPlayer) return;

        var prevPlayer = currentPlayer();

        config.setCurrentPlayer(nextPlayer);

        prevPlayer.ifPresent(this::disconnectPlayer);

        configManager.save();
    }

    private void disconnectPlayer(ServerPlayerEntity player) {
        Text msg = config.getCurrentPlayerUuid()
                .flatMap(uuid -> playerUtil.getUsername(uuid).join())
                .map(name -> translations.translateText(player, "player-switch.time_expired_other_user", styled(name, YELLOW)).formatted(GRAY))
                .orElseGet(() -> translations.translateText(player, "player-switch.time_expired").formatted(GRAY));

        player.networkHandler.disconnect(msg);
    }
}
