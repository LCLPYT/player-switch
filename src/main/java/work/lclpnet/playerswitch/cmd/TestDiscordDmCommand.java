package work.lclpnet.playerswitch.cmd;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import work.lclpnet.kibu.cmd.type.CommandRegistrar;
import work.lclpnet.kibu.cmd.type.KibuCommand;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.playerswitch.config.Config;
import work.lclpnet.playerswitch.config.PlayerEntry;
import work.lclpnet.playerswitch.util.DiscordBot;

public class TestDiscordDmCommand implements KibuCommand {

    private final DiscordBot discordBot;
    private final Translations translations;
    private final ConfigManager<Config> configManager;

    public TestDiscordDmCommand(DiscordBot discordBot, Translations translations, ConfigManager<Config> configManager) {
        this.discordBot = discordBot;
        this.translations = translations;
        this.configManager = configManager;
    }

    @Override
    public void register(CommandRegistrar commandRegistrar) {
        commandRegistrar.registerCommand(CommandManager.literal("test_discord_bot")
                .requires(s -> s.hasPermissionLevel(4))
                .executes(this::testDiscordCommand));
    }

    private int testDiscordCommand(CommandContext<ServerCommandSource> ctx) {
        for (PlayerEntry participant : configManager.config().getParticipants()) {
            String language = participant.getLanguage();

            if (language.isBlank()) {
                language = "en_us";
            }

            String msg = translations.translate(language, "player-switch.discord.test_dm");

            discordBot.sendDirectMessage(participant.getDiscordId(), msg);
        }

        return 1;
    }
}
