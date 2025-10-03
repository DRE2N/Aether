package de.erethon.aether.creature;

import de.erethon.aether.Aether;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SkinCache {

    File cacheFile;
    YamlConfiguration diskCache;
    Aether plugin = Aether.getInstance();
    Map<String, Skin> skins = new ConcurrentHashMap<>(); // Changed to Map for faster lookups
    Set<String> pendingSkins = Collections.synchronizedSet(new HashSet<>()); // Track skins being fetched
    private String authToken = "";

    public SkinCache(File cacheFile) {
        this.cacheFile = cacheFile;
        diskCache = YamlConfiguration.loadConfiguration(cacheFile);
        loadCache();
    }

    public Skin get(String link) {
        if (link == null || link.isEmpty()) {
            return null;
        }

        // Check if we have it cached
        Skin cached = skins.get(link);
        if (cached != null) {
            return cached;
        }

        // Don't fetch if already pending
        if (!pendingSkins.contains(link) && !MineSkinFetcher.skinsInQueue.contains(link)) {
            fetch(link);
        }

        return null; // Skin not available yet
    }

    public void fetch(String link) {
        if (link == null || link.isEmpty()) {
            return;
        }

        // Check if already cached or being fetched
        if (skins.containsKey(link)) {
            return;
        }

        if (pendingSkins.contains(link)) {
            return;
        }

        pendingSkins.add(link);
        Aether.log("Initiating skin fetch for: " + link);

        MineSkinFetcher.fetchSkinFromIdAsync(link, new MineSkinFetcher.Callback() {
            @Override
            public void call(Skin skinData) {
                if (skinData == null) {
                    pendingSkins.remove(link);
                    return;
                }
                skins.put(link, skinData);
                pendingSkins.remove(link);
                saveCache();
                Aether.log("Skin cached and saved: " + link);
            }

            @Override
            public void failed(String url) {
                pendingSkins.remove(url);
                Aether.log("Skin fetch failed permanently: " + url);
            }
        });
    }

    public void refresh() {
        List<String> toRefresh = new ArrayList<>();
        for (NPCData npcData : plugin.getCreatureManager().getCreatures()) {
            String skinLink = npcData.getSkinLink();
            if (skinLink != null && !skinLink.isEmpty() && !skins.containsKey(skinLink)) {
                toRefresh.add(skinLink);
            }
        }

        Aether.log("Refreshing " + toRefresh.size() + " uncached skins...");
        for (String link : toRefresh) {
            fetch(link);
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

        List<String> skinsList = diskCache.getStringList("skins");
        for (String s : skinsList) {
            if (s == null || s.trim().isEmpty()) {
                continue;
            }
            String[] split = s.split(";");
            if (split.length != 3) {
                Aether.log("Skipping malformed skin entry: " + s);
                continue;
            }
            skins.put(split[0], new Skin(split[0], split[1], split[2]));
        }
        Aether.log("Loaded " + skins.size() + " skins from local cache.");
    }

    public void saveCache() {
        List<String> skinsList = new ArrayList<>();
        for (Skin skin : skins.values()) {
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
