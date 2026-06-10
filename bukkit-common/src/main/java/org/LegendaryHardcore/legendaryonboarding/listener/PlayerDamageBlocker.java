package org.LegendaryHardcore.legendaryonboarding.listener;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public final class PlayerDamageBlocker implements Listener {
    private final LegendaryOnboarding plugin;

    public PlayerDamageBlocker(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (plugin.isOnboardingActive(player.getUniqueId())) {
            event.setCancelled(true);
            player.setFireTicks(0);
            player.setFallDistance(0f);
        }
    }
}
