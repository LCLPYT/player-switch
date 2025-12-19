package work.lclpnet.playerswitch.util.queue;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.UUIDUtil;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import work.lclpnet.gaco.ds.queue.JsonFileQueuePersistence;
import work.lclpnet.gaco.ds.queue.QueueTransfer;
import work.lclpnet.gaco.ds.queue.SeamlessQueue;
import work.lclpnet.playerswitch.config.PlayerEntry;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.floor;
import static java.lang.Math.max;

public class SeamlessPlayerQueue implements PlayerQueue {

    private final Set<PlayerEntry> playerEntries;
    private final Codec<PlayerEntry> codec;
    private final Logger logger;
    private @Nullable SeamlessQueue<PlayerEntry> queue = null;

    public SeamlessPlayerQueue(Collection<PlayerEntry> playerEntries, Logger logger) {
        this.playerEntries = Set.copyOf(playerEntries);
        this.logger = logger;

        var byUuid = playerEntries.stream()
                .collect(Collectors.toMap(PlayerEntry::getUuid, Function.identity()));

        codec = UUIDUtil.AUTHLIB_CODEC.comapFlatMap(
                uuid -> {
                    PlayerEntry entry = byUuid.get(uuid);

                    if (entry == null) {
                        return DataResult.error(() -> "Unknown participant with id " + uuid);
                    }

                    return DataResult.success(entry);
                },
                PlayerEntry::getUuid
        );
    }

    @Override
    public void restore(Path path) {
        float marginPercent = 0.3f;
        int margin = (int) floor(marginPercent * playerEntries.size());

        if (playerEntries.size() > 1) {
            margin = max(margin, 1);
        }

        QueueTransfer<PlayerEntry> transfer;

        try {
            var persistence = new JsonFileQueuePersistence<>(path, codec, logger);

            transfer = persistence.restore();
        } catch (Throwable t) {
            logger.error("Failed to restore queue", t);
            transfer = QueueTransfer.empty();
        }

        queue = new SeamlessQueue<>(playerEntries, new Random(), margin, transfer);
    }

    @Override
    public void save(Path path) {
        var queue = this.queue;

        if (queue == null) return;

        var persistence = new JsonFileQueuePersistence<>(path, codec, logger);
        persistence.store(queue.transfer());
    }

    @Override
    public PlayerEntry next() {
        var queue = Objects.requireNonNull(this.queue, "Queue not initialized yet");

        PlayerEntry entry = queue.next();

        queue.pushElement(entry);

        return entry;
    }

    @Override
    public void sync(PlayerEntry current) {
        if (current == null) return;

        var queue = Objects.requireNonNull(this.queue, "Queue not initialized yet");

        var history = queue.transfer().history();

        // make sure the current player is the last history element in the queue
        if (history.isEmpty() || !current.equals(history.getLast())) {
            queue.pushElement(current);
            queue.pushUpcoming(current);
        }
    }
}
