package org.LegendaryHardcore.legendaryonboarding.listener;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Collection;

public class PlayerJoin implements Listener {
    private final LegendaryOnboarding plugin;

    public PlayerJoin(LegendaryOnboarding plugin) {

        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        // Always record their latest name
        plugin.getAcceptedStore().recordSeenName(uuid, player.getName());

        // If the player has already accepted, do nothing onboarding-related.
        if (plugin.getAcceptedStore().isAccepted(uuid)) {
            plugin.canAcceptRules.put(uuid, false);
            return;
        }

        // Ensure command can't be used until rules sequence enables it
        plugin.canAcceptRules.put(uuid, false);

        // Save the player's 'return to' location once (only if not already stored)
        if (!plugin.getPendingStore().hasPending(uuid)) {
            plugin.getPendingStore().setPending(uuid, player.getLocation());
        }

        player.getScheduler().run(plugin, task -> plugin.getRulesSequence().start(player),
                null
        );
    }
}