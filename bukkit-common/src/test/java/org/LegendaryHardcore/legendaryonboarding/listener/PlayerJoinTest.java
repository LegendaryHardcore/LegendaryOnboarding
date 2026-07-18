package org.LegendaryHardcore.legendaryonboarding.listener;

import org.LegendaryHardcore.legendaryonboarding.ConfigData.MessageConsumption;
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

    @Test
    void messageConsumptionModesMatchExpectedSuppression() {
        assertFalse(PlayerJoin.shouldConsumeMessage(MessageConsumption.NONE, true));
        assertFalse(PlayerJoin.shouldConsumeMessage(MessageConsumption.SOME, false));
        assertTrue(PlayerJoin.shouldConsumeMessage(MessageConsumption.SOME, true));
        assertTrue(PlayerJoin.shouldConsumeMessage(MessageConsumption.ALL, false));
    }

    @Test
    void onlyAllModeRedistributesInGameMessages() {
        assertFalse(PlayerJoin.shouldRedistributeInGame(MessageConsumption.NONE));
        assertFalse(PlayerJoin.shouldRedistributeInGame(MessageConsumption.SOME));
        assertTrue(PlayerJoin.shouldRedistributeInGame(MessageConsumption.ALL));
    }

    @Test
    void onlyNonOnboardingAllModeRedistributesDiscordSrvMessages() {
        assertFalse(PlayerJoin.shouldRedistributeDiscordSrv(MessageConsumption.NONE, false));
        assertFalse(PlayerJoin.shouldRedistributeDiscordSrv(MessageConsumption.SOME, false));
        assertFalse(PlayerJoin.shouldRedistributeDiscordSrv(MessageConsumption.ALL, true));
        assertTrue(PlayerJoin.shouldRedistributeDiscordSrv(MessageConsumption.ALL, false));
    }

    @Test
    void consumesJoinMessageWhenPlayerWillEnterOrResumeOnboarding() {
        assertTrue(PlayerJoin.shouldConsumeOwnJoinMessage(
                true,
                false,
                false,
                false,
                false,
                false,
                false
        ));
        assertTrue(PlayerJoin.shouldConsumeOwnJoinMessage(
                true,
                true,
                false,
                false,
                false,
                true,
                false
        ));
        assertFalse(PlayerJoin.shouldConsumeOwnJoinMessage(
                true,
                false,
                false,
                true,
                false,
                true,
                false
        ));
        assertFalse(PlayerJoin.shouldConsumeOwnJoinMessage(
                false,
                false,
                false,
                false,
                false,
                true,
                false
        ));
    }
}
