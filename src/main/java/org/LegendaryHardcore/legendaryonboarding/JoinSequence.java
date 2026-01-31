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
        final UUID uuid = player.getUniqueId();

        // Reset so player can no longer run /accept
        plugin.canAcceptRules.put(uuid, false);

        // Give the player blindness and other stasis effects
        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline()) return;
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.BLINDNESS,
                    Integer.MAX_VALUE,
                    1,
                    false,
                    false
            ));
        }, null, 1L);

        // Join Sequence title timing
        List<TitleContent> contents = plugin.getConfigData().getJoinSequenceContent();
        final int duration = plugin.getConfigData().getJoinSequenceDuration() * TPS;
        final int fadeIn = plugin.getConfigData().getJoinSequenceFadeIn() * TPS;
        final int fadeOut = plugin.getConfigData().getJoinSequenceFadeOut() * TPS;

        final int perMessageDelay = duration + fadeIn + fadeOut;
        long currentDelay = 1L;

        // For each join sequence message, schedule each title/subtitles
        for (TitleContent content : contents) {
            final TitleContent c = content;
            final long scheduledDelay = currentDelay;

            player.getScheduler().runDelayed(plugin, task -> {
                if (!player.isOnline()) return;
                player.sendTitle(c.title(), c.subtitle(), fadeIn, duration, fadeOut);
            }, null, min1(scheduledDelay));

            currentDelay += perMessageDelay;
        }

        // Final step after titles
        final long finishDelay = currentDelay;

        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline()) return;

            // Clear effects and restore player state
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.setGravity(true);
            player.setGameMode(GameMode.SURVIVAL);
            player.setInvisible(false);
            player.setInvulnerable(false);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setFallDistance(0f);

            returnToPendingSoftSafe(player);
        }, null, min1(finishDelay));
    }


    /**
     * Check the original location the player was at and make sure it's safe/loaded
     * If the world isn't loaded, or the location is missing, fix it
     * If the location isn't safe, adjust
     * Then teleport
     */
    private void restoreToPendingLocation(Player player) {
        final UUID uuid = player.getUniqueId();

        Location pendingLoc = plugin.getPendingStore().getPendingLocation(uuid);
        if (pendingLoc == null) {
            plugin.getLogger().warning("No pending location found for " + player.getName() + " (" + uuid + ")");
            return;
        }

        // Teleport async for Folia safety
        player.teleportAsync(pendingLoc).thenAccept(success -> {
            if (!success) {
                plugin.getLogger().warning("Teleport back to pending location failed for " + player.getName());
                return;
            }
            // Clear pending once we successfully returned them
            plugin.getPendingStore().clearPending(uuid);
        });
    }

    private void returnToPendingSoftSafe(Player player) {
        final UUID uuid = player.getUniqueId();

        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) return;

            // Read pending location (safe to read memory map; world access below is on scheduler)
            var pending = plugin.getPendingStore().getPendingLocation(uuid);

            // Fallback: if no pending, use spawn in player's current world
            if (pending == null || pending.getWorld() == null) {
                pending = player.getWorld().getSpawnLocation();
            }

            Location safe = makePhysicallySafe(pending);
            if (safe == null) {
                // Last resort fallback
                safe = player.getWorld().getSpawnLocation().add(0.5, 0, 0.5);
            }

            Location finalSafe = safe;
            player.teleportAsync(finalSafe).thenAccept(success -> {
                // IMPORTANT: teleportAsync callback might not be on the player thread,
                // so schedule post-teleport actions back onto the player scheduler.
                player.getScheduler().run(plugin, t2 -> {
                    if (!player.isOnline()) return;

                    if (success) {
                        applySoftProtection(player, 5); // seconds
                        plugin.getPendingStore().clearPending(uuid);

                        // Set Accepted (persistent)
                        plugin.getAcceptedStore().setAccepted(uuid, true);
                    } else {
                        plugin.getLogger().warning("Teleport back to pending failed for " + player.getName());
                        // Keep pending so we can retry later
                    }
                }, null);
            });
        }, null);
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
        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline()) return;
            player.setInvulnerable(false);
            // Let potion expire naturally, or clear it explicitly:
            // player.removePotionEffect(PotionEffectType.RESISTANCE);
        }, null, min1(ticks));
    }

    private static long min1(long ticks) {
        return Math.max(1L, ticks);
    }

}