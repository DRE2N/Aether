package de.erethon.aether.creature;

import de.erethon.aether.Aether;
import de.erethon.aether.ai.GoalLoader;
import de.erethon.aether.ai.goals.AEPathfinderGoal;
import de.erethon.aether.combat.SpellCastEntry;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.spellbook.api.SpellLibrary;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class NPCData {

    private static final Aether plugin = Aether.getInstance();

    // General
    private String ID;
    private int currentVersion = 0;
    private boolean isValid = false;
    ConfigurationSection cfg;
    private Class entityClass;
    private EntityType displayType;
    private Component displayName = Component.text("NPC");
    private boolean instancable = true;
    private boolean hasCollision = true;
    private boolean persistent = true;
    private boolean invulnerable;
    private boolean glowing;
    private boolean gravity = true;
    private boolean nameTagVisible;
    private int noDamageTicks;
    private int maximumAir;

    private String modelID = "";

    // Players
    private String skinLink = "https://minesk.in/c39b10d504e0453788d0c81ec9ab970e";
    private List<String> skins = new ArrayList<>();
    private int skinLayers = 127;
    Random random = new Random();

    // Attributes
    private final Map<Holder<Attribute>, Double> attributes = new HashMap<>();

    // Equipment
    private HItem mainHand;
    private HItem offHand;
    private HItem helmet;
    private HItem chest;
    private HItem leggings;
    private HItem boots;

    // Sounds & Messages
    private Sound attackSound;
    private Sound hurtSound = null;
    private Sound ambientSound = null;
    private Sound shootSound = null;
    private Sound deathSound = null;
    private List<String> ambientMessages = new ArrayList<>();
    private boolean randomTalker = false;

    // Combat
    String faction;
    EntityType projectile;
    BoundingBox hitbox;
    private BlockPos homeLocation;
    private int homeRange;

    private final Set<SpellCastEntry> onDamagedSpells = new HashSet<>();
    private final Set<SpellCastEntry> onTimerSpells = new HashSet<>();
    private final Set<SpellCastEntry> onAttackSpells = new HashSet<>();
    private final Set<SpellCastEntry> onDeathSpells = new HashSet<>();
    private final Set<SpellCastEntry> onTargetSpells = new HashSet<>();

    // AI
    private Set<AEPathfinderGoal> goals = new HashSet<>();
    private Set<AEPathfinderGoal> targets = new HashSet<>();

    // Loot
    private int dropXP = 0;
    private Set<HItem> loot = new HashSet<>();

    public NPCData(EntityType<?> displayType) {
        this.displayType = displayType;
    }

    public NPCData(ConfigurationSection cfg, String id) {
        this.cfg = cfg;
        this.ID = id;
        load();
    }

    public AetherBaseMob spawn(Location location) {
        if (!isValid) {
            MessageUtil.log("Tried to spawn invalid NPC " + ID);
            return null;
        }
        return new AetherBaseMob(this, location.getWorld());
    }

    public @NotNull EntityType<?> getDisplayType() {
        return displayType;
    }

    public void setDisplayType(EntityType<?> displayType) {
        this.displayType = displayType;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public boolean isInstancable() {
        return instancable;
    }

    public int getCurrentVersion() {
        return currentVersion;
    }

    public boolean isValid() {
        return isValid;
    }

    public String getID() {
        return ID;
    }

    public ConfigurationSection getCfg() {
        return cfg;
    }

    public BoundingBox getHitbox() {
        return hitbox;
    }

    public double getAttribute(Holder<Attribute> attribute) {
        return attributes.getOrDefault(attribute, 0.0);
    }

    public Map<Holder<Attribute>, Double> getAttributes() {
        return attributes;
    }

    public HItem getMainHand() {
        return mainHand;
    }

    public HItem getOffHand() {
        return offHand;
    }

    public HItem getHelmet() {
        return helmet;
    }

    public HItem getChest() {
        return chest;
    }

    public HItem getLeggings() {
        return leggings;
    }

    public HItem getBoots() {
        return boots;
    }

    public EntityType getProjectile() {
        return projectile;
    }


    public String getFaction() {
        return faction;
    }

    public Sound getAttackSound() {
        return attackSound;
    }

    public Sound getAmbientSound() {
        return ambientSound;
    }

    public Sound getShootSound() {
        return shootSound;
    }

    public Sound getDeathSound() {
        return deathSound;
    }

    public Sound getHurtSound() {
        return hurtSound;
    }

    public int getDropXP() {
        return dropXP;
    }

    public Set<HItem> getLoot() {
        return loot;
    }

    public boolean hasCollision() {
        return hasCollision;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public boolean isGravity() {
        return gravity;
    }

    public boolean isNameTagVisible() {
        return nameTagVisible;
    }

    public int getNoDamageTicks() {
        return noDamageTicks;
    }

    public int getMaximumAir() {
        return maximumAir;
    }

    public String getSkinLink() {
        if (skins.isEmpty()) {
            return skinLink;
        }
        int index = random.nextInt(0, skins.size());
        return skins.get(index);
    }

    public Set<AEPathfinderGoal> getGoals() {
        return goals;
    }

    public Set<AEPathfinderGoal> getTargets() {
        return targets;
    }

    public Set<SpellCastEntry> getOnDamagedSpells() {
        return onDamagedSpells;
    }

    public Set<SpellCastEntry> getOnTimerSpells() {
        return onTimerSpells;
    }

    public Set<SpellCastEntry> getOnAttackSpells() {
        return onAttackSpells;
    }

    public Set<SpellCastEntry> getOnDeathSpells() {
        return onDeathSpells;
    }

    public Set<SpellCastEntry> getOnTargetSpells() {
        return onTargetSpells;
    }

    public BlockPos getHomeLocation() {
        return homeLocation;
    }

    public int getHomeRange() {
        return homeRange;
    }

    public String getModelID() {
        return modelID;
    }

    public boolean hasModel() {
        return !modelID.equals("");
    }

    public void load() {
        MessageUtil.log("Loading npc " + ID);
        // Loading
        String classString = cfg.getString("class", "de.erethon.aether.creature.AetherBaseMob");
        try {
            Map.Entry<Plugin, Class<? extends Entity >> entry = Map.entry(plugin, (Class<? extends Entity>) Class.forName(classString));
            EntityType.customEntities.put(ID, entry);
        } catch (ClassNotFoundException e) {
            Aether.addException(ID, "Could not find class " + classString, "Ensure the class exists", null);
            return;
        }
        // General
        displayName = MiniMessage.miniMessage().deserialize(cfg.getString("displayName", "NPC"));
        currentVersion = cfg.getInt("version", 0);
        try {
            displayType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("minecraft", cfg.getString("displayType", "pig")));
        } catch (Exception e) {
            Aether.addException(ID, "Could not find displayType " + cfg.getString("displayType", "pig"), "Ensure the displayType exists in vanilla", e);
            return;
        }
        instancable = cfg.getBoolean("instancable", true);
        modelID = cfg.getString("model", "");
        hasCollision = cfg.getBoolean("config.collision", true);
        persistent = cfg.getBoolean("config.persistent", true);
        invulnerable = cfg.getBoolean("config.invulnerable", false);
        glowing = cfg.getBoolean("config.glowing", false);
        gravity = cfg.getBoolean("config.gravity", true);
        nameTagVisible = cfg.getBoolean("config.nametagVisible", false);
        noDamageTicks = cfg.getInt("config.noDamageTicks", 20);
        maximumAir = cfg.getInt("config.maximumAir", 20);
        // Players
        skinLink = cfg.getString("skin");
        if (cfg.contains("skins")) {
            skins = cfg.getStringList("skins");
            MessageUtil.log("Loaded " + skins.size() + " skins.");
        }
        // Attributes
        if (cfg.contains("attributes") && cfg.isConfigurationSection("attributes")) {
            Registry<Attribute> attributeRegistry = BuiltInRegistries.ATTRIBUTE;
            for (String key : cfg.getConfigurationSection("attributes").getKeys(false)) {
                ConfigurationSection section = cfg.getConfigurationSection("attributes." + key);
                if (section == null) {
                    Aether.addException(ID, "No configuration found for attribute " + key, "Ensure the attribute exists (see docs)", null);
                    continue;
                }
                String id = section.getString("id");
                if (id == null) {
                    Aether.addException(ID, "No id found for attribute " + key, "Ensure the section includes 'id: '", null);
                    continue;
                }
                Double value = section.getDouble("value", 0.0);
                if (value == 0.0) {
                    Aether.addException(ID, "No value found for attribute " + key, "Ensure the section includes 'value: '", null);
                    continue;
                }
                Attribute attribute;
                try {
                    attribute = attributeRegistry.get(ResourceLocation.fromNamespaceAndPath("minecraft", id));
                } catch (Exception e) {
                    Aether.addException(ID, "Could not find attribute " + id, "Ensure the attribute exists", e);
                    continue;
                }
                Holder<Attribute> attributeHolder;
                try {
                    attributeHolder = attributeRegistry.wrapAsHolder(attribute);
                } catch (Exception e) {
                    Aether.addException(ID, "Unable to find holder for attribute " + id, "Please report this error", e);
                    continue;
                }
                attributes.put(attributeHolder, value);
            }
        }
        // Equipment - Critical, return if it fails
        HItemLibrary itemLibrary = plugin.getItemLibrary();
        if (itemLibrary == null) {
            Aether.addException(ID, "No item library found", "Ensure Hephaestus is installed and working", null);
        }
        try {
            mainHand = itemLibrary.get(NamespacedKey.fromString(cfg.getString("equipment.hand", "minecraft:air")));
        } catch (Exception e) {
            Aether.addException(ID, "Could not find hand item " + cfg.getString("equipment.hand", "minecraft:air"), "Ensure the item exists in the item library", e);
        }
        try {
            offHand = itemLibrary.get(NamespacedKey.fromString(cfg.getString("equipment.offhand", "minecraft:air")));
        } catch (Exception e) {
            Aether.addException(ID, "Could not find offhand item " + cfg.getString("equipment.offhand", "minecraft:air"), "Ensure the item exists in the item library", e);
        }
        try {
            helmet = itemLibrary.get(NamespacedKey.fromString(cfg.getString("equipment.helmet", "minecraft:air")));
        } catch (Exception e) {
            Aether.addException(ID, "Could not find helmet item " + cfg.getString("equipment.helmet", "minecraft:air"), "Ensure the item exists in the item library", e);
        }
        try {
            chest = itemLibrary.get(NamespacedKey.fromString(cfg.getString("equipment.chest", "minecraft:air")));
        } catch (Exception e) {
            Aether.addException(ID, "Could not find chest item " + cfg.getString("equipment.chest", "minecraft:air"), "Ensure the item exists in the item library", e);
        }
        try {
            leggings = itemLibrary.get(NamespacedKey.fromString(cfg.getString("equipment.leggings", "minecraft:air")));
        } catch (Exception e) {
            Aether.addException(ID, "Could not find leggings item " + cfg.getString("equipment.leggings", "minecraft:air"), "Ensure the item exists in the item library", e);
        }
        try {
            boots = itemLibrary.get(NamespacedKey.fromString(cfg.getString("equipment.boots", "minecraft:air")));
        } catch (Exception e) {
            Aether.addException(ID, "Could not find boots item " + cfg.getString("equipment.boots", "minecraft:air"), "Ensure the item exists in the item library", e);
        }
        // Sounds & Messages - We can ignore these
        if (cfg.contains("interaction.sounds")) {
            try {
                attackSound = Sound.valueOf(cfg.getString("interaction.sounds.attack"));
            } catch (Exception e) {
                Aether.addException(ID, "Could not find attack sound " + cfg.getString("interaction.sounds.attack"), "Ensure the sound exists in the server", e);
            }
            try {
                ambientSound = Sound.valueOf(cfg.getString("interaction.sounds.ambient"));
            } catch (Exception e) {
                Aether.addException(ID, "Could not find ambient sound " + cfg.getString("interaction.sounds.ambient"), "Ensure the sound exists in the server", e);
            }
            try {
                shootSound = Sound.valueOf(cfg.getString("interaction.sounds.shoot"));
            } catch (Exception e) {
                Aether.addException(ID, "Could not find shoot sound " + cfg.getString("interaction.sounds.shoot"), "Ensure the sound exists in the server", e);
            }
            try {
                deathSound = Sound.valueOf(cfg.getString("interaction.sounds.death"));
            } catch (Exception e) {
                Aether.addException(ID, "Could not find death sound " + cfg.getString("interaction.sounds.death"), "Ensure the sound exists in the server", e);
            }
            try {
                hurtSound = Sound.valueOf(cfg.getString("interaction.sounds.hurt"));
            } catch (Exception e) {
                Aether.addException(ID, "Could not find hurt sound " + cfg.getString("interaction.sounds.hurt"), "Ensure the sound exists in the server", e);
            }
        }
        ambientMessages = cfg.getStringList("interaction.messages");
        randomTalker = cfg.getBoolean("interaction.randomTalker", false);
        // Combat
        faction = cfg.getString("team", null);

        if (cfg.contains("homeLocation")) {
            ConfigurationSection section = cfg.getConfigurationSection("homeLocation");
            if (section == null) {
                Aether.addException(ID, "No configuration found for homeLocation", "Ensure the configuration is correct and not empty", null);
            } else {
                homeLocation = new BlockPos(section.getInt("x"), section.getInt("y"), section.getInt("z"));
                homeRange = section.getInt("range", 32);
            }
        }

        // AI
        if (cfg.contains("ai.goals")) {
            try {
                goals = GoalLoader.loadGoals(cfg.getStringList("ai.goals"));
            } catch (Exception e) {
                Aether.addException(ID, "Error loading goals", "Ensure the goals are properly configured", e);
                MessageUtil.log("Error loading goals for " + ID);
                e.printStackTrace();
            }
        }
        if (cfg.contains("ai.targets")) {
            try {
                targets = GoalLoader.loadGoals(cfg.getStringList("ai.targets"));
            } catch (Exception e) {
                Aether.addException(ID, "Error loading targets", "Ensure the targets are properly configured", e);
                MessageUtil.log("Error loading targets for " + ID);
                e.printStackTrace();
            }
        }

        // Loot
        dropXP = cfg.getInt("loot.xp", 0);
        if (cfg.contains("loot.items")) {
            Set<HItem> loot = new HashSet<>();
            for (String s : cfg.getStringList("loot.items")) {
                String[] split = s.split(";");
                HItem hitem = plugin.getItemLibrary().get(NamespacedKey.fromString(split[0]));
                if (hitem == null) {
                    Aether.addException(ID, "Could not find item " + split[0], "Ensure the item exists in the item library", null);
                    return;
                }
                loot.add(hitem);
            }
            MessageUtil.log("Loaded " + loot.size() + " loot items.");
            this.loot = loot;
        }
        MessageUtil.log("Loaded NPC: " + getID());
        isValid = true;
    }

    public void loadDelayed() {
        SpellLibrary spellbook = Bukkit.getServer().getSpellbookAPI().getLibrary();
        if (spellbook == null) {
            Aether.addException(ID, "No Spellbook found", "Are you running Papyrus?", null);
            return; // Very critical lol
        }
        if (cfg.contains("spells.onDamaged")) {
            ConfigurationSection section = cfg.getConfigurationSection("spells.onDamaged");
            if (section == null || section.getKeys(false).isEmpty()) {
                Aether.addException(ID, "No spells found for onDamaged", "Ensure the configuration is correct and not empty", null);
            } else {
                for (String string : section.getKeys(false)) {
                    SpellCastEntry entry = new SpellCastEntry();
                    if (section.getConfigurationSection(string) == null) {
                        Aether.addException(ID, "No onDamaged config found for spell " + string, "Ensure the configuration is correct", null);
                        continue;
                    }
                    entry.load(ID + " onDamaged", section.getConfigurationSection(string));
                    onDamagedSpells.add(entry);
                }
            }
        }
        if (cfg.contains("spells.onTimer")) {
            ConfigurationSection section = cfg.getConfigurationSection("spells.onTimer");
            if (section == null || section.getKeys(false).isEmpty()) {
                Aether.addException(ID, "No spells found for onTimer", "Ensure the configuration is correct and not empty", null);
            } else {
                for (String string : section.getKeys(false)) {
                    SpellCastEntry entry = new SpellCastEntry();
                    if (section.getConfigurationSection(string) == null) {
                        Aether.addException(ID, "No onTimer config found for spell " + string, "Ensure the configuration is correct", null);
                        continue;
                    }
                    entry.load(ID + " onTimer", section.getConfigurationSection(string));
                    onTimerSpells.add(entry);
                }
            }
        }
        if (cfg.contains("spells.onAttack")) {
            ConfigurationSection section = cfg.getConfigurationSection("spells.onAttack");
            if (section == null || section.getKeys(false).isEmpty()) {
                Aether.addException(ID, "No spells found for onAttack", "Ensure the configuration is correct and not empty", null);
            } else {
                for (String string : section.getKeys(false)) {
                    SpellCastEntry entry = new SpellCastEntry();
                    if (section.getConfigurationSection(string) == null) {
                        Aether.addException(ID, "No onAttack config found for spell " + string, "Ensure the configuration is correct", null);
                        continue;
                    }
                    entry.load(ID + " onAttack", section.getConfigurationSection(string));
                    onAttackSpells.add(entry);
                }
            }
        }
        if (cfg.contains("spells.onDeath")) {
            ConfigurationSection section = cfg.getConfigurationSection("spells.onDeath");
            if (section == null || section.getKeys(false).isEmpty()) {
                Aether.addException(ID, "No spells found for onDeath", "Ensure the configuration is correct and not empty", null);
            } else {
                for (String string : section.getKeys(false)) {
                    SpellCastEntry entry = new SpellCastEntry();
                    if (section.getConfigurationSection(string) == null) {
                        Aether.addException(ID, "No onDeath config found for spell " + string, "Ensure the configuration is correct", null);
                        continue;
                    }
                    entry.load(ID + " onDeath", section.getConfigurationSection(string));
                    onDeathSpells.add(entry);
                }
            }
        }
        if (cfg.contains("spells.onTarget")) {
            ConfigurationSection section = cfg.getConfigurationSection("spells.onTarget");
            if (section == null || section.getKeys(false).isEmpty()) {
                Aether.addException(ID, "No spells found for onTarget", "Ensure the configuration is correct and not empty", null);
            } else {
                for (String string : section.getKeys(false)) {
                    SpellCastEntry entry = new SpellCastEntry();
                    if (section.getConfigurationSection(string) == null) {
                        MessageUtil.log("No configuration found for spell " + string + " in " + ID);
                        continue;
                    }
                    entry.load(ID + " onTarget", section.getConfigurationSection(string));
                    onTargetSpells.add(entry);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "NPCData{" +
                "class=" + entityClass +
                ";displayType=" + displayType +
                ";displayName=" + displayName +
                ";instancable=" + instancable +
                ";modelID=" + modelID +
                ";persistent=" + persistent +
                "}";
    }
}
