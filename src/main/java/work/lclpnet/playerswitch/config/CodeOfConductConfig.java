package work.lclpnet.playerswitch.config;

import com.electronwill.nightconfig.core.serde.annotations.SerdeComment;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CodeOfConductConfig {

    @SerdeComment("Whether to enable the code of conduct feature")
    private boolean enabled = false;

    @SerdeComment("Localized code of conduct text")
    private Map<String, String> languages = Map.of("en_us", "Hello", "de_de", "Hallo");
}
