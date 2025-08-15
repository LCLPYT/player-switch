package work.lclpnet.playerswitch.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeComment;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MotdConfig {

    @SerdeComment("Whether to set the message of the day (MOTD) according to the player whose turn it is.")
    private boolean enabled = true;

    @SerdeComment("The language to use for the message of the day")
    private String language = "en_us";
}
