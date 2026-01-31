package org.LegendaryHardcore.legendaryonboarding.command;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import org.jetbrains.annotations.NotNull;

public class PlayerAccept implements CommandExecutor {
    private final LegendaryOnboarding plugin;

    public PlayerAccept(LegendaryOnboarding plugin) {

        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can't be run from console.");
            return true;
        }

        final UUID uuid = player.getUniqueId();

        // Handle player that has already excepted and bail early
        if (plugin.getAcceptedStore().isAccepted(uuid)) {
            player.sendMessage(ChatColor.RED + "You are already accepted the rules.");
            return true;
        }

        // Immediately prevent re-running /accept during join sequence
        plugin.canAcceptRules.put(uuid, false);

        // Start join sequence and send player back to their last location
        player.getScheduler().run(plugin, task -> plugin.getJoinSequence().start(player), null);
        return true;
    }
}