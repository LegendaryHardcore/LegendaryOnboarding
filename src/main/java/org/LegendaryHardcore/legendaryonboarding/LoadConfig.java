package org.LegendaryHardcore.legendaryonboarding;

import org.bukkit.configuration.file.FileConfiguration;
import org.LegendaryHardcore.legendaryonboarding.ConfigData.PlayerLocation;
import org.LegendaryHardcore.legendaryonboarding.ConfigData.TitleContent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.Map;

/*
 *  Load contents of config
 */
public class LoadConfig {
    private final LegendaryOnboarding plugin;

    public LoadConfig(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    /*
     *  Load contents of config.yml
     *  @return A ConfigData object, or null if the config file cannot be read
     */
    public ConfigData load() {
        FileConfiguration config = plugin.getConfig();

        try {
            // Server name
            String serverName = config.getString("SERVER_NAME");

            // LP pre-onboard and post-onboard groups
            String preOnboardGroup = config.getString("PRE_ONBOARD_GROUP", "DEFAULT");
            String postOnboardGroup = config.getString("POST_ONBOARD_GROUP", "player");

            String onboardGamemode = config.getString("ONBOARD_GAMEMODE", "SURVIVAL");

            // Location
            PlayerLocation onboardLocation = new PlayerLocation(
                    config.getString("POS_WORLD"),
                    config.getDouble("POS_X"),
                    config.getDouble("POS_Y"),
                    config.getDouble("POS_Z"),
                    (float) config.getDouble("POS_YAW"),
                    (float) config.getDouble("POS_PITCH")
            );

            // Rules sequence
            List<TitleContent> rulesContent = loadTitleContents(config.getList("RULES_SEQUENCE_CONTENT"));
            int rulesDuration = config.getInt("RULES_SEQUENCE_DURATION");
            int rulesSequenceFadeIn = config.getInt("RULES_SEQUENCE_FADE_IN");
            int rulesSequenceFadeOut = config.getInt("RULES_SEQUENCE_FADE_OUT");

            // Prompt accept (title and chat message)
            List<TitleContent> promptAccept = loadTitleContents(config.getList("PROMPT_ACCEPT"));
            String promptChat = config.getString("PROMPT_CHAT");

            // Join sequence
            List<TitleContent> joinContent = loadTitleContents(config.getList("JOIN_SEQUENCE_CONTENT"));
            int joinDuration = config.getInt("JOIN_SEQUENCE_DURATION");
            int joinSequenceFadeIn = config.getInt("JOIN_SEQUENCE_FADE_IN");
            int joinSequenceFadeOut = config.getInt("JOIN_SEQUENCE_FADE_OUT");

            // Random spawn
            int randomSpawnX1 = config.getInt("RANDOM_SPAWN_X1");
            int randomSpawnZ1 = config.getInt("RANDOM_SPAWN_Z1");
            int randomSpawnX2 = config.getInt("RANDOM_SPAWN_X2");
            int randomSpawnZ2 = config.getInt("RANDOM_SPAWN_Z2");
            return new ConfigData(
                    serverName,
                    preOnboardGroup,
                    postOnboardGroup,
                    onboardGamemode,
                    onboardLocation,
                    rulesContent,
                    rulesDuration,
                    rulesSequenceFadeIn,
                    rulesSequenceFadeOut,
                    promptAccept,
                    promptChat,
                    joinContent,
                    joinDuration,
                    joinSequenceFadeIn,
                    joinSequenceFadeOut,
                    randomSpawnX1,
                    randomSpawnZ1,
                    randomSpawnX2,
                    randomSpawnZ2
            );

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to load config file- ", e);
            return null;
        }
    }

    /*
     *  Load list of title and subtitles from config
     */
    private List<TitleContent> loadTitleContents(List<?> titleContents) {
        if (titleContents == null) {
            return new ArrayList<>();
        }

        List<TitleContent> contents = new ArrayList<>();
        for (Object item : titleContents) {
            if (item instanceof Map<?, ?> map) {
                Object titleObj = map.get("TITLE");
                Object subtitleObj = map.get("SUBTITLE");

                String title = (titleObj instanceof String) ? (String) titleObj : "";
                String subtitle = (subtitleObj instanceof String) ? (String) subtitleObj : "";

                contents.add(new TitleContent(title, subtitle));
            }
        }
        return contents;
    }
}


