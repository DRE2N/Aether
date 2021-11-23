package de.erethon.aether;

import de.erethon.aether.creature.*;
import de.erethon.aether.listener.AEPacketListener;
import de.erethon.aether.listener.EntityListener;
import de.erethon.aether.listener.PlayerListener;
import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.compatibility.Internals;
import de.erethon.commons.javaplugin.DREPlugin;
import de.erethon.commons.javaplugin.DREPluginSettings;
import de.erethon.aether.commands.CommandCache;
import io.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;

import java.io.File;

public final class Aether extends DREPlugin {

    static Aether instance;
    public static File MOBDATA;
    public static File CREATURES;
    NamespacedKey key = new NamespacedKey(this, "aether");
    CommandCache commands;
    CreatureManager creatureManager;
    ActiveCreatureManager activeCreatureManager;
    SkinCache skinCache;
    NPCInstancing npcInstancing;
    PlayerListener playerListener;
    AEPacketListener packetListener;
    EntityListener entityListener;

    public Aether() {
        settings = DREPluginSettings.builder()
                .paper(true)
                .economy(true)
                .internals(Internals.v1_16_R3)
                .build();
    }

    @Override
    public void onLoad() {
        PacketEvents.create(this);
        PacketEvents.get().load();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!compat.isPaper()) {
            MessageUtil.log("Please use Paper. https://papermc.io/");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        CREATURES = new File(getDataFolder(), "creatures");
        if (!CREATURES.exists()) {
            CREATURES.mkdir();
        }
        //npcManager = new NPCManager();
        creatureManager = new CreatureManager();
        activeCreatureManager = new ActiveCreatureManager();
        playerListener = new PlayerListener();
        entityListener = new EntityListener();
        skinCache = new SkinCache();
        skinCache.refresh();

        npcInstancing = new NPCInstancing();

        packetListener = new AEPacketListener();
        PacketEvents.get().registerListener(packetListener);
        PacketEvents.get().init();

        //Bukkit.getPluginManager().registerEvents(npcManager, this);
        Bukkit.getPluginManager().registerEvents(playerListener, this);
        Bukkit.getPluginManager().registerEvents(entityListener, this);

        commands = new CommandCache(this);
        setCommandCache(commands);
        commands.register(this);

        //npcManager.loadFiles();

    }

    @Override
    public void onDisable() {
        activeCreatureManager.clearHealthBars();
        PacketEvents.get().terminate();
    }

    public static void debug(String string) {
        Aether.getInstance().getLogger().info(string);
    }

    public static Aether getInstance() {
        return instance;
    }

    public ActiveCreatureManager getActiveCreatureManager() {
        return activeCreatureManager;
    }

    public CreatureManager getCreatureManager() {
        return creatureManager;
    }

    public NPCInstancing getNpcInstancing() {
        return npcInstancing;
    }

    public SkinCache getSkinCache() {
        return skinCache;
    }

    public NamespacedKey getKey() {
        return key;
    }
}
