package de.erethon.aether.creature;

import de.erethon.aether.Aether;
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
    private String authToken = "";

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
        MineSkinFetcher.fetchSkinFromIdAsync(link, skinData ->  {
            if (skinData == null) {
                return;
            }
            skins.add(new Skin(link, skinData.texture(), skinData.signature()));
            MineSkinFetcher.skinsInQueue.remove(link);
            saveCache();
        });
    }

    public void refresh() {
        for (NPCData npcData : plugin.getCreatureManager().getCreatures()) {
            fetch(npcData.getSkinLink());
        }
    }

    public String getAuthToken() {
        return authToken;
    }

    public void loadCache() {
        authToken = diskCache.getString("mineskin-auth");
        Aether.log("Raw auth token from config: '" + authToken + "'");
        if (authToken == null) {
            authToken = "";
            Aether.log("Auth token was null, setting to empty string");
        } else {
            authToken = authToken.trim(); // Trim whitespace
            Aether.log("Auth token loaded and trimmed: '" + authToken + "' (length: " + authToken.length() + ")");
        }
        for (String s : diskCache.getStringList("skins")) {
            if (s == null || s.trim().isEmpty()) {
                continue;
            }
            String[] split = s.split(";");
            if (split.length != 3) {
                Aether.log("Skipping malformed skin entry: " + s);
                continue;
            }
            skins.add(new Skin(split[0], split[1], split[2]));
        }
        Aether.log("Loaded " + skins.size() + " skins from local cache.");
    }

    public void saveCache() {
        List<String> skinsList = new ArrayList<>();
        for (Skin skin : skins) {
            skinsList.add(skin.link() + ";" + skin.texture() + ";" + skin.signature());
        }
        diskCache.set("skins", skinsList);
        // Preserve the auth token when saving
        if (authToken != null && !authToken.isEmpty()) {
            diskCache.set("mineskin-auth", authToken);
        }
        try {
            diskCache.save(cacheFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
