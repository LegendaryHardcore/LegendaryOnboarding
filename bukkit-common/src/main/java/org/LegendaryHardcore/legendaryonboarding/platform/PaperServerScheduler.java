package org.LegendaryHardcore.legendaryonboarding.platform;

import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public final class PaperServerScheduler extends AbstractServerScheduler {
    public PaperServerScheduler(Plugin plugin) {
        super(plugin);
    }

    @Override
    public TaskHandle runEntity(Entity entity, Runnable task) {
        return plugin.getServer().getScheduler()
                .runTask(plugin, guarded("entity task", task))::cancel;
    }

    @Override
    public TaskHandle runAtLocation(Location location, Runnable task) {
        return plugin.getServer().getScheduler()
                .runTask(plugin, guarded("location task", task))::cancel;
    }

    @Override
    public TaskHandle runEntityDelayed(Entity entity, Runnable task, Runnable retired, long delayTicks) {
        return plugin.getServer().getScheduler()
                .runTaskLater(plugin, guarded("delayed entity task", task), minOne(delayTicks))::cancel;
    }

    @Override
    public TaskHandle runEntityAtFixedRate(
            Entity entity,
            Runnable task,
            Runnable retired,
            long initialDelayTicks,
            long periodTicks
    ) {
        return plugin.getServer().getScheduler()
                .runTaskTimer(
                        plugin,
                        guarded("repeating entity task", task),
                        minOne(initialDelayTicks),
                        minOne(periodTicks)
                )::cancel;
    }

    @Override
    public TaskHandle runAsyncDelayed(Runnable task, long delay, TimeUnit unit) {
        long delayTicks = Math.max(1L, (unit.toMillis(delay) + 49L) / 50L);
        return plugin.getServer().getScheduler()
                .runTaskLaterAsynchronously(
                        plugin,
                        guarded("delayed async task", task),
                        delayTicks
                )::cancel;
    }

    private static long minOne(long ticks) {
        return Math.max(1L, ticks);
    }
}
