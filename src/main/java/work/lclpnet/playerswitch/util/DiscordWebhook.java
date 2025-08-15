package work.lclpnet.playerswitch.util;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.playerswitch.config.Config;
import work.lclpnet.playerswitch.config.PlayerEntry;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhook {

    private final ConfigManager<Config> configManager;
    private final HttpClient httpClient;
    private final Translations translations;
    private final PlayerUtil playerUtil;
    private final Logger logger;

    public DiscordWebhook(ConfigManager<Config> configManager, HttpClient httpClient, Translations translations,
                          PlayerUtil playerUtil, Logger logger) {
        this.configManager = configManager;
        this.httpClient = httpClient;
        this.translations = translations;
        this.playerUtil = playerUtil;
        this.logger = logger;
    }

    public void sendNotification() {
        var config = configManager.config();
        var webhookConfig = config.getDiscordWebhook();

        String uri = webhookConfig.getUrl();

        if (uri.isBlank()) return;

        PlayerEntry playerEntry = config.getCurrentPlayerEntry().orElse(null);

        if (playerEntry == null) return;

        String language = webhookConfig.getMessageLanguage();
        String discordUserId = playerEntry.getDiscordId();

        if (discordUserId.isBlank()) {
            playerUtil.getUsername(playerEntry.getUuid()).thenAccept(opt -> {
                JSONObject msg = opt.or(() -> Optional.of(playerEntry.getName()).filter(s -> !s.isBlank()))
                        .map(username -> getTurnMessage(username, language))
                        .orElseGet(() -> getTurnMessageWithoutUser(language));

                sendAsync(uri, msg.toString());
            });
        } else {
            var msg = getTurnMessageWithMention(discordUserId, language);

            sendAsync(uri, msg.toString());
        }
    }

    private JSONObject getTurnMessageWithoutUser(String language) {
        String msg = translations.translate(language, "player-switch.discord.next_turn");

        return getMessage(msg);
    }

    private JSONObject getTurnMessage(String username, String language) {
        String msg = translations.translate(language, "player-switch.discord.your_turn")
                .formatted(username);

        return getMessage(msg);
    }

    private @NotNull JSONObject getTurnMessageWithMention(String discordUserId, String language) {
        String msg = translations.translate(language, "player-switch.discord.your_turn_mention")
                .formatted(discordUserId);

        var users = new JSONArray();
        users.put(discordUserId);

        var allowedMentions = new JSONObject();
        allowedMentions.put("users", users);

        var json = getMessage(msg);
        json.put("allowed_mentions", allowedMentions);

        return json;
    }

    private @NotNull JSONObject getMessage(String msg) {
        var json = new JSONObject();
        json.put("content", msg);

        return json;
    }

    private void sendAsync(String uri, String content) {
        CompletableFuture.runAsync(() -> {
            try {
                sendWebhookRequest(uri, content);
            } catch (IOException | InterruptedException e) {
                logger.error("Failed to send Discord webhook message", e);
            }
        });
    }

    private void sendWebhookRequest(String uri, String content) throws IOException, InterruptedException {
        logger.debug("Sending Discord webhook message...");

        var request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .POST(HttpRequest.BodyPublishers.ofString(content))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 204) {
            logger.error("Unexpected HTTP Status code of discord webhook response: {}", response.statusCode());
            logger.debug("Discord webhook response body: {}", response.body());
            return;
        }

        logger.debug("Discord webhook message successfully sent");
    }
}
