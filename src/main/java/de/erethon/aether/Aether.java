package de.erethon.aether;

import de.erethon.aether.creature.*;
import de.erethon.aether.listener.AEPacketListener;
import de.erethon.aether.listener.EntityListener;
import de.erethon.aether.listener.PlayerListener;
import de.erethon.aether.tools.UpdatedMessageUtil;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommandCache;
import de.erethon.bedrock.compatibility.Internals;
import de.erethon.bedrock.plugin.EPlugin;
import de.erethon.bedrock.plugin.EPluginSettings;
import de.erethon.aether.commands.CommandCache;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;

import java.io.File;

public final class Aether extends EPlugin {

    static Aether instance;
    public static File MOBDATA;
    public static File CREATURES;
    NamespacedKey key = new NamespacedKey(this, "aether");
    ECommandCache commands;
    CreatureManager creatureManager;
    ActiveCreatureManager activeCreatureManager;
    SkinCache skinCache;
    PlayerListener playerListener;
    AEPacketListener packetListener;
    EntityListener entityListener;

    public Aether() {
        settings = EPluginSettings.builder()
                .economy(true)
                .internals(Internals.v1_16_R3)
                .build();
    }

    @Override
    public void onLoad() {
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


        packetListener = new AEPacketListener();

        //Bukkit.getPluginManager().registerEvents(npcManager, this);
        Bukkit.getPluginManager().registerEvents(playerListener, this);
        Bukkit.getPluginManager().registerEvents(entityListener, this);

        commands = new CommandCache(this);
        setCommandCache(commands);
        commands.register(this);

        //npcManager.loadFiles();
        System.setProperty("net.kyori.adventure.text.warnWhenLegacyFormattingDetected", "false");
        MessageUtil.log("Warn for legacy formatting: " + System.getProperty("net.kyori.adventure.text.warnWhenLegacyFormattingDetected"));

    }

    @Override
    public void onDisable() {
        activeCreatureManager.clearHealthBars();
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

    public SkinCache getSkinCache() {
        return skinCache;
    }

    public NamespacedKey getKey() {
        return key;
    }
}
