package org.LegendaryHardcore.legendaryonboarding.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDataSnapshotStoreTest {
    @Test
    void pendingStatusAloneDoesNotRestoreAnOldSnapshot() {
        assertFalse(PlayerDataSnapshotStore.shouldRestoreOfflineSnapshot(
                true,
                false,
                false
        ));
    }

    @Test
    void restoresOnlyMarkedOfflinePendingPlayers() {
        assertTrue(PlayerDataSnapshotStore.shouldRestoreOfflineSnapshot(
                true,
                false,
                true
        ));
        assertFalse(PlayerDataSnapshotStore.shouldRestoreOfflineSnapshot(
                true,
                true,
                true
        ));
        assertFalse(PlayerDataSnapshotStore.shouldRestoreOfflineSnapshot(
                false,
                false,
                true
        ));
    }
}
