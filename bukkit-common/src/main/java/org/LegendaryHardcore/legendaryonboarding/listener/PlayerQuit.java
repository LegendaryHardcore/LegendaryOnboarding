package org.LegendaryHardcore.legendaryonboarding.listener;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuit implements Listener {
    private final LegendaryOnboarding plugin;

    public PlayerQuit(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    public void suppressOnboardingQuitMessage(PlayerQuitEvent event) {
        if (shouldSuppressQuitMessage(
                plugin.isOnboardingActive(event.getPlayer().getUniqueId())
        )) {
            event.quitMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        plugin.preparePlayerForDeparture(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.preparePlayerForDeparture(event.getPlayer());
        plugin.scheduleDepartureSnapshotRestore(event.getPlayer().getUniqueId());
    }

    static boolean shouldSuppressQuitMessage(boolean onboardingActive) {
        return onboardingActive;
    }
}
