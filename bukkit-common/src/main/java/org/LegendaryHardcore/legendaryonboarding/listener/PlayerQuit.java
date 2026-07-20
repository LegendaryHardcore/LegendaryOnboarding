package org.LegendaryHardcore.legendaryonboarding.listener;

import net.kyori.adventure.text.Component;
import org.LegendaryHardcore.legendaryonboarding.ConfigData;
import org.LegendaryHardcore.legendaryonboarding.ConfigData.MessageConsumption;
import org.LegendaryHardcore.legendaryonboarding.DiscordSrvBridge;
import org.LegendaryHardcore.legendaryonboarding.DiscordSrvMessagesConfig;
import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuit implements Listener {
    private final LegendaryOnboarding plugin;

    public PlayerQuit(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    public void suppressOnboardingQuitMessage(PlayerQuitEvent event) {
        MessageConsumption inGameMode = plugin.getConfigData().getInGameQuitMessages();
        MessageConsumption discordSrvMode = plugin.getConfigData().getDiscordSrvQuitMessages();
        boolean onboardingActive = plugin.isOnboardingActive(event.getPlayer().getUniqueId());
        boolean consumeInGame = PlayerJoin.shouldConsumeMessage(inGameMode, onboardingActive);
        boolean consumeDiscordSrv = PlayerJoin.shouldConsumeMessage(discordSrvMode, onboardingActive);
        plugin.debugLog(ConfigData.DebugOption.LOG_JOIN_QUIT_MESSAGE_HANDLING,
                () -> "Quit message handling player=" + event.getPlayer().getName()
                + " onboardingActive=" + onboardingActive
                + " inGameMode=" + inGameMode
                + " discordSrvMode=" + discordSrvMode
                + " consumeInGame=" + consumeInGame
                + " consumeDiscordSrv=" + consumeDiscordSrv);
        if (!consumeInGame && !consumeDiscordSrv) return;

        Component original = event.quitMessage();
        if (original == null) return;

        event.quitMessage(null);
        if (PlayerJoin.shouldRedistributeInGame(inGameMode, consumeInGame)) {
            plugin.sendToNonOnboardingPlayers(original);
        }
        if (consumeDiscordSrv && PlayerJoin.shouldRedistributeDiscordSrv(
                discordSrvMode,
                onboardingActive
        )) {
            DiscordSrvBridge.forward(
                    plugin,
                    DiscordSrvMessagesConfig.Type.QUIT,
                    event.getPlayer(),
                    original
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        plugin.deferPlayerDepartureCleanup(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.deferPlayerDepartureCleanup(event.getPlayer());
        plugin.scheduleDepartureSnapshotRestore(event.getPlayer().getUniqueId());
    }

    static boolean shouldSuppressQuitMessage(boolean onboardingActive) {
        return PlayerJoin.shouldConsumeMessage(MessageConsumption.SOME, onboardingActive);
    }
}
