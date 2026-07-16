package org.LegendaryHardcore.legendaryonboarding.listener;

import net.kyori.adventure.text.Component;
import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public final class StandardAnnouncementBlocker implements Listener {
    private final LegendaryOnboarding plugin;

    public StandardAnnouncementBlocker(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Component message = event.message();
        if (!shouldFilter() || message == null) return;
        event.message(null);
        sendToEligiblePlayers(message);
    }

    private boolean shouldFilter() {
        return plugin.getConfigData().isBlockExternalMessages();
    }

    private void sendToEligiblePlayers(Component message) {
        plugin.sendToNonOnboardingPlayers(message);
    }
}
