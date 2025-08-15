package work.lclpnet.playerswitch.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeSkipDeserializingIf;
import com.electronwill.nightconfig.core.serde.annotations.SerdeSkipSerializingIf;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter @Setter
public class PlayerEntry {

    @Nullable
    @SerdeSkipSerializingIf(SerdeSkipSerializingIf.SkipSerIf.IS_NULL)
    @SerdeSkipDeserializingIf(value = {
            SerdeSkipDeserializingIf.SkipDeIf.IS_NULL,
            SerdeSkipDeserializingIf.SkipDeIf.IS_MISSING
    })
    private UUID uuid = null;

    @Nullable
    @SerdeSkipSerializingIf(SerdeSkipSerializingIf.SkipSerIf.IS_NULL)
    @SerdeSkipDeserializingIf(value = {
            SerdeSkipDeserializingIf.SkipDeIf.IS_NULL,
            SerdeSkipDeserializingIf.SkipDeIf.IS_MISSING
    })
    private String name = null;

    @Nullable
    @SerdeSkipSerializingIf(SerdeSkipSerializingIf.SkipSerIf.IS_NULL)
    @SerdeSkipDeserializingIf(value = {
            SerdeSkipDeserializingIf.SkipDeIf.IS_NULL,
            SerdeSkipDeserializingIf.SkipDeIf.IS_MISSING
    })
    private String discordId = null;

    public static PlayerEntry of(String name) {
        var entry = new PlayerEntry();
        entry.setName(name);

        return entry;
    }
}
