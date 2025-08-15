package work.lclpnet.playerswitch.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MojangAPI {

    private static final String
            PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile/lookup/%s", 
            UUID_BULK_LOOKUP = "https://api.minecraftservices.com/minecraft/profile/lookup/bulk/byname";

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

    public Optional<Map<String, UUID>> getUuids(Collection<String> names) throws IOException, InterruptedException {
        if (names.size() > 10) throw new IllegalArgumentException("May not request more than 10 uuids at once");

        var array = new JSONArray();
        
        for (String name : names) {
            array.put(name);
        }

        var request = HttpRequest.newBuilder()
                .uri(URI.create(UUID_BULK_LOOKUP))
                .POST(HttpRequest.BodyPublishers.ofString(array.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return Optional.empty();
        }
        
        String body = response.body();
        JSONArray resArray = new JSONArray(body);
        
        Map<String, UUID> uuids = new HashMap<>(resArray.length());

        for (Object obj : resArray) {
            if (!(obj instanceof JSONObject entry)) continue;

            UUID uuid = parseUuidWithoutDashes(entry.getString("id"));
            String name = entry.getString("name");

            uuids.put(name, uuid);
        }

        return Optional.of(uuids);
    }

    public UUID parseUuidWithoutDashes(String hex) {
        if (hex.length() != 32) throw new IllegalArgumentException("Expected hex string of length 32");

        String sb = hex.substring(0, 8) + '-' +
                hex.substring(8, 12) + '-' +
                hex.substring(12, 16) + '-' +
                hex.substring(16, 20) + '-' +
                hex.substring(20, 32);

        return UUID.fromString(sb);
    }
}
