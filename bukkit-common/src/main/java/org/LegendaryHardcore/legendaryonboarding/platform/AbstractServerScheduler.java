package org.LegendaryHardcore.legendaryonboarding.platform;

import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public abstract class AbstractServerScheduler implements ServerScheduler {
    protected final Plugin plugin;

    protected AbstractServerScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    protected Runnable guarded(String operation, Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable throwable) {
                plugin.getLogger().log(Level.SEVERE, "Scheduled task failed: " + operation, throwable);
            }
        };
    }
}
