package org.LegendaryHardcore.legendaryonboarding;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Owns titlesequence.yml and migrates sequence settings out of legacy config.yml files.
 */
public final class TitleSequenceConfig {
    static final String FILE_NAME = "titlesequence.yml";
    private static final List<String> LEGACY_PATHS = List.of(
            "RULES_SEQUENCE_FADE_IN",
            "RULES_SEQUENCE_DURATION",
            "RULES_SEQUENCE_FADE_OUT",
            "RULES_SEQUENCE_CONTENT",
            "PROMPT_ACCEPT",
            "JOIN_SEQUENCE_FADE_IN",
            "JOIN_SEQUENCE_DURATION",
            "JOIN_SEQUENCE_FADE_OUT",
            "JOIN_SEQUENCE_CONTENT"
    );

    private final JavaPlugin plugin;

    public TitleSequenceConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public YamlConfiguration updateAndLoad() {
        File sequenceFile = new File(plugin.getDataFolder(), FILE_NAME);
        File mainConfigFile = new File(plugin.getDataFolder(), "config.yml");

        try {
            if (!sequenceFile.exists()) {
                migrateLegacySequence(mainConfigFile, sequenceFile);
            }

            try (InputStream defaults = plugin.getResource(FILE_NAME)) {
                if (defaults == null) {
                    plugin.getLogger().severe("Missing bundled config resource: " + FILE_NAME);
                    return null;
                }

                ConfigUpdater.UpdateResult result = ConfigUpdater.update(sequenceFile, defaults);
                for (String removedPath : result.removedPaths()) {
                    plugin.getLogger().info("Removed obsolete title sequence path: " + removedPath);
                }
                if (result.updated()) {
                    plugin.getLogger().info("Updated " + FILE_NAME + " to the current layout.");
                }
            }

            return ConfigUpdater.load(sequenceFile);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Could not prepare " + FILE_NAME + ". Existing config files were left unchanged.",
                    exception
            );
            return null;
        }
    }

    static boolean migrateLegacySequence(File mainConfigFile, File sequenceFile)
            throws IOException, InvalidConfigurationException {
        if (!mainConfigFile.exists() || sequenceFile.exists()) {
            return false;
        }

        YamlConfiguration legacy = ConfigUpdater.load(mainConfigFile);
        YamlConfiguration sequence = new YamlConfiguration();
        boolean migrated = false;

        for (String path : LEGACY_PATHS) {
            if (legacy.contains(path)) {
                sequence.set(path, legacy.get(path));
                migrated = true;
            }
        }

        String promptChat = legacy.getString("PROMPT_CHAT");
        if (promptChat != null && !promptChat.isBlank()) {
            migratePromptChat(sequence, promptChat);
            migrated = true;
        }

        if (migrated) {
            ConfigUpdater.saveAtomically(sequenceFile, sequence.saveToString());
        }
        return migrated;
    }

    private static void migratePromptChat(YamlConfiguration sequence, String promptChat) {
        List<Map<?, ?>> promptSteps = sequence.getMapList("PROMPT_ACCEPT");
        Map<Object, Object> firstStep = new LinkedHashMap<>();
        if (promptSteps.isEmpty()) {
            firstStep.put("CHAT", promptChat);
            sequence.set("PROMPT_ACCEPT", List.of(firstStep));
            return;
        }

        firstStep.putAll(promptSteps.getFirst());
        firstStep.putIfAbsent("CHAT", promptChat);
        List<Map<?, ?>> migratedSteps = new ArrayList<>(promptSteps);
        migratedSteps.set(0, firstStep);
        sequence.set("PROMPT_ACCEPT", migratedSteps);
    }
}
