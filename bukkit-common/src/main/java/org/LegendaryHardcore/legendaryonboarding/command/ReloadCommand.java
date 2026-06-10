package org.LegendaryHardcore.legendaryonboarding.command;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ReloadCommand implements CommandExecutor, TabCompleter {
    private static final String RELOAD_PERMISSION = "legendaryonboarding.reload";
    private final LegendaryOnboarding plugin;

    public ReloadCommand(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " reload");
            return true;
        }

        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }

        if (!plugin.reloadPluginConfig()) {
            sender.sendMessage(ChatColor.RED + "LegendaryOnboarding could not reload. Check the server log.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "LegendaryOnboarding reloaded.");
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission(RELOAD_PERMISSION) || args.length != 1) {
            return List.of();
        }

        return completeFirstArgument(args[0]);
    }

    static List<String> completeFirstArgument(String input) {
        return "reload".regionMatches(true, 0, input, 0, input.length())
                ? List.of("reload")
                : List.of();
    }
}
