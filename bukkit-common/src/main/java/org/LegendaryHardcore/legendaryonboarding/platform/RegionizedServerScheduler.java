package org.LegendaryHardcore.legendaryonboarding.platform;

import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public final class RegionizedServerScheduler extends AbstractServerScheduler {
    public RegionizedServerScheduler(Plugin plugin) {
        super(plugin);
    }

    @Override
    public TaskHandle runEntity(Entity entity, Runnable task) {
        var scheduledTask = entity.getScheduler().run(
                plugin,
                ignored -> guarded("entity task", task).run(),
                null
        );
        return scheduledTask == null ? TaskHandle.NO_OP : scheduledTask::cancel;
    }

    @Override
    public TaskHandle runAtLocation(Location location, Runnable task) {
        return plugin.getServer().getRegionScheduler().run(
                plugin,
                location,
                ignored -> guarded("location task", task).run()
        )::cancel;
    }

    @Override
    public TaskHandle runEntityDelayed(Entity entity, Runnable task, Runnable retired, long delayTicks) {
        var scheduledTask = entity.getScheduler().runDelayed(
                plugin,
                ignored -> guarded("delayed entity task", task).run(),
                guarded("retired delayed entity task", retired),
                Math.max(1L, delayTicks)
        );
        return scheduledTask == null ? TaskHandle.NO_OP : scheduledTask::cancel;
    }

    @Override
    public TaskHandle runEntityAtFixedRate(
            Entity entity,
            Runnable task,
            Runnable retired,
            long initialDelayTicks,
            long periodTicks
    ) {
        var scheduledTask = entity.getScheduler().runAtFixedRate(
                plugin,
                ignored -> guarded("repeating entity task", task).run(),
                guarded("retired repeating entity task", retired),
                Math.max(1L, initialDelayTicks),
                Math.max(1L, periodTicks)
        );
        return scheduledTask == null ? TaskHandle.NO_OP : scheduledTask::cancel;
    }

    @Override
    public TaskHandle runAsyncDelayed(Runnable task, long delay, TimeUnit unit) {
        return plugin.getServer().getAsyncScheduler().runDelayed(
                plugin,
                ignored -> guarded("delayed async task", task).run(),
                delay,
                unit
        )::cancel;
    }
}
