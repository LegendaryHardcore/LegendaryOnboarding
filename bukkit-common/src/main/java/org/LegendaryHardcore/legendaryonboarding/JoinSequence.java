package org.LegendaryHardcore.legendaryonboarding;

import org.LegendaryHardcore.legendaryonboarding.ConfigData.TitleContent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Material;
import org.bukkit.block.Block;

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

            var pending = plugin.getPendingStore().getPendingLocation(uuid);

            if (pending == null || pending.getWorld() == null) {
                pending = player.getWorld().getSpawnLocation();
            }

            // Stop pinning the player before teleporting away from the onboarding location.
            plugin.stopMovementLock(uuid);
            Location scanOrigin = pending;
            plugin.getPlatformScheduler().runAtLocation(scanOrigin, () -> {
                Location safe;
                try {
                    safe = makePhysicallySafe(scanOrigin);
                } catch (RuntimeException exception) {
                    plugin.getLogger().warning(
                            "Could not scan the return location for " + player.getName()
                                    + ": " + exception.getMessage()
                    );
                    safe = null;
                }

                Location finalSafe = safe;
                plugin.getPlatformScheduler().runEntity(player, () -> {
                    if (!player.isOnline()) {
                        clearGuards(uuid);
                        return;
                    }

                    Location target = finalSafe;
                    if (target == null) {
                        target = player.getWorld().getSpawnLocation().add(0.5, 0, 0.5);
                    }
                    teleportAndRelease(player, target, true);
                });
            });
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
                    Location spawn = player.getWorld().getSpawnLocation().add(0.5, 0, 0.5);
                    teleportAndRelease(player, spawn, false);
                    return;
                }

                plugin.getLogger().warning(
                        "Spawn fallback teleport failed for " + player.getName()
                                + "; releasing the player at their current location."
                );
                finishOnboarding(player);
            });
            return null;
        });
    }

    private void finishOnboarding(Player player) {
        UUID uuid = player.getUniqueId();
        try {
            plugin.stopMovementLock(uuid);
            plugin.clearSequencePotionEffects(player);
            player.setGameMode(GameMode.SURVIVAL);
            player.setInvisible(false);
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setFallDistance(0f);
            plugin.getPendingStore().clearPending(uuid);
            plugin.getAcceptedStore().markAccepted(uuid, player.getName());
            applySoftProtection(player, 5);
        } finally {
            clearGuards(uuid);
        }
    }

    private Location makePhysicallySafe(Location base) {
        if (base == null) return null;
        World w = base.getWorld();
        if (w == null) return null;

        // Center on block
        Location centered = base.clone();
        centered.setX(centered.getBlockX() + 0.5);
        centered.setZ(centered.getBlockZ() + 0.5);

        if (isPhysicallySafe(centered)) return centered;

        // Search nearby within a modest radius
        return findNearbySafe(centered, 6, 4);
    }

    private boolean isPhysicallySafe(Location loc) {
        World w = loc.getWorld();
        if (w == null) return false;

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        // Avoid void / ceiling extremes
        if (y <= w.getMinHeight() + 1) return false;
        if (y >= w.getMaxHeight() - 2) return false;

        Block feet = w.getBlockAt(x, y, z);
        Block head = w.getBlockAt(x, y + 1, z);
        Block below = w.getBlockAt(x, y - 1, z);

        // Must have space
        if (!feet.isPassable()) return false;
        if (!head.isPassable()) return false;

        // Must have ground
        if (!below.getType().isSolid()) return false;

        // Avoid standing in/over obvious hazards
        Material feetType = feet.getType();
        Material belowType = below.getType();

        if (feetType == Material.LAVA || feetType == Material.FIRE || feetType == Material.SOUL_FIRE) return false;
        if (belowType == Material.LAVA
                || belowType == Material.MAGMA_BLOCK
                || belowType == Material.CAMPFIRE
                || belowType == Material.SOUL_CAMPFIRE
                || belowType == Material.CACTUS
                || belowType == Material.POWDER_SNOW) return false;

        return true;
    }

    private Location findNearbySafe(Location base, int radius, int vertical) {
        World w = base.getWorld();
        if (w == null) return null;

        int bx = base.getBlockX();
        int by = base.getBlockY();
        int bz = base.getBlockZ();

        // ring search outward for closest safe
        for (int dy = 0; dy <= vertical; dy++) {
            for (int sign : new int[]{0, 1, -1}) {
                int y = by + (dy * sign);

                for (int r = 0; r <= radius; r++) {
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (Math.abs(dx) != r && Math.abs(dz) != r) continue;

                            Location cand = new Location(
                                    w,
                                    bx + dx + 0.5,
                                    y,
                                    bz + dz + 0.5,
                                    base.getYaw(),
                                    base.getPitch()
                            );

                            if (isPhysicallySafe(cand)) return cand;
                        }
                    }
                }
            }
        }
        return null;
    }


    private void applySoftProtection(Player player, int seconds) {
        // Clear immediate hazards
        player.setFireTicks(0);

        // Temporary invulnerability
        player.setInvulnerable(true);

        // Optional: a bit of resistance so explosions / first hits don’t chunk them
        // (comment out if you don't want potion effects)
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, seconds * 20, 4, false, false));

        long ticks = seconds * 20L;
        plugin.getPlatformScheduler().runEntityDelayed(player, () -> {
            if (!player.isOnline()) return;
            player.setInvulnerable(false);
            // Let potion expire naturally, or clear it explicitly:
            // player.removePotionEffect(PotionEffectType.RESISTANCE);
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
