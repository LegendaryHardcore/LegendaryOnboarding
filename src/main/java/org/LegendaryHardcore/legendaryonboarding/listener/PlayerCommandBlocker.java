package org.LegendaryHardcore.legendaryonboarding.listener;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.UUID;
import java.util.List;

public class PlayerCommandBlocker implements Listener {
    private final LegendaryOnboarding plugin;

    public PlayerCommandBlocker(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Already accepted? No restrictions.
        if (plugin.getAcceptedStore().isAccepted(uuid)) return;

        // Normalize command
        String message = event.getMessage().toLowerCase().trim();
        if (!message.startsWith("/")) return;

        String command = message.substring(1).split(" ")[0];

        // Strip namespace if present (minecraft:help → help)
        if (command.contains(":")) {
            command = command.substring(command.indexOf(':') + 1);
        }

        List<String> whitelist = plugin.getConfigData().getCommandWhitelist();

        if (whitelist.contains(command)) {
            return; // allowed
        }

        // Block everything else
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cYou must accept the rules before using commands.");
    }
}