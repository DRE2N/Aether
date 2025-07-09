package de.erethon.aether.creature;

import de.erethon.aether.Aether;
import de.erethon.aether.ai.goals.AEPathfinderGoal;
import de.erethon.aether.combat.SpellCastEntry;
import de.erethon.aether.tools.NMSUtils;
import de.erethon.bedrock.chat.MessageUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftMob;
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

@Deprecated
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
        npcData = plugin.getCreatureManager().getByID(npcID);
        if (npcData == null) {
            MessageUtil.log(npcID + " is invalid. (UUID: " + entity.getUniqueId() + ")");
            return;
        }
        baseEntity = entity;
        setProperties();
    }

    public ActiveNPC(NPCData npcData, String id) {
        this.npcData = npcData;
        this.id = id;
    }

    public void spawn(Location location) {
        return; /*
        net.minecraft.world.entity.Entity nmsEntity = NMSUtils.spawnEntityWithoutSending(location, npcData.getBaseType());
        if (nmsEntity == null) {
            return;
        }
        baseEntity = nmsEntity.getBukkitEntity();
        plugin.getActiveCreatureManager().addActive(baseEntity, this);
        baseEntity.getPersistentDataContainer().set(plugin.getKey(), PersistentDataType.STRING, npcData.getID());
        setProperties();
        NMSUtils.setDisplayType(baseEntity, npcData);
        NMSUtils.addEntity(nmsEntity, location);
        */
    }

    public void setProperties() {
        baseEntity.setSilent(true);
        //NMSUtils.setDisplayType(baseEntity, npcData);
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
        }
        /*if (npcData.getDisplayType() == org.bukkit.entity.EntityType.PLAYER) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                AEPacketListener.doPlayerStuff(player, baseEntity.getUniqueId(), npcData.getDisplayName());
            }
        }*/
        if (npcData.getDisplayName() != null) {
            baseEntity.setCustomNameVisible(true);
        }
        CraftEntity craftEntity = (CraftEntity) baseEntity;
        if (!npcData.getGoals().isEmpty() || !npcData.getTargets().isEmpty()) {
            Bukkit.getMobGoals().removeAllGoals((org.bukkit.entity.Mob) baseEntity);
        }
        net.minecraft.world.entity.Mob mob = (Mob) craftEntity.getHandle();
        CraftLivingEntity entity = (CraftLivingEntity) craftEntity;
        for (AEPathfinderGoal aegoal : npcData.getGoals()) {
            addGoal(aegoal.getPrio(), aegoal.get(entity.getHandle()));
        }
        for (AEPathfinderGoal aetarget : npcData.getTargets()) {
            addTarget(aetarget.getPrio(), aetarget.get(entity.getHandle()));
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

    public void setAttributes(LivingEntity living) {
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
        baseEntity.broadcastHurtAnimation(baseEntity.getTrackedBy());
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

    public net.minecraft.world.entity.Mob getNMSMob() {
        return (net.minecraft.world.entity.Mob) ((CraftEntity) baseEntity).getHandle();
    }

    public boolean isAttacked() {
        return isAttacked;
    }

    public void setAttacked(boolean attacked) {
        isAttacked = attacked;
    }

    public void onDamaged() {
        for (SpellCastEntry spell : npcData.getOnDamagedSpells()) {
            castSpell(spell);
        }
    }

    public void onAttack() {
        for (SpellCastEntry spell : npcData.getOnAttackSpells()) {
            castSpell(spell);
        }
    }

    public void onDeath() {
        for (SpellCastEntry spell : npcData.getOnDeathSpells()) {
            castSpell(spell);
        }
    }

    public void onTarget() {
        for (SpellCastEntry spell : npcData.getOnTargetSpells()) {
            castSpell(spell);
        }
    }

    private void castSpell(SpellCastEntry entry) {
        if (!entry.canCast()) {
            return;
        }
        LivingEntity caster = (LivingEntity) getBaseEntity();
        if (entry.getSpell() == null) {
            return;
        }
        caster.cast(entry.getSpell());
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
