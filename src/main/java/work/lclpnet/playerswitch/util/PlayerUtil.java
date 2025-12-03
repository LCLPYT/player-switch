package work.lclpnet.playerswitch.util;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import work.lclpnet.kibu.translate.text.FormatWrapper;
import work.lclpnet.playerswitch.config.Config;
import work.lclpnet.playerswitch.config.PlayerEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import static net.minecraft.util.Formatting.OBFUSCATED;
import static net.minecraft.util.Formatting.YELLOW;
import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

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

    public CompletableFuture<String> getSafeUsername(PlayerEntry entry) {
        return getUsername(entry).thenApply(opt -> opt.orElseGet(() -> {
            String nameFromConfig = entry.getName();
            return nameFromConfig.isBlank() ? "?" : nameFromConfig;
        }));
    }

    public static @NotNull FormatWrapper formatUsername(String username, Config config) {
        boolean hide = config.isHideCurrentPlayer();

        if (hide) {
            username = "?".repeat(6);
        }

        FormatWrapper formatted = styled(username, YELLOW);

        if (hide) {
            formatted.formatted(OBFUSCATED);
        }

        return formatted;
    }
}
