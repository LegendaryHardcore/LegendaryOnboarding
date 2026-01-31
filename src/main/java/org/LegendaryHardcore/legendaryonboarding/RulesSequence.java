package org.LegendaryHardcore.legendaryonboarding;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.LegendaryHardcore.legendaryonboarding.ConfigData.TitleContent;
import java.util.List;

/*
 *  Rules sequence class.
 *  Displays welcome message, rules and prompts user to accept the rules
 */
public class RulesSequence {
    private final LegendaryOnboarding plugin;

    // For converting seconds to ticks (1 second = 20 ticks)
    private static final int TPS = 20;

    public RulesSequence(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    public void start(Player player) {
        // Ensure that player does not run /accept before all rules have been displayed
        plugin.canAcceptRules.put(player.getUniqueId(), false);

        // Freeze player in config specified gamemode at location of onboarding
        Location onboardLocation = plugin.getConfigData().getOnboardLocation().toLocation();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.teleport(onboardLocation);
            switch(plugin.getConfigData().getOnboardGamemode()) {
                case "SURVIVAL":
                    player.setGameMode(GameMode.SURVIVAL);
                    break;
                case "CREATIVE":
                    player.setGameMode(GameMode.CREATIVE);
                    break;
                case "ADVENTURE":
                    player.setGameMode(GameMode.ADVENTURE);
                    break;
                case "SPECTATOR":
                    player.setGameMode(GameMode.SPECTATOR);
                    break;
            }
            player.setGravity(false);
            player.setInvisible(true);
            player.setInvulnerable(true);

        }, 1L);

        /* Welcome message */
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                String serverName = plugin.getConfigData().getServerName();
                String playerName = player.getName();
                String welcomeMessage = "Welcome to " + serverName + ", " + playerName;
                player.sendTitle(welcomeMessage,"", 20, 60, 20);
        }, 0);

        /* Rules */
        List<TitleContent> contents = plugin.getConfigData().getRulesSequenceContent();
        final int duration = plugin.getConfigData().getRulesSequenceDuration() * TPS;
        final int fadeIn = plugin.getConfigData().getRulesSequenceFadeIn() * TPS;
        final int fadeOut = plugin.getConfigData().getRulesSequenceFadeOut() * TPS;

        int delay = duration + fadeIn + fadeOut;
        int current_delay = 100;

        // For each rule in the config, display title and subtitle
        for (int i = 0; i < contents.size(); i++) {
            final TitleContent content = contents.get(i);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    String title = content.title();
                    String subtitle = content.subtitle();
                    player.sendTitle(title, subtitle, fadeIn, duration, fadeOut);

            }, current_delay);
            current_delay += delay;
        }

        /* Prompt Accept */
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Escape if player is offline
            if (!player.isOnline()) {
                return;
            }

            plugin.canAcceptRules.put(player.getUniqueId(), true);

            // Prompt player to run the /accept command
            List<TitleContent> promptContents = plugin.getConfigData().getPromptAccept();
            for (TitleContent prompt : promptContents) {
                String title = prompt.title();
                String subtitle = prompt.subtitle();
                player.sendTitle(title, subtitle, 20, 9999, 0);
                }

            player.sendMessage(plugin.getConfigData().getPromptChat());
        },current_delay);
    }
}
