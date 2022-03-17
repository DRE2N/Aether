package de.erethon.aether.creature;

import de.erethon.aether.Aether;
import de.erethon.aether.ai.pathfinder.goals.AEPathfinderGoal;
import de.erethon.aether.listener.AEPacketListener;
import de.erethon.aether.tools.NMSUtils;
import de.erethon.aether.tools.UpdatedMessageUtil;
import de.erethon.bedrock.chat.MessageUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftMob;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class ActiveNPC {

    Aether plugin = Aether.getInstance();
    String id;
    ArmorStand stand;
    Entity baseEntity;
    NPCData npcData;
    boolean isAttacked = false;
    boolean isTalking = false;
    Set<Player> viewers = new HashSet<>();

    public ActiveNPC(NPCData npcData) {
        this.npcData = npcData;
    }

    public ActiveNPC(org.bukkit.entity.Entity entity) {
        String npcID = entity.getPersistentDataContainer().get(plugin.getKey(), PersistentDataType.STRING);
        if (npcID == null) {
            return;
        }
        UpdatedMessageUtil.log("Found " + npcID + " in world, updating & adding to manager...");
        npcData = plugin.getCreatureManager().getByID(npcID);
        if (npcData == null) {
            MessageUtil.log(npcID + " is invalid.");
            return;
        }
        baseEntity = entity;
    }

    public ActiveNPC(NPCData npcData, String id) {
        this.npcData = npcData;
        this.id = id;
    }

    public void spawn(Location location) {
        net.minecraft.world.entity.Entity nmsEntity = NMSUtils.spawnEntityWithoutSending(location, npcData.getBaseType());
        if (nmsEntity == null) {
            return;
        }
        baseEntity = nmsEntity.getBukkitEntity();
        plugin.getActiveCreatureManager().addActive(baseEntity, this);
        baseEntity.getPersistentDataContainer().set(plugin.getKey(), PersistentDataType.STRING, npcData.getID());
        setProperties();
        NMSUtils.addEntity(nmsEntity, location);
    }

    public void setProperties() {
        baseEntity.setSilent(true);
        baseEntity.setGlowing(npcData.isGlowing());
        baseEntity.setGravity(npcData.isGravity());
        baseEntity.setInvulnerable(npcData.isInvulnerable());
        baseEntity.setPersistent(npcData.isPersistent());
        if (baseEntity instanceof LivingEntity livingBase) {
            LivingEntity living = (LivingEntity) baseEntity;
            setAttributes(living);
            livingBase.setCollidable(npcData.hasCollision());
            livingBase.setMaximumAir(npcData.getMaximumAir());
            livingBase.setMaximumNoDamageTicks(npcData.getNoDamageTicks());
            equip(living);
        }

        if (npcData.getDisplayType() == org.bukkit.entity.EntityType.PLAYER) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                AEPacketListener.doPlayerStuff(player, baseEntity.getUniqueId(), npcData.getDisplayName());
            }
        }
        if (npcData.getDisplayName() != null) {
            baseEntity.setCustomName(npcData.getDisplayName());
            baseEntity.setCustomNameVisible(true);
        }
        CraftEntity craftEntity = (CraftEntity) baseEntity;
        Bukkit.getMobGoals().removeAllGoals((org.bukkit.entity.Mob) baseEntity);
        net.minecraft.world.entity.Mob mob = (Mob) craftEntity.getHandle();
        for (AEPathfinderGoal aegoal : npcData.getGoals()) {
            CraftLivingEntity entity = (CraftLivingEntity) craftEntity;
            addTarget(0, new NearestAttackableTargetGoal<>(mob, net.minecraft.world.entity.LivingEntity.class, false));
            addGoal(aegoal.getPrio(), aegoal.get(entity.getHandle()));
        }
    }

    public void setDisplayname(String displayname) {
        baseEntity.setCustomName(displayname);
    }

    public void displayTextAboveHead(Player player, String text, int timeout, boolean multiline) {
        if (isTalking) {
            return;
        }
        isTalking = true;
        String name = baseEntity.getCustomName();
        String[] strings = text.split(";");
        if (multiline && text.contains(";")) {
            baseEntity.setCustomName("§a" + name + ": §7§o" + strings[0]);
            baseEntity.setCustomNameVisible(true);
            CraftWorld craftWorld = (CraftWorld) baseEntity.getWorld();
            Level world = craftWorld.getHandle();
            stand = new ArmorStand(EntityType.ARMOR_STAND, world);
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.getBukkitEntity().setCustomName("§7§o" + strings[1]);
            stand.setCustomNameVisible(true);
            stand.getBukkitEntity().teleport(baseEntity.getLocation().clone().add(0, 1.68, 0));
            world.addFreshEntity(stand, CreatureSpawnEvent.SpawnReason.CUSTOM);
        } else {
            baseEntity.setCustomName("§a" + name + ": §7§o" + text);
            baseEntity.setCustomNameVisible(true);
        }
        BukkitRunnable removeStand = new BukkitRunnable() {
            @Override
            public void run() {
                baseEntity.setCustomName(name);
                if (baseEntity instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity) baseEntity;
                    living.setAI(true);
                }
                if (stand != null) {
                    stand.getBukkitEntity().remove();
                }
                isTalking = false;
            }
        };
        removeStand.runTaskLater(plugin, timeout * 20L);
    }

    public void equip(LivingEntity entity) {
        EntityEquipment eq = entity.getEquipment();
        eq.setItemInMainHand(new ItemStack(npcData.getMainHand()));
        eq.setItemInOffHand(new ItemStack(npcData.getOffHand()));
        eq.setHelmet(new ItemStack(npcData.getHelmet()));
        eq.setChestplate(new ItemStack(npcData.getChest()));
        eq.setLeggings(new ItemStack(npcData.getLeggings()));
        eq.setBoots(new ItemStack(npcData.getBoots()));
    }

    public void setAttributes(LivingEntity living) {
        // Health
        if (living.getAttribute(Attribute.GENERIC_MAX_HEALTH) == null) {
            living.registerAttribute(Attribute.GENERIC_MAX_HEALTH);
        }
        living.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(npcData.getMaxHealth());
        // Range
        if (living.getAttribute(Attribute.GENERIC_FOLLOW_RANGE) == null) {
            living.registerAttribute(Attribute.GENERIC_FOLLOW_RANGE);
        }
        living.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(npcData.getRange());
        // knockback resistance
        if (living.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE) == null) {
            living.registerAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        }
        living.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(npcData.getKnockbackResistance());
        // Movement speed
        if (living.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) == null) {
            living.registerAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        }
        living.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(npcData.getMovementSpeed());
        // Damage
        if (living.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) == null) {
            living.registerAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        }
        living.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(npcData.getDamage());
        // Armor
        if (living.getAttribute(Attribute.GENERIC_ARMOR) == null) {
            living.registerAttribute(Attribute.GENERIC_ARMOR);
        }
        living.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(npcData.getArmor());
        // Toughness
        if (living.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS) == null) {
            living.registerAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS);
        }
        living.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(npcData.getArmorToughness());
        // Knockback
        if (living.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK) == null) {
            living.registerAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK);
        }
        living.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK).setBaseValue(npcData.getKnockback());
        // Attack speed
        if (living.getAttribute(Attribute.GENERIC_ATTACK_SPEED) == null) {
            living.registerAttribute(Attribute.GENERIC_ATTACK_SPEED);
        }
        living.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(npcData.getAttackSpeed());
    }

    public void addGoal(int prio, Goal goal) {
        CraftMob mob = (CraftMob) baseEntity;
        mob.getHandle().goalSelector.addGoal(prio, goal);
    }

    public void addTarget(int prio, Goal goal) {
        CraftMob mob = (CraftMob) baseEntity;
        mob.getHandle().targetSelector.addGoal(prio, goal);
    }


    public void damage() {
        baseEntity.playEffect(EntityEffect.HURT);
    }

    public void playAmbientSound() {
        if (npcData.getAmbientSound() == null) {
            return;
        }
        baseEntity.getWorld().playSound(baseEntity.getLocation(), npcData.getAmbientSound(), org.bukkit.SoundCategory.VOICE, 1.0f, 1.0f);
    }

    public void playEffect(EntityEffect entityEffect) {
        baseEntity.playEffect(entityEffect);
    }


    public Entity getBaseEntity() {
        return baseEntity;
    }

    public boolean isAttacked() {
        return isAttacked;
    }

    public void setAttacked(boolean attacked) {
        isAttacked = attacked;
    }

    public Set<Player> getViewers() {
        viewers.addAll(Bukkit.getOnlinePlayers());
        return viewers;
    }

    public NPCData getNpc() {
        return npcData;
    }

    public boolean hasHit() {
        return true;
    }
}
