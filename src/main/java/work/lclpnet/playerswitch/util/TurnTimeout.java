package work.lclpnet.playerswitch.util;

import org.slf4j.Logger;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.playerswitch.config.Config;
import work.lclpnet.playerswitch.config.DiscordBotConfig;
import work.lclpnet.playerswitch.config.PlayerEntry;
import work.lclpnet.playerswitch.util.msg.Messenger;

import java.util.concurrent.TimeUnit;

import static java.lang.Math.max;

public class TurnTimeout {

    public static final long REMINDER_MIN_REMAINING_MILLIS = TimeUnit.MINUTES.toMillis(30);

    private final ConfigManager<Config> configManager;
    private final Messenger messenger;
    private final Logger logger;

    public TurnTimeout(ConfigManager<Config> configManager, Messenger messenger, Logger logger) {
        this.configManager = configManager;
        this.messenger = messenger;
        this.logger = logger;
    }

    public void start() {
        Config config = configManager.config();
        long now = System.currentTimeMillis();

        config.setTurnDeadline(now + TimeUnit.SECONDS.toMillis(config.getTurnTimeoutSeconds()));
        config.setNextTurnReminder(nextReminder(now));
    }

    private long nextReminder(long now) {
        long turnReminderSeconds = configManager.config().getDiscordBot().getTurnReminderSeconds();

        if (turnReminderSeconds <= 0) return 0;

        return now + TimeUnit.SECONDS.toMillis(max(60, turnReminderSeconds));
    }

    public boolean tick() {
        Config config = configManager.config();
        long deadline = config.getTurnDeadline();

        if (deadline <= 0) return false;

        long now = System.currentTimeMillis();

        checkReminders(config, now, deadline);

        if (now < deadline) return false;

        logger.info("Turn timeout was reached.");

        return true;
    }

    private void checkReminders(Config config, long now, long deadline) {
        DiscordBotConfig discordBot = config.getDiscordBot();

        if (!discordBot.isEnabled() || discordBot.getTurnReminderSeconds() <= 0) return;

        long nextReminder = config.getNextTurnReminder();

        if (nextReminder > 0 && now < nextReminder) return;

        config.setNextTurnReminder(nextReminder(now));

        // persist immediately, the config file auto-reloads and would otherwise revert the reminder timestamp
        configManager.save();

        if (nextReminder <= 0 || deadline - now < REMINDER_MIN_REMAINING_MILLIS) return;

        PlayerEntry playerEntry = config.getCurrentPlayerEntry().orElse(null);

        if (playerEntry == null) return;

        messenger.sendTurnReminder(playerEntry);
    }
}
