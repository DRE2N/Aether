package de.erethon.aether.spawning;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.CreatureManager;
import de.erethon.aether.creature.NPCData;
import de.erethon.papyrus.entities.CustomMob;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;

public class AESpawner {

    private final Aether plugin = Aether.getInstance();
    private final CreatureManager creatureManager = plugin.getCreatureManager();
    private final ConfigurationSection config;

    private final String id;

    private NPCData npcData = null;
    private Location centerLocation;
    private int radius = 16;
    private int radiusY = 0;
    private int mobsPerSpawn = 1;
    private double probability = 1.00;
    private int maxMobs = 5;
    private int maxMobsRange = 32;
    private int maxMobsRangeY = 16;
    private int cooldown = 5;
    private int activationRange = 32;
    private boolean isTicking = true;

    private String entityID;

    private int lastSpawnFailTick = 0;

    public AESpawner(ConfigurationSection config, String id) {
        this.config = config;
        this.id = id;
        load();
    }

    public void spawn() {
        if (centerLocation.getWorld() == null || !centerLocation.isChunkLoaded()) {
            return;
        }
        if (npcData == null) {
            return;
        }
        if (Math.random() > probability) {
            return;
        }
        if (lastSpawnFailTick + cooldown > Bukkit.getCurrentTick()) {
            return;
        }
        if (centerLocation.getNearbyPlayers(activationRange).isEmpty()) {
            return;
        }
        if (entityID != null) {
            if (centerLocation.getNearbyLivingEntities(maxMobsRange, maxMobsRangeY, e -> {
                if (e instanceof CustomMob customMob) {
                    String id = customMob.getPapyrusId();
                    return id != null && id.equals(entityID);
                }
                return false;
            }).size() >= maxMobs) {
                lastSpawnFailTick = Bukkit.getCurrentTick();
                return;
            }
        }
        Random random = new Random();
        for (int i = 0; i < mobsPerSpawn; i++) {
            int nx = centerLocation.getBlockX() - radius + random.nextInt(radius * 2);
            int nz = centerLocation.getBlockZ() - radius + random.nextInt(radius * 2);
            Location loc;
            if (radiusY == 0) {
                Location highestBlock = centerLocation.getWorld().getHighestBlockAt(nx, nz, HeightMap.MOTION_BLOCKING).getLocation();
                loc = highestBlock.add(0, 1, 0);
            } else {
                int ny = centerLocation.getBlockY() - radiusY + random.nextInt(radiusY * 2);
                loc = new Location(centerLocation.getWorld(), nx + 0.5, ny, nz + 0.5);
                if (loc.getBlock().getType().isSolid() || !loc.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                    i--;
                    continue;
                }
            }
            Class<? extends AetherBaseMob> toSpawn = npcData.getEntityClass();
            AetherBaseMob activeNPC = null;
            try {
                activeNPC = toSpawn.getConstructor(NPCData.class, World.class).newInstance(npcData, loc.getWorld());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                Aether.log("Spawner " + id + " failed to spawn mob of type " + toSpawn.getName() + ": " + e.getMessage());
                return;
            }
            activeNPC.setPos(loc.getX(), loc.getY(), loc.getZ());
            activeNPC.addToWorld();
        }
        lastSpawnFailTick = 0;
    }

    public Location getCenterLocation() {
        return centerLocation;
    }

    public boolean isTicking() {
        return isTicking;
    }

    public String getId() {
        return id;
    }

    public int getRadius() {
        return radius;
    }

    private void load() {
        npcData = creatureManager.getByID(config.getString("id"));
        if (npcData != null) {
            entityID = npcData.getID();
        }
        World world = Bukkit.getWorld(config.getString("loc.world", "Erethon"));
        int x = config.getInt("loc.x", 0);
        int y = config.getInt("loc.y", 64);
        int z = config.getInt("loc.z", 0);
        isTicking = config.getBoolean("isTicking", true);
        centerLocation = new Location(world, x, y, z);
        Aether.log("Loaded spawner " + config.getName() + " at " + centerLocation.toString());
        radius = config.getInt("radius", 16);
        radiusY = config.getInt("radiusY", 4);
        mobsPerSpawn = config.getInt("mobsPerSpawn", 1);
        probability = config.getDouble("chance", 0.00);
        maxMobs = config.getInt("maxMobs", 10);
        maxMobsRange = config.getInt("maxMobsRange", 16);
        maxMobsRangeY = config.getInt("maxMobsRangeY", 8);
        cooldown = config.getInt("cooldown", 30);
        activationRange = config.getInt("activationRange", 32);
    }
}
