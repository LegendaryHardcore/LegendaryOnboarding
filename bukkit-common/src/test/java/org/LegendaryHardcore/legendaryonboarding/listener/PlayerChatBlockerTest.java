package org.LegendaryHardcore.legendaryonboarding.listener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerChatBlockerTest {
    @Test
    void activeOnboardingAlwaysBlocksOutgoingChat() {
        assertTrue(PlayerChatBlocker.shouldBlockSender(true));
    }

    @Test
    void inactivePlayersCanChatRegardlessOfAcceptanceHistory() {
        assertFalse(PlayerChatBlocker.shouldBlockSender(false));
    }
}
