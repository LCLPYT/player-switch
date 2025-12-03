package work.lclpnet.playerswitch.util.queue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.playerswitch.config.PlayerEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class SeamlessPlayerQueueTest {

    private static final Logger logger = LoggerFactory.getLogger(SeamlessPlayerQueueTest.class);

    private Path path;

    @BeforeEach
    public void setup() throws IOException {
        path = Files.createTempFile("queue", ".json");
    }

    @AfterEach
    public void destroy() throws IOException {
        Files.delete(path);
    }

    @Test
    void test_new1p_noErrors() {
        var a = playerEntry("Alpha");

        var participants = List.of(a);

        var queue = new SeamlessPlayerQueue(participants, logger);

        queue.restore(path);

        // current player is indicated by the config, queue only draws the next players
        // assume we start with player A
        queue.sync(a);

        for (int i = 0; i < 1000; i++) {
            assertSame(a, queue.next());
        }
    }

    @RepeatedTest(100)
    void test_new2p_alwaysAlternating() {
        var a = playerEntry("Alpha");
        var b = playerEntry("Bravo");

        var participants = List.of(a, b);

        var queue = new SeamlessPlayerQueue(participants, logger);

        queue.restore(path);

        // current player is indicated by the config, queue only draws the next players
        // assume we start with player A
        queue.sync(a);

        for (int i = 0; i < 1000; i++) {
            assertSame(b, queue.next());
            assertSame(a, queue.next());
        }
    }

    @RepeatedTest(10)
    void test_new3p_neverConsecutive() {
        var a = playerEntry("Alpha");
        var b = playerEntry("Bravo");
        var c = playerEntry("Charlie");

        var participants = List.of(a, b, c);

        var queue = new SeamlessPlayerQueue(participants, logger);

        queue.restore(path);

        // current player is indicated by the config, queue only draws the next players
        // assume we start with player A
        PlayerEntry last = a;

        queue.sync(last);

        for (int i = 0; i < 5000; i++) {
            PlayerEntry next = queue.next();

            assertNotSame(last, next);

            last = next;
        }
    }

    @RepeatedTest(100)
    void test_paused3p_neverConsecutive() {
        var a = playerEntry("Alpha");
        var b = playerEntry("Bravo");
        var c = playerEntry("Charlie");

        var participants = List.of(a, b, c);

        var queue = new SeamlessPlayerQueue(participants, logger);

        queue.restore(path);

        // current player is indicated by the config, queue only draws the next players
        // assume we start with player A
        PlayerEntry last = a;
        queue.sync(last);

        for (int i = 0; i < 5; i++) {
            PlayerEntry next = queue.next();

            assertNotSame(last, next);

            last = next;
        }

        queue.save(path);

        // new queue instance (like server restart)
        queue = new SeamlessPlayerQueue(participants, logger);

        queue.restore(path);
        queue.sync(last);

        for (int i = 0; i < 100; i++) {
            PlayerEntry next = queue.next();

            assertNotSame(last, next);

            last = next;
        }
    }

    private static PlayerEntry playerEntry(String name) {
        var entry = new PlayerEntry();

        entry.setName(name);
        entry.setUuid(UUID.randomUUID());

        return entry;
    }
}