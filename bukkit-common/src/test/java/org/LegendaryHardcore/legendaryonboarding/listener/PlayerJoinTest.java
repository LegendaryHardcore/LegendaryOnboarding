package org.LegendaryHardcore.legendaryonboarding.listener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerJoinTest {
    @Test
    void releasesPendingPlayerWhenSequenceIsDisabled() {
        assertTrue(PlayerJoin.shouldReleasePendingPlayer(false, true));
    }

    @Test
    void leavesOtherJoinCasesAlone() {
        assertFalse(PlayerJoin.shouldReleasePendingPlayer(true, true));
        assertFalse(PlayerJoin.shouldReleasePendingPlayer(false, false));
    }

    @Test
    void returningPlayersRequireExplicitOptIn() {
        assertFalse(PlayerJoin.shouldOnboardReturningPlayer(true, false, false));
        assertTrue(PlayerJoin.shouldOnboardReturningPlayer(true, false, true));
        assertTrue(PlayerJoin.shouldOnboardReturningPlayer(true, true, false));
        assertTrue(PlayerJoin.shouldOnboardReturningPlayer(false, false, false));
    }
}
