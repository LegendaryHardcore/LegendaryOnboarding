package org.LegendaryHardcore.legendaryonboarding;

import org.LegendaryHardcore.legendaryonboarding.command.PlayerAccept;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerJoin;
import org.LegendaryHardcore.legendaryonboarding.listener.PlayerQuit;
import org.bukkit.plugin.java.JavaPlugin;
import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.Bukkit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Import plugin content
import org.LegendaryHardcore.legendaryonboarding.storage.AcceptedStore;
import org.LegendaryHardcore.legendaryonboarding.storage.PendingStore;

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

    private LuckPerms luckPermsApi;
    public LuckPerms getLuckPermsAPI() {
        return this.luckPermsApi;
    }

    private AcceptedStore acceptedStore;
    public AcceptedStore getAcceptedStore() { return acceptedStore; }

    private PendingStore pendingStore;
    public PendingStore getPendingStore() { return pendingStore; }

    /*
     *  Get instance of LuckPerms API.
     *  @return true if API is found, false if not
     */
    private boolean initLuckPermsAPI() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPermsApi = provider.getProvider();
            return true;
        }
        return false;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!initLuckPermsAPI()) {
            getLogger().severe("Could not find LuckPerms API.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

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