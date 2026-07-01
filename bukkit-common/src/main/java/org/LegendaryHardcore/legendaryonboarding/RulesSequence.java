package org.LegendaryHardcore.legendaryonboarding;

import org.LegendaryHardcore.legendaryonboarding.ConfigData.TitleContent;

import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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
        if (!plugin.getConfigData().isTitleSequenceEnabled()) return;

        final UUID uuid = player.getUniqueId();

        // Ensure that player does not run /accept before all rules have been displayed
        plugin.canAcceptRules.put(uuid, false);

        // Determine where the intro should happen
        final Location anchor;
        if (plugin.getConfigData().isOnboardTeleport()) {
            anchor = plugin.getConfigData().getOnboardLocation().toLocation();
            if (anchor == null) {
                plugin.getLogger().severe(
                        "No loaded onboarding world matches POS_WORLD or POS_WORLD_TYPE."
                );
                plugin.endOnboarding(uuid);
                return;
            }
        } else {
            // "in place" intro: use where they are right now
            anchor = player.getLocation().clone();
        }

        // Teleport only if enabled, otherwise just freeze where the player is
        if (plugin.getConfigData().isOnboardTeleport()) {
            scheduleFreezeAndTeleport(player, anchor, 1L);
        } else {
            // Freeze without teleport but still schedule 1 tick later for consistency
            plugin.getPlatformScheduler().runEntityDelayed(player, () -> {
                if (!isActive(player)) return;
                applyOnboardingFreeze(player);
                plugin.startMovementLock(player, anchor, true);
            }, 1L);
        }

        long t = 5L;
        t = scheduleWelcome(player, t);
        t = scheduleRules(player, t);
        startCountdown(player, t);
        schedulePromptAccept(player, t);
    }

    private void scheduleFreezeAndTeleport(Player player, Location loc, long delayTicks) {
        plugin.getPlatformScheduler().runEntityDelayed(player, () -> {
            if (!isActive(player)) return;

            player.teleportAsync(loc).whenComplete((success, error) -> {
                plugin.getPlatformScheduler().runEntity(player, () -> {
                    if (!isActive(player)) return;
                    if (error != null) {
                        plugin.getLogger().warning(
                                "Onboarding teleport failed for " + player.getName() + ": " + error.getMessage()
                        );
                        return;
                    }
                    if (!Boolean.TRUE.equals(success) || !player.isOnline()) return;

                    applyOnboardingFreeze(player);
                    plugin.startMovementLock(player, loc, true);
                });
            });
        }, delayTicks);
    }

    private void applyOnboardingFreeze(Player player) {
        switch (plugin.getConfigData().getOnboardGamemode().toUpperCase()) {
            case "CREATIVE" -> player.setGameMode(GameMode.CREATIVE);
            case "ADVENTURE" -> player.setGameMode(GameMode.ADVENTURE);
            case "SPECTATOR" -> player.setGameMode(GameMode.SPECTATOR);
            default -> player.setGameMode(GameMode.SURVIVAL);
        }
        player.setInvisible(true);
        player.setInvulnerable(true);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFallDistance(0f);
    }

    private long scheduleWelcome(Player player, long startTick) {
        long t = startTick;
        for (TitleContent content : plugin.getConfigData().getWelcomeSequenceContent()) {
            long scheduled = t;
            plugin.getPlatformScheduler().runEntityDelayed(player, () -> {
                if (!isActive(player)) return;
                showStep(plugin, player, applyPlaceholders(
                        content,
                        plugin.getConfigData().getServerName(),
                        player.getName()
                ));
            }, scheduled);
            t += stepTicks(content);
        }
        return t;
    }

    private long scheduleRules(Player player, long startTick) {
        List<TitleContent> contents = plugin.getConfigData().getRulesSequenceContent();

        long t = startTick;

        for (TitleContent c : contents) {
            long scheduled = t;
            plugin.getPlatformScheduler().runEntityDelayed(player, () -> {
                if (!isActive(player)) return;
                showStep(plugin, player, c);
            }, scheduled);

            t += stepTicks(c);
        }

        return t;
    }

    private void schedulePromptAccept(Player player, long startTick) {
        UUID uuid = player.getUniqueId();

        plugin.getPlatformScheduler().runEntityDelayed(player, () -> {
            if (!isActive(player)) return;

            plugin.stopCountdown(uuid);
            player.sendActionBar(net.kyori.adventure.text.Component.empty());
            plugin.canAcceptRules.put(uuid, true);

            for (TitleContent prompt : plugin.getConfigData().getPromptAccept()) {
                showStep(plugin, player, prompt);
            }
        }, startTick);
    }

    private void startCountdown(Player player, long totalTicks) {
        String message = plugin.getConfigData().getActionBarCountdown();
        if (message == null || message.isBlank() || totalTicks <= 0L) {
            return;
        }

        UUID uuid = player.getUniqueId();
        AtomicInteger seconds = new AtomicInteger(countdownSeconds(totalTicks));
        var task = plugin.getPlatformScheduler().runEntityAtFixedRate(
                player,
                () -> {
                    int remaining = seconds.getAndDecrement();
                    if (!isActive(player) || remaining <= 0) {
                        plugin.stopCountdown(uuid);
                        player.sendActionBar(net.kyori.adventure.text.Component.empty());
                        return;
                    }
                    player.sendActionBar(TextFormatter.format(
                            applyCountdownPlaceholder(message, remaining)
                    ));
                },
                () -> plugin.countdownTasks.remove(uuid),
                1L,
                TPS
        );
        plugin.setCountdownTask(uuid, task);
    }

    static void showStep(LegendaryOnboarding plugin, Player player, TitleContent content) {
        if (content.stopAllSounds()) {
            player.stopAllSounds();
        }

        if (!content.title().isEmpty() || !content.subtitle().isEmpty()) {
            showTitle(
                    player,
                    content.title(),
                    content.subtitle(),
                    content.fadeIn() * TPS,
                    content.duration() * TPS,
                    content.fadeOut() * TPS
            );
        }
        if (!content.chat().isEmpty()) {
            player.sendMessage(TextFormatter.formatChat(content.chat()));
        }
        if (content.sound() != null) {
            player.playSound(
                    player.getLocation(),
                    content.sound().name(),
                    content.sound().category(),
                    content.sound().volume(),
                    content.sound().pitch()
            );
        }

        for (String effectName : content.removePotionEffects()) {
            PotionEffectType type = potionEffectType(effectName);
            if (type == null) {
                plugin.getLogger().warning("Unknown potion effect in sequence: " + effectName);
                continue;
            }
            player.removePotionEffect(type);
            plugin.untrackSequencePotionEffect(player.getUniqueId(), type);
        }

        for (ConfigData.PotionEffectConfig effect : content.potionEffects()) {
            PotionEffectType type = potionEffectType(effect.type());
            if (type == null) {
                plugin.getLogger().warning("Unknown potion effect in sequence: " + effect.type());
                continue;
            }
            int durationTicks = effect.duration() < 0
                    ? Integer.MAX_VALUE
                    : Math.max(1, effect.duration() * TPS);
            player.addPotionEffect(new PotionEffect(
                    type,
                    durationTicks,
                    effect.amplifier(),
                    effect.ambient(),
                    effect.particles(),
                    effect.icon()
            ), true);
            plugin.trackSequencePotionEffect(player.getUniqueId(), type);
        }
    }

    static long stepTicks(TitleContent content) {
        return (long) (content.fadeIn() + content.duration() + content.fadeOut()) * TPS;
    }

    static int countdownSeconds(long ticks) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (ticks + TPS - 1L) / TPS));
    }

    static String applyCountdownPlaceholder(String message, int seconds) {
        return message.replace("{seconds}", Integer.toString(seconds));
    }

    static TitleContent applyPlaceholders(
            TitleContent content,
            String serverName,
            String playerName
    ) {
        return new TitleContent(
                applyPlaceholders(content.title(), serverName, playerName),
                applyPlaceholders(content.subtitle(), serverName, playerName),
                applyPlaceholders(content.chat(), serverName, playerName),
                content.fadeIn(),
                content.duration(),
                content.fadeOut(),
                content.stopAllSounds(),
                content.sound(),
                content.potionEffects(),
                content.removePotionEffects()
        );
    }

    private static String applyPlaceholders(String text, String serverName, String playerName) {
        return text
                .replace("{server_name}", serverName)
                .replace("{player_name}", playerName);
    }

    static void showTitle(
            Player player,
            String title,
            String subtitle,
            int fadeInTicks,
            int stayTicks,
            int fadeOutTicks
    ) {
        player.showTitle(Title.title(
                TextFormatter.format(title),
                TextFormatter.format(subtitle),
                Title.Times.times(
                        ticks(fadeInTicks),
                        ticks(stayTicks),
                        ticks(fadeOutTicks)
                )
        ));
    }

    private static Duration ticks(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }

    @SuppressWarnings("deprecation")
    static PotionEffectType potionEffectType(String name) {
        String normalized = name.trim();
        int separator = normalized.indexOf(':');
        if (separator >= 0) normalized = normalized.substring(separator + 1);
        return PotionEffectType.getByName(normalized.toUpperCase(java.util.Locale.ROOT));
    }

    private boolean isActive(Player player) {
        return player.isOnline()
                && plugin.getConfigData().isTitleSequenceEnabled()
                && plugin.canAcceptRules.containsKey(player.getUniqueId());
    }

}
