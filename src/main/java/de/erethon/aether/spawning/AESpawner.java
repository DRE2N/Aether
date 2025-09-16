package de.erethon.aether.spawning;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.AetherBaseMob;
import de.erethon.aether.creature.CreatureManager;
import de.erethon.aether.creature.NPCData;
import de.erethon.hephaestus.utils.HRandom;
import de.erethon.papyrus.entities.CustomMob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class AESpawner {

    private final Aether plugin = Aether.getInstance();
    private final CreatureManager creatureManager = plugin.getCreatureManager();
    private final ConfigurationSection config;

    private String id;

    // Wave config/state
    private int waveCooldown = 20 * 60; // ticks between waves
    private int waveSize = 1;           // how many mobs per wave
    private boolean inWave = false;
    private int remainingInWave = 0;
    private int nextIntraTick = 0;      // next tick at which we may spawn within a wave
    private int nextWaveTick = 0;       // next tick at which we may start a wave

    private int lastMobCount = 0;

    // Spawning config/state
    private NPCData npcData = null;
    private Location centerLocation;
    private int radius = 16;
    private int radiusY = 4; // Mainly used to prevent spawning on roofs, trees, etc.
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

    // Weighted mob selection
    private final Map<String, Integer> weightedIds = new LinkedHashMap<>();
    private final Map<String, NPCData> npcCache = new HashMap<>();
    private final Set<String> entityIDs = new HashSet<>();

    // Papyrus identifier for counting existing mobs of this spawner's type
    private String entityID;

    public AESpawner(ConfigurationSection config, String id) {
        this.config = config;
        this.id = id;
        load();
    }

    public void spawn() {
        if (centerLocation.getWorld() == null || !centerLocation.isChunkLoaded()) {
            return;
        }
        if (weightedIds.isEmpty()) {
            return;
        }
        int now = Bukkit.getCurrentTick();
        // If not actively spawning a wave, consider starting one
        if (!inWave) {
            if (now < nextWaveTick) {
                return;
            }
            if (centerLocation.getNearbyPlayers(activationRange, p -> p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE).isEmpty()) {
                nextWaveTick = now + 200; // retry in ~10s to avoid per-tick checks
                return;
            }
            if (Math.random() > probability) {
                nextWaveTick = now + waveCooldown;
                return;
            }
            // Capacity check before starting a wave
            if (!entityIDs.isEmpty()) {
                int nearby = centerLocation.getNearbyLivingEntities(maxMobsRange, maxMobsRangeY, e -> {
                    if (e instanceof CustomMob customMob) {
                        String pid = customMob.getPapyrusId();
                        return pid != null && entityIDs.contains(pid);
                    }
                    return false;
                }).size();
                if (nearby >= maxMobs) {
                    nextWaveTick = now + waveCooldown;
                    return;
                }
                lastMobCount = nearby;
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
        if (!entityIDs.isEmpty()) {
            int nearby = centerLocation.getNearbyLivingEntities(maxMobsRange, maxMobsRangeY, e -> {
                if (e instanceof CustomMob customMob) {
                    String pid = customMob.getPapyrusId();
                    return pid != null && entityIDs.contains(pid);
                }
                return false;
            }).size();
            if (nearby >= maxMobs) {
                endWave(now);
                return;
            }
            lastMobCount = nearby;
        }

        // Attempt to spawn exactly one mob per intra cooldown
        Location loc = pickSpawnLocation();
        if (loc == null) {
            nextIntraTick = now + cooldown;
            return;
        }
        String chosenId;
        try {
            chosenId = HRandom.selectWeightedRandomValue(weightedIds, null);
        } catch (Throwable t) {
            List<String> keys = new ArrayList<>(weightedIds.keySet());
            chosenId = keys.get(new Random().nextInt(keys.size()));
        }
        NPCData chosen = npcCache.get(chosenId);
        if (chosen == null) {
            nextIntraTick = now + cooldown;
            return;
        }
        Class<? extends AetherBaseMob> toSpawn = chosen.getEntityClass();
        try {
            AetherBaseMob activeNPC;
            if (minLevel != -1 && maxLevel != -1) {
                int level;
                if (minLevel == maxLevel) {
                    level = minLevel;
                } else {
                    level = minLevel + new Random().nextInt(maxLevel - minLevel + 1);
                }
                activeNPC = toSpawn.getConstructor(NPCData.class, World.class, Integer.class).newInstance(chosen, loc.getWorld(), level);
            } else {
                activeNPC = toSpawn.getConstructor(NPCData.class, World.class).newInstance(chosen, loc.getWorld());
            }
            activeNPC.setPos(loc.getX(), loc.getY(), loc.getZ());
            activeNPC.addToWorld();
            BlockPos bp = new BlockPos(centerLocation.getBlockX(), centerLocation.getBlockY(), centerLocation.getBlockZ());
            // Make them stay around the spawner area
            activeNPC.setHomeTo(bp, radius);
            activeNPC.setPersistenceRequired(false);
            activeNPC.persist = false;
            MoveTowardsRestrictionGoal goal = new MoveTowardsRestrictionGoal(activeNPC, 0.15);
            activeNPC.goalSelector.addGoal(7, goal);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            Aether.log("Spawner " + id + " failed to spawn mob: " + e.getMessage());
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

            Location highestBlock = centerLocation.getWorld().getHighestBlockAt(nx, nz, HeightMap.MOTION_BLOCKING).getLocation();
            if (highestBlock.getY() > centerLocation.getBlockY() + radiusY) {
                continue;
            }
            if (highestBlock.getY() < centerLocation.getBlockY() - radiusY) {
                continue;
            }
            return highestBlock.add(0, 1, 0);
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
        // Backwards compatibility: return an arbitrary id if present
        return entityIDs.stream().findFirst().orElse(entityID);
    }

    public Set<String> getEntityIDs() {
        return Collections.unmodifiableSet(entityIDs);
    }

    public String getEntityIDsString() {
        return String.join(",", entityIDs);
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

    public boolean isInWave() {
        return inWave;
    }

    public int getRemainingInWave() {
        return remainingInWave;
    }

    public int getNextWaveTick() {
        return nextWaveTick;
    }

    public int getNextIntraTick() {
        return nextIntraTick;
    }

    public int getLastMobCount() {
        return lastMobCount;
    }

    private void load() {
        // comma-separated id string with optional weights (e.g., "bandit;70, marksman;30" or "bandit:70,marksman:30").
        weightedIds.clear();
        npcCache.clear();
        entityIDs.clear();
        String rawId = config.getString("id");
        if (rawId != null && !rawId.isBlank()) {
            String[] parts = rawId.split(",");
            for (String p : parts) {
                String token = p.trim();
                if (token.isEmpty()) continue;
                String idStr = token;
                int weight = 100;
                int sep = token.indexOf(';');
                if (sep < 0) sep = token.indexOf(':');
                if (sep > 0 && sep < token.length() - 1) {
                    idStr = token.substring(0, sep).trim();
                    try {
                        weight = Integer.parseInt(token.substring(sep + 1).trim());
                    } catch (NumberFormatException ignored) {}
                }
                if (idStr.isEmpty()) continue;
                weightedIds.put(idStr, weight);
            }
        }
        if (weightedIds.isEmpty()) {
            String single = config.getString("id");
            if (single != null && !single.isBlank()) {
                weightedIds.put(single.trim(), 100);
            }
        }
        for (Map.Entry<String, Integer> entry : weightedIds.entrySet()) {
            String mobId = entry.getKey();
            NPCData data = creatureManager.getByID(mobId);
            if (data == null) {
                Aether.addException("Spawner:" + id, "Unknown NPC id '" + mobId + "' in spawner", "Ensure the NPC id exists", null);
                continue;
            }
            npcCache.put(mobId, data);
            entityIDs.add(mobId);
        }
        npcData = npcCache.values().stream().findFirst().orElse(null);
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
