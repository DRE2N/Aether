package de.erethon.aether.creature;

import de.erethon.aether.Aether;
import de.erethon.bedrock.chat.MessageUtil;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkinCache {

    File cacheFile;
    YamlConfiguration diskCache;
    Aether plugin = Aether.getInstance();
    Set<Skin> skins = new HashSet<>();

    public SkinCache(File cacheFile) {
        this.cacheFile = cacheFile;
        diskCache = YamlConfiguration.loadConfiguration(cacheFile);
        loadCache();
    }

    public Skin get(String link) {
        for (Skin skin : skins) {
            if (skin.link().equals(link)) {
                return skin;
            }
        }
        fetch(link);
        return null;
    }

    public void fetch(String link) {
        MineSkinFetcher.fetchSkinFromIdAsync(link, skinData -> skins.add(skinData));
    }

    public void refresh() {
        for (NPCData npcData : plugin.getCreatureManager().getCreatures()) {
            fetch(npcData.getSkinLink());
        }
    }

    public void loadCache() {
        for (String s : diskCache.getStringList("skins")) {
            String[] split = s.split(";");
            skins.add(new Skin(split[0], split[1], split[2]));
        }
        MessageUtil.log("Loaded " + skins.size() + " skins from local cache.");
    }

    public void saveCache() {
        List<String> skinsList = new ArrayList<>();
        for (Skin skin : skins) {
            skinsList.add(skin.link() + ";" + skin.texture() + ";" + skin.signature());
        }
        diskCache.set("skins", skinsList);
        try {
            diskCache.save(cacheFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
