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

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerSwitchInit implements DedicatedServerModInitializer {

	public static final String MOD_ID = "player-switch";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private boolean setupSuccess = true;

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

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			var manager = new SwitchManager(configManager, playerUtil, translations, server, discordWebhook, LOGGER);

			setupSuccess = manager.setup(scheduler, hooks);
		});

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			if (setupSuccess) return;

			LOGGER.error("Shutting down server as player-switch is not configured. For more information, see https://github.com/LCLPYT/player-switch");
			server.stop(false);
		});

		AtomicBoolean destroyed = new AtomicBoolean(false);

		Runnable shutdown = () -> {
			if (destroyed.getAndSet(true)) return;

			configManager.save();
			configManager.close();
			client.close();
		};

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> shutdown.run());

		Runtime.getRuntime().addShutdownHook(new Thread(shutdown, "player-switch shutdown hook"));

		LOGGER.info("Initialized.");
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