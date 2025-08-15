package work.lclpnet.playerswitch.util;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.kibu.hook.HookRegistrar;
import work.lclpnet.kibu.hook.player.PlayerConnectionHooks;
import work.lclpnet.kibu.scheduler.api.TaskScheduler;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.playerswitch.PlayerSwitchInit;
import work.lclpnet.playerswitch.config.Config;
import work.lclpnet.playerswitch.config.PlayerEntry;
import work.lclpnet.playerswitch.hook.PlayerCanJoinCallback;
import work.lclpnet.playerswitch.hook.ServerPausedCallback;
import work.lclpnet.playerswitch.hook.ServerTickPauseCallback;
import work.lclpnet.playerswitch.mixin.ServerConfigurationNetworkHandlerAccessor;
import work.lclpnet.playerswitch.type.PlayerSwitchGameProfile;

import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.lang.Math.max;
import static net.minecraft.util.Formatting.*;
import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class SwitchManager {

    private static final int MOTD_UPDATE_TICKS = 20;

    private final ConfigManager<Config> configManager;
    private final Config config;
    private final PlayerUtil playerUtil;
    private final Translations translations;
    private final MinecraftServer server;
    private final DiscordWebhook discordWebhook;
    private final Logger logger;
    private final ServerMotd motd;

    private int motdUpdateTimer = 0;

    public SwitchManager(ConfigManager<Config> configManager, PlayerUtil playerUtil, Translations translations,
                         MinecraftServer server, DiscordWebhook discordWebhook, Logger logger) {
        this.configManager = configManager;
        this.config = configManager.config();
        this.playerUtil = playerUtil;
        this.translations = translations;
        this.server = server;
        this.discordWebhook = discordWebhook;
        this.logger = logger;
        motd = new ServerMotd(server, translations, configManager);
    }

    public boolean setup(TaskScheduler scheduler, HookRegistrar hooks) {
        if (config.getParticipants().isEmpty()) {
            logger.error("No participants are configured. Please modify {}. For more information, see https://github.com/LCLPYT/player-switch", PlayerSwitchInit.configPath());
            return false;
        }

        hooks.registerHook(PlayerCanJoinCallback.HOOK, this::checkCanJoin);
        hooks.registerHook(ServerTickPauseCallback.HOOK, this::shouldPause);
        hooks.registerHook(ServerPausedCallback.HOOK, this::onServerPaused);
        hooks.registerHook(PlayerConnectionHooks.QUIT, this::onPlayerDisconnect);
        ServerLifecycleEvents.BEFORE_SAVE.register(this::onBeforeSave);

        scheduler.interval(this::tick, 1);

        update();

        return true;
    }

    private boolean shouldPause(MinecraftServer s) {
        return PlayerLookup.all(s).isEmpty() || PlayerLookup.all(s).stream().noneMatch(this::isCurrentPlayer);
    }

    public boolean isCurrentPlayer(ServerPlayerEntity player) {
        return config.getCurrentPlayerUuid()
                .map(uuid -> uuid.equals(PlayerUnifier.getRealUuid(player)))
                .orElse(false);
    }

    private Optional<CompletableFuture<Optional<String>>> preloadUsername() {
        return config.getCurrentPlayerEntry().map(playerUtil::getUsername);
    }

    private @Nullable Text checkCanJoin(SocketAddress socketAddress, GameProfile gameProfile) {
        Object networkHandler = ((PlayerSwitchGameProfile) gameProfile).playerSwitch$getNetworkHandler();

        if (!(networkHandler instanceof ServerConfigurationNetworkHandler handler)) return null;

        SyncedClientOptions syncedOptions = ((ServerConfigurationNetworkHandlerAccessor) handler).getSyncedOptions();
        String language = syncedOptions != null ? syncedOptions.language() : "en_us";

        PlayerEntry entry = config.getCurrentPlayerEntry().orElse(null);

        if (entry == null) {
            return translations.translateText(language, "player-switch.not_configured").formatted(RED);
        }

        if (entry.getUuid().equals(gameProfile.getId())) {
            if (PlayerLookup.all(server).isEmpty()) {
                return null;
            }

            return translations.translateText(language, "player-switch.other_online").formatted(YELLOW);
        }

        if (config.getParticipants().stream().noneMatch(pe -> gameProfile.getId().equals(pe.getUuid()))) {
            return translations.translateText(language, "player-switch.not_participating").formatted(RED);
        }

        return playerUtil.getUsername(entry).join()
                .map(name -> translations.translateText(language, "player-switch.other_user_turn", styled(name, YELLOW)).formatted(RED))
                .orElseGet(() -> translations.translateText(language, "player-switch.not_your_turn").formatted(RED));
    }

    private void tick() {
        int ticks = config.getElapsedTicks();
        int ticksLeft = max(0, config.getSwitchDelayTicks() - ticks);

        if (ticksLeft == 0) {
            switchPlayer();
            return;
        }

        if (config.getParticipants().size() > 1) {
            currentPlayer().ifPresent(player ->
                    TimeHelper.formatTime(translations, ticksLeft).formatted(AQUA).sendTo(player, true));
        }

        config.setElapsedTicks(ticks + 1);
        config.setTotalTicks(config.getTotalTicks() + 1);

        if (config.getMotd().isEnabled() && motdUpdateTimer++ >= MOTD_UPDATE_TICKS) {
            motdUpdateTimer = 0;
            updateMotd();
        }
    }

    public Optional<ServerPlayerEntity> currentPlayer() {
        return Optional.ofNullable(server.getPlayerManager().getPlayer(config.getFixedUuid()))
                .filter(player -> PlayerUnifier.getRealUuid(player)
                        .equals(config.getCurrentPlayerUuid().orElse(null)));
    }

    public void update() {
        if (config.getMotd().isEnabled()) {
            updateMotd();
        }

        configManager.save();
    }

    private void updateMotd() {
        preloadUsername().ifPresentOrElse(
                future -> future.thenAccept(opt -> opt.ifPresentOrElse(
                        this::updateMotd,
                        () -> updateMotd("?")
                )).exceptionally(t -> {
                    logger.error("Failed to preload username", t);
                    motd.setMotd(motd.firstLine());
                    return null;
                }),
                motd::noParticipants
        );
    }

    private void updateMotd(String username) {
        if (server.getPlayerManager() == null) return;

        if (PlayerLookup.all(server).isEmpty()) {
            motd.currentlyWaiting(username);
        } else {
            motd.currentlyPlaying(username);
        }
    }

    private void switchPlayer() {
        int currentPlayer = config.getCurrentPlayer();
        int count = config.getParticipants().size();
        int nextPlayer = (currentPlayer + 1) % count;

        if (nextPlayer == currentPlayer) return;

        var prevPlayer = currentPlayer();

        config.setElapsedTicks(0);
        config.setCurrentPlayer(nextPlayer);

        prevPlayer.ifPresent(this::disconnectPlayer);

        update();

        discordWebhook.sendNotification();
    }

    private void disconnectPlayer(ServerPlayerEntity player) {
        Text msg = config.getCurrentPlayerEntry()
                .flatMap(entry -> playerUtil.getUsername(entry).join())
                .map(name -> translations.translateText(player, "player-switch.time_expired_other_user", styled(name, YELLOW)).formatted(GRAY))
                .orElseGet(() -> translations.translateText(player, "player-switch.time_expired").formatted(GRAY));

        player.networkHandler.disconnect(msg);
    }

    private void onPlayerDisconnect(ServerPlayerEntity player) {
        update();
    }

    private void onServerPaused(MinecraftServer server) {
        update();
    }

    private void onBeforeSave(MinecraftServer _server, boolean flush, boolean force) {
        update();
    }
}
