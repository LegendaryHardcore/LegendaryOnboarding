package org.LegendaryHardcore.legendaryonboarding;

import org.LegendaryHardcore.legendaryonboarding.ConfigData.TitleContent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.UUID;

/*
 *  Class for the join sequence after accepting rules.
 *  Displays join messages and assigns player to post-onboard group
 */
public class JoinSequence {
    private final LegendaryOnboarding plugin;

    // For converting seconds to ticks (1 second = 20 ticks)
    private static final int TPS = 20;

    public JoinSequence(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    public void start(Player player) {
        if (!plugin.getConfigData().isTitleSequenceEnabled()) return;

        final UUID uuid = player.getUniqueId();

        // HARD GUARD: only allow one join sequence at a time
        if (!plugin.joinSequenceActive.add(uuid)) {
            plugin.acceptInProgress.remove(uuid); // avoid deadlock if this was spammed
            return;
        }

        // Reset so player can no longer run /accept
        plugin.canAcceptRules.put(uuid, false);
        plugin.startMovementLock(player, player.getLocation(), true);

        // Join Sequence title timing
        List<TitleContent> contents = plugin.getConfigData().getJoinSequenceContent();
        long currentDelay = 1L;

        // For each join sequence message, schedule each title/subtitles
        for (TitleContent content : contents) {
            final TitleContent c = content;
            final long scheduledDelay = currentDelay;

            plugin.getPlatformScheduler().runEntityDelayed(player, () -> {
                if (!isActive(player)) return;
                RulesSequence.showStep(plugin, player, c);
            }, min1(scheduledDelay));

            currentDelay += RulesSequence.stepTicks(c);
        }

        // Final step after titles
        final long finishDelay = currentDelay;

        plugin.getPlatformScheduler().runEntityDelayed(player, () -> {
            if (!isActive(player)) {
                clearGuards(uuid);
                return;
            }

            returnToPendingSoftSafe(player);
        }, min1(finishDelay));
    }

    private void returnToPendingSoftSafe(Player player) {
        final UUID uuid = player.getUniqueId();

        plugin.getPlatformScheduler().runEntity(player, () -> {
            if (!isActive(player)) {
                clearGuards(uuid);
                return;
            }

            // Stop pinning the player before teleporting away from the onboarding location.
            plugin.stopMovementLock(uuid);
            plugin.resolveSafeReturnLocation(
                    player,
                    plugin.getPendingStore().getPendingLocation(uuid),
                    target -> {
                        if (target == null) {
                            holdForReturnRecovery(player);
                            return;
                        }
                        teleportAndRelease(player, target, true);
                    }
            );
        });
    }

    private void teleportAndRelease(Player player, Location target, boolean allowSpawnFallback) {
        UUID uuid = player.getUniqueId();
        player.teleportAsync(target).handle((success, error) -> {
            plugin.getPlatformScheduler().runEntity(player, () -> {
                if (!player.isOnline()) {
                    clearGuards(uuid);
                    return;
                }

                if (error == null && Boolean.TRUE.equals(success)) {
                    finishOnboarding(player);
                    return;
                }

                if (error != null) {
                    plugin.getLogger().warning(
                            "Return teleport errored for " + player.getName() + ": " + error.getMessage()
                    );
                } else {
                    plugin.getLogger().warning("Return teleport failed for " + player.getName());
                }

                if (allowSpawnFallback) {
                    plugin.resolveSafeReturnLocation(player, null, fallback -> {
                        if (fallback == null) {
                            holdForReturnRecovery(player);
                        } else {
                            teleportAndRelease(player, fallback, false);
                        }
                    });
                    return;
                }

                plugin.getLogger().warning(
                        "Spawn fallback teleport failed for " + player.getName()
                                + "; retaining onboarding state for recovery."
                );
                holdForReturnRecovery(player);
            });
            return null;
        });
    }

    private void holdForReturnRecovery(Player player) {
        UUID uuid = player.getUniqueId();
        plugin.getLogger().warning(
                "No safe return world/location could be found for "
                        + player.getName()
                        + "; retaining pending onboarding state instead of "
                        + "releasing them at the onboarding location."
        );
        plugin.markForcedCleanup(uuid);
        plugin.getPendingStore().markCleanupRequired(uuid);
        plugin.joinSequenceActive.remove(uuid);
        plugin.acceptInProgress.remove(uuid);
        plugin.canAcceptRules.put(uuid, false);
        player.setInvulnerable(true);
        player.setFallDistance(0f);
        plugin.startMovementLock(player, player.getLocation(), true);
    }

    private void finishOnboarding(Player player) {
        UUID uuid = player.getUniqueId();
        try {
            plugin.clearForcedCleanup(uuid);
            plugin.stopMovementLock(uuid);
            plugin.clearSequencePotionEffects(player);
            player.setGameMode(GameMode.SURVIVAL);
            player.setInvisible(false);
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setFallDistance(0f);
            plugin.completeOnboarding(player);
            applySoftProtection(player, 5);
        } finally {
            clearGuards(uuid);
        }
    }

    private void applySoftProtection(Player player, int seconds) {
        UUID uuid = player.getUniqueId();

        // Clear immediate hazards
        player.setFireTicks(0);

        // Temporary invulnerability
        plugin.softProtectedPlayers.add(uuid);
        player.setInvulnerable(true);

        // Optional: a bit of resistance so explosions / first hits don’t chunk them
        // (comment out if you don't want potion effects)
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, seconds * 20, 4, false, false));

        long ticks = seconds * 20L;
        plugin.getPlatformScheduler().runEntityDelayed(player, () -> {
            if (!player.isOnline()) return;
            plugin.clearSoftProtectionState(player);
        }, min1(ticks));
    }

    private static long min1(long ticks) {
        return Math.max(1L, ticks);
    }

    private boolean isActive(Player player) {
        return player.isOnline()
                && plugin.getConfigData().isTitleSequenceEnabled()
                && plugin.joinSequenceActive.contains(player.getUniqueId());
    }

    private void clearGuards(UUID uuid) {
        plugin.canAcceptRules.remove(uuid);
        plugin.acceptInProgress.remove(uuid);
        plugin.joinSequenceActive.remove(uuid);
        plugin.debugForcedPlayers.remove(uuid);
    }


}
