package work.lclpnet.playerswitch.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeComment;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DiscordBotConfig {

    @SerdeComment("Whether to enable the discord bot integration")
    private boolean enabled = false;

    @SerdeComment("Discord bot secret token")
    private String token = "";

    @SerdeComment("Display info about the run via the activity status of the bot")
    private boolean useStatusAsActivity = true;

    @SerdeComment("Language for general messages, such as activity status")
    private String language = "en_us";
}
