package org.LegendaryHardcore.legendaryonboarding.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingStoreTest {
    @Test
    void cleanupOnlyEntryDoesNotRequireALocation() {
        PendingStore.PendingEntry entry = PendingStore.PendingEntry.cleanupOnly();

        assertNull(entry.worldName());
        assertTrue(entry.cleanupRequired());
        assertFalse(entry.debugSession());
        assertFalse(entry.announceWhenComplete());
    }

    @Test
    void markingCleanupPreservesReturnLocationAndSessionIntent() {
        PendingStore.PendingEntry existing = new PendingStore.PendingEntry(
                "world",
                12.5,
                64,
                -3.5,
                90,
                10,
                false,
                true,
                false
        );
        PendingStore.PendingEntry entry = PendingStore.cleanupRequiredEntry(existing);

        assertTrue(entry.cleanupRequired());
        assertTrue(entry.debugSession());
        assertFalse(entry.announceWhenComplete());
        assertTrue(entry.worldName().equals("world"));
    }

    @Test
    void configuringSessionClearsCleanupMarker() {
        PendingStore.PendingEntry entry = PendingStore.PendingEntry.cleanupOnly()
                .withSession(true, false);

        assertFalse(entry.cleanupRequired());
        assertTrue(entry.debugSession());
        assertFalse(entry.announceWhenComplete());
    }

    @Test
    void configuringSessionPreservesSavedReturnLocation() {
        PendingStore.PendingEntry entry = new PendingStore.PendingEntry(
                "world_the_end",
                21.25,
                255,
                -40.5,
                180,
                -15,
                true,
                false,
                false
        ).withSession(false, true);

        assertFalse(entry.cleanupRequired());
        assertFalse(entry.debugSession());
        assertTrue(entry.announceWhenComplete());
        assertEquals("world_the_end", entry.worldName());
        assertEquals(21.25, entry.x());
        assertEquals(255, entry.y());
        assertEquals(-40.5, entry.z());
    }
}
