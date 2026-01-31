package org.LegendaryHardcore.legendaryonboarding.storage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

public final class PendingStore {
    private final JavaPlugin plugin;
    private final File file;

    private final ConcurrentMap<UUID, PendingLocation> pending = new ConcurrentHashMap<>();
    private final Object ioLock = new Object();

    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final AtomicBoolean saveScheduled = new AtomicBoolean(false);

    // Time values
    private static final long SAVE_DEBOUNCE_MS = TimeUnit.SECONDS.toMillis(5);

    public PendingStore(JavaPlugin plugin) {
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

            if (worldName == null) continue;

            try {
                UUID uuid = UUID.fromString(key);
                pending.put(uuid, new PendingLocation(worldName, x, y, z, yaw, pitch));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("pending.yml has invalid UUID: " + key);
            }
        }
    }

    public boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    /*
    Save a player's 'return to' location. Call this ONLY from the main thread / player scheduler
     */
    public void setPending(UUID uuid, Location loc){
        pending.put(uuid, PendingLocation.from(loc));
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
        PendingLocation pl = pending.get(uuid);
        if (pl == null) return null;

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
        plugin.getServer().getAsyncScheduler().runDelayed(
                plugin,
                task -> {
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
                PendingLocation pl = entry.getValue();
                String base = "pending." + uuid;

                yml.set(base + ".world", pl.worldName());
                yml.set(base + ".x", pl.x());
                yml.set(base + ".y", pl.y());
                yml.set(base + ".z", pl.z());
                yml.set(base + ".yaw", pl.yaw());
                yml.set(base + ".pitch", pl.pitch());
            }
            try {
                yml.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save pending yml: " + e.getMessage());
            }
        }
    }

    public record PendingLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        public static PendingLocation from(Location loc){
            return new PendingLocation(
                    loc.getWorld().getName(),
                    loc.getX(),
                    loc.getY(),
                    loc.getZ(),
                    loc.getYaw(),
                    loc.getPitch()
            );
        }
    }
}
