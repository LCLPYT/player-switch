package work.lclpnet.playerswitch.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeComment;
import lombok.Getter;
import lombok.Setter;
import work.lclpnet.kibu.scheduler.Ticks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class Config {

    @SerdeComment("A list of participating players.")
    private List<PlayerEntry> participants = List.of();

    @SerdeComment("The index of the current participating player")
    private int currentPlayer = 0;

    @SerdeComment("The time the current player has already played, in ticks")
    private int elapsedTicks = 0;

    @SerdeComment("The play time after which to switch to the next player, in ticks (1 second = 20 ticks, 1 minute = 1200 ticks, 10 minutes = 12000 ticks ...)")
    private int switchDelayTicks = Ticks.minutes(10);

    @SerdeComment("This UUID will be assigned to every player, so that everyone has the same player and world data")
    private UUID fixedUuid = UUID.fromString("a139e840-ff37-4cc7-a322-896af1a975f9");

    @SerdeComment("This username will be assigned to every player")
    private String fixedUsername = "Player";

    @SerdeComment("Total challenge time, in ticks")
    private long totalTicks = 0;

    @SerdeComment("Limit max players visually")
    private boolean limitMaxPlayers = true;

    @SerdeComment("Message of the day options")
    private MotdConfig motd = new MotdConfig();

    @SerdeComment("A Discord Webhook can be configured so that notifications about new turns are sent to a Discord channel. Participants that have a Discord user ID defined will be pinged when it's their turn.")
    private final DiscordWebhookConfig discordWebhook = new DiscordWebhookConfig();

    @SerdeComment("A Discord bot can be used to send players direct messages when it's their turn.")
    private final DiscordBotConfig discordBot = new DiscordBotConfig();

    @SerdeComment("Player ordering type to use")
    private final QueueType queueType = QueueType.BALANCED_RANDOM;

    @SerdeComment("Ticks since the last switch.")
    private long ticksSinceLastSwitch = 0;

    @SerdeComment("Turn timeout in seconds. If the current player does not play their turn within the timeout, the player will be skipped.")
    private long turnTimeoutSeconds = TimeUnit.DAYS.toSeconds(3);

    @SerdeComment("Code of conduct. Is shown every time a player joins the server.")
    private final CodeOfConductConfig codeOfConduct = new CodeOfConductConfig();

    @SerdeComment("Whether to hide the current player from others")
    private boolean hideCurrentPlayer = false;

    public Optional<PlayerEntry> getCurrentPlayerEntry() {
        if (participants.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(participants.get(currentPlayer));
    }

    public Optional<UUID> getCurrentPlayerUuid() {
        return getCurrentPlayerEntry().map(PlayerEntry::getUuid);
    }

    public int participantIndex(PlayerEntry entry) {
        int i = 0;

        for (PlayerEntry participant : participants) {
            if (entry.equals(participant)) {
                return i;
            }

            i++;
        }

        return -1;
    }
}
