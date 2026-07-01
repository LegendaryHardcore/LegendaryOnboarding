package org.LegendaryHardcore.legendaryonboarding.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.LegendaryHardcore.legendaryonboarding.ConfigData;
import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;

public final class MessageResponsibilityManager {
    private final LegendaryOnboarding plugin;
    private final PlayerChatBlocker chatBlocker;
    private final PlayerJoin playerJoin;
    private final PlayerQuit playerQuit;
    private boolean active;

    public MessageResponsibilityManager(LegendaryOnboarding plugin) {
        this.plugin = plugin;
        this.chatBlocker = new PlayerChatBlocker(plugin);
        this.playerJoin = new PlayerJoin(plugin);
        this.playerQuit = new PlayerQuit(plugin);
    }

    public void activate(ConfigData.EventPriorities priorities) {
        deactivate();

        PluginManager manager = plugin.getServer().getPluginManager();
        manager.registerEvent(
                AsyncChatEvent.class,
                chatBlocker,
                priorities.chatConsumption(),
                (listener, event) -> chatBlocker.onChat((AsyncChatEvent) event),
                plugin,
                false
        );
        manager.registerEvent(
                PlayerJoinEvent.class,
                playerJoin,
                priorities.joinMessage(),
                (listener, event) -> playerJoin.onPlayerJoin((PlayerJoinEvent) event),
                plugin
        );
        manager.registerEvents(playerQuit, plugin);
        manager.registerEvent(
                PlayerQuitEvent.class,
                playerQuit,
                priorities.quitMessage(),
                (listener, event) ->
                        playerQuit.suppressOnboardingQuitMessage((PlayerQuitEvent) event),
                plugin
        );
        active = true;
    }

    public void deactivate() {
        if (!active) return;
        HandlerList.unregisterAll(chatBlocker);
        HandlerList.unregisterAll(playerJoin);
        HandlerList.unregisterAll(playerQuit);
        active = false;
    }

    boolean isActive() {
        return active;
    }
}
