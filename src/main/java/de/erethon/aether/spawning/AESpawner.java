package de.erethon.aether.spawning;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.CreatureManager;
import de.erethon.aether.creature.NPCData;
import de.erethon.bedrock.chat.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.EntityType;

import java.util.Random;

public class AESpawner {

    private final Aether plugin = Aether.getInstance();
    private final CreatureManager creatureManager = plugin.getCreatureManager();
    private final ConfigurationSection config;

    private String id;

    private NPCData npcData = null;
    private Location centerLocation;
    private int radius = 16;
    private int radiusY = 4;
    private int mobsPerSpawn = 1;
    private double probability = 1.00;
    private int maxMobs = 10;
    private int maxMobsRange = 16;
    private int maxMobsRangeY = 8;
    private int cooldown = 30;
    private int activationRange = 32;
    private boolean isTicking = true;

    private EntityType entityType;

    public AESpawner(ConfigurationSection config, String id) {
        this.config = config;
        this.id = id;
        load();
    }

    public void spawn() {
        if (centerLocation.getWorld() == null || !centerLocation.isChunkLoaded()) {
            return;
        }
        if (centerLocation.getNearbyPlayers(activationRange).isEmpty()) {
            return;
        }
        if (centerLocation.getNearbyLivingEntities(maxMobsRange, maxMobsRangeY, e -> e.getType().equals(entityType)).size() >= maxMobs) {
            return;
        }
        Random random = new Random();
        for (int i = 0; i < mobsPerSpawn; i++) {
            int nx = centerLocation.getBlockX() - radius + random.nextInt(radius * 2);
            int nz = centerLocation.getBlockZ() - radius + random.nextInt(radius * 2);
            int ny = centerLocation.getBlockY() - radiusY + random.nextInt(radiusY * 2);
            Location loc = new Location(centerLocation.getWorld(), nx, ny, nz);
            if (loc.add(0, 1, 0).getBlock().getType().isSolid()) {
                break;
            }
            ActiveNPC activeNPC = new ActiveNPC(npcData);
            activeNPC.spawn(loc);
        }
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

    private void load() {
        npcData = creatureManager.getByID(config.getString("id"));
        World world = Bukkit.getWorld(config.getString("loc.world", "Erethon"));
        int x = config.getInt("loc.x", 0);
        int y = config.getInt("loc.y", 64);
        int z = config.getInt("loc.z", 0);
        isTicking = config.getBoolean("isTicking", true);
        centerLocation = new Location(world, x, y, z);
        MessageUtil.log("Loaded spawner " + config.getName() + " at " + centerLocation.toString());
        radius = config.getInt("radius", 16);
        radiusY = config.getInt("radiusY", 4);
        mobsPerSpawn = config.getInt("mobsPerSpawn", 1);
        probability = config.getDouble("chance", 0.00);
        maxMobs = config.getInt("maxMobs", 10);
        maxMobsRange = config.getInt("maxMobsRange", 16);
        cooldown = config.getInt("cooldown", 30);
        activationRange = config.getInt("activationRange", 32);
        entityType = CraftMagicNumbers.getEntityType(npcData.getBaseType());
    }
}
