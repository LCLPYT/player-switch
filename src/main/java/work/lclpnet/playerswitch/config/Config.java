package work.lclpnet.playerswitch.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeComment;
import lombok.Getter;
import lombok.Setter;
import work.lclpnet.kibu.scheduler.Ticks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
public class Config {

    @SerdeComment("The uuids of participating players, e.g. [\"00000000-0000-0000-0000-000000000000\"]")
    private List<UUID> participants = List.of();

    @SerdeComment("The index of the current participating player")
    private int currentPlayer = 0;

    @SerdeComment("The time the current player has player, in ticks")
    private int elapsedTicks = 0;

    @SerdeComment("The time after which to switch to the next player, in ticks")
    private int switchDelayTicks = Ticks.minutes(10);

    public Optional<UUID> getCurrentPlayerUuid() {
        if (participants.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(participants.get(currentPlayer));
    }
}
