package work.lclpnet.playerswitch.config;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class PlayerEntry {

    public static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private UUID uuid = NULL_UUID;

    private String name = "";

    private String discordId = "";

    public static PlayerEntry of(String name) {
        var entry = new PlayerEntry();
        entry.setName(name);

        return entry;
    }
}
