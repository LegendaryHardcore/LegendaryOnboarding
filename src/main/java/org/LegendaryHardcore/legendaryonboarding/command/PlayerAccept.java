package org.LegendaryHardcore.legendaryonboarding.command;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class PlayerAccept implements CommandExecutor {
    private final LegendaryOnboarding plugin;

    public PlayerAccept(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can't be run from console.");
            return true;
        }

        if (plugin.canAcceptRules.get(player.getUniqueId())) {
            plugin.getJoinSequence().start(player);
        }
        return true;
    }
}