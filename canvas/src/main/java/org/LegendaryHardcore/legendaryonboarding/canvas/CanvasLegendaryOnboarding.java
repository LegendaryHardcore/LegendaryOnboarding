package org.LegendaryHardcore.legendaryonboarding.canvas;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.LegendaryHardcore.legendaryonboarding.platform.RegionizedServerScheduler;
import org.LegendaryHardcore.legendaryonboarding.platform.ServerScheduler;

public final class CanvasLegendaryOnboarding extends LegendaryOnboarding {
    @Override
    protected ServerScheduler createPlatformScheduler() {
        return new RegionizedServerScheduler(this);
    }
}
