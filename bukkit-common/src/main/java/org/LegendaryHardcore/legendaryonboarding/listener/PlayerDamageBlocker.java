package org.LegendaryHardcore.legendaryonboarding.listener;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.LegendaryHardcore.legendaryonboarding.TextFormatter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PlayerDamageBlocker implements Listener {
    private static final long NOTICE_COOLDOWN_MILLIS = 2_000L;
    private final LegendaryOnboarding plugin;
    private final ConcurrentMap<NoticeKey, Long> lastNotice = new ConcurrentHashMap<>();

    public PlayerDamageBlocker(LegendaryOnboarding plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (plugin.isOnboardingActive(player.getUniqueId())) {
            event.setCancelled(true);
            player.setFireTicks(0);
            player.setFallDistance(0f);
            notifyAttacker(event, player);
        }
    }

    private void notifyAttacker(EntityDamageEvent event, Player onboardingPlayer) {
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) return;

        Player attacker = attackingPlayer(byEntity);
        if (attacker == null || attacker.getUniqueId().equals(onboardingPlayer.getUniqueId())) {
            return;
        }

        String configured = plugin.getConfigData().getOnboardingDamageMessage();
        if (configured == null || configured.isBlank()) return;
        if (!shouldSendNotice(attacker.getUniqueId(), onboardingPlayer.getUniqueId())) return;
        attacker.sendMessage(TextFormatter.formatChat(
                configured.replace("{player}", onboardingPlayer.getName())
        ));
    }

    private boolean shouldSendNotice(UUID attacker, UUID target) {
        long now = System.currentTimeMillis();
        NoticeKey key = new NoticeKey(attacker, target);
        AtomicBoolean allowed = new AtomicBoolean(false);
        lastNotice.compute(key, (ignored, previous) -> {
            if (previous == null || now - previous >= NOTICE_COOLDOWN_MILLIS) {
                allowed.set(true);
                return now;
            }
            return previous;
        });
        if (lastNotice.size() > 1_000) {
            lastNotice.entrySet().removeIf(entry ->
                    now - entry.getValue() >= NOTICE_COOLDOWN_MILLIS
            );
        }
        return allowed.get();
    }

    static Player attackingPlayer(EntityDamageByEntityEvent event) {
        return PlayerInteractionBlocker.playerResponsibleFor(event.getDamager());
    }

    private record NoticeKey(UUID attacker, UUID target) {
    }
}
