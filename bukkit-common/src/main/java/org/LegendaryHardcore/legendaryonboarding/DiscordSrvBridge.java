package org.LegendaryHardcore.legendaryonboarding;

import github.scarsz.discordsrv.DiscordSRV;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

final class DiscordSrvBridge {
    private DiscordSrvBridge() {
    }

    static void forwardFirstJoinMessage(
            LegendaryOnboarding plugin,
            Player player,
            Component message
    ) {
        Plugin discordSrvPlugin = plugin.getServer()
                .getPluginManager()
                .getPlugin("DiscordSRV");
        if (!(discordSrvPlugin instanceof DiscordSRV discordSrv)
                || !discordSrv.isEnabled()) {
            return;
        }

        try {
            discordSrv.sendJoinMessage(
                    player,
                    PlainTextComponentSerializer.plainText().serialize(message)
            );
        } catch (RuntimeException exception) {
            plugin.getLogger().warning(
                    "Could not forward the completed onboarding announcement for "
                            + player.getName() + " to DiscordSRV: "
                            + exception.getMessage()
            );
        }
    }
}
