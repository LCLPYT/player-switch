package work.lclpnet.playerswitch.util.queue;

import work.lclpnet.playerswitch.config.PlayerEntry;

import java.nio.file.Path;

public interface PlayerQueue {

    void restore(Path path);

    void save(Path path);

    PlayerEntry next();

    void sync(PlayerEntry current);
}
