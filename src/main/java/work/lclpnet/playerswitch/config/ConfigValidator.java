package work.lclpnet.playerswitch.config;

import org.slf4j.Logger;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.playerswitch.util.MojangAPI;

import java.io.IOException;
import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class ConfigValidator {

    private final ConfigManager<Config> configManager;
    private final MojangAPI mojangAPI;
    private final Logger logger;

    public ConfigValidator(ConfigManager<Config> configManager, MojangAPI mojangAPI, Logger logger) {
        this.configManager = configManager;
        this.mojangAPI = mojangAPI;
        this.logger = logger;
    }

    public void validate() {
        int i = 0;
        List<PlayerEntry> missingUuid = new ArrayList<>();

        for (PlayerEntry entry : configManager.config().getParticipants()) {
            validateEntry(entry, i);

            if (entry.getUuid() == null) {
                missingUuid.add(entry);
            }

            i++;
        }

        if (fetchMissingUuids(missingUuid)) {
            configManager.save();
        }
    }

    private boolean fetchMissingUuids(List<PlayerEntry> missingUuid) {
        if (missingUuid.isEmpty()) return false;

        List<String> names = missingUuid.stream()
                .map(PlayerEntry::getName)
                .map(Objects::requireNonNull)
                .distinct()
                .toList();

        logger.warn("Detected {} participants without a defined UUID. Attempting to fetch the UUIDs by their names...", missingUuid.size());

        Map<String, UUID> uuids = fetchUuids(names);

        for (PlayerEntry entry : missingUuid) {
            String name = Objects.requireNonNull(entry.getName());

            UUID uuid = uuids.get(name);

            if (uuid == null) {
                throw new IllegalArgumentException("Failed to retrieve uuid for player " + name);
            }

            entry.setUuid(uuid);
        }

        return true;
    }

    private Map<String, UUID> fetchUuids(List<String> names) {
        Map<String, UUID> uuids = new HashMap<>();

        final int maxBatchSize = 10;
        int offset = 0;

        while (offset < names.size()) {
            int remain = max(0, names.size() - offset);
            int batch = min(maxBatchSize, remain);

            fetchUuids(names.subList(offset, batch), uuids);

            offset += batch;
        }

        return uuids;
    }

    private void fetchUuids(List<String> names, Map<String, UUID> uuids) {
        logger.info("Fetching UUIDs of players {} ...", names);
        try {
            mojangAPI.getUuids(names)
                    .ifPresentOrElse(uuids::putAll, () -> logger.error("Failed to fetch player uuids for {}", names));
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to fetch player uuids", e);
        }
    }

    private void validateEntry(PlayerEntry entry, int i) {
        if (entry.getUuid() == null && entry.getName() == null) {
            throw new RuntimeException("Invalid config: Participant %s has neither uuid nor name set.".formatted(i));
        }
    }
}
