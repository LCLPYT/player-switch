package work.lclpnet.playerswitch.util;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static net.minecraft.ChatFormatting.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerMotdTest {

    @Test
    void convertText() {
        var text = Component.literal("test ")
                .append(Component.literal("123 ").withStyle(RED))
                .append(Component.literal("foo ").withStyle(BOLD, BLUE))
                .append("bar");

        assertEquals("test §r§c123 §r§9§lfoo §rbar", ServerMotd.convertText(text));
    }
}