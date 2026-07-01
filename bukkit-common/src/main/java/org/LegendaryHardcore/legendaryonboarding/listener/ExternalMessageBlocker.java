package org.LegendaryHardcore.legendaryonboarding.listener;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.BroadcastMessageEvent;

public final class ExternalMessageBlocker implements Listener {
    private final LegendaryOnboarding plugin;

    public ExternalMessageBlocker(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBroadcast(BroadcastMessageEvent event) {
        if (!plugin.getConfigData().isBlockExternalMessages()) return;
        event.getRecipients().removeIf(recipient ->
                recipient instanceof Player player
                        && plugin.isOnboardingActive(player.getUniqueId())
        );
    }
}
