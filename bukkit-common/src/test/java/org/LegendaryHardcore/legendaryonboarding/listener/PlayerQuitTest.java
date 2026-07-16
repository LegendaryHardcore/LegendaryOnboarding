package org.LegendaryHardcore.legendaryonboarding.listener;

import org.LegendaryHardcore.legendaryonboarding.ConfigData.MessageConsumption;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerQuitTest {
    @Test
    void suppressesOnlyOnboardingPlayerQuitMessages() {
        assertTrue(PlayerQuit.shouldSuppressQuitMessage(true));
        assertFalse(PlayerQuit.shouldSuppressQuitMessage(false));
    }

    @Test
    void someModeMatchesLegacyQuitBehavior() {
        assertTrue(PlayerJoin.shouldConsumeMessage(MessageConsumption.SOME, true));
        assertFalse(PlayerJoin.shouldConsumeMessage(MessageConsumption.SOME, false));
    }
}
