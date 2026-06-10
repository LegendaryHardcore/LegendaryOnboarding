package org.LegendaryHardcore.legendaryonboarding.listener;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerQuit implements Listener {
    private final LegendaryOnboarding plugin;

    public PlayerQuit(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // pending.yml remains authoritative while the player disconnects, so damage
        // protection continues through the tail end of the quit lifecycle.
        plugin.canAcceptRules.remove(uuid);
        plugin.acceptInProgress.remove(uuid);
        plugin.joinSequenceActive.remove(uuid);
        plugin.stopCountdown(uuid);

        var task = plugin.movementLocks.remove(uuid);
        if (task != null) task.cancel();
    }
}
