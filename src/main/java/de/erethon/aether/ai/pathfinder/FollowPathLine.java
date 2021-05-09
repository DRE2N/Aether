package de.erethon.aether.ai.pathfinder;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class FollowPathLine {

    List<Location> locations = new ArrayList<>();
    int waitAtPointDelay = 0;
    boolean repeat = false;
    boolean teleportIfFarAway = false;
}
