package org.LegendaryHardcore.legendaryonboarding.command;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;

public final class ReloadCommand implements CommandExecutor, TabCompleter {
    private static final String RELOAD_PERMISSION = "legendaryonboarding.reload";
    private static final String STATUS_PERMISSION = "legendaryonboarding.status";
    private static final String DEBUG_PERMISSION = "legendaryonboarding.debug";
    private static final List<HelpEntry> HELP_ENTRIES = List.of(
            new HelpEntry(
                    "/accept",
                    "Accept the server rules after onboarding unlocks",
                    "legendaryonboarding.accept"
            ),
            new HelpEntry(
                    "/lo help",
                    "Show the LegendaryOnboarding command list",
                    null
            ),
            new HelpEntry(
                    "/lo reload",
                    "Reload the plugin configuration",
                    RELOAD_PERMISSION
            ),
            new HelpEntry(
                    "/lo status <playername | UUID>",
                    "Show a player's onboarding and acceptance state",
                    STATUS_PERMISSION
            ),
            new HelpEntry(
                    "/lo debug start <playername | UUID>",
                    "Force an online player into onboarding",
                    DEBUG_PERMISSION
            ),
            new HelpEntry(
                    "/lo debug end <playername | UUID>",
                    "End onboarding or queue offline cleanup",
                    DEBUG_PERMISSION
            ),
            new HelpEntry(
                    "/lo debug fix <playername | UUID>",
                    "Clear leaked onboarding state now or on next login",
                    DEBUG_PERMISSION
            ),
            new HelpEntry(
                    "/lo debug isAccepted <playername | UUID> <true | false | remove>",
                    "Change or remove a player's stored acceptance state",
                    DEBUG_PERMISSION
            )
    );
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
        if (args.length == 0) {
            help(sender);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> help(sender);
            case "reload" -> reload(sender);
            case "status" -> status(sender, args, label);
            case "debug" -> debug(sender, args, label);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand: "
                        + ChatColor.YELLOW + args[0] + ChatColor.GRAY
                        + ". Use /" + label + " help.");
                yield true;
            }
        };
    }

    private boolean help(CommandSender sender) {
        List<HelpEntry> entries = visibleHelpEntries(sender::hasPermission);
        sender.sendMessage(ChatColor.AQUA + "LegendaryOnboarding"
                + ChatColor.GRAY + " - Help");
        for (HelpEntry entry : entries) {
            sender.sendMessage(ChatColor.YELLOW + entry.usage()
                    + ChatColor.DARK_GRAY + " - "
                    + ChatColor.GRAY + entry.description());
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!requirePermission(sender, RELOAD_PERMISSION)) return true;
        if (!plugin.reloadPluginConfig()) {
            sender.sendMessage(ChatColor.RED + "LegendaryOnboarding could not reload. Check the server log.");
            return true;
        }
        sender.sendMessage(ChatColor.GREEN + "LegendaryOnboarding reloaded.");
        return true;
    }

    private boolean status(CommandSender sender, String[] args, String label) {
        if (!requirePermission(sender, STATUS_PERMISSION)) return true;
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " status <playername | UUID>");
            return true;
        }

        PlayerTarget target = resolveTarget(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found in online or stored onboarding data.");
            return true;
        }

        UUID uuid = target.uuid();
        boolean hasEntry = plugin.getAcceptedStore().hasEntry(uuid);
        sender.sendMessage(ChatColor.GOLD + "LegendaryOnboarding status");
        sender.sendMessage(ChatColor.GRAY + "Player: " + ChatColor.WHITE + target.displayName());
        sender.sendMessage(ChatColor.GRAY + "UUID: " + ChatColor.WHITE + uuid);
        sender.sendMessage(ChatColor.GRAY + "Online: " + value(target.player() != null));
        sender.sendMessage(ChatColor.GRAY + "Currently onboarding: "
                + value(plugin.isOnboardingActive(uuid)));
        sender.sendMessage(ChatColor.GRAY + "Pending record: "
                + value(plugin.getPendingStore().hasPending(uuid)));
        sender.sendMessage(ChatColor.GRAY + "Saved return location: "
                + value(plugin.getPendingStore().hasReturnLocation(uuid)));
        sender.sendMessage(ChatColor.GRAY + "Deferred cleanup required: "
                + value(plugin.getPendingStore().isCleanupRequired(uuid)));
        sender.sendMessage(ChatColor.GRAY + "Can use /accept: "
                + value(plugin.canAcceptRules.getOrDefault(uuid, false)));
        sender.sendMessage(ChatColor.GRAY + "Post-accept sequence active: "
                + value(plugin.joinSequenceActive.contains(uuid)));
        sender.sendMessage(ChatColor.GRAY + "Debug forced: "
                + value(plugin.debugForcedPlayers.contains(uuid)
                || plugin.getPendingStore().isDebugSession(uuid)));
        sender.sendMessage(ChatColor.GRAY + "Accepted state: " + ChatColor.WHITE
                + (hasEntry ? plugin.getAcceptedStore().isAccepted(uuid) : "missing"));
        return true;
    }

    private boolean debug(CommandSender sender, String[] args, String label) {
        if (!requirePermission(sender, DEBUG_PERMISSION)) return true;
        if (args.length < 3) {
            sendDebugUsage(sender, label);
            return true;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        PlayerTarget target = resolveTarget(args[2]);
        if (target == null) {
            if (action.equals("fix")) {
                sender.sendMessage(ChatColor.RED
                        + "Could not resolve that player offline. Provide their UUID instead.");
            } else {
                sender.sendMessage(ChatColor.RED
                        + "Player not found in online or stored onboarding data.");
            }
            return true;
        }

        return switch (action) {
            case "start" -> debugStart(sender, target);
            case "end" -> debugEnd(sender, target);
            case "fix" -> debugFix(sender, target);
            case "isaccepted" -> debugAccepted(sender, target, args, label);
            default -> {
                sendDebugUsage(sender, label);
                yield true;
            }
        };
    }

    private boolean debugStart(CommandSender sender, PlayerTarget target) {
        Player player = target.player();
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Debug start requires an online player.");
            return true;
        }
        if (!canDebugStart(plugin.isOnboardingActive(target.uuid()))) {
            sender.sendMessage(ChatColor.RED
                    + target.displayName() + " is already onboarding.");
            return true;
        }
        if (!plugin.getConfigData().isTitleSequenceEnabled()) {
            sender.sendMessage(ChatColor.RED
                    + "The title sequence is disabled. Enable it before using debug start.");
            return true;
        }

        plugin.getPlatformScheduler().runEntity(player, () -> {
            if (!player.isOnline() || plugin.isOnboardingActive(target.uuid())) {
                return;
            }
            plugin.startOnboarding(player, false, true);
        });
        sender.sendMessage(ChatColor.GREEN + "Started onboarding for " + target.displayName() + ".");
        return true;
    }

    private boolean debugEnd(CommandSender sender, PlayerTarget target) {
        if (!canDebugEnd(plugin.isOnboardingActive(target.uuid()))) {
            sender.sendMessage(ChatColor.RED
                    + target.displayName() + " is not currently onboarding.");
            return true;
        }
        boolean immediate = plugin.endOnboarding(target.uuid());
        if (immediate) {
            sender.sendMessage(ChatColor.GREEN
                    + "Ended onboarding for " + target.displayName() + ".");
        } else {
            sender.sendMessage(ChatColor.YELLOW
                    + "Queued full onboarding cleanup for " + target.displayName()
                    + " on their next login.");
        }
        return true;
    }

    private boolean debugFix(CommandSender sender, PlayerTarget target) {
        Player player = target.player();
        if (player != null) {
            plugin.getPlatformScheduler().runEntity(player, () -> {
                if (player.isOnline()) {
                    plugin.applyDebugFix(player);
                }
            });
            sender.sendMessage(ChatColor.GREEN
                    + "Cleared leaked onboarding state for "
                    + target.displayName() + ".");
            return true;
        }

        plugin.queueDeferredDebugFix(target.uuid());
        sender.sendMessage(ChatColor.YELLOW
                + "Queued a deferred onboarding-state fix for "
                + target.displayName() + " on their next login.");
        return true;
    }

    static boolean canDebugStart(boolean onboardingActive) {
        return !onboardingActive;
    }

    static boolean canDebugEnd(boolean onboardingActive) {
        return onboardingActive;
    }

    private boolean debugAccepted(
            CommandSender sender,
            PlayerTarget target,
            String[] args,
            String label
    ) {
        if (args.length != 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label
                    + " debug isAccepted <playername | UUID> <true | false | remove>");
            return true;
        }

        switch (args[3].toLowerCase(Locale.ROOT)) {
            case "true" -> plugin.getAcceptedStore().setAccepted(
                    target.uuid(), target.displayName(), true
            );
            case "false" -> plugin.getAcceptedStore().setAccepted(
                    target.uuid(), target.displayName(), false
            );
            case "remove" -> plugin.getAcceptedStore().clear(target.uuid());
            default -> {
                sender.sendMessage(ChatColor.RED + "Accepted state must be true, false, or remove.");
                return true;
            }
        }

        sender.sendMessage(ChatColor.GREEN + "Updated accepted state for "
                + target.displayName() + " to " + args[3].toLowerCase(Locale.ROOT) + ".");
        return true;
    }

    private PlayerTarget resolveTarget(String input) {
        UUID uuid = parseUuid(input);
        if (uuid != null) {
            Player online = plugin.getServer().getPlayer(uuid);
            OfflinePlayer offline = plugin.getServer().getOfflinePlayer(uuid);
            String name = online != null ? online.getName() : offline.getName();
            return new PlayerTarget(uuid, name == null ? uuid.toString() : name, online);
        }

        Player online = plugin.getServer().getPlayerExact(input);
        if (online != null) {
            return new PlayerTarget(online.getUniqueId(), online.getName(), online);
        }

        UUID storedUuid = plugin.getAcceptedStore().findByName(input);
        if (storedUuid != null) {
            Player storedOnline = plugin.getServer().getPlayer(storedUuid);
            return new PlayerTarget(storedUuid, input, storedOnline);
        }

        OfflinePlayer cached = plugin.getServer().getOfflinePlayerIfCached(input);
        if (cached != null) {
            return new PlayerTarget(cached.getUniqueId(), input, cached.getPlayer());
        }
        return null;
    }

    private static UUID parseUuid(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean requirePermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
        return false;
    }

    private static String value(boolean value) {
        return (value ? ChatColor.GREEN : ChatColor.RED) + Boolean.toString(value);
    }

    private static void sendDebugUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.RED + "Usage: /" + label
                + " debug <start | end | fix> <playername | UUID>");
        sender.sendMessage(ChatColor.RED + "Usage: /" + label
                + " debug isAccepted <playername | UUID> <true | false | remove>");
    }

    @Override
    public @NotNull List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("help"));
            if (sender.hasPermission(RELOAD_PERMISSION)) options.add("reload");
            if (sender.hasPermission(STATUS_PERMISSION)) options.add("status");
            if (sender.hasPermission(DEBUG_PERMISSION)) options.add("debug");
            return matching(options, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")
                && sender.hasPermission(DEBUG_PERMISSION)) {
            return matching(List.of("start", "end", "fix", "isAccepted"), args[1]);
        }
        if ((args.length == 2 && args[0].equalsIgnoreCase("status")
                && sender.hasPermission(STATUS_PERMISSION))
                || (args.length == 3 && args[0].equalsIgnoreCase("debug")
                && sender.hasPermission(DEBUG_PERMISSION))) {
            return matching(
                    plugin.getServer().getOnlinePlayers().stream()
                            .map(Player::getName)
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .toList(),
                    args[args.length - 1]
            );
        }
        if (args.length == 4
                && args[0].equalsIgnoreCase("debug")
                && args[1].equalsIgnoreCase("isAccepted")
                && sender.hasPermission(DEBUG_PERMISSION)) {
            return matching(List.of("true", "false", "remove"), args[3]);
        }
        return List.of();
    }

    static List<String> completeFirstArgument(String input) {
        return matching(List.of("help", "reload", "status", "debug"), input);
    }

    static List<HelpEntry> visibleHelpEntries(Predicate<String> hasPermission) {
        return HELP_ENTRIES.stream()
                .filter(entry -> entry.permission() == null
                        || hasPermission.test(entry.permission()))
                .toList();
    }

    static List<String> matching(List<String> options, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }

    private record PlayerTarget(UUID uuid, String displayName, Player player) {
    }

    record HelpEntry(String usage, String description, String permission) {
    }
}
