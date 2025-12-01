package work.lclpnet.playerswitch;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.kibu.hook.HookContainer;
import work.lclpnet.kibu.scheduler.KibuScheduling;
import work.lclpnet.kibu.scheduler.api.Scheduler;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.kibu.translate.util.ModTranslations;
import work.lclpnet.playerswitch.config.Config;
import work.lclpnet.playerswitch.config.ConfigValidator;
import work.lclpnet.playerswitch.util.*;
import work.lclpnet.playerswitch.util.msg.Messenger;
import work.lclpnet.playerswitch.util.queue.PlayerQueue;
import work.lclpnet.playerswitch.util.queue.RepeatingPlayerQueue;
import work.lclpnet.playerswitch.util.queue.SeamlessPlayerQueue;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerSwitchInit implements DedicatedServerModInitializer {

	public static final String MOD_ID = "player-switch";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeServer() {
        var client = HttpClient.newHttpClient();
        var api = new MojangAPI(client);

        var configManager = loadConfig(api);

		var scheduler = new Scheduler(LOGGER);
		KibuScheduling.getRootScheduler().addChild(scheduler);
		Translations translations = getTranslations();

		var hooks = new HookContainer();

		var playerUtil = new PlayerUtil(api, LOGGER);

		var unifier = new PlayerUnifier(configManager.config());
		unifier.setup(hooks);

        var discordWebhook = new DiscordWebhook(configManager, client, translations, playerUtil, LOGGER);
        var discordBot = new DiscordBot(configManager, translations, LOGGER);

        discordBot.setup().join();

        var messenger = new Messenger(discordWebhook, discordBot);

        var queue = loadQueue(configManager);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var manager = new SwitchManager(configManager, playerUtil, translations, server, messenger, LOGGER, queue);

            boolean setupSuccess = manager.setup(scheduler, hooks);

			if (setupSuccess) {
                logStatus(configManager, playerUtil);
                return;
            }

			LOGGER.error("Shutting down server as player-switch is not configured. For more information, see https://github.com/LCLPYT/player-switch");
			server.stop(false);
		});

		AtomicBoolean destroyed = new AtomicBoolean(false);

		Runnable shutdown = () -> {
			if (destroyed.getAndSet(true)) return;

			configManager.save();
			configManager.close();
			client.close();

            queue.save(getQueuePath());

            discordBot.shutdown();
		};

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> shutdown.run());

		Runtime.getRuntime().addShutdownHook(new Thread(shutdown, "player-switch shutdown hook"));

		LOGGER.info("Initialized.");
	}

    private void logStatus(ConfigManager<Config> configManager, PlayerUtil playerUtil) {
        var playerEntry = configManager.config().getCurrentPlayerEntry().orElse(null);

        if (playerEntry == null) {
            LOGGER.warn("It's no player's turn currently...");
            return;
        }

        playerUtil.getSafeUsername(playerEntry).thenAccept(name -> LOGGER.info("Currently, it's {}'s turn", name));
    }

    private PlayerQueue loadQueue(ConfigManager<Config> configManager) {
        var config = configManager.config();
        var participants = config.getParticipants();

        PlayerQueue queue = switch (config.getQueueType()) {
            case REPEATING -> new RepeatingPlayerQueue(participants, LOGGER);
            case BALANCED_RANDOM -> new SeamlessPlayerQueue(participants, LOGGER);
        };

        queue.restore(getQueuePath());

        return queue;
    }

    private ConfigManager<Config> loadConfig(MojangAPI api) {
		Path configPath = configPath();

		var configManager = new ConfigManager<>(configPath, new Config());

		configManager.load();

        var validator = new ConfigValidator(configManager, api, LOGGER);
        validator.validate();

        configManager.onChanged(validator::validate);

		return configManager;
	}

    public static @NotNull Path configPath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve(MOD_ID)
                .resolve("config.toml");
	}

    public static @NotNull Path getQueuePath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve(MOD_ID)
                .resolve("queue.json");
    }

	private static Translations getTranslations() {
		var result = ModTranslations.fromAssets(MOD_ID, LOGGER);
		Translations translations = result.translations();

		result.whenLoaded().thenRun(() -> LOGGER.info("{} translations loaded.", MOD_ID));

		return translations;
	}

	/**
	 * Creates an identifier namespaced with the identifier of the mod.
	 * @param path The path.
	 * @return An identifier of this mod with the given path.
	 */
	public static Identifier identifier(String path) {
		return Identifier.of(MOD_ID, path);
	}
}