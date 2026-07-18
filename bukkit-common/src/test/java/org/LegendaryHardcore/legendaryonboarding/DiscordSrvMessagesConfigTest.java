package org.LegendaryHardcore.legendaryonboarding;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordSrvMessagesConfigTest {
    @Test
    void readsDiscordSrvStyleEmbedTemplate() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("FIRST_JOIN.ENABLED", true);
        yaml.set("FIRST_JOIN.CONTENT", "Welcome {player}");
        yaml.set("FIRST_JOIN.EMBED.ENABLED", true);
        yaml.set("FIRST_JOIN.EMBED.COLOR", "#112233");
        yaml.set("FIRST_JOIN.EMBED.TITLE.TEXT", "Hello {display_name}");
        yaml.set("FIRST_JOIN.EMBED.TIMESTAMP", true);
        yaml.set("FIRST_JOIN.EMBED.FIELDS", java.util.List.of(
                "Server;{server_name};true",
                "blank"
        ));

        DiscordSrvMessagesConfig.MessageTemplate template =
                DiscordSrvMessagesConfig.loadTemplate(
                        yaml,
                        DiscordSrvMessagesConfig.Type.FIRST_JOIN
                );

        assertTrue(template.enabled());
        assertEquals("Welcome {player}", template.content());
    }

    @Test
    void disabledTemplateDoesNotCreateContent() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("QUIT.ENABLED", false);

        DiscordSrvMessagesConfig.MessageTemplate template =
                DiscordSrvMessagesConfig.loadTemplate(
                        yaml,
                        DiscordSrvMessagesConfig.Type.QUIT
                );

        assertFalse(template.enabled());
        assertEquals("", template.content());
    }
}
