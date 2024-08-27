package de.erethon.aether.creature;

import com.google.gson.Gson;
import de.erethon.aether.Aether;
import de.erethon.aether.ai.GoalLoader;
import de.erethon.aether.ai.goals.AEPathfinderGoal;
import de.erethon.aether.combat.SpellCastEntry;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.spellbook.api.SpellData;
import de.erethon.spellbook.api.SpellLibrary;
import de.erethon.spellbook.api.SpellbookAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.bukkit.Bukkit;
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
    Random random = new Random();

    // Attributes
    private double maxHealth = 20;
    private double range = 32;
    private double knockbackResistance;
    private double movementSpeed = 0.2;
    private double flyingSpeed = 2;
    private double damage = 2;
    private double armor;
    private double armorToughness;
    private double knockback;
    private double attackSpeed = 10;

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

    private Set<SpellCastEntry> onDamagedSpells = new HashSet<>();
    private Set<SpellCastEntry> onTimerSpells = new HashSet<>();
    private Set<SpellCastEntry> onAttackSpells = new HashSet<>();
    private Set<SpellCastEntry> onDeathSpells = new HashSet<>();
    private Set<SpellCastEntry> onTargetSpells = new HashSet<>();

    // AI
    private Set<AEPathfinderGoal> goals = new HashSet<>();
    private Set<AEPathfinderGoal> targets = new HashSet<>();

    // Loot
    private int dropXP = 0;
    private Set<ItemStack> loot = new HashSet<>();

    public NPCData(EntityType<?> displayType) {
        this.displayType = displayType;
        loot.add(new ItemStack(Material.BEDROCK));
    }

    public NPCData(ConfigurationSection cfg, String id) {
        this.cfg = cfg;
        this.ID = id;
        load();
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


    public String getID() {
        return ID;
    }

    public ConfigurationSection getCfg() {
        return cfg;
    }

    public BoundingBox getHitbox() {
        return hitbox;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getRange() {
        return range;
    }

    public double getKnockbackResistance() {
        return knockbackResistance;
    }

    public double getMovementSpeed() {
        return movementSpeed;
    }

    public double getDamage() {
        return damage;
    }

    public double getArmor() {
        return armor;
    }

    public double getArmorToughness() {
        return armorToughness;
    }

    public double getKnockback() {
        return knockback;
    }

    public double getAttackSpeed() {
        return attackSpeed;
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

    public double getFlyingSpeed() {
        return flyingSpeed;
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

    public Collection<? extends org.bukkit.inventory.ItemStack> getLoot() {
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
            MessageUtil.log("Could not find class " + classString + " for " + ID + "! Unable to load entity.");
            throw new RuntimeException(e);
        }
        // General
        displayName = MiniMessage.miniMessage().deserialize(cfg.getString("displayName", "NPC"));
        displayType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("minecraft", cfg.getString("displayType", "pig")));
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
        maxHealth = cfg.getDouble("config.attributes.health", 20);
        range = cfg.getDouble("config.attributes.range", 32);
        knockbackResistance = cfg.getDouble("config.attributes.knockbackResistance", 0);
        movementSpeed = cfg.getDouble("config.attributes.speed", 0.2);
        damage = cfg.getDouble("config.attributes.damage", 2);
        armor = cfg.getDouble("config.attributes.armor", 0);
        armorToughness = cfg.getDouble("config.attributes.armorToughness", 0);
        knockback = cfg.getDouble("config.attributes.knockback", 0);
        attackSpeed = cfg.getDouble("config.attributes.attackSpeed", 4);
        flyingSpeed = cfg.getDouble("config.attributes.flyingSpeed", 0.4);
        // Equipment
        HItemLibrary itemLibrary = plugin.getItemLibrary();
        mainHand = itemLibrary.get(NamespacedKey.fromString(cfg.getString("equipment.hand", "minecraft:air")));
        offHand = itemLibrary.get(NamespacedKey.fromString(cfg.getString("equipment.offhand", "minecraft:air")));
        helmet = itemLibrary.get(NamespacedKey.fromString(cfg.getString("equipment.helmet", "minecraft:air")));
        chest = itemLibrary.get(NamespacedKey.fromString(cfg.getString("equipment.chest", "minecraft:air")));
        leggings = itemLibrary.get(NamespacedKey.fromString(cfg.getString("equipment.leggings", "minecraft:air")));
        boots = itemLibrary.get(NamespacedKey.fromString(cfg.getString("equipment.boots", "minecraft:air")));
        // Sounds & Messages
        if (cfg.contains("interaction.sounds")) {
            attackSound = Sound.valueOf(cfg.getString("interaction.sounds.attack"));
            ambientSound = Sound.valueOf(cfg.getString("interaction.sounds.ambient", null));
            shootSound = Sound.valueOf(cfg.getString("interaction.sounds.shoot", null));
            deathSound = Sound.valueOf(cfg.getString("interaction.sounds.death", null));
            hurtSound = Sound.valueOf(cfg.getString("interaction.sounds.hurt", null));
        }
        ambientMessages = cfg.getStringList("interaction.messages");
        randomTalker = cfg.getBoolean("interaction.randomTalker", false);
        // Combat
        faction = cfg.getString("faction", null);
        SpellLibrary spellbook = Bukkit.getServer().getSpellbookAPI().getLibrary();
        if (cfg.contains("spells.onDamaged")) {
            ConfigurationSection section = cfg.getConfigurationSection("spells.onDamaged");
            if (section == null || section.getKeys(false).isEmpty()) {
                MessageUtil.log("No spells found for onDamaged in " + ID);
            } else {
                for (String string : section.getKeys(false)) {
                    SpellCastEntry entry = new SpellCastEntry();
                    if (section.getConfigurationSection(string) == null) {
                        MessageUtil.log("No configuration found for " + string + " in " + ID);
                        continue;
                    }
                    entry.load(section.getConfigurationSection(string));
                    onDamagedSpells.add(entry);
                }
            }
        }
        if (cfg.contains("spells.onTimer")) {
            ConfigurationSection section = cfg.getConfigurationSection("spells.onTimer");
            if (section == null || section.getKeys(false).isEmpty()) {
                MessageUtil.log("No spells found for onTimer in " + ID);
            } else {
                for (String string : section.getKeys(false)) {
                    SpellCastEntry entry = new SpellCastEntry();
                    if (section.getConfigurationSection(string) == null) {
                        MessageUtil.log("No configuration found for " + string + " in " + ID);
                        continue;
                    }
                    entry.load(section.getConfigurationSection(string));
                    onDamagedSpells.add(entry);
                }
            }
        }
        if (cfg.contains("spells.onAttack")) {
            ConfigurationSection section = cfg.getConfigurationSection("spells.onAttack");
            if (section == null || section.getKeys(false).isEmpty()) {
                MessageUtil.log("No spells found for onAttack in " + ID);
            } else {
                for (String string : section.getKeys(false)) {
                    SpellCastEntry entry = new SpellCastEntry();
                    if (section.getConfigurationSection(string) == null) {
                        MessageUtil.log("No configuration found for " + string + " in " + ID);
                        continue;
                    }
                    entry.load(section.getConfigurationSection(string));
                    onDamagedSpells.add(entry);
                }
            }
        }
        if (cfg.contains("spells.onDeath")) {
            ConfigurationSection section = cfg.getConfigurationSection("spells.onDeath");
            if (section == null || section.getKeys(false).isEmpty()) {
                MessageUtil.log("No spells found for onDeath in " + ID);
            } else {
                for (String string : section.getKeys(false)) {
                    SpellCastEntry entry = new SpellCastEntry();
                    if (section.getConfigurationSection(string) == null) {
                        MessageUtil.log("No configuration found for " + string + " in " + ID);
                        continue;
                    }
                    entry.load(section.getConfigurationSection(string));
                    onDamagedSpells.add(entry);
                }
            }
        }
        if (cfg.contains("spells.onTarget")) {
            ConfigurationSection section = cfg.getConfigurationSection("spells.onTarget");
            if (section == null || section.getKeys(false).isEmpty()) {
                MessageUtil.log("No spells found for onTarget in " + ID);
            } else {
                for (String string : section.getKeys(false)) {
                    SpellCastEntry entry = new SpellCastEntry();
                    if (section.getConfigurationSection(string) == null) {
                        MessageUtil.log("No configuration found for " + string + " in " + ID);
                        continue;
                    }
                    entry.load(section.getConfigurationSection(string));
                    onDamagedSpells.add(entry);
                }
            }
        }

        // AI
        if (cfg.contains("ai.goals")) {
            try {
                goals = GoalLoader.loadGoals(cfg.getStringList("ai.goals"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (cfg.contains("ai.targets")) {
            try {
                targets = GoalLoader.loadGoals(cfg.getStringList("ai.targets"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Loot
        dropXP = cfg.getInt("loot.xp", 0);
        if (cfg.contains("loot.items")) {
            Set<ItemStack> loot = new HashSet<>();
            for (String s : cfg.getStringList("loot.items")) {
                String[] split = s.split(":");
                Material material = Material.valueOf(split[0]);
                int amount = Integer.parseInt(split[1]);
                loot.add(new ItemStack(material, amount));
            }
        }
        MessageUtil.log("Loaded NPC: " + this);
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
