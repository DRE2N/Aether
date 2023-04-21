package de.erethon.aether;

import de.erethon.aether.commands.CommandCache;
import de.erethon.aether.creature.ActiveCreatureManager;
import de.erethon.aether.creature.CreatureManager;
import de.erethon.aether.creature.SkinCache;
import de.erethon.aether.listener.EntityListener;
import de.erethon.aether.listener.PlayerListener;
import de.erethon.aether.network.AetherPacketHandler;
import de.erethon.aether.spawning.SpawnerManager;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommandCache;
import de.erethon.bedrock.compatibility.Internals;
import de.erethon.bedrock.plugin.EPlugin;
import de.erethon.bedrock.plugin.EPluginSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;

public final class Aether extends EPlugin implements Listener {

    static Aether instance;

    public static File MOBDATA;
    public static File CREATURES;
    public static File SPAWNERS;
    public static File SKINS;
    NamespacedKey key = new NamespacedKey(this, "aether");

    private AetherPacketHandler packetHandler;
    ECommandCache commands;
    CreatureManager creatureManager;
    ActiveCreatureManager activeCreatureManager;
    SkinCache skinCache;
    PlayerListener playerListener;
    EntityListener entityListener;
    SpawnerManager spawnerManager;

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
        SPAWNERS = new File(getDataFolder(), "spawners");
        if (!SPAWNERS.exists()) {
            SPAWNERS.mkdir();
        }
        SKINS = new File(getDataFolder(), "skinCache.yml");
        if (!SKINS.exists()) {
            try {
                SKINS.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //npcManager = new NPCManager();
        creatureManager = new CreatureManager();
        MessageUtil.log("Available creatures: ");
        String creatures = "";
        for (EntityType entityType : BuiltInRegistries.ENTITY_TYPE.stream().distinct().toList()) {
            creatures = creatures + entityType.getDescriptionId() + ", ";
        }
        MessageUtil.log(creatures);
        activeCreatureManager = new ActiveCreatureManager();
        playerListener = new PlayerListener();
        entityListener = new EntityListener();
        skinCache = new SkinCache(SKINS);
        skinCache.refresh();


        //Bukkit.getPluginManager().registerEvents(npcManager, this);
        Bukkit.getPluginManager().registerEvents(playerListener, this);
        Bukkit.getPluginManager().registerEvents(entityListener, this);

        commands = new CommandCache(this);
        setCommandCache(commands);
        commands.register(this);
        spawnerManager = new SpawnerManager();
        spawnerManager.loadSpawners();
        spawnerManager.startSpawning();


        //npcManager.loadFiles();
        System.setProperty("net.kyori.adventure.text.warnWhenLegacyFormattingDetected", "false");
        MessageUtil.log("Warn for legacy formatting: " + System.getProperty("net.kyori.adventure.text.warnWhenLegacyFormattingDetected"));

    }

    @Override
    public void onDisable() {
        skinCache.saveCache();
        spawnerManager.stopSpawning();
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

    public SpawnerManager getSpawnerManager() {
        return spawnerManager;
    }

    public SkinCache getSkinCache() {
        return skinCache;
    }

    public NamespacedKey getKey() {
        return key;
    }
}
