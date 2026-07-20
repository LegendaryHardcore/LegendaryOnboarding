package org.LegendaryHardcore.legendaryonboarding;

import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.event.EventPriority;
import java.util.Set;
import java.util.List;

/*
 *  Class for data loaded from config
 */
public final class ConfigData {
    public enum MessageConsumption {
        NONE,
        SOME,
        ALL
    }

    /** Named diagnostics enabled by entries in config.yml's DEBUG list. */
    public enum DebugOption {
        FORCE_ONBOARDING,
        LOG_START,
        LOG_END,
        LOG_SEQUENCE_START,
        LOG_RETURN_LOCATION_CAPTURE,
        LOG_RETURN_LOCATION_REUSE,
        LOG_RETURN_LOCATION_RESOLUTION,
        LOG_DEPARTURE_CLEANUP,
        LOG_JOIN_QUIT_MESSAGE_HANDLING,
        LOG_DISCORDSRV_AVATAR,
        LOG_ACTION_BAR_LIFECYCLE,
        LOG_DEBUG_FIX_CLEANUP
    }

    private final boolean titleSequenceEnabled;
    private final String serverName;
    private final String onboardGamemode;
    private final PlayerLocation onboardLocation;
    private final String actionBarCountdown;
    private final int actionBarRefreshTicks;
    private final List<TitleContent> welcomeSequenceContent;
    private final List<TitleContent> rulesSequenceContent;
    private final List<TitleContent> promptAccept;
    private final List<TitleContent> joinSequenceContent;
    private final boolean onboardTeleport;
    private final Set<DebugOption> debugOptions;
    private final boolean blockAdvancements;
    private final boolean onboardUnacceptedReturningPlayers;
    private final boolean blockExternalMessages;
    private final MessageConsumption inGameJoinMessages;
    private final MessageConsumption inGameQuitMessages;
    private final MessageConsumption discordSrvJoinMessages;
    private final MessageConsumption discordSrvQuitMessages;
    private final boolean hideOnboardingPlayersFromTab;
    private final PlayerLocation cleanupFallbackLocation;
    private final World.Environment cleanupFallbackEnvironment;
    private final int cleanupFallbackRadius;
    private final int returnDesiredY;
    private final String onboardingDamageMessage;
    private final String firstJoinMessage;
    private final List<String> commandWhitelist;
    private final EventPriorities eventPriorities;

    // One independently timed sequence step. Empty title/subtitle/chat values are ignored.
    public record TitleContent(
            String title,
            String subtitle,
            String chat,
            int fadeIn,
            int duration,
            int fadeOut,
            boolean stopAllSounds,
            SoundEffect sound,
            List<PotionEffectConfig> potionEffects,
            List<String> removePotionEffects
    ) {}

    public record SoundEffect(String name, SoundCategory category, float volume, float pitch) {}

    public record PotionEffectConfig(
            String type,
            int duration,
            int amplifier,
            boolean ambient,
            boolean particles,
            boolean icon
    ) {}

    public record EventPriorities(
            EventPriority chatConsumption,
            EventPriority joinMessage,
            EventPriority quitMessage
    ) {}

    // Location
    public record PlayerLocation(
            String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            World.Environment fallbackEnvironment
    ) {
        public Location toLocation() {
            World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world == null && fallbackEnvironment != null) {
                world = org.bukkit.Bukkit.getWorlds().stream()
                        .filter(candidate ->
                                candidate.getEnvironment() == fallbackEnvironment)
                        .findFirst()
                        .orElse(null);
            }
            if (world == null) return null;
            return new Location(world, x, y, z, yaw, pitch);
        }
    }

    public ConfigData(
            boolean titleSequenceEnabled,
            String serverName,
            String onboardGamemode,
            PlayerLocation onboardLocation,
            String actionBarCountdown,
            int actionBarRefreshTicks,
            List<TitleContent> welcomeSequenceContent,
            List<TitleContent> rulesSequenceContent,
            List<TitleContent> promptAccept,
            List<TitleContent> joinSequenceContent,
            List<String> commandWhitelist,
            boolean onboardTeleport,
            Set<DebugOption> debugOptions,
            boolean blockAdvancements,
            boolean onboardUnacceptedReturningPlayers,
            boolean blockExternalMessages,
            MessageConsumption inGameJoinMessages,
            MessageConsumption inGameQuitMessages,
            MessageConsumption discordSrvJoinMessages,
            MessageConsumption discordSrvQuitMessages,
            boolean hideOnboardingPlayersFromTab,
            PlayerLocation cleanupFallbackLocation,
            World.Environment cleanupFallbackEnvironment,
            int cleanupFallbackRadius,
            int returnDesiredY,
            String onboardingDamageMessage,
            String firstJoinMessage,
            EventPriorities eventPriorities) {
        this.titleSequenceEnabled = titleSequenceEnabled;
        this.serverName = serverName;
        this.onboardGamemode = onboardGamemode;
        this.onboardLocation = onboardLocation;
        this.actionBarCountdown = actionBarCountdown;
        this.actionBarRefreshTicks = actionBarRefreshTicks;
        this.welcomeSequenceContent = welcomeSequenceContent;
        this.rulesSequenceContent = rulesSequenceContent;
        this.promptAccept = promptAccept;
        this.joinSequenceContent = joinSequenceContent;
        this.commandWhitelist = commandWhitelist;
        this.onboardTeleport = onboardTeleport;
        this.debugOptions = Set.copyOf(debugOptions);
        this.blockAdvancements = blockAdvancements;
        this.onboardUnacceptedReturningPlayers = onboardUnacceptedReturningPlayers;
        this.blockExternalMessages = blockExternalMessages;
        this.inGameJoinMessages = inGameJoinMessages;
        this.inGameQuitMessages = inGameQuitMessages;
        this.discordSrvJoinMessages = discordSrvJoinMessages;
        this.discordSrvQuitMessages = discordSrvQuitMessages;
        this.hideOnboardingPlayersFromTab = hideOnboardingPlayersFromTab;
        this.cleanupFallbackLocation = cleanupFallbackLocation;
        this.cleanupFallbackEnvironment = cleanupFallbackEnvironment;
        this.cleanupFallbackRadius = cleanupFallbackRadius;
        this.returnDesiredY = returnDesiredY;
        this.onboardingDamageMessage = onboardingDamageMessage;
        this.firstJoinMessage = firstJoinMessage;
        this.eventPriorities = eventPriorities;
    }

    public boolean isTitleSequenceEnabled() {
        return titleSequenceEnabled;
    }

    public String getServerName() {
        return serverName;
    }

    public String getOnboardGamemode() {
        return onboardGamemode;
    }

    public PlayerLocation getOnboardLocation() {
        return onboardLocation;
    }

    public String getActionBarCountdown() {
        return actionBarCountdown;
    }

    public int getActionBarRefreshTicks() {
        return actionBarRefreshTicks;
    }

    public List<TitleContent> getWelcomeSequenceContent() {
        return welcomeSequenceContent;
    }

    public List<TitleContent> getRulesSequenceContent() {
        return rulesSequenceContent;
    }

    public List<TitleContent> getPromptAccept() {
        return promptAccept;
    }

    public List<TitleContent> getJoinSequenceContent() {
        return joinSequenceContent;
    }

    public boolean isOnboardTeleport() { return onboardTeleport; }

    public boolean isDebugForceOnboarding() {
        return debugOptions.contains(DebugOption.FORCE_ONBOARDING);
    }

    public boolean isDebugLogging() {
        return debugOptions.stream().anyMatch(option -> option != DebugOption.FORCE_ONBOARDING);
    }

    public boolean isDebugEnabled(DebugOption option) {
        return debugOptions.contains(option);
    }

    public boolean isBlockAdvancements() { return blockAdvancements; }

    public boolean isOnboardUnacceptedReturningPlayers() {
        return onboardUnacceptedReturningPlayers;
    }

    public boolean isBlockExternalMessages() { return blockExternalMessages; }

    public MessageConsumption getInGameJoinMessages() {
        return inGameJoinMessages;
    }

    public MessageConsumption getInGameQuitMessages() {
        return inGameQuitMessages;
    }

    public MessageConsumption getDiscordSrvJoinMessages() {
        return discordSrvJoinMessages;
    }

    public MessageConsumption getDiscordSrvQuitMessages() {
        return discordSrvQuitMessages;
    }

    public boolean isHideOnboardingPlayersFromTab() {
        return hideOnboardingPlayersFromTab;
    }

    public PlayerLocation getCleanupFallbackLocation() {
        return cleanupFallbackLocation;
    }

    public World.Environment getCleanupFallbackEnvironment() {
        return cleanupFallbackEnvironment;
    }

    public int getCleanupFallbackRadius() {
        return cleanupFallbackRadius;
    }

    public int getReturnDesiredY() {
        return returnDesiredY;
    }

    public String getOnboardingDamageMessage() { return onboardingDamageMessage; }

    public String getFirstJoinMessage() { return firstJoinMessage; }

    public List<String> getCommandWhitelist() { return commandWhitelist; }

    public EventPriorities getEventPriorities() { return eventPriorities; }
}
