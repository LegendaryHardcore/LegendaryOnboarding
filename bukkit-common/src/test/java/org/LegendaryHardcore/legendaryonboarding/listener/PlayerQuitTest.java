package org.LegendaryHardcore.legendaryonboarding.listener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerQuitTest {
    @Test
    void suppressesOnlyOnboardingPlayerQuitMessages() {
        assertTrue(PlayerQuit.shouldSuppressQuitMessage(true));
        assertFalse(PlayerQuit.shouldSuppressQuitMessage(false));
    }
}
