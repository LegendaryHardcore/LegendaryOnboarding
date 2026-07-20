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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
            Set<ConfigData.DebugOption> debugOptions = parseDebugOptions(
                    config.getStringList("DEBUG")
            );
            migrateLegacyDebugOptions(config, debugOptions);
            boolean debugForceOnboarding = debugOptions.contains(
                    ConfigData.DebugOption.FORCE_ONBOARDING
            );
            boolean titleSequenceEnabled =
                    isSequenceActive(configuredSequenceEnabled, debugForceOnboarding);

            // Server name
            String serverName = config.getString("SERVER_NAME", "Minecraft Server");

            // Normalize Gamemode
            String onboardGamemode = settingString(
                    config, "GAMEMODE", "ONBOARD_GAMEMODE", "SURVIVAL"
            ).toUpperCase();
            if (onboardGamemode.equals("SPECTATOR")) {
                plugin.getLogger().warning(
                        "ONBOARDSETTINGS.GAMEMODE is SPECTATOR, which prevents players from seeing "
                                + "the blindness fog. Use ADVENTURE, SURVIVAL, or CREATIVE "
                                + "when blindness should be visible."
                );
            }

            // Location used only when teleporting to a dedicated onboarding area.
            String worldName = settingString(config, "POS_WORLD", "POS_WORLD", "world");
            World.Environment onboardWorldEnvironment = parseWorldEnvironment(
                    settingString(config, "POS_WORLD_TYPE", "POS_WORLD_TYPE", "NORMAL")
            );

            // Are we teleporting the player?
            boolean onboardTeleport = settingBoolean(config, "TELEPORT", "ONBOARD_TELEPORT", false);
            boolean blockAdvancements = settingBoolean(
                    config, "BLOCK_ADVANCEMENTS", "BLOCK_ADVANCEMENTS", true
            );
            boolean onboardUnacceptedReturningPlayers =
                    settingBoolean(
                            config,
                            "FORCE_RETURNING_PLAYERS",
                            "ONBOARD_UNACCEPTED_RETURNING_PLAYERS",
                            false
                    );
            boolean blockExternalMessages =
                    settingBoolean(
                            config,
                            "BLOCK_EXTERNAL_MESSAGES_DURING_ONBOARDING",
                            "BLOCK_EXTERNAL_MESSAGES_DURING_ONBOARDING",
                            true
                    );
            MessageConsumption inGameJoinMessages = parseMessageConsumption(
                    settingString(
                            config,
                            "MESSAGE_CONSUMPTION.IN_GAME.JOIN",
                            "MESSAGE_CONSUMPTION.IN_GAME.JOIN",
                            null
                    ),
                    MessageConsumption.SOME,
                    "MESSAGE_CONSUMPTION.IN_GAME.JOIN"
            );
            MessageConsumption inGameQuitMessages = parseMessageConsumption(
                    settingString(
                            config,
                            "MESSAGE_CONSUMPTION.IN_GAME.QUIT",
                            "MESSAGE_CONSUMPTION.IN_GAME.QUIT",
                            null
                    ),
                    MessageConsumption.SOME,
                    "MESSAGE_CONSUMPTION.IN_GAME.QUIT"
            );
            MessageConsumption discordSrvJoinMessages = parseMessageConsumption(
                    settingString(
                            config,
                            "MESSAGE_CONSUMPTION.DISCORDSRV.JOIN",
                            "MESSAGE_CONSUMPTION.DISCORDSRV.JOIN",
                            null
                    ),
                    MessageConsumption.SOME,
                    "MESSAGE_CONSUMPTION.DISCORDSRV.JOIN"
            );
            MessageConsumption discordSrvQuitMessages = parseMessageConsumption(
                    settingString(
                            config,
                            "MESSAGE_CONSUMPTION.DISCORDSRV.QUIT",
                            "MESSAGE_CONSUMPTION.DISCORDSRV.QUIT",
                            null
                    ),
                    MessageConsumption.SOME,
                    "MESSAGE_CONSUMPTION.DISCORDSRV.QUIT"
            );
            boolean hideOnboardingPlayersFromTab =
                    settingBoolean(
                            config,
                            "HIDE_ONBOARDING_PLAYERS_FROM_TAB",
                            "HIDE_ONBOARDING_PLAYERS_FROM_TAB",
                            true
                    );
            boolean cleanupFallbackEnabled =
                    settingBoolean(
                            config,
                            "CLEANUP_FALLBACK_ENABLED",
                            "CLEANUP_FALLBACK_ENABLED",
                            false
                    );
            World.Environment cleanupFallbackEnvironment = parseWorldEnvironment(
                    settingString(
                            config,
                            "CLEANUP_FALLBACK_WORLD_TYPE",
                            "CLEANUP_FALLBACK_WORLD_TYPE",
                            "NORMAL"
                    )
            );
            PlayerLocation cleanupFallbackLocation = cleanupFallbackEnabled
                    ? new PlayerLocation(
                    settingString(
                            config, "CLEANUP_FALLBACK_WORLD", "CLEANUP_FALLBACK_WORLD", "world"
                    ),
                    settingDouble(config, "CLEANUP_FALLBACK_X", "CLEANUP_FALLBACK_X", 0.0),
                    settingDouble(config, "CLEANUP_FALLBACK_Y", "CLEANUP_FALLBACK_Y", 64.0),
                    settingDouble(config, "CLEANUP_FALLBACK_Z", "CLEANUP_FALLBACK_Z", 0.0),
                    (float) settingDouble(
                            config, "CLEANUP_FALLBACK_YAW", "CLEANUP_FALLBACK_YAW", 0.0
                    ),
                    (float) settingDouble(
                            config, "CLEANUP_FALLBACK_PITCH", "CLEANUP_FALLBACK_PITCH", 0.0
                    ),
                    cleanupFallbackEnvironment
            )
                    : null;
            int cleanupFallbackRadius = boundedFallbackRadius(
                    settingInt(
                            config,
                            "CLEANUP_FALLBACK_RADIUS",
                            "CLEANUP_FALLBACK_RADIUS",
                            5000
                    )
            );
            int returnDesiredY =
                    settingInt(config, "RETURN_DESIRED_Y", "RETURN_DESIRED_Y", 64);
            String onboardingDamageMessage = settingString(
                    config,
                    "DAMAGE_MESSAGE",
                    "ONBOARDING_DAMAGE_MESSAGE",
                    "{yellow}{player} is currently onboarding."
            );
            String firstJoinMessage = settingString(
                    config,
                    "INGAME_FIRST_JOIN_MESSAGE",
                    "FIRST_JOIN_MESSAGE",
                    "{yellow}{player} joined the server for the first time"
            );
            ConfigData.EventPriorities eventPriorities = new ConfigData.EventPriorities(
                    parseEventPriority(
                            settingString(
                                    config,
                                    "EVENT_PRIORITIES.CHAT_CONSUMPTION",
                                    "EVENT_PRIORITIES.CHAT_CONSUMPTION",
                                    null
                            ),
                            EventPriority.MONITOR,
                            "EVENT_PRIORITIES.CHAT_CONSUMPTION"
                    ),
                    parseEventPriority(
                            settingString(
                                    config,
                                    "EVENT_PRIORITIES.JOIN_MESSAGE",
                                    "EVENT_PRIORITIES.JOIN_MESSAGE",
                                    null
                            ),
                            EventPriority.HIGHEST,
                            "EVENT_PRIORITIES.JOIN_MESSAGE"
                    ),
                    parseEventPriority(
                            settingString(
                                    config,
                                    "EVENT_PRIORITIES.QUIT_MESSAGE",
                                    "EVENT_PRIORITIES.QUIT_MESSAGE",
                                    null
                            ),
                            EventPriority.HIGHEST,
                            "EVENT_PRIORITIES.QUIT_MESSAGE"
                    )
            );

            PlayerLocation onboardLocation = new PlayerLocation(
                    worldName,
                    settingDouble(config, "POS_X", "POS_X", 0.0),
                    settingDouble(config, "POS_Y", "POS_Y", 120.0),
                    settingDouble(config, "POS_Z", "POS_Z", 0.0),
                    (float) settingDouble(config, "POS_YAW", "POS_YAW", 0.0),
                    (float) settingDouble(config, "POS_PITCH", "POS_PITCH", 0.0),
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
            List<String> commandWhitelist = settingStringList(
                    config, "COMMAND_WHITELIST", "COMMAND_WHITELIST"
            )
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
                    debugOptions,
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

    private Set<ConfigData.DebugOption> parseDebugOptions(List<String> configuredOptions) {
        Set<ConfigData.DebugOption> options = EnumSet.noneOf(ConfigData.DebugOption.class);
        for (String configuredOption : configuredOptions) {
            if (configuredOption == null || configuredOption.isBlank()) continue;

            String normalized = configuredOption.trim()
                    .toUpperCase(java.util.Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
            normalized = switch (normalized) {
                case "LOG_SEQUENCESTART" -> "LOG_SEQUENCE_START";
                case "LOG_RETURNLOCATIONCAPTURE" -> "LOG_RETURN_LOCATION_CAPTURE";
                case "LOG_RETURNLOCATIONREUSE" -> "LOG_RETURN_LOCATION_REUSE";
                case "LOG_RETURNLOCATIONRESOLUTION" -> "LOG_RETURN_LOCATION_RESOLUTION";
                case "LOG_DEPARTURECLEANUP" -> "LOG_DEPARTURE_CLEANUP";
                case "LOG_JOINQUITMESSAGEHANDLING" -> "LOG_JOIN_QUIT_MESSAGE_HANDLING";
                case "LOG_DISCORDSRVAVATAR" -> "LOG_DISCORDSRV_AVATAR";
                case "LOG_ACTIONBAR_LIFECYCLE" -> "LOG_ACTION_BAR_LIFECYCLE";
                case "LOG_DEBUGFIXCLEANUP" -> "LOG_DEBUG_FIX_CLEANUP";
                case "LOG_DISCORD_SRV_AVATAR" -> "LOG_DISCORDSRV_AVATAR";
                default -> normalized;
            };
            try {
                options.add(ConfigData.DebugOption.valueOf(normalized));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning(
                        "Unknown DEBUG option '" + configuredOption + "'; ignoring it."
                );
            }
        }
        return options;
    }

    private static void migrateLegacyDebugOptions(
            FileConfiguration config,
            Set<ConfigData.DebugOption> debugOptions
    ) {
        if (config.getBoolean("DEBUG_FORCE_ONBOARDING", false)) {
            debugOptions.add(ConfigData.DebugOption.FORCE_ONBOARDING);
        }
        if (config.getBoolean("DEBUG_LOGGING", false)) {
            for (ConfigData.DebugOption option : ConfigData.DebugOption.values()) {
                if (option != ConfigData.DebugOption.FORCE_ONBOARDING) {
                    debugOptions.add(option);
                }
            }
        }
    }

    private static String settingPath(String setting) {
        return "ONBOARDSETTINGS." + setting;
    }

    private static boolean settingBoolean(
            FileConfiguration config,
            String setting,
            String legacyPath,
            boolean fallback
    ) {
        String path = settingPath(setting);
        return config.contains(path, true)
                ? config.getBoolean(path, fallback)
                : config.getBoolean(legacyPath, fallback);
    }

    private static String settingString(
            FileConfiguration config,
            String setting,
            String legacyPath,
            String fallback
    ) {
        String path = settingPath(setting);
        return config.contains(path, true)
                ? config.getString(path, fallback)
                : config.getString(legacyPath, fallback);
    }

    private static int settingInt(
            FileConfiguration config,
            String setting,
            String legacyPath,
            int fallback
    ) {
        String path = settingPath(setting);
        return config.contains(path, true)
                ? config.getInt(path, fallback)
                : config.getInt(legacyPath, fallback);
    }

    private static double settingDouble(
            FileConfiguration config,
            String setting,
            String legacyPath,
            double fallback
    ) {
        String path = settingPath(setting);
        return config.contains(path, true)
                ? config.getDouble(path, fallback)
                : config.getDouble(legacyPath, fallback);
    }

    private static List<String> settingStringList(
            FileConfiguration config,
            String setting,
            String legacyPath
    ) {
        String path = settingPath(setting);
        return config.contains(path, true)
                ? config.getStringList(path)
                : config.getStringList(legacyPath);
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
