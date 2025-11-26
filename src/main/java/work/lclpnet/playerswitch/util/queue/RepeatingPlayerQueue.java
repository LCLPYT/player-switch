package work.lclpnet.playerswitch.util.queue;

import org.json.JSONObject;
import org.slf4j.Logger;
import work.lclpnet.playerswitch.config.PlayerEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RepeatingPlayerQueue implements PlayerQueue {

    private final List<PlayerEntry> participants;
    private final Logger logger;
    private int current = 0;

    public RepeatingPlayerQueue(List<PlayerEntry> participants, Logger logger) {
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("Participants must not be empty");
        }

        this.participants = participants;
        this.logger = logger;
    }

    @Override
    public void restore(Path path) {
        String str;

        try {
            str = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read queue json", e);
            return;
        }

        var json = new JSONObject(str);

        current = json.optInt("current", 0);
    }

    @Override
    public void save(Path path) {
        var json = new JSONObject();

        json.put("current", current);

        try {
            Files.writeString(path, json.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to write queue json", e);
        }
    }

    @Override
    public PlayerEntry next() {
        int next = (current + 1) % participants.size();

        current = next;

        return participants.get(next);
    }
}
