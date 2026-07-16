package org.LegendaryHardcore.legendaryonboarding;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
            assertFalse(config.getBoolean("DEBUG_LOGGING", true));
            assertTrue(config.getBoolean("BLOCK_ADVANCEMENTS", false));
            assertFalse(config.getBoolean("ONBOARD_UNACCEPTED_RETURNING_PLAYERS", true));
            assertTrue(config.getBoolean("BLOCK_EXTERNAL_MESSAGES_DURING_ONBOARDING", false));
            assertEquals("SOME", config.getString("MESSAGE_CONSUMPTION.IN_GAME.JOIN"));
            assertEquals("SOME", config.getString("MESSAGE_CONSUMPTION.IN_GAME.QUIT"));
            assertEquals("SOME", config.getString("MESSAGE_CONSUMPTION.DISCORDSRV.JOIN"));
            assertEquals("SOME", config.getString("MESSAGE_CONSUMPTION.DISCORDSRV.QUIT"));
            assertEquals("MONITOR", config.getString("EVENT_PRIORITIES.CHAT_CONSUMPTION"));
            assertEquals("HIGHEST", config.getString("EVENT_PRIORITIES.JOIN_MESSAGE"));
            assertEquals("HIGHEST", config.getString("EVENT_PRIORITIES.QUIT_MESSAGE"));
            assertTrue(config.getBoolean("HIDE_ONBOARDING_PLAYERS_FROM_TAB", false));
            assertFalse(config.getBoolean("CLEANUP_FALLBACK_ENABLED", true));
            assertEquals("world", config.getString("CLEANUP_FALLBACK_WORLD"));
            assertEquals("NORMAL", config.getString("CLEANUP_FALLBACK_WORLD_TYPE"));
            assertEquals(0.0, config.getDouble("CLEANUP_FALLBACK_X"));
            assertEquals(64.0, config.getDouble("CLEANUP_FALLBACK_Y"));
            assertEquals(0.0, config.getDouble("CLEANUP_FALLBACK_Z"));
            assertEquals(5000, config.getInt("CLEANUP_FALLBACK_RADIUS"));
            assertEquals(64, config.getInt("RETURN_DESIRED_Y"));
            assertEquals("NORMAL", config.getString("POS_WORLD_TYPE"));
            assertEquals(
                    "{yellow}{player} is currently onboarding.",
                    config.getString("ONBOARDING_DAMAGE_MESSAGE")
            );
            assertEquals(
                    "{yellow}{player} joined the server for the first time",
                    config.getString("FIRST_JOIN_MESSAGE")
            );
        }
    }

    @Test
    void bundledSequenceIsDisabledUntilConfigured() throws Exception {
        try (InputStream defaults = LoadConfigTest.class.getClassLoader()
                .getResourceAsStream("titlesequence.yml")) {
            assertNotNull(defaults);
            var config = ConfigUpdater.load(defaults);
            assertFalse(config.getBoolean("ENABLED", true));
            assertFalse(config.getString("ACTIONBAR_COUNTDOWN", "").isBlank());
            assertEquals(20, config.getInt("ACTIONBAR_REFRESH_TICKS"));
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
    void boundsConfiguredFallbackRadius() {
        assertEquals(0, LoadConfig.boundedFallbackRadius(-1));
        assertEquals(0, LoadConfig.boundedFallbackRadius(0));
        assertEquals(5000, LoadConfig.boundedFallbackRadius(5000));
        assertEquals(30_000_000, LoadConfig.boundedFallbackRadius(Integer.MAX_VALUE));
    }

    @Test
    void usesDefaultForNonPositiveActionBarRefreshTicks() {
        assertEquals(20, LoadConfig.positiveOrDefault(-1, 20));
        assertEquals(20, LoadConfig.positiveOrDefault(0, 20));
        assertEquals(5, LoadConfig.positiveOrDefault(5, 20));
    }

    @Test
    void parsesWorldEnvironmentAliasesAndDefaults() {
        assertEquals(
                org.bukkit.World.Environment.NORMAL,
                LoadConfig.parseWorldEnvironment(null)
        );
        assertEquals(
                org.bukkit.World.Environment.NORMAL,
                LoadConfig.parseWorldEnvironment("overworld")
        );
        assertEquals(
                org.bukkit.World.Environment.NETHER,
                LoadConfig.parseWorldEnvironment("nether")
        );
        assertEquals(
                org.bukkit.World.Environment.THE_END,
                LoadConfig.parseWorldEnvironment("end")
        );
        assertEquals(
                org.bukkit.World.Environment.THE_END,
                LoadConfig.parseWorldEnvironment("THE_END")
        );
    }

    @Test
    void parsesConfiguredEventPrioritiesCaseInsensitively() {
        assertEquals(
                org.bukkit.event.EventPriority.MONITOR,
                LoadConfig.parseEventPriority("monitor")
        );
        assertEquals(
                org.bukkit.event.EventPriority.HIGHEST,
                LoadConfig.parseEventPriority(" HIGHEST ")
        );
        assertNull(LoadConfig.parseEventPriority("after-everything"));
        assertNull(LoadConfig.parseEventPriority(null));
    }

    @Test
    void parsesMessageConsumptionCaseInsensitively() {
        assertEquals(
                ConfigData.MessageConsumption.NONE,
                LoadConfig.parseMessageConsumption(" none ")
        );
        assertEquals(
                ConfigData.MessageConsumption.ALL,
                LoadConfig.parseMessageConsumption("ALL")
        );
        assertNull(LoadConfig.parseMessageConsumption("sometimes"));
        assertNull(LoadConfig.parseMessageConsumption(null));
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
