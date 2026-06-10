package org.LegendaryHardcore.legendaryonboarding;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadConfigTest {
    @Test
    void debugModeActivatesSequenceWithoutProductionEnableFlag() {
        assertFalse(LoadConfig.isSequenceActive(false, false));
        assertTrue(LoadConfig.isSequenceActive(false, true));
        assertTrue(LoadConfig.isSequenceActive(true, false));
        assertTrue(LoadConfig.isSequenceActive(true, true));
    }

    @Test
    void bundledDebugModeIsDisabledByDefault() throws Exception {
        try (InputStream defaults = LoadConfigTest.class.getClassLoader()
                .getResourceAsStream("config.yml")) {
            assertNotNull(defaults);
            var config = ConfigUpdater.load(defaults);
            assertFalse(config.getBoolean("DEBUG_FORCE_ONBOARDING", true));
            assertTrue(config.getBoolean("BLOCK_ADVANCEMENTS", false));
        }
    }

    @Test
    void bundledWelcomeSequenceIsConfigurable() throws Exception {
        try (InputStream defaults = LoadConfigTest.class.getClassLoader()
                .getResourceAsStream("titlesequence.yml")) {
            assertNotNull(defaults);
            var welcome = ConfigUpdater.load(defaults).getMapList("WELCOME_SEQUENCE");
            assertFalse(welcome.isEmpty());
            assertEquals(
                    "Welcome to {gold}{server_name}{reset}, {player_name}!",
                    welcome.getFirst().get("TITLE")
            );
        }
    }

    @Test
    void onlyRequiresWorldForEnabledTeleportingSequence() {
        assertFalse(LoadConfig.requiresOnboardingWorld(false, false));
        assertFalse(LoadConfig.requiresOnboardingWorld(false, true));
        assertFalse(LoadConfig.requiresOnboardingWorld(true, false));
        assertTrue(LoadConfig.requiresOnboardingWorld(true, true));
    }

    @Test
    void usesSequenceDefaultsAndPerStepOverrides() {
        var steps = LoadConfig.loadTitleContents(
                List.of(
                        Map.of("TITLE", "Default timing"),
                        Map.of(
                                "SUBTITLE", "Custom timing",
                                "CHAT", "https://example.com",
                                "FADE_IN", 0,
                                "DURATION", 9,
                                "FADE_OUT", 2
                        )
                ),
                1,
                5,
                1
        );

        assertEquals(
                new ConfigData.TitleContent(
                        "Default timing", "", "", 1, 5, 1, false, null, List.of(), List.of()
                ),
                steps.getFirst()
        );
        assertEquals(
                new ConfigData.TitleContent(
                        "",
                        "Custom timing",
                        "https://example.com",
                        0,
                        9,
                        2,
                        false,
                        null,
                        List.of(),
                        List.of()
                ),
                steps.get(1)
        );
    }

    @Test
    void clampsNegativePerStepTimings() {
        var step = LoadConfig.loadTitleContents(
                List.of(Map.of("DURATION", -5)),
                1,
                5,
                1
        ).getFirst();

        assertEquals(0, step.duration());
    }

    @Test
    void loadsSoundAndPotionEffects() {
        var step = LoadConfig.loadTitleContents(
                List.of(Map.of(
                        "SOUND", Map.of(
                                "NAME", "minecraft:block.note_block.pling",
                                "CATEGORY", "players",
                                "VOLUME", 0.5,
                                "PITCH", 1.2
                        ),
                        "STOP_ALL_SOUNDS", true,
                        "POTION_EFFECTS", List.of(Map.of(
                                "TYPE", "minecraft:blindness",
                                "DURATION", -1,
                                "AMPLIFIER", 1,
                                "PARTICLES", false
                        )),
                        "REMOVE_POTION_EFFECTS", List.of("minecraft:night_vision")
                )),
                1,
                5,
                1
        ).getFirst();

        assertEquals("minecraft:block.note_block.pling", step.sound().name());
        assertTrue(step.stopAllSounds());
        assertEquals(org.bukkit.SoundCategory.PLAYERS, step.sound().category());
        assertEquals("minecraft:blindness", step.potionEffects().getFirst().type());
        assertEquals(-1, step.potionEffects().getFirst().duration());
        assertEquals(List.of("minecraft:night_vision"), step.removePotionEffects());
    }
}
