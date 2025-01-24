package de.erethon.aether;

import de.erethon.aether.commands.CommandCache;
import de.erethon.aether.creature.ActiveCreatureManager;
import de.erethon.aether.creature.ActiveNPC;
import de.erethon.aether.creature.CreatureManager;
import de.erethon.aether.creature.SkinCache;
import de.erethon.aether.creature.SynchedDataMappings;
import de.erethon.aether.listener.EntityListener;
import de.erethon.aether.listener.PlayerListener;
import de.erethon.aether.models.ModelRegistry;
import de.erethon.aether.models.ModelViewPersistenceHandlerImpl;
import de.erethon.aether.network.AetherPacketHandler;
import de.erethon.aether.spawning.SpawnerManager;
import de.erethon.aether.tools.ErrorEntry;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommandCache;
import de.erethon.bedrock.compatibility.Internals;
import de.erethon.bedrock.plugin.EPlugin;
import de.erethon.bedrock.plugin.EPluginSettings;
import de.erethon.hephaestus.Hephaestus;
import de.erethon.hephaestus.items.HItemLibrary;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.hephaestus.bukkit.BukkitModelEngine;
import team.unnamed.hephaestus.bukkit.BukkitModelEngineAdapt;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.key.Key;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.DimensionTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class Aether extends EPlugin implements Listener {

    static Aether instance;

    public static File MOBDATA;
    public static File MODELS;
    public static File CREATURES;
    public static File SPAWNERS;
    public static File SKINS;
    NamespacedKey key = new NamespacedKey(this, "aether");

    ECommandCache commands;
    CreatureManager creatureManager;
    ActiveCreatureManager activeCreatureManager;
    SkinCache skinCache;
    PlayerListener playerListener;
    EntityListener entityListener;
    SpawnerManager spawnerManager;

    final ModelRegistry modelRegistry = new ModelRegistry();
    private BukkitModelEngine engine;

    private Hephaestus hephaestusPlugin;
    private HItemLibrary hitemLibrary;

    private static final List<ErrorEntry> errors = new ArrayList<>();

    public static final byte[] AIR_SECTION = new byte[]{0, 0, 0, 0, 0, 0, 39, 0};
    public static final int ADDED_SECTIONS = 256;

    public Aether() {
        settings = EPluginSettings.builder()
                .economy(true)
                .internals(Internals.v1_16_R3)
                .build();
    }

    public static List<ErrorEntry> getErrors() {
        return errors;
    }

    public static void addException(String locationIdentifier, String errorMessage,  String friendlyMessage, @Nullable Exception e) {
        if (e == null) {
            errors.add(new ErrorEntry(locationIdentifier, friendlyMessage, errorMessage, null));
            MessageUtil.log(errorMessage);
            return;
        }
        errors.add(new ErrorEntry(locationIdentifier, friendlyMessage, errorMessage, e.getStackTrace()));
        MessageUtil.log(errorMessage);
        e.printStackTrace();
    }


    @Override
    public void onLoad() {
        hephaestusPlugin = (Hephaestus) Bukkit.getPluginManager().getPlugin("Hephaestus");
        hitemLibrary = hephaestusPlugin.getLibrary();
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

        MODELS = new File(getDataFolder(), "models");
        if (!MODELS.exists()) {
            MODELS.mkdir();
        }
        modelRegistry.loadFromFolder(MODELS);
        engine = BukkitModelEngineAdapt.create(this, new ModelViewPersistenceHandlerImpl(modelRegistry));


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
        createEntityMappings();
        //npcManager = new NPCManager();
        creatureManager = new CreatureManager();
        activeCreatureManager = new ActiveCreatureManager();
        playerListener = new PlayerListener();
        entityListener = new EntityListener();
        skinCache = new SkinCache(SKINS);


        //Bukkit.getPluginManager().registerEvents(npcManager, this);
        Bukkit.getPluginManager().registerEvents(playerListener, this);
        Bukkit.getPluginManager().registerEvents(entityListener, this);

        commands = new CommandCache(this);
        setCommandCache(commands);
        commands.register(this);
        spawnerManager = new SpawnerManager();
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getServer().getPluginManager().registerEvents(this, this);
    }


    @Override
    public void onDisable() {
        skinCache.saveCache();
    }
    
    public void reload() {
        errors.clear();
        creatureManager.reload();
        spawnerManager.reloadSpawners();
        if (errors.isEmpty()) {
            return;
        }
        MessageUtil.broadcastMessageIf("<red>Encountered errors during Aether reload: ", player -> player.hasPermission("aether.admin"));
        for (ErrorEntry error : errors) {
            sendNiceErrorToAdmins(error);
        }
    }

    @EventHandler
    private void onFinishStartup(ServerLoadEvent event) {
        if (event.getType() != ServerLoadEvent.LoadType.STARTUP) {
            return;
        }
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                creatureManager.loadDelayed();
            }
        };
        runnable.runTaskLater(this, 60);
        MessageUtil.broadcastMessageIf("<green>Aether has started up and is now ready.", player -> player.hasPermission("aether.admin"));
    }
    
    private void sendNiceErrorToAdmins(ErrorEntry entry) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("aether.admin")) {
                Component dash = Component.text("- ", NamedTextColor.DARK_GRAY);
                Component hover = getHoverComponent(entry);
                Component error = Component.text(entry.errorMessage(), NamedTextColor.RED).hoverEvent(hover);
                Component message = dash.append(error).append(Component.text(" at " + entry.locationIdentifier(), NamedTextColor.GRAY));
                player.sendMessage(message);
            }
        }
    }

    private static @NotNull Component getHoverComponent(ErrorEntry entry) {
        Component stackTrace = Component.empty();
        if (entry.stackFrames() == null) {
            return  Component.text("Hint: ", NamedTextColor.GRAY).append(Component.text(entry.friendlyMessage(), NamedTextColor.RED));
        }
        int maxDepth = 5;
        for (StackTraceElement element : entry.stackFrames()) {
            stackTrace = stackTrace.append(Component.text(element.toString(), NamedTextColor.GRAY).append(Component.newline()));
            maxDepth--;
            if (maxDepth == 0) {
                break;
            }
        }
        return Component.text("Hint: ", NamedTextColor.GRAY).append(Component.text(entry.friendlyMessage(), NamedTextColor.RED)).append(Component.newline()).append(Component.newline()).append(Component.text("Stacktrace: ", NamedTextColor.GRAY)).append(Component.newline()).append(stackTrace);
    }

    private void createEntityMappings() {
        World world = Bukkit.getWorlds().get(0);
        CraftWorld craftWorld = (CraftWorld) world;
        SynchedDataMappings.generateMappings(craftWorld.getHandle());
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

    public ModelRegistry getModelRegistry() {
        return modelRegistry;
    }

    public BukkitModelEngine getModelEngine() {
        return engine;
    }

    public HItemLibrary getItemLibrary() {
        return hitemLibrary;
    }

    public NamespacedKey getKey() {
        return key;
    }
}
