package de.erethon.aether.creature;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import de.erethon.aether.Aether;
import de.erethon.aether.tools.NMSUtils;
import de.erethon.commons.chat.MessageUtil;
import net.minecraft.server.v1_16_R3.EntityArmorStand;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.PathfinderGoal;
import net.minecraft.server.v1_16_R3.World;
import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftMob;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class ActiveNPC {

    Aether plugin = Aether.getInstance();
    String id;
    ProtocolManager protocol = ProtocolLibrary.getProtocolManager();
    EntityArmorStand stand;
    Entity baseEntity;
    NPC npc;
    boolean isAttacked = false;
    boolean isTalking = false;

    public ActiveNPC(NPC npc) {
        this.npc = npc;
    }

    public ActiveNPC(org.bukkit.entity.Entity entity) {
        String npcID = entity.getPersistentDataContainer().get(plugin.getKey(), PersistentDataType.STRING);
        if (npcID == null) {
            return;
        }
        MessageUtil.log("Found " + npcID + " in world, updating & adding to manager...");
        npc = plugin.getCreatureManager().getByID(npcID);
        if (npc == null) {
            MessageUtil.log(npcID + " is invalid.");
            return;
        }
        baseEntity = entity;
        setProperties();
    }

    public ActiveNPC(NPC npc,  String id) {
        this.npc = npc;
        this.id = id;
    }

    public void spawn(Location location) {
        net.minecraft.server.v1_16_R3.Entity nmsEntity = NMSUtils.spawnEntityWithoutSending(location, npc.getBaseType());
        if (nmsEntity == null) {
            return;
        }
        baseEntity = nmsEntity.getBukkitEntity();
        plugin.getActiveCreatureManager().addActive(baseEntity, this);
        baseEntity.getPersistentDataContainer().set(plugin.getKey(), PersistentDataType.STRING, npc.getID());
        setProperties();
        NMSUtils.addEntity(nmsEntity, location);
    }

    public void setProperties() {
        baseEntity.setSilent(true);
        baseEntity.setGlowing(npc.isGlowing());
        baseEntity.setGravity(npc.isGravity());
        baseEntity.setInvulnerable(npc.isInvulnerable());
        baseEntity.setPersistent(npc.isPersistent());
        if (baseEntity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) baseEntity;
            setAttributes(living);
            LivingEntity livingBase = (LivingEntity) baseEntity;
            livingBase.setCollidable(npc.hasCollision());
            livingBase.setMaximumAir(npc.getMaximumAir());
            livingBase.setMaximumNoDamageTicks(npc.getNoDamageTicks());
            equip(living);
        }

        if (npc.getDisplayType() == org.bukkit.entity.EntityType.PLAYER) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                //PacketListener.doPlayerStuff(player, baseEntity.getUniqueId(), npc.getDisplayName());
            }
        }
        if (npc.getDisplayName() != null) {
            baseEntity.setCustomName(npc.getDisplayName());
            baseEntity.setCustomNameVisible(true);
        }
        Mob mob = (Mob) baseEntity;
        /*Bukkit.getMobGoals().removeAllGoals(mob);
        addTarget(2,  new PathfinderGoalNearestAttackableTarget((EntityInsentient) nmsEntity, EntityHuman.class, true));
        if (nmsEntity instanceof EntityCreature) {
            addGoal(1, new PathfinderGoalMeleeAttack((EntityCreature) nmsEntity, 1.5, true));
        }*/
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
        NPCInstancing instancing = plugin.getNpcInstancing();
        String[] strings = text.split(";");
        if (multiline && text.contains(";")) {
            MessageUtil.broadcastMessage("Found multiline...");
            baseEntity.setCustomName("§a" + name + ": §7§o" + strings[0]);
            baseEntity.setCustomNameVisible(true);
            CraftWorld craftWorld = (CraftWorld) baseEntity.getWorld();
            World world = craftWorld.getHandle();
            stand = new EntityArmorStand(EntityTypes.ARMOR_STAND, world);
            instancing.addInstanced(stand.getUniqueID());
            instancing.show(player, stand.getUniqueID());
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.getBukkitEntity().setCustomName("§7§o" + strings[1]);
            stand.setCustomNameVisible(true);
            MessageUtil.broadcastMessage(stand.toString());
            stand.getBukkitEntity().teleport(baseEntity.getLocation().clone().add(0, 1.68, 0));
            world.addEntity(stand, CreatureSpawnEvent.SpawnReason.CUSTOM);
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
                    instancing.removeInstanced(stand.getUniqueID());
                    stand.getBukkitEntity().remove();
                }
                isTalking = false;
            }
        };
        removeStand.runTaskLater(plugin, timeout * 20L);
    }

    public void equip(LivingEntity entity) {
        EntityEquipment eq = entity.getEquipment();
        eq.setItemInMainHand(new ItemStack(npc.getMainHand()));
        eq.setItemInOffHand(new ItemStack(npc.getOffHand()));
        eq.setHelmet(new ItemStack(npc.getHelmet()));
        eq.setChestplate(new ItemStack(npc.getChest()));
        eq.setLeggings(new ItemStack(npc.getLeggings()));
        eq.setBoots(new ItemStack(npc.getBoots()));
    }

    public void setAttributes(LivingEntity living) {
        // Health
        if (living.getAttribute(Attribute.GENERIC_MAX_HEALTH) == null) {
            living.registerAttribute(Attribute.GENERIC_MAX_HEALTH);
        }
        living.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(npc.getMaxHealth());
        // Range
        if (living.getAttribute(Attribute.GENERIC_FOLLOW_RANGE) == null) {
            living.registerAttribute(Attribute.GENERIC_FOLLOW_RANGE);
        }
        living.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(npc.getRange());
        // knockback resistance
        if (living.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE) == null) {
            living.registerAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        }
        living.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(npc.getKnockbackResistance());
        // Movement speed
        if (living.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) == null) {
            living.registerAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        }
        living.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(npc.getMovementSpeed());
        // Damage
        if (living.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) == null) {
            living.registerAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        }
        living.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(npc.getDamage());
        // Armor
        if (living.getAttribute(Attribute.GENERIC_ARMOR) == null) {
            living.registerAttribute(Attribute.GENERIC_ARMOR);
        }
        living.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(npc.getArmor());
        // Toughness
        if (living.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS) == null) {
            living.registerAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS);
        }
        living.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(npc.getArmorToughness());
        // Knockback
        if (living.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK) == null) {
            living.registerAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK);
        }
        living.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK).setBaseValue(npc.getKnockback());
        // Attack speed
        if (living.getAttribute(Attribute.GENERIC_ATTACK_SPEED) == null) {
            living.registerAttribute(Attribute.GENERIC_ATTACK_SPEED);
        }
        living.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(npc.getAttackSpeed());
    }

    public void addGoal(int prio, PathfinderGoal goal) {
        CraftMob mob = (CraftMob) baseEntity;
        mob.getHandle().goalSelector.addGoal(prio, goal);
    }

    public void addTarget(int prio, PathfinderGoal goal) {
        CraftMob mob = (CraftMob) baseEntity;
        mob.getHandle().targetSelector.addGoal(prio, goal);
    }


    public void damage() {
        baseEntity.playEffect(EntityEffect.HURT);
    }

    public void playAmbientSound() {
        if (npc.getAmbientSound() == null) {
            return;
        }
        baseEntity.getWorld().playSound(baseEntity.getLocation(), npc.getAmbientSound(), org.bukkit.SoundCategory.VOICE, 1.0f, 1.0f);
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


    public NPC getNpc() {
        return npc;
    }

    public boolean hasHit() {
        return true;
    }
}
