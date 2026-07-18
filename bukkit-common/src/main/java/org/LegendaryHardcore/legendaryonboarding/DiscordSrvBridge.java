package org.LegendaryHardcore.legendaryonboarding;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Sends LegendaryOnboarding-owned notifications through DiscordSRV. */
public final class DiscordSrvBridge {
    private DiscordSrvBridge() {
    }

    public static void forward(
            LegendaryOnboarding plugin,
            DiscordSrvMessagesConfig.Type type,
            Player player,
            Component gameMessage
    ) {
        DiscordSrvMessagesConfig.MessageTemplate template = plugin
                .getDiscordSrvMessagesConfig()
                .get(type);
        if (!template.enabled()) {
            return;
        }

        Plugin discordSrvPlugin = plugin.getServer()
                .getPluginManager()
                .getPlugin("DiscordSRV");
        if (!(discordSrvPlugin instanceof DiscordSRV discordSrv)
                || !discordSrv.isEnabled()) {
            return;
        }

        try {
            TextChannel channel = discordSrv.getMainTextChannel();
            if (channel == null) {
                plugin.getLogger().warning(
                        "DiscordSRV has no main Discord channel; could not send "
                                + type + " announcement for " + player.getName() + "."
                );
                return;
            }
            String avatarUrl = DiscordSRV.getAvatarUrl(player);
            plugin.debugLog(() -> "DiscordSRV avatar URL for "
                    + player.getName() + ": " + avatarUrl);
            MessageFormat format = template.toMessageFormat();
            Message message = DiscordSRV.translateMessage(
                    format,
                    (value, escapeMarkdown) -> replacePlaceholders(
                            value,
                            Boolean.TRUE.equals(escapeMarkdown),
                            plugin,
                            player,
                            gameMessage,
                            avatarUrl
                    )
            );
            if (message != null) {
                DiscordUtil.queueMessage(channel, message);
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().warning(
                    "Could not forward the " + type + " announcement for "
                            + player.getName() + " to DiscordSRV: "
                            + exception.getMessage()
            );
        }
    }

    private static String replacePlaceholders(
            String value,
            boolean escapeMarkdown,
            LegendaryOnboarding plugin,
            Player player,
            Component gameMessage,
            String avatarUrl
    ) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        String playerName = replacement(player.getName(), escapeMarkdown);
        String displayName = replacement(
                PlainTextComponentSerializer.plainText().serialize(player.displayName()),
                escapeMarkdown
        );
        String message = replacement(
                PlainTextComponentSerializer.plainText().serialize(gameMessage),
                escapeMarkdown
        );
        return value
                .replace("{player}", playerName)
                .replace("{display_name}", displayName)
                .replace("{message}", message)
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{avatar_url}", avatarUrl)
                .replace("{server_name}", replacement(
                        plugin.getConfigData().getServerName(),
                        escapeMarkdown
                ));
    }

    private static String replacement(String value, boolean escapeMarkdown) {
        return escapeMarkdown ? DiscordUtil.escapeMarkdown(value) : value;
    }
}
