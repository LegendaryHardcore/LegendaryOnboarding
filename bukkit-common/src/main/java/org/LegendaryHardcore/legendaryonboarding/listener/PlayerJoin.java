package org.LegendaryHardcore.legendaryonboarding.listener;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PlayerJoin implements Listener {
    private final LegendaryOnboarding plugin;

    public PlayerJoin(LegendaryOnboarding plugin) {

        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        // Always record their latest name
        plugin.getAcceptedStore().recordSeenName(uuid, player.getName());

        if (!plugin.getConfigData().isTitleSequenceEnabled()) {
            return;
        }

        final boolean accepted = plugin.getAcceptedStore().isAccepted(uuid);
        final boolean hasPending = plugin.getPendingStore().hasPending(uuid);
        final boolean forceOnboarding = plugin.getConfigData().isDebugForceOnboarding();
        final boolean debugForced = forceOnboarding && (accepted || player.hasPlayedBefore());
        if (debugForced) {
            plugin.debugForcedPlayers.add(uuid);
        }

        // If accepted, ensure no stale onboarding state remains, then bail
        if (accepted && !forceOnboarding) {
            plugin.debugForcedPlayers.remove(uuid);
            plugin.clearSequencePotionEffects(player);
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
            beginProtection(player);

            plugin.getPlatformScheduler().runEntity(player, () -> plugin.getRulesSequence().start(player));
            return;
        }

        // Do not grab players that have played before
        if (player.hasPlayedBefore() && !forceOnboarding) {
            // Not accepted, no pending, returning player -> do nothing onboarding-related
            plugin.canAcceptRules.remove(uuid);
            plugin.acceptInProgress.remove(uuid);
            return;
        }

        // Brand new player: start onboarding
        beginProtection(player);

        // Save their return location once
        plugin.getPendingStore().setPending(uuid, player.getLocation());

        // Start onboarding Sequence
        plugin.getPlatformScheduler().runEntity(player, () -> plugin.getRulesSequence().start(player));
    }

    private void beginProtection(Player player) {
        plugin.canAcceptRules.put(player.getUniqueId(), false);
        player.setInvulnerable(true);
        player.setFireTicks(0);
        player.setFallDistance(0f);
    }
}
