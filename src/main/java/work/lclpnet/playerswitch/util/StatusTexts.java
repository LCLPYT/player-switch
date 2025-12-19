package work.lclpnet.playerswitch.util;

import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.kibu.translate.text.RootText;
import work.lclpnet.playerswitch.config.Config;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.lang.Math.max;
import static net.minecraft.ChatFormatting.*;
import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;
import static work.lclpnet.playerswitch.util.TimeHelper.formatTime;

public class StatusTexts {

    private final Translations translations;
    private final ConfigManager<Config> configManager;
    private final PlayerUtil playerUtil;
    private final Logger logger;

    @Setter
    private @Nullable PlayerList playerManager = null;

    public StatusTexts(Translations translations, ConfigManager<Config> configManager, PlayerUtil playerUtil, Logger logger) {
        this.translations = translations;
        this.configManager = configManager;
        this.playerUtil = playerUtil;
        this.logger = logger;
    }

    public String getTimeString(long ticks, String language) {
        return formatTime(translations, ticks)
                .translateTo(language)
                .getString();
    }

    public Component currentlyPlaying(String username) {
        Config config = configManager.config();
        String language = config.getMotd().getLanguage();

        int remainingTicks = max(0, config.getSwitchDelayTicks() - config.getElapsedTicks());

        String turnTimeRemaining = getTimeString(remainingTicks, language);

        RootText secondLine = translations.translateText(
                language, "player-switch.motd.now_playing", PlayerUtil.formatUsername(username, config)
        ).formatted(AQUA);

        if (config.getParticipants().size() > 1) {
            return secondLine.append(Component.literal(" (").withStyle(AQUA)
                    .append(translations.translateText(
                            language, "player-switch.motd.time_left", styled(turnTimeRemaining, YELLOW)
                    ))
                    .append(")"));
        }

        return secondLine;
    }

    public Component currentlyWaiting(String username) {
        String language = configManager.config().getMotd().getLanguage();

        return translations.translateText(
                language, "player-switch.motd.waiting", PlayerUtil.formatUsername(username, configManager.config())
        ).formatted(GRAY, ITALIC);
    }

    public Component noParticipants() {
        return Component.literal("No participants configured").withStyle(RED);
    }

    public CompletableFuture<Optional<Component>> prepareStatus() {
        return preloadUsername().map(
                future -> future
                        .thenApply(opt -> Optional.of(getStatusWithUsername(opt.orElse("?"))))
                        .exceptionally(t -> {
                            logger.error("Failed to preload username", t);
                            return Optional.empty();
                        })
        ).orElseGet(() -> CompletableFuture.completedFuture(Optional.of(noParticipants())));
    }

    public Component getStatusWithUsername(String username) {
        var playerManager = this.playerManager;

        if (playerManager == null) {
            return Component.empty();
        }

        if (playerManager.getPlayers().isEmpty()) {
            return currentlyWaiting(username);
        }

        return currentlyPlaying(username);
    }

    private Optional<CompletableFuture<Optional<String>>> preloadUsername() {
        return configManager.config().getCurrentPlayerEntry().map(playerUtil::getUsername);
    }
}
