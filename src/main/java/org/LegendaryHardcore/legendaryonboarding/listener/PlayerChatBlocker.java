package org.LegendaryHardcore.legendaryonboarding.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class PlayerChatBlocker implements Listener {
    private final LegendaryOnboarding plugin;

    public PlayerChatBlocker(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Accepted players can chat.
        if (plugin.getAcceptedStore().isAccepted(uuid)) return;

        // Only block if they are actively onboarding (or resuming onboarding).
        boolean onboardingActive =
                plugin.canAcceptRules.containsKey(uuid) || plugin.getPendingStore().hasPending(uuid);

        if (!onboardingActive) return;

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "You must accept the rules before chatting.");
    }
}
