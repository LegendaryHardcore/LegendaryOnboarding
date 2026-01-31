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

        plugin.canAcceptRules.remove(event.getPlayer().getUniqueId());
        plugin.acceptInProgress.remove(event.getPlayer().getUniqueId());
        UUID uuid = event.getPlayer().getUniqueId();

        plugin.joinSequenceActive.remove(uuid);

        var task = plugin.movementLocks.remove(uuid);
        if (task != null) task.cancel();
    }
}