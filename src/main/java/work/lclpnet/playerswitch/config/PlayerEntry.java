package work.lclpnet.playerswitch.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeSkipDeserializingIf;
import com.electronwill.nightconfig.core.serde.annotations.SerdeSkipSerializingIf;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class PlayerEntry {

    public static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @SerdeSkipDeserializingIf(SerdeSkipDeserializingIf.SkipDeIf.IS_MISSING)
    @SerdeSkipSerializingIf(SerdeSkipSerializingIf.SkipSerIf.IS_NULL)
    private UUID uuid = NULL_UUID;

    @SerdeSkipDeserializingIf(SerdeSkipDeserializingIf.SkipDeIf.IS_MISSING)
    @SerdeSkipSerializingIf(SerdeSkipSerializingIf.SkipSerIf.IS_NULL)
    private String name = "";

    @SerdeSkipDeserializingIf(SerdeSkipDeserializingIf.SkipDeIf.IS_MISSING)
    @SerdeSkipSerializingIf(SerdeSkipSerializingIf.SkipSerIf.IS_NULL)
    private String discordId = "";

    public static PlayerEntry of(String name) {
        var entry = new PlayerEntry();
        entry.setName(name);

        return entry;
    }
}
