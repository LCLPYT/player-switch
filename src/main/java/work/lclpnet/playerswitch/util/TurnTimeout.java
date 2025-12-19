package work.lclpnet.playerswitch.util;

import org.slf4j.Logger;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.playerswitch.config.Config;
import work.lclpnet.playerswitch.config.DiscordBotConfig;
import work.lclpnet.playerswitch.config.PlayerEntry;
import work.lclpnet.playerswitch.util.msg.Messenger;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.max;

public class TurnTimeout {

    public static final long REMINDER_MIN_REMAINING_SECONDS = TimeUnit.MINUTES.toSeconds(30);

    private final ConfigManager<Config> configManager;
    private final Messenger messenger;
    private final Logger logger;
    private final Runnable switchAction;

    public TurnTimeout(ConfigManager<Config> configManager, Messenger messenger, Logger logger, Runnable switchAction) {
        this.configManager = configManager;
        this.messenger = messenger;
        this.logger = logger;
        this.switchAction = switchAction;
    }

    public void tick() {
        Config config = configManager.config();

        Calendar instance = Calendar.getInstance();
        instance.get(Calendar.SECOND);

        long ticksSinceLastSwitch = config.getTicksSinceLastSwitch() + 1;

        config.setTicksSinceLastSwitch(ticksSinceLastSwitch);

        checkReminders();

        if (ticksSinceLastSwitch < config.getTurnTimeoutSeconds() * 20) return;

        logger.info("Turn timeout was reached.");
        switchAction.run();
    }

    private void checkReminders() {
        Config config = configManager.config();
        DiscordBotConfig discordBot = config.getDiscordBot();

        long turnReminderSeconds = discordBot.getTurnReminderSeconds();

        if (!discordBot.isEnabled() || turnReminderSeconds <= 0) return;

        long turnTimeoutTicks = config.getTurnTimeoutSeconds() * 20;
        long ticksSinceLastSwitch = config.getTicksSinceLastSwitch();
        long ticksUntilSkip = turnTimeoutTicks - ticksSinceLastSwitch;

        if (ticksUntilSkip < REMINDER_MIN_REMAINING_SECONDS) return;

        long turnReminderTicks = max(60, turnReminderSeconds) * 20;

        if (ticksSinceLastSwitch % turnReminderTicks != 0) return;

        PlayerEntry playerEntry = config.getCurrentPlayerEntry().orElse(null);

        if (playerEntry == null) return;

        messenger.sendTurnReminder(playerEntry);
    }
}
