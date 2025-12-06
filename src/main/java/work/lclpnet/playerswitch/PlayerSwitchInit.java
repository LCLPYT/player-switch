package work.lclpnet.playerswitch;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.kibu.cmd.impl.CommandContainer;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.kibu.hook.HookContainer;
import work.lclpnet.kibu.scheduler.KibuScheduling;
import work.lclpnet.kibu.scheduler.api.Scheduler;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.kibu.translate.util.LocaleUtil;
import work.lclpnet.kibu.translate.util.ModTranslations;
import work.lclpnet.playerswitch.cmd.ResetRunCommand;
import work.lclpnet.playerswitch.cmd.SkipCommand;
import work.lclpnet.playerswitch.cmd.TestDiscordDmCommand;
import work.lclpnet.playerswitch.config.Config;
import work.lclpnet.playerswitch.config.ConfigValidator;
import work.lclpnet.playerswitch.config.PlayerEntry;
import work.lclpnet.playerswitch.hook.CodeOfConductCallback;
import work.lclpnet.playerswitch.util.*;
import work.lclpnet.playerswitch.util.msg.Messenger;
import work.lclpnet.playerswitch.util.queue.PlayerQueue;
import work.lclpnet.playerswitch.util.queue.RepeatingPlayerQueue;
import work.lclpnet.playerswitch.util.queue.SeamlessPlayerQueue;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerSwitchInit implements DedicatedServerModInitializer {

	public static final String MOD_ID = "player-switch";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private @Nullable String levelName = null;

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

        var statusTexts = new StatusTexts(translations, configManager, playerUtil, LOGGER);

        var discordWebhook = new DiscordWebhook(configManager, client, translations, playerUtil, LOGGER);
        var discordBot = new DiscordBot(configManager, translations, statusTexts, LOGGER);

        discordBot.setup().join();

        var messenger = new Messenger(discordWebhook, discordBot);

        var queue = loadQueue(configManager, messenger);

        AtomicBoolean resetRun = new AtomicBoolean(false);

        var container = new CommandContainer();

        new TestDiscordDmCommand(discordBot, translations, configManager).register(container);
        new ResetRunCommand(resetRun).register(container);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            levelName = server.getSaveProperties().getLevelName();

            statusTexts.setPlayerManager(server.getPlayerManager());

            var manager = new SwitchManager(configManager, playerUtil, translations, server, messenger, LOGGER, statusTexts, queue);

            boolean setupSuccess = manager.setup(scheduler, hooks);

			if (setupSuccess) {
                new SkipCommand(manager).register(container);

                discordBot.updateActivityStatus();

                logStatus(configManager, playerUtil);
                return;
            }

			LOGGER.error("Shutting down server as player-switch is not configured. For more information, see https://github.com/LCLPYT/player-switch");
			server.stop(false);
		});


        handleCodeOfConduct(configManager);

        AtomicBoolean destroyed = new AtomicBoolean(false);

		Runnable shutdown = () -> {
			if (destroyed.getAndSet(true)) return;

            boolean doResetRun = resetRun.get();

            if (doResetRun) {
                resetRun(configManager.config());
            }

			configManager.save();
			configManager.close();
			client.close();

            if (!doResetRun) {
                queue.save(getQueuePath());
            }

            discordBot.shutdown();
		};

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> shutdown.run());

		Runtime.getRuntime().addShutdownHook(new Thread(shutdown, "player-switch shutdown hook"));

		LOGGER.info("Initialized.");
	}

    private void resetRun(Config config) {
        config.reset();

        try {
            Files.deleteIfExists(getQueuePath());
        } catch (IOException e) {
            LOGGER.error("Failed to delete queue path", e);
        }

        String levelName = this.levelName;

        if (levelName == null) return;

        Path dir = Path.of(levelName);

        try (var files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(dir))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            LOGGER.error("Failed to delete world", e);
        }
    }

    private void handleCodeOfConduct(ConfigManager<Config> configManager) {
        CodeOfConductCallback.HOOK.register((profile, lang) -> {
            Config config = configManager.config();

            var codeOfConduct = config.getCodeOfConduct();

            if (!codeOfConduct.isEnabled() || config.getCurrentPlayerUuid()
                    .filter(uuid -> uuid.equals(profile.id())).isEmpty())
                return null;

            var languages = codeOfConduct.getLanguages();

            String text = languages.get(lang);

            if (text == null && !languages.isEmpty()) {
                text = languages.values().iterator().next();
            }

            if (text == null) return null;

            ZonedDateTime now = ZonedDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
                    .withLocale(LocaleUtil.getLocale(lang));

            return text.concat("\n(%s)".formatted(now.format(formatter)));
        });
    }

    private void logStatus(ConfigManager<Config> configManager, PlayerUtil playerUtil) {
        var playerEntry = configManager.config().getCurrentPlayerEntry().orElse(null);

        if (playerEntry == null) {
            LOGGER.warn("It's no player's turn currently...");
            return;
        }

        playerUtil.getSafeUsername(playerEntry).thenAccept(name -> LOGGER.info("Currently, it's {}'s turn", name));
    }

    private PlayerQueue loadQueue(ConfigManager<Config> configManager, Messenger messenger) {
        var config = configManager.config();
        var participants = config.getParticipants();

        PlayerQueue queue = switch (config.getQueueType()) {
            case REPEATING -> new RepeatingPlayerQueue(participants, LOGGER);
            case BALANCED_RANDOM -> new SeamlessPlayerQueue(participants, LOGGER);
        };

        queue.restore(getQueuePath());

        if (config.getCurrentPlayer() < 0) {
            chooseInitialPlayer(queue, config, messenger);
        }

        config.getCurrentPlayerEntry().ifPresent(queue::sync);

        return queue;
    }

    private void chooseInitialPlayer(PlayerQueue queue, Config config, Messenger messenger) {
        LOGGER.info("It's nobody turn currently, choosing initial participant entry...");

        PlayerEntry initial = queue.next();

        LOGGER.info("Chose {} as initial participant entry", initial);

        int index = config.participantIndex(initial);

        if (index == -1) {
            LOGGER.error("Unknown initial participant entry: {}", index);
            index = 0;
        }

        config.setCurrentPlayer(index);

        queue.save(getQueuePath());

        messenger.sendTurnNotification(initial);
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