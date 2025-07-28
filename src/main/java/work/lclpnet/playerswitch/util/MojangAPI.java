package work.lclpnet.playerswitch.util;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MojangAPI {

    private static final String
            PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile/lookup/%s";

    private final HttpClient client;

    public MojangAPI(HttpClient client) {
        this.client = client;
    }

    public CompletableFuture<Optional<String>> getUsername(UUID uuid, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getUsernameSync(uuid);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to fetch username", e);
            }
        }, executor);
    }

    /**
     * Returns the current Minecraft username for the given player UUID.
     * If the player does not exist or an error occurs, returns an empty {@link Optional}.
     *
     * @param uuid The player's UUID
     * @return Optional containing the username, or empty if not found or on error.
     */
    private Optional<String> getUsernameSync(UUID uuid) throws IOException, InterruptedException {
        String strippedUuid = uuid.toString().replace("-", "");
        String url = String.format(PROFILE_URL, strippedUuid);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return Optional.empty();
        }

        String body = response.body();
        JSONObject json = new JSONObject(body);

        return Optional.ofNullable(json.optString("name"));
    }
}
