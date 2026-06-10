package org.LegendaryHardcore.legendaryonboarding.folia;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.LegendaryHardcore.legendaryonboarding.platform.RegionizedServerScheduler;
import org.LegendaryHardcore.legendaryonboarding.platform.ServerScheduler;

public final class FoliaLegendaryOnboarding extends LegendaryOnboarding {
    @Override
    protected ServerScheduler createPlatformScheduler() {
        return new RegionizedServerScheduler(this);
    }
}
