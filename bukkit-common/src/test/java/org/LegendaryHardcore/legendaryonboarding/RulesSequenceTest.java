package org.LegendaryHardcore.legendaryonboarding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RulesSequenceTest {
    @Test
    void roundsCountdownUpToWholeSeconds() {
        assertEquals(0, RulesSequence.countdownSeconds(0));
        assertEquals(1, RulesSequence.countdownSeconds(1));
        assertEquals(1, RulesSequence.countdownSeconds(20));
        assertEquals(2, RulesSequence.countdownSeconds(21));
    }

    @Test
    void appliesCountdownPlaceholder() {
        assertEquals(
                "Accept unlocks in 12s",
                RulesSequence.applyCountdownPlaceholder("Accept unlocks in {seconds}s", 12)
        );
    }

    @Test
    void appliesWelcomePlaceholdersWithoutChangingTiming() {
        var content = new ConfigData.TitleContent(
                "Welcome to {server_name}, {player_name}!",
                "{player_name} joined {server_name}",
                "Hello {player_name}",
                1,
                3,
                1,
                false,
                null,
                java.util.List.of(),
                java.util.List.of()
        );

        assertEquals(
                new ConfigData.TitleContent(
                        "Welcome to Example Server, Alex!",
                        "Alex joined Example Server",
                        "Hello Alex",
                        1,
                        3,
                        1,
                        false,
                        null,
                        java.util.List.of(),
                        java.util.List.of()
                ),
                RulesSequence.applyPlaceholders(content, "Example Server", "Alex")
        );
    }
}
