package de.erethon.aether.creature;

import de.erethon.aether.Aether;
import de.erethon.bedrock.chat.MessageUtil;
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
        MessageUtil.log("Could not find creature with id " + id + "!");
        return null;
    }

    public void reload() {
        creatures.clear();
        load();
    }

    public void load() {
        creatures.clear();
        MessageUtil.log("Loading creatures (" + Aether.CREATURES.listFiles().length + " files in folder)");
        for (File file : Aether.CREATURES.listFiles()){
            if (file.getName().contains("disabled")) {
                continue;
            }
            if (file.isDirectory()) {
                loadSub(file);
                continue;
            }
            loadNPCFile(file);
        }
        MessageUtil.log("Loaded " + creatures.size() + " creatures.");
    }

    public void loadSub(File file) {
        for (File f : file.listFiles()){
            if (f.getName().contains("disabled")) {
                continue;
            }
            if (f.isDirectory()) {
                loadSub(f);
            }
            loadNPCFile(file);
        }
    }

    private void loadNPCFile(File file) {
        YamlConfiguration configuration;
        try {
            configuration = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            Aether.addException("YamlConfiguration", "Error loading NPC file " + file.getName(), "Make sure its a valid YAML file", e);
            MessageUtil.log("Error loading NPC file " + file.getName());
            e.printStackTrace();
            return;
        }
        String id = file.getName().replaceAll(".yml", "");
        try {
            creatures.add(new NPCData(configuration, id));
        } catch (Exception e) {
            Aether.addException("NPCData", "Error loading NPC data" + id, "Check the file for errors", e);
            MessageUtil.log("Error loading NPC data " + id);
            e.printStackTrace();
        }
    }

}
