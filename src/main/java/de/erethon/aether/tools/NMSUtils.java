package de.erethon.aether.tools;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.entity.EntityType;
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
        world.addEntity(stand, CreatureSpawnEvent.SpawnReason.CUSTOM);
        return stand;
    }

    public static Entity spawnEntityWithoutSending(Location location, EntityType type) {
        CraftWorld craftWorld = (CraftWorld) location.getWorld();
        Level world = craftWorld.getHandle();
        Optional<net.minecraft.world.entity.EntityType<?>> types = net.minecraft.world.entity.EntityType.byString(type.getKey().asString());
        if (types.isPresent()) {
            net.minecraft.world.entity.EntityType<?> entityTypes = types.get();
            return entityTypes.create(world);
        }
        return null;
    }

    public static void addEntity(Entity entity, Location location) {
        entity.getBukkitEntity().teleport(location);
        Level world = entity.getCommandSenderWorld();
        world.addEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
    }

}

