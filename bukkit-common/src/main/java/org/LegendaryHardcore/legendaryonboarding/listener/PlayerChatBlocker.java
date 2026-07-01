package org.LegendaryHardcore.legendaryonboarding.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class PlayerChatBlocker implements Listener {
    private final LegendaryOnboarding plugin;

    public PlayerChatBlocker(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    public void onChat(AsyncChatEvent event) {
        if (plugin.getConfigData().isBlockExternalMessages()) {
            event.viewers().removeIf(audience ->
                    audience instanceof Player viewer
                            && plugin.isOnboardingActive(viewer.getUniqueId())
            );
        }

        Player player = event.getPlayer();
        if (!shouldBlockSender(plugin.isOnboardingActive(player.getUniqueId()))) return;

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "You must accept the rules before chatting.");
    }

    static boolean shouldBlockSender(boolean onboardingActive) {
        return onboardingActive;
    }
}
