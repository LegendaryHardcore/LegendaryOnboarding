package org.LegendaryHardcore.legendaryonboarding.listener;

import net.kyori.adventure.text.Component;
import org.LegendaryHardcore.legendaryonboarding.ConfigData;
import org.LegendaryHardcore.legendaryonboarding.ConfigData.MessageConsumption;
import org.LegendaryHardcore.legendaryonboarding.DiscordSrvBridge;
import org.LegendaryHardcore.legendaryonboarding.DiscordSrvMessagesConfig;
import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

public class PlayerJoin implements Listener {
    private final LegendaryOnboarding plugin;

    public PlayerJoin(LegendaryOnboarding plugin) {

        this.plugin = plugin;
    }

    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        // Always record their latest name
        plugin.beginPlayerSession(uuid);
        plugin.getAcceptedStore().recordSeenName(uuid, player.getName());
        plugin.synchronizeTabVisibility(player);
        plugin.consumeDeferredDebugFix(player);

        boolean sequenceEnabled = plugin.getConfigData().isTitleSequenceEnabled();
        boolean hasPending = plugin.getPendingStore().hasPending(uuid);
        boolean cleanupRequired =
                hasPending && plugin.getPendingStore().isCleanupRequired(uuid);
        if (cleanupRequired) {
            player.setInvulnerable(true);
            player.setFireTicks(0);
            player.setFallDistance(0f);
            plugin.releasePendingPlayer(player);
            return;
        }

        if (!sequenceEnabled) {
            if (shouldReleasePendingPlayer(sequenceEnabled, hasPending)) {
                plugin.releasePendingPlayer(player);
            }
            return;
        }

        final boolean accepted = plugin.getAcceptedStore().isAccepted(uuid);
        final boolean forceOnboarding = plugin.getConfigData().isDebugForceOnboarding();
        final boolean onboardReturning =
                plugin.getConfigData().isOnboardUnacceptedReturningPlayers();
        final boolean debugForced = forceOnboarding && (accepted || player.hasPlayedBefore());
        consumeJoinMessageIfConfigured(
                event,
                shouldConsumeOwnJoinMessage(
                        sequenceEnabled,
                        hasPending,
                        cleanupRequired,
                        accepted,
                        forceOnboarding,
                        player.hasPlayedBefore(),
                        onboardReturning
                )
        );

        // If accepted, ensure no stale onboarding state remains, then bail
        if (accepted && !forceOnboarding) {
            if (hasPending) {
                plugin.releasePendingPlayer(player);
                return;
            }
            plugin.debugForcedPlayers.remove(uuid);
            plugin.clearSequencePotionEffects(player);
            plugin.canAcceptRules.remove(uuid);
            plugin.acceptInProgress.remove(uuid);

            var task = plugin.movementLocks.remove(uuid);
            if (task != null) task.cancel();

            return;
        }

        // If they are NOT accepted but have pending, they disconnected
        // mid-onboarding. Their current login location is authoritative: the
        // plugin may have been disabled while they played and moved elsewhere.
        if (hasPending) {
            boolean resumingDebug =
                    debugForced || plugin.getPendingStore().isDebugSession(uuid);
            boolean announceWhenComplete =
                    !resumingDebug && plugin.getPendingStore().shouldAnnounceWhenComplete(uuid);
            plugin.resumeOnboardingFromCurrentLocation(
                    player,
                    announceWhenComplete,
                    resumingDebug
            );
            return;
        }

        // Returning players are ignored unless testing explicitly opts them in.
        if (!shouldOnboardReturningPlayer(
                player.hasPlayedBefore(),
                forceOnboarding,
                onboardReturning
        )) {
            // Not accepted, no pending, returning player -> do nothing onboarding-related
            plugin.canAcceptRules.remove(uuid);
            plugin.acceptInProgress.remove(uuid);
            return;
        }

        plugin.startOnboarding(player, !forceOnboarding, debugForced);
    }

    private void consumeJoinMessageIfConfigured(
            PlayerJoinEvent event,
            boolean onboardingRelated
    ) {
        MessageConsumption inGameMode = plugin.getConfigData().getInGameJoinMessages();
        MessageConsumption discordSrvMode = plugin.getConfigData().getDiscordSrvJoinMessages();
        boolean consumeInGame = shouldConsumeMessage(inGameMode, onboardingRelated);
        boolean consumeDiscordSrv = shouldConsumeMessage(discordSrvMode, onboardingRelated);
        plugin.debugLog(ConfigData.DebugOption.LOG_JOIN_QUIT_MESSAGE_HANDLING,
                () -> "Join message handling player=" + event.getPlayer().getName()
                + " onboardingRelated=" + onboardingRelated
                + " inGameMode=" + inGameMode
                + " discordSrvMode=" + discordSrvMode
                + " consumeInGame=" + consumeInGame
                + " consumeDiscordSrv=" + consumeDiscordSrv);
        if (!consumeInGame && !consumeDiscordSrv) return;

        Component original = event.joinMessage();
        if (original == null) return;

        event.joinMessage(null);
        if (shouldRedistributeInGame(inGameMode, consumeInGame)) {
            plugin.sendToNonOnboardingPlayers(original);
        }
        if (consumeDiscordSrv && shouldRedistributeDiscordSrv(
                discordSrvMode,
                onboardingRelated
        )) {
            DiscordSrvBridge.forward(
                    plugin,
                    DiscordSrvMessagesConfig.Type.JOIN,
                    event.getPlayer(),
                    original
            );
        }
    }

    static boolean shouldReleasePendingPlayer(boolean sequenceEnabled, boolean hasPending) {
        return !sequenceEnabled && hasPending;
    }

    static boolean shouldConsumeOwnJoinMessage(
            boolean sequenceEnabled,
            boolean hasPending,
            boolean cleanupRequired,
            boolean accepted,
            boolean forceOnboarding,
            boolean hasPlayedBefore,
            boolean onboardUnacceptedReturningPlayers
    ) {
        if (cleanupRequired || hasPending) {
            return true;
        }
        if (!sequenceEnabled) {
            return false;
        }
        if (accepted && !forceOnboarding) {
            return false;
        }
        return shouldOnboardReturningPlayer(
                hasPlayedBefore,
                forceOnboarding,
                onboardUnacceptedReturningPlayers
        );
    }

    static boolean shouldConsumeMessage(MessageConsumption mode, boolean onboardingActive) {
        return switch (mode) {
            case NONE -> false;
            case SOME -> onboardingActive;
            case ALL -> true;
        };
    }

    /**
     * The Bukkit join event is shared by in-game delivery and DiscordSRV. When
     * DiscordSRV takes ownership on its own, replay the message in game unless
     * this player's in-game mode explicitly suppresses it.
     */
    static boolean shouldRedistributeInGame(
            MessageConsumption mode,
            boolean consumeInGame
    ) {
        return !consumeInGame || mode == MessageConsumption.ALL;
    }

    static boolean shouldRedistributeDiscordSrv(
            MessageConsumption mode,
            boolean onboardingRelated
    ) {
        return mode == MessageConsumption.ALL && !onboardingRelated;
    }

    static boolean shouldOnboardReturningPlayer(
            boolean hasPlayedBefore,
            boolean forceOnboarding,
            boolean onboardUnacceptedReturningPlayers
    ) {
        return !hasPlayedBefore || forceOnboarding || onboardUnacceptedReturningPlayers;
    }

}
