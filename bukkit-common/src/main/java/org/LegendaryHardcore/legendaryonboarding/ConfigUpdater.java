package org.LegendaryHardcore.legendaryonboarding;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

/**
 * Updates the live config to the bundled layout while retaining current values.
 */
public final class ConfigUpdater {
    private static final String CONFIG_FILE_NAME = "config.yml";
    private static final String OLD_FIRST_JOIN_MESSAGE =
            "{yellow}{player} has joined for the first time.";
    private static final String CURRENT_FIRST_JOIN_MESSAGE =
            "{yellow}{player} joined the server for the first time";

    private final JavaPlugin plugin;

    public ConfigUpdater(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @return true when the config is ready to load, otherwise false
     */
    public boolean update() {
        File configFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME);

        try (InputStream defaultStream = plugin.getResource(CONFIG_FILE_NAME)) {
            if (defaultStream == null) {
                plugin.getLogger().severe("Missing bundled config resource: " + CONFIG_FILE_NAME);
                return false;
            }

            UpdateResult result = update(configFile, defaultStream);
            for (String removedPath : result.removedPaths()) {
                plugin.getLogger().info("Removed obsolete config path: " + removedPath);
            }
            if (result.updated()) {
                plugin.getLogger().info("Updated config.yml to the current layout.");
            }
            return true;
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Could not update config.yml. The existing file was left unchanged.",
                    exception
            );
            return false;
        }
    }

    static UpdateResult update(File liveFile, InputStream defaultStream)
            throws IOException, InvalidConfigurationException {
        Objects.requireNonNull(liveFile, "liveFile");
        Objects.requireNonNull(defaultStream, "defaultStream");

        YamlConfiguration liveConfig = load(liveFile);
        YamlConfiguration mergedConfig = load(defaultStream);
        Set<String> removedPaths = new HashSet<>();
        String originalYaml = liveConfig.saveToString();

        migrateFormerDefaults(liveConfig);
        overlayExistingValues(liveConfig, mergedConfig, "", removedPaths);

        String mergedYaml = mergedConfig.saveToString();
        boolean updated = !originalYaml.equals(mergedYaml);
        if (updated) {
            saveAtomically(liveFile, mergedYaml);
        }

        return new UpdateResult(updated, Set.copyOf(removedPaths));
    }

    private static void migrateFormerDefaults(YamlConfiguration config) {
        if (OLD_FIRST_JOIN_MESSAGE.equals(config.getString("FIRST_JOIN_MESSAGE"))) {
            config.set("FIRST_JOIN_MESSAGE", CURRENT_FIRST_JOIN_MESSAGE);
        }
    }

    static YamlConfiguration load(File file)
            throws IOException, InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.options().parseComments(true);
        if (file.exists()) {
            config.load(file);
        }
        return config;
    }

    static YamlConfiguration load(InputStream stream)
            throws IOException, InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.options().parseComments(true);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            config.load(reader);
        }
        return config;
    }

    static void migrate(YamlConfiguration config, String oldPath, String newPath) {
        if (!config.contains(oldPath) || config.contains(newPath)) {
            return;
        }

        config.set(newPath, config.get(oldPath));
        config.set(oldPath, null);
    }

    private static void overlayExistingValues(
            ConfigurationSection liveSection,
            ConfigurationSection mergedSection,
            String pathPrefix,
            Set<String> removedPaths
    ) {
        for (String liveKey : liveSection.getKeys(false)) {
            String path = pathPrefix.isBlank() ? liveKey : pathPrefix + "." + liveKey;
            if (!mergedSection.contains(liveKey, true)) {
                removedPaths.add(path);
                continue;
            }

            Object liveValue = liveSection.get(liveKey);
            Object defaultValue = mergedSection.get(liveKey);
            if (liveValue instanceof ConfigurationSection liveChild
                    && defaultValue instanceof ConfigurationSection mergedChild) {
                overlayExistingValues(liveChild, mergedChild, path, removedPaths);
                continue;
            }

            if (liveValue instanceof ConfigurationSection
                    || defaultValue instanceof ConfigurationSection) {
                continue;
            }

            mergedSection.set(liveKey, liveValue);
        }
    }

    static void saveAtomically(File target, String contents) throws IOException {
        File parent = target.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }

        File temporary = File.createTempFile(target.getName(), ".tmp", parent);
        try {
            Files.writeString(temporary.toPath(), contents, StandardCharsets.UTF_8);
            try {
                Files.move(
                        temporary.toPath(),
                        target.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(
                        temporary.toPath(),
                        target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } finally {
            Files.deleteIfExists(temporary.toPath());
        }
    }

    record UpdateResult(boolean updated, Set<String> removedPaths) {
    }
}
