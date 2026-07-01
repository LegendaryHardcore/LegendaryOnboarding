package org.LegendaryHardcore.legendaryonboarding.listener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCommandBlockerTest {
    @Test
    void allowsRootAndHelpCommandsDuringOnboarding() {
        assertTrue(PlayerCommandBlocker.isOnboardingHelpCommand("/lo"));
        assertTrue(PlayerCommandBlocker.isOnboardingHelpCommand("/lo help"));
        assertTrue(PlayerCommandBlocker.isOnboardingHelpCommand(
                "/legendaryonboarding help"
        ));
        assertTrue(PlayerCommandBlocker.isOnboardingHelpCommand(
                "/legendaryonboarding:legendaryonboarding HELP"
        ));
    }

    @Test
    void doesNotBypassOtherManagementCommands() {
        assertFalse(PlayerCommandBlocker.isOnboardingHelpCommand("/lo reload"));
        assertFalse(PlayerCommandBlocker.isOnboardingHelpCommand(
                "/lo debug end Tester"
        ));
        assertFalse(PlayerCommandBlocker.isOnboardingHelpCommand("/help"));
        assertFalse(PlayerCommandBlocker.isOnboardingHelpCommand("lo help"));
    }
}
