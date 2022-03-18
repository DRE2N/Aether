package de.erethon.aether.creature;

import de.erethon.aether.ai.GoalLoader;
import de.erethon.aether.ai.goals.AEPathfinderGoal;
import de.erethon.bedrock.chat.MessageUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

import java.util.*;

public class NPCData {

    // General
    private String ID;
    ConfigurationSection cfg;
    private EntityType baseType;
    private EntityType displayType;
    private String displayName = "npc";
    private boolean instancable = true;
    private boolean hasCollision = true;
    private boolean persistent = true;
    private boolean invulnerable;
    private boolean glowing;
    private boolean gravity = true;
    private boolean nameTagVisible;
    private int noDamageTicks;
    private int maximumAir;

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
    private Material mainHand;
    private Material offHand;
    private Material helmet;
    private Material chest;
    private Material leggings;
    private Material boots;

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

    // AI
    private Set<AEPathfinderGoal> goals = new HashSet<>();
    private Set<AEPathfinderGoal> targets = new HashSet<>();

    // Loot
    private int dropXP = 0;
    private Set<ItemStack> loot = new HashSet<>();

    public NPCData(EntityType baseType, EntityType displayType) {
        this.baseType = baseType;
        this.displayType = displayType;
        loot.add(new ItemStack(Material.BEDROCK));
    }

    public NPCData(ConfigurationSection cfg, String id) {
        this.cfg = cfg;
        this.ID = id;
        load();
    }

    public EntityType getBaseType() {
        return baseType;
    }

    public void setBaseType(EntityType baseType) {
        this.baseType = baseType;
    }

    public EntityType getDisplayType() {
        return displayType;
    }

    public void setDisplayType(EntityType displayType) {
        this.displayType = displayType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isInstancable() {
        return instancable;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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

    public Material getMainHand() {
        return mainHand;
    }

    public Material getOffHand() {
        return offHand;
    }

    public Material getHelmet() {
        return helmet;
    }

    public Material getChest() {
        return chest;
    }

    public Material getLeggings() {
        return leggings;
    }

    public Material getBoots() {
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

    public void load() {
        // General
        MessageUtil.log("Loading npc " + ID);
        displayName = cfg.getString("displayname", "");
        baseType = EntityType.valueOf(cfg.getString("baseType", "PIG").toUpperCase());
        displayType = EntityType.valueOf(cfg.getString("displayType", "PIG").toUpperCase());
        instancable = cfg.getBoolean("instancable", true);
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
        mainHand = Material.valueOf(cfg.getString("equipment.hand", "AIR").toUpperCase());
        offHand = Material.valueOf(cfg.getString("equipment.offhand", "AIR").toUpperCase());
        helmet = Material.valueOf(cfg.getString("equipment.helmet", "AIR").toUpperCase());
        chest = Material.valueOf(cfg.getString("equipment.chest", "AIR").toUpperCase());
        leggings = Material.valueOf(cfg.getString("equipment.leggings", "AIR").toUpperCase());
        boots = Material.valueOf(cfg.getString("equipment.boots", "AIR").toUpperCase());
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

    }
}
