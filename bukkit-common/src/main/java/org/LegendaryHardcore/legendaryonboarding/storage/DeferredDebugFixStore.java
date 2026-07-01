package org.LegendaryHardcore.legendaryonboarding.storage;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Persists "state reset only" debug fixes for offline players so they can be
 * applied on the player's next login without reusing onboarding cleanup state.
 */
public final class DeferredDebugFixStore {
    private final LegendaryOnboarding plugin;
    private final Path markerDirectory;
    private final Object ioLock = new Object();

    public DeferredDebugFixStore(LegendaryOnboarding plugin) {
        this.plugin = plugin;
        this.markerDirectory = plugin.getDataFolder()
                .toPath()
                .resolve("recovery")
                .resolve("deferred-debug-fix");
    }

    public void mark(UUID uuid) {
        synchronized (ioLock) {
            try {
                Files.createDirectories(markerDirectory);
                Files.writeString(markerPath(uuid), "queued\n");
            } catch (IOException exception) {
                plugin.getLogger().warning(
                        "Could not queue deferred debug fix for "
                                + uuid + ": " + exception.getMessage()
                );
            }
        }
    }

    public boolean consume(UUID uuid) {
        synchronized (ioLock) {
            Path marker = markerPath(uuid);
            if (!Files.isRegularFile(marker)) {
                return false;
            }
            try {
                Files.deleteIfExists(marker);
                return true;
            } catch (IOException exception) {
                plugin.getLogger().warning(
                        "Could not clear deferred debug fix marker for "
                                + uuid + ": " + exception.getMessage()
                );
                return false;
            }
        }
    }

    public boolean has(UUID uuid) {
        synchronized (ioLock) {
            return Files.isRegularFile(markerPath(uuid));
        }
    }

    private Path markerPath(UUID uuid) {
        return markerDirectory.resolve(uuid + ".marker");
    }
}
