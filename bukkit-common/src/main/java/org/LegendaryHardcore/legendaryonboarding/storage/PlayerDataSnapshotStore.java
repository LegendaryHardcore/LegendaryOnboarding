package org.LegendaryHardcore.legendaryonboarding.storage;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps a durable copy of the player's server data from immediately before
 * onboarding. This is the recovery path when no enabled plugin remains to
 * move the player out of the onboarding world on their next login.
 */
public final class PlayerDataSnapshotStore {
    private final LegendaryOnboarding plugin;
    private final Path snapshotDirectory;
    private final Path metadataDirectory;
    private final Path restoreRequiredDirectory;
    private final Object ioLock = new Object();

    public PlayerDataSnapshotStore(LegendaryOnboarding plugin) {
        this.plugin = plugin;
        this.snapshotDirectory = plugin.getDataFolder()
                .toPath()
                .resolve("recovery")
                .resolve("playerdata");
        this.metadataDirectory = plugin.getDataFolder()
                .toPath()
                .resolve("recovery")
                .resolve("metadata");
        this.restoreRequiredDirectory = plugin.getDataFolder()
                .toPath()
                .resolve("recovery")
                .resolve("restore-required");
    }

    public boolean captureNewSession(Player player) {
        UUID uuid = player.getUniqueId();
        Path snapshot = snapshotPath(uuid);
        synchronized (ioLock) {
            try {
                Files.deleteIfExists(restoreRequiredPath(uuid));
                player.saveData();
                Path playerData = findExistingPlayerData(uuid);
                if (playerData == null) {
                    if (player.hasPlayedBefore()) {
                        plugin.getLogger().warning(
                                "Could not locate player data for " + player.getName()
                                        + "; durable onboarding recovery is unavailable "
                                        + "for this session."
                        );
                    } else {
                        plugin.getLogger().info(
                                "No existing playerdata file was found for brand-new player "
                                        + player.getName()
                                        + "; pending return location was still recorded."
                        );
                    }
                    return false;
                }

                Files.createDirectories(snapshotDirectory);
                copyReplacingAtomically(playerData, snapshot);
                writeMetadata(player, playerData);
                Location location = player.getLocation();
                plugin.getLogger().info(
                        "Captured pre-onboarding recovery data for "
                                + player.getName() + " at "
                                + formatLocation(location) + "."
                );
                return true;
            } catch (IOException | RuntimeException exception) {
                plugin.getLogger().warning(
                        "Could not capture pre-onboarding data for "
                                + player.getName() + ": " + exception.getMessage()
                );
                return false;
            }
        }
    }

    public boolean restore(UUID uuid) {
        Path snapshot = snapshotPath(uuid);
        synchronized (ioLock) {
            if (!Files.isRegularFile(snapshot)) {
                return false;
            }

            try {
                Path playerData = findExistingPlayerData(uuid);
                if (playerData == null) {
                    playerData = preferredPlayerDataPath(uuid);
                }
                if (playerData == null) {
                    plugin.getLogger().warning(
                            "Could not find a world playerdata directory while restoring "
                                    + uuid + "."
                    );
                    return false;
                }

                Files.createDirectories(playerData.getParent());
                copyReplacingAtomically(snapshot, playerData);
                plugin.getLogger().info(
                        "Restored pre-onboarding recovery data for "
                                + uuid + metadataDescription(uuid) + "."
                );
                return true;
            } catch (IOException exception) {
                plugin.getLogger().warning(
                        "Could not restore pre-onboarding data for "
                                + uuid + ": " + exception.getMessage()
                );
                return false;
            }
        }
    }

    public void markRestoreRequired(UUID uuid) {
        synchronized (ioLock) {
            try {
                Files.createDirectories(restoreRequiredDirectory);
                Files.writeString(restoreRequiredPath(uuid), "required\n");
            } catch (IOException exception) {
                plugin.getLogger().warning(
                        "Could not mark player-data recovery as required for "
                                + uuid + ": " + exception.getMessage()
                );
            }
        }
    }

    public boolean restoreIfRequired(UUID uuid) {
        synchronized (ioLock) {
            if (!Files.isRegularFile(restoreRequiredPath(uuid))) {
                return false;
            }
        }

        if (!restore(uuid)) {
            return false;
        }

        synchronized (ioLock) {
            try {
                Files.deleteIfExists(restoreRequiredPath(uuid));
            } catch (IOException exception) {
                plugin.getLogger().warning(
                        "Restored player data for " + uuid
                                + " but could not clear its recovery marker: "
                                + exception.getMessage()
                );
            }
        }
        return true;
    }

    public void restoreOfflineRequired(Set<UUID> pending, Set<UUID> online) {
        for (UUID uuid : pending) {
            if (shouldRestoreOfflineSnapshot(
                    true,
                    online.contains(uuid),
                    isRestoreRequired(uuid)
            )) {
                restoreIfRequired(uuid);
            }
        }
    }

    static boolean shouldRestoreOfflineSnapshot(
            boolean pending,
            boolean online,
            boolean restoreRequired
    ) {
        return pending && !online && restoreRequired;
    }

    public void delete(UUID uuid) {
        synchronized (ioLock) {
            try {
                Files.deleteIfExists(snapshotPath(uuid));
                Files.deleteIfExists(metadataPath(uuid));
                Files.deleteIfExists(restoreRequiredPath(uuid));
            } catch (IOException exception) {
                plugin.getLogger().warning(
                        "Could not delete completed onboarding recovery snapshot for "
                                + uuid + ": " + exception.getMessage()
                );
            }
        }
    }

    private Path snapshotPath(UUID uuid) {
        return snapshotDirectory.resolve(uuid + ".dat");
    }

    private Path metadataPath(UUID uuid) {
        return metadataDirectory.resolve(uuid + ".yml");
    }

    private Path restoreRequiredPath(UUID uuid) {
        return restoreRequiredDirectory.resolve(uuid + ".marker");
    }

    private boolean isRestoreRequired(UUID uuid) {
        synchronized (ioLock) {
            return Files.isRegularFile(restoreRequiredPath(uuid));
        }
    }

    private Path findExistingPlayerData(UUID uuid) {
        String fileName = uuid + ".dat";
        for (World world : orderedWorlds()) {
            Path candidate = world.getWorldFolder()
                    .toPath()
                    .resolve("playerdata")
                    .resolve(fileName);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private Path preferredPlayerDataPath(UUID uuid) {
        List<World> worlds = orderedWorlds();
        if (worlds.isEmpty()) {
            return null;
        }
        return worlds.getFirst()
                .getWorldFolder()
                .toPath()
                .resolve("playerdata")
                .resolve(uuid + ".dat");
    }

    private List<World> orderedWorlds() {
        List<World> worlds = new ArrayList<>(plugin.getServer().getWorlds());
        worlds.sort(Comparator.comparingInt(world ->
                world.getEnvironment() == World.Environment.NORMAL ? 0 : 1
        ));
        return worlds;
    }

    private void writeMetadata(Player player, Path source) throws IOException {
        Location location = player.getLocation();
        YamlConfiguration metadata = new YamlConfiguration();
        metadata.set("playerName", player.getName());
        metadata.set("world", location.getWorld().getName());
        metadata.set("x", location.getX());
        metadata.set("y", location.getY());
        metadata.set("z", location.getZ());
        metadata.set("yaw", location.getYaw());
        metadata.set("pitch", location.getPitch());
        metadata.set("source", source.toAbsolutePath().normalize().toString());

        Files.createDirectories(metadataDirectory);
        Path target = metadataPath(player.getUniqueId());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        metadata.save(temporary.toFile());
        try {
            Files.move(
                    temporary,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String metadataDescription(UUID uuid) {
        Path path = metadataPath(uuid);
        if (!Files.isRegularFile(path)) {
            return "";
        }

        YamlConfiguration metadata = YamlConfiguration.loadConfiguration(path.toFile());
        String world = metadata.getString("world");
        if (world == null) {
            return "";
        }
        return " captured at " + world
                + " " + formatCoordinate(metadata.getDouble("x"))
                + ", " + formatCoordinate(metadata.getDouble("y"))
                + ", " + formatCoordinate(metadata.getDouble("z"));
    }

    private static String formatLocation(Location location) {
        return location.getWorld().getName()
                + " " + formatCoordinate(location.getX())
                + ", " + formatCoordinate(location.getY())
                + ", " + formatCoordinate(location.getZ());
    }

    private static String formatCoordinate(double coordinate) {
        return String.format(java.util.Locale.ROOT, "%.2f", coordinate);
    }

    private static void copyReplacingAtomically(Path source, Path target) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.move(
                    temporary,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
