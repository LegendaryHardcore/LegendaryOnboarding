package org.LegendaryHardcore.legendaryonboarding;

import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.objects.MessageFormat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Owns the direct DiscordSRV announcement templates in discordSRVmessages.yml.
 */
public final class DiscordSrvMessagesConfig {
    public enum Type {
        FIRST_JOIN,
        JOIN,
        QUIT
    }

    private static final String FILE_NAME = "discordSRVmessages.yml";

    private final MessageTemplate firstJoin;
    private final MessageTemplate join;
    private final MessageTemplate quit;

    private DiscordSrvMessagesConfig(
            MessageTemplate firstJoin,
            MessageTemplate join,
            MessageTemplate quit
    ) {
        this.firstJoin = firstJoin;
        this.join = join;
        this.quit = quit;
    }

    public static DiscordSrvMessagesConfig updateAndLoad(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        try (InputStream defaults = plugin.getResource(FILE_NAME)) {
            if (defaults == null) {
                plugin.getLogger().severe("Missing bundled config resource: " + FILE_NAME);
                return null;
            }
            ConfigUpdater.UpdateResult result = ConfigUpdater.update(file, defaults);
            if (result.updated()) {
                plugin.getLogger().info("Updated " + FILE_NAME + " to the current layout.");
            }

            YamlConfiguration yaml = ConfigUpdater.load(file);
            return new DiscordSrvMessagesConfig(
                    loadTemplate(yaml, Type.FIRST_JOIN),
                    loadTemplate(yaml, Type.JOIN),
                    loadTemplate(yaml, Type.QUIT)
            );
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Could not update or load " + FILE_NAME + ".",
                    exception
            );
            return null;
        }
    }

    public MessageTemplate get(Type type) {
        return switch (type) {
            case FIRST_JOIN -> firstJoin;
            case JOIN -> join;
            case QUIT -> quit;
        };
    }

    static MessageTemplate loadTemplate(YamlConfiguration yaml, Type type) {
        String path = type.name();
        if (!yaml.getBoolean(path + ".ENABLED", false)) {
            return MessageTemplate.disabled();
        }

        ConfigurationSection embed = yaml.getConfigurationSection(path + ".EMBED");
        EmbedTemplate embedTemplate = embed == null || !embed.getBoolean("ENABLED", true)
                ? null
                : loadEmbed(embed);
        return new MessageTemplate(
                true,
                yaml.getString(path + ".CONTENT", ""),
                embedTemplate
        );
    }

    private static EmbedTemplate loadEmbed(ConfigurationSection embed) {
        ConfigurationSection author = embed.getConfigurationSection("AUTHOR");
        ConfigurationSection title = embed.getConfigurationSection("TITLE");
        ConfigurationSection footer = embed.getConfigurationSection("FOOTER");

        return new EmbedTemplate(
                color(embed.get("COLOR")),
                value(author, "NAME"),
                value(author, "URL"),
                value(author, "IMAGE_URL"),
                embed.getString("THUMBNAIL_URL", ""),
                value(title, "TEXT"),
                value(title, "URL"),
                embed.getString("DESCRIPTION", ""),
                embed.getString("IMAGE_URL", ""),
                value(footer, "TEXT"),
                value(footer, "ICON_URL"),
                embed.getBoolean("TIMESTAMP", false),
                fields(embed.getStringList("FIELDS"))
        );
    }

    private static String value(ConfigurationSection section, String key) {
        return section == null ? "" : section.getString(key, "");
    }

    private static int color(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (!(value instanceof String text) || text.isBlank()) {
            return 0;
        }

        String normalized = text.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        try {
            return Integer.parseInt(normalized, 16);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static List<FieldTemplate> fields(List<String> values) {
        List<FieldTemplate> fields = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (value.equalsIgnoreCase("blank")) {
                fields.add(new FieldTemplate("\u200B", "\u200B", false));
                continue;
            }

            String[] parts = value.split(";", 3);
            if (parts.length < 2) {
                continue;
            }
            boolean inline = parts.length == 3 && Boolean.parseBoolean(parts[2].trim());
            fields.add(new FieldTemplate(parts[0], parts[1], inline));
        }
        return List.copyOf(fields);
    }

    public record MessageTemplate(
            boolean enabled,
            String content,
            EmbedTemplate embed
    ) {
        static MessageTemplate disabled() {
            return new MessageTemplate(false, "", null);
        }

        public MessageFormat toMessageFormat() {
            MessageFormat format = new MessageFormat();
            format.setContent(emptyToNull(content));
            if (embed == null) {
                return format;
            }

            format.setColorRaw(embed.color());
            format.setAuthorName(emptyToNull(embed.authorName()));
            format.setAuthorUrl(emptyToNull(embed.authorUrl()));
            format.setAuthorImageUrl(emptyToNull(embed.authorImageUrl()));
            format.setThumbnailUrl(emptyToNull(embed.thumbnailUrl()));
            format.setTitle(emptyToNull(embed.title()));
            format.setTitleUrl(emptyToNull(embed.titleUrl()));
            format.setDescription(emptyToNull(embed.description()));
            format.setImageUrl(emptyToNull(embed.imageUrl()));
            format.setFooterText(emptyToNull(embed.footerText()));
            format.setFooterIconUrl(emptyToNull(embed.footerIconUrl()));
            if (embed.timestamp()) {
                format.setTimestamp(Instant.now());
            }
            format.setFields(embed.fields().stream()
                    .map(field -> new MessageEmbed.Field(
                            field.name(),
                            field.value(),
                            field.inline()
                    ))
                    .toList());
            return format;
        }

        private static String emptyToNull(String value) {
            return value == null || value.isBlank() ? null : value;
        }
    }

    record EmbedTemplate(
            int color,
            String authorName,
            String authorUrl,
            String authorImageUrl,
            String thumbnailUrl,
            String title,
            String titleUrl,
            String description,
            String imageUrl,
            String footerText,
            String footerIconUrl,
            boolean timestamp,
            List<FieldTemplate> fields
    ) {
    }

    record FieldTemplate(String name, String value, boolean inline) {
    }
}
