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

        // Initalise sequences classes
        this.rulesSequence = new RulesSequence(this);
        this.joinSequence = new JoinSequence(this);

        // Register PlayerJoin and PlayerQuit listeners
        getServer().getPluginManager().registerEvents(new PlayerJoin(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuit(this), this);

        getCommand("accept").setExecutor(new PlayerAccept(this));
        getLogger().info("Onboard enabled.");
    }

    @Override
    public void onDisable() {
        canAcceptRules.clear();
    }

}