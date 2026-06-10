package org.LegendaryHardcore.legendaryonboarding.listener;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class PlayerAdvancementBlocker implements Listener {
    private final LegendaryOnboarding plugin;

    public PlayerAdvancementBlocker(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCriterionGrant(PlayerAdvancementCriterionGrantEvent event) {
        if (plugin.getConfigData().isBlockAdvancements()
                && plugin.isOnboardingActive(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
