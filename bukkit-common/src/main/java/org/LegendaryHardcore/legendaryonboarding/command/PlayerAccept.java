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

        if (!plugin.getConfigData().isTitleSequenceEnabled()) {
            sender.sendMessage(ChatColor.RED + "The onboarding sequence is currently disabled.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can't be run from console.");
            return true;
        }

        final UUID uuid = player.getUniqueId();

        // Handle player that has already excepted and bail early
        if (plugin.getAcceptedStore().isAccepted(uuid)
                && !plugin.debugForcedPlayers.contains(uuid)) {
            player.sendMessage(ChatColor.RED + "You have already accepted the rules.");
            return true;
        }

        boolean onboardingActive =
                plugin.canAcceptRules.containsKey(uuid) || plugin.getPendingStore().hasPending(uuid);
        if (!onboardingActive) {
            player.sendMessage(ChatColor.GRAY + "You do not need to accept the rules.");
            return true;
        }

        // Only allow when rules sequence has enabled /accept
        boolean allowedNow = plugin.canAcceptRules.getOrDefault(uuid, false);
        if (!allowedNow) {
            player.sendMessage(ChatColor.RED + "You are not allowed to use this command yet.");
            return true;
        }

        // ---- spam guard ----
        Boolean was = plugin.acceptInProgress.putIfAbsent(uuid, true);
        if (was != null) {
            player.sendMessage(ChatColor.GRAY + "Processing...");
            return true;
        }

        // Immediately prevent re-running /accept during join sequence
        plugin.canAcceptRules.put(uuid, false);
        plugin.stopCountdown(uuid);
        player.sendActionBar(net.kyori.adventure.text.Component.empty());

        // Clear the long-running prompt title
        player.resetTitle();

        // Start join sequence and send player back to their last location
        plugin.getPlatformScheduler().runEntity(player, () -> plugin.getJoinSequence().start(player));
        return true;
    }
}
