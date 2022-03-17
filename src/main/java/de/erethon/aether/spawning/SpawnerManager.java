package de.erethon.aether.spawning;

import de.erethon.aether.Aether;
import de.erethon.bedrock.chat.MessageUtil;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SpawnerManager {

    List<AESpawner> spawners = new ArrayList<>();

    public void loadSpawners() {
        for (File file : Aether.SPAWNERS.listFiles()) {
            YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);
            for (String key : fileConfig.getKeys(false)) {
                spawners.add(new AESpawner(fileConfig.getConfigurationSection(key)));
            }
        }
        MessageUtil.log("Loaded " + spawners.size() + " spawners.");
    }

    public void reloadSpawners() {
        stopSpawning();
        spawners.clear();
        loadSpawners();
        startSpawning();
    }

    public void startSpawning() {
        for (AESpawner spawner : spawners) {
            spawner.start();
        }
    }

    public void stopSpawning() {
        for (AESpawner spawner : spawners) {
            spawner.stop();
        }
    }

}
