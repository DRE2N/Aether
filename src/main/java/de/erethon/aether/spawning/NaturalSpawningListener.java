package de.erethon.aether.spawning;

import de.erethon.aether.Aether;
import de.erethon.papyrus.events.MobSpawnFinalizedEvent;
import de.erethon.papyrus.events.MobSpawnForBiomeEvent;
import de.erethon.papyrus.events.PreCategorySpawnEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class NaturalSpawningListener implements Listener {

    private final Aether plugin = Aether.getInstance();


    @EventHandler
    private void onFinalizeSpawn(MobSpawnFinalizedEvent event) {

    }

    @EventHandler
    private void onSelect(MobSpawnForBiomeEvent event) {

    }

    @EventHandler
    private void preCategorySpawn(PreCategorySpawnEvent event) {

    }
}
