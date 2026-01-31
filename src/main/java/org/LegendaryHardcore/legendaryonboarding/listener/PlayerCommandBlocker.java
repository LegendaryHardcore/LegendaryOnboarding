package org.LegendaryHardcore.legendaryonboarding.listener;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class PlayerCommandBlocker implements Listener {
    private final LegendaryOnboarding plugin;

    // Allowlist: keep it small and explicit
    private static final Set<String> ALLOW = Set.of(
            "/accept",
            "/rules",
            "/help"
    );

    public PlayerCommandBlocker(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        if (plugin.getAcceptedStore().isAccepted(uuid)) return;

        String msg = event.getMessage().trim();
        String base = msg.split("\\s+")[0].toLowerCase(Locale.ROOT);

        if (ALLOW.contains(base)) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage("You must accept the rules first. Type /accept");
    }
}