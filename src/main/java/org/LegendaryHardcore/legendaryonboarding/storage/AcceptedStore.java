package org.LegendaryHardcore.legendaryonboarding.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

public final class AcceptedStore {
    private final JavaPlugin plugin;
    private final File file;

    // fast in-memory cache
    private final ConcurrentMap<UUID, Boolean> accepted = new ConcurrentHashMap<>();

    // simple lock to avoid save/write races
    private final Object ioLock = new Object();

    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final AtomicBoolean saveScheduled = new AtomicBoolean(false);

    // Time values
    private static final long SAVE_DEBOUNCE_MS = TimeUnit.SECONDS.toMillis(5);

    public AcceptedStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "accepted.yml");
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create accepted.yml: " + e.getMessage());
                return;
            }
        }

        accepted.clear();

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        if (!yml.isConfigurationSection("accepted")) return;

        for (String key : yml.getConfigurationSection("accepted").getKeys(false)) {
            boolean val = yml.getBoolean("accepted." + key, false);
            if (val) {
                try {
                    accepted.put(UUID.fromString(key), true);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("accepted.yml has invalid UUID: " + key);
                }
            }
        }
    }

    public boolean isAccepted(UUID uuid) {
        return accepted.getOrDefault(uuid, false);
    }

    public void setAccepted(UUID uuid, boolean value) {
        if (value) accepted.put(uuid, true);
        else accepted.remove(uuid);

        markDirtyAndScheduleSave();
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
        synchronized (ioLock) {
            YamlConfiguration yml = new YamlConfiguration();
            for (UUID uuid : accepted.keySet()) {
                yml.set("accepted." + uuid.toString(), true);
            }
            try {
                yml.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not save accepted.yml (" + file.getName() + "): " + e.getMessage());
            }
        }
    }
}
