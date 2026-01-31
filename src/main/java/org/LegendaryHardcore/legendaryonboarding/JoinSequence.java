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
        }, null,1L);

        // Join Sequence title timing
        List<TitleContent> contents = plugin.getConfigData().getJoinSequenceContent();
        final int duration = plugin.getConfigData().getJoinSequenceDuration() * TPS;
        final int fadeIn = plugin.getConfigData().getJoinSequenceFadeIn() * TPS;
        final int fadeOut = plugin.getConfigData().getJoinSequenceFadeOut() * TPS;

        final int perMessageDelay = duration + fadeIn + fadeOut;
        long currentDelay = 0L;

        // For each join sequence message, schedule each title/subtitles
        for (TitleContent content : contents) {
            final TitleContent c = content;
            final long scheduledDelay = currentDelay;

            player.getScheduler().runDelayed(plugin, task -> {
                if (!player.isOnline()) return;
                player.sendTitle(c.title(), c.subtitle(), fadeIn, duration, fadeOut);
            }, null, scheduledDelay);

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

            restoreToPendingLocation(player);
        }, null, finishDelay);
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
}