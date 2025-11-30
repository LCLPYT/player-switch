package work.lclpnet.playerswitch.util;

import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.playerswitch.config.Config;

import java.util.Calendar;

public class TurnTimeout {

    private final ConfigManager<Config> configManager;
    private final Runnable switchAction;

    public TurnTimeout(ConfigManager<Config> configManager, Runnable switchAction) {
        this.configManager = configManager;
        this.switchAction = switchAction;
    }

    public void tick() {
        Config config = configManager.config();

        Calendar instance = Calendar.getInstance();
        instance.get(Calendar.SECOND);

        long ticksSinceLastSwitch = config.getTicksSinceLastSwitch() + 1;

        config.setTicksSinceLastSwitch(ticksSinceLastSwitch);

        if (ticksSinceLastSwitch < config.getTurnTimeoutSeconds() * 20) return;

        switchAction.run();
    }
}
