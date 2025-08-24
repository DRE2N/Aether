package de.erethon.aether.spawning;

import de.erethon.aether.Aether;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpawnerManager extends BukkitRunnable implements Listener {

    private final List<AESpawner> configuredSpawners = new ArrayList<>();
    private final HashMap<ChunkPos, Set<AESpawner>> spawnersbyChunk = new HashMap<>();
    private final Set<AESpawner> activeSpawners = new HashSet<>();
    private boolean listenersRegistered = false;

    public SpawnerManager() {
        loadSpawners();
        if (!listenersRegistered) {
            Bukkit.getPluginManager().registerEvents(this, Aether.getInstance());
            listenersRegistered = true;
        }
        runTaskTimer(Aether.getInstance(), 0, 20);
    }

    @Override
    public void run() {
        for (AESpawner spawner : activeSpawners) {
            if (spawner.isTicking()) {
                spawner.spawn();
            }
        }
    }

    public void loadSpawners() {
        File[] files = Aether.SPAWNERS.listFiles();
        if (files == null) {
            Aether.log("No spawner files found.");
            return;
        }
        for (File file : files) {
            YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);
            for (String key : fileConfig.getKeys(false)) {
                try {
                    configuredSpawners.add(new AESpawner(fileConfig.getConfigurationSection(key), key));
                } catch (Exception e) {
                    Aether.addException("SpawnerManager.loadSpawners", "Error while loading spawner " + key, "Error while loading spawner " + key, e);
                }
            }
        }
        Aether.log("Loaded " + configuredSpawners.size() + " spawners.");
        for (AESpawner spawner : configuredSpawners) {
            World world = spawner.getCenterLocation().getWorld();
            if (world == null) continue;
            int x = spawner.getCenterLocation().getBlockX();
            int z = spawner.getCenterLocation().getBlockZ();
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            ChunkPos pos = new ChunkPos(chunkX, chunkZ);
            Set<AESpawner> spawnersAtChunk = spawnersbyChunk.getOrDefault(pos, new HashSet<>());
            spawnersAtChunk.add(spawner);
            spawnersbyChunk.put(pos, spawnersAtChunk);
            // Immediately activate spawners in already-loaded chunks
            if (world.isChunkLoaded(chunkX, chunkZ)) {
                activeSpawners.add(spawner);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        int x = event.getChunk().getX();
        int z = event.getChunk().getZ();
        Set<AESpawner> spawnersAtChunk = spawnersbyChunk.getOrDefault(new ChunkPos(x, z), new HashSet<>());
        activeSpawners.addAll(spawnersAtChunk);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        int x = event.getChunk().getX();
        int z = event.getChunk().getZ();
        Set<AESpawner> spawnersAtChunk = spawnersbyChunk.getOrDefault(new ChunkPos(x, z), new HashSet<>());
        activeSpawners.removeAll(spawnersAtChunk);
    }

    public void triggerSpawner(String id) {
        for (AESpawner spawner : configuredSpawners) {
            if (spawner.getId().equals(id)) {
                spawner.spawn();
            }
        }
    }

    public void reloadSpawners() {
        configuredSpawners.clear();
        spawnersbyChunk.clear();
        activeSpawners.clear();
        loadSpawners();
    }

    // Expose configured spawners for commands and debugging
    public List<AESpawner> getConfiguredSpawners() {
        return configuredSpawners;
    }

}
