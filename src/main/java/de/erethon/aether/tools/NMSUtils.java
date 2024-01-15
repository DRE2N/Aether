package de.erethon.aether.tools;

import de.erethon.aether.creature.NPCData;
import de.erethon.bedrock.chat.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Optional;

public class NMSUtils {

    public static ArmorStand spawnInvisibleArmorstand(Location location, boolean invisible, boolean marker, boolean invulnerable, String name) {
        CraftWorld craftWorld = (CraftWorld) location.getWorld();
        Level world = craftWorld.getHandle();
        ArmorStand stand = new ArmorStand(net.minecraft.world.entity.EntityType.ARMOR_STAND, world);
        stand.setInvisible(invisible);
        stand.collides = false;
        stand.setMarker(marker);
        stand.setInvulnerable(invulnerable);
        stand.getBukkitEntity().teleport(location);
        stand.getBukkitEntity().setCustomName(name);
        stand.setCustomNameVisible(true);
        world.addFreshEntity(stand, CreatureSpawnEvent.SpawnReason.CUSTOM);
        return stand;
    }

    public static Entity spawnEntityWithoutSending(Location location, EntityType type) {
        CraftWorld craftWorld = (CraftWorld) location.getWorld();
        ServerLevel world = craftWorld.getHandle();
        return type.create(world);
    }

    public static void setDisplayType(org.bukkit.entity.Entity entity, NPCData data) {
        EntityType type = data.getDisplayType();
        if (type == null) {
            return;
        }
        CraftEntity craftEntity = (CraftEntity) entity;
        craftEntity.getHandle().displayEntityType = type;

    }

    public static EntityType getDisplayType(org.bukkit.entity.Entity entity) {
        CraftEntity craftEntity = (CraftEntity) entity;
        EntityType type = craftEntity.getHandle().displayEntityType;
        if (type == null) {
            return EntityType.PIG;
        }
        return type;
    }

    public static void addEntity(Entity entity, Location location) {
        entity.getBukkitEntity().teleport(location);
        Level world = entity.getCommandSenderWorld();
        world.addFreshEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
    }

}

