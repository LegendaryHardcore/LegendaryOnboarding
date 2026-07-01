package org.LegendaryHardcore.legendaryonboarding;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.HeightMap;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SafeLocationFinder {
    private static final int BUILD_HEIGHT_BUFFER = 8;
    private static final Set<String> PREFERRED_SURFACE_NAMES = Set.of(
            "GRASS_BLOCK",
            "DIRT",
            "COARSE_DIRT",
            "ROOTED_DIRT",
            "PODZOL",
            "MYCELIUM",
            "DIRT_PATH",
            "MUD",
            "MUDDY_MANGROVE_ROOTS",
            "MOSS_BLOCK",
            "PALE_MOSS_BLOCK"
    );

    private SafeLocationFinder() {
    }

    public static Location findHighestSurface(Location origin, int radius) {
        if (origin == null || origin.getWorld() == null) return null;
        Location preferred = findHighestSurface(origin, radius, true);
        return preferred != null ? preferred : findHighestSurface(origin, radius, false);
    }

    private static Location findHighestSurface(
            Location origin,
            int radius,
            boolean preferredOnly
    ) {
        int baseX = origin.getBlockX();
        int baseZ = origin.getBlockZ();

        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) continue;
                    int x = baseX + dx;
                    int z = baseZ + dz;
                    Location candidate = findPreferredSurfaceInColumn(
                            origin,
                            x,
                            z,
                            preferredOnly
                    );
                    if (candidate != null) return candidate;
                }
            }
        }
        return null;
    }

    private static Location findPreferredSurfaceInColumn(
            Location origin,
            int x,
            int z,
            boolean preferredOnly
    ) {
        World world = origin.getWorld();
        if (world == null) return null;

        int highestY = Math.min(
                world.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE),
                world.getMaxHeight() - 1
        );
        for (int groundY = highestY;
             groundY >= world.getMinHeight() + 1;
             groundY--) {
            Block surface = world.getBlockAt(x, groundY, z);
            if (surface.isLiquid()) return null;
            if (!surface.isPassable()) {
                if (groundY > maximumReturnGroundY(world)) return null;
                if (preferredOnly && !isPreferredSurface(surface.getType())) {
                    return null;
                }
                Location candidate = new Location(
                        world,
                        x + 0.5,
                        groundY + 1,
                        z + 0.5,
                        origin.getYaw(),
                        origin.getPitch()
                );
                return isPhysicallySafe(candidate) ? candidate : null;
            }

            // Ignore vegetation and thin snow above the exposed ground.
        }
        return null;
    }

    static int maximumReturnGroundY(int worldMaxHeight) {
        return worldMaxHeight - BUILD_HEIGHT_BUFFER;
    }

    private static int maximumReturnGroundY(World world) {
        return maximumReturnGroundY(world.getMaxHeight());
    }

    public static Location findNear(Location origin, int radius, int verticalRange) {
        if (origin == null || origin.getWorld() == null) return null;
        int desiredY = clampY(origin.getWorld(), origin.getBlockY());
        List<Integer> yLevels = preferredYLevels(
                origin.getWorld().getMinHeight() + 2,
                origin.getWorld().getMaxHeight() - 2,
                desiredY,
                verticalRange
        );
        return find(origin, radius, yLevels);
    }

    public static Location findNearDesiredY(Location origin, int desiredY, int radius) {
        if (origin == null || origin.getWorld() == null) return null;
        World world = origin.getWorld();
        int clampedY = clampY(world, desiredY);
        List<Integer> yLevels = preferredYLevels(
                world.getMinHeight() + 2,
                world.getMaxHeight() - 2,
                clampedY,
                Integer.MAX_VALUE
        );
        Location adjusted = origin.clone();
        adjusted.setY(clampedY);
        return find(adjusted, radius, yLevels);
    }

    static List<Integer> preferredYLevels(
            int minimumY,
            int maximumY,
            int desiredY,
            int maximumDistance
    ) {
        int clampedDesired = Math.max(minimumY, Math.min(maximumY, desiredY));
        int availableDistance = Math.max(
                clampedDesired - minimumY,
                maximumY - clampedDesired
        );
        int limit = Math.min(maximumDistance, availableDistance);
        List<Integer> levels = new ArrayList<>();
        levels.add(clampedDesired);
        for (int distance = 1; distance <= limit; distance++) {
            int below = clampedDesired - distance;
            int above = clampedDesired + distance;
            if (below >= minimumY) levels.add(below);
            if (above <= maximumY) levels.add(above);
        }
        return levels;
    }

    static boolean isPreferredSurface(Material material) {
        String name = material.name();
        return name.endsWith("_LEAVES") || PREFERRED_SURFACE_NAMES.contains(name);
    }

    private static Location find(Location origin, int radius, List<Integer> yLevels) {
        World world = origin.getWorld();
        if (world == null) return null;

        int baseX = origin.getBlockX();
        int baseZ = origin.getBlockZ();
        for (int y : yLevels) {
            for (int ring = 0; ring <= radius; ring++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    for (int dz = -ring; dz <= ring; dz++) {
                        if (Math.abs(dx) != ring && Math.abs(dz) != ring) continue;
                        Location candidate = new Location(
                                world,
                                baseX + dx + 0.5,
                                y,
                                baseZ + dz + 0.5,
                                origin.getYaw(),
                                origin.getPitch()
                        );
                        if (isPhysicallySafe(candidate)) return candidate;
                    }
                }
            }
        }
        return null;
    }

    public static boolean isPhysicallySafe(Location location) {
        World world = location.getWorld();
        if (world == null) return false;

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        if (y <= world.getMinHeight() + 1 || y >= world.getMaxHeight() - 2) {
            return false;
        }

        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block below = world.getBlockAt(x, y - 1, z);
        if (!isDryPassableSpace(feet)
                || !isDryPassableSpace(head)
                || !below.getType().isSolid()) {
            return false;
        }

        Material feetType = feet.getType();
        Material belowType = below.getType();
        if (feetType == Material.LAVA
                || feetType == Material.FIRE
                || feetType == Material.SOUL_FIRE
                || feetType == Material.POWDER_SNOW) {
            return false;
        }
        return belowType != Material.LAVA
                && belowType != Material.MAGMA_BLOCK
                && belowType != Material.CAMPFIRE
                && belowType != Material.SOUL_CAMPFIRE
                && belowType != Material.CACTUS
                && belowType != Material.POWDER_SNOW;
    }

    static boolean isDryPassableMaterial(Material material) {
        return switch (material) {
            case WATER,
                 LAVA,
                 BUBBLE_COLUMN,
                 KELP,
                 KELP_PLANT,
                 SEAGRASS,
                 TALL_SEAGRASS,
                 POWDER_SNOW -> false;
            default -> true;
        };
    }

    private static boolean isDryPassableSpace(Block block) {
        return block.isPassable()
                && !block.isLiquid()
                && isDryPassableMaterial(block.getType());
    }

    private static int clampY(World world, int y) {
        return Math.max(
                world.getMinHeight() + 2,
                Math.min(world.getMaxHeight() - 2, y)
        );
    }
}
