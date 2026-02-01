package org.LegendaryHardcore.legendaryonboarding;

import org.LegendaryHardcore.legendaryonboarding.ConfigData.PlayerLocation;
import org.LegendaryHardcore.legendaryonboarding.ConfigData.TitleContent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

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
            String serverName = config.getString("SERVER_NAME", "Minecraft Server");

            // Normalize Gamemode
            String onboardGamemode = config.getString("ONBOARD_GAMEMODE", "SURVIVAL").toUpperCase();

            // Location (world name required; provide default)
            String worldName = config.getString("POS_WORLD", "world");

            // Are we teleporting the player?
            boolean onboardTeleport = config.getBoolean("ONBOARD_TELEPORT", true);

            // Validate that world exists
            World w = Bukkit.getWorld(worldName);
            if (w == null) {
                plugin.getLogger().severe("[LegendaryOnboarding] World " + worldName + " not found!");
                return null;
            }


            PlayerLocation onboardLocation = new PlayerLocation(
                    worldName,
                    config.getDouble("POS_X", 0.0),
                    config.getDouble("POS_Y", 120.0),
                    config.getDouble("POS_Z", 0.0),
                    (float) config.getDouble("POS_YAW", 0.0),
                    (float) config.getDouble("POS_PITCH", 0.0)
            );

            // Rules sequence
            List<TitleContent> rulesContent = loadTitleContents(config.getMapList("RULES_SEQUENCE_CONTENT"));
            int rulesDuration = nonNegative(config.getInt("RULES_SEQUENCE_DURATION", 5));
            int rulesSequenceFadeIn = nonNegative(config.getInt("RULES_SEQUENCE_FADE_IN", 1));
            int rulesSequenceFadeOut = nonNegative(config.getInt("RULES_SEQUENCE_FADE_OUT", 1));

            // Prompt accept (title and chat message)
            List<TitleContent> promptAccept = loadTitleContents(config.getMapList("PROMPT_ACCEPT"));
            String promptChat = config.getString("PROMPT_CHAT", "");

            // Commands whitelist that the commands don't consume
            List<String> commandWhitelist = config.getStringList("COMMAND_WHITELIST")
                    .stream()
                    .map(String::toLowerCase)
                    .toList();

            // Join sequence
            List<TitleContent> joinContent = loadTitleContents(config.getMapList("JOIN_SEQUENCE_CONTENT"));
            int joinDuration = nonNegative(config.getInt("JOIN_SEQUENCE_DURATION", 3));
            int joinSequenceFadeIn = nonNegative(config.getInt("JOIN_SEQUENCE_FADE_IN", 1));
            int joinSequenceFadeOut = nonNegative(config.getInt("JOIN_SEQUENCE_FADE_OUT", 1));

            return new ConfigData(
                    serverName,
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
                    commandWhitelist,
                    onboardTeleport
            );

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to load config file- ", e);
            return null;
        }
    }

    public static int nonNegative(int value) {
        return Math.max(0, value);
    }

    /*
     *  Load list of title and subtitles from config
     */
    private List<TitleContent> loadTitleContents(List<Map<?, ?>> titleContents) {
        if (titleContents == null || titleContents.isEmpty()) return new ArrayList<>();

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


