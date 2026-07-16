package org.LegendaryHardcore.legendaryonboarding.storage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

public final class PendingStore {
    private final LegendaryOnboarding plugin;
    private final File file;

    private final ConcurrentMap<UUID, PendingEntry> pending = new ConcurrentHashMap<>();
    private final Object ioLock = new Object();

    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final AtomicBoolean saveScheduled = new AtomicBoolean(false);

    // Time values
    private static final long SAVE_DEBOUNCE_MS = TimeUnit.SECONDS.toMillis(5);

    public PendingStore(LegendaryOnboarding plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pending.yml");
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create pending.yml: " + e.getMessage());
                return;
            }
        }

        pending.clear();

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        if (!yml.isConfigurationSection("pending")) return;

        for (String key : yml.getConfigurationSection("pending").getKeys(false)) {
            String base = "pending." + key;

            String worldName = yml.getString(base + ".world", null);
            double x = yml.getDouble(base + ".x");
            double y = yml.getDouble(base + ".y");
            double z = yml.getDouble(base + ".z");
            float yaw = (float) yml.getDouble(base + ".yaw");
            float pitch = (float) yml.getDouble(base + ".pitch");
            boolean cleanupRequired = yml.getBoolean(base + ".cleanupRequired", false);
            boolean debugSession = yml.getBoolean(base + ".debugSession", false);
            boolean announceWhenComplete = yml.getBoolean(base + ".announceWhenComplete", true);

            if (worldName == null && !cleanupRequired) continue;

            try {
                UUID uuid = UUID.fromString(key);
                pending.put(uuid, new PendingEntry(
                        worldName,
                        x,
                        y,
                        z,
                        yaw,
                        pitch,
                        cleanupRequired,
                        debugSession,
                        announceWhenComplete
                ));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("pending.yml has invalid UUID: " + key);
            }
        }
    }

    public boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public boolean isCleanupRequired(UUID uuid) {
        PendingEntry entry = pending.get(uuid);
        return entry != null && entry.cleanupRequired();
    }

    public boolean hasReturnLocation(UUID uuid) {
        PendingEntry entry = pending.get(uuid);
        return entry != null && entry.worldName() != null;
    }

    public boolean isDebugSession(UUID uuid) {
        PendingEntry entry = pending.get(uuid);
        return entry != null && entry.debugSession();
    }

    public boolean shouldAnnounceWhenComplete(UUID uuid) {
        PendingEntry entry = pending.get(uuid);
        return entry != null && entry.announceWhenComplete();
    }

    public Set<UUID> pendingPlayerIds() {
        return Set.copyOf(pending.keySet());
    }

    /*
    Save a player's 'return to' location. Call this ONLY from the main thread / player scheduler
     */
    public void setPending(
            UUID uuid,
            Location loc,
            boolean debugSession,
            boolean announceWhenComplete
    ){
        pending.put(uuid, PendingEntry.from(loc, debugSession, announceWhenComplete));
        markDirtyAndScheduleSave();
    }

    /**
     * Persist a new return destination before moving the player into onboarding.
     * This critical write must survive an immediate disconnect or shutdown.
     */
    public void setPendingAndFlush(
            UUID uuid,
            Location loc,
            boolean debugSession,
            boolean announceWhenComplete
    ) {
        pending.put(uuid, PendingEntry.from(loc, debugSession, announceWhenComplete));
        dirty.set(false);
        saveSync();
    }

    public void markCleanupRequired(UUID uuid) {
        pending.compute(uuid, (ignored, existing) -> cleanupRequiredEntry(existing));
        markDirtyAndScheduleSave();
    }

    public void markCleanupRequiredAndFlush(UUID uuid) {
        pending.compute(uuid, (ignored, existing) -> cleanupRequiredEntry(existing));
        dirty.set(false);
        saveScheduled.set(false);
        saveSync();
    }

    public void markAllCleanupRequired() {
        if (pending.isEmpty()) return;
        pending.replaceAll((ignored, existing) -> cleanupRequiredEntry(existing));
        markDirtyAndScheduleSave();
    }

    public void markAllCleanupRequiredAndFlush() {
        if (pending.isEmpty()) return;
        pending.replaceAll((ignored, existing) -> cleanupRequiredEntry(existing));
        dirty.set(false);
        saveScheduled.set(false);
        saveSync();
    }

    static PendingEntry cleanupRequiredEntry(PendingEntry existing) {
        return existing == null
                ? PendingEntry.cleanupOnly()
                : existing.withCleanupRequired(true);
    }

    public void configureSession(
            UUID uuid,
            boolean debugSession,
            boolean announceWhenComplete
    ) {
        pending.computeIfPresent(uuid, (ignored, existing) ->
                existing.withSession(debugSession, announceWhenComplete)
        );
        markDirtyAndScheduleSave();
    }

    public void clearPending(UUID uuid){
        pending.remove(uuid);
        markDirtyAndScheduleSave();
    }

    /**
     * Returns a Bukkit Location if the world is loaded, otherwise null.
     * Should be called from main thread / player scheduler when you intend to teleport
     */
    public Location getPendingLocation(UUID uuid){
        PendingEntry pl = pending.get(uuid);
        if (pl == null) return null;
        if (pl.worldName() == null) return null;

        World w = Bukkit.getWorld(pl.worldName());
        if (w == null) return null;

        return new Location(w, pl.x(), pl.y(), pl.z(), pl.yaw(), pl.pitch());
    }

    private void markDirtyAndScheduleSave() {
        dirty.set(true);

        // Only schedule one pending save at a time
        if (!saveScheduled.compareAndSet(false, true)) {
            return;
        }

        // Schedule one async save shortly in the future to coalesce updates
        plugin.getPlatformScheduler().runAsyncDelayed(
                () -> {
                    try {
                        if (dirty.getAndSet(false)) {
                            saveSync(); // runs on async scheduler thread
                        }
                    } finally {
                        // allow future schedules
                        saveScheduled.set(false);

                        // edge case: if changes happened during save, schedule again
                        if (dirty.get()) {
                            markDirtyAndScheduleSave();
                        }
                    }
                },
                SAVE_DEBOUNCE_MS,
                TimeUnit.MILLISECONDS
        );
    }

    public void flushNow() {
        dirty.set(false);
        saveScheduled.set(false);
        saveSync(); // Do a final write
    }

    private void saveSync(){
        synchronized (ioLock){
            YamlConfiguration yml = new YamlConfiguration();

            for (var entry : pending.entrySet()) {
                UUID uuid = entry.getKey();
                PendingEntry pl = entry.getValue();
                String base = "pending." + uuid;

                if (pl.worldName() != null) {
                    yml.set(base + ".world", pl.worldName());
                    yml.set(base + ".x", pl.x());
                    yml.set(base + ".y", pl.y());
                    yml.set(base + ".z", pl.z());
                    yml.set(base + ".yaw", pl.yaw());
                    yml.set(base + ".pitch", pl.pitch());
                }
                yml.set(base + ".cleanupRequired", pl.cleanupRequired());
                yml.set(base + ".debugSession", pl.debugSession());
                yml.set(base + ".announceWhenComplete", pl.announceWhenComplete());
            }
            try {
                yml.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save pending yml: " + e.getMessage());
            }
        }
    }

    public record PendingEntry(
            String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            boolean cleanupRequired,
            boolean debugSession,
            boolean announceWhenComplete
    ) {
        public static PendingEntry from(
                Location loc,
                boolean debugSession,
                boolean announceWhenComplete
        ){
            return new PendingEntry(
                    loc.getWorld().getName(),
                    loc.getX(),
                    loc.getY(),
                    loc.getZ(),
                    loc.getYaw(),
                    loc.getPitch(),
                    false,
                    debugSession,
                    announceWhenComplete
            );
        }

        public static PendingEntry cleanupOnly() {
            return new PendingEntry(null, 0, 0, 0, 0, 0, true, false, false);
        }

        public PendingEntry withCleanupRequired(boolean cleanupRequired) {
            return new PendingEntry(
                    worldName,
                    x,
                    y,
                    z,
                    yaw,
                    pitch,
                    cleanupRequired,
                    debugSession,
                    announceWhenComplete
            );
        }

        public PendingEntry withSession(boolean debugSession, boolean announceWhenComplete) {
            return new PendingEntry(
                    worldName,
                    x,
                    y,
                    z,
                    yaw,
                    pitch,
                    false,
                    debugSession,
                    announceWhenComplete
            );
        }
    }
}
