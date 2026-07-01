package org.LegendaryHardcore.legendaryonboarding;

import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.event.EventPriority;
import java.util.List;

/*
 *  Class for data loaded from config
 */
public final class ConfigData {
    private final boolean titleSequenceEnabled;
    private final String serverName;
    private final String onboardGamemode;
    private final PlayerLocation onboardLocation;
    private final String actionBarCountdown;
    private final List<TitleContent> welcomeSequenceContent;
    private final List<TitleContent> rulesSequenceContent;
    private final List<TitleContent> promptAccept;
    private final List<TitleContent> joinSequenceContent;
    private final boolean onboardTeleport;
    private final boolean debugForceOnboarding;
    private final boolean blockAdvancements;
    private final boolean onboardUnacceptedReturningPlayers;
    private final boolean blockExternalMessages;
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
            List<TitleContent> welcomeSequenceContent,
            List<TitleContent> rulesSequenceContent,
            List<TitleContent> promptAccept,
            List<TitleContent> joinSequenceContent,
            List<String> commandWhitelist,
            boolean onboardTeleport,
            boolean debugForceOnboarding,
            boolean blockAdvancements,
            boolean onboardUnacceptedReturningPlayers,
            boolean blockExternalMessages,
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
        this.welcomeSequenceContent = welcomeSequenceContent;
        this.rulesSequenceContent = rulesSequenceContent;
        this.promptAccept = promptAccept;
        this.joinSequenceContent = joinSequenceContent;
        this.commandWhitelist = commandWhitelist;
        this.onboardTeleport = onboardTeleport;
        this.debugForceOnboarding = debugForceOnboarding;
        this.blockAdvancements = blockAdvancements;
        this.onboardUnacceptedReturningPlayers = onboardUnacceptedReturningPlayers;
        this.blockExternalMessages = blockExternalMessages;
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

    public boolean isDebugForceOnboarding() { return debugForceOnboarding; }

    public boolean isBlockAdvancements() { return blockAdvancements; }

    public boolean isOnboardUnacceptedReturningPlayers() {
        return onboardUnacceptedReturningPlayers;
    }

    public boolean isBlockExternalMessages() { return blockExternalMessages; }

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
