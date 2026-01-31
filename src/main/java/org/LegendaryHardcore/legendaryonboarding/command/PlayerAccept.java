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

        boolean allowedNow = plugin.canAcceptRules.getOrDefault(uuid, false);
        if (!allowedNow) {
            player.sendMessage(ChatColor.RED + "You are not allowed to use this command.");
            return true;
        }

        // Mark accepted (persistent)
        plugin.getAcceptedStore().setAccepted(uuid, true);

        // Immediately prevent re-running /accept during join sequence
        plugin.canAcceptRules.put(uuid, false);

        // Start join sequence and send player back to their last location
        plugin.getJoinSequence().start(player);
        return true;
    }
}