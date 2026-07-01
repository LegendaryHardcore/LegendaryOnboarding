package org.LegendaryHardcore.legendaryonboarding;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigUpdaterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void preservesExistingValuesAndAddsNewDefaults() throws Exception {
        File liveFile = writeLiveConfig("""
                server:
                  name: Custom Server
                  teleport: false
                messages:
                  - Existing message
                """);

        ConfigUpdater.UpdateResult result = update(liveFile, """
                server:
                  name: Default Server
                  teleport: true
                  world: world
                messages:
                  - Default message
                """);

        YamlConfiguration updated = YamlConfiguration.loadConfiguration(liveFile);
        assertTrue(result.updated());
        assertEquals("Custom Server", updated.getString("server.name"));
        assertFalse(updated.getBoolean("server.teleport"));
        assertEquals("world", updated.getString("server.world"));
        assertEquals("Existing message", updated.getStringList("messages").getFirst());
    }

    @Test
    void removesObsoletePathsAtAnyDepth() throws Exception {
        File liveFile = writeLiveConfig("""
                server:
                  name: Custom Server
                  removed-child: old
                REMOVED_ROOT: old
                """);

        ConfigUpdater.UpdateResult result = update(liveFile, """
                server:
                  name: Default Server
                """);

        YamlConfiguration updated = YamlConfiguration.loadConfiguration(liveFile);
        assertFalse(updated.contains("server.removed-child"));
        assertFalse(updated.contains("REMOVED_ROOT"));
        assertEquals(
                java.util.Set.of("server.removed-child", "REMOVED_ROOT"),
                result.removedPaths()
        );
    }

    @Test
    void usesDefaultWhenAPathChangesBetweenValueAndSection() throws Exception {
        File liveFile = writeLiveConfig("""
                location: old-format
                prompt:
                  title: old-section
                """);

        update(liveFile, """
                location:
                  world: world
                prompt: New prompt
                """);

        YamlConfiguration updated = YamlConfiguration.loadConfiguration(liveFile);
        assertEquals("world", updated.getString("location.world"));
        assertEquals("New prompt", updated.getString("prompt"));
    }

    @Test
    void doesNotRewriteAnAlreadyCurrentConfig() throws Exception {
        String yaml = """
                server:
                  name: Custom Server
                """;
        File liveFile = writeLiveConfig(yaml);

        ConfigUpdater.UpdateResult result = update(liveFile, yaml);

        assertFalse(result.updated());
        assertTrue(result.removedPaths().isEmpty());
    }

    @Test
    void updatesFormerFirstJoinDefaultButPreservesCustomMessages() throws Exception {
        File formerDefault = writeLiveConfig("""
                FIRST_JOIN_MESSAGE: "{yellow}{player} has joined for the first time."
                """);

        update(formerDefault, """
                FIRST_JOIN_MESSAGE: "{yellow}{player} joined the server for the first time"
                """);

        assertEquals(
                "{yellow}{player} joined the server for the first time",
                YamlConfiguration.loadConfiguration(formerDefault)
                        .getString("FIRST_JOIN_MESSAGE")
        );

        File custom = temporaryDirectory.resolve("custom.yml").toFile();
        Files.writeString(
                custom.toPath(),
                "FIRST_JOIN_MESSAGE: \"Welcome {player}!\"\n",
                StandardCharsets.UTF_8
        );
        update(custom, """
                FIRST_JOIN_MESSAGE: "{yellow}{player} joined the server for the first time"
                """);

        assertEquals(
                "Welcome {player}!",
                YamlConfiguration.loadConfiguration(custom)
                        .getString("FIRST_JOIN_MESSAGE")
        );
    }

    private File writeLiveConfig(String yaml) throws Exception {
        Path path = temporaryDirectory.resolve("config.yml");
        Files.writeString(path, yaml, StandardCharsets.UTF_8);
        return path.toFile();
    }

    private ConfigUpdater.UpdateResult update(File liveFile, String defaults) throws Exception {
        return ConfigUpdater.update(
                liveFile,
                new ByteArrayInputStream(defaults.getBytes(StandardCharsets.UTF_8))
        );
    }
}
