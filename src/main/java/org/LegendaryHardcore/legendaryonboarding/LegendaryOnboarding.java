package org.LegendaryHardcore.legendaryonboarding;

// Import plugin content
import org.LegendaryHardcore.legendaryonboarding.storage.AcceptedStore;
import org.LegendaryHardcore.legendaryonboarding.storage.PendingStore;
import org.LegendaryHardcore.legendaryonboarding.command.PlayerAccept;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerJoin;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerQuit;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerChatBlocker;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerCommandBlocker;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/*
 *   Main Onboard plugin class
 */
public final class LegendaryOnboarding extends JavaPlugin {

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

    public final ConcurrentMap<UUID, ScheduledTask> movementLocks = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<UUID, Boolean> acceptInProgress = new ConcurrentHashMap<>();
    public final java.util.Set<UUID> joinSequenceActive = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.config = new LoadConfig(this).load();
        if (this.config == null) {
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

        var acceptCmd = getCommand("accept");
        if (acceptCmd == null) {
            getLogger().severe("Command 'accept' is missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        acceptCmd.setExecutor(new PlayerAccept(this));

        getLogger().info("LegendaryOnboarding enabled.");
    }

    @Override
    public void onDisable() {
        canAcceptRules.clear();
        acceptInProgress.clear();
        joinSequenceActive.clear();
        movementLocks.clear();

        if (pendingStore != null) pendingStore.flushNow();
        if (acceptedStore != null) acceptedStore.flushNow();
    }


    public void startMovementLock(Player player, Location anchor, boolean lockView) {
        final UUID uuid = player.getUniqueId();

        // cancel old lock if any
        ScheduledTask old = movementLocks.remove(uuid);
        if (old != null) old.cancel();

        final Location fixed = anchor.clone();
        fixed.setX(fixed.getBlockX() + 0.5);
        fixed.setZ(fixed.getBlockZ() + 0.5);

        ScheduledTask task = player.getScheduler().runAtFixedRate(this, t -> {
            if (!player.isOnline()) {
                t.cancel();
                movementLocks.remove(uuid);
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
        }, null, 1L, 2L);

        movementLocks.put(uuid, task);
    }

    public void stopMovementLock(UUID uuid) {
        ScheduledTask task = movementLocks.remove(uuid);
        if (task != null) task.cancel();
    }
}