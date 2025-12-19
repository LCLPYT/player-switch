package work.lclpnet.playerswitch.util.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.kibu.translate.util.LocaleUtil;
import work.lclpnet.playerswitch.config.Config;
import work.lclpnet.playerswitch.config.DiscordBotConfig;
import work.lclpnet.playerswitch.config.PlayerEntry;
import work.lclpnet.playerswitch.util.StatusTexts;
import work.lclpnet.playerswitch.util.SwitchManager;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class DiscordBot {

    public static final String SKIP_TURN_BUTTON_ID = "skip_turn";
    private final ConfigManager<Config> configManager;
    private final Translations translations;
    private final StatusTexts statusTexts;
    private final Logger logger;
    private @Nullable JDA jda = null;
    private @Nullable DiscordBotListener listener = null;
    private boolean ready = false;

    public DiscordBot(ConfigManager<Config> configManager, Translations translations, StatusTexts statusTexts, Logger logger) {
        this.configManager = configManager;
        this.translations = translations;
        this.statusTexts = statusTexts;
        this.logger = logger;
    }

    public CompletableFuture<Void> setup() {
        DiscordBotConfig config = botConfig();

        if (!config.isEnabled()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                var listener = new DiscordBotListener(translations, configManager);

                this.jda = JDABuilder.createLight(config.getToken())
                        .enableIntents(GatewayIntent.DIRECT_MESSAGES)
                        .addEventListeners(listener)
                        .build()
                        .awaitReady();

                this.listener = listener;

                ready = true;
            } catch (InterruptedException e) {
                logger.error("Failed to setup discord bot", e);
            }
        });
    }

    private DiscordBotConfig botConfig() {
        return configManager.config().getDiscordBot();
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

    public void sendDirectMessage(PlayerEntry playerEntry, String message) {
        sendDirectMessage(playerEntry, message, false, UnaryOperator.identity());
    }

    public void sendDirectMessage(PlayerEntry playerEntry, String message, boolean storeRef, UnaryOperator<MessageCreateAction> processor) {
        String userId = playerEntry.getDiscordId();
        var jda = this.jda;

        if (!ready || jda == null || userId.isBlank()) return;

        try {
            jda.retrieveUserById(userId)
                    .queue(user -> user.openPrivateChannel()
                            .queue(channel -> processor.apply(channel.sendMessage(message))
                                    .queue(sentMsg -> {
                                        if (!storeRef) return;

                                        storeMessageReference(sentMsg, userId);
                                    })));
        } catch (Throwable t) {
            logger.error("Failed to send discord direct message to user '{}'", userId, t);
        }
    }

    private void storeMessageReference(Message sentMsg, String userId) {
        // need to get the fresh instance of the player entry, since the instance could have been swapped since
        PlayerEntry freshEntry = configManager.config().getPlayerEntryByDiscordId(userId).orElse(null);

        if (freshEntry == null) return;

        freshEntry.setLastInteractionMessageId(sentMsg.getId());
        configManager.save();
    }

    public void sendTurnNotification(PlayerEntry playerEntry) {
        removeSkipButtonFromLastInteraction(playerEntry);
        sendTurnNotification(playerEntry, "player-switch.discord.your_turn");
    }

    public void sendTurnReminder(PlayerEntry playerEntry) {
        modifyLastInteraction(playerEntry, msg -> msg.delete().queue());
        sendTurnNotification(playerEntry, "player-switch.discord.turn_reminder");
    }

    private void sendTurnNotification(PlayerEntry playerEntry, String messageKey) {
        if (!ready) return;

        String discordId = playerEntry.getDiscordId();

        if (discordId.isBlank()) return;

        String language = playerEntry.getSafeLanguage();
        Locale locale = LocaleUtil.getLocale(language);

        Config config = configManager.config();

        var deadline = Instant.now().plusSeconds(config.getTurnTimeoutSeconds());
        var dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale)
                .withZone(ZoneId.systemDefault());

        String deadlineFormatted = dateFormatter.format(deadline);

        String skipLabel = translations.translate(language, "player-switch.discord.skip_turn_label");
        String msg = translations.translate(language, messageKey, deadlineFormatted, skipLabel);

        sendDirectMessage(playerEntry, msg, true, action -> action
                .addComponents(ActionRow.of(Button.danger(SKIP_TURN_BUTTON_ID, skipLabel))));
    }

    public void sendSkipNotification(PlayerEntry playerEntry) {
        if (!ready) return;

        String language = playerEntry.getSafeLanguage();
        String msg = translations.translate(language, "player-switch.discord.skipped");

        sendDirectMessage(playerEntry, msg);
    }

    public void setActivity(String status) {
        var jda = this.jda;

        if (jda == null || !ready || !botConfig().isUseStatusAsActivity()) return;

        jda.getPresence().setActivity(Activity.customStatus(status));
    }

    public void updateActivityStatus() {
        if (jda == null || !ready || !botConfig().isUseStatusAsActivity()) return;

        Config config = configManager.config();
        String language = botConfig().getLanguage();

        String totalTime = statusTexts.getTimeString(config.getTotalTicks(), language);

        String stats = translations.translate(
                language,
                "player-switch.discord.bot_status.turn_stats",
                config.getTurnCount(),
                totalTime
        );

        if (config.isHideCurrentPlayer()) {
            setActivity(stats);
            return;
        }

        statusTexts.prepareStatus().thenAccept(opt -> opt.ifPresentOrElse(
                status -> {
                    String activity = translations.translate(
                            language,
                            "player-switch.discord.bot_status.turn_status",
                            status.getString(),
                            stats
                    );

                    setActivity(activity);
                },
                () -> setActivity(stats)
        ));
    }

    public void setSwitchManager(SwitchManager manager) {
        DiscordBotListener listener = this.listener;

        if (listener != null) {
            listener.setSwitchManager(manager);
        }
    }

    public void removeSkipButtonFromLastInteraction(PlayerEntry playerEntry) {
        modifyLastInteraction(playerEntry, msg -> msg.editMessageComponents().queue());
    }

    public void modifyLastInteraction(PlayerEntry playerEntry, Consumer<Message> action) {
        String userId = playerEntry.getDiscordId();
        String messageId = playerEntry.getLastInteractionMessageId();

        var jda = this.jda;

        if (!ready || jda == null || userId.isBlank() || messageId.isBlank()) return;

        try {
            jda.retrieveUserById(userId)
                    .queue(user -> user.openPrivateChannel()
                            .queue(channel -> channel.retrieveMessageById(messageId)
                                    .queue(action)));
        } catch (Throwable t) {
            logger.error("Failed to send discord direct message to user '{}'", userId, t);
        }

        playerEntry.setLastInteractionMessageId("");
    }
}
