package org.LegendaryHardcore.legendaryonboarding;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitleSequenceConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void bundledSequenceIsDisabledByDefault() throws Exception {
        try (var defaults = TitleSequenceConfigTest.class.getClassLoader()
                .getResourceAsStream(TitleSequenceConfig.FILE_NAME)) {
            assertNotNull(defaults);
            assertFalse(ConfigUpdater.load(defaults).getBoolean("ENABLED", true));
        }
    }

    @Test
    void migratesLegacySequenceAndPromptChatIntoNewFile() throws Exception {
        Path mainPath = temporaryDirectory.resolve("config.yml");
        Path sequencePath = temporaryDirectory.resolve("titlesequence.yml");
        Files.writeString(mainPath, """
                SERVER_NAME: Custom Server
                RULES_SEQUENCE_DURATION: 9
                RULES_SEQUENCE_CONTENT:
                  - TITLE: Custom rules
                PROMPT_ACCEPT:
                  - SUBTITLE: Type /accept
                PROMPT_CHAT: "Rules: https://example.com/rules"
                JOIN_SEQUENCE_CONTENT:
                  - TITLE: Welcome
                """, StandardCharsets.UTF_8);

        assertTrue(TitleSequenceConfig.migrateLegacySequence(
                mainPath.toFile(),
                sequencePath.toFile()
        ));

        YamlConfiguration sequence = YamlConfiguration.loadConfiguration(sequencePath.toFile());
        assertEquals(9, sequence.getInt("RULES_SEQUENCE_DURATION"));
        assertEquals(
                "Custom rules",
                sequence.getMapList("RULES_SEQUENCE_CONTENT").getFirst().get("TITLE")
        );
        assertEquals(
                "Rules: https://example.com/rules",
                sequence.getMapList("PROMPT_ACCEPT").getFirst().get("CHAT")
        );
        assertEquals(
                "Welcome",
                sequence.getMapList("JOIN_SEQUENCE_CONTENT").getFirst().get("TITLE")
        );
        assertFalse(sequence.contains("SERVER_NAME"));
        assertTrue(Files.readString(mainPath).contains("PROMPT_CHAT"));
    }

    @Test
    void doesNotOverwriteAnExistingSequenceFile() throws Exception {
        Path mainPath = temporaryDirectory.resolve("config.yml");
        Path sequencePath = temporaryDirectory.resolve("titlesequence.yml");
        Files.writeString(mainPath, "RULES_SEQUENCE_DURATION: 9\n");
        Files.writeString(sequencePath, "RULES_SEQUENCE_DURATION: 3\n");

        assertFalse(TitleSequenceConfig.migrateLegacySequence(
                mainPath.toFile(),
                sequencePath.toFile()
        ));
        assertEquals(
                3,
                YamlConfiguration.loadConfiguration(sequencePath.toFile())
                        .getInt("RULES_SEQUENCE_DURATION")
        );
    }
}
