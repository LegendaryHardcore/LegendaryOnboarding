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
        if (!plugin.getConfigData().isTitleSequenceEnabled()) return;

        UUID uuid = event.getPlayer().getUniqueId();

        // Already accepted? No restrictions.
        if (plugin.getAcceptedStore().isAccepted(uuid)) return;

        // Only restrict if they're actively onboarding (or resuming via pending).
        boolean onboardingActive =
                plugin.canAcceptRules.containsKey(uuid) || plugin.getPendingStore().hasPending(uuid);

        if (!onboardingActive) return;

        // Normalize command
        String message = event.getMessage().toLowerCase().trim();
        if (!message.startsWith("/")) return;
        if (isOnboardingHelpCommand(message)) return;

        String command = message.substring(1).split(" ")[0];

        // Strip namespace if present (minecraft:help → help)
        if (command.contains(":")) {
            command = command.substring(command.indexOf(':') + 1);
        }

        // Always allow accept (failsafe)
        if (command.equals("accept")) return;

        List<String> whitelist = plugin.getConfigData().getCommandWhitelist();

        if (whitelist.contains(command)) {
            return; // allowed
        }

        // Block everything else
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cYou must accept the rules before using commands.");
    }

    static boolean isOnboardingHelpCommand(String message) {
        if (message == null) return false;
        String normalized = message.toLowerCase().trim();
        if (!normalized.startsWith("/")) return false;

        String[] parts = normalized.substring(1).split("\\s+");
        String root = parts[0];
        if (root.contains(":")) {
            root = root.substring(root.indexOf(':') + 1);
        }
        if (!root.equals("lo") && !root.equals("legendaryonboarding")) {
            return false;
        }
        return parts.length == 1 || parts[1].equals("help");
    }
}
