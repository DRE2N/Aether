package de.erethon.aether.groups;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;


public class FormationTools {

    public static List<Location> getLine(Location center, FormationDirection dir, int amount, double spacing) {
        List<Location> result = new ArrayList<>();
        World world = center.getWorld();
        double x = center.getX();
        double y = center.getY();
        double z = center.getZ();
        int distance = amount / 2;
        double current = x;
        if (dir == FormationDirection.X) {
            for (int i = 0; i < distance; i++) {
                current = current - spacing;
                Location location = new Location(world, current, y, z);
                Location highest = world.getHighestBlockAt(location).getLocation().add(0,1,0);
                result.add(highest);
            }
            current = x;
            for (int i = 0; i < distance; i++) {
                current = current + spacing;
                Location location = new Location(world, current, y, z);
                Location highest = world.getHighestBlockAt(location).getLocation().add(0,1,0);
                result.add(highest);
            }
        }
        if (dir == FormationDirection.Z) {
            for (int i = 0; i < distance; i++) {
                current = current - spacing;
                Location location = new Location(world, x, y, current);
                Location highest = world.getHighestBlockAt(location).getLocation().add(0,1,0);
                result.add(highest);
            }
            current = x;
            for (int i = 0; i < distance; i++) {
                current = current + spacing;
                Location location = new Location(world, x, y, current);
                Location highest = world.getHighestBlockAt(location).getLocation().add(0,1,0);
                result.add(highest);
            }
        }
        return result;
    }
    public static List<Location> getSquare(Location center, FormationDirection dir, int width, int length, double spacingWidth, double spacingLength) {
        List<Location> result = new ArrayList<>();
        World world = center.getWorld();
        double x = center.getX();
        double y = center.getY();
        double z = center.getZ();
        int distanceLength = length / 2;
        double current;
        if (dir == FormationDirection.X) {
            current = z;
            for (int i = 0; i < distanceLength; i++) {
                current = current - spacingLength;
                Location lineCenter = new Location(world, x, y, current);
                result.addAll(getLine(lineCenter, dir, width, spacingWidth));
            }
            current = z;
            for (int i = 0; i < distanceLength; i++) {
                current = current + spacingLength;
                Location lineCenter = new Location(world, x, y, current);
                result.addAll(getLine(lineCenter, dir, width, spacingWidth));
            }
        }
        if (dir == FormationDirection.Z) {
            current = x;
            for (int i = 0; i < distanceLength; i++) {
                current = current - spacingLength;
                Location lineCenter = new Location(world, current, y, z);
                result.addAll(getLine(lineCenter, dir, width, spacingWidth));
            }
            current = x;
            for (int i = 0; i < distanceLength; i++) {
                current = current + spacingLength;
                Location lineCenter = new Location(world, current, y, z);
                result.addAll(getLine(lineCenter, dir, width, spacingWidth));
            }
        }
        return result;
    }
}
