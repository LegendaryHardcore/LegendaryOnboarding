package org.LegendaryHardcore.legendaryonboarding;

import org.LegendaryHardcore.legendaryonboarding.ConfigData.TitleContent;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.UUID;

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
        final UUID uuid = player.getUniqueId();

        // Ensure that player does not run /accept before all rules have been displayed
        plugin.canAcceptRules.put(uuid, false);

        // Freeze player in config specified gamemode at location of onboarding
        Location onboardLocation = plugin.getConfigData().getOnboardLocation().toLocation(player.getWorld());

        // Apply freeze effects 1 tick later
        scheduleFreezeAndTeleport(player, onboardLocation, 1L);

        long t = 5L;
        t = scheduleWelcome(player, t);
        t = scheduleRules(player, t);
        schedulePromptAccept(player, t);
    }

    private void scheduleFreezeAndTeleport(Player player, Location loc, long delayTicks) {
        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline()) return;

            player.teleportAsync(loc).thenAccept(success -> {
                if (!success || !player.isOnline()) return;

                applyOnboardingFreeze(player);

                startMovementLock(player, loc);
            });
        }, null, delayTicks);
    }

    private void applyOnboardingFreeze(Player player) {
        switch (plugin.getConfigData().getOnboardGamemode().toUpperCase()) {
            case "CREATIVE" -> player.setGameMode(GameMode.CREATIVE);
            case "ADVENTURE" -> player.setGameMode(GameMode.ADVENTURE);
            case "SPECTATOR" -> player.setGameMode(GameMode.SPECTATOR);
            default -> player.setGameMode(GameMode.SURVIVAL);
        }
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS,
                Integer.MAX_VALUE,
                1,
                false,
                false
        ));
        player.setInvisible(true);
        player.setInvulnerable(true);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFallDistance(0f);
    }

    private long scheduleWelcome(Player player, long startTick) {
        // schedule immediately at startTick
        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline()) return;

            String serverName = plugin.getConfigData().getServerName();
            String playerName = player.getName();
            String welcomeMessage = "Welcome to " + serverName + ", " + playerName + "!";

            player.sendTitle(welcomeMessage, "", 20, 60, 20);
        }, null, startTick);

        // return next available tick; your welcome title is (20+60+20)=100 ticks if you want spacing
        return Math.max(startTick, 0L) + 100L;
    }

    private long scheduleRules(Player player, long startTick) {
        List<TitleContent> contents = plugin.getConfigData().getRulesSequenceContent();
        int duration = plugin.getConfigData().getRulesSequenceDuration() * TPS;
        int fadeIn = plugin.getConfigData().getRulesSequenceFadeIn() * TPS;
        int fadeOut = plugin.getConfigData().getRulesSequenceFadeOut() * TPS;

        long t = startTick;
        long per = (long) duration + fadeIn + fadeOut;

        for (TitleContent c : contents) {
            long scheduled = t;
            player.getScheduler().runDelayed(plugin, task -> {
                if (!player.isOnline()) return;
                player.sendTitle(c.title(), c.subtitle(), fadeIn, duration, fadeOut);
            }, null, scheduled);

            t += per;
        }

        return t;
    }

    private void schedulePromptAccept(Player player, long startTick) {
        UUID uuid = player.getUniqueId();

        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline()) return;

            plugin.canAcceptRules.put(uuid, true);

            for (TitleContent prompt : plugin.getConfigData().getPromptAccept()) {
                player.sendTitle(prompt.title(), prompt.subtitle(), 20, 9999, 0);
            }

            player.sendMessage(plugin.getConfigData().getPromptChat());
        }, null, startTick);
    }

    private void startMovementLock(Player player, Location anchor) {
        final UUID uuid = player.getUniqueId();

        // cancel any old lock just in case
        ScheduledTask old = plugin.movementLocks.remove(uuid);
        if (old != null) old.cancel();

        Location fixed = anchor.clone();
        fixed.setX(fixed.getBlockX() + 0.5);
        fixed.setZ(fixed.getBlockZ() + 0.5);

        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, t -> {
            if (!player.isOnline()) {
                t.cancel();
                plugin.movementLocks.remove(uuid);
                return;
            }

            // hard stop movement
            player.setVelocity(player.getVelocity().zero());

            // if they drifted at all, snap back
            Location cur = player.getLocation();
            if (cur.getWorld() != fixed.getWorld()) {
                player.teleportAsync(fixed);
                return;
            }

            double dx = cur.getX() - fixed.getX();
            double dy = cur.getY() - fixed.getY();
            double dz = cur.getZ() - fixed.getZ();

            // tiny tolerance to avoid micro jitter
            if (Math.abs(dx) > 0.05 || Math.abs(dy) > 0.05 || Math.abs(dz) > 0.05) {
                player.teleportAsync(fixed);
            }
        }, null, 1L, 2L); // start after 1 tick, repeat every 2 ticks

        plugin.movementLocks.put(uuid, task);
    }


}