package org.LegendaryHardcore.legendaryonboarding.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadCommandTest {
    @Test
    void completesReloadCaseInsensitively() {
        assertEquals(List.of("reload"), ReloadCommand.completeFirstArgument(""));
        assertEquals(List.of("reload"), ReloadCommand.completeFirstArgument("re"));
        assertEquals(List.of("reload"), ReloadCommand.completeFirstArgument("REL"));
    }

    @Test
    void rejectsUnknownOrCompleteExtraText() {
        assertTrue(ReloadCommand.completeFirstArgument("status").isEmpty());
        assertTrue(ReloadCommand.completeFirstArgument("reload-now").isEmpty());
    }
}
