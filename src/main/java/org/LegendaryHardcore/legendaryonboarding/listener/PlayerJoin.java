package org.LegendaryHardcore.legendaryonboarding.listener;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.group.Group;

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

        plugin.canAcceptRules.put(player.getUniqueId(), false);

        // Load LuckPerms async
        plugin.getLuckPermsAPI().getUserManager().loadUser(uuid).thenAccept(user -> {
            if (user == null) return;


            Collection<Group> groups = user.getInheritedGroups(user.getQueryOptions());
            boolean isPre = groups.stream().anyMatch(g -> g.getName().equalsIgnoreCase(preGroup));
            if (!isPre) return;

            // Player might quite while LP was loading
            if (!player.isOnline()) return;

            //Run on the player's scheduler to keep folia safe
            player.getScheduler().run(plugin, scheduledTask -> {
                if (!player.isOnline()) return;
                plugin.getRulesSequence().start(player);
            } null);
        });
    }
}