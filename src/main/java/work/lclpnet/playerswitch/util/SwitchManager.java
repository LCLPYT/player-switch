package work.lclpnet.playerswitch.util;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerCommonNetworkHandler;
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
import work.lclpnet.playerswitch.hook.*;
import work.lclpnet.playerswitch.mixin.ServerCommonNetworkHandlerAccessor;
import work.lclpnet.playerswitch.mixin.ServerConfigurationNetworkHandlerAccessor;
import work.lclpnet.playerswitch.type.GameProfileCapture;
import work.lclpnet.playerswitch.util.msg.Messenger;
import work.lclpnet.playerswitch.util.queue.PlayerQueue;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.lang.Math.max;
import static net.minecraft.util.Formatting.*;

public class SwitchManager {

    private static final int MOTD_UPDATE_TICKS = 20;

    private final ConfigManager<Config> configManager;
    private final Config config;
    private final PlayerUtil playerUtil;
    private final Translations translations;
    private final MinecraftServer server;
    private final Messenger messenger;
    private final Logger logger;
    private final StatusTexts statusTexts;
    private final ServerMotd motd;
    private final Map<UUID, ServerConfigurationNetworkHandler> handlers = new HashMap<>();
    private final PlayerQueue queue;
    private final TurnTimeout turnTimeout;

    private int motdUpdateTimer = 0;

    public SwitchManager(ConfigManager<Config> configManager, PlayerUtil playerUtil, Translations translations,
                         MinecraftServer server, Messenger messenger, Logger logger, StatusTexts statusTexts,
                         PlayerQueue queue) {

        this.configManager = configManager;
        this.config = configManager.config();
        this.playerUtil = playerUtil;
        this.translations = translations;
        this.server = server;
        this.messenger = messenger;
        this.logger = logger;
        this.statusTexts = statusTexts;
        this.queue = queue;

        motd = new ServerMotd(server, translations, configManager, statusTexts);
        turnTimeout = new TurnTimeout(configManager, logger, this::skipPlayer);
    }

    public boolean setup(TaskScheduler scheduler, HookRegistrar hooks) {
        if (config.getParticipants().isEmpty()) {
            logger.error("No participants are configured. Please modify {}. For more information, see https://github.com/LCLPYT/player-switch", PlayerSwitchInit.configPath());
            return false;
        }

        hooks.registerHook(PlayerBeforeCheckCanJoinCallback.HOOK, this::beforeCheckCanJoin);
        hooks.registerHook(PlayerHandlerDisconnectCallback.HOOK, this::onPlayerHandlerDisconnect);
        hooks.registerHook(PlayerCanJoinCallback.HOOK, this::checkCanJoin);
        hooks.registerHook(PlayerConnectionHooks.JOIN, this::onJoin);
        hooks.registerHook(ServerTickPauseCallback.HOOK, this::shouldPause);
        hooks.registerHook(ServerPausedCallback.HOOK, this::onServerPaused);
        hooks.registerHook(PlayerConnectionHooks.QUIT, this::onPlayerDisconnect);
        hooks.registerHook(ServerMaxPlayersCallback.HOOK, this::modifyMaxPlayers);
        hooks.registerHook(HideOnlinePlayersCallback.HOOK, this::shouldHideOnlinePlayers);

        ServerLifecycleEvents.BEFORE_SAVE.register(this::onBeforeSave);

        scheduler.interval(this::tick, 1);

        update();

        return true;
    }

    private void onJoin(ServerPlayerEntity player) {
        UUID realId = PlayerUnifier.getRealProfile(player).id();
        handlers.remove(realId);
    }

    private boolean shouldHideOnlinePlayers() {
        return config.isHideCurrentPlayer();
    }

    private void onPlayerHandlerDisconnect(ServerCommonNetworkHandler handler) {
        var profile = ((ServerCommonNetworkHandlerAccessor) handler).invokeGetProfile();

        if (profile == null) return;

        UUID id = profile.id();

        if (id == null) return;

        handlers.remove(id);
    }

    private void beforeCheckCanJoin(GameProfile profile, ServerConfigurationNetworkHandler handler) {
        handlers.put(profile.id(), handler);
    }

    private boolean shouldPause(MinecraftServer s) {
        boolean shouldPause = PlayerLookup.all(s).isEmpty() || PlayerLookup.all(s).stream().noneMatch(this::isCurrentPlayer);

        if (shouldPause) {
            turnTimeout.tick();
        }

        return shouldPause;
    }

    public boolean isCurrentPlayer(ServerPlayerEntity player) {
        return config.getCurrentPlayerUuid()
                .map(uuid -> uuid.equals(PlayerUnifier.getRealUuid(player)))
                .orElse(false);
    }

    private @Nullable Text checkCanJoin(SocketAddress socketAddress, PlayerConfigEntry playerEntry) {
        var handler = handlers.get(playerEntry.id());

        if (handler == null) return null;

        SyncedClientOptions syncedOptions = ((ServerConfigurationNetworkHandlerAccessor) handler).getSyncedOptions();
        String language = syncedOptions != null ? syncedOptions.language() : "en_us";

        PlayerEntry entry = config.getCurrentPlayerEntry().orElse(null);

        if (entry == null) {
            return translations.translateText(language, "player-switch.not_configured").formatted(RED);
        }

        if (entry.getUuid().equals(playerEntry.id())) {
            if (PlayerLookup.all(server).isEmpty()) {
                return null;
            }

            return translations.translateText(language, "player-switch.other_online").formatted(YELLOW);
        }

        if (config.getParticipants().stream().noneMatch(pe -> playerEntry.id().equals(pe.getUuid()))) {
            return translations.translateText(language, "player-switch.not_participating").formatted(RED);
        }

        return playerUtil.getUsername(entry).join()
                .map(name -> translations.translateText(language, "player-switch.other_user_turn", PlayerUtil.formatUsername(name, config)).formatted(RED))
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
        statusTexts.prepareStatus().thenAccept(opt -> opt.ifPresentOrElse(
                motd::setStatus,
                () -> motd.setMotd(motd.firstLine())
        ));
    }

    public void skipPlayer() {
        int currentPlayer = config.getCurrentPlayer();

        var participants = config.getParticipants();

        if (participants.size() < 2) return;

        if (currentPlayer >= 0 && currentPlayer < participants.size()) {
            PlayerEntry current = participants.get(currentPlayer);

            logger.info("Skipping player {}'s turn", current.getName());
        } else {
            logger.info("Skipping the turn of the current player");
        }

        switchPlayer();
    }

    private void switchPlayer() {
        config.setTicksSinceLastSwitch(0);
        doSwitchPlayer();
        update();
    }

    private void doSwitchPlayer() {
        int currentPlayer = config.getCurrentPlayer();

        PlayerEntry current = config.getParticipants().get(currentPlayer);
        PlayerEntry next = queue.next();

        saveQueueAsync();

        if (current == next) return;

        int nextPlayer = config.participantIndex(next);

        if (nextPlayer == -1) {
            logger.error("Unknown next participant entry: {}", nextPlayer);
            return;
        }

        var prevPlayer = currentPlayer();

        config.setElapsedTicks(0);
        config.setCurrentPlayer(nextPlayer);
        config.setTurnCount(config.getTurnCount() + 1);

        prevPlayer.ifPresent(this::disconnectPlayer);

        messenger.sendTurnNotification(next);
        messenger.onNextTurn();

        logStatus(current, next);
    }

    private void logStatus(PlayerEntry current, PlayerEntry next) {
        playerUtil.getSafeUsername(current).thenCompose(currentName ->
                playerUtil.getSafeUsername(next).thenAccept(nextName ->
                        logger.info("{}'s turn is over, switching to {}'s turn", currentName, nextName)));
    }

    private void saveQueueAsync() {
        CompletableFuture.runAsync(() -> queue.save(PlayerSwitchInit.getQueuePath()));
    }

    private void disconnectPlayer(ServerPlayerEntity player) {
        Text msg = config.getCurrentPlayerEntry()
                .flatMap(entry -> playerUtil.getUsername(entry).join())
                .map(name -> translations.translateText(player, "player-switch.time_expired_other_user", PlayerUtil.formatUsername(name, config)).formatted(GRAY))
                .orElseGet(() -> translations.translateText(player, "player-switch.time_expired").formatted(GRAY));

        player.networkHandler.disconnect(msg);
    }

    private void onPlayerDisconnect(ServerPlayerEntity player) {
        update();

        var realProfile = GameProfileCapture.get(player.networkHandler).playerSwitch$getRealGameProfile();

        handlers.remove(realProfile.id());
    }

    private void onServerPaused(MinecraftServer server) {
        update();
    }

    private void onBeforeSave(MinecraftServer _server, boolean flush, boolean force) {
        update();
    }

    private int modifyMaxPlayers(int maxPlayers) {
        return config.isLimitMaxPlayers() ? 1 : maxPlayers;
    }
}
