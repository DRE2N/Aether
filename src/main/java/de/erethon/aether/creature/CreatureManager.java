package de.erethon.aether.creature;

import de.erethon.aether.Aether;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CreatureManager {

    private Set<NPCData> creatures = new HashSet<>();
    private Map<String, AetherBaseMob> activeTaggedMobs = new HashMap<>();

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
        Aether.log("Could not find creature with id " + id + "!");
        return null;
    }

    public List<String> getIDs() {
        List<String> ids = new ArrayList<>();
        for (NPCData npcData : creatures) {
            ids.add(npcData.getID());
        }
        return ids;
    }

    public void addTaggedMob(String tag, AetherBaseMob mob) {
        activeTaggedMobs.put(tag, mob);
    }

    public AetherBaseMob getTaggedMob(String tag) {
        return activeTaggedMobs.get(tag);
    }

    public void removeTaggedMob(String tag) {
        activeTaggedMobs.remove(tag);
    }

    public void reload() {
        creatures.clear();
        load();
        loadDelayed();
    }

    public void load() {
        creatures.clear();
        Aether.log("Loading creatures (" + Aether.CREATURES.listFiles().length + " files in folder)");
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
        Aether.log("Loaded " + creatures.size() + " creatures.");
    }

    public void loadDelayed() { // Spellbook
        for (NPCData creature : creatures) {
            creature.loadDelayed();
        }
    }

    public void loadSub(File file) {
        for (File f : file.listFiles()){
            if (f.getName().contains("disabled")) {
                continue;
            }
            if (f.isDirectory()) {
                loadSub(f);
                continue;
            }
            loadNPCFile(f);
        }
    }

    private void loadNPCFile(File file) {
        YamlConfiguration configuration;
        try {
            configuration = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            Aether.addException("YamlConfiguration", "Error loading NPC file " + file.getName(), "Make sure its a valid YAML file", e);
            Aether.log("Error loading NPC file " + file.getName());
            e.printStackTrace();
            return;
        }
        String id = file.getName().replaceAll(".yml", "");
        try {
            NPCData data = new NPCData(configuration, id);
            creatures.add(data);
        } catch (Exception e) {
            Aether.addException("NPCData", "Error loading NPC data" + id, "Check the file for errors", e);
            Aether.log("Error loading NPC data " + id);
            e.printStackTrace();
        }
    }

    public static void loadEarly(File folder) {
        for (File file : folder.listFiles()){
            if (file.getName().contains("disabled")) {
                continue;
            }
            if (file.isDirectory()) {
                loadEarlySub(file);
                continue;
            }
            loadEarlyNPCFile(file);
        }
    }

    private static void loadEarlyNPCFile(File file) {
        YamlConfiguration configuration;
        try {
            configuration = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            Aether.addException("YamlConfiguration", "Error loading NPC file " + file.getName(), "Make sure its a valid YAML file", e);
            Aether.log("Error loading NPC file " + file.getName());
            e.printStackTrace();
            return;
        }
        String id = file.getName().replaceAll(".yml", "");
        String className = configuration.getString("class", "de.erethon.aether.creature.AetherBaseMob");
        Class <? extends Entity> clazz;
        try {
            clazz = (Class<? extends Entity>) Class.forName(className);
            EntityType.customEntities.put(id, Map.entry(Aether.getInstance(), clazz));
            Aether.log("Registered class mapping for " + id + " to " + className);
        } catch (ClassNotFoundException e) {
            Aether.addException("NPCData", "Error loading NPC data" + id, "Class " + className + " not found", e);
            Aether.log("Error loading NPC data " + id + ": Class " + className + " not found");
            e.printStackTrace();

        }
    }

    private static void loadEarlySub(File file) {
        for (File f : file.listFiles()){
            if (f.getName().contains("disabled")) {
                continue;
            }
            if (f.isDirectory()) {
                loadEarlySub(f);
                continue;
            }
            loadEarlyNPCFile(f);
        }
    }

}
