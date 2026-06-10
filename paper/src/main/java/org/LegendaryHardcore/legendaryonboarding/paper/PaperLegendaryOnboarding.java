package org.LegendaryHardcore.legendaryonboarding.paper;

import org.LegendaryHardcore.legendaryonboarding.LegendaryOnboarding;
import org.LegendaryHardcore.legendaryonboarding.platform.PaperServerScheduler;
import org.LegendaryHardcore.legendaryonboarding.platform.ServerScheduler;

public final class PaperLegendaryOnboarding extends LegendaryOnboarding {
    @Override
    protected ServerScheduler createPlatformScheduler() {
        return new PaperServerScheduler(this);
    }
}
