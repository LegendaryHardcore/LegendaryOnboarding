package org.LegendaryHardcore.legendaryonboarding;

import org.bukkit.Location;
import org.bukkit.World;
import java.util.List;

/*
 *  Class for data loaded from config
 */
public final class ConfigData {
    private final String serverName;
    private final String onboardGamemode;
    private final PlayerLocation onboardLocation;
    private final List<TitleContent> rulesSequenceContent;
    private final int rulesSequenceDuration;
    private final int rulesSequenceFadeIn;
    private final int rulesSequenceFadeOut;
    private final List<TitleContent> promptAccept;
    private final String promptChat;
    private final List<TitleContent> joinSequenceContent;
    private final int joinSequenceDuration;
    private final int joinSequenceFadeIn;
    private final int joinSequenceFadeOut;

    // Title content
    public record TitleContent(
            String title,
            String subtitle
    ) {}

    // Location
    public record PlayerLocation(
            String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {

        /*
         *  Try to convert PlayerLocation to a Bukkit Location
         *  @return A Bukkit Location object, or null if the world is not found
         */
        public Location toLocation(World fallback) {
            World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world == null) world = fallback;
            if (world == null) return null;
            return new Location(world, x, y, z, yaw, pitch);
        }
    }

    public ConfigData(
            String serverName,
            String onboardGamemode,
            PlayerLocation onboardLocation,
            List<TitleContent> rulesSequenceContent,
            int rulesSequenceDuration,
            int rulesSequenceFadeIn,
            int rulesSequenceFadeOut,
            List<TitleContent> promptAccept,
            String promptChat,
            List<TitleContent> joinSequenceContent,
            int joinSequenceDuration,
            int joinSequenceFadeIn,
            int joinSequenceFadeOut) {
        this.serverName = serverName;
        this.onboardGamemode = onboardGamemode;
        this.onboardLocation = onboardLocation;
        this.rulesSequenceContent = rulesSequenceContent;
        this.rulesSequenceDuration = rulesSequenceDuration;
        this.rulesSequenceFadeIn = rulesSequenceFadeIn;
        this.rulesSequenceFadeOut = rulesSequenceFadeOut;
        this.promptAccept = promptAccept;
        this.promptChat = promptChat;
        this.joinSequenceContent = joinSequenceContent;
        this.joinSequenceDuration = joinSequenceDuration;
        this.joinSequenceFadeIn = joinSequenceFadeIn;
        this.joinSequenceFadeOut = joinSequenceFadeOut;
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

    public List<TitleContent> getRulesSequenceContent() {
        return rulesSequenceContent;
    }

    public int getRulesSequenceDuration() {
        return rulesSequenceDuration;
    }

    public int getRulesSequenceFadeIn() {
        return rulesSequenceFadeIn;
    }

    public int getRulesSequenceFadeOut() {
        return rulesSequenceFadeOut;
    }

    public List<TitleContent> getPromptAccept() {
        return promptAccept;
    }

    public String getPromptChat() {
        return promptChat;
    }

    public List<TitleContent> getJoinSequenceContent() {
        return joinSequenceContent;
    }

    public int getJoinSequenceDuration() {
        return joinSequenceDuration;
    }

    public int getJoinSequenceFadeIn() {
        return joinSequenceFadeIn;
    }

    public int getJoinSequenceFadeOut() {
        return joinSequenceFadeOut;
    }
}