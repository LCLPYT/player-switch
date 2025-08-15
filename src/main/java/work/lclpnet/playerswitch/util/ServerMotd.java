package work.lclpnet.playerswitch.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.playerswitch.config.Config;
import work.lclpnet.playerswitch.type.PlayerSwitchMinecraftServer;

import java.util.Arrays;
import java.util.Optional;

import static java.lang.Math.max;
import static net.minecraft.util.Formatting.*;
import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;
import static work.lclpnet.playerswitch.util.TimeHelper.formatTime;

public class ServerMotd {

    private final MinecraftServer server;
    private final Translations translations;
    private final ConfigManager<Config> configManager;

    public ServerMotd(MinecraftServer server, Translations translations, ConfigManager<Config> configManager) {
        this.server = server;
        this.translations = translations;
        this.configManager = configManager;
    }

    public MutableText firstLine() {
        Config config = configManager.config();
        String language = config.getMotd().getLanguage();

        String totalTime = formatTime(translations, config.getTotalTicks())
                .translateTo(language)
                .getString();

        return Text.empty()
                .append(translations.translateText(language, "player-switch.motd.subject").formatted(GREEN))
                .append(Text.literal(" | ").formatted(DARK_GREEN, BOLD))
                .append(Text.literal('«' + totalTime + '»').formatted(GOLD));
    }

    public void currentlyPlaying(String username) {
        Config config = configManager.config();
        String language = config.getMotd().getLanguage();

        int remainingTicks = max(0, config.getSwitchDelayTicks() - config.getElapsedTicks());

        String turnTimeRemaining = formatTime(translations, remainingTicks)
                .translateTo(language)
                .getString();

        var msg = firstLine().append("\n").append(translations.translateText(
                language, "player-switch.motd.now_playing", styled(username, YELLOW)
        ).formatted(AQUA));

        if (config.getParticipants().size() > 1) {
            msg.append(" ").append(translations.translateText(
                    language, "player-switch.motd.time_left", styled(turnTimeRemaining, YELLOW)
            ).formatted(AQUA));
        }

        setMotd(msg);
    }

    public void currentlyWaiting(String username) {
        String language = configManager.config().getMotd().getLanguage();

        var msg = firstLine().append("\n").append(translations.translateText(
                language, "player-switch.motd.waiting", styled(username, YELLOW)
        ).formatted(RED, ITALIC));

        setMotd(msg);
    }

    public void noParticipants() {
        var msg = firstLine()
                .append("\n")
                .append(Text.literal("No participants configured").formatted(RED));

        setMotd(msg);
    }

    public void setMotd(Text motd) {
        String string = convertText(motd);

        server.setMotd(string);

        ((PlayerSwitchMinecraftServer) server).playerSwitch$updateMetadata();
    }

    public static @NotNull String convertText(Text motd) {
        var builder = new StringBuilder();

        motd.asOrderedText().accept(new CharacterVisitor() {
            Style lastStyle = Style.EMPTY;

            @Override
            public boolean accept(int index, Style style, int codePoint) {
                if (!style.equals(lastStyle)) {
                    appendStyle(style, builder);
                    lastStyle = style;
                }

                builder.append((char) codePoint);
                return true;
            }
        });

        return builder.toString();
    }

    private static void appendStyle(Style style, StringBuilder builder) {
        builder.append(RESET);

        TextColor color = style.getColor();

        if (color != null) {
            matchColor(color).ifPresent(builder::append);
        }

        if (style.isBold()) builder.append(BOLD);
        if (style.isItalic()) builder.append(ITALIC);
        if (style.isObfuscated()) builder.append(OBFUSCATED);
        if (style.isUnderlined()) builder.append(UNDERLINE);
        if (style.isStrikethrough()) builder.append(STRIKETHROUGH);
    }

    private static Optional<Formatting> matchColor(TextColor color) {
        String name = color.getName();

        return Arrays.stream(Formatting.values())
                .filter(Formatting::isColor)
                .filter(formatting -> name.equals(formatting.getName()))
                .findAny();
    }
}
