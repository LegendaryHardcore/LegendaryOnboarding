package org.LegendaryHardcore.legendaryonboarding.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
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
        UUID uuid = event.getPlayer().getUniqueId();

        // If they've accepted, let them chat
        if (plugin.getAcceptedStore().isAccepted(uuid)) return;

        // Otherwise block chat
        event.setCancelled(true);
        event.getPlayer().sendMessage(Component.text("You must accept the rules first. Type /accept"));
    }
}