package work.lclpnet.playerswitch.util;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.playerswitch.config.Config;
import work.lclpnet.playerswitch.config.DiscordBotConfig;
import work.lclpnet.playerswitch.config.PlayerEntry;

import java.util.concurrent.CompletableFuture;

public class DiscordBot {

    private final ConfigManager<Config> configManager;
    private final Translations translations;
    private final Logger logger;
    private @Nullable JDA jda = null;
    private boolean ready = false;

    public DiscordBot(ConfigManager<Config> configManager, Translations translations, Logger logger) {
        this.configManager = configManager;
        this.translations = translations;
        this.logger = logger;
    }

    public CompletableFuture<Void> setup() {
        DiscordBotConfig config = configManager.config().getDiscordBot();

        if (!config.isEnabled()) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            try {
                this.jda = JDABuilder.createLight(config.getToken())
                        .enableIntents(GatewayIntent.DIRECT_MESSAGES)
                        .build()
                        .awaitReady();

                ready = true;
            } catch (InterruptedException e) {
                logger.error("Failed to setup discord bot", e);
            }
        });
    }

    public void shutdown() {
        var jda = this.jda;

        if (jda == null) return;

        ready = false;

        try {
            jda.awaitShutdown();
        } catch (InterruptedException e) {
            jda.shutdownNow();
        }
    }

    public void sendDirectMessage(String userId, String message) {
        var jda = this.jda;

        if (!ready || jda == null) return;

        jda.retrieveUserById(userId)
                .queue(user -> user.openPrivateChannel()
                        .queue(channel -> channel.sendMessage(message).queue()));
    }

    public void sendTurnNotification(PlayerEntry playerEntry) {
        if (!ready) return;

        String discordId = playerEntry.getDiscordId();

        if (discordId.isBlank()) return;

        String language = playerEntry.getLanguage();

        if (language.isBlank()) {
            language = "en_us";
        }

        String msg = translations.translate(language, "player-switch.discord.your_turn");

        sendDirectMessage(discordId, msg);
    }
}
