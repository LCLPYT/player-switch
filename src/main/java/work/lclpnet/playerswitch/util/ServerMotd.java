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

        String totalTime = formatTime(translations, config.getTotalTicks())
                .translateTo("en_us")
                .getString();

        return Text.empty()
                .append(Text.literal("Player-Switch challenge").formatted(GREEN))
                .append(Text.literal(" | ").formatted(DARK_GREEN, BOLD))
                .append(Text.literal('«' + totalTime + '»').formatted(GOLD));
    }

    public void currentlyPlaying(String username) {
        Config config = configManager.config();

        int remainingTicks = max(0, config.getSwitchDelayTicks() - config.getElapsedTicks());

        String turnTimeRemaining = formatTime(translations, remainingTicks)
                .translateTo("en_us")
                .getString();

        var msg = firstLine()
                .append("\n")
                .append(Text.literal("Now playing: ").formatted(AQUA))
                .append(Text.literal(username).formatted(YELLOW));

        if (config.getParticipants().size() > 1) {
            msg.append(Text.literal(" ").formatted(AQUA)
                    .append("(")
                    .append(Text.literal(turnTimeRemaining).formatted(YELLOW))
                    .append(" left)"));
        }

        setMotd(msg);
    }

    public void currentlyWaiting(String username) {
        var msg = firstLine()
                .append("\n")
                .append(Text.literal("Waiting for: ").formatted(AQUA))
                .append(Text.literal(username).formatted(YELLOW));

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
