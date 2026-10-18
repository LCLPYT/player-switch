package work.lclpnet.playerswitch.util.discord;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores the id of the latest turn notification per discord user.
 * Kept separate from the config, because the config auto-reloads from disk and would revert recently stored ids.
 */
public class TurnMessageStore {

    private final Path path;
    private final Logger logger;
    private final Map<String, String> messageIds = new HashMap<>();

    public TurnMessageStore(Path path, Logger logger) {
        this.path = path;
        this.logger = logger;
    }

    public synchronized void load() {
        messageIds.clear();

        if (!Files.isRegularFile(path)) return;

        try {
            var json = new JSONObject(Files.readString(path, StandardCharsets.UTF_8));

            for (String discordId : json.keySet()) {
                messageIds.put(discordId, json.getString(discordId));
            }
        } catch (IOException | JSONException e) {
            logger.error("Failed to load turn messages from {}", path, e);
        }
    }

    public synchronized void put(String discordId, String messageId) {
        messageIds.put(discordId, messageId);
        save();
    }

    public synchronized @Nullable String take(String discordId) {
        String messageId = messageIds.remove(discordId);

        if (messageId != null) {
            save();
        }

        return messageId;
    }

    public synchronized void clear() {
        messageIds.clear();

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.error("Failed to delete turn messages file {}", path, e);
        }
    }

    private void save() {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, new JSONObject(messageIds).toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to save turn messages to {}", path, e);
        }
    }
}
