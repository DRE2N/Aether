package de.erethon.aether;

import de.erethon.aether.commands.CommandCache;
import de.erethon.aether.combat.MobAttributeCurve;
import de.erethon.aether.creature.ActiveCreatureManager;
import de.erethon.aether.creature.CreatureManager;
import de.erethon.aether.creature.SkinCache;
import de.erethon.aether.listener.EntityListener;
import de.erethon.aether.listener.PlayerListener;
import de.erethon.aether.qxl.actions.HideMobAction;
import de.erethon.aether.qxl.actions.RemoveMobAction;
import de.erethon.aether.qxl.actions.WalkMobAction;
import de.erethon.aether.qxl.actions.ShowMobAction;
import de.erethon.aether.qxl.actions.SpawnMobAction;
import de.erethon.aether.qxl.actions.SpawnerAction;
import de.erethon.aether.qxl.conditions.CharacterLevelCondition;
import de.erethon.aether.qxl.objectives.DeliverItemObjective;
import de.erethon.aether.qxl.objectives.InteractMobObjective;
import de.erethon.aether.qxl.objectives.KillMobObjective;
import de.erethon.aether.qxl.objectives.TimedDefenseObjective;
import de.erethon.aether.spawning.MobSpawnConfig;
import de.erethon.aether.spawning.NaturalSpawningListener;
import de.erethon.aether.spawning.SpawnerManager;
import de.erethon.aether.tools.ErrorEntry;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.command.ECommandCache;
import de.erethon.bedrock.compatibility.Internals;
import de.erethon.bedrock.plugin.EPlugin;
import de.erethon.bedrock.plugin.EPluginSettings;
import de.erethon.hephaestus.Hephaestus;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QRegistries;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    NaturalSpawningListener naturalSpawningListener;
    SpawnerManager spawnerManager;
    MobSpawnConfig mobSpawnConfig;
    private Map<Holder<Attribute>, MobAttributeCurve> mobAttributeCurves = Map.of();
    private static final String MOB_ATTRIBUTE_CURVES_FILE = "mob-attribute-curves.yml";

    private Hephaestus hephaestusPlugin;
    private HItemLibrary hitemLibrary;

    private static final List<ErrorEntry> errors = new ArrayList<>();

    public static boolean AETHER_DEBUG_MODE = false;

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
            Aether.log(errorMessage);
            return;
        }
        errors.add(new ErrorEntry(locationIdentifier, friendlyMessage, errorMessage, e.getStackTrace()));
        Aether.log(errorMessage);
        e.printStackTrace();
    }


    @Override
    public void onLoad() {
        instance = this;
        hephaestusPlugin = (Hephaestus) Bukkit.getPluginManager().getPlugin("Hephaestus");
        hitemLibrary = hephaestusPlugin.getLibrary();
        CREATURES = new File(getDataFolder(), "creatures");
        if (!CREATURES.exists()) {
            CREATURES.mkdir();
        }
        CreatureManager.loadEarly(CREATURES); // We need to register class mappings before the world is loaded
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!compat.isPaper()) {
            Aether.log("Please use Paper. https://papermc.io/");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        MODELS = new File(getDataFolder(), "models");
        if (!MODELS.exists()) {
            MODELS.mkdir();
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
        loadMobAttributeCurves();
        creatureManager = new CreatureManager();
        activeCreatureManager = new ActiveCreatureManager();
        playerListener = new PlayerListener();
        entityListener = new EntityListener(this);
        skinCache = new SkinCache(SKINS);
        spawnerManager = new SpawnerManager();
        mobSpawnConfig = new MobSpawnConfig();
        if (Bukkit.getPluginManager().isPluginEnabled("Factions")) {
            naturalSpawningListener = new NaturalSpawningListener(mobSpawnConfig);
            Bukkit.getPluginManager().registerEvents(naturalSpawningListener, this);
        } else {
            getLogger().warning("Factions is unavailable; region-aware natural spawning is disabled.");
        }


        //Bukkit.getPluginManager().registerEvents(npcManager, this);
        Bukkit.getPluginManager().registerEvents(playerListener, this);
        Bukkit.getPluginManager().registerEvents(entityListener, this);

        commands = new CommandCache(this);
        setCommandCache(commands);
        commands.register(this);
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onFinishStartup(ServerLoadEvent event) {
                if (event.getType() == ServerLoadEvent.LoadType.STARTUP) {
                    loadDelayedCreatures();
                }
            }
        }, this);
        QuestsXL questsXL = (QuestsXL) Bukkit.getPluginManager().getPlugin("QuestsXL");
        if (questsXL != null) {
            questsXL.registerComponent(QRegistries.ACTIONS, "spawn_mob", SpawnMobAction::new);
            questsXL.registerComponent(QRegistries.ACTIONS, "spawner", SpawnerAction::new);
            questsXL.registerComponent(QRegistries.ACTIONS, "walk_mob", WalkMobAction::new);
            questsXL.registerComponent(QRegistries.ACTIONS, "show_mob", ShowMobAction::new);
            questsXL.registerComponent(QRegistries.ACTIONS, "hide_mob", HideMobAction::new);
            questsXL.registerComponent(QRegistries.ACTIONS, "remove_mob", RemoveMobAction::new);
            questsXL.registerComponent(QRegistries.CONDITIONS, "character_level", CharacterLevelCondition::new);
            questsXL.registerComponent(QRegistries.OBJECTIVES, "deliver_item", DeliverItemObjective::new);
            questsXL.registerComponent(QRegistries.OBJECTIVES, "kill_mob", KillMobObjective::new);
            questsXL.registerComponent(QRegistries.OBJECTIVES, "timed_defense", TimedDefenseObjective::new);
            questsXL.registerComponent(QRegistries.OBJECTIVES, "interact_mob", InteractMobObjective::new);
        }
    }


    @Override
    public void onDisable() {
        skinCache.saveCache();
    }
    
    public void reload() {
        errors.clear();
        loadMobAttributeCurves();
        creatureManager.reload();
        reloadMobSpawns();
        spawnerManager.reloadSpawners();
        skinCache.refresh();
        if (errors.isEmpty()) {
            return;
        }
        MessageUtil.broadcastMessageIf("<red>Encountered errors during Aether reload: ", player -> player.hasPermission("aether.admin"));
        for (ErrorEntry error : errors) {
            sendNiceErrorToAdmins(error);
        }
    }

    public Map<Holder<Attribute>, MobAttributeCurve> getMobAttributeCurves() {
        return mobAttributeCurves;
    }

    private void loadMobAttributeCurves() {
        try (var reader = new InputStreamReader(getResource(MOB_ATTRIBUTE_CURVES_FILE), StandardCharsets.UTF_8)) {
            YamlConfiguration bundled = new YamlConfiguration();
            bundled.load(reader);
            Map<Holder<Attribute>, MobAttributeCurve> curves = MobAttributeCurve.load(bundled);
            if (mobAttributeCurves.isEmpty()) {
                mobAttributeCurves = Map.copyOf(curves);
            }
            File file = new File(getDataFolder(), MOB_ATTRIBUTE_CURVES_FILE);
            if (!file.exists()) {
                saveResource(MOB_ATTRIBUTE_CURVES_FILE, false);
            }
            YamlConfiguration config = new YamlConfiguration();
            config.load(file);
            curves.putAll(MobAttributeCurve.load(config));
            mobAttributeCurves = Map.copyOf(curves);
        } catch (IOException | InvalidConfigurationException | IllegalArgumentException e) {
            addException(MOB_ATTRIBUTE_CURVES_FILE, "Could not load mob attribute curves: " + e.getMessage(),
                    "Correct the curve configuration and reload Aether", null);
        }
    }

    private void loadDelayedCreatures() {
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

    public HItemLibrary getItemLibrary() {
        return hitemLibrary;
    }

    public void registerTranslation(String key, Locale locale, String translation) {
        hephaestusPlugin.getTranslationManager().registerTranslation(key, locale, translation);
        Aether.log("Registered translation " + key + " for locale " + locale + " with text: " + translation);
    }

    public MobSpawnConfig getMobSpawnConfig() {
        return mobSpawnConfig;
    }

    public void reloadMobSpawns() {
        mobSpawnConfig = new MobSpawnConfig();
        if (naturalSpawningListener != null) {
            HandlerList.unregisterAll(naturalSpawningListener);
            naturalSpawningListener = null;
        }
        if (Bukkit.getPluginManager().isPluginEnabled("Factions")) {
            naturalSpawningListener = new NaturalSpawningListener(mobSpawnConfig);
            Bukkit.getPluginManager().registerEvents(naturalSpawningListener, this);
        }
    }


    public NamespacedKey getKey() {
        return key;
    }
    
    public static void log(String msg) {
        getInstance().getLogger().info(msg);
    }
}
