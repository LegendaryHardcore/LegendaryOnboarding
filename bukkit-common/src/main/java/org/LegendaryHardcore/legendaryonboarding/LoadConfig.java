package org.LegendaryHardcore.legendaryonboarding;

import org.LegendaryHardcore.legendaryonboarding.ConfigData.PlayerLocation;
import org.LegendaryHardcore.legendaryonboarding.ConfigData.TitleContent;
import org.LegendaryHardcore.legendaryonboarding.ConfigData.MessageConsumption;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventPriority;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/*
 * Load operational settings from config.yml and sequence settings from titlesequence.yml.
 */
public class LoadConfig {
    private final LegendaryOnboarding plugin;
    private final YamlConfiguration titleSequence;

    public LoadConfig(LegendaryOnboarding plugin, YamlConfiguration titleSequence) {
        this.plugin = plugin;
        this.titleSequence = titleSequence;
    }

    /*
     * Load the combined runtime configuration.
     *  @return A ConfigData object, or null if the config file cannot be read
     */
    public ConfigData load() {
        FileConfiguration config = plugin.getConfig();

        try {
            boolean configuredSequenceEnabled = titleSequence.getBoolean("ENABLED", false);
            boolean debugForceOnboarding = config.getBoolean("DEBUG_FORCE_ONBOARDING", false);
            boolean titleSequenceEnabled =
                    isSequenceActive(configuredSequenceEnabled, debugForceOnboarding);

            // Server name
            String serverName = config.getString("SERVER_NAME", "Minecraft Server");

            // Normalize Gamemode
            String onboardGamemode = config.getString("ONBOARD_GAMEMODE", "SURVIVAL").toUpperCase();
            if (onboardGamemode.equals("SPECTATOR")) {
                plugin.getLogger().warning(
                        "ONBOARD_GAMEMODE is SPECTATOR, which prevents players from seeing "
                                + "the blindness fog. Use ADVENTURE, SURVIVAL, or CREATIVE "
                                + "when blindness should be visible."
                );
            }

            // Location used only when teleporting to a dedicated onboarding area.
            String worldName = config.getString("POS_WORLD", "world");
            World.Environment onboardWorldEnvironment = parseWorldEnvironment(
                    config.getString("POS_WORLD_TYPE", "NORMAL")
            );

            // Are we teleporting the player?
            boolean onboardTeleport = config.getBoolean("ONBOARD_TELEPORT", false);
            boolean debugLogging = config.getBoolean("DEBUG_LOGGING", false);
            boolean blockAdvancements = config.getBoolean("BLOCK_ADVANCEMENTS", true);
            boolean onboardUnacceptedReturningPlayers =
                    config.getBoolean("ONBOARD_UNACCEPTED_RETURNING_PLAYERS", false);
            boolean blockExternalMessages =
                    config.getBoolean("BLOCK_EXTERNAL_MESSAGES_DURING_ONBOARDING", true);
            MessageConsumption inGameJoinMessages = parseMessageConsumption(
                    config.getString("MESSAGE_CONSUMPTION.IN_GAME.JOIN"),
                    MessageConsumption.SOME,
                    "MESSAGE_CONSUMPTION.IN_GAME.JOIN"
            );
            MessageConsumption inGameQuitMessages = parseMessageConsumption(
                    config.getString("MESSAGE_CONSUMPTION.IN_GAME.QUIT"),
                    MessageConsumption.SOME,
                    "MESSAGE_CONSUMPTION.IN_GAME.QUIT"
            );
            MessageConsumption discordSrvJoinMessages = parseMessageConsumption(
                    config.getString("MESSAGE_CONSUMPTION.DISCORDSRV.JOIN"),
                    MessageConsumption.SOME,
                    "MESSAGE_CONSUMPTION.DISCORDSRV.JOIN"
            );
            MessageConsumption discordSrvQuitMessages = parseMessageConsumption(
                    config.getString("MESSAGE_CONSUMPTION.DISCORDSRV.QUIT"),
                    MessageConsumption.SOME,
                    "MESSAGE_CONSUMPTION.DISCORDSRV.QUIT"
            );
            boolean hideOnboardingPlayersFromTab =
                    config.getBoolean("HIDE_ONBOARDING_PLAYERS_FROM_TAB", true);
            boolean cleanupFallbackEnabled =
                    config.getBoolean("CLEANUP_FALLBACK_ENABLED", false);
            World.Environment cleanupFallbackEnvironment = parseWorldEnvironment(
                    config.getString("CLEANUP_FALLBACK_WORLD_TYPE", "NORMAL")
            );
            PlayerLocation cleanupFallbackLocation = cleanupFallbackEnabled
                    ? new PlayerLocation(
                    config.getString("CLEANUP_FALLBACK_WORLD", "world"),
                    config.getDouble("CLEANUP_FALLBACK_X", 0.0),
                    config.getDouble("CLEANUP_FALLBACK_Y", 64.0),
                    config.getDouble("CLEANUP_FALLBACK_Z", 0.0),
                    (float) config.getDouble("CLEANUP_FALLBACK_YAW", 0.0),
                    (float) config.getDouble("CLEANUP_FALLBACK_PITCH", 0.0),
                    cleanupFallbackEnvironment
            )
                    : null;
            int cleanupFallbackRadius = boundedFallbackRadius(
                    config.getInt("CLEANUP_FALLBACK_RADIUS", 5000)
            );
            int returnDesiredY =
                    config.getInt("RETURN_DESIRED_Y", 64);
            String onboardingDamageMessage = config.getString(
                    "ONBOARDING_DAMAGE_MESSAGE",
                    "{yellow}{player} is currently onboarding."
            );
            String firstJoinMessage = config.getString(
                    "FIRST_JOIN_MESSAGE",
                    "{yellow}{player} joined the server for the first time"
            );
            ConfigData.EventPriorities eventPriorities = new ConfigData.EventPriorities(
                    parseEventPriority(
                            config.getString("EVENT_PRIORITIES.CHAT_CONSUMPTION"),
                            EventPriority.MONITOR,
                            "EVENT_PRIORITIES.CHAT_CONSUMPTION"
                    ),
                    parseEventPriority(
                            config.getString("EVENT_PRIORITIES.JOIN_MESSAGE"),
                            EventPriority.HIGHEST,
                            "EVENT_PRIORITIES.JOIN_MESSAGE"
                    ),
                    parseEventPriority(
                            config.getString("EVENT_PRIORITIES.QUIT_MESSAGE"),
                            EventPriority.HIGHEST,
                            "EVENT_PRIORITIES.QUIT_MESSAGE"
                    )
            );

            PlayerLocation onboardLocation = new PlayerLocation(
                    worldName,
                    config.getDouble("POS_X", 0.0),
                    config.getDouble("POS_Y", 120.0),
                    config.getDouble("POS_Z", 0.0),
                    (float) config.getDouble("POS_YAW", 0.0),
                    (float) config.getDouble("POS_PITCH", 0.0),
                    onboardWorldEnvironment
            );

            // Teleporting requires either the named world or a loaded world of
            // the configured environment type.
            if (requiresOnboardingWorld(titleSequenceEnabled, onboardTeleport)
                    && onboardLocation.toLocation() == null) {
                plugin.getLogger().severe(
                        "[LegendaryOnboarding] World " + worldName
                                + " not found and no loaded "
                                + onboardWorldEnvironment + " world is available!"
                );
                return null;
            }

            String actionBarCountdown = titleSequence.getString(
                    "ACTIONBAR_COUNTDOWN",
                    "{yellow}/accept unlocks in {gold}{seconds}{yellow}s"
            );
            int actionBarRefreshTicks = positiveOrDefault(
                    titleSequence.getInt("ACTIONBAR_REFRESH_TICKS", 20),
                    20
            );

            List<TitleContent> welcomeContent = loadTitleContents(
                    titleSequence.getMapList("WELCOME_SEQUENCE"),
                    1,
                    3,
                    1
            );

            int rulesSequenceFadeIn = nonNegative(titleSequence.getInt("RULES_SEQUENCE_FADE_IN", 1));
            int rulesDuration = nonNegative(titleSequence.getInt("RULES_SEQUENCE_DURATION", 5));
            int rulesSequenceFadeOut = nonNegative(titleSequence.getInt("RULES_SEQUENCE_FADE_OUT", 1));
            List<TitleContent> rulesContent = loadTitleContents(
                    titleSequence.getMapList("RULES_SEQUENCE_CONTENT"),
                    rulesSequenceFadeIn,
                    rulesDuration,
                    rulesSequenceFadeOut
            );

            // Prompt steps stay visible until /accept unless individually overridden.
            List<TitleContent> promptAccept = loadTitleContents(
                    titleSequence.getMapList("PROMPT_ACCEPT"),
                    1,
                    500,
                    0
            );

            // Commands whitelist that the commands don't consume
            List<String> commandWhitelist = config.getStringList("COMMAND_WHITELIST")
                    .stream()
                    .map(String::toLowerCase)
                    .toList();

            int joinSequenceFadeIn = nonNegative(titleSequence.getInt("JOIN_SEQUENCE_FADE_IN", 1));
            int joinDuration = nonNegative(titleSequence.getInt("JOIN_SEQUENCE_DURATION", 3));
            int joinSequenceFadeOut = nonNegative(titleSequence.getInt("JOIN_SEQUENCE_FADE_OUT", 1));
            List<TitleContent> joinContent = loadTitleContents(
                    titleSequence.getMapList("JOIN_SEQUENCE_CONTENT"),
                    joinSequenceFadeIn,
                    joinDuration,
                    joinSequenceFadeOut
            );

            return new ConfigData(
                    titleSequenceEnabled,
                    serverName,
                    onboardGamemode,
                    onboardLocation,
                    actionBarCountdown,
                    actionBarRefreshTicks,
                    welcomeContent,
                    rulesContent,
                    promptAccept,
                    joinContent,
                    commandWhitelist,
                    onboardTeleport,
                    debugForceOnboarding,
                    debugLogging,
                    blockAdvancements,
                    onboardUnacceptedReturningPlayers,
                    blockExternalMessages,
                    inGameJoinMessages,
                    inGameQuitMessages,
                    discordSrvJoinMessages,
                    discordSrvQuitMessages,
                    hideOnboardingPlayersFromTab,
                    cleanupFallbackLocation,
                    cleanupFallbackEnvironment,
                    cleanupFallbackRadius,
                    returnDesiredY,
                    onboardingDamageMessage,
                    firstJoinMessage,
                    eventPriorities
            );

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to load config file- ", e);
            return null;
        }
    }

    public static int nonNegative(int value) {
        return Math.max(0, value);
    }

    static int positiveOrDefault(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    static int boundedFallbackRadius(int value) {
        return Math.min(30_000_000, nonNegative(value));
    }

    static World.Environment parseWorldEnvironment(String value) {
        if (value == null) return World.Environment.NORMAL;
        return switch (value.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "NETHER" -> World.Environment.NETHER;
            case "END", "THE_END" -> World.Environment.THE_END;
            default -> World.Environment.NORMAL;
        };
    }

    static boolean requiresOnboardingWorld(boolean titleSequenceEnabled, boolean onboardTeleport) {
        return titleSequenceEnabled && onboardTeleport;
    }

    static boolean isSequenceActive(boolean configuredSequenceEnabled, boolean debugForceOnboarding) {
        return configuredSequenceEnabled || debugForceOnboarding;
    }

    private EventPriority parseEventPriority(
            String value,
            EventPriority fallback,
            String path
    ) {
        EventPriority parsed = parseEventPriority(value);
        if (parsed != null) return parsed;
        plugin.getLogger().warning(
                path + " must be LOWEST, LOW, NORMAL, HIGH, HIGHEST, or MONITOR; using "
                        + fallback + "."
        );
        return fallback;
    }

    static EventPriority parseEventPriority(String value) {
        if (value == null) return null;
        try {
            return EventPriority.valueOf(
                    value.trim().toUpperCase(java.util.Locale.ROOT)
            );
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private MessageConsumption parseMessageConsumption(
            String value,
            MessageConsumption fallback,
            String path
    ) {
        MessageConsumption parsed = parseMessageConsumption(value);
        if (parsed != null) return parsed;
        plugin.getLogger().warning(path + " must be NONE, SOME, or ALL; using " + fallback + ".");
        return fallback;
    }

    static MessageConsumption parseMessageConsumption(String value) {
        if (value == null) return null;
        try {
            return MessageConsumption.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /*
     *  Load list of title and subtitles from config
     */
    static List<TitleContent> loadTitleContents(
            List<Map<?, ?>> titleContents,
            int defaultFadeIn,
            int defaultDuration,
            int defaultFadeOut
    ) {
        if (titleContents == null || titleContents.isEmpty()) return new ArrayList<>();

        List<TitleContent> contents = new ArrayList<>();
        for (Object item : titleContents) {
            if (item instanceof Map<?, ?> map) {
                String title = stringValue(map.get("TITLE"));
                String subtitle = stringValue(map.get("SUBTITLE"));
                String chat = stringValue(map.get("CHAT"));
                int fadeIn = nonNegative(intValue(map.get("FADE_IN"), defaultFadeIn));
                int duration = nonNegative(intValue(map.get("DURATION"), defaultDuration));
                int fadeOut = nonNegative(intValue(map.get("FADE_OUT"), defaultFadeOut));
                boolean stopAllSounds = booleanValue(map.get("STOP_ALL_SOUNDS"), false);
                ConfigData.SoundEffect sound = loadSound(map.get("SOUND"));
                List<ConfigData.PotionEffectConfig> potionEffects =
                        loadPotionEffects(map.get("POTION_EFFECTS"));
                List<String> removePotionEffects = stringList(map.get("REMOVE_POTION_EFFECTS"));

                contents.add(new TitleContent(
                        title,
                        subtitle,
                        chat,
                        fadeIn,
                        duration,
                        fadeOut,
                        stopAllSounds,
                        sound,
                        potionEffects,
                        removePotionEffects
                ));
            }
        }
        return contents;
    }

    static ConfigData.SoundEffect loadSound(Object value) {
        if (value instanceof String name && !name.isBlank()) {
            return new ConfigData.SoundEffect(name, SoundCategory.MASTER, 1.0f, 1.0f);
        }
        if (!(value instanceof Map<?, ?> map)) return null;

        String name = stringValue(map.get("NAME"));
        if (name.isBlank()) return null;

        SoundCategory category;
        try {
            category = SoundCategory.valueOf(
                    stringValue(map.get("CATEGORY")).toUpperCase(java.util.Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            category = SoundCategory.MASTER;
        }

        return new ConfigData.SoundEffect(
                name,
                category,
                Math.max(0.0f, floatValue(map.get("VOLUME"), 1.0f)),
                Math.max(0.0f, floatValue(map.get("PITCH"), 1.0f))
        );
    }

    static List<ConfigData.PotionEffectConfig> loadPotionEffects(Object value) {
        if (!(value instanceof List<?> values)) return List.of();

        List<ConfigData.PotionEffectConfig> effects = new ArrayList<>();
        for (Object item : values) {
            if (!(item instanceof Map<?, ?> map)) continue;
            String type = stringValue(map.get("TYPE"));
            if (type.isBlank()) continue;

            effects.add(new ConfigData.PotionEffectConfig(
                    type,
                    intValue(map.get("DURATION"), -1),
                    nonNegative(intValue(map.get("AMPLIFIER"), 0)),
                    booleanValue(map.get("AMBIENT"), false),
                    booleanValue(map.get("PARTICLES"), false),
                    booleanValue(map.get("ICON"), false)
            ));
        }
        return List.copyOf(effects);
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> values)) return List.of();
        return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private static String stringValue(Object value) {
        return value instanceof String string ? string : "";
    }

    private static int intValue(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static float floatValue(Object value, float fallback) {
        return value instanceof Number number ? number.floatValue() : fallback;
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        return value instanceof Boolean bool ? bool : fallback;
    }
}
