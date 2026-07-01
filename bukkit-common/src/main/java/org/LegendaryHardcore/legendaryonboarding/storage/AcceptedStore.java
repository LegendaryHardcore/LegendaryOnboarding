package org.LegendaryHardcore.legendaryonboarding.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AcceptedStore {
    private final LegendaryOnboarding plugin;
    private final File file;

    private static final int MAX_NAMES = 10;
    private static final long SAVE_DEBOUNCE_MS = TimeUnit.SECONDS.toMillis(5);

    // UUID -> AcceptedEntry
    private final ConcurrentMap<UUID, AcceptedEntry> accepted = new ConcurrentHashMap<>();

    private final Object ioLock = new Object();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final AtomicBoolean saveScheduled = new AtomicBoolean(false);

    public AcceptedStore(LegendaryOnboarding plugin) {
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
        ConfigurationSection root = yml.getConfigurationSection("accepted");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("accepted.yml has invalid UUID: " + key);
                continue;
            }

            String base = "accepted." + key;

            // Support old format: accepted.<uuid>: true
            if (!yml.isConfigurationSection(base)) {
                boolean oldVal = yml.getBoolean(base, false);
                if (oldVal) {
                    accepted.put(uuid, new AcceptedEntry(new ArrayList<>(), true, null));
                }
                continue;
            }

            boolean isAccepted = yml.getBoolean(base + ".accepted", false);
            List<String> names = yml.getStringList(base + ".names");
            String firstAccepted = yml.getString(base + ".firstAccepted", null);

            accepted.put(uuid, new AcceptedEntry(
                    clampNames(names),
                    isAccepted,
                    firstAccepted
            ));
        }
    }

    public boolean isAccepted(UUID uuid) {
        AcceptedEntry e = accepted.get(uuid);
        return e != null && e.accepted();
    }

    public boolean hasEntry(UUID uuid) {
        return accepted.containsKey(uuid);
    }

    public AcceptedEntry getEntry(UUID uuid) {
        return accepted.get(uuid);
    }

    public UUID findByName(String name) {
        if (name == null || name.isBlank()) return null;
        for (Map.Entry<UUID, AcceptedEntry> entry : accepted.entrySet()) {
            if (entry.getValue().names().stream().anyMatch(name::equalsIgnoreCase)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Update name list on join (works whether accepted or not). */
    public void recordSeenName(UUID uuid, String currentName) {
        if (currentName == null || currentName.isBlank()) return;

        accepted.compute(uuid, (id, existing) -> {
            AcceptedEntry e = (existing == null)
                    ? new AcceptedEntry(new ArrayList<>(), false, null)
                    : existing;

            List<String> updatedNames = mergeName(e.names(), currentName);

            if (!updatedNames.equals(e.names())) {
                markDirtyAndScheduleSave();
                return e.withNames(updatedNames);
            }
            return e;
        });
    }

    /** Mark accepted forever (also records current name + firstAccepted timestamp if missing). */
    public void markAccepted(UUID uuid, String currentName) {
        accepted.compute(uuid, (id, existing) -> {
            AcceptedEntry e = (existing == null)
                    ? new AcceptedEntry(new ArrayList<>(), false, null)
                    : existing;

            List<String> updatedNames = mergeName(e.names(), currentName);

            String first = e.firstAccepted();
            if (first == null || first.isBlank()) {
                first = Instant.now().toString();
            }

            if (!e.accepted() || !Objects.equals(first, e.firstAccepted()) || !updatedNames.equals(e.names())) {
                markDirtyAndScheduleSave();
            }

            return new AcceptedEntry(updatedNames, true, first);
        });
    }

    public void setAccepted(UUID uuid, String currentName, boolean value) {
        accepted.compute(uuid, (id, existing) -> {
            AcceptedEntry entry = existing == null
                    ? new AcceptedEntry(new ArrayList<>(), false, null)
                    : existing;
            List<String> names = mergeName(entry.names(), currentName);
            String firstAccepted = entry.firstAccepted();
            if (value && (firstAccepted == null || firstAccepted.isBlank())) {
                firstAccepted = Instant.now().toString();
            }
            AcceptedEntry updated = new AcceptedEntry(names, value, firstAccepted);
            if (!updated.equals(entry)) {
                markDirtyAndScheduleSave();
            }
            return updated;
        });
    }

    public void clear(UUID uuid) {
        if (accepted.remove(uuid) != null) {
            markDirtyAndScheduleSave();
        }
    }

    private void markDirtyAndScheduleSave() {
        dirty.set(true);

        if (!saveScheduled.compareAndSet(false, true)) return;

        plugin.getPlatformScheduler().runAsyncDelayed(
                () -> {
                    try {
                        if (dirty.getAndSet(false)) {
                            saveSync();
                        }
                    } finally {
                        saveScheduled.set(false);
                        if (dirty.get()) markDirtyAndScheduleSave();
                    }
                },
                SAVE_DEBOUNCE_MS,
                TimeUnit.MILLISECONDS
        );
    }

    public void flushNow() {
        dirty.set(false);
        saveScheduled.set(false);
        saveSync();
    }

    private void saveSync() {
        synchronized (ioLock) {
            YamlConfiguration yml = new YamlConfiguration();

            Map<UUID, AcceptedEntry> snapshot = new HashMap<>(accepted);

            for (Map.Entry<UUID, AcceptedEntry> entry : snapshot.entrySet()) {
                UUID uuid = entry.getKey();
                AcceptedEntry e = entry.getValue();

                String base = "accepted." + uuid;

                yml.set(base + ".names", clampNames(e.names()));
                yml.set(base + ".accepted", e.accepted());
                if (e.firstAccepted() != null) {
                    yml.set(base + ".firstAccepted", e.firstAccepted());
                }
            }

            try {
                yml.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not save accepted.yml (" + file.getName() + "): " + e.getMessage());
            }
        }
    }

    private static List<String> clampNames(List<String> names) {
        if (names == null || names.isEmpty()) return new ArrayList<>();

        List<String> cleaned = new ArrayList<>();
        for (String n : names) {
            if (n == null) continue;
            String s = n.trim();
            if (!s.isEmpty()) cleaned.add(s);
        }

        if (cleaned.size() > MAX_NAMES) {
            return new ArrayList<>(cleaned.subList(cleaned.size() - MAX_NAMES, cleaned.size()));
        }
        return cleaned;
    }

    private static List<String> mergeName(List<String> existing, String currentName) {
        List<String> list = (existing == null) ? new ArrayList<>() : new ArrayList<>(existing);
        String cur = currentName.trim();
        if (cur.isEmpty()) return clampNames(list);

        list.removeIf(n -> n != null && n.equalsIgnoreCase(cur));
        list.add(cur);

        return clampNames(list);
    }

    public record AcceptedEntry(List<String> names, boolean accepted, String firstAccepted) {
        public AcceptedEntry withNames(List<String> newNames) {
            return new AcceptedEntry(newNames, accepted, firstAccepted);
        }
    }
}
