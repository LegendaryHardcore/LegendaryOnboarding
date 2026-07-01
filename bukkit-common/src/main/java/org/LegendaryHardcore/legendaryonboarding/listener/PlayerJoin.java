package org.LegendaryHardcore.legendaryonboarding.listener;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PlayerJoin implements Listener {
    private final LegendaryOnboarding plugin;

    public PlayerJoin(LegendaryOnboarding plugin) {

        this.plugin = plugin;
    }

    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        // Always record their latest name
        plugin.beginPlayerSession(uuid);
        plugin.getAcceptedStore().recordSeenName(uuid, player.getName());
        plugin.synchronizeTabVisibility(player);
        plugin.consumeDeferredDebugFix(player);

        boolean sequenceEnabled = plugin.getConfigData().isTitleSequenceEnabled();
        boolean hasPending = plugin.getPendingStore().hasPending(uuid);
        boolean cleanupRequired =
                hasPending && plugin.getPendingStore().isCleanupRequired(uuid);
        if (cleanupRequired) {
            player.setInvulnerable(true);
            player.setFireTicks(0);
            player.setFallDistance(0f);
            plugin.releasePendingPlayer(player);
            return;
        }

        if (!sequenceEnabled) {
            if (shouldReleasePendingPlayer(sequenceEnabled, hasPending)) {
                plugin.releasePendingPlayer(player);
            }
            return;
        }

        final boolean accepted = plugin.getAcceptedStore().isAccepted(uuid);
        final boolean forceOnboarding = plugin.getConfigData().isDebugForceOnboarding();
        final boolean onboardReturning =
                plugin.getConfigData().isOnboardUnacceptedReturningPlayers();
        final boolean debugForced = forceOnboarding && (accepted || player.hasPlayedBefore());

        // If accepted, ensure no stale onboarding state remains, then bail
        if (accepted && !forceOnboarding) {
            if (hasPending) {
                plugin.releasePendingPlayer(player);
                return;
            }
            plugin.debugForcedPlayers.remove(uuid);
            plugin.clearSequencePotionEffects(player);
            plugin.canAcceptRules.remove(uuid);
            plugin.acceptInProgress.remove(uuid);

            var task = plugin.movementLocks.remove(uuid);
            if (task != null) task.cancel();

            return;
        }

        // If they are NOT accepted but have pending, they disconnected
        // mid-onboarding. Their current login location is authoritative: the
        // plugin may have been disabled while they played and moved elsewhere.
        if (hasPending) {
            event.joinMessage(null);
            boolean resumingDebug =
                    debugForced || plugin.getPendingStore().isDebugSession(uuid);
            boolean announceWhenComplete =
                    !resumingDebug && plugin.getPendingStore().shouldAnnounceWhenComplete(uuid);
            plugin.resumeOnboardingFromCurrentLocation(
                    player,
                    announceWhenComplete,
                    resumingDebug
            );
            return;
        }

        // Returning players are ignored unless testing explicitly opts them in.
        if (!shouldOnboardReturningPlayer(
                player.hasPlayedBefore(),
                forceOnboarding,
                onboardReturning
        )) {
            // Not accepted, no pending, returning player -> do nothing onboarding-related
            plugin.canAcceptRules.remove(uuid);
            plugin.acceptInProgress.remove(uuid);
            return;
        }

        event.joinMessage(null);
        plugin.startOnboarding(player, !forceOnboarding, debugForced);
    }

    static boolean shouldReleasePendingPlayer(boolean sequenceEnabled, boolean hasPending) {
        return !sequenceEnabled && hasPending;
    }

    static boolean shouldOnboardReturningPlayer(
            boolean hasPlayedBefore,
            boolean forceOnboarding,
            boolean onboardUnacceptedReturningPlayers
    ) {
        return !hasPlayedBefore || forceOnboarding || onboardUnacceptedReturningPlayers;
    }

}
