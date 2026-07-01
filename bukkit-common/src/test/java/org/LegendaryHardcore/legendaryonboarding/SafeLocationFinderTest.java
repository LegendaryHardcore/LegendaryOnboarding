package org.LegendaryHardcore.legendaryonboarding;

import org.junit.jupiter.api.Test;
import org.bukkit.Material;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeLocationFinderTest {
    @Test
    void searchesFromDesiredYAndPrefersLowerAtEqualDistance() {
        assertEquals(
                List.of(64, 63, 65, 62, 66),
                SafeLocationFinder.preferredYLevels(-62, 318, 64, 2)
        );
    }

    @Test
    void clampsDesiredYInsideSafeWorldBounds() {
        assertEquals(
                List.of(318, 317, 316),
                SafeLocationFinder.preferredYLevels(-62, 318, 400, 2)
        );
        assertEquals(
                List.of(-62, -61, -60),
                SafeLocationFinder.preferredYLevels(-62, 318, -100, 2)
        );
    }

    @Test
    void fullHeightSearchStillBeginsAtDesiredY() {
        List<Integer> levels = SafeLocationFinder.preferredYLevels(
                -62,
                318,
                80,
                Integer.MAX_VALUE
        );

        assertEquals(80, levels.getFirst());
        assertEquals(381, levels.size());
    }

    @Test
    void recognizesNaturalPreferredSurfaceBlocks() {
        assertTrue(SafeLocationFinder.isPreferredSurface(Material.GRASS_BLOCK));
        assertTrue(SafeLocationFinder.isPreferredSurface(Material.COARSE_DIRT));
        assertTrue(SafeLocationFinder.isPreferredSurface(Material.ROOTED_DIRT));
        assertTrue(SafeLocationFinder.isPreferredSurface(Material.OAK_LEAVES));
        assertTrue(SafeLocationFinder.isPreferredSurface(Material.MANGROVE_LEAVES));
        assertFalse(SafeLocationFinder.isPreferredSurface(Material.STONE));
        assertFalse(SafeLocationFinder.isPreferredSurface(Material.DEEPSLATE));
    }

    @Test
    void reservesSpaceBelowTheBuildCeiling() {
        assertEquals(312, SafeLocationFinder.maximumReturnGroundY(320));
        assertEquals(248, SafeLocationFinder.maximumReturnGroundY(256));
    }

    @Test
    void rejectsSubmergedSpawnSpace() {
        assertTrue(SafeLocationFinder.isDryPassableMaterial(Material.AIR));
        assertFalse(SafeLocationFinder.isDryPassableMaterial(Material.WATER));
        assertFalse(SafeLocationFinder.isDryPassableMaterial(Material.BUBBLE_COLUMN));
        assertFalse(SafeLocationFinder.isDryPassableMaterial(Material.KELP));
        assertFalse(SafeLocationFinder.isDryPassableMaterial(Material.SEAGRASS));
        assertFalse(SafeLocationFinder.isDryPassableMaterial(Material.POWDER_SNOW));
    }
}
