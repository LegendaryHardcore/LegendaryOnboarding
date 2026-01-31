package org.LegendaryHardcore.legendaryonboarding;

// Import plugin content
import org.LegendaryHardcore.legendaryonboarding.storage.AcceptedStore;
import org.LegendaryHardcore.legendaryonboarding.storage.PendingStore;
import org.LegendaryHardcore.legendaryonboarding.command.PlayerAccept;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerJoin;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerQuit;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import org.bukkit.plugin.java.JavaPlugin;

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

        // Register PlayerJoin and PlayerQuit listeners
        getServer().getPluginManager().registerEvents(new PlayerJoin(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuit(this), this);

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

        if (pendingStore != null) pendingStore.flushNow();
        if (acceptedStore != null) acceptedStore.flushNow();
    }


}