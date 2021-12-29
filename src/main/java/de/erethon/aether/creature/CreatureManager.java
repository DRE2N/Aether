package de.erethon.aether.creature;

import de.erethon.aether.Aether;
import de.erethon.aether.tools.UpdatedMessageUtil;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class CreatureManager {

    Set<NPCData> creatures = new HashSet<>();

    public CreatureManager() {
        load();
    }

    public Set<NPCData> getCreatures() {
        return creatures;
    }

    public NPCData getByID(String id) {
        for (NPCData npcData : creatures) {
            if (npcData.getID().equalsIgnoreCase(id)) {
                return npcData;
            }
        }
        return null;
    }

    public void load() {
        creatures.clear();
        UpdatedMessageUtil.log("Loading creatures (" + Aether.CREATURES.listFiles().length + " files in folder)");
        for (File file : Aether.CREATURES.listFiles()){
            if (file.getName().contains("disabled")) {
                continue;
            }
            if (file.isDirectory()) {
                loadSub(file);
                continue;
            }
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
            String id = file.getName().replaceAll(".yml", "");
            creatures.add(new NPCData(configuration, id));
        }
        UpdatedMessageUtil.log("Loaded " + creatures.size() + " creatures.");
    }

    public void loadSub(File file) {
        for (File f : file.listFiles()){
            if (f.getName().contains("disabled")) {
                continue;
            }
            if (f.isDirectory()) {
                loadSub(f);
            }
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(f);
            String id = f.getName().replaceAll(".yml", "");
            creatures.add(new NPCData(configuration, id));
        }
    }

}
