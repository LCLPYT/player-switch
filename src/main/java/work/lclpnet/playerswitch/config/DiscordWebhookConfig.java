package work.lclpnet.playerswitch.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeComment;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DiscordWebhookConfig {

    @SerdeComment("Whether to enable the discord webhook integration")
    private boolean enabled = false;

    @SerdeComment("The Discord Webhook url. Can be retrieved by creating a Webhook in the \"Integration\" tab of the server settings.")
    private String url = "";

    @SerdeComment("The language to use for Discord messages")
    private String messageLanguage = "en_us";
}
