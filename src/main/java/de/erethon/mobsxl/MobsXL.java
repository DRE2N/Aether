package de.erethon.mobsxl;

import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.compatibility.Internals;
import de.erethon.commons.javaplugin.DREPlugin;
import de.erethon.commons.javaplugin.DREPluginSettings;
import de.erethon.mobsxl.commands.CommandCache;
import de.erethon.mobsxl.npc.NPCManager;
import org.bukkit.Bukkit;

import java.io.File;

public final class MobsXL extends DREPlugin {

    static MobsXL instance;
    public static File MOBDATA;
    CommandCache commands;
    NPCManager npcManager;
    PlayerListener playerListener;

    public MobsXL() {
        settings = DREPluginSettings.builder()
                .paper(true)
                .economy(true)
                .internals(Internals.v1_16_R3)
                .build();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!compat.isPaper()) {
            MessageUtil.log("Please use Paper. https://papermc.io/");
            Bukkit.getPluginManager().disablePlugin(this);
        }
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        MOBDATA = new File(getDataFolder(), "mobs");
        if (!MOBDATA.exists()) {
            MOBDATA.mkdir();
        }

        //npcManager = new NPCManager();
        playerListener = new PlayerListener();

        //Bukkit.getPluginManager().registerEvents(npcManager, this);
        Bukkit.getPluginManager().registerEvents(playerListener, this);

        commands = new CommandCache(this);
        setCommandCache(commands);
        commands.register(this);

        //npcManager.loadFiles();

    }

    @Override
    public void onDisable() {
    }

    public static void debug(String string) {
        MobsXL.getInstance().getLogger().info(string);
    }

    public static MobsXL getInstance() {
        return instance;
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }
}
