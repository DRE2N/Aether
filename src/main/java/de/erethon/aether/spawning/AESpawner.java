package de.erethon.aether.spawning;

import de.erethon.aether.Aether;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.CreatureManager;
import de.erethon.aether.creature.NPCData;
import de.erethon.bedrock.chat.MessageUtil;
import org.apache.logging.log4j.message.Message;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AESpawner {

    Aether plugin = Aether.getInstance();
    CreatureManager creatureManager = plugin.getCreatureManager();
    ConfigurationSection config;

    NPCData npcData = null;
    Location centerLocation;
    int radius = 16;
    int radiusY = 4;
    int mobsPerSpawn = 1;
    double probability = 1.00;
    int maxMobs = 10;
    int maxMobsRange = 16;
    int cooldown = 30;
    int activationRange = 32;

    BukkitRunnable runnable;

    public AESpawner(ConfigurationSection config) {
        this.config = config;
        load();
    }

    public void start() {
        runnable = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        };
        runnable.runTaskTimer(plugin, cooldown * 20L, cooldown * 20L);
    }

    public void stop() {
        runnable.cancel();
    }

    public void tick() {
        if (centerLocation.getWorld() == null || !centerLocation.isChunkLoaded()) {
            return;
        }
        if (centerLocation.getNearbyPlayers(activationRange).isEmpty()) {
            return;
        }
        if (centerLocation.getNearbyEntitiesByType(npcData.getBaseType().getEntityClass(), maxMobsRange).size() > maxMobs) {
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

    private void load() {
        npcData = creatureManager.getByID(config.getString("id"));
        World world = Bukkit.getWorld(config.getString("loc.world", "Erethon"));
        int x = config.getInt("loc.x", 0);
        int y = config.getInt("loc.y", 64);
        int z = config.getInt("loc.z", 0);
        centerLocation = new Location(world, x, y, z);
        radius = config.getInt("radius", 16);
        radiusY = config.getInt("radiusY", 4);
        mobsPerSpawn = config.getInt("mobsPerSpawn", 1);
        probability = config.getDouble("chance", 0.00);
        maxMobs = config.getInt("maxMobs", 10);
        maxMobsRange = config.getInt("maxMobsRange", 16);
        cooldown = config.getInt("cooldown", 30);
        activationRange = config.getInt("activationRange", 32);
    }
}
