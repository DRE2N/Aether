package de.erethon.aether.creature;

import de.erethon.aether.Aether;

import java.util.HashSet;
import java.util.Set;

public class SkinCache {

    Aether plugin = Aether.getInstance();
    Set<Skin> skins = new HashSet<>();

    public Skin get(int id) {
        for (Skin skin : skins) {
            if (skin.id() == id) {
                return skin;
            }
        }
        fetch(id);
        return null;
    }

    public void fetch(int id) {
        MineSkinFetcher.fetchSkinFromIdAsync(id, skinData -> skins.add(skinData));
    }

    public void refresh() {
        for (NPCData npcData : plugin.getCreatureManager().getCreatures()) {
            fetch(npcData.getSkinID());
        }
    }
}
