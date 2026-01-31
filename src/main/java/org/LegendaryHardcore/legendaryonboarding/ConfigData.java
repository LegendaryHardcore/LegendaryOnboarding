package org.LegendaryHardcore.legendaryonboarding;

import org.bukkit.Location;
import org.bukkit.World;
import java.util.List;

/*
 *  Class for data loaded from config
 */
public final class ConfigData {
    private final String serverName;
    private final String preOnboardGroup;
    private final String postOnboardGroup;
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
    private final int randomSpawnX1;
    private final int randomSpawnZ1;
    private final int randomSpawnX2;
    private final int randomSpawnZ2;

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
        public Location toLocation() {
            World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world == null) {

                return null;
            }
            return new Location(world, x, y, z, yaw, pitch);
        }
    }

    public ConfigData(
            String serverName,
            String preOnboardGroup,
            String postOnboardGroup,
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
            int joinSequenceFadeOut,
            int randomSpawnX1,
            int randomSpawnZ1,
            int randomSpawnX2,
            int randomSpawnZ2) {
        this.serverName = serverName;
        this.preOnboardGroup = preOnboardGroup;
        this.postOnboardGroup = postOnboardGroup;
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
        this.randomSpawnX1 = randomSpawnX1;
        this.randomSpawnZ1 = randomSpawnZ1;
        this.randomSpawnX2 = randomSpawnX2;
        this.randomSpawnZ2 = randomSpawnZ2;
    }

    public String getServerName() {
        return serverName;
    }
    public String getPreOnboardGroup() {
        return preOnboardGroup;
    }

    public String getPostOnboardGroup() {
        return postOnboardGroup;
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

    public int getRandomSpawnX1() {
        return randomSpawnX1;
    }

    public int getRandomSpawnZ1() {
        return randomSpawnZ1;
    }

    public int getRandomSpawnX2() {
        return randomSpawnX2;
    }

    public int getRandomSpawnZ2() {
        return randomSpawnZ2;
    }
}