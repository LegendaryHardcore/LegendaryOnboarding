package org.LegendaryHardcore.legendaryonboarding;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.LegendaryHardcore.legendaryonboarding.ConfigData.TitleContent;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;

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

        // Give player blindness
        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline()) return;
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
        }, 1L);

        /* Join Sequence */
        List<TitleContent> contents = plugin.getConfigData().getJoinSequenceContent();
        final int duration = plugin.getConfigData().getJoinSequenceDuration() * TPS;
        final int fadeIn = plugin.getConfigData().getJoinSequenceFadeIn() * TPS;
        final int fadeOut = plugin.getConfigData().getJoinSequenceFadeOut() * TPS;

        final int perMessageDelay = duration + fadeIn + fadeOut;
        long currentDelay = 0L;

        // For each join sequence message, display title and subtitle
        for (TitleContent content : contents) {
            final TitleContent c = content;
            final long scheduleDelay = currentDelay;

            player.getScheduler().runDelayed(plugin,task -> {
                if (!player.isOnline()) return;
                player.sendTitle(c.title(), c.subtitle(), fadeIn, fadeOut, scheduleDelay);

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

                teleportToRandomLocationAndFinalize(player);
            }, finishDelay);
        }

            )

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                String title = content.title();
                String subtitle = content.subtitle();
                player.sendTitle(title, subtitle, fadeIn, duration, fadeOut);
            }, currentDelay);
            currentDelay += perMessageDelay;
        }
    }

    /*
    Check the original location the player was at, make sure it's safe to teleport back, and after fixes, teleport
     */

    /*
     * Asynchronously move player to post-onboard group
     */
    private void updateLuckPermsGroup(Player player) {
        String preOnboardGroup = plugin.getConfigData().getPreOnboardGroup();
        String postOnboardGroup = plugin.getConfigData().getPostOnboardGroup();

        LuckPerms lp = plugin.getLuckPermsAPI();

        lp.getUserManager().loadUser(player.getUniqueId()).thenAcceptAsync(user -> {

            // Remove from pre-onboard group
            user.data().clear(NodeType.INHERITANCE.predicate(node ->
                    node.getGroupName().equalsIgnoreCase(preOnboardGroup)
            ));

            // Add to post-onboard group
            user.data().add(InheritanceNode.builder(postOnboardGroup).build());

            lp.getUserManager().saveUser(user).join();

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.recalculatePermissions();
            });
        });
    }
}