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

        final boolean accepted = plugin.getAcceptedStore().isAccepted(uuid);
        final boolean hasPending = plugin.getPendingStore().hasPending(uuid);

        // If accepted, ensure no stale onboarding state remains, then bail
        if (accepted) {
            plugin.canAcceptRules.remove(uuid);
            plugin.acceptInProgress.remove(uuid);

            var task = plugin.movementLocks.remove(uuid);
            if (task != null) task.cancel();

            // If they somehow still had pending from an old crash, clear it
            if (hasPending) plugin.getPendingStore().clearPending(uuid);

            return;
        }

        // If they are NOT accepted but have pending, they disconnected mid-onboarding.
        // Resume onboarding (do NOT allow skipping)
        if (hasPending) {
            plugin.canAcceptRules.put(uuid, false);

            player.getScheduler().run(plugin, task -> plugin.getRulesSequence().start(player), null);
            return;
        }

        // Do not grab players that have played before
        if (player.hasPlayedBefore()) {
            // Not accepted, no pending, returning player -> do nothing onboarding-related
            plugin.canAcceptRules.remove(uuid);
            plugin.acceptInProgress.remove(uuid);
            return;
        }

        // Brand new player: start onboarding
        plugin.canAcceptRules.put(uuid, false);

        // Save their return location once
        plugin.getPendingStore().setPending(uuid, player.getLocation());

        // Start onboarding Sequence
        player.getScheduler().run(plugin, task -> plugin.getRulesSequence().start(player), null);
    }
}