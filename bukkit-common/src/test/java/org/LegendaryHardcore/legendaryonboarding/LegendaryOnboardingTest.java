package org.LegendaryHardcore.legendaryonboarding;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegendaryOnboardingTest {
    @Test
    void includesPendingPlayersWhenReleasingOnReload() {
        UUID accepting = UUID.randomUUID();
        UUID joining = UUID.randomUUID();
        UUID debugForced = UUID.randomUUID();
        UUID pending = UUID.randomUUID();

        assertEquals(
                Set.of(accepting, joining, debugForced, pending),
                LegendaryOnboarding.onboardingPlayersForRelease(
                        Set.of(accepting),
                        Set.of(joining),
                        Set.of(debugForced),
                        Set.of(pending)
                )
        );
    }

    @Test
    void randomFallbackCoordinateStaysInsideInclusiveRadius() {
        Random random = new Random(12345);
        for (int attempt = 0; attempt < 10_000; attempt++) {
            double coordinate = LegendaryOnboarding.randomFallbackCoordinate(
                    250.5,
                    5000,
                    random
            );
            assertTrue(coordinate >= -4749.5);
            assertTrue(coordinate <= 5250.5);
        }
    }

    @Test
    void zeroFallbackRadiusKeepsConfiguredCoordinate() {
        assertEquals(
                123.75,
                LegendaryOnboarding.randomFallbackCoordinate(
                        123.75,
                        0,
                        new Random(1)
                )
        );
    }

    @Test
    void restoresDepartingPlayersWithRuntimeOrPendingOnboardingState() {
        assertTrue(LegendaryOnboarding.shouldRestoreDepartingPlayer(true, false));
        assertTrue(LegendaryOnboarding.shouldRestoreDepartingPlayer(false, true));
        org.junit.jupiter.api.Assertions.assertFalse(
                LegendaryOnboarding.shouldRestoreDepartingPlayer(false, false)
        );
    }

    @Test
    void ordinaryDisconnectsDoNotPreserveCleanupMode() {
        org.junit.jupiter.api.Assertions.assertFalse(
                LegendaryOnboarding.shouldPreserveCleanupOnDeferredDeparture(false)
        );
        assertTrue(LegendaryOnboarding.shouldPreserveCleanupOnDeferredDeparture(true));
    }

    @Test
    void unfinishedShutdownSessionResumesInsteadOfCleaningUp() {
        assertTrue(LegendaryOnboarding.shouldResumePendingPlayer(
                true,
                false,
                false,
                false
        ));
        assertTrue(LegendaryOnboarding.shouldResumePendingPlayer(
                true,
                false,
                true,
                true
        ));
        org.junit.jupiter.api.Assertions.assertFalse(
                LegendaryOnboarding.shouldResumePendingPlayer(
                        true,
                        true,
                        false,
                        false
                )
        );
        org.junit.jupiter.api.Assertions.assertFalse(
                LegendaryOnboarding.shouldResumePendingPlayer(
                        false,
                        false,
                        false,
                        false
                )
        );
        org.junit.jupiter.api.Assertions.assertFalse(
                LegendaryOnboarding.shouldResumePendingPlayer(
                        true,
                        false,
                        true,
                        false
                )
        );
    }

    @Test
    void shutdownOnlyPreservesCleanupForCleanupAlreadyInProgress() {
        org.junit.jupiter.api.Assertions.assertFalse(
                LegendaryOnboarding.shouldMarkCleanupOnDisable(false, false)
        );
        assertTrue(LegendaryOnboarding.shouldMarkCleanupOnDisable(true, false));
        assertTrue(LegendaryOnboarding.shouldMarkCleanupOnDisable(false, true));
    }

    @Test
    void forwardsCompletionAnnouncementWhenDiscordSrvNativeJoinWasSuppressed() {
        org.junit.jupiter.api.Assertions.assertFalse(
                LegendaryOnboarding.shouldForwardFirstJoinToDiscord(
                        ConfigData.MessageConsumption.NONE
                )
        );
        assertTrue(LegendaryOnboarding.shouldForwardFirstJoinToDiscord(
                ConfigData.MessageConsumption.SOME
        ));
        assertTrue(LegendaryOnboarding.shouldForwardFirstJoinToDiscord(
                ConfigData.MessageConsumption.ALL
        ));
    }
}
