package org.LegendaryHardcore.legendaryonboarding.platform;

import org.bukkit.entity.Entity;
import org.bukkit.Location;

import java.util.concurrent.TimeUnit;

public interface ServerScheduler {
    TaskHandle runEntity(Entity entity, Runnable task);

    TaskHandle runAtLocation(Location location, Runnable task);

    TaskHandle runEntityDelayed(Entity entity, Runnable task, Runnable retired, long delayTicks);

    default TaskHandle runEntityDelayed(Entity entity, Runnable task, long delayTicks) {
        return runEntityDelayed(entity, task, () -> { }, delayTicks);
    }

    TaskHandle runEntityAtFixedRate(
            Entity entity,
            Runnable task,
            Runnable retired,
            long initialDelayTicks,
            long periodTicks
    );

    TaskHandle runAsyncDelayed(Runnable task, long delay, TimeUnit unit);
}
