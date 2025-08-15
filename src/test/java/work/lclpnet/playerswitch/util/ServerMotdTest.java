package work.lclpnet.playerswitch.util;

import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import static net.minecraft.util.Formatting.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerMotdTest {

    @Test
    void convertText() {
        var text = Text.literal("test ")
                .append(Text.literal("123 ").formatted(RED))
                .append(Text.literal("foo ").formatted(BOLD, BLUE))
                .append("bar");

        assertEquals("test §r§c123 §r§9§lfoo §rbar", ServerMotd.convertText(text));
    }
}