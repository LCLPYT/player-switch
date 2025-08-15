package work.lclpnet.playerswitch.util;

import org.slf4j.Logger;
import work.lclpnet.playerswitch.config.PlayerEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public class PlayerUtil {

    private final Map<UUID, CompletableFuture<Optional<String>>> names = new HashMap<>();
    private final MojangAPI api;
    private final Logger logger;

    public PlayerUtil(MojangAPI api, Logger logger) {
        this.api = api;
        this.logger = logger;
    }

    public synchronized CompletableFuture<Optional<String>> getUsername(UUID uuid) {
        return names.computeIfAbsent(uuid, id -> api.getUsername(uuid, ForkJoinPool.commonPool())
                .<Optional<String>>thenApply(opt -> {
                    if (opt.isPresent()) return opt;

                    synchronized (this) {
                        names.remove(id);
                    }

                    return Optional.empty();
                })
                .exceptionally(t -> {
                    logger.error("Failed to fetch username", t);
                    return Optional.empty();
                }));
    }

    public CompletableFuture<Optional<String>> getUsername(PlayerEntry entry) {
        String subname = entry.getDisplayName();

        if (!subname.isBlank()) {
            return CompletableFuture.completedFuture(Optional.of(subname));
        }

        return getUsername(entry.getUuid());
    }
}
