package org.LegendaryHardcore.legendaryonboarding;

// Import plugin content
import org.LegendaryHardcore.legendaryonboarding.storage.AcceptedStore;
import org.LegendaryHardcore.legendaryonboarding.storage.PendingStore;
import org.LegendaryHardcore.legendaryonboarding.command.PlayerAccept;
import org.LegendaryHardcore.legendaryonboarding.command.ReloadCommand;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerJoin;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerQuit;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerChatBlocker;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerCommandBlocker;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerDamageBlocker;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerAdvancementBlocker;
import org.LegendaryHardcore.legendaryonboarding.platform.ServerScheduler;
import org.LegendaryHardcore.legendaryonboarding.platform.TaskHandle;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/*
 *   Main Onboard plugin class
 */
public abstract class LegendaryOnboarding extends JavaPlugin {

    // Track whether each player can use /accept
    public final Map<UUID, Boolean> canAcceptRules = new ConcurrentHashMap<>();

    private RulesSequence rulesSequence;
    public RulesSequence getRulesSequence() {
        return this.rulesSequence;
    }

    private JoinSequence joinSequence;
    public JoinSequence getJoinSequence() {
        return this.joinSequence;
    }

    private ConfigData config;
    public ConfigData getConfigData() {
        return this.config;
    }

    private AcceptedStore acceptedStore;
    public AcceptedStore getAcceptedStore() { return acceptedStore; }

    private PendingStore pendingStore;
    public PendingStore getPendingStore() { return pendingStore; }

    public final ConcurrentMap<UUID, TaskHandle> movementLocks = new ConcurrentHashMap<>();
    public final ConcurrentMap<UUID, TaskHandle> countdownTasks = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<UUID, Boolean> acceptInProgress = new ConcurrentHashMap<>();
    public final java.util.Set<UUID> joinSequenceActive = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public final java.util.Set<UUID> debugForcedPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public final ConcurrentMap<UUID, java.util.Set<PotionEffectType>> sequencePotionEffects =
            new ConcurrentHashMap<>();

    private ServerScheduler platformScheduler;

    protected abstract ServerScheduler createPlatformScheduler();

    public ServerScheduler getPlatformScheduler() {
        return platformScheduler;
    }

    @Override
    public void onEnable() {
        platformScheduler = createPlatformScheduler();
        saveDefaultConfig();
        if (!reloadPluginConfig()) {
            getLogger().severe("Could not load config.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize storage classes
        this.acceptedStore = new AcceptedStore(this);
        this.pendingStore = new PendingStore(this);

        this.acceptedStore.load();
        this.pendingStore.load();

        // Initialize sequences classes
        this.rulesSequence = new RulesSequence(this);
        this.joinSequence = new JoinSequence(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerJoin(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuit(this), this);
        getServer().getPluginManager().registerEvents(new PlayerChatBlocker(this), this);
        getServer().getPluginManager().registerEvents(new PlayerCommandBlocker(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDamageBlocker(this), this);
        getServer().getPluginManager().registerEvents(new PlayerAdvancementBlocker(this), this);

        var acceptCmd = getCommand("accept");
        if (acceptCmd == null) {
            getLogger().severe("Command 'accept' is missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        acceptCmd.setExecutor(new PlayerAccept(this));

        var reloadCmd = getCommand("legendaryonboarding");
        if (reloadCmd == null) {
            getLogger().severe("Command 'legendaryonboarding' is missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        var reloadCommand = new ReloadCommand(this);
        reloadCmd.setExecutor(reloadCommand);
        reloadCmd.setTabCompleter(reloadCommand);

        getLogger().info("LegendaryOnboarding enabled.");
    }

    @Override
    public void onDisable() {
        canAcceptRules.clear();
        acceptInProgress.clear();
        joinSequenceActive.clear();
        debugForcedPlayers.clear();
        sequencePotionEffects.clear();
        countdownTasks.values().forEach(TaskHandle::cancel);
        countdownTasks.clear();
        movementLocks.clear();

        if (pendingStore != null) pendingStore.flushNow();
        if (acceptedStore != null) acceptedStore.flushNow();
    }


    public void startMovementLock(Player player, Location anchor, boolean lockView) {
        final UUID uuid = player.getUniqueId();

        // cancel old lock if any
        TaskHandle old = movementLocks.remove(uuid);
        if (old != null) old.cancel();

        final Location fixed = anchor.clone();
        fixed.setX(fixed.getBlockX() + 0.5);
        fixed.setZ(fixed.getBlockZ() + 0.5);

        TaskHandle task = platformScheduler.runEntityAtFixedRate(player, () -> {
            if (!player.isOnline()) {
                stopMovementLock(uuid);
                return;
            }

            // stop movement
            player.setVelocity(player.getVelocity().zero());
            player.setFallDistance(0f);

            Location cur = player.getLocation();
            if (cur.getWorld() != fixed.getWorld()) {
                player.teleportAsync(fixed);
                return;
            }

            // snap if drifted
            double dx = cur.getX() - fixed.getX();
            double dy = cur.getY() - fixed.getY();
            double dz = cur.getZ() - fixed.getZ();

            if (Math.abs(dx) > 0.05 || Math.abs(dy) > 0.05 || Math.abs(dz) > 0.05) {
                Location snap = fixed.clone();
                if (lockView) {
                    snap.setYaw(fixed.getYaw());
                    snap.setPitch(fixed.getPitch());
                } else {
                    snap.setYaw(cur.getYaw());
                    snap.setPitch(cur.getPitch());
                }
                player.teleportAsync(snap);
                return;
            }

            // lock view even if no drift (prevents looking around)
            if (lockView) {
                float yaw = fixed.getYaw();
                float pitch = fixed.getPitch();

                // only adjust if changed a bit, avoids extra work
                if (Math.abs(cur.getYaw() - yaw) > 2.0f || Math.abs(cur.getPitch() - pitch) > 2.0f) {
                    Location snap = cur.clone();
                    snap.setYaw(yaw);
                    snap.setPitch(pitch);
                    player.teleportAsync(snap);
                }
            }
        }, () -> movementLocks.remove(uuid), 1L, 2L);

        movementLocks.put(uuid, task);
    }

    public void stopMovementLock(UUID uuid) {
        TaskHandle task = movementLocks.remove(uuid);
        if (task != null) task.cancel();
    }

    public void setCountdownTask(UUID uuid, TaskHandle task) {
        TaskHandle old = countdownTasks.put(uuid, task);
        if (old != null) old.cancel();
    }

    public void stopCountdown(UUID uuid) {
        TaskHandle task = countdownTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    public boolean isOnboardingActive(UUID uuid) {
        return canAcceptRules.containsKey(uuid)
                || joinSequenceActive.contains(uuid)
                || (pendingStore != null && pendingStore.hasPending(uuid));
    }

    public void trackSequencePotionEffect(UUID uuid, PotionEffectType type) {
        sequencePotionEffects
                .computeIfAbsent(uuid, ignored -> ConcurrentHashMap.newKeySet())
                .add(type);
    }

    public void untrackSequencePotionEffect(UUID uuid, PotionEffectType type) {
        var effects = sequencePotionEffects.get(uuid);
        if (effects == null) return;
        effects.remove(type);
        if (effects.isEmpty()) sequencePotionEffects.remove(uuid);
    }

    public void clearSequencePotionEffects(Player player) {
        var effects = sequencePotionEffects.remove(player.getUniqueId());
        if (effects == null) return;
        effects.forEach(player::removePotionEffect);
    }

    public boolean reloadPluginConfig() {
        var titleSequence = new TitleSequenceConfig(this).updateAndLoad();
        if (titleSequence == null || !new ConfigUpdater(this).update()) {
            return false;
        }

        reloadConfig();
        ConfigData loaded = new LoadConfig(this, titleSequence).load();
        if (loaded == null) {
            return false;
        }

        boolean wasEnabled = config != null && config.isTitleSequenceEnabled();
        boolean wasDebugEnabled = config != null && config.isDebugForceOnboarding();
        this.config = loaded;
        if (wasEnabled && !loaded.isTitleSequenceEnabled()) {
            releaseActivePlayers(java.util.Set.copyOf(canAcceptRules.keySet()));
        } else if (wasDebugEnabled && !loaded.isDebugForceOnboarding()) {
            releaseActivePlayers(java.util.Set.copyOf(debugForcedPlayers));
        }
        return true;
    }

    private void releaseActivePlayers(java.util.Set<UUID> players) {
        for (UUID uuid : players) {
            Player player = getServer().getPlayer(uuid);
            if (player == null) {
                if (pendingStore != null) pendingStore.clearPending(uuid);
                clearRuntimeState(uuid);
                continue;
            }

            platformScheduler.runEntity(player, () -> {
                player.resetTitle();
                player.sendActionBar(net.kyori.adventure.text.Component.empty());
                clearSequencePotionEffects(player);
                player.setGameMode(GameMode.SURVIVAL);
                player.setInvisible(false);
                player.setInvulnerable(false);
                player.setAllowFlight(false);
                player.setFlying(false);
                player.setFallDistance(0f);
                stopMovementLock(uuid);

                Location pending = pendingStore.getPendingLocation(uuid);
                if (pending != null) {
                    player.teleportAsync(pending);
                }
                pendingStore.clearPending(uuid);
                clearRuntimeState(uuid);
            });
        }
    }

    private void clearRuntimeState(UUID uuid) {
        canAcceptRules.remove(uuid);
        acceptInProgress.remove(uuid);
        joinSequenceActive.remove(uuid);
        debugForcedPlayers.remove(uuid);
        sequencePotionEffects.remove(uuid);
        stopCountdown(uuid);
        stopMovementLock(uuid);
    }
}
