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

    private String id;

    private NPCData npcData = null;
    private Location centerLocation;
    private int radius = 16;
    private int radiusY = 0;
    private int mobsPerSpawn = 1;
    private double probability = 1.00;
    private int maxMobs = 5;
    private int maxMobsRange = 32;
    private int maxMobsRangeY = 16;
    private int cooldown = 5; // intra-spawn cooldown (ticks) within a wave
    private int activationRange = 32;
    private boolean isTicking = true;
    private int minLevel = -1;
    private int maxLevel = -1;

    // Papyrus identifier for counting existing mobs of this spawner's type
    private String entityID;

    // Wave config/state
    private int waveCooldown = 20 * 60; // ticks between waves
    private int waveSize = 1;           // how many mobs per wave
    private boolean inWave = false;
    private int remainingInWave = 0;
    private int nextIntraTick = 0;      // next tick at which we may spawn within a wave
    private int nextWaveTick = 0;       // next tick at which we may start a wave

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
        int now = Bukkit.getCurrentTick();
        // If not actively spawning a wave, consider starting one
        if (!inWave) {
            if (now < nextWaveTick) {
                return;
            }
            if (centerLocation.getNearbyPlayers(activationRange).isEmpty()) {
                nextWaveTick = now + 20; // retry in ~1s to avoid per-tick checks
                return;
            }
            if (Math.random() > probability) {
                nextWaveTick = now + waveCooldown;
                return;
            }
            // Capacity check before starting a wave
            if (entityID != null) {
                int nearby = centerLocation.getNearbyLivingEntities(maxMobsRange, maxMobsRangeY, e -> {
                    if (e instanceof CustomMob customMob) {
                        String id = customMob.getPapyrusId();
                        return id != null && id.equals(entityID);
                    }
                    return false;
                }).size();
                if (nearby >= maxMobs) {
                    nextWaveTick = now + waveCooldown;
                    return;
                }
            }
            // Start new wave
            inWave = true;
            remainingInWave = Math.max(1, waveSize);
            nextIntraTick = now; // spawn first mob immediately
        }

        if (now < nextIntraTick) {
            return;
        }
        if (centerLocation.getNearbyPlayers(activationRange).isEmpty()) {
            endWave(now);
            return;
        }
        if (entityID != null) {
            int nearby = centerLocation.getNearbyLivingEntities(maxMobsRange, maxMobsRangeY, e -> {
                if (e instanceof CustomMob customMob) {
                    String id = customMob.getPapyrusId();
                    return id != null && id.equals(entityID);
                }
                return false;
            }).size();
            if (nearby >= maxMobs) {
                endWave(now);
                return;
            }
        }

        // Attempt to spawn exactly one mob per intra cooldown
        Location loc = pickSpawnLocation();
        if (loc == null) {
            nextIntraTick = now + cooldown;
            return;
        }
        Class<? extends AetherBaseMob> toSpawn = npcData.getEntityClass();
        try {
            AetherBaseMob activeNPC;
            if (minLevel != -1 && maxLevel != -1) {
                int level;

                if (minLevel == maxLevel) {
                    level = minLevel;
                } else {
                    level = minLevel + new Random().nextInt(maxLevel - minLevel + 1);
                }
                activeNPC = toSpawn.getConstructor(NPCData.class, World.class, Integer.class).newInstance(npcData, loc.getWorld(), level);
            } else {
                activeNPC = toSpawn.getConstructor(NPCData.class, World.class).newInstance(npcData, loc.getWorld());
            }
            activeNPC.setPos(loc.getX(), loc.getY(), loc.getZ());
            activeNPC.addToWorld();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            Aether.log("Spawner " + id + " failed to spawn mob of type " + toSpawn.getName() + ": " + e.getMessage());
            e.printStackTrace();
            endWave(now);
            return;
        }

        remainingInWave--;
        if (remainingInWave <= 0) {
            endWave(now);
        } else {
            nextIntraTick = now + cooldown;
        }
    }

    private void endWave(int now) {
        inWave = false;
        remainingInWave = 0;
        nextIntraTick = 0;
        nextWaveTick = now + waveCooldown;
    }

    private Location pickSpawnLocation() {
        Random random = new Random();
        int attempts = 6;
        while (attempts-- > 0) {
            int nx = centerLocation.getBlockX() - radius + random.nextInt(radius * 2 + 1);
            int nz = centerLocation.getBlockZ() - radius + random.nextInt(radius * 2 + 1);
            Location loc;
            if (radiusY == 0) {
                Location highestBlock = centerLocation.getWorld().getHighestBlockAt(nx, nz, HeightMap.MOTION_BLOCKING).getLocation();
                loc = highestBlock.add(0, 1, 0);
            } else {
                int ny = centerLocation.getBlockY() - radiusY + random.nextInt(radiusY * 2 + 1);
                loc = new Location(centerLocation.getWorld(), nx + 0.5, ny, nz + 0.5);
                if (loc.getBlock().getType().isSolid() || !loc.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                    continue;
                }
            }
            return loc;
        }
        return null;
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

    public String getEntityID() {
        return entityID;
    }

    public int getRadiusY() {
        return radiusY;
    }

    public int getMobsPerSpawn() {
        return mobsPerSpawn;
    }

    public double getProbability() {
        return probability;
    }

    public int getMaxMobs() {
        return maxMobs;
    }

    public int getMaxMobsRange() {
        return maxMobsRange;
    }

    public int getMaxMobsRangeY() {
        return maxMobsRangeY;
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getActivationRange() {
        return activationRange;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getWaveCooldown() {
        return waveCooldown;
    }

    public int getWaveSize() {
        return waveSize;
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
        Aether.log("Loaded spawner " + config.getName() + " at " + centerLocation);
        radius = config.getInt("radius", 16);
        radiusY = config.getInt("radiusY", 4);
        mobsPerSpawn = config.getInt("mobsPerSpawn", 1);
        probability = config.getDouble("chance", 1.00);
        maxMobs = config.getInt("maxMobs", 10);
        maxMobsRange = config.getInt("maxMobsRange", 16);
        maxMobsRangeY = config.getInt("maxMobsRangeY", 8);
        cooldown = config.getInt("cooldown", 30); // intra-spawn cooldown (ticks)
        activationRange = config.getInt("activationRange", 32);
        // Waves
        waveSize = config.getInt("waveSize", mobsPerSpawn);
        waveCooldown = config.getInt("waveCooldown", 20 * 60);
        // Levels
        minLevel = config.getInt("minLevel", -1);
        maxLevel = config.getInt("maxLevel", -1);
    }
}
