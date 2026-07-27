package com.rift.zoneWars;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class GetZoneBorderBlockLocation {
    public static Location findClosestAirOnSolidBlock(World world, int x, int z, int playerY, int radius) {
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        int startY = Math.max(minY, Math.min(maxY, playerY));
        int maxRadius = Math.min(radius, Math.max(startY - minY, maxY - startY));
        Location reusableLocation = new Location(world, x, startY, z);
        if (isAirOnSolid(world, x, startY, z)) {
            return reusableLocation;
        }
        for (int offset = 1; offset <= maxRadius; offset++) {
            int checkUpY = startY + offset;
            if (checkUpY <= maxY) {
                if (isAirOnSolid(world, x, checkUpY, z)) {
                    reusableLocation.setY(checkUpY);
                    return reusableLocation;
                }
            }
            int checkDownY = startY - offset;
            if (checkDownY >= minY) {
                if (isAirOnSolid(world, x, checkDownY, z)) {
                    reusableLocation.setY(checkDownY);
                    return reusableLocation;
                }
            }
        }
        if (world.getBlockState(x, playerY, z).getType().isAir()) {
            return new Location(world, x, playerY, z);
        }
        return null;
    }
    private static boolean isAirOnSolid(World world, int x, int y, int z) {
        Material currentBlock = world.getType(x, y, z);
        if (!currentBlock.isAir()) {
            return false;
        }
        Material blockBelow = world.getType(x, y - 1, z);
        return blockBelow.isSolid() && !blockBelow.isTransparent();
    }
}