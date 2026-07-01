package org.LegendaryHardcore.legendaryonboarding.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadCommandTest {
    @Test
    void completesAdminSubcommandsCaseInsensitively() {
        assertEquals(
                List.of("help", "reload", "status", "debug"),
                ReloadCommand.completeFirstArgument("")
        );
        assertEquals(List.of("help"), ReloadCommand.completeFirstArgument("he"));
        assertEquals(List.of("reload"), ReloadCommand.completeFirstArgument("re"));
        assertEquals(List.of("reload"), ReloadCommand.completeFirstArgument("REL"));
        assertEquals(List.of("status"), ReloadCommand.completeFirstArgument("sta"));
        assertEquals(List.of("debug"), ReloadCommand.completeFirstArgument("DEB"));
    }

    @Test
    void rejectsUnknownOrCompleteExtraText() {
        assertTrue(ReloadCommand.completeFirstArgument("settings").isEmpty());
        assertTrue(ReloadCommand.completeFirstArgument("reload-now").isEmpty());
    }

    @Test
    void completesAcceptedStateValues() {
        assertEquals(
                List.of("true", "false", "remove"),
                ReloadCommand.matching(List.of("true", "false", "remove"), "")
        );
        assertEquals(
                List.of("remove"),
                ReloadCommand.matching(List.of("true", "false", "remove"), "rem")
        );
    }

    @Test
    void helpOnlyShowsCommandsTheSenderCanUse() {
        var playerEntries = ReloadCommand.visibleHelpEntries(
                permission -> permission.equals("legendaryonboarding.accept")
        );
        assertEquals(
                List.of("/accept", "/lo help"),
                playerEntries.stream().map(ReloadCommand.HelpEntry::usage).toList()
        );

        var adminEntries = ReloadCommand.visibleHelpEntries(permission -> true);
        assertEquals(8, adminEntries.size());
        assertTrue(adminEntries.stream()
                .map(ReloadCommand.HelpEntry::usage)
                .toList()
                .contains("/lo reload"));
        assertTrue(adminEntries.stream()
                .map(ReloadCommand.HelpEntry::usage)
                .toList()
                .contains("/lo debug fix <playername | UUID>"));
        assertTrue(adminEntries.stream()
                .map(ReloadCommand.HelpEntry::usage)
                .toList()
                .contains("/lo debug isAccepted <playername | UUID> <true | false | remove>"));
    }

    @Test
    void debugStartRequiresPlayerToBeOutsideOnboarding() {
        assertTrue(ReloadCommand.canDebugStart(false));
        org.junit.jupiter.api.Assertions.assertFalse(
                ReloadCommand.canDebugStart(true)
        );
    }

    @Test
    void debugEndRequiresPlayerToBeOnboarding() {
        assertTrue(ReloadCommand.canDebugEnd(true));
        org.junit.jupiter.api.Assertions.assertFalse(
                ReloadCommand.canDebugEnd(false)
        );
    }
}
