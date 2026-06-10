package org.LegendaryHardcore.legendaryonboarding.platform;

@FunctionalInterface
public interface TaskHandle {
    TaskHandle NO_OP = () -> { };

    void cancel();
}
