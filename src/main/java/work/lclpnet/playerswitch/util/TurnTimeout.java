package work.lclpnet.playerswitch.util;

import org.slf4j.Logger;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.playerswitch.config.Config;

import java.util.Calendar;

public class TurnTimeout {

    private final ConfigManager<Config> configManager;
    private final Logger logger;
    private final Runnable switchAction;

    public TurnTimeout(ConfigManager<Config> configManager, Logger logger, Runnable switchAction) {
        this.configManager = configManager;
        this.logger = logger;
        this.switchAction = switchAction;
    }

    public void tick() {
        Config config = configManager.config();

        Calendar instance = Calendar.getInstance();
        instance.get(Calendar.SECOND);

        long ticksSinceLastSwitch = config.getTicksSinceLastSwitch() + 1;

        config.setTicksSinceLastSwitch(ticksSinceLastSwitch);

        if (ticksSinceLastSwitch < config.getTurnTimeoutSeconds() * 20) return;

        logger.info("Turn timeout was reached.");
        switchAction.run();
    }
}
